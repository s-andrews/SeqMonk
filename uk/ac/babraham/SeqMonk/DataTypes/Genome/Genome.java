package uk.ac.babraham.SeqMonk.DataTypes.Genome;

import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;

public interface Genome {

	public Chromosome getExactChromsomeNameMatch (String name);
	public AnnotationCollection annotationCollection ();
	public int getChromosomeCount ();
	public boolean hasChromosome (Chromosome c);
	public Chromosome [] getAllChromosomes ();
	public ChromosomeWithOffset getChromosome (String name);
	public String [] listUnloadedFeatureTypes ();
	public long getTotalGenomeLength ();
	public String species ();
	public String assembly ();
	public String toString ();
	public int getLongestChromosomeLength ();
	
}
