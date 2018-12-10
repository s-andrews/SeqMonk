package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class VistoryImage extends JPanel implements VistoryBlock {

	private BufferedImage image;
	
	public VistoryImage (BufferedImage image) {
		this.image = image;
	}
	
	@Override
	public JPanel getPanel() {
		return(this);
	}

	@Override
	public String getHTML() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void paintComponent (Graphics g) {
		g.drawImage(image, 0, 0, this);
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(image.getWidth(),image.getHeight());
	}

}
