package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryEvent extends VistoryBlock {

	public String type;
	public String text;
	
	public VistoryEvent(String type, String text) {
		this.type = type;
		this.text = text;

		setBackground(Color.LIGHT_GRAY);
		JLabel typeLabel = new JLabel(type);
		add(typeLabel,BorderLayout.NORTH);
		JTextArea textArea = new JTextArea(text);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		
		textArea.setFont(getFont());
		
		add(textArea,BorderLayout.CENTER);
	}
	
	@Override
	public String getHTML() {

		StringBuffer sb = new StringBuffer();
		sb.append("<p class=\"eventtype\">");
		sb.append(EscapeHTML.escapeHTML(type));
		sb.append("</p><p class=\"eventtext\">");
		sb.append(EscapeHTML.escapeHTML(text));
		sb.append("</p>");
		
		return sb.toString();
	}

}
