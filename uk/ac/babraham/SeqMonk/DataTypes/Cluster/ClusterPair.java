/**
 * Copyright 2012-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataTypes.Cluster;

import java.util.Vector;

public class ClusterPair {

	private Integer index = null;
	private ClusterPair pair1 = null;
	private ClusterPair pair2 = null;
	private float rValue;
	
	public ClusterPair (Integer index) {
		this.index = index;
		this.rValue = 1;
	}
	
	public ClusterPair (ClusterPair pair1, ClusterPair pair2, float rValue) {
		this.pair1 = pair1;
		this.pair2 = pair2;
		this.rValue = rValue;
	}
	
	public ClusterPair pair1 () {
		return pair1;
	}
	
	public ClusterPair pair2 () {
		return pair2;
	}
	
	public Integer index () {
		return index;
	}
	
	public float rValue () {
		return rValue;
	}

	public Integer [] getAllIndices () {
		if (index != null) {
			return new Integer [] {index};
		}
		else {
			Vector<Integer>indices = new Vector<Integer>();
			getAllIndices(indices);
			return indices.toArray(new Integer[0]);
		}
	}
	
	public ClusterPair [] getConnectedClusters (float rValue) {
		if (rValue > 1) {
			rValue = 1;
		}
		Vector<ClusterPair> keepers = new Vector<ClusterPair>();
		getConnectedClusters(keepers,rValue);
		
		return keepers.toArray(new ClusterPair[0]);
	}
	
	protected void getConnectedClusters (Vector<ClusterPair>keepers, float rValue) {
		if (this.rValue >= rValue) {
			keepers.add(this);
		}
		else {
			pair1.getConnectedClusters(keepers,rValue);
			pair2.getConnectedClusters(keepers,rValue);
		}
	}
	
	protected void getAllIndices(Vector<Integer>indices) {
		if (index != null) {
			indices.add(index);
		}
		else {
			pair1.getAllIndices(indices);
			pair2.getAllIndices(indices);
		}
	}
	
}
