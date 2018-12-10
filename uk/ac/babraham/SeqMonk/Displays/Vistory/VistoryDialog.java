package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryBlock;
import uk.ac.babraham.SeqMonk.Vistory.VistoryListener;

public class VistoryDialog extends JDialog implements VistoryListener {

	private Vistory vistory;
	private static VistoryDialog vistoryDialog = null;
	
	private JPanel vistoryPanel = null;
	
	
	private VistoryDialog () {
		super(SeqMonkApplication.getInstance(),"Vistory");
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.vistory = Vistory.getInstance();
		
		vistory.addListener(this);
		
		vistoryPanel = new JPanel();
		
		vistoryPanel.setLayout(new BoxLayout(vistoryPanel, BoxLayout.Y_AXIS));
		
		VistoryBlock [] blocks = vistory.blocks();
		
		for (int b=0;b<blocks.length;b++) {
			vistoryPanel.add(blocks[b].getPanel());
		}
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(vistoryPanel),BorderLayout.CENTER);
		
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
		vistoryPanel.add(block.getPanel());
		
		vistoryPanel.validate();
	}

	@Override
	public void blockRemoved(VistoryBlock block) {
		vistoryPanel.remove(block.getPanel());
		vistoryPanel.validate();
		
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
