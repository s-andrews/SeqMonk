package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryBlock;

public class VistoryBlockButtons extends JPanel implements MouseListener {

	private VistoryBlock block;
			
	public VistoryBlockButtons (VistoryBlock block) {
		this.block = block;
		addMouseListener(this);
	}
	
	public void paintComponent (Graphics g) {
		g.setColor(Color.RED);
		g.fillRect(getWidth()-10, 0, 10, 10);
		g.setColor(Color.WHITE);
		g.drawLine(getWidth()-8, 2, getWidth()-2, 8);
		g.drawLine(getWidth()-8, 8, getWidth()-2, 2);
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		if (me.getY() < 10 && me.getX() > getWidth()-10) {
			Vistory.getInstance().removeBlock(block);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}
	
}