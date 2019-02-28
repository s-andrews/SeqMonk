package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Displays.Vistory.VistoryBlockButtons;;

public abstract class VistoryBlock extends JPanel implements MouseListener {
	
	private Date date;
	
	public VistoryBlock () {
		date = new Date();
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);
		add(new VistoryBlockButtons(this),BorderLayout.EAST);
		setBorder(BorderFactory.createLineBorder(Color.WHITE,3));
		addMouseListener(this);
	}
	
	public void requestVistoryFocus () {
		Vistory.getInstance().requestVistoryFocus(this);
	}
	
	public void setVistoryFocus (boolean hasFocus) {
		if (hasFocus) {
			setBorder(BorderFactory.createLineBorder(Color.GRAY,3));
		}
		else {
			setBorder(BorderFactory.createLineBorder(Color.WHITE,3));
		}
	}
		
	public abstract String getHTML();
	
	@Override
	public void mouseClicked(MouseEvent e) {
		requestVistoryFocus();
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

}
