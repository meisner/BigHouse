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
 * @author David Meisner (meisner@umich.edu)
 *
 */

package core;

import java.io.Serializable;

import core.Constants.StatName;
import core.Constants.TimeWeightedStatName;
import stat.Statistic;
import stat.StatsCollection;
import stat.TimeWeightedStatistic;

/** 
 * This class is just a big wrapper for StatsCollection 
 */
public class ExperimentOutput implements Serializable{
	
	private StatsCollection stats;
	
	public ExperimentOutput(){
		this.stats = new StatsCollection();

	}//End ExperimentOutput()
	
	public void addOutput(StatName name, double meanPrecision, double quantile, double quantilePrecision, int warmupSamples) {
		
		Statistic stat = new Statistic(stats, name, warmupSamples, meanPrecision, quantile, quantilePrecision);
		this.stats.addStatistic(name, stat);
		
	}//End addOutput
	
	public void addOutput(StatName name, double meanPrecision, double quantile, double quantilePrecision, int warmupSamples, double[] xValues) {
		
		Statistic stat = new Statistic(stats, name, warmupSamples, meanPrecision, quantile, quantilePrecision, xValues);
		this.stats.addStatistic(name, stat);
		
	}//End addOutput
	
	public void addTimeWeightedOutput(TimeWeightedStatName name, double meanPrecision, double quantile, double quantilePrecision, int warmupSamples, double window) {
		
		TimeWeightedStatistic stat = new TimeWeightedStatistic(stats, name, warmupSamples, meanPrecision, quantile, quantilePrecision, window);
		this.stats.addTimeWeightedStatistic(name, stat);
		
	}//End addTimeWeightedOutput

	public StatsCollection getStats() {
		return this.stats;
	}//End getStats()
	
	public Statistic getStat(StatName statName) {
		return this.stats.getStat(statName);
	}//End getStat()
	
	public Statistic getTimeWeightedStat(TimeWeightedStatName statName) {
		return this.stats.getTimeWeightedStat(statName);
	}//End getStat()
	
}//ExperimentOutput