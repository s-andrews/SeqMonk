package uk.ac.babraham.SeqMonk.Vistory;

import java.util.Date;

import javax.swing.JPanel;;

public abstract class VistoryBlock extends JPanel {
	
	private Date date;
	
	public VistoryBlock () {
		date = new Date();
	}
	
	public abstract String getHTML();

}
