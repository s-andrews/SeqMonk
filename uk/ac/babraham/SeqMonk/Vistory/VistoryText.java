package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryText extends VistoryBlock {

	protected JTextArea text;
	
	public VistoryText () {
		text = new JTextArea();
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setFont(getFont());
		add(text,BorderLayout.CENTER);
	}
	
	public void setText (String text) {
		this.text.setText(text);
	}
	
	@Override
	public String getHTML() {

		StringBuffer sb = new StringBuffer();
		
		String [] sections = text.getText().split("\n");
		
		for (int s=0;s<sections.length;s++) {
			sb.append("<p>"+EscapeHTML.escapeHTML(sections[s])+"</p>");
		}
	
		return sb.toString();
	}

	@Override
	public boolean wantsFocus () {
		return true;
	}
	
	@Override
	public Component componentToFocus () {
		return text;
	}
	
}
