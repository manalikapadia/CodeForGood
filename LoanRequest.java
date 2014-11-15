package process;

import java.sql.*;

public class LoanRequest {
	public int clientID;
	public int age;
	public int group;
	public String businessType;
	public String gender;
	
	public int weeks_to_repay;
	public double size_of_loan;
	public double interestRate;
	public int noOfCompetitors;
	
	public double savings; // FROM DB
	public boolean isMale; // FROM DB
	public int no_prev_loans; // FROM DB
	public Loan[] previous_loans; // from DB
	public Connection conn;
	public Statement st;
	public ResultSet rs;
	
	public LoanRequest(int in_clientID) {
		clientID = in_clientID;
		try{
			//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			//Properties connectionProps = new Properties();
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn=DriverManager.getConnection("jdbc:mysql://musoni.c4leprqodag9.eu-west-1.rds.amazonaws.com", "student", "cfg2014!");
			st = conn.createStatement();
			//for individual assessment
			rs = st.executeQuery("select (SELECT 2014-YEAR(cd.dateOfBirth) from ml_client_details cd where client_id = "+clientID+") as 'Age',"+
								"(SELECT cd.gender_cd from ml_client_details cd where client_id = "+clientID+") as 'Gender',"+
								"(select gc.group_id from m_group_client gc where client_id = "+clientID+") as 'Group',"+
								"(select cb.businessType from ml_client_business cb where client_id = "+clientID+") as 'Business',"+
								"(select sum(sa.account_balance_derived) from  m_savings_account sa where client_id = "+clientID+") as 'Savings',"+
								"(select l.nominal_interest_rate_per_period from m_loan l  where client_id = "+clientID+") as 'Interest',"+
								"(select l.principal_amount from m_loan l  where client_id = "+clientID+") as 'Amount',"+
								"(select l.term_frequency from m_loan l  where client_id = "+clientID+") as 'Weeks',"+
								"(select count(*) from ml_client_business where businessType = (select businessType from ml_client_business where client_id = "+clientID+") and client_id <> "+clientID+") as 'Competitors'"+
								"(select count(*) from m_loan where client_id = "+clientID+" and loan_status_id <> 100) as 'PrevLoans';");
			if(rs.next())
			{
				age = rs.getInt("Age");
				gender = rs.getString("Gender")+"";
				isMale = gender.equals("Female")?false:true;
				group = rs.getInt("Group");
				businessType = rs.getString("Business");
				savings = rs.getDouble("Savings");
				interestRate = rs.getDouble("Interest");
				size_of_loan = rs.getDouble("Amount");
				weeks_to_repay = rs.getInt("Weeks");
				no_prev_loans = rs.getInt("PrevLoans");
				noOfCompetitors = rs.getInt("Competitors");
			}
			//for group assessment
			rs = st.executeQuery("select (select avg(account_balance_derived) from m_savings_account where client_id IN (select g2.client_id from m_group_client g1, m_group_client g2  where g1.group_id = g2.group_id and g1.client_id <> g2.client_id and g1.client_id = @client_id )) as 'AvgSaving',"+
								"(select avg(approved_principal) from m_loan where client_id IN (select g2.client_id from m_group_client g1, m_group_client g2  where g1.group_id = g2.group_id and g1.client_id <> g2.client_id and g1.client_id = @client_id )) as 'AvgLoan',"+
								"(select sum(account_balance_derived) from m_savings_account where client_id IN (select g2.client_id from m_group_client g1, m_group_client g2  where g1.group_id = g2.group_id and g1.client_id <> g2.client_id and g1.client_id = @client_id )) as 'TotalSaving',"+
								"(select sum(approved_principal) from m_loan where client_id IN (select g2.client_id from m_group_client g1, m_group_client g2  where g1.group_id = g2.group_id and g1.client_id <> g2.client_id and g1.client_id = @client_id )) as 'TotalLoan',"+
								"(select count(*) from ml_client_business where businessType = (select businessType from ml_client_business where client_id = @client_id) and client_id IN (select g2.client_id from m_group_client g1, m_group_client g2  where g1.group_id = g2.group_id and g1.client_id <> g2.client_id and g1.client_id = @client_id )) 'GroupCompetitors',"+
								"(select count(*)  from m_loan l where  l.client_id IN (select g2.client_id from m_group_client g1, m_group_client g2  where g1.group_id = g2.group_id and g1.client_id <> g2.client_id and g1.client_id = @client_id ) and loan_status_id = 300) as 'ActiveLoans',"+
								"(select count(*)  from m_loan l where  l.client_id IN (select g2.client_id from m_group_client g1, m_group_client g2  where g1.group_id = g2.group_id and g1.client_id <> g2.client_id and g1.client_id = @client_id ) and loan_status_id = 600) as 'ClosedLoans';");
			if(rs.next())
			{
				//
			}
			if (no_prev_loans>0) {
				previous_loans = new Loan[no_prev_loans];
				int i = 0;
				rs = st.executeQuery("select loan_status_id as 'Status', approved_principal as 'Amount', term_frequency = 'Tenure' from m_loan where client_id = "+clientID+" and loan_status_id <> 100");
				while(rs.next()){
					previous_loans[i] = new Loan(clientID, rs.getDouble("Amount"), rs.getInt("Status"), rs.getInt("Tenure"));
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public double gender_modifier(boolean isMale) {
		double score = 0;
		
		if (isMale) {
			score = 1.055;
		} else {
			score = 0.945;
		}
		
		return score;		
	}
	
	
	public double age_modifier(int age) {
		double score = 1;
		
		if (age<18) {
			score = 0;
		} else if (age>=18 && age<=25) {
			score = 0.8;
		} else if (age>25 && age<=40) {
			score = 1.5;
		} else if (age>40 && age<=50) {
			score = 1.6;
		} else if (age>50 && age<=60) {
			score = 1.1;
		}
		
		return score;
	}
	
	public double business_score(String business_type) {
		double score = 1;
		switch (business_type) {
			case "Agriculture" : score = 1;
			case "Shopkeeping" : score = 1;
			default			   : score = 1;
			// add rest of cases
		}
		
		return score;
	}
	
	public double loan_size_class() {
		int loan_class = 1;
		
		if (size_of_loan < 1000) {
			loan_class = 1;
		} else if (size_of_loan >= 1000 && size_of_loan < 4500) {
			loan_class = 2;
		} else if (size_of_loan >= 4500 && size_of_loan < 8000) {
			loan_class = 3;
		} else if (size_of_loan >= 8000 && size_of_loan < 12500) {
			loan_class = 4;
		} else if (size_of_loan >= 12500 && size_of_loan < 16000) {
			loan_class = 5;
		} else if (size_of_loan >= 16000 && size_of_loan < 19500) {
			loan_class = 6;
		} else if (size_of_loan >= 19500 && size_of_loan < 23000) {
			loan_class = 7;
		} else if (size_of_loan >= 23000 && size_of_loan < 26500) {
			loan_class = 8;
		} else if (size_of_loan >= 26500 && size_of_loan < 30000) {
			loan_class = 9;
		} else { // size of loan >= 30000
			loan_class = 10;
		}
		
		return loan_class;
	}
	
	public double repay_rate_class() {
		double rate_class = 1;
		
		if (weeks_to_repay < 20) {
			rate_class = 1;
		} else if (weeks_to_repay >= 20 && weeks_to_repay < 25) {
			rate_class = 2;
		} else if (weeks_to_repay >= 25 && weeks_to_repay < 30) {
			rate_class = 3;
		} else if (weeks_to_repay >= 30 && weeks_to_repay < 35) {
			rate_class = 4;
		} else if (weeks_to_repay >= 35 && weeks_to_repay < 40) {
			rate_class = 5;
		} else if (weeks_to_repay >= 40 && weeks_to_repay < 45) {
			rate_class = 6;
		} else if (weeks_to_repay >= 45 && weeks_to_repay < 50) {
			rate_class = 7;
		} else if (weeks_to_repay >= 50 && weeks_to_repay < 55) {
			rate_class = 8;
		} else if (weeks_to_repay >= 55 && weeks_to_repay < 60) {
			rate_class = 9;
		} else { // weeks_to_repay >= 60
			rate_class = 10;
		}
		
		return rate_class;
	}
	
	public double savings_rating() {
		double rating = 1;
		
		if (savings < 0) {
			rating = 1;
		} else if (savings >= 0 && savings < 1000) {
			rating = 2;
		} else if (savings >= 1000 && savings < 2000) {
			rating = 3;
		} else if (savings >= 2000 && savings < 30000) {
			rating = 4;
		} else if (savings >= 3000 && savings < 4000) {
			rating = 5;
		} else if (savings >= 4000 && savings < 5000) {
			rating = 6;
		} else if (savings >= 5000 && savings < 6000) {
			rating = 7;
		} else if (savings >= 6000 && savings < 7000) {
			rating = 8;
		} else if (savings >= 7000 && savings < 8000) {
			rating = 9;
		} else { // savings >= 8000
			rating = 10;
		}
		
		return rating;
	}
	
	public double history_coefficient() {
		double h = 0.1;
				
		if (no_prev_loans > 0) {
			Double size_of_largest = 0.0;
			int success_loans = 0;
			
			for (int i=0; i<no_prev_loans; i++) {
				if (size_of_largest<previous_loans[i].amount) {
					size_of_largest = previous_loans[i].amount;
				}
				if (previous_loans[i].status == 600 || previous_loans[i].status == 700) {
					success_loans++;
				}
			}
			
			Double stepup = size_of_largest / size_of_loan;
			if (stepup < 1) {
				h = 3 * no_prev_loans;
			} else {
				h = (1.0 / stepup) * (success_loans / no_prev_loans);
			}
		}
		
		return h;
	}
	
	public double review_request() {
		double score = 0;
		
		double rate_of_repay = repay_rate_class()/loan_size_class();
		
		double security = savings_rating()/loan_size_class();
		
		System.out.println("rate of repay: " + rate_of_repay);
		System.out.println("security: " + security);
		
		score = rate_of_repay * security * age_modifier(age) * business_score(businessType) * history_coefficient();
		
		return score;
	}
	
	
	public static void main(String[] args) {
		LoanRequest test = new LoanRequest(4357);
		
		// THESE WILL BE READ FROM DB BUT INITIALISED HERE FOR TESTING
		test.age = 48;
		test.size_of_loan = 17000;
		test.weeks_to_repay = 25;
		test.group = 15;
		test.businessType = "Agriculture";
		test.isMale = true;
		test.savings = 3000;
		test.no_prev_loans = 1;
		test.previous_loans = new Loan[1];
		test.previous_loans[0] = new Loan(test.clientID, 10000.0, 600, 25);
		
		System.out.print("Score: " + test.review_request());
	}

}
