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
import java.util.HashSet;
import java.util.Vector;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class DownloadableGenomeTreeModel implements TreeModel {

	private DownloadableGenomeSet genomes;
	private Character [] charIndexes;
	
	public DownloadableGenomeTreeModel (DownloadableGenomeSet genomes) {
		this.genomes = genomes;
		
		HashSet<Character>usedChars = new HashSet<Character>();
		
		GenomeSpecies [] species = genomes.species();
		
		for (int s=0;s<species.length;s++) {
			usedChars.add(species[s].name().charAt(0));
		}
		
		charIndexes = usedChars.toArray(new Character[0]);

		Arrays.sort(charIndexes);
	}
	
	public void addTreeModelListener(TreeModelListener l) {}

	public Object getChild(Object parent, int index) {

		if (parent instanceof GenomeSpecies) {
			return ((GenomeSpecies)parent).assemblySets()[index];
		}
		else if (parent instanceof GenomeAssemblySet) {
			return ((GenomeAssemblySet)parent).assemblies()[index];
		}
		else if (parent instanceof DownloadableGenomeSet) {
			return charIndexes[index];
		}
		else if (parent instanceof Character) {
			return getSpeciesForCharacter((Character)parent)[index];
		}
		
		return null;
	}
	
	private GenomeSpecies [] getSpeciesForCharacter (Character c) {
		
		GenomeSpecies [] allSpecies = genomes.species();
		
		Vector<GenomeSpecies>keepers = new Vector<GenomeSpecies>();
		
		for (int s=0;s<allSpecies.length;s++) {
			if (allSpecies[s].name().charAt(0) == c.charValue()) {
				keepers.add(allSpecies[s]);
			}
		}
		
		return keepers.toArray(new GenomeSpecies[0]);
	}

	public int getChildCount(Object parent) {
		if (parent instanceof GenomeSpecies) {
			return ((GenomeSpecies)parent).assemblySets().length;
		}
		else if (parent instanceof GenomeAssemblySet) {
			return ((GenomeAssemblySet)parent).assemblies().length;
		}
		else if (parent instanceof DownloadableGenomeSet) {
			return charIndexes.length;
		}
		else if (parent instanceof Character) {
			return getSpeciesForCharacter((Character)parent).length;
		}
		return 0;
	}

	public int getIndexOfChild(Object parent, Object child) {

		if (parent instanceof GenomeSpecies) {
			GenomeAssemblySet [] g = ((GenomeSpecies)parent).assemblySets();
			for (int i=0;i<g.length;i++) {
				if (g[i]==child){
					return i;
				}
			}
		}
		else if (parent instanceof GenomeAssemblySet) {
			GenomeAssembly [] g = ((GenomeAssemblySet)parent).assemblies();
			for (int i=0;i<g.length;i++) {
				if (g[i]==child){
					return i;
				}
			}
		}

		else if (parent instanceof DownloadableGenomeSet) {
			for (int i=0;i<charIndexes.length;i++) {
				if (child == charIndexes[i]) {
					return i;
				}
			}
		}
		else if (parent instanceof Character) {
			GenomeSpecies [] s = getSpeciesForCharacter((Character)parent);
			for (int i=0;i<s.length;i++) {
				if (s[i]==child){
					return i;
				}
			}
		}

		return -1;
	}

	public Object getRoot() {
		return genomes;
	}

	public boolean isLeaf(Object node) {
		if (node instanceof GenomeAssembly) {
			return true;
		}
		return false;
	}

	public void removeTreeModelListener(TreeModelListener l) {}

	public void valueForPathChanged(TreePath path, Object newValue) {}

}
