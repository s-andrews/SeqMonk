package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.Date;

import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryText extends VistoryBlock implements FocusListener {

	protected JTextArea text;
	
	public VistoryText (Date date, String data) {
		super(date);
		startSetup();
		text.setText(data.replaceAll("<br>", "\n"));
	}
	
	public VistoryText () {
		startSetup();
	}
	
	private void startSetup() {
		text = new JTextArea("[Enter text here]");
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setFont(getFont());
		text.addMouseListener(this);
		text.addFocusListener(this);
		add(text,BorderLayout.CENTER);
	}
	
	public void setText (String text) {
		this.text.setText(text);
	}
	
	public void mouseClicked (MouseEvent me) {
		if (text.getText().equals("[Enter text here]")) {
			text.setText("");
		}
		super.mouseClicked(me);
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
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (text.getText().equals("")) {
			text.setText("[Enter text here]");
		}
	}

	@Override
	public String getType() {
		return "TEXT";
	}

	@Override
	public String getData() {
		return text.getText().replaceAll("\n", "<br>");
	}

}
