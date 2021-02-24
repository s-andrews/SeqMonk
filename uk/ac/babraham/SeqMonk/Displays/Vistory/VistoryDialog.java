/**
 * Copyright Copyright 2018- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryBlock;
import uk.ac.babraham.SeqMonk.Vistory.VistoryListener;

public class VistoryDialog extends JFrame implements VistoryListener {

	private Vistory vistory;
	private static VistoryDialog vistoryDialog = null;
	
	private static boolean hasBeenVisible = false;
	
	private ScrollablePanel vistoryPanel = null;
	private JScrollPane scrollPane;
	
	private static final BlankPanel BLANK_PANEL = new BlankPanel();
		
	private VistoryDialog () {
		super("Vistory");
		setIconImage(SeqMonkApplication.getInstance().getIconImage());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.vistory = Vistory.getInstance();
		
		vistory.addListener(this);
		
		vistoryPanel = new ScrollablePanel();

		addCurrentBlocks(true);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().setBackground(Color.WHITE);
		getContentPane().add(new VistoryToolbar(this),BorderLayout.PAGE_START);
		scrollPane = new JScrollPane(vistoryPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scrollPane,BorderLayout.CENTER);
		
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}
	
	public static void showVistory () {
		hasBeenVisible = true;
		if (vistoryDialog == null) vistoryDialog = new VistoryDialog();
		vistoryDialog.setVisible(true);
	}
	
	public static boolean hasBeenVisible () {
		return hasBeenVisible;
	}
	

	public void addCurrentBlocks (boolean scrollToEnd) {
		
		vistoryPanel.setLayout(new BoxLayout(vistoryPanel, BoxLayout.PAGE_AXIS));
		vistoryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		vistoryPanel.setBackground(Color.WHITE);
		
		VistoryBlock [] blocks = vistory.blocks();
		
		for (int b=0;b<blocks.length;b++) {
			vistoryPanel.add(blocks[b]);
			vistoryPanel.add(Box.createRigidArea(new Dimension(50,0)));
		}

		// Add a blank space so we're not always typing right at the bottom of the window.
		vistoryPanel.add(BLANK_PANEL);

		vistoryPanel.revalidate();
		
		// Scroll to bottom when new block is added.
		
		// Have tried numerous variations on this, but none of them scroll right to the
		// bottom.  Not sure what the heck is going on or how to fix it.
		
		if (scrollPane != null && scrollToEnd) {
//			scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
						
			scrollPane.getViewport().scrollRectToVisible(BLANK_PANEL.getBounds());
		}

	}
	
	@Override
	public void blockAdded(VistoryBlock block) {
		
		vistoryPanel.removeAll();
		vistoryPanel.revalidate();
		
		addCurrentBlocks(!block.allowsRelativePosition());
		
		// If it's a text box find out if it wants focus and give it if it does
		if (isVisible()) {
			Vistory.getInstance().requestVistoryFocus(block);
		}
	}

	@Override
	public void blockRemoved(VistoryBlock block) {
		vistoryPanel.remove(block);
		vistoryPanel.revalidate();
		
	}

	@Override
	public void blockEdited(VistoryBlock block) {
		vistoryPanel.validate();
	}

	@Override
	public void blocksReordered() {
		vistoryPanel.removeAll();
		vistoryPanel.revalidate();
		
		addCurrentBlocks(false);
	}

	
	
	@Override
	public void vistoryUpdated() {
		vistoryPanel.validate();
	}

	@Override
	public void vistoryCleared() {
		vistoryPanel.removeAll();
		addCurrentBlocks(true);
	}
	
	
	
	
}
