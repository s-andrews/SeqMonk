/**
 * Copyright 2010-18 Simon Andrews
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

	private ProbeList probeList;
	private String featureType;
	
	/**
	 * Instantiates a new ProbeList annotation parser.
	 * 
	 * @param genome The genome into which data will be put
	 */
	public ProbeListAnnotationParser (Genome genome, ProbeList probes, String featureType) {
		super(genome);
		this.probeList = probes;
		this.featureType = featureType;
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

		AnnotationSet currentAnnotation = new AnnotationSet(genome, probeList.name());
		annotationSets.add(currentAnnotation);

		Probe [] probes = probeList.getAllProbes();

		
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

			
			Feature feature = new Feature(featureType,probes[p].chromosome().name());
			if (probes[p].hasDefinedName()) {
				feature.addAttribute("name", probes[p].name());
			}
			feature.setLocation(new Location(probes[p].start(),probes[p].end(),probes[p].strand()));
			currentAnnotation.addFeature(feature);
		}
		
		return annotationSets.toArray(new AnnotationSet[0]);
	}
	
}
