/**
 * Copyright 2011-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Pipelines;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Quantitation.BasePairQuantitation;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * The Wiggle pipeline generates a continuous set of probes over either the genome,
 * the current chromosome or the currently visible region.
 * 
 * @author andrewss
 *
 */
public class WigglePipeline extends Pipeline implements ProgressListener {

	private WiggleOptionsPanel optionsPanel;
	private SeqMonkApplication application;
	private BasePairQuantitation bpq = null;
	
	private static final int TARGET_SIZE = 2000000;
	
	public WigglePipeline (SeqMonkApplication application) {
		super(application.dataCollection());
		this.application = application;
		optionsPanel = new WiggleOptionsPanel();
	}
	
	public JPanel getOptionsPanel(SeqMonkApplication application) {
		return optionsPanel;
	}
	
	public boolean createsNewProbes () {
		return true;
	}


	public boolean isReady() {
		return true;
	}

	protected void startPipeline() {

		// We first need to generate probes over all of the features listed in
		// the feature types.  The probes should cover the whole area of the
		// feature regardless of where it splices.
		
		boolean logTransform = optionsPanel.logTransform();
		boolean correctTotal = optionsPanel.correctTotal();
		int probeSize = optionsPanel.probeSize();
		int stepSize = optionsPanel.stepSize();
		
		
		super.progressUpdated("Making probes", 0, 1);

		Probe [] probes = null;
		
		String region = optionsPanel.getRegion();
		try {
		if (region.equals("Whole Genome")) {
			probes = makeGenomeProbes(probeSize, stepSize);
		
		}
		else if (region.equals("Current Chromosome")) {
			probes = makeChromosomeProbes(DisplayPreferences.getInstance().getCurrentChromosome(), probeSize, stepSize);
		}
		else if (region.equals("Currently Visible Region")) {
			probes = makeVisibleProbes(DisplayPreferences.getInstance().getCurrentChromosome(), SequenceRead.start(DisplayPreferences.getInstance().getCurrentLocation()), SequenceRead.end(DisplayPreferences.getInstance().getCurrentLocation()), probeSize, stepSize);
		}
		}
		catch (SeqMonkException sme) {
			progressExceptionReceived(sme);
			return;
		}
				
		collection().setProbeSet(new ProbeSet("Wiggle probes", probes));
		
		// Having made probes we now need to quantitate them.  We run the base pair quantitation
		// method from inside the pipeline rather than doing this again ourselves.
		if (cancel) {
			progressCancelled();
			return;
		}
		
		bpq = new BasePairQuantitation(application);
		
		bpq.addProgressListener(this);
		
		bpq.quantitate(collection(),data,QuantitationStrandType.getTypeOptions()[0],correctTotal,false,false,false,logTransform,false);
		
	}
	
	public void cancel () {
		super.cancel();
		
		// We have to override this method so we can see cancel
		// messages come in to pass them on to the base pair
		// quantitation section which otherwise wouldn't cancel
		// until it had completed.
		
		if (bpq != null) {
			bpq.cancel();
		}
		
	}
	
	private Probe [] makeGenomeProbes (int size, int step) throws SeqMonkException {
		Chromosome [] chrs = collection().genome().getAllChromosomes();
		
		Vector<Probe> probes = new Vector<Probe>();
		for (int c=0;c<chrs.length;c++) {
			
			super.progressUpdated("Making probes", c, chrs.length*2);

			if (cancel) {
				return new Probe[0];
			}
			
			Probe [] chrProbes = makeChromosomeProbes(chrs[c], size, step);
			for (int i=0;i<chrProbes.length;i++) {
				probes.add(chrProbes[i]);
			}
		}
		
		return probes.toArray(new Probe[0]);
	}
	
	private Probe [] makeChromosomeProbes (Chromosome chromosome, int size, int step) throws SeqMonkException {
		return makeVisibleProbes(chromosome, 1, chromosome.length(), size, step);
	}
	
	private Probe [] makeVisibleProbes (Chromosome chromosome, int start, int end, int size, int step) throws SeqMonkException {
		
		Vector<Probe> probes = new Vector<Probe>();
		
		while (start < end-(size+1)) {
			probes.add(new Probe(chromosome, start,start+(size-1)));
			start += step;
		}
		
		return probes.toArray(new Probe[0]);
	}
	
	public String name() {
		return "Wiggle Plot for Initial Data Inspection";
	}
	
	
	public void progressWarningReceived(Exception e) {
		// Pass this along
		progressWarningReceived(e);
	}
	
	public void progressExceptionReceived(Exception e) {
		// Pass this along
		super.progressExceptionReceived(e);
	}

	public void progressComplete(String command, Object result) {
		// We can now report that the quantitation is complete
		
		quantitatonComplete();
	}
	
	public void progressUpdated (String message, int current, int total) {
		// Pass this on with an offset
		super.progressUpdated(message, total+current, total*2);
	}
	
	public void progressCancelled () {
		super.progressCancelled();
	}


	private class WiggleOptionsPanel extends JPanel implements ActionListener {
		
		JComboBox regionBox;
		JCheckBox logTransformBox;
		JCheckBox correctTotalBox;
		JTextField sizeField;
		JTextField stepField;
		
		public WiggleOptionsPanel () {
	
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			add(new JLabel("Region to cover "),gbc);
			
			gbc.gridx=2;
			
			regionBox = new JComboBox(new String [] {"Currently Visible Region","Current Chromosome","Whole Genome"});
			regionBox.addActionListener(this);
			add(regionBox,gbc);
	
			gbc.gridy++;
			gbc.gridx=1;
			
			add(new JLabel("Probe Width"),gbc);
			
			gbc.gridx=2;
	
			sizeField = new JTextField();
			sizeField.addKeyListener(new NumberKeyListener(false, false));
			add(sizeField,gbc);
	
			gbc.gridy++;
			gbc.gridx=1;
			
			add(new JLabel("Step Size"),gbc);
			
			gbc.gridx=2;
	
			stepField = new JTextField();
			stepField.addKeyListener(new NumberKeyListener(false, false));
			
			add(stepField,gbc);
			
			gbc.gridy++;
			gbc.gridx=1;
			
			add(new JLabel("Total Count Correction"),gbc);
			
			gbc.gridx=2;
			
			correctTotalBox = new JCheckBox();
			correctTotalBox.setSelected(true);
			
			add(correctTotalBox,gbc);
	
			gbc.gridy++;
			gbc.gridx=1;
			
			add(new JLabel("Log transform"),gbc);
			
			gbc.gridx=2;
			
			logTransformBox = new JCheckBox();
			
			add(logTransformBox,gbc);
			
			actionPerformed(null);
			
		}
		
		public String getRegion () {
			return (String)regionBox.getSelectedItem();
		}
				
		public boolean logTransform () {
			return logTransformBox.isSelected();
		}
		
		public boolean correctTotal () {
			return correctTotalBox.isSelected();
		}
		
		public int probeSize () {
			if (sizeField.getText().trim().length() == 0) {
				actionPerformed(null);
			}
			
			return Integer.parseInt(sizeField.getText());
		}
		
		public int stepSize () {
			if (stepField.getText().trim().length()==0) {
				return probeSize();
			}
			
			return Integer.parseInt(stepField.getText());
		}
		
	
		public void actionPerformed(ActionEvent e) {
			String region = (String)regionBox.getSelectedItem();
			if (region.equals("Whole Genome")) {
				updateSize(collection().genome().getTotalGenomeLength());
			}
			else if (region.equals("Current Chromosome")) {
				updateSize(DisplayPreferences.getInstance().getCurrentChromosome().length());
			}
			else if (region.equals("Currently Visible Region")) {
				updateSize(SequenceRead.length(DisplayPreferences.getInstance().getCurrentLocation()));
			}
		}
		
		private void updateSize(long totalLength) {
			// We aim for the target number of probes to cover whatever region we're looking at
			totalLength /= TARGET_SIZE;
			
			totalLength+=1;
			
			sizeField.setText(""+totalLength);
			stepField.setText(""+totalLength);
		}
		
	}

}
