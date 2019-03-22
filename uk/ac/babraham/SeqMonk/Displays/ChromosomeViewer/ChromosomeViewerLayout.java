/**
 * Copyright Copyright 2018-19 Simon Andrews
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

package uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;

public class ChromosomeViewerLayout implements LayoutManager2 {

	public static Integer ANNOTATION_TRACK = 56426;
	public static Integer DATA_TRACK = 56427;
	public static Integer FIXED_HEIGHT = 56428;
	
	private Vector<Component> annotationTracks = new Vector<Component>();
	private Vector<Component> dataTracks = new Vector<Component>();
	private Vector<Component> fixedTracks = new Vector<Component>();
	
	private static int PREFERRED_ANNOT_HEIGHT = 30;
	private static int MIN_ANNOT_HEIGHT = 15;
	private static int MIN_DATA_HEIGHT = 5;

		
	@Override
	public void addLayoutComponent(String arg0, Component arg1) {}

	@Override
	public void layoutContainer(Container c) {

//		System.err.println("Layout called");
		/*
		 * The process will be
		 * 
		 * 1. Find the total height available
		 * 2. Find the amount of fixed space needed
		 * 3. Find the size for well spaced annotation tracks
		 * 4. See if we have enough space left to draw all data tracks
		 * 5. If not compact the annotation tracks and try again
		 * 6. If still not then re-expand the annotation tracks and compact data tracks until stuff fits
		 */

		int width = c.getWidth();
		int height = c.getHeight();
		
		int totalAvailableHeight = c.getHeight();
		int fixedHeight = 0;
		for (int i=0;i<fixedTracks.size();i++) {
			fixedHeight += fixedTracks.get(i).getPreferredSize().height;
		}
		
		// If there are no data tracks then we just expand the annotation tracks to fit
		// (if there are any
		
		if (dataTracks.size() == 0) {
			if (annotationTracks.size() == 0) {
				// It doesn't matter what we do - we're only drawing the fixed bit
				setLayout(0,width,height);
			}
			else {
				setLayout((totalAvailableHeight-fixedHeight)/annotationTracks.size(),width,height);
			}
			
			return;
		}
		
		
		int annotationHeight = PREFERRED_ANNOT_HEIGHT*annotationTracks.size();
		
		// Do we have enough space like this?
		if ((totalAvailableHeight - (fixedHeight+annotationHeight))/dataTracks.size() >= 30) {
			
			// We're all good
			setLayout(PREFERRED_ANNOT_HEIGHT,width,height);
			return;
		}
		
		// OK, so we don't have enough space.  Do we have enough with compacted annotation?
		annotationHeight = MIN_ANNOT_HEIGHT*annotationTracks.size();

		// Do we have enough space like this?
		if ((totalAvailableHeight - (fixedHeight+annotationHeight))/dataTracks.size() >= 30) {
			
			// We're all good
			setLayout(MIN_ANNOT_HEIGHT,width,height);
			return;
		}
		
		// No, we're still out.  Is this because annotation takes up more than half the space
		// available?  If so then it gets compacted, if not then it gets expanded
		
		annotationHeight = PREFERRED_ANNOT_HEIGHT*annotationTracks.size();
		
		if (totalAvailableHeight > (annotationHeight*2)) {
			setLayout(PREFERRED_ANNOT_HEIGHT,width,height);
		}
		else {
			setLayout(MIN_ANNOT_HEIGHT,width,height);
		}
		
	}
	
	private void setLayout (int annotationHeight, int width, int height) {
		
		// Annotation tracks first
		
		int yStart = 0;
		
		for (int i=0;i<annotationTracks.size();i++) {
			Component c = annotationTracks.elementAt(i);
			c.setLocation(0, yStart);
			c.setBounds(0, yStart, width, annotationHeight);
			yStart += annotationHeight;
		}
		
		int yBottom = height;
		// Fixed tracks now
		for (int i=fixedTracks.size()-1;i>=0;i--) {
			Component c = fixedTracks.elementAt(i);
			c.setLocation(0, yBottom - c.getPreferredSize().height);
			c.setBounds(0, yBottom-c.getPreferredSize().height, width, c.getPreferredSize().height);
			yBottom -= c.getPreferredSize().height;
		}
		
		// Now the data tracks need to fit in between yStart and yBottom
		
		int dataYStart = yStart;
		int lastYEnd = dataYStart;

		// Work out the overall data height to see if we need to specially accommodate selected 
		// tracks
		
		double dataHeight = (yBottom-yStart) / (double)dataTracks.size();
		
		int selectedStore = -1;
		int selectedHeight = (int)Math.ceil(dataHeight);
		
		if (dataHeight < 50) {
			// We need to accommodate a selected track to make sure that it is visible, if
			// there is one.
			
			for (int i=0;i<dataTracks.size();i++) {
				if (((ChromosomeDataTrack)dataTracks.get(i)).getStore() == SeqMonkApplication.getInstance().dataCollection().getActiveDataStore()) {
					selectedStore = i;
					break;
				}
			}
			
			if (selectedStore >=0) {
				// We reserve 100px for this (or how ever much space we actually have)
				
				selectedHeight = Math.min(100, (yBottom-yStart));
				
				if (dataTracks.size()>1) {
					dataHeight = (yBottom-(yStart+selectedHeight)) / (double)(dataTracks.size()-1);
				}
			}
			
		}
		
		for (int i=0;i<dataTracks.size();i++) {
			Component c = dataTracks.elementAt(i);
			
			if (i == selectedStore) {
				// We have to draw this with a preferred height
				int yEnd = lastYEnd+selectedHeight;
				c.setLocation(0, lastYEnd);
				c.setBounds(0, lastYEnd, width, (yEnd-lastYEnd));
				lastYEnd = yEnd;
				continue;
				
			}
			
			int yEnd = (int)Math.round(dataYStart+(dataHeight*(i+1)));
			if (selectedStore >=0 && i>selectedStore) {
				yEnd+= (int)(selectedHeight-dataHeight);
			}
			if (yEnd - lastYEnd < MIN_DATA_HEIGHT) {
				// We make this zero sized.
				c.setBounds(0, 0, 0, 0);
			}
			else {
				c.setLocation(0, lastYEnd);
				c.setBounds(0, lastYEnd, width, (yEnd-lastYEnd));
//				System.err.println("Drawing reads "+i+" from "+lastYEnd+" with height "+((yEnd-lastYEnd)+1));
				lastYEnd = yEnd;
			}
		}
		
	}
	

	@Override
	public Dimension minimumLayoutSize(Container c) {
		/*
		 * Our width can be anything, but our min height
		 * will be the fixed heights plus the smallest height
		 * for an annotation track, plus at least one data 
		 * track
		 */
		
		int height = MIN_DATA_HEIGHT;
		height += MIN_ANNOT_HEIGHT+annotationTracks.size();
		for (int i=0;i<fixedTracks.size();i++) {
			height += fixedTracks.get(i).getPreferredSize().height;
		}
		
		return new Dimension(0, height);
	}

	@Override
	public Dimension preferredLayoutSize(Container c) {
		return(c.getSize());
	}

	@Override
	public void removeLayoutComponent(Component c) {
		if (annotationTracks.contains(c)) annotationTracks.remove(c);
		if (dataTracks.contains(c))dataTracks.remove(c);
		if (fixedTracks.contains(c))fixedTracks.remove(c);
	}

	@Override
	public void addLayoutComponent(Component c, Object type) {
		if (type.equals(ANNOTATION_TRACK)) annotationTracks.add(c);
		else if (type.equals(DATA_TRACK)) dataTracks.add(c);
		else if (type.equals(FIXED_HEIGHT)) fixedTracks.add(c);
		else {
			throw new IllegalStateException("Unknown track type "+type);
		}
	}

	@Override
	public float getLayoutAlignmentX(Container c) {
		return 0;
	}

	@Override
	public float getLayoutAlignmentY(Container arg0) {
		return 0;
	}

	@Override
	public void invalidateLayout(Container c) {
//		System.err.println("Invalidate called");
		layoutContainer(c);
	}

	@Override
	public Dimension maximumLayoutSize(Container c) {
		return c.getSize();
	}

}
