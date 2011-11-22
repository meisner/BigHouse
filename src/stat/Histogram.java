/**
 * Copyright (c) 2011 The Regents of The University of Michigan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met: redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer;
 * redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution;
 * neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author: David Meisner (meisner@umich.edu)
 *
 */
package stat;

import java.io.Serializable;
import java.util.Arrays;

import core.Sim;

public class Histogram implements Serializable{

	private double[] x_values;
	private double[] y_values;
	private double[] cdf;
	
	
	public Histogram(int nBins, double minValue, double maxValue){
		
		this.x_values = new double[nBins];
		this.y_values = new double[nBins];
		double deltaX = (maxValue - minValue)/nBins;
		for(int i = 0; i < nBins; i++){
			this.x_values[i] = (i+1)*deltaX;
			this.y_values[i] = 0.0d;
		}//End for i
		this.cdf = null;
		
	}//End histogram
	
	public Histogram(double[] xValues){
		this.x_values = xValues.clone();
		this.y_values = new double[xValues.length];
		this.cdf = null;
	}//End Histogram
	
	public Histogram(double[] xValues, double[] yValues){
		this.x_values = xValues.clone();
		this.y_values = yValues.clone();
		this.cdf = null;
	}//End Histogram
	
	private int findClosestBin(double[] values, double searchValue){
		
		int bin = Arrays.binarySearch(values, searchValue);
		
		if(bin < 0){
			bin = -bin-1;
		}
		
		return bin;
		
	}//End findClosestBin()
	
	private void createCdf() {
		
		this.cdf = new double[this.x_values.length];
		
		double total = 0;
		for(int i = 0; i < this.x_values.length; i++) {
			total += this.y_values[i];
		}//End for i
		
		double runningCdf = 0.0d;
		for(int i = 0; i < this.x_values.length; i++) {
			double pdf = this.y_values[i]/total;
			runningCdf += pdf;
			this.cdf[i] = runningCdf;
		}//End for i
		
	}//End createCdf()
	
	public void addSample(double value) {
		
		this.cdf = null;
		int bin = findClosestBin(this.x_values, value);
		if(bin > this.y_values.length - 1){
			bin = this.y_values.length - 1;
		}
		this.y_values[bin] += 1;
		
	}//End addSample()
	
	public Histogram combineHistogram(Histogram histogram) {
		
		//Make sure we can combine these histograms
		double[] combinedYs = new double[x_values.length];
		for(int i = 0; i < this.x_values.length; i++) {
			if(Double.compare(this.x_values[i], histogram.x_values[i]) != 0){
				Sim.fatalError("Cannot combine histograms with different x values");
			}
			combinedYs[i] = this.y_values[i] + histogram.y_values[i];
		}//End for i
		
		Histogram combinedHistogram = new Histogram(this.x_values, combinedYs);		
				
		return combinedHistogram;
		
	}//End combineHistogram
	
	private double interpolate(double bottomX, double topX, double bottomY, double topY, double xValue) {
		return bottomY + (topY - bottomY)/(topX - bottomX) * ( xValue - bottomX);
	}//End interpolate()

	public double getQuantile(double quantile) {

		if(this.cdf == null){
			this.createCdf();
		}
		
		int bin = findClosestBin(this.cdf, quantile);
		double topX = this.cdf[bin];
		double topY = this.x_values[bin];
		
		double bottomX = 0.0d;
		double bottomY = 0.0d;
		
		if(bin != 0){
			bottomX = this.cdf[bin-1];
			bottomY = this.x_values[bin-1];
		}
		
		double xValue = interpolate(bottomX, topX, bottomY, topY, quantile);
		
		return xValue;		
		
	}//End getQuantile()

	public void printCdf() {

		if(this.cdf == null){
			System.out.println("CDF is null");
		} else {
			System.out.println("Bin, X, CDF");
			for(int i = 0; i < this.cdf.length; i++) {
				System.out.println(i + ", " + this.x_values[i] + ", "+this.cdf[i]);	
			}//End for i
		}
		
	}//End printCdf()

	public void printHistogram() {
		System.out.println("Bin, X, Count");
		for(int i = 0; i < this.cdf.length; i++) {
			System.out.println(i + ", " + this.x_values[i] + ", "+this.y_values[i]);	
		}//End for i
	}//End printHistogram()

	public double[] getYValues() {
		return this.y_values;		
	}//End getYValues()

	public double getCdfValue(double xValue) {
		
		if(this.cdf == null){
			this.createCdf();
		}
		
		int bin = findClosestBin(this.x_values, xValue);
	
		double topX = this.x_values[bin];
		double topY = this.cdf[bin];
		
		double bottomX = 0.0d;
		double bottomY = 0.0d;
		
		if(bin != 0){
			bottomX = this.x_values[bin-1];
			bottomY = this.cdf[bin-1];
		}
		
		double cdfValue = interpolate(bottomX, topX, bottomY, topY, xValue);
		
		return cdfValue;		
		
	}//End getCdfValue()

	public double[] getXValues() {
		return this.x_values;//End getXValues		
	}//End getXValues()
	
}//End histogram
