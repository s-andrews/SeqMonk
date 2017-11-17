package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;

public class ReplicateSetSelector extends JDialog {

	private boolean madeASelection = false;
	private JList<ReplicateSet> repSetList;
	
	private ReplicateSetSelector () {
		
		super(SeqMonkApplication.getInstance(),"Select RepSets");
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setModal(true);
		
	
		ReplicateSet [] allRepSets = SeqMonkApplication.getInstance().dataCollection().getAllReplicateSets();
		
		repSetList = new JList<ReplicateSet>(allRepSets);
		repSetList.setCellRenderer(new TypeColourRenderer());
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(repSetList), BorderLayout.CENTER);
		
		JButton selectButton = new JButton("Select Rep Sets");
		selectButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				madeASelection = true;
				setVisible(false);
			}
		});
		
		getContentPane().add(selectButton, BorderLayout.SOUTH);
		
		setSize(200,300);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
				
	}
	
	private ReplicateSet [] getRepSets() {
		if (madeASelection) {
			Object [] values = repSetList.getSelectedValues();
			ReplicateSet [] returnValues = new ReplicateSet [values.length];
			for (int i=0;i<values.length;i++) {
				returnValues[i] = (ReplicateSet)values[i];
			}
			return returnValues;
		}
		else {
			return new ReplicateSet[0];
		}
	}
	
	
	public static ReplicateSet [] selectReplicateSets () {
		
		ReplicateSetSelector rs = new ReplicateSetSelector();
		rs.setVisible(true);
		
		ReplicateSet [] repSets = rs.getRepSets();
		rs.dispose();
		
		return(repSets);
		
	}
	
	
}
