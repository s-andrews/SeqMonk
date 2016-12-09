package uk.ac.babraham.SeqMonk.Filters;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.Displays.FeaturePositionSelector.FeaturePositionSelectorPanel;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class FeatureFilter extends ProbeFilter {

	private JPanel options;
	
	public FeatureFilter(DataCollection collection) throws SeqMonkException {
		super(collection);
		options = new FeatureFilterOptionsPanel();
	}

	
	protected String listName() {
		// TODO Auto-generated method stub
		return null;
	}

	protected String listDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	protected void generateProbeList() {
		// TODO Auto-generated method stub

	}

	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasOptionsPanel() {
		return true;
	}

	public JPanel getOptionsPanel() {
		return options;
	}

	public String name() {
		return "Feature Filter";
	}

	public String description() {
		return "A filter based on the relationship between probes and features";
	}

	private class FeatureFilterOptionsPanel extends JPanel {
		
		private FeaturePositionSelectorPanel featurePositions;
		private JComboBox relationshipTypeBox;
		private JTextField distanceField;
		private JComboBox directionalityBox;
		
		public FeatureFilterOptionsPanel () {
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			
			gbc.gridx=0;
			gbc.gridy=0;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridwidth=2;

			JLabel header1 = new JLabel("Define Feature Positions",JLabel.CENTER);
			header1.setFont(new Font(header1.getFont().getName(), Font.BOLD, (int)(header1.getFont().getSize()*1.5)));
			
			add(header1,gbc);
			
			gbc.gridy++;

			featurePositions = new FeaturePositionSelectorPanel(collection, false, false);
			add(featurePositions,gbc);

			
			gbc.gridy++;
			
			JLabel header2 = new JLabel("Define Relationship with Probes",JLabel.CENTER);
			header2.setFont(new Font(header2.getFont().getName(), Font.BOLD, (int)(header2.getFont().getSize()*1.5)));
			
			add(header2,gbc);
			
			gbc.gridy++;
			gbc.gridwidth = 1;
			
			add(new JLabel("Select probes which are"),gbc);
			
			gbc.gridx=1;
						
			relationshipTypeBox = new JComboBox(new String [] {
					"Overlapping",
					"Close to",
					"Exactly matching",
					"Surrounding",
					"Contained within"
			});
			
			relationshipTypeBox.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					if (distanceField == null) return;
					if (relationshipTypeBox.getSelectedItem().equals("Close to")) {
						distanceField.setEnabled(true);
					}
					else {
						distanceField.setEnabled(false);
					}
				}
			});
			
			add(relationshipTypeBox,gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			add(new JLabel("Distance cutoff (bp)"),gbc);
			
			gbc.gridx=1;
			
			distanceField = new JTextField("2000");
			distanceField.addKeyListener(new NumberKeyListener(false, false));
			distanceField.setEnabled(false);
			
			add(distanceField,gbc);
			
		}
		
		public Dimension getPreferredSize () {
			return new Dimension(600,300);
		}
		
	}
	
	
}
