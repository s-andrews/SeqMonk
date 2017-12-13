package uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;

public class CountedException implements Comparable<CountedException>{

	
	private Exception exception;
	private int count;
	
	
	public CountedException (Exception exception) {
		this.exception = exception;
		count = 1;
	}
	
	public void increment () {
		++count;
	}
	
	public Exception exception () {
		return exception;
	}
	
	public int count () {
		return count;
	}
	
	public String toString() {
		return "["+count+" times] "+exception.getMessage();
	}

	@Override
	public int compareTo(CountedException o) {
		return o.count-count;
	}
}
