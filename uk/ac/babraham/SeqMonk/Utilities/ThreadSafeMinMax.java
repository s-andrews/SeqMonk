/**
 * Copyright 2011- 21 Simon Andrews
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
/**
 * This class provides a thread safe implementation of a record
 * of the max and min values ever seen
 * 
 * @author andrewss
 *
 */
public class ThreadSafeMinMax {

	private int min = 0;
	private int max = 0;
	private boolean anyData = false;
	
	public synchronized void addValue (int value) {
		if (anyData) {
			if (value < min) min = value;
			if (value > max) max = value;
		}
		else {
			min = value;
			max = value;
		}
	
	}
	
	public int min () {
		return min;
	}
	
	public int max () {
		return max;
	}
	
	
}
