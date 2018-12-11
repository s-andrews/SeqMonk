package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryTitle extends VistoryBlock {

	protected JTextArea text;
	private int indexPosition = 0;
	
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
	
	public void setIndex (int index) {
		this.indexPosition = index;
	}
	
	public void setText (String text) {
		this.text.setText(text);
	}
	
	
	public String getText () {
		return text.getText();
	}
	@Override
	public String getHTML() {
		return("<h2 id=\""+indexPosition+"\">"+EscapeHTML.escapeHTML(text.getText())+"</h2>");
	
	}

}
