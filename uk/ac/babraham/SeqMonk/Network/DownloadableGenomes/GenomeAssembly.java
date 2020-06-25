/**
 * Copyright 2010-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Network.DownloadableGenomes;

import java.util.Date;

public class GenomeAssembly implements Comparable<GenomeAssembly> {

	private GenomeAssemblySet set;
	private String assembly;
	private int fileSize;
	private Date date;
	
	public GenomeAssembly (GenomeAssemblySet set, String assembly, int fileSize, Date date) {
		this.set = set;
		this.assembly = assembly;
		this.fileSize = fileSize;
		this.date = date;
		set.addAssembly(this);
	}
	
	public GenomeAssemblySet set () {
		return set;
	}
	
	public String assembly () {
		return assembly;
	}
	
	public int fileSize () {
		return fileSize;
	}
	
	public String toString () {
		return ""+assembly()+" ("+date()+")";
	}
	
	public Date date () {
		return date;
	}

	@Override
	public int compareTo(GenomeAssembly o) {
		return date.compareTo(o.date);
	}
	
}
