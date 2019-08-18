package uk.ac.babraham.SeqMonk.Displays.DesignEditor;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.Dialogs.ReplicateSetSelector;

public class StatisticalDesignEditorDialog extends JDialog implements ActionListener {
	
	private StatisticalDesign design;
	
	public StatisticalDesignEditorDialog (StatisticalDesign design) {
		super(SeqMonkApplication.getInstance(), "Statistical Design Editor");
		this.design = design;
		
		Container c = getContentPane();
		
		c.setLayout(new BorderLayout());
		
		JTable table = new JTable(design);
		
		c.add(new JScrollPane(table), BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton addButton = new JButton ("Add Cofactor");
		addButton.setActionCommand("add");
		addButton.addActionListener(this);
		buttonPanel.add(addButton);
		
		JButton removeButton = new JButton("Remove Cofactor");
		removeButton.setActionCommand("remove");
		removeButton.addActionListener(this);
		buttonPanel.add(removeButton);
		
		c.add(buttonPanel,BorderLayout.NORTH);
		
		
		JPanel finishedPanel = new JPanel();
		
		JButton finishedButton = new JButton("Finished");
		finishedButton.setActionCommand("finished");
		finishedButton.addActionListener(this);
		finishedPanel.add(finishedButton);
		
		c.add(finishedPanel,BorderLayout.SOUTH);
		
		setModal(true);
		setSize(300,400);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().contentEquals("finished")) {
			setVisible(true);
			dispose();
		}
		else if (ae.getActionCommand().contentEquals("remove")) {
			design.removeFactor();
		}
		else if (ae.getActionCommand().contentEquals("add")) {
			ReplicateSet [] factorSets = ReplicateSetSelector.selectReplicateSets();
			if (factorSets != null && factorSets.length > 1) {
				try {
					design.addFactor(factorSets);
				}
				catch(InvalidFactorException ife) {
					JOptionPane.showMessageDialog(this, "Couldn't add factor: "+ife.getLocalizedMessage(), "Invalid Factor", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}
	}

}
