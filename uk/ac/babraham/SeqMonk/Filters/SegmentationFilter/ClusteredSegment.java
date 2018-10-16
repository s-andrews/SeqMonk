package uk.ac.babraham.SeqMonk.Filters.SegmentationFilter;

public class ClusteredSegment {

	protected int startIndex;
	protected int endIndex;
	protected float mean;
	
	public ClusteredSegment (int startIndex, int endIndex, float mean) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.mean = mean;
	}
	
	
}
