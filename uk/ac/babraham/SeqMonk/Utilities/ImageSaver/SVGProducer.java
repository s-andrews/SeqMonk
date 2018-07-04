/**
 * Copyright 2013-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities.ImageSaver;

import java.io.PrintWriter;

public interface SVGProducer {

	/**
	 * This interface can be implemented by any component which would rather generate its
	 * own SVG code rather than using the generic SVG generator which is based on the 
	 * standard Graphics interface.  The image saver will always use this method over
	 * Graphics if this is available.
	 * 
	 * It's particularly useful for components which make use of the Graphics2D methods 
	 * which aren't implemented in the standard SVG generator.
	 */
	
	public void writeSVG (PrintWriter pr);
	
}
