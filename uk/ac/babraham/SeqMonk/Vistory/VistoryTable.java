package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryTable extends VistoryBlock implements TableModel {

	String [][] tableData;
	
	
	public VistoryTable (TableModel model) {
		
		tableData = new String [model.getRowCount()+1][model.getColumnCount()];
		
		// First row is headers
		for (int i=0;i<tableData[0].length;i++) {
			tableData[0][i] = model.getColumnName(i);
		}
		
		// Now the rest of the data
		for (int i=0;i<tableData.length-1;i++) {
			for (int j=0;j<tableData[0].length;j++) {
				tableData[i+1][j] = model.getValueAt(i, j).toString();
			}
		}
		
		add(new JTable(this),BorderLayout.CENTER);
	}
	
	public void setData (String[][] tableData) {
		this.tableData = tableData;
	}
	
	@Override
	public String getHTML() {

		StringBuffer sb = new StringBuffer();
		sb.append("<table>");
		
		for (int i=0;i<tableData.length;i++) {
			sb.append("<tr>");
			for (int j=0;j<tableData[i].length;j++) {
				if (i==0) {
					sb.append("<th>");
					sb.append(EscapeHTML.escapeHTML(tableData[i][j]));
					sb.append("</th>");
				}
				else {
					sb.append("<td>");
					sb.append(EscapeHTML.escapeHTML(tableData[i][j]));
					sb.append("</td>");
				}
			}
			sb.append("</tr>");
		}
		
		sb.append("</table>");
	
		return sb.toString();
	}


	@Override
	public void addTableModelListener(TableModelListener l) {}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return tableData.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return tableData[0][columnIndex];
	}

	@Override
	public int getRowCount() {
		return tableData[0].length-1;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return tableData[rowIndex+1][columnIndex];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}


}
