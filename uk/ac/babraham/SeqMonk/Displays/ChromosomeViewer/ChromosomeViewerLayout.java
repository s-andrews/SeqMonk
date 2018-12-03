package uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.Vector;

public class ChromosomeViewerLayout implements LayoutManager2 {

	public static Integer ANNOTATION_TRACK = 56426;
	public static Integer DATA_TRACK = 56427;
	public static Integer FIXED_HEIGHT = 56428;
	
	private Vector<Component> annotationTracks = new Vector<Component>();
	private Vector<Component> dataTracks = new Vector<Component>();
	private Vector<Component> fixedTracks = new Vector<Component>();
	
	private static int PREFERRED_ANNOT_HEIGHT = 30;
	private static int MIN_ANNOT_HEIGHT = 15;
	private static int MIN_DATA_HEIGHT = 10;

		
	@Override
	public void addLayoutComponent(String arg0, Component arg1) {}

	@Override
	public void layoutContainer(Container c) {

		System.err.println("Layout called");
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
		
		// OK, so we don't have enough space.
		setLayout(MIN_ANNOT_HEIGHT,width,height);
		
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
		
		double dataHeight = (yBottom-yStart) / (double)dataTracks.size();
		
		for (int i=0;i<dataTracks.size();i++) {
			Component c = dataTracks.elementAt(i);
			
			int yEnd = (int)Math.round(dataYStart+(dataHeight*(i+1)));
			if (yEnd - lastYEnd < MIN_DATA_HEIGHT) {
				// We make this zero sized.
				c.setBounds(0, 0, 0, 0);
			}
			else {
				c.setLocation(0, lastYEnd);
				c.setBounds(0, lastYEnd, width, yEnd-lastYEnd);
				System.err.println("Drawing reads "+i+" from "+lastYEnd+" with height "+(yEnd-lastYEnd));
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
		System.err.println("Invalidate called");
		layoutContainer(c);
	}

	@Override
	public Dimension maximumLayoutSize(Container c) {
		return c.getSize();
	}

}