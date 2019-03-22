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

import java.util.Iterator;
import java.util.Vector;

public class GenomeSpecies {

	private Vector<GenomeAssemblySet> assemblySets = new Vector<GenomeAssemblySet>();
	private String name;
	
	public GenomeSpecies (String name) {
		this.name = name;
	}
	
	public String name () {
		return name;
	}
	
	public String toString () {
		return name();
	}
	
	public void addAssemblySet (GenomeAssemblySet assembly) {
		if (assembly != null) {
			assemblySets.add(assembly);
		}
	}
	
	public GenomeAssemblySet getAssemblySet (String name) {
		Iterator<GenomeAssemblySet> it = assemblySets.iterator();
		while (it.hasNext()) {
			GenomeAssemblySet set = it.next();
			if (set.assembly().equals(name)) {
				return set;
			}
		}
		
		// We need a new set
		GenomeAssemblySet set = new GenomeAssemblySet(this, name);
		return set;
	}
	
	public GenomeAssemblySet [] assemblySets () {
		return assemblySets.toArray(new GenomeAssemblySet[0]);
	}
	
	public GenomeAssembly [] assemblies () {
		Vector<GenomeAssembly> assemblies = new Vector<GenomeAssembly>();

		Iterator<GenomeAssemblySet> it = assemblySets.iterator();
		while (it.hasNext()) {
			GenomeAssemblySet set = it.next();
			GenomeAssembly [] as = set.assemblies();
			for (int i=0;i<as.length;i++) {
				assemblies.add(as[i]);
			}
			
		}
		return assemblies.toArray(new GenomeAssembly[0]);
	}
	
	
}
