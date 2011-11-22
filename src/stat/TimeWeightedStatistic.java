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

import core.Sim;
import core.Constants.StatName;
import core.Constants.TimeWeightedStatName;

public class TimeWeightedStatistic extends Statistic {

	private double window_size;
	private double sample_window_start;
	private double last_sample_time;
	private double average_accum;
	private double old_value;
	private boolean old_value_set;
	
	private double accum_weight; //Used for sanity checking
	
	private TimeWeightedStatName name;

	public TimeWeightedStatistic(StatsCollection statsCollection, TimeWeightedStatName name, int nWarmupSamples, double meanPrecision, double quantile, double quantilePrecision, double windowSize) {
		super(statsCollection, null, nWarmupSamples,meanPrecision, quantile, quantilePrecision);

		this.name = name;
		this.sample_window_start = 0.0d;
		this.window_size = windowSize;
		this.last_sample_time = 0.0d;
		this.average_accum = 0.0d;
		this.accum_weight = 0.0d;
		this.old_value = 0.0d;
		this.old_value_set = false;
		
	}//End TimeWeightedStatistic()

	
	public void addSample(double newValue, double time){
		
		
//		System.out.println("at " + time + " util is " + newValue);
		if(this.old_value_set == false) {
			this.old_value = newValue;
			this.old_value_set = true;
			return;
		}
		
		double value = this.old_value;
		this.old_value = newValue;
		double currentPeriodLength = time - this.sample_window_start;

		if(currentPeriodLength > this.window_size) {
			
			//There are three pieces			
			//The part that falls in the first window
			//  | -------------------------------------------|---------------------------|----------------------|
			//  sample_window_start        ^last_sample_time         ^time
			double weight = (this.window_size - (this.last_sample_time -  this.sample_window_start));
			if(weight/this.window_size > 1.0) {
				System.out.println("weight " + weight + " window_size " +this.window_size + " last_sample_time " + this.last_sample_time + " sample_window_start "+this.sample_window_start);
				Sim.fatalError("This ratio shouldn't be > 1");			
			}//End if
			if(Math.abs(weight) < 1e-8 ) {
				weight=0;
			}
			this.accum_weight += weight;
			double firstWindowPart = weight * value;
			average_accum += firstWindowPart;
			
			double overallAverage = average_accum/this.window_size;

			
//			if(Math.abs(overallAverage) < 1e-6 ) {
//				overallAverage=0;
//			}
//			else 
				if (overallAverage < 0 ){
					System.out.println("average_accum "+average_accum+",  weight "+weight +", last_sample_time " + last_sample_time + ", sample_window_start "+ sample_window_start + ", value " + value+ ", currentPeriodLength " + currentPeriodLength + ", time "+ time);
				Sim.fatalError("overallAverage is < 0: " + overallAverage);
			}
			super.addSample(overallAverage);
			
			double remainder = currentPeriodLength - this.window_size;
	
			//A window that is just one value
			int wholePeriods = (int) Math.floor(remainder/this.window_size);
			
			for(int i = 0; i < wholePeriods; i++) {
				super.addSample(value);
			}//End for i
			
			//A new window with just a portion filled
			remainder = remainder - this.window_size * (wholePeriods);
			this.average_accum = remainder * value;
			this.accum_weight = remainder;

			this.sample_window_start = this.window_size * (wholePeriods + 1) + this.sample_window_start;
			this.last_sample_time = time;	

//			System.out.println("Has good samples: " + super.getNGoodSamples());
			
		} else { 
			
			double timeWeight = time - this.last_sample_time;
			this.average_accum += timeWeight * value;
			this.last_sample_time = time;
			
		}//End if

	}//End addSample()

	
	public void setWindowSize(double size) {
		this.window_size = size;		
	}

}//End class TimeWeightedStatistic
