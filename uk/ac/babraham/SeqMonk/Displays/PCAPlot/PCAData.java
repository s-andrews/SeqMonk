package uk.ac.babraham.SeqMonk.Displays.PCAPlot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressRecordDialog;
import uk.ac.babraham.SeqMonk.R.RProgressListener;
import uk.ac.babraham.SeqMonk.R.RScriptRunner;
import uk.ac.babraham.SeqMonk.Utilities.FloatVector;
import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

public class PCAData implements Runnable {

	private ProgressDialog pd;
	private DataStore [] stores;
	private ProbeList probeList;
	
	private float [] variances;
	private float [][] pcaResults;
	private float [][] pcaRotations;

	
	public PCAData (ProbeList probes,DataStore [] stores) {
		this.stores = stores;
		this.probeList = probes;
		
		pd = new ProgressDialog("Running PCA...");
		
		Thread t = new Thread(this);
		t.start();
		
	}
	
	
	private void runPCA () {
		
		File tempDir;
		
		Probe [] probes = probeList.getAllProbes();
		
		try {

			pd.progressUpdated("Creating temp directory",0,1);

			tempDir = TempDirectory.createTempDirectory();

//			System.err.println("Temp dir is "+tempDir.getAbsolutePath());

			pd.progressUpdated("Writing R script",0,1);
			// Get the template script
			Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Displays/PCAPlot/pca_template.r"));

			// Substitute in the variables we need to change
			template.setValue("WORKING", tempDir.getAbsolutePath().replace("\\", "/"));

			// Write the script file
			File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
			PrintWriter pr = new PrintWriter(scriptFile);
			pr.print(template.toString());
			pr.close();

			// Write the count data
			File countFile = new File(tempDir.getAbsoluteFile()+"/data.txt");

			pr = new PrintWriter(countFile);

			StringBuffer sb;

			pd.progressUpdated("Writing count data",0,1);
			
			for (int p=0;p<probes.length;p++) {
								
				sb = new StringBuffer();
				for (int d=0;d<stores.length;d++) {
					if (d>0) sb.append("\t");
				
					sb.append(stores[d].getValueForProbe(probes[p]));
				}

				pr.println(sb.toString());
			}	
			
			pr.close();

			pd.progressUpdated("Running R Script",0,1);

			RScriptRunner runner = new RScriptRunner(tempDir);
			RProgressListener listener = new RProgressListener(runner);
			runner.addProgressListener(new ProgressRecordDialog("R Session",runner));
			runner.runScript();

			while (true) {

				if (listener.cancelled()) {
					pd.progressCancelled();
					return;
				}
				if (listener.exceptionReceived()) {
					pd.progressExceptionReceived(listener.exception());
					return;
				}
				if (listener.complete()) break;

				Thread.sleep(500);
			}
			
			// We can now parse and store the results

			File varianceFile = new File(tempDir.getAbsolutePath()+"/variance_data.txt");

			BufferedReader br = new BufferedReader(new FileReader(varianceFile));

			String line = br.readLine();

			FloatVector varianceVector = new FloatVector();
			
			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");
				varianceVector.add(Float.parseFloat(sections[1]));			
			}
						
			br.close();
			variances = varianceVector.toArray();
			
			// We want to convert the variance values into percentages
			// since that's what everyone wants for PCA.
			
			float varianceTotal = 0;
			for (int v=0;v<variances.length;v++) {
				varianceTotal += variances[v];
			}

			// Now convert to percentages
			for (int v=0;v<variances.length;v++) {
				variances[v] = (variances[v]/varianceTotal)*100;
			}
			
			
			// We don't want to keep more than 10PCs
			if (variances.length > 10) {
				float [] shortVariances = new float[10];
				for (int i=0;i<shortVariances.length;i++) {
					shortVariances[i] = variances[i];
				}
				variances = shortVariances;
			}


			File pcaFile = new File(tempDir.getAbsolutePath()+"/pca_data.txt");

			br = new BufferedReader(new FileReader(pcaFile));

			line = br.readLine();

			pcaResults = new float[stores.length][variances.length];
			
			int storeIndex = 0;
			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");
				
				for (int i=0;i<variances.length;i++) {
					pcaResults[storeIndex][i] = Float.parseFloat(sections[i+1]);
				}
				
				++storeIndex;
			}
			
			br.close();			

			
			File weightsFile = new File(tempDir.getAbsolutePath()+"/pca_weights.txt");

			br = new BufferedReader(new FileReader(weightsFile));

			line = br.readLine();

			pcaRotations = new float[probeList.getAllProbes().length][variances.length];
			
			int probeIndex = 0;
			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");
				
				for (int i=0;i<variances.length;i++) {
					// We multiply by 1000 to get the values on a vaguely
					// sane scale
					pcaRotations[probeIndex][i] = Float.parseFloat(sections[i+1])*1000;
				}
				
				++probeIndex;
			}
			
			br.close();			

			
			
			runner.cleanUp();
		}
		catch (Exception e) {
			pd.progressExceptionReceived(e);
			return;
		}
	
		pd.progressComplete("pca_analysis", this);
		
		// TODO: Open the graph of variances and PCA results
		new PCAScatterPlotDialog(this);
		new PCAVarianceDialog(this);
		
	}
	
	public String probeListName () {
		return probeList.name();
	}

	public int getPCCount () {
		return variances.length;
	}

	public int getStoreCount () {
		return stores.length;
	}
	
	public int getProbeCount () {
		return probeList.getAllProbes().length;
	}
	
	public Probe [] getProbesAt (int [] indices) {
		
		Probe [] returnProbes = new Probe[indices.length];
		Probe [] allProbes = probeList.getAllProbes();
		for (int i=0;i<indices.length;i++) {
			returnProbes[i] = allProbes[indices[i]];
		}
		
		return returnProbes;
	}
	
	public DataStore getStore (int index) {
		return stores[index];
	}

	public void run() {
		runPCA();
	}
	
	public float getPCAValue(int storeIndex, int pcaIndex) {
		return pcaResults[storeIndex][pcaIndex];
	}
	
	public float [] getPCARotations(int pcaIndex) {
		float [] rotations = new float[pcaRotations.length];
		for (int i=0;i<rotations.length;i++) {
			rotations[i] = pcaRotations[i][pcaIndex];
		}
		return rotations;
	}
	
	public float getMaxPCARotation (int pcaIndex) {
		float max = pcaRotations[0][pcaIndex];

		for (int i=0;i<pcaRotations.length;i++) {
			if (pcaRotations[i][pcaIndex] > max) max = pcaRotations[i][pcaIndex];
		}
		
		return max;	
	}
	
	
	public float getMinPCARotation (int pcaIndex) {
		float min = pcaRotations[0][pcaIndex];

		for (int i=0;i<pcaRotations.length;i++) {
			if (pcaRotations[i][pcaIndex] < min) min = pcaRotations[i][pcaIndex];
		}
		
		return min;	
	}
	
	public ProbeList getProbeList (int pcaIndex,double weightCutoff) {

		float [] weights = getPCARotations(pcaIndex);
		Probe [] probes = probeList.getAllProbes();
		
		
		IntVector higherIndices = new IntVector();
		IntVector lowerIndices = new IntVector();
		
		for (int i=0;i<weights.length;i++) {
			if (weights[i] > weightCutoff) {
				higherIndices.add(i);
			}
			else {
				lowerIndices.add(i);
			}
		}

		int [] indicesToAdd;
		String direction;
		
		if (higherIndices.length() > lowerIndices.length()) {
			indicesToAdd = lowerIndices.toArray();
			direction = "below";
		}
		else {
			indicesToAdd = higherIndices.toArray();
			direction = "above";
		}
		
		
		ProbeList filteredList = new ProbeList(probeList, "PC"+(pcaIndex+1)+" weighted probes", "Probes from PC"+(pcaIndex+1)+" with a weight "+direction+" "+weightCutoff, "Weight");
		
		for (int i=0;i<indicesToAdd.length;i++) {
			filteredList.addProbe(probes[indicesToAdd[i]], weights[indicesToAdd[i]]);
		}
				
		return filteredList;	
	}

	
	public float [] variances () {
		return variances;
	}
	
	public void writeExportData (File file) throws IOException {
	
		PrintWriter pr = new PrintWriter(file);
		
		StringBuffer sb = new StringBuffer();
		sb.append("DataStore");
		for (int p=0;p<pcaResults[0].length;p++) {
			sb.append("\t");
			sb.append("PC");
			sb.append(p+1);
		}
		
		pr.println(sb.toString());

		sb = new StringBuffer();
		sb.append("VarianceExplained");
		for (int p=0;p<pcaResults[0].length;p++) {
			sb.append("\t");
			sb.append(variances[p]);
		}
		
		pr.println(sb.toString());

		
		
		for (int s=0;s<stores.length;s++) {
			sb = new StringBuffer();
			sb.append(stores[s]);
			for (int p=0;p<pcaResults[0].length;p++) {
				sb.append("\t");
				sb.append(pcaResults[s][p]);
			}
			pr.println(sb.toString());
		}
		
		pr.close();
		
	}
	
	
}