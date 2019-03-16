package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryEvent extends VistoryBlock {

	public String type;
	public String text;
	
	public VistoryEvent (Date date, String data) {
		super(date);
		String [] splitdata = data.split(":", 2);
		this.type = splitdata[0];
		this.text = splitdata[1].replaceAll("<br>", "\n");
		
		startSetup();
	}
	
	public VistoryEvent(String type, String text) {
		this.type = type;
		this.text = text;
	
		startSetup();
	}
	
	
	private void startSetup () {
		setBackground(Color.LIGHT_GRAY);
		JLabel typeLabel = new JLabel(type);
		add(typeLabel,BorderLayout.NORTH);
		JTextArea textArea = new JTextArea(text);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		
		textArea.setFont(getFont());
		textArea.addMouseListener(this);
		
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

	@Override
	public String getType() {
		return "EVENT";
	}

	@Override
	public String getData() {
		return type+":"+text.replaceAll("\n", "<br>");
	}

	@Override
	public boolean allowsRelativePosition() {
		return false;
	}

}
