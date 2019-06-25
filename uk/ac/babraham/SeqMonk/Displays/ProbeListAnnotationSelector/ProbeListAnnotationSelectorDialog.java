package uk.ac.babraham.SeqMonk.Displays.ProbeListAnnotationSelector;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.DataViewer.DataTreeRenderer;
import uk.ac.babraham.SeqMonk.Displays.DataViewer.ProbeSetTreeModel;

public class ProbeListAnnotationSelectorDialog extends JDialog implements ActionListener,TreeSelectionListener {
	
	JTree tree;
	
	JList<ProbeListAnnotation> currentListAnnotations;
	DefaultListModel<ProbeListAnnotation> currentListAnnotationsModel;
	
	JList<ProbeListAnnotation> selectedListAnnotations;
	DefaultListModel<ProbeListAnnotation> selectedListAnnotationsModel;
	JButton addButton;
	JButton removeButton;
	
	public ProbeListAnnotationSelectorDialog () {
		this(new ProbeListAnnotation[0]);
	}
	
	public ProbeListAnnotationSelectorDialog (ProbeListAnnotation [] alreadySelected) {
		
		super(SeqMonkApplication.getInstance(),"Select Annotations",true);
		
		getContentPane().setLayout(new BorderLayout());
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		// Top left is tree of probe lists
		tree = new JTree(new ProbeSetTreeModel(SeqMonkApplication.getInstance().dataCollection()));
		tree.setCellRenderer(new DataTreeRenderer());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.getSelectionModel().addTreeSelectionListener(this);
		for (int i = 0; i < tree.getRowCount(); i++) {
		    tree.expandRow(i);
		}
		
		mainPanel.add(new JScrollPane(tree),BorderLayout.CENTER);

		// Top right is a list of the annotations within the list
		currentListAnnotationsModel = new DefaultListModel<ProbeListAnnotation>();
		currentListAnnotations = new JList<ProbeListAnnotation>(currentListAnnotationsModel);

		mainPanel.add(new JScrollPane(currentListAnnotations),BorderLayout.EAST);
		
		// Bottom is the set of selected lists
		JPanel selectedPanel = new JPanel();
		selectedPanel.setLayout(new BorderLayout());
		selectedListAnnotationsModel = new DefaultListModel<ProbeListAnnotation>();
		selectedListAnnotations = new JList<ProbeListAnnotation>(selectedListAnnotationsModel);
		for (int s=0;s<alreadySelected.length;s++) {
			selectedListAnnotationsModel.addElement(alreadySelected[s]);
		}
		
		selectedPanel.add(new JScrollPane(selectedListAnnotations),BorderLayout.CENTER);
		JPanel addRemovePanel = new JPanel();
		addButton = new JButton("Add Annotations");
		addButton.setActionCommand("add");
		addButton.addActionListener(this);
		addRemovePanel.add(addButton);
		
		removeButton = new JButton("Remove Annotations");
		removeButton.setActionCommand("remove");
		removeButton.addActionListener(this);
		addRemovePanel.add(removeButton);
		
		selectedPanel.add(addRemovePanel,BorderLayout.NORTH);
		
		mainPanel.add(selectedPanel,BorderLayout.SOUTH);
		
		// The button at the bottom
		
		JPanel buttonPanel = new JPanel();
		
		JButton selectButton = new JButton("Select annotations");
		selectButton.setActionCommand("select");
		selectButton.addActionListener(this);
		buttonPanel.add(selectButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		getContentPane().add(mainPanel,BorderLayout.CENTER);
		
		
		setSize(600,600);
		
		
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String ac = ae.getActionCommand();
		
		if (ac.equals("select")) {
			setVisible(false);
		}
		else if (ac.equals("add")) {
			// Add all selected current annotations and remove them from this list.
			ProbeListAnnotation [] selected = currentListAnnotations.getSelectedValuesList().toArray(new ProbeListAnnotation[0]);
			
			for (int s=0;s<selected.length;s++) {
				selectedListAnnotationsModel.addElement(selected[s]);
			}
			for (int s=0;s<selected.length;s++) {
				currentListAnnotationsModel.removeElement(selected[s]);
			}
		}
		else if (ac.equals("remove")) {
			ProbeListAnnotation [] selected = selectedListAnnotations.getSelectedValuesList().toArray(new ProbeListAnnotation[0]);
			
			for (int s=0;s<selected.length;s++) {
				selectedListAnnotationsModel.removeElement(selected[s]);
			
				// If we're removing something from the currently selected list then
				// put it back into the available list
				if (tree.getSelectionPath() != null && selected[s].list().equals(tree.getSelectionPath().getLastPathComponent())) {
					currentListAnnotationsModel.addElement(selected[s]);
				}
			}
		}
		
	}
	
	private ProbeListAnnotation [] getCurrentSelection () {
		ProbeListAnnotation [] selected = new ProbeListAnnotation[selectedListAnnotationsModel.getSize()];
		for (int i=0;i<selected.length;i++) {
			selected[i] = selectedListAnnotationsModel.elementAt(i);
		}
		
		return selected;
	}
	
	private boolean selectedContains (ProbeListAnnotation a) {
		ProbeListAnnotation [] selected = getCurrentSelection();
		
		for (int i=0;i<selected.length;i++) {
			if (selected[i].list()==a.list() && selected[i].annotation() == a.annotation()) {
				return true;
			}
		}
		
		return false;
	}
	
	
	public ProbeListAnnotation [] getAnnotations () {
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true); // It's modal so it will stall here
		
		return(getCurrentSelection());
		
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		// Clear the current list
		currentListAnnotationsModel.clear();
		
		// See if we have a new list selected
		if (e.getPath() != null) {
			
			ProbeList thisList = (ProbeList)e.getPath().getLastPathComponent();
			
			for (int i=0;i<thisList.getValueNames().length;i++) {
				ProbeListAnnotation newAnnot = new ProbeListAnnotation(thisList, thisList.getValueNames()[i]);
				
				if (!selectedContains(newAnnot)) {
					currentListAnnotationsModel.addElement(newAnnot);
				}
			}
		}
	}
	

}
 