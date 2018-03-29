/**
 * Copyright 2014-18 Laura Biggins
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
	float minCorrelation = (float) 0.1;
	
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
	boolean lines = false;
	
	/** whether the program is currently calculating i.e. whether calculateCoordinates is running - this is controlled by the stop or play buttons on the menu */
	protected boolean calculating = false;
	
	private static int BORDER = 40; 
	
	private float [] xCoordinates;
	private float [] yCoordinates;
	
	//private float shiftFactor = (float)0.3;
	
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
		
		//gpCluster = new GiraphPlotCluster(xCoordinates, yCoordinates);
		calculating = true;
		Thread t = new Thread(this);
		t.start();
	}
	
	public void restart(){
		lines = false;
		calculating = true;
		//shiftFactor = (float)0.3;
		
		Thread t = new Thread(this);				
		t.start();
	}
	
	// create random starting coordinates
	public float [][] getStartingGridCoordinates(int n){
	
		float [][] randomCoordinates = new float[2][n];
		
		for(int i=0; i<n; i++){
			
			randomCoordinates[0][i] = (float)Math.random();
			randomCoordinates[1][i] = (float)Math.random(); 
		}
		
		return randomCoordinates;
	}	
		
	
	/** This run method calculates the coordinates for the circles, should be between 0 and 1 ish. */
	public void run(){
		
		float coolingOff = (float)0.8;
		
		// compare the last 10 total deltas to check whether it's improving or not
		float [] lastDeltas = new float[10];
		
		int x=0;
		
		// the number of iterations TODO: stop this automatically
		while(calculating){
		//for(int x=0; x<1000; x++){
			
			float totalDelta = 0;
			
			for (int i = 0; i < probeLists.length; i++){												
				
				/** loop through all the other gene lists to compare i to and shift j each time */
				for (int j = 0; j < probeLists.length; j++){
															
					if(j != i){	
																			
						/** The correlation between the 2 gene lists */
						float correlation = getOverlapProportion(probeLists[i], probeLists[j]);
						
						/** The difference in the x coordinates between gene list i and j */ 
						double distanceX = xCoordinates[i] - xCoordinates[j];
						
						/** The difference in the y coordinates between gene list i and j */
						double distanceY =  yCoordinates[i] - yCoordinates[j];
						 
						/** The distance between the 2 gene lists */
						float actualDistance = (float)Math.sqrt((distanceX * distanceX) + (distanceY * distanceY)); 																	
						
						/** The difference between the actual distance and the ideal distance */
						float delta = (1 - correlation) - actualDistance;
						
						totalDelta += Math.abs(delta);
						
						// we don't want the circles to keep trying to get as far away from each other as possible						
						// if they're not correlated at all
						if(correlation > 0.3 || Math.abs(delta) > 0.7){ 
						
							if(delta > 0){
								/** move closer */
								
								xCoordinates[j] -= (float)distanceX * coolingOff; 
								yCoordinates[j] -= (float)distanceY * coolingOff; 
							}
							
							else if (delta < 0){
								/** move further away */
								
								xCoordinates[j] += (float)distanceX * coolingOff; 
								yCoordinates[j] += (float)distanceY * coolingOff;
				 			}
						}	
					}	
				}
			}
			
			if(x < 10){
				
				lastDeltas[x] = totalDelta;
			}
			else{
				
				// check the differences - see if it's still improving
				if(x%10 == 0){
					int improvingCount = 0;

					for(int d=0; d<9; d++){
					
						if(lastDeltas[d] - lastDeltas[d+1] > 0.1){
							improvingCount++;
						}	
					}
					if(improvingCount < 4){
												
						calculating = false;
					}
					System.err.println("x = " + x);
					System.err.println("improving count = " + improvingCount);
					
				}
				
				lastDeltas[x%10] = totalDelta;
				
			}
			x++;
			
		/*	if (x%200 == 0){
				//System.err.println("x = " + x);	
				System.err.println("total delta = " + totalDelta);	
				
				readyToDraw = true;
				//System.err.println("ready, let's draw...");			
			
				//  update min and max values 
				// do this here rather than in the paint method as it's not going to change each time it's repainted
				minValueX = getMinValue(xCoordinates);
				maxValueX = getMaxValue(xCoordinates);
				minValueY = getMinValue(yCoordinates);
				maxValueY = getMaxValue(yCoordinates);		
				
				revalidate();
				repaint();

			}
		*/	coolingOff *= 0.9;
		}
		
		gpCluster = new GiraphPlotCluster(xCoordinates, yCoordinates);
		readyToDraw = true;
		//System.err.println("ready, let's draw...");			
	
		//  update min and max values 
		// do this here rather than in the paint method as it's not going to change each time it's repainted
		minValueX = getMinValue(xCoordinates);
		maxValueX = getMaxValue(xCoordinates);
		minValueY = getMinValue(yCoordinates);
		maxValueY = getMaxValue(yCoordinates);		
		
		lines = true;
		calculating = false;
		revalidate();
		repaint();
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
			
			//Color colourForCluster = new Color((int)(255-(increment*i)), 0, (int)(increment*i));
			Color colourForCluster = new Color((int)(255-(increment*i)), (int)(increment*i), (int)(increment*i));
			
			for (int j = 0; j < clusters[i].length; j++){

				colours[clusters[i][j]] = colourForCluster;
			}	
		}
		return (colours);
	}
	
			
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

	// The scaled y0 value for the circle
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
			xlimits[1] = xlimits[0] + getHeight() - BORDER - BORDER;
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
			ylimits[1] = ylimits[0] + getWidth() - BORDER - BORDER;
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
		
		
		if(gpCluster != null){
					
			if (gpCluster.clustersReady){
							
				probeListColours = getColoursForClusters(gpCluster.clusterPair().getClusters((float)0.7), probeLists.length);			
			}
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