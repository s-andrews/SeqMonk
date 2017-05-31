package uk.ac.babraham.SeqMonk.Displays.TsneDataStorePlot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.swing.JOptionPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressRecordDialog;
import uk.ac.babraham.SeqMonk.Displays.PCAPlot.PCAScatterPlotDialog;
import uk.ac.babraham.SeqMonk.Displays.PCAPlot.PCASource;
import uk.ac.babraham.SeqMonk.R.RProgressListener;
import uk.ac.babraham.SeqMonk.R.RScriptRunner;
import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

public class TsneDataStoreResult implements PCASource, Runnable {

	private ProgressDialog pd;
	private DataStore [] stores;
	private ProbeList probeList;
	private int iterations;
	private int perplexity;
	
	private Probe [] usedProbes;
	
	private float [] variances;
	private float [][] pcaResults;

	
	public TsneDataStoreResult (ProbeList probes,DataStore [] stores, int iterations, int perplexity) {
		this.stores = stores;
		this.probeList = probes;
		this.iterations = iterations;
		this.perplexity = perplexity;
			
		pd = new ProgressDialog("Running Tsne...");
	
		// Use only the stores which are quantitated
		Vector<DataStore> validStores = new Vector<DataStore>();
		
		for (int s=0;s<stores.length;s++) {
			if (stores[s].isQuantitated()) {
				validStores.add(stores[s]);
			}
		}
		
		this.stores = validStores.toArray(new DataStore[0]);
		
		if (this.stores.length < 2) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "Can't run Tsne - you need at least 2 visible, quantitated data stores to run this.","Can't run PCA",JOptionPane.ERROR_MESSAGE);
			pd.progressCancelled();
			return;
		}
		
		
		// We need to filter the probes to only get those which have valid 
		// values in all datasets otherwise we'll get a crash from R
		
		Vector<Probe> validProbes = new Vector<Probe>();
		
		usedProbes = probes.getAllProbes();
		
		PROBE: for (int p=0;p<usedProbes.length;p++) {
			for (int s=0;s<stores.length;s++) {
				try {
					float value = stores[s].getValueForProbe(usedProbes[p]);
					if (Float.isInfinite(value) || Float.isNaN(value)) {
						continue PROBE;
					}
				}
				catch (SeqMonkException sme) {
					continue PROBE;
				}
			}
			validProbes.add(usedProbes[p]);
		}

		if (validProbes.size() == 0) {
			pd.progressExceptionReceived(new SeqMonkException("There weren't any probes which had valid (non-null, non-infinite) values across all Data Sets.  Can't run PCA."));
			return;
		}
		
		if (validProbes.size() < usedProbes.length) {
			pd.progressWarningReceived(new SeqMonkException("Had to exclude "+(usedProbes.length-validProbes.size())+" probes which had invalid (null or infinite) values in at least one data store."));
			usedProbes = validProbes.toArray(new Probe[0]);
		}
		
		
		Thread t = new Thread(this);
		t.start();
		
	}
	
	
	private void runTsne () {
		
		File tempDir;
				
		try {

			pd.progressUpdated("Creating temp directory",0,1);

			tempDir = TempDirectory.createTempDirectory();

//			System.err.println("Temp dir is "+tempDir.getAbsolutePath());

			pd.progressUpdated("Writing R script",0,1);
			// Get the template script
			Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Displays/TsneDataStorePlot/tsne_template.r"));

			// Substitute in the variables we need to change
			template.setValue("WORKING", tempDir.getAbsolutePath().replace("\\", "/"));
			template.setValue("ITERATIONS",""+iterations);
			template.setValue("PERPLEXITY", ""+perplexity);

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
			
			for (int p=0;p<usedProbes.length;p++) {
								
				sb = new StringBuffer();
				for (int d=0;d<stores.length;d++) {
					if (d>0) sb.append("\t");
				
					sb.append(stores[d].getValueForProbe(usedProbes[p]));
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


			// Tsne only ever returns 2 dimensions so we'll fake up a variances
			// result which says that we only have 2 components with 50% each
			variances = new float[]{50,50};

			File tsneFile = new File(tempDir.getAbsolutePath()+"/tsne_data.txt");

			BufferedReader br = new BufferedReader(new FileReader(tsneFile));

			String line = br.readLine();

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

			// We don't get weights from Tsne

			
			
			runner.cleanUp();
		}
		catch (Exception e) {
			pd.progressExceptionReceived(e);
			return;
		}
	
		pd.progressComplete("tsne_analysis", this);
		
		new PCAScatterPlotDialog(this);
		
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
		return usedProbes.length;
	}
	
	public Probe [] getProbesAt (int [] indices) {
		
		Probe [] returnProbes = new Probe[indices.length];
		for (int i=0;i<indices.length;i++) {
			returnProbes[i] = usedProbes[indices[i]];
		}
		
		return returnProbes;
	}
	
	public DataStore getStore (int index) {
		return stores[index];
	}

	public void run() {
		runTsne();
	}
	
	public float getPCAValue(int storeIndex, int pcaIndex) {
		return pcaResults[storeIndex][pcaIndex];
	}
		
	public float [] variances () {
		return variances;
	}
	
	public void writeExportData (File file) throws IOException {
	
		PrintWriter pr = new PrintWriter(file);
		
		StringBuffer sb;
		
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


	public String getPCName(int index) {
		return "Tsne Dim"+(index+1);
	}
	
	


}
