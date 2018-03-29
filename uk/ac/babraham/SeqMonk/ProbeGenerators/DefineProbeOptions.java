/**
 * Copyright Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Quantitation.Options.DefineQuantitationOptions;

/**
 * DefineProbeOptions provides the dialog box which is displayed to
 * select which of the probe generators they want to use.  It also
 * organises the display of the options panel from the various 
 * generators to set the options.  Once complete it also launches
 * the selected probe generator.
 * 
 * The list of generators is hard-coded inside this class.  There
 * is no way to alter this list from outside the class.
 */
public class DefineProbeOptions extends JDialog implements ActionListener, ProbeGeneratorListener, ListSelectionListener {

	private JPanel mainPanel;
	private ProbeGenerator [] generators;
	private JList generatorList;
	private JPanel optionPanel;
	private JButton runButton;
	private SeqMonkApplication application;

	
	/**
	 * Instantiates a new define probe options.
	 * 
	 * @param application
	 */
	public DefineProbeOptions (SeqMonkApplication application) {
		super(application,"Define Probes...");
		this.application = application;
		getContentPane().setLayout(new BorderLayout());

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		listPanel.add(new JLabel("Probe Generator Options",JLabel.CENTER),BorderLayout.NORTH);
		generators = new ProbeGenerator [] {
				new RunningWindowProbeGenerator(application.dataCollection()),
				new FeatureProbeGenerator(application.dataCollection()),
				new FeaturePercentileProbeGenerator(application.dataCollection()),
				new ContigProbeGenerator(application.dataCollection()),
				new EvenCoverageProbeGenerator(application.dataCollection()),
				new MacsPeakCaller(application.dataCollection()),
				new ReadPositionProbeGenerator(application.dataCollection()),
				new RandomProbeGenerator(application.dataCollection()),
				new ProbeListProbeGenerator(application.dataCollection()),
				new ShuffleListProbeGenerator(application.dataCollection()),
				new InterstitialProbeGenerator(application.dataCollection()),
				new DeduplicationProbeGenerator(application.dataCollection()),
				new MergeConsecutiveProbeGenerator(application.dataCollection()),
				new CurrentRegionProbeGenerator(application.dataCollection()),
		};
		
		// If we don't have an existing probe set then remove the generators
		// which require this.
		
		if (application.dataCollection().probeSet() == null) {
			
			Vector<ProbeGenerator>usableGenerators = new Vector<ProbeGenerator>();
			for (int i=0;i<generators.length;i++) {
				if (!generators[i].requiresExistingProbeSet()) {
					usableGenerators.add(generators[i]);
				}
			}
			
			generators = usableGenerators.toArray(new ProbeGenerator[0]);
		}
		
		
		generatorList = new JList(generators);
		generatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		generatorList.getSelectionModel().addListSelectionListener(this);
		listPanel.add(new JScrollPane(generatorList),BorderLayout.CENTER);
				
		mainPanel.add(listPanel,BorderLayout.WEST);
		
		getContentPane().add(mainPanel,BorderLayout.WEST);
		
		optionPanel = new JPanel();
		mainPanel.add(optionPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		
		buttonPanel.add(closeButton);

		runButton = new JButton("Create Probes");
		runButton.setActionCommand("run");
		runButton.addActionListener(this);
		runButton.setEnabled(false);
		
		buttonPanel.add(runButton);
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		optionPanel = new JPanel();
		optionPanel.add(new JLabel("Select an option"));
		getContentPane().add(optionPanel,BorderLayout.CENTER);
		
		setSize(800,480);
		setLocationRelativeTo(application);
		setVisible(true);
		
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#optionsReady()
	 */
	public void optionsReady (){
		runButton.setEnabled(true);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#optionsNotReady()
	 */
	public void optionsNotReady () {
		runButton.setEnabled(false);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("run")) {
			
			// Let's make sure they really want to do this
			if (application.dataCollection().probeSet() != null) {
				int answer = JOptionPane.showConfirmDialog(this, "This will wipe out your existing probes and quantitations.  Are you sure you want to continue?","Are you sure?",JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION) {
					return;
				}
			}
			
			runButton.setEnabled(false); // They can't do two runs at once
			generators[generatorList.getSelectedIndex()].addProbeGeneratorListener(new ProbeGeneratorProgressDialog(application,generators[generatorList.getSelectedIndex()]));
			generators[generatorList.getSelectedIndex()].addProbeGeneratorListener(this);
			generators[generatorList.getSelectedIndex()].generateProbes();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#generationComplete(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet)
	 */
	public void generationComplete(ProbeSet probes) {
		
		if (probes.getAllProbes().length == 0) {
			JOptionPane.showMessageDialog(this,"The options you chose produced no probes","No Probes Found",JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		application.dataCollection().setProbeSet(probes);
		setVisible(false);
		dispose();
		// We now need to launch the quantitation tool, otherwise this will
		// have been somewhat pointless!
		new DefineQuantitationOptions(application);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#generationCancelled()
	 */
	public void generationCancelled () {
		optionsReady();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#generationExceptionReceived(java.lang.Exception)
	 */
	public void generationExceptionReceived(Exception e) {
		// This will be handled elsewhere...
		runButton.setEnabled(true);  // Let them have another go!
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#updateGenerationProgress(java.lang.String, int, int)
	 */
	public void updateGenerationProgress(String message, int current, int total) {	
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
		// We need to add a new option panel
		getContentPane().remove(optionPanel);
		if (generatorList.getSelectedIndex() >= 0) {
			generators[generatorList.getSelectedIndex()].addProbeGeneratorListener(this);
			optionPanel = generators[generatorList.getSelectedIndex()].getOptionsPanel();
			optionPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			runButton.setEnabled(generators[generatorList.getSelectedIndex()].isReady());

			
			getContentPane().add(optionPanel,BorderLayout.CENTER);
			validate();
			repaint();
		}
	}

}
