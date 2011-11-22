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
import java.util.Iterator;
import java.util.Vector;

import core.Sim;

import umontreal.iro.lecuyer.probdist.ChiSquareDist;

public class Sequence implements Serializable{

	private Vector<Double> sequence;
	private SimpleStatistic simple_stat;

	public Sequence() {
		this.sequence = new Vector<Double>();
		this.simple_stat = new SimpleStatistic();
	}//End Sequence()

	public void insert(double value) {
		this.sequence.add(value);		
		this.simple_stat.addSample(value);
	}//End insert()

	public double[] getValues() {

		double[] values = new double[this.sequence.size()];

		Iterator<Double> iter = this.sequence.iterator();
		int i = 0;
		while(iter.hasNext()){
			values[i] = iter.next().doubleValue();
			i++;
		}//End while

		return values;

	}//End getValues()

	public static double chiSquaredQuantile(double quantile, int degreesFreedom) {

		return ChiSquareDist.inverseF(degreesFreedom, quantile);

	}

	private enum RunState{
		FIRST,SECOND,UP,DOWN,SKIP
	}//End enum RunState

	public static int[] getRunCounts(double[] values, int maxRun) {

		int[] runCounts = new int[maxRun];

		double lastValue = 0;
		int runLength = 0;
		
		RunState state = RunState.FIRST;
		
		for(int i = 0; i < values.length; i++) {

			double currentValue = values[i];
			switch(state) {

			case SKIP:
				state = RunState.FIRST;
				break;
			
			case FIRST:
				state = RunState.SECOND;
				break;

			case SECOND:
				
				if(currentValue > lastValue) {
					state = RunState.UP;
					runLength = 1;
				} else if(currentValue < lastValue) {
					state = RunState.DOWN;
					runLength = 1;
				} else {
					state = RunState.SECOND;
				}
				
				break;

			case UP:
				
				if(currentValue > lastValue) {
					runLength++;
				} else {
					if(runLength > maxRun){
						runCounts[maxRun-1] += 1;
					} else {
						runCounts[runLength-1] += 1;
					}
					state = RunState.FIRST;
					runLength = 0;
				}

				break;

			case DOWN:
				if(currentValue < lastValue) {
					runLength++;
				} else {
//					System.out.println("recording DOWN run of length " + runLength);
					if(runLength > maxRun){
						runCounts[maxRun-1] += 1;
					} else {
						runCounts[runLength-1] += 1;
					}
					state = RunState.FIRST;
					runLength = 0;
				}
				break;

			}//End switch	

			lastValue = currentValue;
		}//End for i

		return runCounts;

	}//End getRunCounts()

	public static boolean isIndependentByRunsTest(int[] runCounts, double confidence) {

		double totalCount = 0;
		double[] runProb = new double[runCounts.length];
		for(int i = 0; i < runCounts.length; i++) {
			totalCount += runCounts[i];
			int runLength = i+1;
			runProb[i] = runLength/((double) Sequence.factorial(runLength + 1) );

		}//End for i
		
		double testStatistic = 0;
		for(int i = 0; i < runCounts.length; i++){
			double rootNumerator = runCounts[i] - totalCount*runProb[i];//Avoid Math.pow
			testStatistic += (rootNumerator*rootNumerator)/(totalCount*runProb[i]);
		}//End for i
		
		double chiSquaredQuantile = Sequence.chiSquaredQuantile(confidence, runCounts.length);
		if(testStatistic < chiSquaredQuantile) {
			return true;
		} else { 			
			return false;
		}		

	}//End isIndependentByRunsTest()

	public static double[] getSpacedSequence(double[] seq, int spacing) {

		int nItems = (int)seq.length/spacing;
		double[] newSequence = new double[nItems];
		for(int i = 0; i < nItems; i++) {
			newSequence[i] = seq[i*spacing];
		}//End for i

		return newSequence;
	}//End getSpacedSequence()

	public static long factorial(int n){
		if( n <= 1 ) {
			return 1;
		} else {
			return n * factorial( n - 1 );
		}
	}//End factorial()

	public int getSize() {
		return this.sequence.size();		
	}//End size()

	public int calculateLagSpacing(int maxLagSpacing, int maxRun, double confidence) {

		int spacing = 1;
		double[] values = this.getValues();
		double[] spaced = Sequence.getSpacedSequence(values, spacing);
		int[] runCounts = Sequence.getRunCounts(spaced, maxRun);
		while(!Sequence.isIndependentByRunsTest(runCounts, confidence)) {
			
			spacing++;
			if(spacing > maxLagSpacing) {
				Sim.fatalError("Needed lag spacing is too good");
			}
			spaced = Sequence.getSpacedSequence(values, spacing);
			runCounts = Sequence.getRunCounts(spaced, maxRun);			
			
		}//End while
		
		return spacing;
		
	}//End calculateLagSpacing()

 	public double getMinValue() {
		return this.simple_stat.getMinValue();		
	}//End getMinValue()

	public double getMaxValue() {
		return this.simple_stat.getMaxValue();		
	}//End getMaxValue()


}//End class Sequence
