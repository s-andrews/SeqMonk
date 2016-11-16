/**
 * Copyright 2013-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class ProbeListCommentEditDialog extends JDialog implements ActionListener {

	
	private ProbeList list;
	private JTextArea editor;
	
	public ProbeListCommentEditDialog (ProbeList list) {
		this(list,SeqMonkApplication.getInstance());
	}
	
	public ProbeListCommentEditDialog (ProbeList list, Component c) {
	
		super(SeqMonkApplication.getInstance(),"Edit comments for "+list.name());
		
		this.list = list;
		
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout());
		
		panel.add(new JLabel("You can use this comments section to record the rationale for a particular filter or probe generation.",JLabel.CENTER),BorderLayout.NORTH);
		
		editor = new JTextArea();
		editor.setWrapStyleWord(true);
		editor.setLineWrap(true);
		editor.setText(list.comments());
		
		panel.add(new JScrollPane(editor,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		closeButton.setActionCommand("close");
		buttonPanel.add(closeButton);
		
		JButton saveButton = new JButton("Save Comments");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);
		
		panel.add(buttonPanel,BorderLayout.SOUTH);
		
		setContentPane(panel);
		
		setSize(600,300);
		setLocationRelativeTo(c);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("save")) {
			list.setComments(editor.getText());
		}
		
		setVisible(false);
		dispose();
		
	}
	
}
