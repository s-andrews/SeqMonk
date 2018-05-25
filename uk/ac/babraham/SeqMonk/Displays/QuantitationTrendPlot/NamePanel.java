package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;

public class NamePanel extends JPanel {

	DataStore [] stores;
	ProbeList [] lists;
	private int maxWidth = 0;

	
	public NamePanel (DataStore [] stores, ProbeList [] lists) {
		
		this.stores = stores;
		this.lists = lists;
		
		FontMetrics fm = this.getFontMetrics(this.getFont());
		
		for (int d=0;d<stores.length;d++) {
			for (int l=0;l<lists.length;l++) {
				String text = stores[d].name()+" - "+lists[l].name();
				int yWidth = fm.stringWidth(text)+6;
				if (yWidth > maxWidth) {
					maxWidth = yWidth;
				}
			}
		}
	}
	
	
	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		for (int d=0;d<stores.length;d++) {
			for (int l=0;l<lists.length;l++) {
				g.setColor(ColourIndexSet.getColour((lists.length*d)+l));

				String text = stores[d].name()+" - "+lists[l].name();
				
				g.drawString(text, 3, ((lists.length*d)+l+1)*g.getFontMetrics().getHeight());

			}
		}

			
		
	}
	
	
	public Dimension getPreferredSize () {
		return new Dimension(maxWidth+3, 1000);
	}
	
	
	public Dimension getMinimumSize() {
		return new Dimension(maxWidth+3, 1);
		
	}	
	
	
}
