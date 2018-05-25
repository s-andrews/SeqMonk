/**
 * Copyright 2009-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The Class TrendOverProbePanel draws a probe trend plot.
 */
public class QuantitationTrendPlotPanel extends JPanel implements MouseMotionListener, MouseListener {

	private QuantitationTrendData data;
	private String segment;

	/** The draw cross hair. */
	private boolean drawCrossHair = false;

	/** The mouse x. */
	private int mouseX = 0;

	/** This says over how many points we should smooth the data */
	private int smoothingLevel = 0;

	/** This just helps with formatting numbers displayed on the plot */
	private DecimalFormat df = new DecimalFormat("#.###");

	private boolean fixedLength = false;
	private double axisMin;
	private double axisMax;

	private int Y_AXIS_SPACE = 20;

	/**
	 * Instantiates a new trend over probe panel.
	 * 
	 * @param probes the probes
	 * @param stores the DataStores to plot
	 * @param prefs the preferences file for this plot
	 */
	public QuantitationTrendPlotPanel (QuantitationTrendData data, String segment) {

		this.data = data;
		this.segment = segment;

		if (segment.equals("upstream")) {
			fixedLength = true;
			axisMin = data.upstreamAxisStart;
			axisMax = data.upstreamAxisEnd;
		}
		else if (segment.equals("central")) {
			fixedLength = data.isFixedWidth();
			if (fixedLength) {
				axisMin = data.centralAxisStart;
				axisMax = data.centralAxisEnd;
			}
		}
		else if (segment.equals("downstream")) {
			fixedLength = true;
			axisMin = data.downstreamAxisStart;
			axisMax = data.downstreamAxisEnd;

		}
		else {
			throw new IllegalStateException("Invalid segment value "+segment);
		}
		addMouseListener(this);
		addMouseMotionListener(this);
	}


	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint (Graphics g) {
		super.paint(g);
		g.setColor(Color.WHITE);

		g.fillRect(0, 0, getWidth(), getHeight());

		FontMetrics metrics = getFontMetrics(g.getFont());


		// If we're here then we can actually draw the graphs

		g.setColor(Color.BLACK);

		// X axis
		g.drawLine(0, getHeight()-Y_AXIS_SPACE, getWidth(), getHeight()-Y_AXIS_SPACE);

		// Y axis
		g.drawLine(0, 10, 0, getHeight()-Y_AXIS_SPACE);

		// X labels
		if (!fixedLength) {
			String xLabel = "Relative distance";
			g.drawString(xLabel,(getWidth()/2)-(metrics.stringWidth(xLabel)/2),getHeight()-5);
		}

		if (fixedLength) {
			AxisScale xAxisScale = new AxisScale(axisMin, axisMax);

			double currentXValue = xAxisScale.getStartingValue();
			double lastXLabelEnd = -1;

			
			while (currentXValue+xAxisScale.getInterval() <= axisMax) {
				int thisX = 0;
				double xProportion = (double)(currentXValue-axisMin)/(axisMax-axisMin);
				thisX += (int)(xProportion * getWidth());

				int thisHalfLabelWidth = (g.getFontMetrics().stringWidth(xAxisScale.format(currentXValue))/2);

				if (thisX - thisHalfLabelWidth <= lastXLabelEnd) {
					currentXValue += xAxisScale.getInterval();
					continue;
				}

				lastXLabelEnd = thisX + thisHalfLabelWidth;

				g.drawString(xAxisScale.format(currentXValue), thisX-thisHalfLabelWidth, (int)((getHeight()-Y_AXIS_SPACE)+15));
				g.drawLine(thisX, getHeight()-Y_AXIS_SPACE, thisX, getHeight()-(Y_AXIS_SPACE-3));

			}
		}

		// Now go through the various datastores
		for (int d=0;d<data.stores().length;d++) {
			for (int l=0;l<data.lists().length;l++){

				g.setColor(ColourIndexSet.getColour((data.lists().length*data.stores().length)+l));

				double [] dataToPlot = null;

				if (segment.equals("upstream")) {
					dataToPlot = data.getUpstreamData(data.stores()[d], data.lists()[l]);
				}
				else if (segment.equals("central")) {
					dataToPlot = data.getCentralData(data.stores()[d], data.lists()[l]);
				}
				else if (segment.equals("downstream")) {
					dataToPlot = data.getDownstreamData(data.stores()[d], data.lists()[l]);
				}

				//TODO: Smooth data if needed
				
				int lastX = 0;
				int lastY = getY(dataToPlot[0]);

				boolean drawnCrossHair = false;

				for (int x=1;x<dataToPlot.length;x++) {
					double xProportion = (double)x/(dataToPlot.length-1);
					int thisX = (int)(xProportion * getWidth());

					if (thisX == lastX) continue; // Only draw something when we've moving somewhere.

					int thisY = getY(dataToPlot[x]);

					if (drawCrossHair && ! drawnCrossHair && thisX >=mouseX) {
						String label = df.format(dataToPlot[x]);

						// Clean up the ends if we can
						label = label.replaceAll("0+$", "");
						label = label.replaceAll("\\.$", "");

						g.drawString(label, thisX+2, thisY);
						drawnCrossHair = true;

						if (d==0) {
							Color oldColor = g.getColor();
							g.setColor(Color.GRAY);
							g.drawLine(thisX, 10, thisX, getHeight()-20);
							g.drawString(""+x, thisX+2, getHeight()-27);
							g.setColor(oldColor);
						}
					}


					g.drawLine(lastX, lastY, thisX, thisY);
					lastX = thisX;
					lastY = thisY;
				}
			}
		}
	}

	/**
	 * Gets the y.
	 * 
	 * @param count the count
	 * @return the y
	 */
	public int getY (double count) {
		double proportion = ((double)(count-data.getMinValue()))/(data.getMaxValue()-data.getMinValue());

		int y = getHeight()-Y_AXIS_SPACE;

		y -= (int)((getHeight()-(10+Y_AXIS_SPACE))*proportion);

		return y;
	}

	/**
	 * Sets a new smoothing level and redraws the plot
	 * @param smoothingLevel
	 */
	public void setSmoothing (int smoothingLevel) {

		this.smoothingLevel = smoothingLevel;		
		repaint();
	}





	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {
		if (e.getX()>10 && e.getX()<getWidth()) {
			drawCrossHair = true;
			mouseX = e.getX();
			repaint();
		}
		else {
			drawCrossHair = false;
			repaint();
		}

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
		drawCrossHair = false;
		repaint();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {}


}
