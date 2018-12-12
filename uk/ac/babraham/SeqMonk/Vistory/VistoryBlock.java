package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
	
	public boolean wantsFocus () {
		return false;
	}
	
	public Component componentToFocus() {
		return null;
	}
	
	public abstract String getHTML();
	
	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		setBorder(BorderFactory.createLineBorder(Color.GRAY,3));
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// This is a kludge to work around the fact that mouseExited is fired when
		// the mouse goes into a sub-component packed within the frame. We ignore 
		// exit events if we find they're inside a different component.
		java.awt.Point p = new java.awt.Point(e.getLocationOnScreen());
        SwingUtilities.convertPointFromScreen(p, e.getComponent());
        if(e.getComponent().contains(p)) {return;}
        
		setBorder(BorderFactory.createLineBorder(Color.WHITE,3));
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}


}
