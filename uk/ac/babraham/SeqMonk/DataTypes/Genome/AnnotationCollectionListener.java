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
package uk.ac.babraham.SeqMonk.DataTypes.Genome;

/**
 * The listener interface for receiving annotationCollection events.
 * The class that is interested in processing a annotationCollection
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addAnnotationCollectionListener<code> method. When
 * the annotationCollection event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see AnnotationCollectionEvent
 */
public interface AnnotationCollectionListener {

	
	/**
	 * Annotation sets added.
	 * 
	 * @param annotationSet the newly added annotation sets
	 */
	public void annotationSetsAdded (AnnotationSet [] annotationSets);
	
	/**
	 * Annotation set removed.
	 * 
	 * @param annotationSet the annotation set
	 */
	public void annotationSetRemoved (AnnotationSet annotationSet);
	
	/**
	 * Annotation set renamed.
	 * 
	 * @param annotationSet the annotation set
	 */
	public void annotationSetRenamed (AnnotationSet annotationSet);
	
	/**
	 * Annotation features renamed.
	 * 
	 * @param annotationSet the annotation set in which features were renamed
	 */
	public void annotationFeaturesRenamed (AnnotationSet annotationSet, String newName);
	
	
	
}
