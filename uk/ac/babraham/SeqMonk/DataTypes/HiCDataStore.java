/**
 * Copyright 2011-15 Simon Andrews
 *
 *    This file is part of SeqMonk.
 *
 *    SeqMonk is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SeqMonk; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.SeqMonk.DataTypes;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;

public interface HiCDataStore {

	
	/**
	 * This method is used by collections (Groups, Replicate Sets) where
	 * they may or may not be composed of HiC data stores. They can implement
	 * this interface but then use this method to say if they can currently
	 * provide HiC information.
	 * 
	 * @return
	 */
	public boolean isValidHiC();
	
	public HiCHitCollection getHiCReadsForProbe (Probe p);
	
	public int getHiCReadCountForProbe(Probe p);
	
	public int getHiCReadCountForChromosome(Chromosome c);
	
	public HiCHitCollection getHiCReadsForChromosome (Chromosome c);
	
	/**
	 * This method is used by classes which want to export a non redundant
	 * set of read pairs, normally for saving or reimporting.  In normal
	 * HiC data structures each pair is duplicated to allow lookups in both
	 * the forward and reverse direction.  This call will return only one
	 * of these, but will not necessarily preserve the order (read 1 vs read 2)
	 * of the original import
	 * @param c The chromosome to query
	 * @return A hit collection of the reads to export
	 */
	public HiCHitCollection getExportableReadsForChromosome (Chromosome c);
	
	public String name();
	
	/*
	 * Returns the total number of hiC pairs in this data.  Should be half the
	 * number of reads
	 */
	public int getTotalPairCount ();
	
	public float getCorrectionForLength(Chromosome c, int minDist, int maxDist);
	
	/**
	 * Counts the number of reads (fragment ends) on this chromosome which participate
	 * in a cis interaction.
	 * 
	 * @param c the chromosome
	 * @return the number of reads in cis
	 */
	public int getCisCountForChromosome (Chromosome c);

	/**
	 * Counts the number of reads (fragment ends) on this chromosome which participate
	 * in a trans interaction.
	 * 
	 * @param c the chromosome
	 * @return the number of reads in trans
	 */
	public int getTransCountForChromosome (Chromosome c);

	/**
	 * Gets the total number of reads in cis in the whole genome
	 * 
	 * @return
	 */
	public int getCisCount();
	
	/**
	 * Gets the total number of reads in trans in the whole genome
	 * @return
	 */
	public int getTransCount();
}
