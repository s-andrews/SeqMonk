/**
 * Copyright Copyright 2016-18 Simon Andrews
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
