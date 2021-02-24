/**
 * Copyright 2012- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Analysis.Statistics;

import java.util.Iterator;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class PCA implements Runnable, Cancellable {

	private double [][] data;
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;
	private double [] extractedEigenValues = null;

	public PCA (double [][] data) {
		this.data = data;
	}
	
	public void startCalculating () {
		Thread t = new Thread(this);
		t.start();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
	}
	
	public double [] extractedEigenValues () {
		return extractedEigenValues;
	}
	
	/**
	 * Adds a progress listener.
	 * 
	 * @param l The listener to add.
	 */
	public void addProgressListener (ProgressListener l) {
		if (l == null) {
			throw new NullPointerException("ProgressListener can't be null");
		}
		
		if (! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	/**
	 * Removes a progress listener.
	 * 
	 * @param l The listener to remove
	 */
	public void removeProgressListener (ProgressListener l) {		
		if (l !=null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}
	
	/**
	 * Informs all progress listeners of an update to the progress
	 * 
	 * @param message The message to display
	 * @param current The current level of completion
	 * @param total The final level of completion
	 */
	protected void progressUpdated(String message, int current, int total) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressUpdated(message, current, total);
		}
	}
		
	/**
	 * Informs all progress listeners that an exception was received
	 * 
	 * @param e The exception received
	 */
	protected void progressExceptionReceived (Exception e) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressExceptionReceived(e);
		}
	}

	/**
	 * Informs all progress listeners that quantitation was cancelled.
	 */
	protected void progressCancelled () {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressCancelled();
		}
	}
	
	/**
	 * Informs all progress listeners that quantitation is complete.
	 */
	protected void progressComplete() {
		
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressComplete("pca",extractedEigenValues);
		}
	}

	public void run() {

		subtractMean();
		
		progressUpdated("Calculating covariance matrix",0,4);

		
		// We need to start by making a covariance matrix
		double [][] rawCovarianceMatrix = calculateCovarianceMatrix(data);

		if (rawCovarianceMatrix == null && cancel) {
			progressCancelled();
			return;
		}

		Matrix covarianceMatrix = new Matrix(rawCovarianceMatrix);
		
		
		progressUpdated("Creating decomposition",1,4);
		
		EigenvalueDecomposition decomp = new EigenvalueDecomposition(covarianceMatrix);
		
		if (cancel) {
			progressCancelled();
			return;
		}

		progressUpdated("Calculating eigenvectors",2,4);
		
		double [] eigenValues = decomp.getRealEigenvalues();
		
		Matrix eigenVectors = decomp.getV();
		
		progressUpdated("Calculating eigenvalues", 3,4);
		
//		System.err.println("Eigenvectors have dimensions "+eigenVectors.getRowDimension()+" x "+eigenVectors.getColumnDimension());
		
		double largestEigenValue = 0;
		int largestIndex = -1;
		
		for (int i=0;i<eigenValues.length;i++) {
//			System.err.println("Eigenvalue "+i+" is "+eigenValues[i]);
			
			if (Math.abs(eigenValues[i]) > largestEigenValue) {
				largestEigenValue = Math.abs(eigenValues[i]);
				largestIndex = i;
			}
		}
		
//		System.err.println("Largest Eigenvalue is "+largestEigenValue+" from index "+largestIndex);
				
		if (cancel) {
			progressCancelled();
			return;
		}

		Matrix principalComponent = eigenVectors.getMatrix(0, eigenVectors.getRowDimension()-1,largestIndex,largestIndex).transpose();
		
//		System.out.println("Principal component is "+principalComponent.get(0, 0)+","+principalComponent.get(0,1));
		
		// Work out the eigenvalue for each column of the original matrix against
		// the principal component
		extractedEigenValues = new double[data[0].length];
		for (int r=0;r<extractedEigenValues.length;r++) {
			
			if (cancel) {
				progressCancelled();
				return;
			}

			double [][] rowData = new double [1][data.length];
			for (int i=0;i<data.length;i++) {
				rowData[0][i] = data[i][r]; 
			}
			Matrix row = new Matrix(rowData);

//			System.err.println("PC is "+principalComponent.getRowDimension()+"x"+principalComponent.getColumnDimension()+" data is "+row.getRowDimension()+"x"+row.getColumnDimension());	


			Matrix product = row.arrayTimes(principalComponent);

			
			for (int i=0;i<product.getColumnDimension();i++) {
				extractedEigenValues[r] += product.get(0, i);
			}

//			System.err.println("Product is "+principalComponent.get(0,0)+"x"+principalComponent.get(0,1)+" data is "+row.get(0,0)+"x"+row.get(0,1)+" Values are "+product.get(0,0)+"x"+product.get(0,1)+"="+extractedEigenValues[r]);	

			
//			System.out.println("Product of "+principalComponent.get(0, 0)+" and "+principalComponent.get(0,1)+" vs "+col.get(0,0)+" and "+col.get(0,1)+" is "+extractedEigenValues[r]);
			
//			System.out.println("Value for row "+r+" is "+extractedEigenValues[r]);
		}
		
		progressComplete();
	}
	
	private void subtractMean () {
		for (int r=0;r<data.length;r++) {
			double total = 0;
			for (int i=0;i<data[r].length;i++) {
				total += data[r][i];
			}
			total /= data[r].length;
			
			for (int i=0;i<data[r].length;i++) {
				data[r][i] -= total;
			}
		}
	}
	
	private double [][] calculateCovarianceMatrix (double [][] rawMatrix) {
		
		double [][] covarianceMatrix = new double[rawMatrix.length][rawMatrix.length];
		
		for (int i=0;i<covarianceMatrix.length;i++) {
			for (int j=i;j<covarianceMatrix.length;j++) {
				
				if (cancel) return null;
				
				// Calculate the covariance between i and j
				
				// Work out mean of i
				double meanI = 0;
				for (int x=0;x<rawMatrix[i].length;x++) {
					meanI += rawMatrix[i][x];
				}
				meanI /= rawMatrix[i].length;
				
				// Work out the mean of j
				double meanJ = 0;
				for (int x=0;x<rawMatrix[j].length;x++) {
					meanJ += rawMatrix[j][x];
				}
				meanJ /= rawMatrix[j].length;
				
				// Sum up the differences from the mean
				
				double cov = 0;
				for (int x=0;x<rawMatrix[i].length;x++) {
					cov += (rawMatrix[i][x]-meanI)*(rawMatrix[j][x]-meanJ);
				}
				cov /= rawMatrix[i].length - 1;
				
//				System.out.println("Cov of "+i+" vs "+j+" is "+cov);
				
				// Now assign this into the covariance matrix
				covarianceMatrix[i][j] = cov;
				covarianceMatrix[j][i] = cov;
				
			}
		}
		
		return covarianceMatrix;
		
	}
	
	
	public static void main (String [] args) {
		
		double [][] matrix = new double [][] {{2.5,0.5,2.2,1.9,3.1,2.3,2,1,1.5,1.1},{2.4,0.7,2.9,2.2,3.0,2.7,1.6,1.1,1.6,0.9}};

//		double [][] matrix = new double [100][100];
//		
//		for (int i=0;i<matrix.length;i++) {
//			for (int j=i;j<matrix.length;j++) {
//				if (j==i) {
//					matrix[i][j] = 1;
//				}
//				else {
//					double corr = Math.random();
//					matrix[i][j] = corr;
//					matrix[j][i] = corr;
//				}
//			}
//		}
		
		
		PCA pca = new PCA(matrix);
		
		pca.addProgressListener(new ProgressListener() {
			
			public void progressWarningReceived(Exception e) {
				e.printStackTrace();
			}
			
			public void progressUpdated(String message, int current, int max) {
				System.out.println(message);
			}
			
			public void progressExceptionReceived(Exception e) {
				e.printStackTrace();
			}
			
			public void progressComplete(String command, Object result) {
				System.out.println("Complete");
			}
			
			public void progressCancelled() {
				System.err.println("Cancelled");
			}
		});

		pca.startCalculating();
		
		while (pca.extractedEigenValues == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for (int i=0;i<pca.extractedEigenValues.length;i++) {
			System.out.println("Eigenvalue for "+i+" is "+pca.extractedEigenValues[i]);
		}
	}
	
 	
}
