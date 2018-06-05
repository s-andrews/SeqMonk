/**
 * Copyright 2011-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities;

import java.io.Serializable;

/**
 * This class implements something like a vector but for primitive doubles
 * 
 * @author andrewss
 *
 */
public class DoubleVector implements Serializable {

	private double [] array = new double [1000];
	private int length = 0;
	private boolean trimmed = false;
	
	
	public synchronized void add (double value) {
		if (array.length == length) {
			makeLonger();
		}
		
		array[length] = value;
		length++;
		trimmed = false;
	}

	
	public int length () {
		return length;
	}
	
	public void setValues (double [] values) {
		array = values;
		length = values.length;
		trimmed = true;
	}
	
	public void clear () {
		array = new double [1000];
		length = 0;
	}
	
	public double [] toArray () {
		if (! trimmed) trim();
		return array;
	}
	
	/** 
	 * This method causes the vector to trim its current storage to the 
	 * actual set of values it's storing so that no extraneous storage
	 * is being used.  It's only useful if we want to keep the vector
	 * around after all of the reads have been added.
	 */
	public void trim () {
		double [] trimmedArray = new double[length];
		for (int i=0;i<trimmedArray.length;i++) {
			trimmedArray[i] = array[i];
		}
		array = trimmedArray;
		trimmed = true;
	}
	
	
	private void makeLonger () {
		int newLength = length * 2;
		
		if (newLength - length < 500) {
			newLength = length+500;
		}
				
		double [] newArray = new double[newLength];
		for (int i=0;i<array.length;i++) {
			newArray[i] = array[i];
		}
		array = newArray;
	}
	
}
