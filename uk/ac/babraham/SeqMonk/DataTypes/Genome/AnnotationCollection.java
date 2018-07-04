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
package uk.ac.babraham.SeqMonk.DataTypes.Genome;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

/**
 * The Class AnnotationCollection is the main object through which 
 * annotation objects can be accessed
 */
public class AnnotationCollection {

	/** The genome. */
	private Genome genome;
	
	/** The annotation sets. */
	private Vector<AnnotationSet> annotationSets = new Vector<AnnotationSet>();
	
	/** The listeners. */
	private Vector<AnnotationCollectionListener>listeners = new Vector<AnnotationCollectionListener>();
	
	/**
	 * Instantiates a new annotation collection.
	 * 
	 * @param genome the genome
	 */
	public AnnotationCollection (Genome genome) {
		this.genome = genome;
	}
	
	/**
	 * Anotation sets.
	 * 
	 * @return the annotation set[]
	 */
	public AnnotationSet [] anotationSets () {
		return annotationSets.toArray(new AnnotationSet[0]);
	}
	
	/**
	 * Adds the annotation collection listener.
	 * 
	 * @param l the l
	 */
	public void addAnnotationCollectionListener (AnnotationCollectionListener l) {
		if (l != null && !listeners.contains(l)) {
			listeners.add(l);
		}
	}
	
	/**
	 * Removes the annotation collection listener.
	 * 
	 * @param l the l
	 */
	public void removeAnnotationCollectionListener (AnnotationCollectionListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}
	
	/**
	 * Adds multiple annotation sets in an efficient manner.
	 * 
	 * @param annotationSets the annotation sets to add
	 */
	public void addAnnotationSets (AnnotationSet [] newSets) {
		
		for (int s=0;s<newSets.length;s++) {
		
			// We want to check that the genome for these sets is compatible with
			// the genome for this set.  This either means they have to be the same
			// or if we're a multi-genome that the genome for this is one of the 
			// genomes within the collection
			
			if (newSets[s].genome() != genome) {
				if (! (genome instanceof MultiGenome)) {
					throw new IllegalArgumentException("Annotation set genome doesn't match annotation collection");
				}
				if (!((MultiGenome)genome).containsGenome(newSets[s].genome())) {
					throw new IllegalArgumentException("Annotation set genome isn't contained within the current multi-genome");
				}
			}
			annotationSets.add(newSets[s]);
			
			if (genome instanceof MultiGenome && newSets[s].isCollectionSet()) {
				newSets[s].resetCollection(this);
			}
			
			else {
				newSets[s].setCollection(this);
			}
		}
		
		Enumeration<AnnotationCollectionListener>l = listeners.elements();
		while (l.hasMoreElements()) {
			l.nextElement().annotationSetsAdded(newSets);
		}
	}

	
	/**
	 * Removes the annotation set.
	 * 
	 * @param annotationSet the annotation set
	 */
	protected void removeAnnotationSet (AnnotationSet annotationSet) {

		// Notify before removing to not mess up the data tree
		Enumeration<AnnotationCollectionListener>l = listeners.elements();
		while (l.hasMoreElements()) {
			l.nextElement().annotationSetRemoved(annotationSet);
		}
			
		annotationSets.remove(annotationSet);
	}
	
	/**
	 * Annotation set renamed.
	 * 
	 * @param set the set
	 */
	protected void annotationSetRenamed (AnnotationSet set) {
		Enumeration<AnnotationCollectionListener>l = listeners.elements();
		while (l.hasMoreElements()) {
			l.nextElement().annotationSetRenamed(set);
		}		
	}

	/**
	 * Annotation features renamed.
	 * 
	 * @param set the set
	 */
	protected void annotationFeaturesRenamed (AnnotationSet set, String name) {
		Enumeration<AnnotationCollectionListener>l = listeners.elements();
		while (l.hasMoreElements()) {
			l.nextElement().annotationFeaturesRenamed(set,name);
		}		
	}

	
	/**
	 * Says whether this collection contains any features of the
	 * given type
	 * 
	 * @param type The feature type
	 * @return true, if this collection has any features of that type
	 */
	public boolean hasDataForType (String type) {
		Enumeration<AnnotationSet>a = annotationSets.elements();
		while (a.hasMoreElements()) {
			if (a.nextElement().hasDataForType(type)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the features for type.
	 * 
	 * @param c the c
	 * @param type the type
	 * @return the features for type
	 */
	public Feature [] getFeaturesForType (Chromosome c, String type) {
		Vector<Feature>features = new Vector<Feature>();
		Enumeration<AnnotationSet>sets = annotationSets.elements();
		while (sets.hasMoreElements()) {
			Feature [] f = sets.nextElement().getFeaturesForType(c.name(), type);
			for (int i=0;i<f.length;i++) {
				features.add(f[i]);
			}
		}
		
		Feature [] allFeatures = features.toArray(new Feature[0]);
		Arrays.sort(allFeatures);		
		return allFeatures;
	}	

	/**
	 * Gets the features for type across all chromosomes.
	 * 
	 * @param type the type
	 * @return the features for type
	 */
	public Feature [] getFeaturesForType (String type) {
		Vector<Feature>features = new Vector<Feature>();
		Enumeration<AnnotationSet>sets = annotationSets.elements();
		while (sets.hasMoreElements()) {
			Feature [] f = sets.nextElement().getFeaturesForType(type);
			for (int i=0;i<f.length;i++) {
				features.add(f[i]);
			}
		}
		
		Feature [] allFeatures = features.toArray(new Feature[0]);
		Arrays.sort(allFeatures);		
		return allFeatures;
	}	

	
	
	/**
	 * List available feature types.
	 * 
	 * @return the string[]
	 */
	public String [] listAvailableFeatureTypes () {
		HashSet<String> t = new HashSet<String>();
		
		Enumeration<AnnotationSet>sets = annotationSets.elements();
		
		while (sets.hasMoreElements()) {
			String [] features = sets.nextElement().getAvailableFeatureTypes();
			for (int i=0;i<features.length;i++) {
				if (!t.contains(features[i])) {
					t.add(features[i]);
				}
			}
		}
		
		String [] finalFeatures = t.toArray(new String[0]);
		Arrays.sort(finalFeatures,new CaseInsensitiveSorter());
		return finalFeatures;
	}

	private class CaseInsensitiveSorter implements Comparator<String> {

		public int compare(String s1, String s2) {
			return s1.compareToIgnoreCase(s2);
		}
		
	}
	
}
