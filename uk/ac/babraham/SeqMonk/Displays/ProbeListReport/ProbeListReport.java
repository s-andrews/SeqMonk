/**
 * Copyright 2013- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.ProbeListReport;

import java.io.StringWriter;
import java.util.Vector;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class ProbeListReport {

	private String html;
	
	
	public ProbeListReport (ProbeList list) {

		Vector<ProbeList> listsToRoot = new Vector<ProbeList>();

		listsToRoot.add(list);

		ProbeList currentList = list;

		while (currentList.parent() != null) {
			currentList = currentList.parent();
			listsToRoot.add(currentList);
		}

		// Now we need to go through the lists to root backwards adding in the info
		// to the report.

		try {
			StringWriter htmlStr = new StringWriter();
			XMLOutputFactory xmlfactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xhtml= xmlfactory.createXMLStreamWriter(htmlStr);

			xhtml.writeStartElement("html");
			xhtml.writeStartElement("head");

			
			xhtml.writeStartElement("title");
			xhtml.writeCharacters("SeqMonk probe list report for ");
			xhtml.writeCharacters(list.name());
			xhtml.writeEndElement();//title
			
			xhtml.writeStartElement("style");
			xhtml.writeAttribute("type", "text/css");
			xhtml.writeCharacters("body {font-family: sans-serif}" +
			"h2 {color: #1111AA}" +
			"h3 {padding-left: 1em}" +
			"p {padding-left: 3em}" +
			"li {padding-bottom: 1em}" +
			"table {padding-left: 3em}" +
			"th {background: #6666AA; color: white; padding: 0.5em}" +
			"td {background: #CCCCCC; padding: 0.5em}" +
			"p.comment {font-family: serif; color: #AAAAAA; font-style: italic; padding-left: 5em}" 
			);
			xhtml.writeEndElement();//style


			xhtml.writeEndElement();//head

			xhtml.writeStartElement("body");

			xhtml.writeStartElement("h1");
			xhtml.writeCharacters("SeqMonk probe list report for "+list.name()+" ("+list.getAllProbes().length+" probes)");
			xhtml.writeEndElement();//h1

			// The description of probe generation
			xhtml.writeStartElement("h2");
			xhtml.writeCharacters("Probe Generation ("+listsToRoot.lastElement().getAllProbes().length+" probes)");
			xhtml.writeEndElement();//h2

			xhtml.writeStartElement("p");
			xhtml.writeCharacters(listsToRoot.lastElement().description());
			xhtml.writeEndElement();//p

			// Add any comments
			String [] commentLines = listsToRoot.lastElement().comments().split("\\n+");
			
			for (int c=0;c<commentLines.length;c++) {
				
				xhtml.writeStartElement("p");
				xhtml.writeAttribute("class", "comment");
				xhtml.writeCharacters(commentLines[c]);
				xhtml.writeEndElement();//p
				
			}
			
			xhtml.writeEmptyElement("hr");

			// The description of the filters
			xhtml.writeStartElement("h2");
			xhtml.writeCharacters("Filters");
			xhtml.writeEndElement();//h2

			int filterCount = 1;

			for (int i=listsToRoot.size()-2;i>=0;i--) {

				xhtml.writeStartElement("h3");
				xhtml.writeCharacters(""+filterCount+": "+listsToRoot.elementAt(i).name()+" ("+listsToRoot.elementAt(i).getAllProbes().length+" probes)");
				xhtml.writeEndElement(); //h3
				xhtml.writeStartElement("p");
				xhtml.writeCharacters(listsToRoot.elementAt(i).description());
				xhtml.writeEndElement(); //p
				
				// Add any comments
				String [] listCommentLines = listsToRoot.elementAt(i).comments().split("\\n+");
				
				for (int c=0;c<listCommentLines.length;c++) {
					
					xhtml.writeStartElement("p");
					xhtml.writeAttribute("class", "comment");
					xhtml.writeCharacters(listCommentLines[c]);
					xhtml.writeEndElement();//p
					
				}

				
				filterCount++;

			}

			xhtml.writeEmptyElement("hr");

			// The description of the datasets

			xhtml.writeStartElement("h2");
			xhtml.writeCharacters("Data Stores");
			xhtml.writeEndElement();//h2

			xhtml.writeStartElement("h3");
			xhtml.writeCharacters("Data Sets");
			xhtml.writeEndElement();//h2

			xhtml.writeStartElement("table");

			xhtml.writeStartElement("tr");

			xhtml.writeStartElement("th");
			xhtml.writeCharacters("DataSet Name");
			xhtml.writeEndElement(); // th

			xhtml.writeStartElement("th");
			xhtml.writeCharacters("Original File");
			xhtml.writeEndElement(); // th

			xhtml.writeStartElement("th");
			xhtml.writeCharacters("Import Options");
			xhtml.writeEndElement(); // th
			
			xhtml.writeEndElement(); // tr (header)

			DataSet [] sets = SeqMonkApplication.getInstance().dataCollection().getAllDataSets();

			for (int i=0;i<sets.length;i++) {
				xhtml.writeStartElement("tr");

				xhtml.writeStartElement("td");
				xhtml.writeCharacters(sets[i].name());
				xhtml.writeEndElement(); // td

				xhtml.writeStartElement("td");
				xhtml.writeCharacters(sets[i].fileName());
				xhtml.writeEndElement(); // td

				xhtml.writeStartElement("td");
				xhtml.writeCharacters(sets[i].importOptions());
				xhtml.writeEndElement(); // td
				
				xhtml.writeEndElement(); // tr (dataset)

			}

			xhtml.writeEndElement(); // table


			xhtml.writeStartElement("h3");
			xhtml.writeCharacters("Data Groups");
			xhtml.writeEndElement();//h3

			DataGroup [] groups = SeqMonkApplication.getInstance().dataCollection().getAllDataGroups();

			if (groups.length > 0) {

				xhtml.writeStartElement("table");

				xhtml.writeStartElement("tr");

				xhtml.writeStartElement("th");
				xhtml.writeCharacters("DataGroup Name");
				xhtml.writeEndElement(); // th

				xhtml.writeStartElement("th");
				xhtml.writeCharacters("Members");
				xhtml.writeEndElement(); // th

				xhtml.writeEndElement(); // tr (header)


				for (int i=0;i<groups.length;i++) {
					DataSet [] groupSets = groups[i].dataSets();

					if (groupSets.length == 0) continue;
					
					xhtml.writeStartElement("tr");

					
					xhtml.writeStartElement("td");
					xhtml.writeAttribute("rowspan", ""+groupSets.length);
					xhtml.writeCharacters(groups[i].name());
					xhtml.writeEndElement(); // td

					for (int j=0;j<groupSets.length;j++) {
						if (j>0) xhtml.writeStartElement("tr");
						xhtml.writeStartElement("td");
						xhtml.writeCharacters(groupSets[j].name());
						xhtml.writeEndElement(); // td
						xhtml.writeEndElement(); // tr (dataset)
					}


				}

				xhtml.writeEndElement(); // table
			}
			else {
				xhtml.writeStartElement("p");
				xhtml.writeCharacters("No groups");
				xhtml.writeEndElement();
			}

			xhtml.writeStartElement("h3");
			xhtml.writeCharacters("Replicate Sets");
			xhtml.writeEndElement();//h3

			ReplicateSet [] replicates = SeqMonkApplication.getInstance().dataCollection().getAllReplicateSets();

			if (replicates.length > 0) {

				xhtml.writeStartElement("table");

				xhtml.writeStartElement("tr");

				xhtml.writeStartElement("th");
				xhtml.writeCharacters("ReplicateSet Name");
				xhtml.writeEndElement(); // th

				xhtml.writeStartElement("th");
				xhtml.writeCharacters("Members");
				xhtml.writeEndElement(); // th

				xhtml.writeStartElement("th");
				xhtml.writeCharacters("Type");
				xhtml.writeEndElement(); // th

				xhtml.writeEndElement(); // tr (header)


				for (int i=0;i<replicates.length;i++) {
					DataStore [] replicateMembers = replicates[i].dataStores();

					if (replicateMembers.length == 0) continue;
					
					xhtml.writeStartElement("tr");

					
					xhtml.writeStartElement("td");
					xhtml.writeAttribute("rowspan", ""+replicateMembers.length);
					xhtml.writeCharacters(replicates[i].name());
					xhtml.writeEndElement(); // td

					for (int j=0;j<replicateMembers.length;j++) {
						if (j>0) xhtml.writeStartElement("tr");
						xhtml.writeStartElement("td");
						xhtml.writeCharacters(replicateMembers[j].name());
						xhtml.writeEndElement(); // td

						xhtml.writeStartElement("td");
						if (replicateMembers[j] instanceof DataSet) {
							xhtml.writeCharacters("Data Set");							
						}
						else {
							xhtml.writeCharacters("Data Group");
						}
						xhtml.writeEndElement(); // td
						xhtml.writeEndElement(); // tr (dataset)

					}


				}

				xhtml.writeEndElement(); // table
			}
			else {
				xhtml.writeStartElement("p");
				xhtml.writeCharacters("No replicate sets");
				xhtml.writeEndElement();
			}

			xhtml.writeEmptyElement("hr");

			// Write out the environment this came from
			
			xhtml.writeStartElement("h2");
			xhtml.writeCharacters("Environment");
			xhtml.writeEndElement(); //h2
			
			xhtml.writeStartElement("table");
			
			xhtml.writeStartElement("tr");
			
			xhtml.writeStartElement("th");
			xhtml.writeCharacters("Name");
			xhtml.writeEndElement(); //th

			xhtml.writeStartElement("th");
			xhtml.writeCharacters("Value");
			xhtml.writeEndElement(); //th
			
			xhtml.writeEndElement(); // Header row

			xhtml.writeStartElement("tr");
			
			xhtml.writeStartElement("td");
			xhtml.writeCharacters("Project Name");
			xhtml.writeEndElement(); //td

			xhtml.writeStartElement("td");
			xhtml.writeCharacters(SeqMonkApplication.getInstance().projectName());
			xhtml.writeEndElement(); //th
			
			xhtml.writeEndElement(); // Project name row

			
			xhtml.writeStartElement("tr");
			
			xhtml.writeStartElement("td");
			xhtml.writeCharacters("Genome Assembly");
			xhtml.writeEndElement(); //td

			xhtml.writeStartElement("td");
			xhtml.writeCharacters(SeqMonkApplication.getInstance().dataCollection().genome().toString());
			xhtml.writeEndElement(); //th
			
			xhtml.writeEndElement(); // Genome version row
			
			xhtml.writeStartElement("tr");
			
			xhtml.writeStartElement("td");
			xhtml.writeCharacters("SeqMonk Version");
			xhtml.writeEndElement(); //td

			xhtml.writeStartElement("td");
			xhtml.writeCharacters("SeqMonk v"+SeqMonkApplication.VERSION);
			xhtml.writeEndElement(); //th
			
			xhtml.writeEndElement(); // Seqmonk version row

			xhtml.writeStartElement("tr");
			
			xhtml.writeStartElement("td");
			xhtml.writeCharacters("Java Version");
			xhtml.writeEndElement(); //td

			xhtml.writeStartElement("td");
			xhtml.writeCharacters(System.getProperty("java.version"));
			xhtml.writeEndElement(); //th
			
			xhtml.writeEndElement(); // Java version row

			
			
			
			xhtml.writeEndElement(); // table

			xhtml.writeEndElement();//body
			xhtml.writeEndElement();//html


			html = htmlStr.toString();

		}
		catch (XMLStreamException ex) {
			throw new IllegalStateException(ex);
		}

	}
	
	public String getHTML () {
		return html;
	}
	


}
