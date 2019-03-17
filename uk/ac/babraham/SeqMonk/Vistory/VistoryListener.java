package uk.ac.babraham.SeqMonk.Vistory;

public interface VistoryListener {

	public void blockAdded (VistoryBlock block);
	
	public void blockRemoved (VistoryBlock block);
	
	public void blockEdited (VistoryBlock block);
	
	public void blocksReordered();
	
	public void vistoryUpdated ();
	
	public void vistoryCleared ();
	
	
}
