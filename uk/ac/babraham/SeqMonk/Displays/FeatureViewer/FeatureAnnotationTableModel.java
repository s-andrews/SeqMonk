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
package uk.ac.babraham.SeqMonk.Displays.FeatureViewer;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationTagValue;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;

public class FeatureAnnotationTableModel extends AbstractTableModel {

	private String name = "No name";
	private String type;
	private String location;
	private String description = "No description";
	private AnnotationTagValue [] tagValues;
	
	public FeatureAnnotationTableModel (Feature f) {
		type = f.type();
		location = f.chromosomeName()+":"+f.location().start()+"-"+f.location().end();
		AnnotationTagValue [] tags = f.getAnnotationTagValues();
		Vector<AnnotationTagValue>keepers = new Vector<AnnotationTagValue>();
		for (int i=0;i<tags.length;i++) {
			if (tags[i].tag().equalsIgnoreCase("name")) {
				name = tags[i].value();
			}
			else if (tags[i].tag().equalsIgnoreCase("description")) {
				description = tags[i].value();
			}
			else {
				keepers.add(tags[i]);
			}
		}
		
		tagValues = keepers.toArray(new AnnotationTagValue[0]);
	}
	
	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return 4+tagValues.length;
	}
	
	public Class<?> getColumnClass (int col) {
		return String.class;
	}
	
	public String getColumnName (int col) {
		if (col==0) return "Annotation Key";
		return "Annotation value";
	}

	public Object getValueAt(int row, int col) {

		switch (col) {
		case 0:
			switch (row){
			case 0: return "Name";
			case 1: return "Type";
			case 2: return "Location";
			case 3: return "Description";
			default:return tagValues[row-4].tag();
			}
		case 1:
			switch (row){
			case 0: return name;
			case 1: return type;
			case 2: return location;
			case 3: return description;
			default:return tagValues[row-4].value();
			}
		}
		
		return null;
	}

	
}
