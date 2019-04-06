package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;;

public class BlankPanel extends JPanel {

	
	public BlankPanel () {
		super();
		setBackground(Color.WHITE);
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(10,300);
	}
	
	public Dimension getMinimumSize () {
		return new Dimension(1,300);
	}
	
}
