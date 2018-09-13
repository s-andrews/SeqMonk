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
package uk.ac.babraham.SeqMonk.Displays.HistogramPlot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The Class HistogramPanel displays an interactive histogram from
 * any linear set of data.
 */
public class HistogramPanel extends JPanel implements Runnable, ChangeListener {

	/** The data. */
	private float [] data;
	
	/** The category slider. */
	private JSlider categorySlider;
	
	/** The main histogram panel. */
	private MainHistogramPanel mainHistogramPanel;
	
	/** The status panel. */
	private StatusPanel statusPanel;
	
	/** The log check box. */
	private JCheckBox logCheckBox;
	
	/** The Constant df. */
	private static final DecimalFormat df = new DecimalFormat("#.##");
	
	/** The calculating categories. */
	private boolean calculatingCategories = false;
	
	/** The stop calculating. */
	private boolean stopCalculating = false;
	
	/** The min data value. */
	private double minDataValue;
	
	/** The max data value. */
	private double maxDataValue;
	
	/** The current category count. */
	private int currentCategoryCount = 0;
	
	/** The currently displayed min value after zooming */
	private double currentMinValue;
	
	/** The currently displayed max value after zooming */
	private double currentMaxValue;
	
	/* These values are used when dragging a selection to zoom */
	private int selectionStart;
	private int selectionEnd;
	private boolean isSelecting = false;
	
	
	
	/**
	 * Instantiates a new histogram panel.
	 * 
	 * @param data the data
	 */
	public HistogramPanel (float [] data) {
				
		this.data = removeNaN(data);
		
		if (data.length < 2) {
			throw new IllegalArgumentException("At least two data points are needed to draw a histogram");
		}
		
		// We need to find the min and max values for the
		// data.  We can do this here since it won't change
	
		minDataValue = data[0];
		maxDataValue = data[0];
		
		for (int i=1;i<data.length;i++) {
			if (data[i] == Double.NaN || data[i] == Double.NEGATIVE_INFINITY || data[i] == Double.POSITIVE_INFINITY) continue;
			if (data[i]<minDataValue) minDataValue = data[i];
			else if (data[i]>maxDataValue) maxDataValue = data[i];
		}
		
		// We'll get weird effects if our interval size is 0
		if (minDataValue == maxDataValue) {
			minDataValue -=1;
			maxDataValue +=1;
		}
		
		// We initially default to showing the whole dataset
		currentMinValue = minDataValue;
		currentMaxValue = maxDataValue;
		
		setLayout(new BorderLayout());
		
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		categorySlider = new JSlider(5,200,25);
		categorySlider.setMajorTickSpacing(15);
		categorySlider.setPaintTicks(true);
		categorySlider.setPaintLabels(true);
		categorySlider.addChangeListener(this);
		topPanel.add(categorySlider,BorderLayout.CENTER);
		topPanel.add(new JLabel("Divisions"),BorderLayout.WEST);
		
		JPanel logPanel = new JPanel();
		logPanel.setLayout(new BorderLayout());
		logPanel.add(new JLabel("Log Scale"),BorderLayout.CENTER);
		
		logCheckBox = new JCheckBox();
		logCheckBox.setSelected(false);
		logCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainHistogramPanel.repaint();
			}
		});
		logPanel.add(logCheckBox,BorderLayout.EAST);
		topPanel.add(logPanel,BorderLayout.EAST);
		
		add(topPanel,BorderLayout.NORTH);
		
		
		mainHistogramPanel = new MainHistogramPanel();
		add(mainHistogramPanel,BorderLayout.CENTER);
		
		statusPanel = new StatusPanel();
		add(statusPanel,BorderLayout.SOUTH);
		
		calcuateCategories(categorySlider.getValue());
		
	}
	
	private float [] removeNaN (float [] data) {
		
		Arrays.sort(data);
		
		int lastValidIndex = data.length-1;
		
		for (;lastValidIndex>0;lastValidIndex--) {
			if(!Float.isNaN(data[lastValidIndex])) break;
		}
		
		if (lastValidIndex == data.length-1) return data;
		
		return Arrays.copyOf(data, lastValidIndex+1);
		
	}
	
	/**
	 * Main histogram panel.
	 * 
	 * @return the j panel
	 */
	public JPanel mainHistogramPanel () {
		return mainHistogramPanel;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {
		// When this fires it means that the categorySlider
		// has moved so we need to recalculate the graph.
				
		calcuateCategories(categorySlider.getValue());
	}
	
	public void setScale (double min, double max, int bins) {
		this.currentMinValue = min;
		this.currentMaxValue = max;
		categorySlider.setValue(bins);
	}
	
	public double currentMinValue () {
		return currentMinValue;
	}
	
	public double currentMaxValue () {
		return currentMaxValue;
	}
	
	public void exportData (File file) throws IOException {
		mainHistogramPanel.exportData(file);
	}
	
	/**
	 * Calcuate categories.
	 * 
	 * @param categoryCount the category count
	 */
	private void calcuateCategories (int categoryCount) {
		
		// If we're already calculating then stop and do it
		// again with the new value
				
		if (calculatingCategories) {
//			System.out.println("Waiting for previous calculation to finish");
			stopCalculating = true;
			while (calculatingCategories) {
//				System.out.println("Still waiting");
				try {
					Thread.sleep(50);
				} 
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		currentCategoryCount = categoryCount;
		
		Thread t = new Thread(this);
		t.start();
		
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

//		System.out.println("Calculating "+currentCategoryCount+" categories from "+currentMinValue+" to "+currentMaxValue);
		
		calculatingCategories = true;
		
		// In this thread we recalculate the categories to display
		
		HistogramCategory [] categories = new HistogramCategory[currentCategoryCount];
		
		double interval = (currentMaxValue-currentMinValue)/categories.length;

//		System.out.println("Calculating "+currentCategoryCount+" categories from "+currentMinValue+" to "+currentMaxValue+" with interval "+interval);

		for (int c=0;c<categories.length;c++) {
			categories[c] = new HistogramCategory(currentMinValue+(interval*c),currentMinValue+(interval*(c+1)));
//			System.out.println("Made category from "+categories[c].minValue+"-"+categories[c].maxValue);
		}
		
		for (int d=0;d<data.length;d++) {
			
			if (d%100 == 0) {
				// Check if we've been asked to stop.
				if (stopCalculating) {
//					System.out.println("Stopping");
					stopCalculating = false;
					calculatingCategories = false;
					return;
				}
			}
			
			// Don't count silly numbers
			if (data[d] == Double.NaN || data[d] == Double.NEGATIVE_INFINITY || data[d] == Double.POSITIVE_INFINITY) continue;
			
			// We need to put in an explicit check for the max value otherwise we 
			// get stupid boundary errors where our int rounds down when it shouldn't
			
			int index;
			
			if (data[d] == currentMaxValue) {
				index = categories.length-1;
			}
			else {
				index = (int)((data[d]-currentMinValue)/interval);
			}
			
			if (index < 0) {
				
				// This just means we've zoomed past this point in the data
//				System.err.println("Histogram index "+index+" < 0 for value "+data[d]+" minValue="+minDataValue+" interval="+interval);
				continue;
			}
			if (index >= categories.length) {
				// This just means we've zoomed past this point in the data
//				System.err.println("Histogram index "+index+" > categories for value "+data[d]+" minValue="+minDataValue+" interval="+interval);
				continue;
			}
			categories[index].count++;
		}
		
		mainHistogramPanel.setData(categories);
		
		calculatingCategories = false;
//		System.out.println("Finished counting");
		
	}
	
	/**
	 * The Class MainHistogramPanel.
	 */
	private class MainHistogramPanel extends JPanel implements MouseListener,MouseMotionListener {
		
		private static final int Y_AXIS_SPACE = 30;
		private int X_AXIS_SPACE = 50; // This will vary once we've calculated the scale
		
		
		/** The categories. */
		private HistogramCategory [] categories = new HistogramCategory[0];
		
		/** The selected category. */
		private HistogramCategory selectedCategory = null;
		
		/** The max count. */
		private int maxCount;
		
		/**
		 * Instantiates a new main histogram panel.
		 */
		public MainHistogramPanel () {
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		
		public void exportData (File file) throws IOException {
		
			PrintWriter pr = new PrintWriter(file);
			
			pr.println("Lower Bound\tUpper Bound\tCount");
			
			for (int c=0;c<categories.length;c++) {
				pr.println(categories[c].minValue+"\t"+categories[c].maxValue+"\t"+categories[c].count);
			}
			
			
			pr.close();
		
		}
		
		
		/**
		 * Sets the data.
		 * 
		 * @param categories the new data
		 */
		public void setData (HistogramCategory [] categories) {
			this.categories = categories;
			maxCount = 0;
			for (int c=0;c<categories.length;c++) {
				if (categories[c].count > maxCount) maxCount = categories[c].count;
			}
			
			repaint();
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		public void paintComponent (Graphics g) {
			super.paintComponent(g);
			
			// We want a white background
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			
			// Draw the graph axes first.  We leave a border on all sides
			g.setColor(Color.BLACK);
			
			// We need to know the y-scaling to be able to leave enough space
			AxisScale yAxisScale = new AxisScale(0, maxCount);

			X_AXIS_SPACE = yAxisScale.getXSpaceNeeded()+10;

			g.drawLine(X_AXIS_SPACE, 5, X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE);
			g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth()-5, getHeight()-Y_AXIS_SPACE);
			
			// If we don't have any data we can stop here
			if (categories == null) return;
			
			// If we have a stupid scale we can also stop here
			if (Double.isInfinite(currentMaxValue) || Double.isNaN(currentMaxValue) || Double.isInfinite(currentMinValue) || Double.isNaN(currentMinValue)) {
				System.err.println("Had infinite or NaN ends to the scale - not going to try to draw that");
				return;
			}
			
			// If we're making a selection then draw the selected region
			if (isSelecting) {
				g.setColor(ColourScheme.DRAGGED_SELECTION);
				g.fillRect(Math.min(selectionStart,selectionEnd), 5, Math.abs(selectionStart-selectionEnd), getHeight()-(Y_AXIS_SPACE+5));
				g.setColor(Color.BLACK);
			}
			

			// We need the scaling factor for the y-axis
			double yScale = 0;
			
			if (logCheckBox.isSelected()) {
				yScale = (double)(getHeight()-(5+Y_AXIS_SPACE))/Math.log(maxCount+1);
			}
			else {
				yScale = (double)(getHeight()-(5+Y_AXIS_SPACE))/maxCount;
			}
			
			// Now draw the scale on the y axis
			
			double currentYValue = yAxisScale.getStartingValue();
			
			while (currentYValue < maxCount) {
				
				double yHeight = 0;
				if (logCheckBox.isSelected()) {
					yHeight = Math.log(currentYValue+1)*yScale;
				}
				else {
					yHeight = currentYValue*yScale;
				}
				
				String yText = yAxisScale.format(currentYValue);
				g.drawString(yText, (X_AXIS_SPACE - 3) - g.getFontMetrics().stringWidth(yText), (int)((getHeight()-Y_AXIS_SPACE)-yHeight)+(g.getFontMetrics().getAscent()/2));

				// Put a line across the plot
				if (currentYValue != 0) {
					g.setColor(Color.LIGHT_GRAY);
					g.drawLine(X_AXIS_SPACE, (int)((getHeight()-Y_AXIS_SPACE)-yHeight), getWidth()-5, (int)((getHeight()-Y_AXIS_SPACE)-yHeight));
					g.setColor(Color.BLACK);
				}
				
				currentYValue += yAxisScale.getInterval();
			}
			
			// Now draw the scale on the x axis
			if (categories.length>0) {
				AxisScale xAxisScale = new AxisScale(currentMinValue, currentMaxValue);
								
				double currentXValue = xAxisScale.getStartingValue();
				while (currentXValue < currentMaxValue) {
					g.drawString(xAxisScale.format(currentXValue), getX(currentXValue), (int)((getHeight()-Y_AXIS_SPACE)+15));
					
					currentXValue += xAxisScale.getInterval();
					
				}
			}
			
			
			
			// Now we can draw the different categories
			for (int c=0;c<categories.length;c++) {
				categories[c].xStart = getX(categories[c].minValue);;
				categories[c].xEnd = getX(categories[c].maxValue);
				
				if (categories[c] == selectedCategory) {
					g.setColor(ColourScheme.HIGHLIGHTED_HISTOGRAM_BAR);
				}
				else {
					g.setColor(ColourScheme.HISTOGRAM_BAR);
				}
				
				double ySize = 0;
				if (logCheckBox.isSelected()) {
					ySize = Math.log(categories[c].count+1)*yScale;
				}
				else {
					ySize = categories[c].count*yScale;
				}
				g.fillRect(categories[c].xStart, (int)((getHeight()-Y_AXIS_SPACE)-ySize), categories[c].xEnd-categories[c].xStart, (int)(ySize));
				
				
				// Draw a box around it
				g.setColor(Color.BLACK);
				g.drawRect(categories[c].xStart, (int)((getHeight()-Y_AXIS_SPACE)-ySize), categories[c].xEnd-categories[c].xStart, (int)(ySize));
				
			}
			
		}
		
		public int getX (double xValue) {
			return X_AXIS_SPACE + (int)(((getWidth()-(X_AXIS_SPACE+5))/(currentMaxValue-currentMinValue))*(xValue-currentMinValue));
		}
		
		public double getXValue (int xPosition) {			
			return ((((double)(xPosition-X_AXIS_SPACE))/(getWidth()-(X_AXIS_SPACE+5)))*(currentMaxValue-currentMinValue))+currentMinValue;			
		}
		
		
		/* (non-Javadoc)
		 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
		 */
		public void mouseDragged(MouseEvent me) {
			if (isSelecting) {
				int x=me.getX();
				if (x<X_AXIS_SPACE) x = X_AXIS_SPACE;
				if (x>getWidth()-5) x = getWidth()-5;
				selectionEnd = x;
				repaint();
			}
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
		 */
		public void mouseMoved(MouseEvent me) {

			// If we're outside the main plot area we don't need to worry about it
			if (me.getX() < 5 || me.getX() > getWidth()-5 || me.getY()<5 || me.getY() > getHeight()-5) {
				if (selectedCategory != null) {
					selectedCategory = null;
					statusPanel.setSelectedCategory(null);
					repaint();
				}
				
				return;
			}
			
			// Check to see if we're in one of the categories
			for (int c=0;c<categories.length;c++) {
				if (me.getX() >= categories[c].xStart && me.getX()<=categories[c].xEnd) {
					// We don't worry about being inside the bar on the y-axis
					
					if (categories[c] != selectedCategory) {
						selectedCategory = categories[c];
						statusPanel.setSelectedCategory(selectedCategory);
						repaint();
					}
					
					return;
				}
			}
			
			
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		public void mouseClicked(MouseEvent arg0) {}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		public void mouseEntered(MouseEvent arg0) {}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		public void mouseExited(MouseEvent arg0) {
			selectedCategory = null;
			statusPanel.setSelectedCategory(null);
			repaint();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public void mousePressed(MouseEvent me) {
			
			// Don't do anything if they pressed the right mouse button
			if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
				return;
			}

			// If they're inside the plot area then start a selection
			if (getXValue(me.getX())>=currentMinValue && getXValue(me.getX())<= currentMaxValue) {
				selectionStart = me.getX();
				isSelecting = true;
			}
			
			
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent me) {
			
			// Zoom out if they pressed the right mouse button
			if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
				double interval = currentMaxValue-currentMinValue;
				double midPoint = currentMinValue+(interval/2);
				
				currentMinValue = Math.max(midPoint-interval, minDataValue);
				currentMaxValue = Math.min(midPoint+interval, maxDataValue);
				
				calcuateCategories(categorySlider.getValue());
				
			}
			
			// If we're selecting then make the new selection
			if (isSelecting) {
				isSelecting = false;
				if (Math.abs(selectionStart-selectionEnd) <= 3) {
					// Don't make a selection from a really small region
					return;
				}
				
				currentMinValue = getXValue(Math.min(selectionStart,selectionEnd));
				currentMaxValue = getXValue(Math.max(selectionStart,selectionEnd));

				calcuateCategories(categorySlider.getValue());

			}
			
			
		}
	
	}
	
	/**
	 * The Class HistogramCategory.
	 */
	private class HistogramCategory {
		
		/** The min value. */
		public double minValue;
		
		/** The max value. */
		public double maxValue;
		
		/** The x start. */
		public int xStart = 0;
		
		/** The x end. */
		public int xEnd = 0;
		
		/** The count. */
		public int count;
		
		/**
		 * Instantiates a new histogram category.
		 * 
		 * @param minValue the min value
		 * @param maxValue the max value
		 */
		public HistogramCategory (double minValue, double maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			count = 0;
		}
	}
	
	/**
	 * The Class StatusPanel.
	 */
	private class StatusPanel extends JPanel {
		
		/** The label. */
		private JLabel label;
				
		/**
		 * Instantiates a new status panel.
		 */
		public StatusPanel () {
			setBackground(Color.WHITE);
			setOpaque(true);
			label = new JLabel("No selected category",JLabel.LEFT);
			setLayout(new BorderLayout());
			add(label,BorderLayout.WEST);
			setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 2));
		}
		
		/**
		 * Sets the selected category.
		 * 
		 * @param category the new selected category
		 */
		public void setSelectedCategory (HistogramCategory category) {
			if (category == null) {
				label.setText("No selected Category");
			}
			else {
				label.setText("Value Range = "+df.format(category.minValue)+" to "+df.format(category.maxValue)+" Count = "+category.count);
			}
		}
		
	}




	
}
