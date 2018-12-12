package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryBlock;
import uk.ac.babraham.SeqMonk.Vistory.VistoryListener;

public class VistoryDialog extends JFrame implements VistoryListener {

	private Vistory vistory;
	private static VistoryDialog vistoryDialog = null;
	
	private ScrollablePanel vistoryPanel = null;
	private GridBagConstraints gbc;
	private JScrollPane scrollPane;
	
	
	
	
	private VistoryDialog () {
		super("Vistory");
		setIconImage(SeqMonkApplication.getInstance().getIconImage());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.vistory = Vistory.getInstance();
		
		
		vistory.addListener(this);
		
		vistoryPanel = new ScrollablePanel();
		gbc = new GridBagConstraints();
		gbc.gridx=0;
		gbc.gridy=0;
		gbc.weightx = 0.5;
		gbc.weighty = 0.01;
		
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		vistoryPanel.setLayout(new GridBagLayout());
		vistoryPanel.setBackground(Color.GRAY);
		
		VistoryBlock [] blocks = vistory.blocks();
		
		for (int b=0;b<blocks.length;b++) {
			gbc.gridy=b;
			vistoryPanel.add(blocks[b],gbc);
		}
		
		gbc.gridy = 100000;
		gbc.weighty=0.999;
		gbc.fill = GridBagConstraints.BOTH;
		vistoryPanel.add(new JPanel(),gbc);
		
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new VistoryToolbar(this),BorderLayout.PAGE_START);
		scrollPane = new JScrollPane(vistoryPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scrollPane,BorderLayout.CENTER);
		
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}
	
	public static void showVistory () {
		if (vistoryDialog == null) vistoryDialog = new VistoryDialog();
		vistoryDialog.setVisible(true);
	}

	@Override
	public void blockAdded(VistoryBlock block) {
		gbc.gridy=vistory.blocks().length-1;
		vistoryPanel.add(block,gbc);
		
		vistoryPanel.revalidate();
		
		// Scroll to bottom when new block is added.
		scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
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
	public void vistoryUpdated() {
		vistoryPanel.validate();
	}

	@Override
	public void vistoryCleared() {
		vistoryPanel.removeAll();
		vistoryPanel.validate();
	}
	
	
	
	
}
