/**
 * Copyright 2014-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.RNASeqQCPlot;

import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class RNAQCResult {

	private DataStore [] stores;
	private Vector<double []> percentageSets = new Vector<double[]>();
	private Vector<String> setTitles = new Vector<String>();
	
	public RNAQCResult (DataStore [] stores) {
		this.stores = stores;
	}
	
	public void addPercentageSet (String title, double [] data) {
		if (data.length != stores.length) {
			throw new IllegalArgumentException("Length of data in percentage set didn't match the length of the stores");
		}
		
		for (int i=0;i<data.length;i++) {
			if (data[i]<0 || data[i] > 100) {
				throw new IllegalArgumentException("Percentage data was not between 0 and 100");
			}
		}
		
		setTitles.add(title);
		percentageSets.add(data);
	}
	
	public String [] getTitles () {
		return setTitles.toArray(new String[0]);
	}
	
	public String [] getStoreNames () {
		String [] storeNames = new String[stores.length];
		
		for (int i=0;i<stores.length;i++) {
			storeNames[i] = stores[i].name();
		}
		
		return storeNames;
	}
	
	public DataStore [] stores () {
		return stores;
	}
	
	public double [][] getPercentageSets () {
		return percentageSets.toArray(new double[0][]);
	}
	
	
}
