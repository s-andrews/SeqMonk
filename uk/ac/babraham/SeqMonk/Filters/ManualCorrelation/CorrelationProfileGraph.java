/**
 * Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Filters.ManualCorrelation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class CorrelationProfileGraph extends JPanel implements MouseListener,MouseMotionListener {

	private DataStoreDrawnPosition [] drawnStores;
	private int changingIndex = -1;
	private double [] profile;
	
	private static final Color BLUE = new Color(0,0,200);
	private static final Color GREEN = new Color(0,200,0);

	public CorrelationProfileGraph (DataStore [] stores) {
		if (stores == null) {
			stores = new DataStore[0];
		}
		setStores(stores);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void setStores (DataStore [] stores) {
		if (drawnStores != null && drawnStores.length == stores.length) {
			// Check if this is different to the existing stores

			boolean different = false;

			for (int i=0;i<stores.length;i++) {
				if (stores[i] != drawnStores[i].store) {
					different = true;
					break;
				}
			}

			if (! different) return; // Nothing to do

		}

		// Update the stores
		profile = new double[stores.length];
		drawnStores = new DataStoreDrawnPosition[stores.length];
		
		for (int i=0;i<profile.length;i++) {
			profile[i] = 0.5d;
			drawnStores[i] = new DataStoreDrawnPosition(stores[i], 0, 0);
			
		}

		repaint();
	}
	
	public double [] profile () {
		return profile;
	}

	public void paint (Graphics g) {
		super.paint(g);

		g.setColor(Color.WHITE);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.BLACK);

		if (drawnStores.length < 3) {
			g.drawString("Too few DataStores", getWidth()/2 - (g.getFontMetrics().stringWidth("Too few DataStores")/2), getHeight()/2);
			return;
		}
		
		// Draw the axes
		g.drawLine(10, getHeight()-20, getWidth()-10, getHeight()-20); // X-axis
		g.drawLine(10, getHeight()-20, 10, 10); // Y-axis

		// Draw x ticks
		for (int s=0;s<drawnStores.length;s++) {
			int xTick = 10 + (s*((getWidth()-20)/(drawnStores.length-1)));
			g.drawLine(xTick, getHeight()-17, xTick, getHeight()-20);
		}

		for (int s=0;s<drawnStores.length;s++) {
			int xTick = 10 + (s*((getWidth()-20)/(drawnStores.length-1)));

			int stringWidth = g.getFontMetrics().stringWidth(drawnStores[s].store.name());
			int xStart = xTick - (stringWidth/2);

			if (xStart < 1) xStart = 1;
			if (xStart + stringWidth > (getWidth()-1)) xStart = (getWidth()-1)-stringWidth;

			g.drawString(drawnStores[s].store.name(), xStart, getHeight()-2);
		}

		// Now draw the probes

		int lastX = 0;
		int lastY = 0;
		int thisX = 0;
		int thisY = 0;

		for (int s=0;s<drawnStores.length;s++) {
			thisX = 10 + (s*((getWidth()-20)/(drawnStores.length-1)));
			thisY = getYPixels(profile[s]);

			drawnStores[s].x = thisX;
			drawnStores[s].y = thisY;
			
			if (s>0) {
				g.setColor(BLUE);
				g.drawLine(lastX, lastY, thisX, thisY);
			}
			
			g.setColor(GREEN);
			g.drawRect(thisX-5, thisY-5, 10, 10);
			
			lastX = thisX;
			lastY = thisY;
		}

	}

	public int getYPixels (double value) {
		return (getHeight()-20) - (int)((getHeight()-30d)*value);
	}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		for (int i=0;i<drawnStores.length;i++) {
			if (drawnStores[i].isClose(e.getX(), e.getY())) {
				changingIndex = i;
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		changingIndex = -1;
	}

	public void mouseDragged(MouseEvent me) {
		if (changingIndex >= 0) {

			double newValue = (me.getY()-10d) / (getHeight()-30d);
			newValue = 1-newValue;
			
			// Put some limits on this
			if (newValue < 0) newValue = 0;
			if (newValue > 1) newValue = 1;
			
			profile[changingIndex] = newValue;
			repaint();
		}
	}

	public void mouseMoved(MouseEvent arg0) {}
	
	private class DataStoreDrawnPosition {
		
		private int x;
		private int y;
		private DataStore store;
		
		public DataStoreDrawnPosition (DataStore store,int x, int y) {
			this.store = store;
			this.x=x;
			this.y=y;
		}
		
		public boolean isClose (int x,int y) {
			double dist = Math.sqrt(Math.pow(this.x-x, 2)+ Math.pow(this.y-y,2));
			return dist < 5;
		}
		
	}

}
