package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.Color;
import java.awt.Dimension;
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
		setBackground(Color.WHITE);
	}
	
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.RED);
		g.fillRect(getWidth()-10, 0, 10, 10);
		g.setColor(Color.WHITE);
		g.drawLine(getWidth()-8, 2, getWidth()-2, 8);
		g.drawLine(getWidth()-8, 8, getWidth()-2, 2);
		
		if (block.allowsRelativePosition()) {
			g.setColor(Color.BLUE);
			g.fillRect(getWidth()-10, 10, 10, 10);
			g.setColor(Color.WHITE);
			g.drawLine(getWidth()-8, 18, getWidth()-5, 12);
			g.drawLine(getWidth()-5, 12, getWidth()-2, 18);

		
			g.setColor(Color.BLUE);
			g.fillRect(getWidth()-10, 20, 10, 10);
			g.setColor(Color.WHITE);
			g.drawLine(getWidth()-8, 22, getWidth()-5, 28);
			g.drawLine(getWidth()-5, 28, getWidth()-2, 22);
		}
	}

	@Override
	public Dimension getMinimumSize () {
		return new Dimension(10,30);
	}

	@Override
	public Dimension getPreferredSize () {
		return new Dimension(10,30);
	}

	
	@Override
	public void mouseClicked(MouseEvent me) {
		if (me.getY() < 10 && me.getX() > getWidth()-10) {
			Vistory.getInstance().removeBlock(block);
		}
		if (block.allowsRelativePosition()) {
			if (me.getY() > 10 && me.getY() < 20 &&me.getX() > getWidth()-10) {
				Vistory.getInstance().raiseBlock(block);
			}
			if (me.getY() > 20 && me.getY() < 30 &&me.getX() > getWidth()-10) {
				Vistory.getInstance().lowerBlock(block);
			}
			
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
