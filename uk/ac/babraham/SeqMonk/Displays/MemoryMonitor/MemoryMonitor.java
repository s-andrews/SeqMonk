/**
 * Copyright 2009-15-13 Simon Andrews
 *
 *    This file is part of SeqMonk.
 *
 *    SeqMonk is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SeqMonk; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.SeqMonk.Displays.MemoryMonitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.CacheListener;

/**
 * The Class MemoryMonitor provides a display which summarises the current
 * memory usage and cache state.
 */
public class MemoryMonitor extends JPanel implements Runnable, MouseListener, MouseMotionListener, CacheListener {

	/** The Constant DARK_GREEN. */
	private static final Color DARK_GREEN = new Color(0,180,0);
	
	/** The Constant DARK_ORANGE. */
	private static final Color DARK_ORANGE = new Color(255,130,0);
	
	/** The Constant DARK_RED. */
	private static final Color DARK_RED = new Color(180,0,0);
	
	/** The monitor tool tip. */
	private String monitorToolTip;
	
	/** The cache tool tip. */
	private String cacheToolTip;
	
	/** The shown warning. */
	public boolean shownWarning = false;
	
	/** The need to show warning. */
	public boolean needToShowWarning = false;
	
	/** Whether the cache was active since the last update */
	private boolean cacheActive = false;
	
	/** Horrible hack to work around an initialisation order problem when loading */
	private boolean registered = false;
	
	/**
	 * Instantiates a new memory monitor.
	 */
	public MemoryMonitor () {
		addMouseListener(this);
		addMouseMotionListener(this);
		Thread t = new Thread(this);
		t.start();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		while (true) {
			
			if (!registered) {
				if (SeqMonkApplication.getInstance() != null) {
					SeqMonkApplication.getInstance().addCacheListener(this);
					registered = true;
				}
			}
			
			try {
				Thread.sleep(1000);
			} 
			catch (InterruptedException e) {}
			if (needToShowWarning && !shownWarning) showMemoryWarning();
			repaint();
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#getPreferredSize()
	 */
	public Dimension getPreferredSize () {
		return new Dimension(100,0);
	}
	
	/**
	 * Show memory warning.
	 */
	synchronized private void showMemoryWarning (){
		if (shownWarning) return;
		shownWarning = true;

		JOptionPane.showMessageDialog(null, "You are running short of available memory.\n Please look at Help > Contents > Configuration to see what you can do about this.", "Low Memory Warning", JOptionPane.WARNING_MESSAGE);

	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent (Graphics g) {
		
		super.paintComponent(g);
		
		paintDiskCache(g);
		paintMemoryMonitor(g);
		
	}
	
	/**
	 * Paint disk cache.
	 * 
	 * @param g the g
	 */
	private void paintDiskCache (Graphics g) {
		// The disk cache is drawn on the right 1/4 of the display

		int xStart = (getWidth()*3)/4;
		int width = getWidth()-xStart;


		// We set the background depending on whether we're using
		// caching or not.
		Color usedColor = DARK_RED;
		if (cacheActive) {
			usedColor = DARK_ORANGE;
			cacheActive = false;
		}
		else {
			usedColor = DARK_GREEN;
		}
		cacheToolTip = "Disk Cache Active";
		
		
		// We have a common disk image over which we overlay
		// some other image to say whether we're using this or
		// not.
		
				
		// Bottom circle first (outlined)		
		g.setColor(usedColor);
		g.fillOval(xStart+2, getHeight()-6, width-4, 4);
		g.setColor(Color.BLACK);
		g.drawOval(xStart+2, getHeight()-6, width-4, 4);
		
		// Then the main body
		g.setColor(usedColor);
		g.fillRect(xStart+2, 5, width-4, getHeight()-9);
		g.setColor(Color.BLACK);
		g.drawLine(xStart+2, 5, xStart+2, getHeight()-5);
		g.drawLine(xStart+(width-2),5,xStart+(width-2),getHeight()-5);
				
		// Then the top circle
		
		g.setColor(usedColor);			
		g.fillOval(xStart+2, 2, width-4, 4);
		g.setColor(Color.BLACK);
		g.drawOval(xStart+2, 2, width-4, 4);
						
	}
	
	/**
	 * Paint memory monitor.
	 * 
	 * @param g the g
	 */
	private void paintMemoryMonitor (Graphics g) {
		
		// The memory monitor is drawn in the right 3/4 of the display
		
		int xStart = 0;
		int xWidth = (getWidth()*3)/4;
		
		long max = Runtime.getRuntime().maxMemory();
		long allocated = Runtime.getRuntime().totalMemory();
		long used = allocated - Runtime.getRuntime().freeMemory();

		// Base colour is green for total available memory
		g.setColor(DARK_GREEN);
		g.fillRect(xStart, 0, xWidth, getHeight());
		
		// Orange is for allocated memory
		int allocatedWidth = (int)(xWidth * ((double)allocated/max));
		g.setColor(DARK_ORANGE);
		g.fillRect(xStart, 0, allocatedWidth, getHeight());
		
		// Red is for used memory
		int usedWidth = (int)(xWidth * ((double)used/max));
		g.setColor(DARK_RED);
		g.fillRect(xStart, 0, usedWidth, getHeight());
		
		int usedPercentage = (int)(100 * ((double)used/max));
		g.setColor(Color.WHITE);
		g.drawString(usedPercentage+"%",xStart+(xWidth/2)-10,getHeight()-3);
		
		
		
		monitorToolTip = "Memory Usage "+(used/(1024*1024))+"MB "+usedPercentage+"% of "+(max/(1024*1024))+"MB";
		
		if (usedPercentage > 90 && ! shownWarning) {
			needToShowWarning = true;
		}

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			if (e.getX() < (getWidth()*3)/4) {
				Runtime.getRuntime().gc();
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {
		if (e.getX() < (getWidth()*3)/4) {
			setToolTipText(monitorToolTip);
		}
		else {
			setToolTipText(cacheToolTip);
		}
	}

	public void cacheUsed() {
		cacheActive = true;
	}

	
	
}
