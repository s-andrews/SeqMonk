/**
 * Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.AnnotationParsers;

import java.io.File;
import java.util.Vector;

import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The ProbeListAnnotationParser converts an existing probe list into
 * an annotation track so you can create a new probe list but still have
 * a way to access results from previous lists.
 */
public class ProbeListAnnotationParser extends AnnotationParser {

	private ProbeList [] probeLists;
	private String listName;
	
	/**
	 * Instantiates a new ProbeList annotation parser.
	 * 
	 * @param genome The genome into which data will be put
	 */
	public ProbeListAnnotationParser (Genome genome, ProbeList [] probeLists, String listName) {
		super(genome);
		this.probeLists = probeLists;
		this.listName = listName;
	}
	
	public ProbeListAnnotationParser(Genome genome, ProbeList list, String listName) {
		this(genome, new ProbeList[] {list}, listName);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#requiresFile()
	 */
	@Override
	public boolean requiresFile() {
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#fileFilter()
	 */
	public FileFilter fileFilter() {
		return null;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#name()
	 */
	public String name() {
		return "ProbeList Parser";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#parseAnnotation(java.io.File, uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome)
	 */
	protected AnnotationSet [] parseAnnotation(File file, Genome genome) throws Exception {
		
		Vector<AnnotationSet> annotationSets = new Vector<AnnotationSet>();

		AnnotationSet currentAnnotation = new AnnotationSet(genome, listName);
		annotationSets.add(currentAnnotation);

		
		for (int l=0;l<probeLists.length;l++) {

			ProbeList probeList = probeLists[l];
			
			Probe [] probes = probeList.getAllProbes();

			// If we're only importing a single feature type
			// then we the same name as for the whole collection.
			// If we're importing multiple lists then we use the
			// name of the individual lists and let them sort it
			// out later if they don't like that.
			String featureTypeName = listName;
			
			if (probeLists.length > 1) {
				featureTypeName = probeList.name();
			}
			
			for (int p=0;p<probes.length;p++) {
		
				if (p % 1+(probes.length/100) == 0) {
					progressUpdated("Converted "+p+" probes", p, probes.length);
				}

				if (p>1000000 && p%1000000 == 0) {
					progressUpdated("Caching...",0,1);
					currentAnnotation.finalise();
					currentAnnotation = new AnnotationSet(genome, probeList.name()+"["+annotationSets.size()+"]");
					annotationSets.add(currentAnnotation);
				}

			
				Feature feature = new Feature(featureTypeName,probes[p].chromosome().name());
				if (probes[p].hasDefinedName()) {
					feature.addAttribute("name", probes[p].name());
				}
				feature.setLocation(new Location(probes[p].start(),probes[p].end(),probes[p].strand()));
				currentAnnotation.addFeature(feature);
			}
		}
		
		return annotationSets.toArray(new AnnotationSet[0]);
	}
	
}
