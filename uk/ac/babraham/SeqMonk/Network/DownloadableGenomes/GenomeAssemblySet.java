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
package uk.ac.babraham.SeqMonk.Network.DownloadableGenomes;

import java.util.Arrays;
import java.util.Vector;

public class GenomeAssemblySet {

	private GenomeSpecies species;
	private String assembly;
	private Vector<GenomeAssembly> assemblies = new Vector<GenomeAssembly>();
	
	public GenomeAssemblySet (GenomeSpecies species, String assembly) {
		this.species = species;
		this.assembly = assembly;
		species.addAssemblySet(this);;
	}
	
	public GenomeSpecies species () {
		return species;
	}
	
	public String assembly () {
		return assembly;
	}
		
	public String toString () {
		return assembly();
	}
	
	public void addAssembly (GenomeAssembly assembly) {
		assemblies.add(assembly);
	}
	
	public GenomeAssembly [] assemblies () {
		GenomeAssembly [] assembliesToReturn = assemblies.toArray(new GenomeAssembly[0]);
		Arrays.sort(assembliesToReturn);
		return (assembliesToReturn);
	}
	
}
