package uk.ac.babraham.SeqMonk.R;

public class RException extends Exception {

	private String RlogText;
	
	public RException (String message, String RlogText) {
		super(message);
		this.RlogText = RlogText;
	}

	public String logText () {
		return RlogText;
	}
	
}
