package uk.ac.babraham.SeqMonk.Vistory;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class VistoryProjectSummary {

	public static void addProjectSummary () {
		Vistory v = Vistory.getInstance();
		
		v.addBlock(new VistoryTitle("Project Summary",2));

		v.addBlock(new VistoryTitle("Basic Project Info",3));
		v.addBlock(new VistoryTable(basicInfo()));

		if (SeqMonkApplication.getInstance().dataCollection().getAllDataSets().length > 0) {
			v.addBlock(new VistoryTitle("Data Sets",3));
			v.addBlock(new VistoryTable(dataSets()));
		}
		
		if (SeqMonkApplication.getInstance().dataCollection().getAllDataGroups().length > 0) {
			v.addBlock(new VistoryTitle("Data Groups",3));
			v.addBlock(new VistoryTable(dataGroups()));
		}
		
		if (SeqMonkApplication.getInstance().dataCollection().getAllReplicateSets().length > 0) {
			v.addBlock(new VistoryTitle("Replicate Sets",3));
			v.addBlock(new VistoryTable(replicateSets()));
		}
		
		if (SeqMonkApplication.getInstance().dataCollection().isQuantitated()) {
			v.addBlock(new VistoryTitle("Quantitation",3));
			v.addBlock(new VistoryTable(probeLists()));
		}
		
	}
	
	private static String [][] basicInfo () {
		String [][] data = new String [5][2];
	
		SeqMonkApplication s = SeqMonkApplication.getInstance();
		
		data[0][0] = "Parameter";
		data[0][1] = "Value";
		
		data[1][0] = "Project Name";
		data[1][1] = s.projectName();
		
		data[2][0] = "Vistory Name";
		if (Vistory.getInstance().saveFile() != null) {
			data[2][1] = Vistory.getInstance().saveFile().getName();
		}
		else {
			data[2][1] = "[Not saved yet]";
		}
		
		data[3][0] = "Genome";
		data[3][1] = s.dataCollection().genome().toString();
		
		data[4][0] = "SeqMonk Version";
		data[4][1] = SeqMonkApplication.VERSION;
		
		return data;
	}
	
	private static String [][] dataSets () {
		
		DataSet [] sets = SeqMonkApplication.getInstance().dataCollection().getAllDataSets();

		String [][] data = new String[sets.length+1][4];
		
		// First Row is the header
		data[0][0] = "Name";
		data[0][1] = "File Name";
		data[0][2] = "Reads";
		data[0][3] = "Import Options";
		
		for (int s=0;s<sets.length;s++) {
			data[s+1][0] = sets[s].name();
			data[s+1][1] = sets[s].fileName();
			data[s+1][2] = ""+sets[s].getTotalReadCount();
			data[s+1][3] = sets[s].importOptions();			
		}
		
		return data;
	}
	
	private static String [][] dataGroups () {
		DataGroup [] groups = SeqMonkApplication.getInstance().dataCollection().getAllDataGroups();

		int maxGroupSize = 0;
		for (int g=0;g<groups.length;g++) {
			if (groups[g].dataSets().length > maxGroupSize) maxGroupSize = groups[g].dataSets().length;
		}
		
		String [][] data = new String[maxGroupSize+1][groups.length];
		
		for (int g=0;g<groups.length;g++) {
			data[0][g] = groups[g].name();
			
			DataSet [] sets = groups[g].dataSets();
			
			for (int i=0;i<maxGroupSize;i++) {
				if (i<sets.length) {
					data[i+1][g] = sets[i].name();
				}
				else {
					data[i+1][g] = "";
				}
			}
		}
		
		return data;
	}

	private static String [][] replicateSets () {
		ReplicateSet [] repSets = SeqMonkApplication.getInstance().dataCollection().getAllReplicateSets();

		int maxGroupSize = 0;
		for (int r=0;r<repSets.length;r++) {
			if (repSets[r].dataStores().length > maxGroupSize) maxGroupSize = repSets[r].dataStores().length;
		}
		
		String [][] data = new String[maxGroupSize+1][repSets.length];
		
		for (int g=0;g<repSets.length;g++) {
			data[0][g] = repSets[g].name();
			
			DataStore [] store = repSets[g].dataStores();
			
			for (int i=0;i<maxGroupSize;i++) {
				if (i<store.length) {
					data[i+1][g] = store[i].name();
				}
				else {
					data[i+1][g] = "";
				}
			}
		}
		
		return data;
	}
	
	private static String [][] probeLists () {

		ProbeList [] lists = SeqMonkApplication.getInstance().dataCollection().probeSet().getAllProbeLists();

		String [][] data = new String[lists.length+1][5];
		
		// First Row is the header
		data[0][0] = "Name";
		data[0][1] = "Parent";
		data[0][2] = "Probes";
		data[0][3] = "Description";
		data[0][4] = "Comments";
		
		for (int l=0;l<lists.length;l++) {
			data[l+1][0] = lists[l].name();
			
			if (lists[l].parent() != null) {
				data[l+1][1] = lists[l].parent().name();
			}
			else {
				data[l+1][1] = "";
			}

			data[l+1][2] = ""+lists[l].getAllProbes().length;
			data[l+1][3] = lists[l].description();
			data[l+1][4] = lists[l].comments();
		}
		
		return data;

	
	}

	
	
	
}
