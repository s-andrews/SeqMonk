package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Date;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Displays.Vistory.VistoryBlockButtons;;

public abstract class VistoryBlock extends JPanel {
	
	private Date date;
	
	public VistoryBlock () {
		date = new Date();
		setLayout(new BorderLayout());
		add(new VistoryBlockButtons(this),BorderLayout.EAST);
	}
	
	public boolean wantsFocus () {
		return false;
	}
	
	public Component componentToFocus() {
		return null;
	}
	
	public abstract String getHTML();

}
