package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JTextArea;

public class VistoryEvent extends VistoryBlock {

	public String type;
	public String text;
	
	public VistoryEvent(String type, String text) {
		this.type = type;
		this.text = text;

		setLayout(new BorderLayout());
		setBackground(Color.LIGHT_GRAY);
		JLabel typeLabel = new JLabel(type);
		add(typeLabel,BorderLayout.NORTH);
		JTextArea textArea = new JTextArea(text);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		
		add(textArea,BorderLayout.CENTER);
	}
	
	@Override
	public String getHTML() {
		// TODO Auto-generated method stub
		return null;
	}

}
