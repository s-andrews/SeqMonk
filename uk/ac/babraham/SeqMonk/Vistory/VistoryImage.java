/**
 * Copyright Copyright 2018-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import net.sourceforge.iharder.base64.Base64;
import uk.ac.babraham.SeqMonk.Utilities.ImageToBase64;

public class VistoryImage extends VistoryBlock implements MouseListener, MouseMotionListener {

	private BufferedImage image;
	private int xOffset = 0;
	
	
	public VistoryImage (Date date, String data) {
		super(date);

		String base64Image = data.split(",")[1];
		try {
			byte [] imageBytes = Base64.decode(base64Image);
			image = ImageIO.read(new ByteArrayInputStream(imageBytes));
		}
		catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	public VistoryImage (BufferedImage image) {
		this.image = image;
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	@Override
	public String getHTML() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<p class=\"vistoryimage\"><img src=\"");
		sb.append(ImageToBase64.imageToBase64(image));
		sb.append("\"></p>");
		
		return(sb.toString());
	}
	
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		g.drawImage(image, xOffset, 0, this);
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(image.getWidth(),image.getHeight());
	}

	public Dimension getMinimumSize () {
		return new Dimension(1,image.getHeight());
	}

	@Override
	public void mouseDragged(MouseEvent me) {
		// We use this to move around the image if there isn't
		// enough space to show all of it.  We'll always be able 
		// to show the height because we can scroll but we can't 
		// always show the width.
		
		// This is irrelevant if the image fits in the window
		if (getWidth() >= image.getWidth()) return;
		
		
		// The biggest start offset will be the difference between
		// the image size and the window width
		int maxOffset = image.getWidth() - getWidth();
		
		// We work out how far we are across the window proportionally
		// and scale the offset by that.
		
		
		int x = me.getX();
		if (x < 0) x=0;
		if (x > getWidth()) x=getWidth();
		
		double proportion = x / (double)getWidth();
		
		xOffset = 0-(int)(maxOffset * proportion);
		
		repaint();
		
		
	}


	@Override
	public void mouseExited(MouseEvent e) {
		if (xOffset != 0) {
			xOffset = 0;
			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {}

	@Override
	public String getType() {
		return "IMAGE";
	}

	@Override
	public String getData() {
		return ImageToBase64.imageToBase64(image);
	}

	@Override
	public boolean allowsRelativePosition() {
		return false;
	}



}
