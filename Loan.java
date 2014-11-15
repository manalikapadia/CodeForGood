package process;

public class Loan {
	public int clientID;
	public Double amount;
	public int status;
	public int term;
	
	public Loan(int cID, Double amt, int stat, int tm) {
		clientID = cID;
		amount = amt;
		status = stat;
		term = tm;
	}
}
