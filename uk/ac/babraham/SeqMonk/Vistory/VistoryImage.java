package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class VistoryImage extends VistoryBlock implements MouseMotionListener, MouseListener {

	private BufferedImage image;
	private int xOffset = 0;
	
	
	public VistoryImage (BufferedImage image) {
		this.image = image;
		addMouseMotionListener(this);
	}
	
	@Override
	public String getHTML() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		g.drawImage(image, xOffset, 0, this);
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(1,image.getHeight());
	}

	@Override
	public void mouseDragged(MouseEvent me) {
		// We use this to move around the image if there isn't
		// enough space to show all of it.  We'll always be able 
		// to show the height because we can scroll but we can't 
		// always show the width.
		
		// This is irrelevant if the image fits in the window
		if (getWidth() >= image.getWidth()) return;
		
		
		// The biggest start offset will be the difference between
		// the image size and the window width
		int maxOffset = image.getWidth() - getWidth();
		
		// We work out how far we are across the window proportionally
		// and scale the offset by that.
		
		
		int x = me.getX();
		if (x < 0) x=0;
		if (x > getWidth()) x=getWidth();
		
		double proportion = x / (double)getWidth();
		
		xOffset = 0-(int)(maxOffset * proportion);
		
		repaint();
		
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {
		if (xOffset != 0) {
			xOffset = 0;
			repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

}
