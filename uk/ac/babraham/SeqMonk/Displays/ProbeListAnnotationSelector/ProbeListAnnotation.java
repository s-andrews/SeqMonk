package uk.ac.babraham.SeqMonk.Displays.ProbeListAnnotationSelector;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class ProbeListAnnotation {

	private ProbeList list;
	private String annotation;
	
	public ProbeListAnnotation (ProbeList list, String annotation) {
		this.list = list;
		this.annotation = annotation;
	}
	
	
	public ProbeList list () {
		return list;
	}
	
	public String annotation () {
		return annotation;
	}
		
	
}
