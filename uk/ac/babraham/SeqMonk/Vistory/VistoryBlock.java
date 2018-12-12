package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.Component;
import java.util.Date;

import javax.swing.JPanel;;

public abstract class VistoryBlock extends JPanel {
	
	private Date date;
	
	public VistoryBlock () {
		date = new Date();
	}
	
	public boolean wantsFocus () {
		return false;
	}
	
	public Component componentToFocus() {
		return null;
	}
	
	public abstract String getHTML();

}
