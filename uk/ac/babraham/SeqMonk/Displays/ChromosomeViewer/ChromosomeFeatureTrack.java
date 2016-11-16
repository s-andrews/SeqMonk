/**
 * Copyright Copyright 2010-15 Simon Andrews
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Displays.FeatureViewer.FeatureViewer;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.PositionFormat;

/**
 * The ChromosomeFeatureTrack is a display which shows one feature type
 * in the chromosome view.  It is usually only created and managed by a
 * surrounding instance of ChromsomeViewer.
 */
public class ChromosomeFeatureTrack extends JPanel {

	/** The chromosome viewer which contains this track **/
	private ChromosomeViewer viewer;
	
	/** The active feature. */
	private Feature activeFeature = null;
	
	/** The features shown in this track */
	private Feature [] features;
	
	/** The name of the feature type shown in this track */
	private String featureName;
		
	/** The current width of this window */
	private int width;
		
	/** The height of this track */
	private int height;
	
	/** An optimisation to allow as many labels as possible to be drawn */
	private int lastLabelEnd = 0;
			
	// This flag is a kludge to work around an antialiasing bug in openJDK which
	// causes refreshes to take forever.  Until they fix this we're going to
	// disable AA on openJDK installations.
	/** A flag to say if we're enabling anti-aliasing */
	private boolean useAntiAliasing = true;
	
	/** An optimisation to allow us to miss out features which would be drawn right on top of each other */
	private int lastXStart = 0;
	
	/** A list of drawn features, used for lookups when finding an active feature */
	private Vector<DrawnFeature> drawnFeatures = new Vector<DrawnFeature>();
	
	/**
	 * Instantiates a new chromosome feature track.  We have to send the name of the feature
	 * type explicitly in case there aren't any features of a given type on a chromosome and
	 * we couldn't then work out the name of the track from the features themselves.
	 * 
	 * @param viewer The chromosome viewer which holds this track
	 * @param type The name of the type of features we're going to show
	 * @param features A list of features we're going to show
	 */
	public ChromosomeFeatureTrack (ChromosomeViewer viewer, String type, Feature [] features) {
		this.viewer = viewer;
		this.featureName = type;
		this.features = features;
		addMouseMotionListener(new FeatureListener());
		addMouseListener(new FeatureListener());
		
		//TODO: Disable this as soon as the openJDK AA bug is fixed
		//This disables anti-aliasing on openJDK installations since
		//they currently have a bug which makes refreshes *really* slow.
		//
		// Since openJDK looks exactly the same as Sun's JDK from System.property
		// then we have to use a big hammer and disable AA under linux, which sucks.
		if (System.getProperty("os.name").toLowerCase().contains("linux")) {
			useAntiAliasing = false;
		}
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(1000, 30);
	}

	public Dimension getMinimumSize () {
		return new Dimension(1, 15);
	}

	
	public void updateFeatures (Feature [] features) {
		this.features = features;
		repaint();
	}
	
	public String type () {
		return featureName;
	}
		
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent (Graphics g) {

		super.paintComponent(g);
		if (useAntiAliasing && g instanceof Graphics2D) {
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		drawnFeatures = new Vector<DrawnFeature>();
		width = getWidth();
		height = getHeight();
		
		if (viewer.getIndex(this)%2 == 0) {
			g.setColor(ColourScheme.FEATURE_BACKGROUND_EVEN);
		}
		else {
			g.setColor(ColourScheme.FEATURE_BACKGROUND_ODD);
		}
		
		g.fillRect(0,0,width,height);
		
		if (viewer.makingSelection()) {
			int selStart = viewer.selectionStart();
			int selEnd = viewer.selectionEnd();
			int useStart = (selEnd > selStart) ? selStart : selEnd;
			int selWidth = selEnd-selStart;
			if (selWidth < 0) selWidth = 0-selWidth;
			g.setColor(ColourScheme.DRAGGED_SELECTION);
			g.fillRect(useStart,0,selWidth,height);
		}
				
		// Now go through all the features figuring out whether they
		// need to be displayed
		
		// Reset the optimisation tracker
		lastXStart = 0;
		
		int startBp = viewer.currentStart();
		int endBp = viewer.currentEnd();
		lastLabelEnd = 0;
		for (int i=0;i<features.length;i++) {
//			System.out.println("Looking at feature "+featureName+" Start: "+features[i].location().start()+" End: "+features[i].location().end()+" Start:"+startBp+" End:"+endBp);
			if (features[i].location().end() > startBp && features[i].location().start() < endBp) {
				// We always draw the active feature last so skip it here.
				if (features[i] != activeFeature)
					drawFeature(features[i],g);
			}
		}
		
		// Finally redraw the active feature so it always goes on top
		lastXStart = 0;
		if (activeFeature != null)
			drawFeature(activeFeature,g);
		
		// Draw a box into which we'll put the track name so it's not obscured
		// by the data
		int nameWidth = g.getFontMetrics().stringWidth(featureName);
		int nameHeight = g.getFontMetrics().getAscent();

		if (viewer.getIndex(this)%2 == 0) {
			g.setColor(ColourScheme.FEATURE_BACKGROUND_EVEN);
		}
		else {
			g.setColor(ColourScheme.FEATURE_BACKGROUND_ODD);
		}
				
		g.fillRect(0, getHeight()-(nameHeight+2), nameWidth+5, nameHeight+2);
		
		
		// Lastly draw the name of the track
		g.setColor(Color.GRAY);
		g.drawString(featureName,0+2,height-2);

	}
	
	
	/**
	 * Draws a single feature in the track
	 * 
	 * @param f the feature to draw
	 * @param g the graphics object to use for drawing
	 */
	private void drawFeature (Feature f, Graphics g) {
		
		if (f.location().strand() == Location.FORWARD) {
			g.setColor(ColourScheme.FORWARD_FEATURE);
		}
		else if (f.location().strand() == Location.REVERSE) {
			g.setColor(ColourScheme.REVERSE_FEATURE);
		}
		else {
			g.setColor(ColourScheme.UNKNOWN_FEATURE);
		}
		
		if (f == activeFeature) {
			g.setColor(ColourScheme.ACTIVE_FEATURE);
		}
		
		// If there's space we'll put a label on the track as
		// well as the feature.
		boolean drawLabel = false;
		int yBoxStart = 2;
		int yBoxEnd = height-2;
		int yText = 0;
		if (height >= 25) {
			drawLabel = true;
			yBoxStart = 2;
			yBoxEnd = height-14;
			yText = height-2;
		}
		
		int wholeXStart = bpToPixel(f.location().start());
		int wholeXEnd = bpToPixel(f.location().end());
				
		
		if (wholeXEnd - wholeXStart < 2) {
			if (wholeXStart-lastXStart < 4) {
				return; // Skip this feature.
			}
			wholeXStart = wholeXEnd - 2;
		}
				
		if (wholeXEnd-wholeXStart < 10 || ! (f.location() instanceof SplitLocation)) {
			// Don't bother drawing sublocations
			g.fillRect(wholeXStart,yBoxStart,(wholeXEnd-wholeXStart),yBoxEnd-yBoxStart);
			drawnFeatures.add(new DrawnFeature(wholeXStart, wholeXEnd, f));
		}

		else {
			// Draw out the individual sublocations if they exist.
			Location [] l = ((SplitLocation)f.location()).subLocations();
			int lastX = -1;
			for (int i=0;i<l.length;i++) {
				int xStart = bpToPixel(l[i].start());
				int xEnd = bpToPixel(l[i].end());
				
				// Make sure something shows up.
				if ((xEnd-xStart)<2) {
					xStart = xEnd -2;
				}
				
				drawnFeatures.add(new DrawnFeature(xStart, xEnd, f));
				
				// Draw an intron line if this isn't the first subLocation
				if (i>0) {
//					g.drawLine(lastX,yBoxStart+(yBoxEnd-yBoxStart)/2,xStart,yBoxStart+(yBoxEnd-yBoxStart)/2);
					if (f.location().strand() == Location.FORWARD) {
						// We're going forward
						g.drawLine(lastX,yBoxStart+(yBoxEnd-yBoxStart)/3,xStart,yBoxStart+(yBoxEnd-yBoxStart)/2);
						g.drawLine(lastX, yBoxEnd-(yBoxEnd-yBoxStart)/3, xStart, yBoxStart+(yBoxEnd-yBoxStart)/2);
					}
					else {
						// We're going backwards
						g.drawLine(xStart,yBoxStart+(yBoxEnd-yBoxStart)/3,lastX,yBoxStart+(yBoxEnd-yBoxStart)/2);
						g.drawLine(xStart, yBoxEnd-(yBoxEnd-yBoxStart)/3, lastX, yBoxStart+(yBoxEnd-yBoxStart)/2);
					}
				}
				
				// Draw a box around the feature
				g.fillRect(xStart,yBoxStart,(xEnd-xStart),yBoxEnd-yBoxStart);
				
				lastX = xEnd;
			}	
		}
		
		lastXStart = wholeXStart;
		
		if (drawLabel && (f==activeFeature || viewer.showAllLables())) {
			g.setColor(Color.DARK_GRAY);
			if (f != activeFeature) {
				// We only draw a label if there's enough space
				if (wholeXStart < lastLabelEnd+4) {
					return;
				}
			}
			g.drawString(f.type()+":"+f.name(),wholeXStart,yText);
			lastLabelEnd = wholeXStart+g.getFontMetrics().stringWidth(f.type()+":"+f.name());
		}
		
	}
	
	/**
	 * Pixel to bp.
	 * 
	 * @param x the x
	 * @return the int
	 */
	private int pixelToBp (int x) {
		int pos = viewer.currentStart()+(int)(((double)x/width)*(viewer.currentEnd()-viewer.currentStart()));
		if (pos < 1) pos = 1;
		if (pos > viewer.chromosome().length()) pos = viewer.chromosome().length();
		return pos;
	}
	
	/**
	 * Bp to pixel.
	 * 
	 * @param bp the bp
	 * @return the int
	 */
	private int bpToPixel (int bp) {
		return (int)(((double)(bp-viewer.currentStart())/((viewer.currentEnd()-viewer.currentStart())))*width);		
	}
	
	/**
	 * A container class which stores a feature and its last drawn position in the display.
	 * Split location features will use a separate DrawnFeature for each exon.
	 */
	private class DrawnFeature {
		
		/** The start. */
		public int start;
		
		/** The end. */
		public int end;
		
		/** The feature. */
		public Feature feature;
		
		/**
		 * Instantiates a new drawn feature.
		 * 
		 * @param start the start position in pixels
		 * @param end the end position in pixels
		 * @param feature the feature
		 */
		public DrawnFeature (int start, int end, Feature feature) {
			this.start = start;
			this.end = end;
			this.feature = feature;
		}
		
		/**
		 * Checks if a given pixel position is inside this feature.
		 * 
		 * @param x the x pixel position
		 * @return true, if this falls within the last drawn position of this feature
		 */
		public boolean isInFeature (int x) {
			if (x >=start && x <= end) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	/**
	 * The listener interface for receiving feature events.
	 * The class that is interested in processing a feature
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addFeatureListener<code> method. When
	 * the feature event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see FeatureEvent
	 */
	private class FeatureListener implements MouseMotionListener, MouseListener {

		/* (non-Javadoc)
		 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
		 */
		public void mouseDragged(MouseEvent me) {
			viewer.setSelectionEnd(me.getX());
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
		 */
		public void mouseMoved(MouseEvent me) {
			int x = me.getX();
			Enumeration<DrawnFeature> e = drawnFeatures.elements();
			while (e.hasMoreElements()) {
				DrawnFeature f = e.nextElement();
				if (f.isInFeature(x)) {
					if (activeFeature != f.feature) {
						int length = 1+(f.feature.location().end()-f.feature.location().start());
						viewer.application().setStatusText(" "+f.feature.type()+": "+f.feature.name()+" "+f.feature.location().start()+"-"+f.feature.location().end()+" ("+PositionFormat.formatLength(length)+")");
						activeFeature = f.feature;
						repaint();
						return;
					}
				}
			}
			setToolTipText(null);
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		public void mouseClicked(MouseEvent me) {
			if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
				viewer.zoomOut();
				return;
			}
			if (me.getClickCount() >= 2) {
				if (activeFeature != null) {
					new FeatureViewer(activeFeature);
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public void mousePressed(MouseEvent me) {
			viewer.setMakingSelection(true);
			viewer.setSelectionStart(me.getX());
			viewer.setSelectionEnd(me.getX());
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent me) {
			viewer.setMakingSelection(false);

			int width = viewer.selectionEnd() - viewer.selectionStart();
			if (width < 0) {
				width = 0-width;
			}
			
			// Stop people from accidentally making really short selections
			if (width < 5) return;
			
			int newStart = pixelToBp(viewer.selectionStart());
			int newEnd = pixelToBp(viewer.selectionEnd());
			
			if (newStart > newEnd) {
				int temp = newStart;
				newStart = newEnd;
				newEnd = temp;
			}
			
			if (newEnd-newStart < 5) return;
						
			DisplayPreferences.getInstance().setLocation(SequenceRead.packPosition(newStart,newEnd,Location.UNKNOWN));

		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		public void mouseEntered(MouseEvent arg0) {
			if (features.length>0)
				viewer.application().setStatusText(" "+features[0].type());
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		public void mouseExited(MouseEvent arg0) {
			activeFeature = null;
			repaint();
		}
		
	}

}
