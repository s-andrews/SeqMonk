package uk.ac.babraham.SeqMonk.Importer;

import java.io.File;
import java.io.FileNotFoundException;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.AnnotationParsers.GenomeParser;
import uk.ac.babraham.SeqMonk.DataParsers.BAMFileParser;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataWriters.SeqMonkDataWriter;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * This class is a non-interactive importer used to create new seqmonk projects
 * from a list of BAM files.
 * 
 * @author Andrewss
 *
 */

public class SeqMonkImporter implements ProgressListener {

	/**
	 * The arguments are:
	 * 
	 * 1 ) The genome to use (Species/Assembly)
	 * 2 ) The output file name
	 * 3 ) Whether to split for RNA-Seq (0 = auto, 1 = no, 2 = yes)
	 * 4 ) MapQ cutoff filter
	 * 5+) List of BAM files to import 
	 * 	
	 * @param args
	 */
	
	private boolean wait = false;
	private Genome genome;
	private DataCollection data;
	
	public SeqMonkImporter (String [] args) {
		
		String genomeString = args[0];
		File outFile = new File(args[1]);
		int splitReads = Integer.parseInt(args[2]);
		int mapqCutoff = Integer.parseInt(args[3]);
		
		File [] files = new File[args.length-4];
		
		for (int i=0; i<files.length;i++) {
			files[i] = new File(args[i+4]);
		}
		
		SeqMonkPreferences prefs = SeqMonkPreferences.getInstance();
		
		
		System.err.println("Reading Genome");

		GenomeParser genomeParser = new GenomeParser();
		genomeParser.addProgressListener(this);
		wait = true;
		
		try {
			genomeParser.parseGenome(new File []{new File(prefs.getGenomeBase().getAbsolutePath()+"/"+genomeString)});
		}
		catch (FileNotFoundException fnfe) {
			System.err.println("Couldn't find genome for "+genomeString);
			fnfe.printStackTrace();
			System.exit(1);
		}

		while(wait) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		
		if (genome == null) {
			System.err.println("No genome loaded for some reason");
			System.exit(1);
		}
		
		data = new DataCollection(genome);
		
		System.err.println("Genome loaded for "+data.genome());

		System.err.println("Parsing Data");

		BAMFileParser parser = new BAMFileParser(data);
		
		for (int f=0;f<files.length;f++) {
		
			System.err.println("Parsing "+files[f].getName());
			parser.setFiles(new File[]{files[f]});
			parser.addProgressListener(this);
			wait = true;
			try {
				parser.parseData();
			} catch (SeqMonkException e) {
				e.printStackTrace();
				System.exit(1);
			}
		
			while(wait) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			}
		}
		
		SeqMonkDataWriter writer = new SeqMonkDataWriter();
		writer.addProgressListener(this);
		wait = true;
		writer.writeData(data, outFile);
		while (wait) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		
		System.err.println("All done");
	}
	
	
	public static void main(String[] args) {
		new SeqMonkImporter(args);
	}

	@Override
	public void progressExceptionReceived(Exception e) {

		e.printStackTrace();
		System.exit(1);
	}

	public void progressWarningReceived(Exception e) {
		System.err.println(e.getMessage());
	}

	public void progressUpdated(String message, int current, int max) {}

	public void progressCancelled() {}

	public void progressComplete(String command, Object result) {
				
		if (command.equals("load_genome")) {
			genome = (Genome)result;
		}
	
		if (command.equals("datasets_loaded")) {
			DataSet [] newSets = (DataSet [])result;
			
			for (int i=0;i<newSets.length;i++) {
				data.addDataSet(newSets[i]);
			}
		}
		
		wait = false;
	}

}
