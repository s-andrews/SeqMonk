package uk.ac.babraham.SeqMonk.Utilities;

public class EscapeHTML {

	public static String escapeHTML (String text) {
		String html = new String(text);
		
		html.replaceAll("&", "&amp;");
		html.replaceAll(">", "&gt;");
		html.replaceAll("<", "&lt;");
		
		return(html);
	}
	
}
