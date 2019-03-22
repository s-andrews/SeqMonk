/**
 * Copyright 2012-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

public interface ProbeGroupGenerator {

	/**
	 * This is a generic interface for any class which allows you to create
	 * groups of probes from a starting list.  The groupings could be created
	 * in any way you like, but the generator should allow you to keep calling
	 * the next group method until it runs out of sets to return.
	 */
	
	public Probe [] nextSet();
	
}
