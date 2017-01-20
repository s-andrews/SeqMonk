/**
 * Copyright 2014-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.RNASeqQCPlot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;

public class PercentileStripChart extends JPanel implements MouseListener, MouseMotionListener {

	private String title;
	private double [] values;
	private DataStore [] stores;
	private static final int TOP_GAP = 20;
	private static final int BOTTOM_GAP = 10;
	private static final int RIGHT_GAP = 10;
	
	private ArrayList<SampleSelectionListener> listeners = new ArrayList<SampleSelectionListener>();
		
	private int mouseYStartDrag = -1;

	public DataStore [] selectedStores = new DataStore[0];
	
	public PercentileStripChart (String title, double [] values, DataStore [] stores) {
		this.title = title;
		this.values = values;
		this.stores = stores;
	}
	
	public void setSelectedStores (DataStore [] stores) {
		this.selectedStores = stores;
		repaint();
	}
	
	public void addSampleSelectionListener (SampleSelectionListener l) {
		if (l != null && !listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeSampleSelectionListener (SampleSelectionListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	
	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		int titleWidth = g.getFontMetrics().stringWidth(title);
		
		int titleX = (getWidth()/2) - (titleWidth/2);
		if (titleX < 2) titleX = 2;
		
		g.setColor(Color.BLACK);
		g.drawString(title, titleX, 15);
		
		int xGap = g.getFontMetrics().stringWidth("100%")+10;
		
		g.drawLine(xGap, TOP_GAP, xGap, getHeight()-BOTTOM_GAP);
		
		g.drawLine(xGap, getY(0), xGap-4, getY(0));

		String percentString = "0%";
		int percentWidth = g.getFontMetrics().stringWidth(percentString);
		g.drawString(percentString, xGap-(7+percentWidth), getY(0)+5);
		
		percentString = "25%";
		percentWidth = g.getFontMetrics().stringWidth(percentString);
		g.drawString(percentString, xGap-(7+percentWidth), getY(25)+5);

		percentString = "50%";
		percentWidth = g.getFontMetrics().stringWidth(percentString);
		g.drawString(percentString, xGap-(7+percentWidth), getY(50)+5);

		percentString = "75%";
		percentWidth = g.getFontMetrics().stringWidth(percentString);
		g.drawString(percentString, xGap-(7+percentWidth), getY(75)+5);

		percentString = "100%";
		percentWidth = g.getFontMetrics().stringWidth(percentString);
		g.drawString(percentString, xGap-(7+percentWidth), getY(100)+5);

		
		for (int i=0;i<values.length;i++) {
			g.setColor(ColourIndexSet.getColour(i));
			
			int valueXPosition = xGap + 5 + (int)(((getWidth()-(xGap+RIGHT_GAP+10))/(double)values.length)*i);
					
			g.fillOval(valueXPosition, getY(values[i])-5, 10, 10);
			
			// See if this is selected
			for (int s=0;s<selectedStores.length;s++) {
				if (selectedStores[s].equals(stores[i])) {
					g.setColor(Color.BLACK);
					g.drawOval(valueXPosition, getY(values[i])-5, 10, 10);
					g.drawOval(valueXPosition+1, getY(values[i])-4, 8, 8);
				}
			}
				
		}
				
		addMouseListener(this);
		addMouseMotionListener(this);
		
	}
	
	private int getY (double percent) {
		
		int dist = getHeight()- (TOP_GAP+BOTTOM_GAP);
		
		return getHeight()-(((int)(dist*percent)/100)+BOTTOM_GAP);
		
	}
	
	private double getValueForY (int pos) {

		double dist = getHeight()- (TOP_GAP+BOTTOM_GAP);

		double proportion = (pos-TOP_GAP)/dist;
		
		proportion = 100 - (proportion*100);
		
		if (proportion > 100) proportion = 100;
		if (proportion < 0) proportion = 0;
		
		return(proportion);
	}

	public void mouseDragged(MouseEvent e) {
		if (mouseYStartDrag == -1) {
			mouseYStartDrag = e.getY();
		}
		else {
			
			double minY;
			double maxY;
			
			if (e.getY()>=mouseYStartDrag) {
				minY = getValueForY(e.getY());
				maxY = getValueForY(mouseYStartDrag);
			}
			else {
				maxY = getValueForY(e.getY());
				minY = getValueForY(mouseYStartDrag);
			}
			
			ArrayList<DataStore> draggedStores = new ArrayList<DataStore>();
			for (int i=0;i<stores.length;i++) {
				if (values[i]>=minY && values[i]<=maxY) {
					draggedStores.add(stores[i]);
				}
			}
			
			for (SampleSelectionListener l : listeners) {
				l.dataStoresSelected(draggedStores.toArray(new DataStore[0]));
			}
		}
	}

	public void mouseMoved(MouseEvent e) {}

	public void mouseClicked(MouseEvent me) {}

	public void mouseEntered(MouseEvent arg0) {}

	public void mouseExited(MouseEvent arg0) {
		mouseYStartDrag = -1;
	}

	public void mousePressed(MouseEvent arg0) {}

	public void mouseReleased(MouseEvent arg0) {
		mouseYStartDrag = -1;
	}
	
}
