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

import core.Constants;
import core.Sim;
import core.Constants.StatName;

public class Statistic implements Serializable{

	private StatName stat_name;
	private boolean other_stats_warmed;
	
	private int n_warmup_samples;
	private double mean_accuracy;
	private double quantile;
	private double quantile_accuracy;
	
	private long good_samples;
	private long discarded_warmup_samples;
	private long discarded_steady_state_samples;
	private long total_samples;

	//Lag spacing variables
	private int calibration_samples;
	private Sequence calibration_sequence;
	private int max_lag_spacing;
	private int max_run;
	private double lag_confidence;
	private int lag;
	
	private boolean just_bins;
	
	private Histogram histogram;	
	private SimpleStatistic simple_stat;
	
	private StatsCollection stat_collection;
	
	protected Phase phase;
	
	private boolean combined;
	
	protected enum Phase{
		WARMUP, CALIBRATION, STEADYSTATE
	}
	
	public Statistic(StatsCollection statCollection, StatName statName, int nWarmupSamples, double meanAccuracy, 
			double quantile, double quantileAccuracy, SimpleStatistic simpleStat, Histogram histogram, 
			int lagSpace, long goodSamples, long totalSamples, long discardedSamples) {
		this(statCollection, statName, 0, meanAccuracy, quantile, quantileAccuracy);
		this.lag = lagSpace;
		this.simple_stat = simpleStat;
		this.histogram = histogram;
		this.combined = true;
		this.good_samples = goodSamples;
		this.total_samples = totalSamples;
		this.discarded_warmup_samples = discardedSamples;
	}//End Statistic

	public Statistic(StatsCollection statCollection, StatName statName, int nWarmupSamples, double meanAccuracy, double quantile, double quantileAccuracy, double[] xValues) {
		this(statCollection, statName, nWarmupSamples, meanAccuracy, quantile, quantileAccuracy);
		this.histogram = new Histogram(xValues);
	}
	
	public Statistic(StatsCollection statCollection, StatName statName, int nWarmupSamples, double meanAccuracy, double quantile, double quantileAccuracy) {
		this.just_bins = false;
		this.stat_collection = statCollection;
		this.combined = false;
		
		this.stat_name = statName;
		this.other_stats_warmed = false;
		
		this.good_samples = 0l;
		this.discarded_warmup_samples = 0l;
		this.discarded_steady_state_samples = 0l;
		this.total_samples = 0l;
		
		this.phase = Phase.WARMUP;
		this.n_warmup_samples = nWarmupSamples;
//		System.out.println("this cont mean accuracy to "+meanAccuracy);
		this.mean_accuracy = meanAccuracy;
		this.quantile = quantile;
		this.quantile_accuracy = quantileAccuracy;
		
		this.max_lag_spacing = 40;
		this.max_run = 50;
		this.lag_confidence = .99;
		this.lag = 1;
		
		this.simple_stat = new SimpleStatistic();
		this.calibration_sequence = new Sequence();
		this.calibration_samples = 5000;
		
		
	}//End Statistic()
	
	public void setJustBins(boolean justBins) {
		this.just_bins = justBins;
	}

	public void addSample(double value) {
		
		if(this.combined) {
			Sim.fatalError("Shouldn't add samples after being combined");
		}
		
		if(this.phase == Phase.WARMUP) { 
			
			this.discardWarmupSample(value);
			if(this.discarded_warmup_samples == this.n_warmup_samples){
				this.stat_collection.reportWarmed(this);
			}
			
			//Check if we're warmed
			if(this.discarded_warmup_samples >= this.n_warmup_samples && this.other_stats_warmed) {
			  this.phase = Phase.CALIBRATION;			  
				System.out.println(this.stat_name + " entered calibration");

			}
			
		} else if(this.phase == Phase.CALIBRATION) { 
						
			this.calibration_sequence.insert(value);
			
			if(this.calibration_sequence.getSize() > 100 && this.just_bins){
				double minValue = this.calibration_sequence.getMinValue();
				double maxValue = this.calibration_sequence.getMaxValue();
				System.out.println("Creating histogram with min " + minValue + " maxValue "+maxValue);
				this.histogram = new Histogram(10000, minValue/2, maxValue*2); // let's delay creating the histogram
				this.phase = Phase.STEADYSTATE;
				System.out.println(this.stat_name + " entered steady state and only took 100 samples");
			} else if(this.calibration_sequence.getSize() >= this.calibration_samples) {
				//Calibration is over
				this.lag = this.calibration_sequence.calculateLagSpacing(max_lag_spacing, max_run, lag_confidence);
				if(this.lag < 0) {
					Sim.fatalError("Couldn't find a valid lag spacing for " + this.stat_name);
				}
				
				double minValue = this.calibration_sequence.getMinValue();
				double maxValue = this.calibration_sequence.getMaxValue();
				System.out.println("Creating histogram with min " + minValue + " maxValue "+maxValue);
				if(this.histogram == null) {
					this.histogram = new Histogram(10000, minValue, maxValue); // let's delay creating the histogram
				}
				
				this.phase = Phase.STEADYSTATE;
				System.out.println(this.stat_name + " entered steady state, lag spacing of " + this.lag);
			}
				
		} else {
//			System.out.println("total samples is "+this.total_samples +" this.total_samples % this.lag "+this.total_samples % this.lag);
			if( (this.total_samples % this.lag) == 0) {
				this.keepSample(value);
			} else {
				this.discardSteadyStateSample(value);
			}
		}
		
		this.total_samples++;
		
		//Assert sample balance
		if(this.total_samples != (this.good_samples + this.discarded_warmup_samples+ this.discarded_steady_state_samples + this.calibration_sequence.getSize())) {
			Sim.fatalError("Total samples " + this.total_samples + "" +
					"!= good samples "+this.good_samples+ 
					" calibration samples " + this.calibration_sequence.getSize() +
					" discarded steady state samples " + this.discarded_steady_state_samples +
					" discarded warmup samples " + this.discarded_warmup_samples);
		}
		
	}//End addSample()
	
	public CombinedStatistic combineStatistics(Statistic stat) {
		
		if(!this.stat_name.equals(stat.getStatName())) {
			Sim.fatalError("Cannot combined statistics unless they're the same kind");
		}
		
		SimpleStatistic combinedSimpleStat = this.simple_stat.combineSimpleStatistics(stat.simple_stat);
		Histogram combinedHistogram = this.histogram.combineHistogram(stat.histogram);
		
		long combinedGoodSamples = this.good_samples + stat.good_samples;
		long combinedTotalSamples = this.total_samples+ stat.total_samples;
		long combinedDiscardedSamples = Math.min(this.discarded_warmup_samples, stat.discarded_warmup_samples);
		System.out.println("Going to set mean accuracy to "+this.mean_accuracy);
		CombinedStatistic combinedStatistic = new CombinedStatistic(this.stat_collection, this.stat_name, this.n_warmup_samples, 
				this.mean_accuracy, this.quantile, this.quantile_accuracy, combinedSimpleStat, combinedHistogram, 
				this.lag, combinedGoodSamples, combinedTotalSamples, combinedDiscardedSamples);		
		
		return combinedStatistic;
		
	}//End combineStatistics()
	
	public long getGoodSamples(){
		return this.good_samples;
	}//End getGoodSamples()

	public StatName getStatName() {
		return this.stat_name;
	}//End getStatName()
	
	public long getTotalSamples(){
		return this.total_samples;
	}//End getTotalSamples()
	
	private void keepSample(double value) {
		
		this.simple_stat.addSample(value);
		this.histogram.addSample(value);

		this.good_samples++;

	}//End discardSample()
	private void discardSteadyStateSample(double value) {
		this.discarded_steady_state_samples++;
	}//End discardSteadyStateSample()
	
	private void discardWarmupSample(double value) {
		
		this.discarded_warmup_samples++;

	}//End discardWarmupSample()
	
	public void setOtherStatsWarmed(boolean warmed) {
		this.other_stats_warmed = warmed;
	}//End setOtherStatsWarmed()
	
	public double getStdDev() {
		return this.simple_stat.getStdDev();
	}//End getStdDev()
	
	public double getAverage() {
		return this.simple_stat.getAverage();
	}//End getAverage()
	
	public double getQuantileAccuracy(){

		double std = this.quantile * this.quantile;
		double z = Constants.Z_95_CONFIDENCE;
		double n_root = Math.sqrt(this.good_samples);
		double range = z * std/n_root;
		double accuracy = range/this.getAverage();

		return accuracy;
		
	}//End getCurrentMeanConverge()
	
	public double getMeanAccuracy(){

		double std = this.getStdDev();
		double z = Constants.Z_95_CONFIDENCE;
		double n_root = Math.sqrt(this.good_samples);
		double range = z * std/n_root;
		double accuracy = range/this.getAverage();

		return accuracy;
		
	}//End getCurrentMeanConverge()
	
	public boolean isMeanCoverged() {

		if( this.getMeanAccuracy() < this.mean_accuracy) {
			return true;
		} else {
			return false;
		}
	}//End isMeanCoverged()
	
	public boolean isQuantileConverged() {
		if( this.getQuantileAccuracy() < this.quantile_accuracy) {
			return true;
		} else {
			return false;
		}		
	}//End isQuantileConverged()
	
	public boolean isSteadyState() {
		if( this.phase == Phase.STEADYSTATE ) {
			return true;
		} else {
			return false;
		}		
	}//End isSteadyState()
	
	public boolean isConverged() {
		
		if(this.good_samples < 100){
			return false;
		}
		
		if(this.isMeanCoverged() && isQuantileConverged() && isSteadyState()){
			return true;
		} else {
//			System.out.println("mean acc " + this.getMeanAccuracy()+ " not converged mean "+this.isMeanCoverged()+" quantile "+this.isQuantileConverged() +" steady "+this.isSteadyState());
			return false;
		}
	}//End isConverged()
	
	public double getCdfValue(double value) {
		return this.histogram.getCdfValue(value);
	}//End getCdfValue()
	
	public double getQuantile(double quantile) {
		if(this.histogram == null){
			return 0.0d;
		}
		return this.histogram.getQuantile(quantile);		
	}//End getQuantile()

	public double getQuantileSetting() {
		return quantile;
	}

	public void printCdf() {
		this.histogram.printCdf();
	}//End printCdf()
	
	public void printHistogram() {
		this.histogram.printHistogram();
	}//End printHistogram()


	public void setHistogramXValues(double[] xValues){
		this.histogram = new Histogram(xValues);
	}//End setHistogramXValues()
	
	public double[] getHistogramXValues() {
		if(this.histogram == null){
			return null;
		}
		return this.histogram.getXValues();		
	}//End getXValues

	public double getTargetQuantile() {
		return this.quantile;
	}

	public void printStatInfo() {
		String out = "name: "+this.stat_name+", averageValue: "+getAverage()+", averageAccuracy: "+this.getMeanAccuracy()
		+", quatileTarget: "+this.getTargetQuantile()+", quantileValue: "+this.getQuantile(this.getTargetQuantile())+", quantileAccuracy: "+this.getQuantileAccuracy() 
		+ ", goodSamples: "+this.good_samples + ", warmupSamples: "+this.discarded_warmup_samples+ ", calibrationSamples: "+this.calibration_samples 
		+", stdDev: "+this.getStdDev() + ", lag: "+this.lag;
		System.out.println(out);
		
	}//End printAllStatInfo()
	
}//End class Statistic
