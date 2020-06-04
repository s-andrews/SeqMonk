package uk.ac.babraham.SeqMonk.Utilities;

public class ValidFileNameGenerator {

	
	public static String makeValidFileName(String suggestedName) {
		/* To be compatible with all platforms the things we can't
		 * have in filenames are:
		 * 
		 * Forward slash
		 * Backslash
		 * Double quote
		 * Greater / Less than
		 * Pipe
		 * Question Mark
		 * Asterisk
		 * Colon
		 * 
		 * We'll substitute these with underscores and then collapse
		 * multiple underscores to one
		 */
		
		String ourVerssion = suggestedName.replaceAll("[\t\n/\\\\\"<>\\|\\*:]", "");
	
		// Lose multiple underscores or spaces and leading or trailing spaces
		ourVerssion = ourVerssion.replaceAll("_+", "_");
		ourVerssion = ourVerssion.replaceAll("\\s+", " ");
		ourVerssion = ourVerssion.replaceAll("^\\s+", "");
		ourVerssion = ourVerssion.replaceAll("\\s+$", "");
		
		return(ourVerssion);
	
	}
	
}
