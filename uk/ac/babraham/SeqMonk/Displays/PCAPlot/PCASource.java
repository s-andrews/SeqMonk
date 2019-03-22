/**
 * Copyright Copyright 2018-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.PCAPlot;

import java.io.File;
import java.io.IOException;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public interface PCASource {

	public String probeListName();
	
	public int getPCCount();
	
	public void writeExportData(File file) throws IOException;
	
	public int getStoreCount();
	
	public float getPCAValue (int storeIndex, int pcindex);
	
	public DataStore getStore(int index);
	
	public String getPCName(int index);
	
}
