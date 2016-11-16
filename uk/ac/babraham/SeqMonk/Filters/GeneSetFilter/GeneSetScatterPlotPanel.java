/**
 * Copyright 2014-15 Laura Biggins
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
package uk.ac.babraham.SeqMonk.Filters.GeneSetFilter;

import java.awt.Color;
import java.awt.Graphics;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.ScatterPlot.ScatterPlotPanel;

public class GeneSetScatterPlotPanel extends ScatterPlotPanel{

	private static final int X_AXIS_SPACE = 50;
	private static final int Y_AXIS_SPACE = 30;
	private float [][] customRegressionValues = null;
//	private SimpleRegression simpleRegression;
	
	public  GeneSetScatterPlotPanel(DataStore xStore, DataStore yStore, ProbeList probeList, ProbeList[] subLists, boolean commonScale, int dotSize, float[][]customRegressionValues, SimpleRegression simpleRegression) {
		
		super(xStore, yStore, probeList, subLists, commonScale, dotSize);
		// TODO Auto-generated constructor stub
		
		this.customRegressionValues = customRegressionValues;
//		this.simpleRegression = simpleRegression;
		
	}
	
	
	public void paint (Graphics g) {
		super.paint(g);					
		
		if(customRegressionValues != null & readyToDraw){
			g.setColor(Color.magenta);
			for (int i=0; i<customRegressionValues[0].length; i++){
				
				// some of the points fall off the plotting area
				if((getX(customRegressionValues[0][i]) > X_AXIS_SPACE ) &&  (getY(customRegressionValues[1][i]) < getHeight()-Y_AXIS_SPACE)){
					g.fillOval(getX(customRegressionValues[0][i]), getY(customRegressionValues[1][i]), 3, 3);
					
				}	
			}
		}
		
		//System.err.println("x for 5.377 = " + getX(5.377) + ", y for 4.761 = " + getY(4.761));
		
		//if(calculateLinearRegression && (simpleRegression != null)){
/*		if(simpleRegression != null){
		
			int x1;
			int x2;
			int y1;
			int y2;
			
			// at intersect of x axis
			y1 = getY((simpleRegression.getSlope() * (double)(getValueFromX(X_AXIS_SPACE))) + simpleRegression.getIntercept());
			
			if(y1 < (getHeight()-Y_AXIS_SPACE)){					
				x1 = X_AXIS_SPACE;
			}
			
			// if y1 is off the screen
			else{
				y1 = getHeight()-Y_AXIS_SPACE;
				
				x1 = getX(((double)(getValueFromY(y1) - simpleRegression.getIntercept()))/simpleRegression.getSlope());
			}
			
			y2 = getY((simpleRegression.getSlope() * (double)(getValueFromX((getWidth()-10)))) + simpleRegression.getIntercept());  
			
			if(y2 <= 10){
				y2 = 10;
				x2 = getX(((double)(getValueFromY(y2) - simpleRegression.getIntercept()))/simpleRegression.getSlope());
			}
			
			else{
				x2 = (getWidth()-10);
			}
			
			g.setColor(Color.BLACK);
			g.drawLine(x1,y1,x2,y2);
*/			
			
		/*	System.err.println("y intercept = " + simpleRegression.getIntercept() + ", slope = " + simpleRegression.getSlope() + 
					", x intercept = " + (-(simpleRegression.getIntercept()/simpleRegression.getSlope())));
		*/
	}			
}

