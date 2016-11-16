/**
 * Copyright 2014-15-15 Laura Biggins
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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

// maybe rename this to GiraphPlotDialog
/** This is where we can make it take different types of input, starting with probelists. 
 * 
 *  call a new panel, set the clusters calculating
 * */

public class GiraphPlot extends JDialog implements ActionListener{
	
	GiraphPlotPanel giraphPlotPanel;
	JToggleButton displayNamesButton;
	
	public GiraphPlot (ProbeList [] probeLists){
		
		super(SeqMonkApplication.getInstance(),"Giraph plot");
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JPanel plotPanel = new JPanel();
		plotPanel.setLayout(new BorderLayout());
		
		giraphPlotPanel = new GiraphPlotPanel(probeLists);  
		plotPanel.add(giraphPlotPanel, BorderLayout.CENTER);
				
		//getContentPane().add(giraphPlotPanel,BorderLayout.CENTER);
			
		JPanel buttonPanel = new JPanel();
		
		buttonPanel.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		c1.insets = new Insets(2,2,2,2);
		c1.gridx=0;
		c1.gridy=0;
		
		displayNamesButton = new JToggleButton("Display names");
		displayNamesButton.addActionListener(this);
		displayNamesButton.setActionCommand("display_names");
		
		buttonPanel.add(displayNamesButton, c1);
		
		c1.gridx++;
		
		JButton saveImageButton = new JButton("Save image");
		saveImageButton.addActionListener(this);
		saveImageButton.setActionCommand("save_image");
		
		buttonPanel.add(saveImageButton, c1);
		
		c1.gridx++;
		
		
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(this);
		stopButton.setActionCommand("stop_moving");
		
		buttonPanel.add(stopButton, c1);
		
		c1.gridx++;
		
		JButton goButton = new JButton("Go");		
		goButton.addActionListener(this);
		goButton.setActionCommand("start_moving");
		
		buttonPanel.add(goButton, c1);
		
		plotPanel.add(buttonPanel, BorderLayout.PAGE_END);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(plotPanel,BorderLayout.CENTER);
		
		setSize(600, 600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);	
	}

	public void actionPerformed(ActionEvent ae) {
		
		if (ae.getActionCommand().equals("stop_moving")){
			giraphPlotPanel.calculating = false;
		}
		else if (ae.getActionCommand().equals("start_moving")){
			giraphPlotPanel.restart();
		}
		else if (ae.getActionCommand().equals("display_names")){
			
			if(giraphPlotPanel.displayNames){
				giraphPlotPanel.displayNames = false;
				giraphPlotPanel.repaint();
				displayNamesButton.setText("display names");
			}
			else{
				giraphPlotPanel.displayNames = true;
				giraphPlotPanel.repaint();
				displayNamesButton.setText("remove names");
			}			
		}
		else if (ae.getActionCommand().equals("save_image")){
			giraphPlotPanel.exportImage = true;
			ImageSaver.saveImage(giraphPlotPanel);
			giraphPlotPanel.exportImage = false;
		}
		
	}	
}
