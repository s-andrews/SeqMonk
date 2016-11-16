package uk.ac.babraham.SeqMonk.Displays.BoxWhisker;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Analysis.Statistics.BoxWhisker;

public class MultiBoxWhiskerPanel extends JPanel {
		
	public MultiBoxWhiskerPanel (BoxWhisker [] whiskers, String [] names, String panelName, float min, float max) {
				
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.insets = new Insets(2,2,2,2);
		
		gbc.gridx=0;
		gbc.gridy=0;
		gbc.gridwidth=whiskers.length+1;
		add(new NoWidthLabel(panelName, JLabel.CENTER),gbc);
		gbc.gridwidth=1;
		
		gbc.fill=GridBagConstraints.BOTH;
		gbc.gridx=0;
		gbc.gridy=2;
		gbc.weighty=0.999;
		gbc.weightx=0.001;
		add(new BoxWhiskerScaleBar(min, max),gbc);
		gbc.weightx=0.5;
		
		for (int i=0;i<whiskers.length;i++) {
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx=i+1;
			gbc.gridy=1;
			gbc.weighty=0.01;
			gbc.anchor = GridBagConstraints.NORTH;
			add(new NoWidthLabel(names[i],JLabel.CENTER),gbc);
			gbc.fill=GridBagConstraints.BOTH;
			gbc.weighty=0.999;
			gbc.gridy=2;
			add(new BoxWhiskerPanel(whiskers[i], min, max),gbc);
		}
	}
	

	/**
	 * A JLabel with no minimum width so out plot isn't forced to be
	 * ridiculously wide if the DataStores have long names.  We have
	 * to live with the fact that names will be truncated if using this.
	 */
	private class NoWidthLabel extends JLabel {
		
		/**
		 * Constructor which passes directly to JLabel
		 * 
		 * @param text the text
		 * @param orientation the orientation
		 */
		public NoWidthLabel (String text, int orientation) {
			super(text,orientation);
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			Dimension orig = super.getPreferredSize();
			return new Dimension (1,orig.height);
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getMinimumSize()
		 */
		public Dimension getMinimumSize () {
			Dimension orig = super.getMinimumSize();
			return new Dimension (1,orig.height);
		}

	}

}


