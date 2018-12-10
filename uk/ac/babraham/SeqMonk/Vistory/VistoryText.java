package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;

import javax.swing.JTextArea;

public class VistoryText extends VistoryBlock {

	private JTextArea text;
	
	public VistoryText () {
		text = new JTextArea();
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		
		setLayout(new BorderLayout());
		add(text,BorderLayout.CENTER);
	}
	
	public void setText (String text) {
		this.text.setText(text);
	}
	
	@Override
	public String getHTML() {
		// TODO Auto-generated method stub
		return null;
	}

}
