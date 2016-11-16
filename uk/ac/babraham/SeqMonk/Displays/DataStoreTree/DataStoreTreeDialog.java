package uk.ac.babraham.SeqMonk.Displays.DataStoreTree;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.HierarchicalClusterSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class DataStoreTreeDialog extends JDialog implements ProgressListener {

	private DataStore [] stores;
	private HierarchicalClusterSet heirarchy;
	private DataStoreClusterPanel clusterPanel;
	private JSlider rValueSlider;
	
	
	public DataStoreTreeDialog (Probe [] probes, DataStore [] stores) {
		
		setTitle("Data Store Tree");
		this.stores = stores;
		
		DataStoreCorrelationDataSource cds = new DataStoreCorrelationDataSource(probes, stores);
		
		heirarchy = new HierarchicalClusterSet(cds);
		heirarchy.addListener(new ProgressDialog("Clustering...",heirarchy));
		heirarchy.addListener(this);
		heirarchy.startClustering();
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
	}

	public void progressExceptionReceived(Exception e) {
		setVisible(false);
		dispose();
	}

	public void progressWarningReceived(Exception e) {}

	public void progressUpdated(String message, int current, int max) {}

	public void progressCancelled() {
		setVisible(false);
		dispose();
	}

	public void progressComplete(String command, Object result) {

		getContentPane().setLayout(new BorderLayout());
		clusterPanel = new DataStoreClusterPanel((ClusterPair)result,stores);
		getContentPane().add(clusterPanel, BorderLayout.CENTER);
		
		rValueSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
		rValueSlider.setPaintTicks(true);
		getContentPane().add(rValueSlider, BorderLayout.NORTH);
		rValueSlider.addChangeListener(new ChangeListener() {
			
			public void stateChanged(ChangeEvent e) {
				clusterPanel.setRProportion(rValueSlider.getValue()/1000f);
			}
		});
		
		JPanel buttonPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		
		buttonPanel.add(closeButton);
		
		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				ImageSaver.saveImage(clusterPanel);
			}
		});
		
		buttonPanel.add(saveImageButton);
		
		JButton createRepSetsButton = new JButton("Split data stores");
		createRepSetsButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				// TODO: Ask for min count per store
				DataStore [][] splitStores = clusterPanel.getSplitStores(2);
				
				for (int s=0;s<splitStores.length;s++) {
					SeqMonkApplication.getInstance().dataCollection().addReplicateSet(new ReplicateSet("Cluster Group "+(s+1), splitStores[s]));
				}
				
				JOptionPane.showMessageDialog(DataStoreTreeDialog.this, "Created "+splitStores.length+" new replicate sets", "Splitting Complete", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		buttonPanel.add(createRepSetsButton);

		JButton reorderStoresButton = new JButton("Reorder stores");
		reorderStoresButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				DataStore [] orderedStores = clusterPanel.getOrderedStores();

				SeqMonkApplication.getInstance().setDrawnDataStores(orderedStores);
				
			}
		});
		
		buttonPanel.add(reorderStoresButton);

		
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		setSize(800,800);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}

	
	
}
