package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JTextArea;

public class VistoryTitle extends VistoryBlock {

	protected JTextArea text;
	
	public VistoryTitle () {
		text = new JTextArea();
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		Font font = getFont();
		
		font = new Font(font.getName(), font.getStyle(), font.getSize()*2);
		text.setFont(font);
		
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
