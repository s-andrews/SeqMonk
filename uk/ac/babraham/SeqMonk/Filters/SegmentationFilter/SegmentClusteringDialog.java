/**
 * Copyright Copyright 2018- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Filters.SegmentationFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * This class takes in an array of segments and plots out a graph
 * of the mean values.  It then allows you to place dividers on 
 * the graph to divide the segments into groups which can then
 * be used to split all of the segments into subgroups afterwards
 * 
 * @author andrewss
 *
 */

public class SegmentClusteringDialog extends JDialog {
	
	private ClusteredSegment [] segments;
	private HashSet<DrawnBoundary> boundaries = new HashSet<DrawnBoundary>();
	
	private BoundaryGraph boundaryGraph = new BoundaryGraph();
	
	private float minValue;
	private float maxValue;
	

	public SegmentClusteringDialog (ClusteredSegment [] segments) {
		super(SeqMonkApplication.getInstance(),"Cluster Segments");
		this.segments = segments;
		
		for (int s=0;s<segments.length;s++) {
			if (s==0) {
				minValue = segments[s].mean;
				maxValue = segments[s].mean;
			}
			if (segments[s].mean > maxValue) maxValue = segments[s].mean;
			if (segments[s].mean < minValue) minValue = segments[s].mean;
		}
		
		// To give us some space at the top/bottom we add 5% to both
		float fivePercent = (maxValue-minValue)/20;
		
		minValue -= fivePercent;
		maxValue += fivePercent;
		
		boundaries.add(new DrawnBoundary(minValue + ((maxValue-minValue)/2)));
		
		getContentPane().setLayout(new BorderLayout());
		
		JPanel buttonPanel = new JPanel();
		
		JButton boundaryButton = new JButton("Add Boundary");
		boundaryButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				boundaries.add(new DrawnBoundary(minValue + ((maxValue-minValue)/2)));
				repaint();
			}
		});
		
		buttonPanel.add(boundaryButton);
		
		JButton clusterButton = new JButton("Cluster");
		clusterButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		
		buttonPanel.add(clusterButton);
		
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		
		getContentPane().add(boundaryGraph, BorderLayout.CENTER);
		
		
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setModal(true);
		setSize(700,500);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}
	
	public float [] getBoundaryValues () {

		float [] boundaryPoints = new float[boundaries.size()];
		Iterator<DrawnBoundary> it = boundaries.iterator();
		
		int index = 0;
		while (it.hasNext()) {
			boundaryPoints[index] = it.next().value;
			index++;
		}
		
		Arrays.sort(boundaryPoints);

		return boundaryPoints;
	}
	
	public ClusteredSegment [][] getClusteredSegments () {
		
		float [] boundaryPoints = getBoundaryValues();
			
		List<List<ClusteredSegment>> buildingSegmentClusters = new ArrayList<List<ClusteredSegment>>();
		
		for (int i=0;i<=boundaryPoints.length;i++) {
			buildingSegmentClusters.add(i, new ArrayList<ClusteredSegment>());
		}
		
		// Now we go through the clustered segments assigning them to the appropriate list
		
		// We go through the boundary points assigning as soon as we have a mean which 
		// is below that point
		
		SEGMENT: for (int s=0;s<segments.length;s++) {
			for (int p=0;p<boundaryPoints.length;p++) {
				if (segments[s].mean <= boundaryPoints[p]) {
					buildingSegmentClusters.get(p).add(segments[s]);
					continue SEGMENT;
				}				
			}
			buildingSegmentClusters.get(boundaryPoints.length).add(segments[s]);
		}
		
		// Turn the final set into an array, ignoring any empty bins
		
		int validBins = 0;
		
		for (int i=0;i<buildingSegmentClusters.size();i++) {
			if (buildingSegmentClusters.get(i).size() > 0) validBins++;
		}
		
		// Make the results array
		ClusteredSegment [][] returnSegments = new ClusteredSegment[validBins][];
		
		validBins = 0;
		for (int i=0;i<buildingSegmentClusters.size();i++) {
			if (buildingSegmentClusters.get(i).size() == 0) continue;
			
			returnSegments[validBins] = new ClusteredSegment[buildingSegmentClusters.get(i).size()];
			
			for (int j=0;j<buildingSegmentClusters.get(i).size();j++) {
				returnSegments[validBins][j] = buildingSegmentClusters.get(i).get(j);
			}
			validBins++;
		}
		
		return returnSegments;
		
	}
	

	private class DrawnBoundary {
		
		private float value;
		private int lastY = 0;
		
		public DrawnBoundary (float value) {
			this.value = value;
		}
		
	}
	
	
	private class BoundaryGraph extends JPanel implements MouseListener, MouseMotionListener{
		
		private DrawnBoundary selectedBoundary = null;
		
		public void paint (Graphics g) {
			super.paint(g);
			
			addMouseListener(this);
			addMouseMotionListener(this);
			
			g.setColor(Color.WHITE);
			g.fillRect(0,0, getWidth(), getHeight());
			g.setColor(Color.BLACK);
			
			AxisScale as = new AxisScale(minValue, maxValue);
			
			int xOffset = as.getXSpaceNeeded()+5;
			
			g.drawLine(xOffset, 0, xOffset, getHeight());
			
			// Draw the axis on the left
			for (double value=as.getStartingValue();value <= as.getMax(); value+=as.getInterval()) {
				String text = as.format(value);
				g.drawLine(xOffset-2, getY(value), xOffset, getY(value));
				
				g.drawString(text, xOffset-(3+g.getFontMetrics().stringWidth(text)),getY(value));
			}
			
			// Draw the mean values
			
			g.setColor(Color.GRAY);
			for (int s=0;s<segments.length;s++) {
				
				int x = xOffset + (int)((getWidth()-(5+xOffset))*(s/(double)(segments.length-1)));
				
				g.drawOval(x,getY(segments[s].mean)-2, 4, 4);
			}
			
			Iterator<DrawnBoundary> b = boundaries.iterator();
			while (b.hasNext()) {
				
				DrawnBoundary boundary = b.next();
				
				if (selectedBoundary != null && selectedBoundary == boundary) continue;
				
				int y = getY(boundary.value);
				boundary.lastY = y;
				
				g.setColor(ColourScheme.REVERSE_FEATURE);
				g.drawLine(xOffset, y, getWidth(), y);
				
				// We'll draw a little delete box on the right so they can remove it
				// if they need to.
				
				g.setColor(Color.WHITE);
				g.fillRect(getWidth()-14, y-6, 12, 12);
				g.setColor(ColourScheme.REVERSE_FEATURE);
				g.drawRect(getWidth()-14, y-6, 12, 12);
				
				g.setColor(ColourScheme.FORWARD_FEATURE);
				g.drawLine(getWidth()-12, y-4, getWidth()-4, y+4);
				g.drawLine(getWidth()-12, y+4, getWidth()-4, y-4);
				
				
			}
			
			if (selectedBoundary != null) {
				g.setColor(ColourScheme.FORWARD_FEATURE);
				int y = getY(selectedBoundary.value);
				selectedBoundary.lastY = y;
				g.drawLine(xOffset,y,getWidth(),y);
			}
			
			
		}
		
		
		public int getY (double value) {
			double proportion = (value-minValue) / (maxValue-minValue);
			
			return getHeight()-(int) (getHeight()*proportion);
		}


		@Override
		public void mouseDragged(MouseEvent me) {
			if (selectedBoundary != null) {
				int height = getHeight()-me.getY();
				
				float proportion = height/(float)getHeight();
				
				float value = minValue + ((maxValue-minValue)*proportion);
				
				selectedBoundary.value = value;
				selectedBoundary.lastY = me.getY();
				repaint();
			}

		}


		@Override
		public void mouseMoved(MouseEvent arg0) {}


		@Override
		public void mouseClicked(MouseEvent me) {

			// See if they're trying to remove a boundary
			if (me.getX() < getWidth()-14) return;
			
			// Find the closest boundary
			DrawnBoundary closestBoundary = null;
			int distance = getHeight();
			
			Iterator<DrawnBoundary> it = boundaries.iterator();
			
			while (it.hasNext()) {
				DrawnBoundary b = it.next();
				int thisDistance = Math.abs(me.getY()-b.lastY);
				if (thisDistance < distance) {
					closestBoundary = b;
					distance = thisDistance;
				}
								
			}
			if (distance <= 3) {
				boundaries.remove(closestBoundary);
				repaint();
			}


		}


		@Override
		public void mouseEntered(MouseEvent e) {}


		@Override
		public void mouseExited(MouseEvent e) {}


		@Override
		public void mousePressed(MouseEvent me) {
			
			// Ignore if we're further right than width-14 as they'd
			// be clicking on a close button.
			if (me.getX() > getWidth()-14) return;
			
			// Find the closest boundary
			DrawnBoundary closestBoundary = null;
			int distance = getHeight();
			
			Iterator<DrawnBoundary> it = boundaries.iterator();
			
			while (it.hasNext()) {
				DrawnBoundary b = it.next();
				int thisDistance = Math.abs(me.getY()-b.lastY);
				if (thisDistance < distance) {
					closestBoundary = b;
					distance = thisDistance;
				}
								
			}
			if (distance <= 3) {
				selectedBoundary = closestBoundary;
				repaint();
			}
			
		}


		@Override
		public void mouseReleased(MouseEvent e) {
			selectedBoundary = null;
			repaint();
		}
		
	}
	
	
}
