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
package uk.ac.babraham.SeqMonk.Displays.AlignedProbePlot;

public class CorrelationPair implements Comparable<CorrelationPair>{
	
	private int index1;
	private int index2;
	private float r;
	
	public CorrelationPair (int index1, int index2, float r) {
		this.index1 = index1;
		this.index2 = index2;
		this.r = r;
	}

	public int compareTo(CorrelationPair o) {
		return Float.compare(o.r, r);
	}
	
	public int index1 () {
		return index1;
	}
	
	public int index2 () {
		return index2;
	}
	
	public float r () {
		return r;
	}
	
	

}
