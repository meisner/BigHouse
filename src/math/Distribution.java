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
package math;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import core.Sim;

public class Distribution implements Serializable{

	//Store the distribution as a CDF because it's easy to get the PDF value from it
	
	private double[] xs;
	private double[] ys;
	private double mean;
	
	public Distribution(double[] xs, double[] ys){
		
		if(xs.length != ys.length){
			Sim.fatalError("X and Y vector must be the same length in a distribution");
		}
		double lastY= 0.0d;
		double expectedValue = 0.0d;
		for(int i = 0; i < ys.length; i++){
			
			if(lastY > ys[i]){
				Sim.fatalError("Y (CDF) values must be monotonically increasing");
			}
			
			if(ys[i] > 1.0){
				Sim.fatalError("Probablity can't be greater than 1.0 ys["+i+"] = "+ys[i]);
			}
			double diff = ys[i] - lastY;
			expectedValue += xs[i]*diff;
			
			lastY = ys[i];
		}
		this.xs = xs;
		this.ys = ys;
		this.mean = expectedValue;
	}
	
	public double getMean(){
		return this.mean;
	}

	private static class XYPair{
		public double x;
		public double y;

	}
	
	public double[] getXs(){
		return this.xs;
	}

	public double[] getYs(){
		return this.ys;
	}
	
	public static Distribution loadDistribution(String fileName){
		return Distribution.loadDistribution(fileName, 1.0);
	}//End loadDistribution()
	
	/*
	 * The CDF should have 0 0 at the beginning and X 1.0 at the end
	 */
	public static Distribution loadDistribution(String fileName, double scalingFactor){
	
		//Use a vector since we don't know how many points there are
		Vector<XYPair> xypairs = new Vector<XYPair>();

		try{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream(fileName);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			
			while ((strLine = br.readLine()) != null)   {
		        String[] parts = strLine.split(" ");
		        XYPair pair = new XYPair();
		        pair.x=Double.valueOf(parts[0]);
		        pair.y=Double.valueOf(parts[1]);
//		        System.out.println(strLine + " to "+pair.x + " "+pair.y);
		        xypairs.add(pair);
			}
			//Close the input stream
			br.close();
			in.close();
			fstream.close();
			
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
			System.err.println("File is " + fileName);
			Sim.fatalError("Couldn't load distribution file");		
		}
		 //Convert to arrays
	    int entries = xypairs.size();
	    double[] ys= new double[entries];
	    double[] xs= new double[entries];
	    Iterator<XYPair> xyiter = xypairs.iterator();
	    int i = 0;
	    while(xyiter.hasNext()){
	      XYPair pair = xyiter.next();
	      xs[i] = pair.x * scalingFactor;
	      ys[i] = pair.y;
	      i++;
	    }
		
	    if(xs[0] != 0 || ys[0] != 0){
	    	System.out.println("xs[0] = "+xs[0]);
	    	System.out.println("ys[0] = "+ys[0]);
	    	Sim.fatalError("The first line of a cdf file needs to be 0 0");
	    }
	    
//	    if(!(Math.abs(ys[ys.length-1] - 1.0) < .0001)){
//	    	Sim.fatalError("The last line of a cdf file needs to be x 1.0, was "+ys[ys.length-1]);
//	    }
		Distribution distribution = new Distribution(xs,ys);
		return distribution;
	}
	
	/* Get the x value of a quantile of the CDF */
	public double getQuantile(double quantile){
		
		int bin = searchForBin(this.ys, quantile);

		//Note x and y are reversed
		double xValue = this.linearlyInterpolateBin(this.ys, this.xs, bin,  quantile);
		
		return xValue;
	}

//	public double getPdfValue(double x){
//		
//		int bin = searchForBin(this.xs, x);
//		
//		double yValue = this.linearlyInterpolateBin(this.xs, this.ys, bin, x);
//		
//		return yValue;
//		
//	}

	public double getCdfValue(double x){
		//Get the bin with the closest value
		int bin = searchForBin(this.xs, x);
		double y = linearlyInterpolateBin(this.xs, this.ys, bin, x);
		return y;
	}
	
	private double linearlyInterpolateBin(double[] xValues, double[] yValues, int bin, double x){
	
		double y = 0.0d;

		//Handle corner case
		if (bin == xs.length -1) {
			y = yValues[bin];
		} else {
			
			double x0 = xValues[bin];
			double y0 = yValues[bin];
			
			double x1 = xValues[bin+1];
			double y1 = yValues[bin+1];			
			
			y = (y1 - y0)/(x1 - x0) * (x - x0) + y0;
//			System.out.println("Bin " + bin + " Target x = " + x + " interping between x0 = " + x0 + " y0 = " + y0 + " and x1 = "+x1+ " y1 = "+ y1 +" and it's " + y);

		}
		
		return y;
	}
	
	/*
	 * Returns the biggest bin which is smaller than the value
	 */
	public static int searchForBin(double[] valueArray, double value){
		
		int bin = Arrays.binarySearch(valueArray, value);
		
		if(bin < 0){
			bin = -bin-2;
		}
		
		if(bin < 0){
			bin = 0;		
		}
		
		if(bin > valueArray.length-1){
			bin = valueArray.length-1;
		}
		return bin;
		
	}
	
	public static Distribution getExponentialDistribution(double lambda, int bins, double xMin, double xMax) {
	
		double stepSize = (xMax - xMin)/(bins-2);
		double[] xs = new double[bins];
		double[] ys = new double[bins];
		
		for( int i = 0; i < bins-1; i++){
			double x = stepSize * i;
			double cdfValue = (1 - Math.exp(-lambda * x));
			xs[i] = x;
			ys[i] = cdfValue;
		}
		
		xs[bins-1] = stepSize * (bins-1);
		ys[bins-1] = (1 - Math.exp(-lambda * xs[bins-1]));
		
		Distribution exponentialDistribution = new Distribution(xs, ys);
		return exponentialDistribution;
		
	}
	
}//End class Distribution
