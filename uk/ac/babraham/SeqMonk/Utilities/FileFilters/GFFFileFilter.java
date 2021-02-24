/**
 * Copyright 2009- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities.FileFilters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A File filter representing GFF Files
 */
public class GFFFileFilter extends FileFilter {

	/* (non-Javadoc)
	 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File f) {
		if (f.isDirectory() || 
				f.getName().toLowerCase().endsWith(".gff") ||
				f.getName().toLowerCase().endsWith(".gff3") || 
				f.getName().toLowerCase().endsWith(".gtf") ||
				f.getName().toLowerCase().endsWith(".gff.gz") ||
				f.getName().toLowerCase().endsWith(".gff3.gz") || 
				f.getName().toLowerCase().endsWith(".gtf.gz")
				) {
			return true;
		}
		else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.filechooser.FileFilter#getDescription()
	 */
	@Override
	public String getDescription() {
		return "GFF Files";
	}


}
