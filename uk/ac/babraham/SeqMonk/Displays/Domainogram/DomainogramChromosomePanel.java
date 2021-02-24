/**
 * Copyright 2013- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Domainogram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;

public class DomainogramChromosomePanel extends JPanel {

	private Probe [] probes;
	private DataStore data;
	private int [] levels;
	private ColourGradient gradient;
	private float min;
	private float max;
	private int chrStart;
	private int longestChr;
	
	public DomainogramChromosomePanel (Probe [] probes, DataStore data, int [] levels, ColourGradient gradient, float min, float max, int longestChr,boolean trimEnds) {
		this.probes = probes;
		this.data = data;
		this.levels = levels;
		this.gradient = gradient;
		this.min = min;
		this.max = max;
		if (trimEnds && probes.length > 0) {
			this.chrStart = probes[0].start();
			this.longestChr = probes[0].end();
			
			for (int p=0;p<probes.length;p++) {
				if (probes[p].end() > this.longestChr) {
					this.longestChr = probes[p].end();
				}
			}
			
//			System.err.println("Chr start"+chrStart+" Chr end="+this.longestChr);
		}
		else {
			this.chrStart = 1;
			this.longestChr = longestChr;
		}
	}
	
	public int getVisibleLength() {
		return longestChr-chrStart;
	}
	
	public void setGradient (ColourGradient gradient) {
		this.gradient = gradient;
		repaint();
	}
	
	public void setLimits (float min, float max) {
		this.min = min;
		this.max = max;
		repaint();
	}
	
	public Dimension getPreferredSize () {
		return (new Dimension(600,50));
	}
	
	private int getX (int position) {
		
		int x = (int)((getWidth() * ((double)(position-chrStart)))/(longestChr-chrStart));
		
//		System.err.println("Width="+getWidth()+" pos="+position+" longest="+longestChr+" x="+x);
		
		return x;
	}
	
	private int getY (int index) {
		return getHeight() - ((getHeight()*index)/levels.length);
	}
	
	public void paint (Graphics g) {
		
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		// Work out which y indices we can actually use
		Vector<Integer> usableYVector = new Vector<Integer>();
		usableYVector.add(0);
		for (int i=1;i<levels.length;i++) {
			
			int thisY = getY(i);
			int lastY = getY(i-1);
			
//			System.err.println("Index = "+i+" thisY="+levels[i]+"="+thisY+" lastY="+levels[i-1]+"="+lastY);
			
			if (thisY != lastY) {
				usableYVector.add(i);
//				System.err.println("Keeping");
			}
		}
		
		int [] usableYindices = new int[usableYVector.size()];
		for (int i=0;i<usableYindices.length;i++) {
			usableYindices[usableYindices.length-(i+1)] = usableYVector.elementAt(i);
		}
		
//		System.err.println("Using "+usableYindices.length+" y indices out of "+levels.length);
		
		
		int lastX = -1;
		
		for (int p=0;p<probes.length;p++) {
			int x = getX(probes[p].start());
						
					
			int endX = getX(probes[p].end());
			if (endX == x) {
				endX++;
			}
			
			if (endX <= lastX) {
//				System.err.println("Skipping "+x+" (lastX = "+lastX+")");
				continue;
			}


			int lastY = 0;

			YINDEX: for (int usableIndex = 0; usableIndex<usableYindices.length;usableIndex++) {
				
				int index = usableYindices[usableIndex];

				int y=getY(index);
				
				int yHeight = y-lastY;
				
//				System.err.println("Index is "+index+" Last y is "+lastY+" y is "+y+" height="+yHeight);
				
				float value = 0;
				int validCount = 0;
				
				for (int pos = p-(levels[index]-1);pos <= p+(levels[index]-1);pos++) {
					if (pos < 0 || pos >= probes.length) {
						lastY=y;
						continue YINDEX;
					}
					
					try {
						float thisValue = data.getValueForProbe(probes[pos]);
						if (Float.isNaN(thisValue) || Float.isInfinite(thisValue)) continue;
						value += thisValue;
						++validCount;
					}
					catch (SeqMonkException e) {
						throw new IllegalStateException(e);
					}
				}
				
				value /= validCount;
				
				if (validCount == 0) continue; // Don't draw anything if we can't calculate a value.
				
				g.setColor(gradient.getColor(value, min, max));
				
				g.fillRect(x, lastY, (endX-x)+1, yHeight);
				
				lastY = y;
				
			}
			
			
			lastX = endX;
			
		}
		
	}
	
}
