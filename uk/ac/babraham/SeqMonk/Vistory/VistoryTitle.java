package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Date;

import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryTitle extends VistoryBlock {

	protected JTextArea text;
	private int indexPosition = 0;
	
	public VistoryTitle(Date date, String data) {
		super(date);
		startSetup();
		text.setText(data.replaceAll("<br>", "\n"));
	}
	
	public VistoryTitle () {
		startSetup();
	}

	private void startSetup() {
		text = new JTextArea("New Title");
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.addMouseListener(this);
		Font font = getFont();
		
		font = new Font(font.getName(), font.getStyle(), font.getSize()*2);
		text.setFont(font);
		
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

	@Override
	public String getType() {
		return "TITLE";
	}

	@Override
	public String getData() {
		return text.getText().replaceAll("\n", "<br>");
	}
	

}
