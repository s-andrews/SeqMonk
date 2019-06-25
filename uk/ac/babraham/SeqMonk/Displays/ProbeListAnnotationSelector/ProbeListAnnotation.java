package uk.ac.babraham.SeqMonk.Displays.ProbeListAnnotationSelector;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class ProbeListAnnotation {

	private ProbeList list;
	private String annotation;
	private int index = -1;
	
	public ProbeListAnnotation (ProbeList list, String annotation) {
		this.list = list;
		this.annotation = annotation;
		
		for (int i=0;i<list.getValueNames().length;i++) {
			if (list.getValueNames()[i].equals(annotation)) {
				index = i;
				break;
			}
		}
		
	}
	
	
	public ProbeList list () {
		return list;
	}
	
	public String annotation () {
		return annotation;
	}
	
	public String toString () {
		return annotation+" ("+list.name()+")";
	}
	
	public int index () {
		return index;
	}
	
}
