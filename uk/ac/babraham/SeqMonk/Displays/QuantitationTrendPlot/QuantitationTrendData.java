package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;

import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

public class QuantitationTrendData implements Runnable, Cancellable {


	/**
	 * This class does the calculation for a set of trends.  It is used in both the quantitation
	 * trend plot and in the quantitation trend heatmap.
	 */

	private DataStore [] stores;
	private ProbeList [] lists;
	private QuantitationTrendPlotPreferencesPanel prefs;
	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();

	private boolean cancel = false;

	// Values we calculate
	double [][][] upstreamData; // Probes first, Stores second
	double [][][] centralData; // Probes first, Stores second
	double [][][] downstreamData; // Probes first, Stores second

	boolean fixedWidth = false;

	double minValue = Double.NaN;
	double maxValue = Double.NaN;

	double upstreamAxisStart = Double.NaN;
	double upstreamAxisEnd = Double.NaN;
	double centralAxisStart = Double.NaN;
	double centralAxisEnd = Double.NaN;
	double downstreamAxisStart = Double.NaN;
	double downstreamAxisEnd = Double.NaN;

	boolean hasUpstream = false;
	boolean hasDownstream = false;


	public QuantitationTrendData (DataStore [] stores, ProbeList [] lists, QuantitationTrendPlotPreferencesPanel prefs) {
		this.stores = stores;
		this.lists = lists;
		this.prefs = prefs;
	}

	public void startCalculating () {
		Thread t = new Thread(this);
		t.start();
	}

	public void addProgressListener (ProgressListener l) {
		if (l == null) return;
		if (!listeners.contains(l)) listeners.add(l);
	}

	public void removeProgressListener (ProgressListener l) {
		if (listeners.contains(l))listeners.remove(l);
	}

	public boolean hasUpstream () {
		return hasUpstream;
	}

	public boolean hasDownstream () {
		return hasDownstream;
	}

	public boolean isFixedWidth () {
		return fixedWidth;
	}

	private int getIndex (Object [] values, Object value) {
		for (int i=0;i<values.length;i++) {
			if (values[i] == value) {
				return i;
			}
		}

		throw new IllegalStateException("Requested value "+value+" not found in list");
	}


	public double [] getUpstreamData (DataStore store, ProbeList probes) {
		return upstreamData[getIndex(lists, probes)][getIndex(stores, store)];

	}

	public double [] getCentralData (DataStore store, ProbeList probes) {
		return centralData[getIndex(lists, probes)][getIndex(stores, store)];
	}

	public double [] getDownstreamData (DataStore store, ProbeList probes) {
		return downstreamData[getIndex(lists, probes)][getIndex(stores, store)];
	}

	public double getMinValue () {
		return minValue;
	}

	public double getMaxValue () {
		return maxValue;
	}

	public double getUpstreamAxisStart () {
		return upstreamAxisStart;
	}
	public double getUpstreamAxisEnd () {
		return upstreamAxisEnd;
	}
	public double getCentralAxisStart () {
		return centralAxisStart;
	}
	public double getCentralAxisEnd () {
		return centralAxisEnd;
	}
	public double getDownstreamAxisStart () {
		return downstreamAxisStart;
	}
	public double getDownstreamAxisEnd () {
		return downstreamAxisEnd;
	}


	private void progressCancelled() {
		for (ProgressListener listener : listeners) {
			listener.progressCancelled();
		}
	}

	private void progressComplete() {
		for (ProgressListener listener : listeners) {
			listener.progressComplete("quantitation trend", this);;
		}
	}


	private void progressExceptionReceived(Exception e) {
		for (ProgressListener listener : listeners) {
			listener.progressExceptionReceived(e);
		}
	}


	private void progressWarningReceived(Exception e) {
		for (ProgressListener listener : listeners) {
			listener.progressWarningReceived(e);;
		}
	}

	private void progressUpdated(String message, int current, int max) {
		for (ProgressListener listener : listeners) {
			listener.progressUpdated(message, current, max);
		}
	}





	@Override
	public void run() {

		// Work out which windows we need to create and whether they are 
		// fixed width or dynamic

		try {
			// Upstream
			Probe [] upstreamProbes = prefs.getUpstreamProbes();
			if (upstreamProbes == null || upstreamProbes.length == 0) {
				hasUpstream = false;
			}
			else {

				// Upstream is always fixed length.  We just need to find out
				// what it is
				upstreamAxisStart = 0-upstreamProbes[0].length();
				upstreamAxisEnd = -1;

				upstreamData = new double[lists.length][stores.length][];

				for (int l=0;l<lists.length;l++) {
					Probe [] probes = lists[l].getAllProbes();

					progressUpdated("Calculating trend...", l, lists.length * 3);

					for (int s=0;s<stores.length;s++) {
						DataStore store = stores[s];

						if (cancel) {
							progressCancelled();
							return;
						}


						upstreamData[l][s] = getMeanQuantitativeValues(upstreamProbes, store, probes, 200);

					}
				}
			}

			// Central
			Probe [] centralProbes = prefs.getCoreProbes();
			if (centralProbes == null || centralProbes.length == 0) {
				throw new IllegalStateException("No core probes generated for some reason");
			}
			else {

				// Central could be fixed or dynamic.  We need to work out which

				int centralLength = centralProbes[0].length();
				for (int i=0;i<centralProbes.length;i++) {
					if (centralProbes[i].length() != centralLength) {
						fixedWidth = false;
						break;
					}
				}

				if (!isFixedWidth()) {
					centralAxisStart = 1;
					centralAxisEnd = centralLength;
				}

				centralData = new double[lists.length][stores.length][];

				for (int l=0;l<lists.length;l++) {
					Probe [] probes = lists[l].getAllProbes();

					progressUpdated("Calculating trend...", lists.length+l, lists.length * 3);


					for (int s=0;s<stores.length;s++) {
						DataStore store = stores[s];

						if (cancel) {
							progressCancelled();
							return;
						}


						centralData[l][s] = getMeanQuantitativeValues(centralProbes, store, probes, 200);

					}
				}
			}

			// Downstream
			Probe [] downstreamProbes = prefs.getDownstreamProbes();
			if (downstreamProbes == null || downstreamProbes.length == 0) {
				hasDownstream = false;
			}
			else {

				// Downstream is always fixed length.  We just need to find out
				// what it is
				downstreamAxisStart = 0-downstreamProbes[0].length();
				downstreamAxisEnd = -1;

				downstreamData = new double[lists.length][stores.length][];

				for (int l=0;l<lists.length;l++) {
					Probe [] probes = lists[l].getAllProbes();

					progressUpdated("Calculating trend...", (lists.length*2)+l, lists.length * 3);

					for (int s=0;s<stores.length;s++) {
						DataStore store = stores[s];

						if (cancel) {
							progressCancelled();
							return;
						}


						downstreamData[l][s] = getMeanQuantitativeValues(downstreamProbes, store, probes, 200);

					}
				}
			}
		}
		catch (SeqMonkException e) {
			progressExceptionReceived(e);
			return;
		}

		// Find min and max values across the whole data.
		boolean valueSet = false;
		
		if (upstreamData != null) {
			
			double [][][] data = upstreamData;
			
			for (int p=0;p<data.length;p++) {
				for (int s=0;s<data[p].length;s++) {
					for (int d=0;d<data[p][s].length;d++) {
						if (Double.isNaN(data[p][s][d])) continue;
						
						if (!valueSet) {
							minValue = data[p][s][d];
							maxValue = minValue;
							valueSet = true;
						}
						else {
							if (data[p][s][d]> maxValue) {
								maxValue = data[p][s][d];
							}
							if (data[p][s][d] < minValue) {
								minValue = data[p][s][d];
							}
						}
						
					}
				}
			}
		}
		
		if (centralData != null) {
			
			double [][][] data = centralData;
			
			for (int p=0;p<data.length;p++) {
				for (int s=0;s<data[p].length;s++) {
					for (int d=0;d<data[p][s].length;d++) {
						if (Double.isNaN(data[p][s][d])) continue;
						
						if (!valueSet) {
							minValue = data[p][s][d];
							maxValue = minValue;
							valueSet = true;
						}
						else {
							if (data[p][s][d]> maxValue) {
								maxValue = data[p][s][d];
							}
							if (data[p][s][d] < minValue) {
								minValue = data[p][s][d];
							}
						}
						
					}
				}
			}
		}

		if (downstreamData != null) {
			
			double [][][] data = downstreamData;
			
			for (int p=0;p<data.length;p++) {
				for (int s=0;s<data[p].length;s++) {
					for (int d=0;d<data[p][s].length;d++) {
						if (Double.isNaN(data[p][s][d])) continue;
						
						if (!valueSet) {
							minValue = data[p][s][d];
							maxValue = minValue;
							valueSet = true;
						}
						else {
							if (data[p][s][d]> maxValue) {
								maxValue = data[p][s][d];
							}
							if (data[p][s][d] < minValue) {
								minValue = data[p][s][d];
							}
						}
						
					}
				}
			}
		}

		progressComplete();
	}

	private double [] getMeanQuantitativeValues (Probe [] windows, DataStore store, Probe [] probes, int numberOfDivisions) throws SeqMonkException {
		double [] values = new double[numberOfDivisions];
		int [] counts = new int[numberOfDivisions];

		int startIndex = 0;


		for (int w=0;w<windows.length;w++) {
			Probe window = windows[w];

			for (int p=startIndex;p<probes.length;p++) {
				Probe probe = probes[p];

				// See if we've shifted chromosomes
				if (p>0 && probe.chromosome() != probes[p-1].chromosome()) {
					// Go on until we find the first hit to the new 
					// chromosome and set that as the starting point
					for (;p<probes.length;p++) {
						if (probes[p].chromosome() == window.chromosome()) {
							startIndex = p;
							probe = probes[p];
							break;
						}
					}
				}

				if (window.chromosome() != probe.chromosome()) break;

				// See if we can shift the start point
				if (probe.end() < window.start() && probes[startIndex].end() < probe.end()) {
					startIndex = p;
					continue;
				}

				// See if we've gone so far we can stop looking
				if (probe.start() > window.end()) break;

				// At this point we should definitely overlap, but let's
				// do a sanity check anyway
				if (probe.start() <= window.end() && probe.end() <= window.start()) {
					// Now we need to find the extent of the overlap.  We're diving
					// the whole window into chunks so we need to find out which of
					// these we start and end in.

					int overlapStart = Math.max(probe.start(), window.start());
					int overlapEnd = Math.min(probe.end(), window.end());

					int divisionStart = Math.round((overlapStart - window.start())/(window.length()/(float)numberOfDivisions));
					int divisionEnd = Math.round((overlapEnd - window.start())/(window.length()/(float)numberOfDivisions));

					for (int i=divisionStart;i<=divisionEnd;i++) {
						values[i] += store.getValueForProbe(probe);
						counts[i]++;
					}

				}
				else {
					throw new IllegalStateException("Probe and window didn't overlap and they should have!");
				}

			}
		}

		//TODO: Interpolate missing values?

		// Now work out the mean values
		for (int i=0;i<values.length;i++) {
			if (counts[i]>0) {
				values[i] /= counts[i];
			}
			else {
				values[i] = Double.NaN;
			}
		}

		return values;

	}


	@Override
	public void cancel() {
		cancel = true;
	}

}
