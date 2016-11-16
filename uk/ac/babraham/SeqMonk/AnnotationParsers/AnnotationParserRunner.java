/**
 * Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.AnnotationParsers;

import java.io.File;

import javax.swing.JFileChooser;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * The Class AnnotationParserRunner provides an asynchonous way to
 * actually set up and run the import of external annotation features.
 */
public class AnnotationParserRunner {
	
	/**
	 * Run annotation parser.
	 * 
	 * @param application the application
	 * @param parser the parser
	 */
	public static void RunAnnotationParser(SeqMonkApplication application, AnnotationParser parser) {
		
		File [] files = null;
		if (parser.requiresFile()) {
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getDataLocation());
			chooser.setMultiSelectionEnabled(true);
			chooser.setFileFilter(parser.fileFilter());
		
			int result = chooser.showOpenDialog(application);
			if (result == JFileChooser.CANCEL_OPTION) return;

			files = chooser.getSelectedFiles();
			SeqMonkPreferences.getInstance().setLastUsedDataLocation(files[0]);
		}
		
		parser.addProgressListener(new ProgressDialog(application, parser.name(), parser));

		parser.parseFiles(files);
	}
	
}
