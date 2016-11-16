/**
 * Copyright 2011-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.GotoDialog;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;

public class RecentLocation {
	private Chromosome c;
	private int start;
	private int end;
	
	public RecentLocation (Chromosome c, int start, int end) {
		this.c = c;
		this.start = start;
		this.end = end;
	}
	
	public Chromosome chromsome () {
		return c;
	}
	
	public int start () {
		return start;
	}
	
	public int end () {
		return end;
	}
	
	public String toString() {
		
		return c.name()+":"+start+"-"+end;
	}

	public int compareTo(RecentLocation l) {
		if (c != l.c) {
			return c.compareTo(l.c);
		}
		if (start != l.start) {
			return start-l.start;
		}
		return end - l.end;
	}
}
