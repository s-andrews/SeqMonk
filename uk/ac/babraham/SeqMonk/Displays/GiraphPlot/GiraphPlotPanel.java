/**
 * Copyright 2014-17 Laura Biggins
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
package uk.ac.babraham.SeqMonk.Displays.GiraphPlot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class GiraphPlotPanel extends JPanel implements Runnable {
			
	private static final long serialVersionUID = 1L;
	
	/** The clustering of the circles for colours */
	GiraphPlotCluster gpCluster; 
	
	/** Whether we're ready to draw */
	private boolean readyToDraw = false;
	
	/** The min and max x and y values */
	private float minValueX;
	private float maxValueX;
	private float minValueY;
	private float maxValueY;
			
	/** All the ProbeLists that we're displaying */
	protected ProbeList [] probeLists;
	
	protected Color [] probeListColours;
	
	/** circle that the mouse is hovering over */
	private Integer currentSelectedProbeList = null;
	private boolean probeListSelected = false;	
	
	/** circle currently clicked on by the user, we need this as well as the one above so that we can keep the annotation 
	 * showing. */
	private Integer currentClickedProbeList = null; 
	private boolean ProbeListClicked = false;
	
	/** Minimum correlation shown by lines */
	float minCorrelation = (float) 0.3;
	
	int minCircleSize = 15;
	
	boolean play = false;  
	
	/** if a circle is being dragged we just display the lines for that circle */
	boolean draggingCircle = false;
	
	/** whether we're exporting the image (so that graphics2 can be turned off) */
	public boolean exportImage = false;
	
	/** 
	 * These are options that the user can toggle
	 * 
	/** whether to display all the probeList names or not */	
	protected boolean displayNames = false;
	
	/** whether to display the lines connecting the circles */
	boolean lines = true;
	
	/** whether the program is currently calculating i.e. whether calculateCoordinates is running - this is controlled by the stop or play buttons on the menu */
	protected boolean calculating = false;
	
	private static int BORDER = 20; 
	
	private float [] xCoordinates;
	private float [] yCoordinates;
	
	private float shiftFactor = (float)0.3;
	
	/** A fixed log2 value to make log2 calculation quicker */
	protected final float log2 = (float)Math.log(2);	
	
	public GiraphPlotPanel(ProbeList [] probeLists){	
		
		this.addMouseMotionListener(mldrag);
		this.addMouseListener(ml);
		this.addComponentListener(compAdapter);
		
		this.probeLists = probeLists;
		xCoordinates = getStartingGridCoordinates(probeLists.length)[0];
		yCoordinates = getStartingGridCoordinates(probeLists.length)[1];
		
		probeListColours = new Color[probeLists.length];
		for(int i=0; i<probeLists.length; i++){
			probeListColours[i] = Color.gray;
		}
		
		gpCluster = new GiraphPlotCluster(xCoordinates, yCoordinates);
		calculating = true;
		Thread t = new Thread(this);
		t.start();
	}
	
	public void restart(){
		calculating = true;
		shiftFactor = (float)0.3;
		
		Thread t = new Thread(this);				
		t.start();
	}
	
	/** TODO: neaten up this old code. */
	public float [][] getStartingGridCoordinates(int n){
		
		float [][] gridCoordinates = new float[2][n];
		
		/** TODO: This might not be great if n is small */
		//int dim = (int)(Math.ceil(Math.sqrt(n)));
		
		float increment = (float)(1/(Math.ceil(Math.sqrt(n))));

		int i = 0;
		float y = 0;
		
		while(i < n){
			for (int j = 0; j <= Math.ceil(Math.sqrt(n)); j++){		
				if(i >= n){
					return gridCoordinates;
				}									
				gridCoordinates[0][i] = (float)(increment)*j;
				gridCoordinates[1][i] = (float)(increment)*y;	
				i++;
			}
			y++;	
		}
		return gridCoordinates;
	}
	
	/** This run method calculates the coordinates for the circles, should be between 0 and 1 ish. */
	public void run(){
		
		System.err.println("calculating positions...");
		
		/** The minimum correlation that we care about - if they're not remotely correlated we don't mind where they are in relation to each other,
		 * they'll get pulled around enough by others that they are correlated with anyway. */
		float minCorrelation = (float)0.3;
		
		/** If two circles are at an optimum distance (within threshold of the diffFactor) then they won't be moved. 
		 * Do not reduce this number or everything starts going around in circles if there aren't many gene lists. */
		float diffThreshold = (float)0.2;
		
		/**  x is a count and is used to control how often coordinates are updated etc */
		int x = 1;
		
		/** This is something to do with not wanting the circles to be too far away from each other - if they try and get too far then they mess up the screen. */
		float minDistance = (float) 0.4;
		
		
		
		/** To work out when to stop calculating */
		float [] previousTotalDifference = new float [20];
		int totalDifferenceIndex = 0;
				
		while (calculating == true){
		
			float thisTotalDifference = 0;			
			/**
			 * calculate overall trajectory for gene list i
			 */
			for (int i = 0; i < probeLists.length; i++){								
				
				/** value that is added to and subtracted from as we decide which way the x coordinate should move */
				float sumDiffX = 0;
				
				/** value that is added to and subtracted from as we decide which way the y coordinate should move */
				float sumDiffY = 0;
				
				/** loop through all the other gene lists to compare i to and work out where we want to move i to */
				for (int j = 0; j < probeLists.length; j++){
															
					if(calculating == false){
						//break LOOP;
						return;
					}
					
					if(j != i){	
																			
						/** 
						 * move closer or further away if the correlation is greater than minCorrelation. MinCorrelation is the minimum correlation that we care about.
						 *   
						 *  circles with no correlation only need to be minDistance away, not as far away as 1 as this distorts the view, but not too close 
						 *  
						 *  Is it right to say OR when the correlation is less than minCorrelation AND the difference is greater than minDistance?? Apparently so - it all goes wrong if that is removed.
						 */	
						
						
						/** The correlation between the 2 gene lists */
						float correlation = getOverlapProportion(probeLists[i], probeLists[j]);
						
						/** The difference in the x coordinates between gene list i and j */ 
						float distanceX = xCoordinates[i] - xCoordinates[j];
						
						/** The difference in the y coordinates between gene list i and j */
						float distanceY =  yCoordinates[i] - yCoordinates[j];
						 
						/** The distance between the 2 gene lists */
						float actualDistance = (float)Math.sqrt((distanceX * distanceX) + (distanceY * distanceY)); 																	
						
						/** The difference between the actual distance and the ideal distance */
						float difference = (1 - correlation) - actualDistance;
						
						/** can we bias this so that if there's a strong attraction, that gets prioritised? */
						
						thisTotalDifference = thisTotalDifference + Math.abs(difference);
											
						
						if((correlation > minCorrelation) || ((correlation <= minCorrelation)&& (Math.abs(difference)>minDistance))){														
							
							/** 
							 * If difference between actual location and desired location is greater than a set value (diffThreshold) then we want to move.  
							 * 
							 */
							//float movement = (1-correlation)/actualDistance;
							float movement;
							
							if(correlation < 0.01){
								movement = (float)0.01/actualDistance;
							}
							else{
								movement = correlation/actualDistance;							
							}								
							
						/*	if(probeLists[j].name().startsWith("Difference above 3")){ 
								System.out.println("========================================");
								System.out.println(probeLists[j].name());
								System.out.println(probeLists[i].name());
								System.out.println("difference = " + difference);
								System.out.println("actual distance = " + actualDistance);
								System.out.println("correlation = " + correlation);
								System.out.println("movement = " + movement);
								System.out.println("distanceX = " + distanceX);
								System.out.println("distanceY = " + distanceY);
							}
						*/	if(difference < -diffThreshold){
							/** move closer */
								
								sumDiffX = sumDiffX - (distanceX * movement);
								sumDiffY = sumDiffY - (distanceY * movement);

							}
							else if (difference > diffThreshold){
							/** move further away */
								
								sumDiffX = sumDiffX + (distanceX * movement); 
								sumDiffY = sumDiffY + (distanceY * movement);

			 				}
						}			
					}
				}
				float meanDiffX = sumDiffX/(float)(probeLists.length-1);
				float meanDiffY = sumDiffY/(float)(probeLists.length-1);	
				
				float xShift = (float) (meanDiffX*shiftFactor);
				float yShift = (float) (meanDiffY*shiftFactor);				
				xCoordinates[i] += xShift; 
				yCoordinates[i] += yShift;	
				
			}	
			
			previousTotalDifference[totalDifferenceIndex] = thisTotalDifference;
			
			if(totalDifferenceIndex < 19){
				totalDifferenceIndex = totalDifferenceIndex +1; 
			}
			else {
				totalDifferenceIndex = 0;
			}
			
			/** If it's the first iteration, notify the app that the first coordinates are ready. */
			if (x == 1){
				
				/** notify that we're ready to draw */
				readyToDraw = true;
				System.err.println("ready, let's draw...");
			}
			
			/**  update min and max values */
			minValueX = getMinValue(xCoordinates);
			maxValueX = getMaxValue(xCoordinates);
			minValueY = getMinValue(yCoordinates);
			maxValueY = getMaxValue(yCoordinates);		
			
			revalidate();
			repaint();

			/** We want to check whether we're still moving in the right direction, if not, stop if we're getting too few improving positions.
			 * or stop if the percentage difference is so small that it's not worth carrying on. */
			if (x%20 == 0){
								
				/** To check whether we're still moving in the right direction*/
				int improvingPositions = 0;
				float improvingMagnitude = 0;
				
				for (int i = 1; i < 20; i++){
					
					float diffDiff = previousTotalDifference[i-1] - previousTotalDifference[i];
					
					if(diffDiff > 0){
						improvingPositions++;
						improvingMagnitude = improvingMagnitude + diffDiff/previousTotalDifference[i];	
						
					}
				}
				
				if((improvingPositions < xCoordinates.length/5) || (improvingMagnitude < 0.00001)){
					
					// stopCalculating
					calculating = false;
					
					System.out.println("stopped calculating at x = " + x + " because...");
					System.out.println("no of improvingPositions " + improvingPositions);	
					System.out.println("improvingMagnitude " + improvingMagnitude);	
					
				}
				System.out.println("no of improvingPositions " + improvingPositions);	
				System.out.println("improvingMagnitude " + improvingMagnitude);	
				gpCluster = new GiraphPlotCluster(xCoordinates, yCoordinates);
								
				shiftFactor = improvingMagnitude;
				/** We're going to gradually reduce the shiftFactor */ 
				//shiftFactor = shiftFactor/2;
			}
			x++;		
		}		
	}
		
	protected float getOverlapProportion (ProbeList list1, ProbeList list2) {
		
		Probe [] firstListProbes = list1.getAllProbes();
		Probe [] secondListProbes = list2.getAllProbes();
		
		int l1 = 0;
		int l2 = 0;
		
		int overlapCount = 0;
		float overlapProportion = 0;
		
		while (true) {
			
			if (l1 >= firstListProbes.length) {
				break;
			}

			if (l2 >= secondListProbes.length) {
				break;
			}
						
			// Compare the two current probes to see what state we're in
			
			if (firstListProbes[l1] == secondListProbes[l2]) {
				//This probe is common to both lists
				++overlapCount;
				++l1;
				++l2;
			}
			else if (firstListProbes[l1].compareTo(secondListProbes[l2]) > 0) {
				// We can make a decision about the lower value (l2)
				l2++;
			}
			else {
				// We can make a decision about the lower value (l1)
				l1++;
			}
			if(firstListProbes.length >= secondListProbes.length){
				overlapProportion = (float)(overlapCount)/secondListProbes.length;
			}
			else{
				overlapProportion = (float)(overlapCount)/firstListProbes.length;
			}					
		}
		
		return overlapProportion;
	}

	
	
	/** 
	 * Used when rValue threshold has been adjusted on the menubar.
	 *  The clusters parameter is passed in from giraphApplication by using the method clusterPair.getValidClusters(menuBar.rValueCutoff())
	 *  This could be done when the clusters are first obtained but I'm going to leave it here for now.
	 */	

	public Color[] getColoursForClusters(Integer[][] clusters, int noOfColourValuesRequired){	
		
		if (clusters.length == 0 | noOfColourValuesRequired == 0){
			System.err.println("Clusters and no of colours required must both be greater than 0.");
			return null;
		}	
		
		/** all the colours, duplicated according to the clusters */
		Color[] colours = new Color[noOfColourValuesRequired];
		
		float increment = 255/clusters.length;
					
		for (int i = 0; i < clusters.length; i++){
			
			Color colourForCluster = new Color((int)(255-(increment*i)), 0, (int)(increment*i));
			
			for (int j = 0; j < clusters[i].length; j++){

				colours[clusters[i][j]] = colourForCluster;
			}	
		}
		return (colours);
	}
	

	/**
	 * Used when adding or removing the lines joining circles
	 */	
/*	public void updateLines(){
		if (lines == false){
			lines = true;
		}
		else{
			lines = false;
		}
		revalidate();
		repaint();
	}
*/	
			
	/** 
	 * Can I just get rid of this now?
	 */
	ComponentAdapter compAdapter = new ComponentAdapter() {

	    public void componentResized(ComponentEvent e){
	    
	    	revalidate();
			repaint();
	    }
	};    
	
	
	/** 
	 * Enables the circles to be manipulated by the mouse. 
	 */	
	MouseMotionListener mldrag = new MouseMotionListener(){		
						
		/** displays functional term if mouse is over the circle */		
		public void mouseMoved(MouseEvent e){	
				
			boolean circleSelected = isProbeListSelected(e.getX(), e.getY());
			
			if(circleSelected){
				revalidate();
				repaint();
			}
		}		
	
		
		/** makes the circles draggable so they can be moved around with the mouse (but not if it's still calculating) */
		
		public void mouseDragged(MouseEvent e){	
			
			if (calculating == false){
					
				//check if a circle is selected
				if(currentClickedProbeList != null){	
				
					int newX = e.getX();
					int newY = e.getY();
					
					if((newX <= getXLimits()[1])&&(newX >= getXLimits()[0])&&(newY <= getYLimits()[1])&&(newY >= getYLimits()[0])) {	
						
						getValueFromX(newX);
						getValueFromY(newY);
						
						xCoordinates[currentClickedProbeList] = getValueFromX(newX);
						yCoordinates[currentClickedProbeList] = getValueFromY(newY);
						
						draggingCircle = true;
						
						revalidate();
						repaint();
								
					}
				}
				gpCluster = new GiraphPlotCluster(xCoordinates, yCoordinates);
			}
			else{
				System.err.println("You need to press stop before you can drag the circles");
				//JOptionPane.showMessageDialog(application,"You need to press stop before you can drag the circles", "cannot drag while calculating", JOptionPane.ERROR_MESSAGE);
			}
		}	 
	};
	
	
	/** 
	 * identifies which circle/ProbeList has been selected (clicked on)
	 */	
	MouseListener ml = new MouseAdapter(){
						
		public void mousePressed(MouseEvent e){
			
			if (isProbeListSelected(e.getX(), e.getY())){
			
				ProbeListClicked = true;
				currentClickedProbeList = currentSelectedProbeList;
			}
			
			else {
				ProbeListClicked = false;
			}
			revalidate();
			repaint();
		} 
		
		public void mouseReleased(MouseEvent e){
			
			draggingCircle = false;
		}
		
		public void mouseClicked(MouseEvent e){
		}	
	};	  
		
	
	/** Returns a boolean to say whether the mouse is over a circle or not
	 * and sets the currentSelected ProbeList */
	
	private boolean isProbeListSelected(int firstClickX, int firstClickY) {	
		
		// This counts backwards to get the gene list that is on the top
		for (int i = probeLists.length-1; i >= 0; i--){	

			int diameter = getDiameterOfCircle(probeLists[i]);
					
			int x0 = getX0(xCoordinates[i], diameter);
			int y0 = getY0(yCoordinates[i], diameter);
			
			if((firstClickX < (x0 + diameter)) && (firstClickX > x0) && (firstClickY < (y0 +diameter)) && (firstClickY > y0)){	 
				
				currentSelectedProbeList = i;				
				probeListSelected = true;
				return true;
			}
		}
		probeListSelected = false;
		return false;	
	}
		

	//Gets the calc coordinate y value from the screen y value.
	public float getValueFromY (int y) {
		
		float proportion = ((float)(y - getYLimits()[0])/ (float)(getYLimits()[1] - getYLimits()[0]));
		
		float value = proportion * (float)(maxValueY-minValueY) + minValueY;
		
		return value;
	}


	// Gets the calc coordinate x value from the screen x value.
	public float getValueFromX (int x) {

		float proportion = ((float)(x - getXLimits()[0])/ (float)(getXLimits()[1] - getXLimits()[0]));
		float unScaled = maxValueX-minValueX;
		
		float value = proportion * unScaled + minValueX;
		
		return value;
	}

	// The scaled x0 value for the circle
	private int getX0(float value, float diameter) {
	
		float proportion = (value-minValueX)/(maxValueX-minValueX);
		int [] xLimits = getXLimits();
		
		// xCentre that may go off screen
		int x = (int)((xLimits[1] - xLimits[0]) * proportion) + xLimits[0]; 
		
		int x0 = (int)(x-((float)(diameter)/2));
		
		if(x0 < xLimits[0]){
			x0 = xLimits[0];
		}
		if(x0+diameter > xLimits[1]){
			x0 = (int)(xLimits[1] - diameter);
		}
		return x0;
	}

	// The scaled x0 value for the circle
	private int getY0(float value, float diameter) {
		
		float proportion = (value-minValueY)/(maxValueY-minValueY);
		int [] yLimits = getYLimits();
		
		// yCentre that may go off screen
		int y = (int)((yLimits[1] - yLimits[0]) * proportion) + yLimits[0]; 
		
		int y0 = (int)(y-((float)(diameter)/2));
		
		if(y0 < yLimits[0]){
			y0 = yLimits[0];
		}
		if(y0+diameter > yLimits[1]){
			y0 = (int)(yLimits[1] - diameter);
		}
		return y0;
	}
	
	// get the XCentre screen position from the calcCoordinates output
	private int getXCentre(float value, float diameter){
		
		int x0 = getX0(value, diameter);
		return((int)(x0 + (diameter/2)));
	}
	
	// get the YCentre screen position from the calcCoordinates output
	private int getYCentre(float value, float diameter){
		
		int y0 = getY0(value, diameter);
		return((int)(y0 + (diameter/2)));
	}
	
	/* The x axis position in the window. We want it centred. */
	private int [] getXLimits(){
		
		int[] xlimits = new int[2];
		if(getWidth() > getHeight()){
			xlimits[0] = (getWidth() - getHeight())/2 + BORDER;
			xlimits[1] = xlimits[0] + getHeight() - BORDER;
		}	
		else{
			xlimits[0] = BORDER;
			xlimits[1] = getWidth() - BORDER;			
		}
		return xlimits;
	}	
	
	/* The x axis position in the window. We want it centred. */
	private int [] getYLimits(){
		
		int[] ylimits = new int[2];
		if(getHeight() > getWidth()){
			ylimits[0] = (getHeight() - getWidth())/2 + BORDER;
			ylimits[1] = ylimits[0] + getWidth() - BORDER;
		}	
		else{
			ylimits[0] = BORDER;
			ylimits[1] = getHeight() - BORDER;			
		}
		return ylimits;
	}
	
		
	private int getDiameterOfCircle(ProbeList pl){
		
		float smallestDim;
		
		if(getYLimits()[1] < getXLimits()[1]){
			smallestDim = getYLimits()[1];
		}
		else{
			smallestDim = getXLimits()[1];
		}
		/** we'll log transform the no of probes to try and get a sensible scaling */
		float log2Value = (float)Math.log(pl.getAllProbes().length)/log2;		
		if(log2Value < 1){
			log2Value = 1;
		}
		
		float div;
		if(probeLists.length < 15){
			div = (float)30;
		}
		else{
			div = probeLists.length*2;
		}
		int diameter = (int)(((smallestDim/div)*log2Value)/2);
				
		//System.err.println("diameter = " + diameter);		
		
		if ((diameter >= minCircleSize) && (diameter <= (smallestDim/5))) {
			return(diameter);
		}
		else if (diameter < minCircleSize){
			return(minCircleSize);
		}		
		// else if diameter is too big, set a maximum circle size 
		return((int)(smallestDim/5));
	}
	
	/* so we can set the minimum x and y values */
	private float getMinValue(float[]coordinates){		
		float min = coordinates[0];		
		for (int i = 1; i < coordinates.length; i++){ 
			if(coordinates[i] < min){
				min = coordinates[i];
			}
		}
		return min;
	}	
	  
	/* so we can set the maximum x and y values */
	private float getMaxValue(float[]coordinates){		
		float max = coordinates[0];		
		for (int i = 1; i < coordinates.length; i++){ 
			if(coordinates[i] > max){
				max = coordinates[i];
			}
		}	
		return max;
	}
	
	public void paint(Graphics g){
		
		super.paint(g);	
						
		// if we're exporting the image we need to not be using Graphics2
		if(exportImage == false){
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g = g2;
		}
		
		FontMetrics fm = g.getFontMetrics();
		
		if (! readyToDraw){
			g.setColor(Color.GRAY);
			String message = "Calculating Plot";
			g.drawString(message, (getWidth()/2)-(fm.stringWidth(message)/2), (getHeight()/2-2));
			return;
		}
		
		if (gpCluster.clustersReady){
						
			probeListColours = getColoursForClusters(gpCluster.clusterPair().getClusters((float)0.7), probeLists.length);			
		}
		
		/** draw each line joining the circles (though this can be turned off) 
		 * This takes ages when there are lots of circles, it's not the calculations, it's just the redrawing of all of the lines
		 * as there are obviously many more lines than circles.
		 *  */	
		if ((lines == true) && (draggingCircle == false)) {
				
			for (int i = 0; i < probeLists.length; i++){	
					
				for (int j = i+1; j < probeLists.length; j++){
										
					float correlation = getOverlapProportion(probeLists[i],probeLists[j]);
					
					if (correlation > minCorrelation){	
						
						float saturation = 1 - ((correlation));
						
						if (saturation < 0.1){
							saturation = (float) 0.1;
						}						
							
						g.setColor(new Color(saturation, saturation, saturation));
						
						g.drawLine(getXCentre(xCoordinates[i], getDiameterOfCircle(probeLists[i])), getYCentre(yCoordinates[i], getDiameterOfCircle(probeLists[i])),
								getXCentre(xCoordinates[j], getDiameterOfCircle(probeLists[j])), getYCentre(yCoordinates[j], getDiameterOfCircle(probeLists[j])));
						
					}	
				}
			}	
		}	
		
		// just paint the lines for the circle being dragged otherwise it takes ages to keep repainting all the lines
		if(draggingCircle == true){
			
			for (int i = 0; i < probeLists.length; i++){	
				
				float correlation = getOverlapProportion(probeLists[currentSelectedProbeList], probeLists[i]);
				
				if (correlation > minCorrelation){	
					
					float saturation = 1 - ((correlation));
					
					if (saturation < 0.1){
						saturation = (float) 0.1;
					}
						
					g.setColor(new Color(saturation, saturation, saturation));		
					
					g.drawLine(getXCentre(xCoordinates[currentSelectedProbeList], getDiameterOfCircle(probeLists[currentSelectedProbeList])), getYCentre(yCoordinates[currentSelectedProbeList], getDiameterOfCircle(probeLists[currentSelectedProbeList])), 
							getXCentre(xCoordinates[i], getDiameterOfCircle(probeLists[i])), getYCentre(yCoordinates[i], getDiameterOfCircle(probeLists[i])));
				}				
			}
		}
				
		
		/**  draw each circle/ProbeList */	
		
		for (int i = 0; i < probeLists.length; i++){
			
			int diameter = getDiameterOfCircle(probeLists[i]);

			int x0 = getX0(xCoordinates[i], diameter);
			int y0 = getY0(yCoordinates[i], diameter);				
			
			g.setColor(probeListColours[i]);			
			
			g.fillOval(x0, y0, diameter, diameter);
			
			// draw a border round the circle so it can be distinguished from neighbours 			
			g.setColor(Color.white);
					
			g.drawOval(x0, y0, diameter, diameter);
			
		}	
		
		if (displayNames == true){
		
			for (int i = 0; i < probeLists.length; i++){
				
				int diameter = getDiameterOfCircle(probeLists[i]);
				
				int xCentre = getXCentre(xCoordinates[i], diameter);
				int yCentre = getYCentre(yCoordinates[i], diameter);		
								
				g.setColor(Color.white);
				g.fillRoundRect((int)(xCentre) -2, (int)(yCentre) -10, fm.stringWidth(probeLists[i].name())+5, 12, 4, 4);
				g.setColor(Color.black);
				g.drawString(probeLists[i].name(), (int)(xCentre), (int)(yCentre));				
			}
		}
			
		
		/**
		 *  display functional term and highlight the selected circle
		 */	
		if(ProbeListClicked){
			
			int diameter = getDiameterOfCircle(probeLists[currentClickedProbeList]);
			int xCentre = getXCentre(xCoordinates[currentClickedProbeList], diameter);
			int yCentre = getYCentre(yCoordinates[currentClickedProbeList], diameter);			
			int x0 = getX0(xCoordinates[currentClickedProbeList], diameter);
			int y0 = getY0(yCoordinates[currentClickedProbeList], diameter);
			
			int adjust = 4;
			
			// draw the highlighted ring
			g.setColor(probeListColours[currentClickedProbeList]);
			g.drawOval(x0-adjust, y0-adjust, diameter+(adjust*2), diameter+(adjust*2));		
						
			if (displayNames == false){
				
			// add the name info
				g.setColor(Color.white);
				g.fillRoundRect(xCentre -2 , yCentre-10, fm.stringWidth(probeLists[currentClickedProbeList].name())+5, 12, 4, 4);
				g.setColor(Color.black);
				g.drawString(probeLists[currentClickedProbeList].name(), xCentre, yCentre);
			}
		}
		
		/** We've got ProbeListSelected as well as ProbeListClicked so that 2 terms can be displayed at the same time,
		 *  one that's been clicked on and one that's moused over.
		 */
		if(probeListSelected){
			
			int diameter = getDiameterOfCircle(probeLists[currentSelectedProbeList]);
			int xCentre = getXCentre(xCoordinates[currentSelectedProbeList], diameter);
			int yCentre = getYCentre(yCoordinates[currentSelectedProbeList], diameter);
			int x0 = getX0(xCoordinates[currentSelectedProbeList], diameter);
			int y0 = getY0(yCoordinates[currentSelectedProbeList], diameter);
				
			int adjust = 4;
			
			g.setColor(probeListColours[currentSelectedProbeList]);
			
			g.drawOval(x0-adjust, y0-adjust, diameter+(adjust*2), diameter+(adjust*2));		
			
			if (displayNames == false){
			// add the name info	
				g.setColor(Color.white);
				g.fillRoundRect(xCentre -2 , yCentre-10, fm.stringWidth(probeLists[currentSelectedProbeList].name())+5, 12, 4,4);
				g.setColor(Color.black);
				g.drawString(probeLists[currentSelectedProbeList].name(), xCentre, yCentre);
			}
		}
	}
	
	/**
	 *  Used in giraphMenuBar to display current info
	 */
	public float getMinCorrelation(){
		return minCorrelation;
	}	
}	