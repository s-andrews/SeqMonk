package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class VistoryImage extends VistoryBlock {

	private BufferedImage image;
	
	public VistoryImage (BufferedImage image) {
		this.image = image;
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
		return new Dimension(1,image.getHeight());
	}

}
