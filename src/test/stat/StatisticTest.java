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
package test.stat;

import generator.ExponentialGenerator;
import generator.MTRandom;
import junit.framework.TestCase;

import org.junit.Test;

import stat.SimpleStatistic;
import stat.Statistic;
import stat.StatsCollection;
import core.Constants.StatName;

public class StatisticTest extends TestCase {

//	@Test
//	public void testCombineStatistics(){
////		assertEquals(combinedStat.getGoodSamples(), stat1.getGoodSamples() + stat2.getGoodSamples());
////		assertEquals(combinedStat.getDiscardedSamples(), stat1.getDiscardedSamples() + stat2.getDiscardedSamples());
////		assertEquals(combinedStat.getTotalSamples(), stat1.getTotalSamples() + stat2.getTotalSamples());
////		assertEquals(combinedStat.getTotalSamples(), nStat1Samples + nStat2Samples);
//
//	}//End testCombineStatistics()

	@Test
	public void testRandomGenerator(){
		int nWarmupSamples = 10;
		double meanAccuracy = .05;
		double quantile = .95;
		double quantileAccuracy = .05;
		StatsCollection statCollection = new StatsCollection();
		Statistic stat = new Statistic(statCollection, StatName.BUSY_PERIOD_TIME, nWarmupSamples, meanAccuracy, quantile, quantileAccuracy);
		stat.setOtherStatsWarmed(true);
		while(!stat.isConverged()) {
			stat.addSample(Math.random());
		}
	}
	
	
	@Test
	public void testExponentialDistributedSampling(){
		
		double lambda = .5;
		ExponentialGenerator exp = new ExponentialGenerator(new MTRandom(), lambda);
		int nWarmupSamples = 10;
		double meanAccuracy = .05;
		double quantile = .95;
		double quantileAccuracy = .05;

		StatsCollection statCollection = new StatsCollection();

		Statistic stat1 = new Statistic(statCollection, StatName.SOJOURN_TIME, nWarmupSamples, meanAccuracy*2, quantile, quantileAccuracy*2);
		stat1.setOtherStatsWarmed(true);
		
		
		Statistic stat2 = new Statistic(statCollection, StatName.SOJOURN_TIME, nWarmupSamples, meanAccuracy*2, quantile, quantileAccuracy*2);
		stat2.setOtherStatsWarmed(true);
		
		Statistic stat3 = new Statistic(statCollection, StatName.SOJOURN_TIME, nWarmupSamples, meanAccuracy*2, quantile, quantileAccuracy*2);
		stat3.setOtherStatsWarmed(true);
		
		Statistic stat4 = new Statistic(statCollection, StatName.SOJOURN_TIME, nWarmupSamples, meanAccuracy*2, quantile, quantileAccuracy*2);
		stat4.setOtherStatsWarmed(true);
		
		while(!stat1.isConverged()) {
			stat1.addSample(exp.next());
		}
		double[] xValues = stat1.getHistogramXValues();
		stat2.setHistogramXValues(xValues);
		stat3.setHistogramXValues(xValues);
		stat4.setHistogramXValues(xValues);
		
		
		while(!stat2.isConverged()) {
			stat2.addSample(exp.next());
		}
		while(!stat3.isConverged()) {
			stat3.addSample(exp.next());
		}
		while(!stat4.isConverged()) {
			stat4.addSample(exp.next());
		}
		
		Statistic uberStat = stat1.combineStatistics(stat2);
		uberStat = uberStat.combineStatistics(stat3);
		uberStat = uberStat.combineStatistics(stat4);
		
		uberStat.setOtherStatsWarmed(true);
		
		while(!uberStat.isConverged()) {
			stat1.addSample(exp.next());
		}
		
		double tolerance = .05;
		double expectedValue = 2;
		double actualValue = uberStat.getAverage();
		assertEquals(expectedValue, actualValue, tolerance*expectedValue);
		
		tolerance = .05;
		expectedValue = -Math.log(1-.95)/lambda;
		actualValue = uberStat.getQuantile(.95);
		assertEquals(expectedValue, actualValue, tolerance*expectedValue);
		
		tolerance = .05;
		expectedValue = 1.0-Math.exp(-lambda*2);
		actualValue = uberStat.getCdfValue(2);
		assertEquals(expectedValue, actualValue, tolerance*expectedValue);
		
		
	}//End testExponentialDistributedSampling
	
	@Test
	public void testExponentialSampling(){
		
		double lambda = .5;
		ExponentialGenerator exp = new ExponentialGenerator(new MTRandom(),lambda);
		int nWarmupSamples = 10;
		double meanAccuracy = .05;
		double quantile = .95;
		double quantileAccuracy = .05;
		
		StatsCollection statCollection = new StatsCollection();

		Statistic stat = new Statistic(statCollection, StatName.SOJOURN_TIME, nWarmupSamples, meanAccuracy, quantile, quantileAccuracy);
		stat.setOtherStatsWarmed(true);
		while(!stat.isConverged()) {
			stat.addSample(exp.next());
		}
		
		double tolerance = .05;
		double expectedValue = 2;
		double actualValue = stat.getAverage();
		assertEquals(expectedValue, actualValue, tolerance*expectedValue);
		
		tolerance = .05;
		expectedValue = -Math.log(1-.95)/lambda;
		actualValue = stat.getQuantile(.95);
		assertEquals(expectedValue, actualValue, tolerance*expectedValue);
		
		tolerance = .05;
		expectedValue = 1.0-Math.exp(-lambda*2);
		actualValue = stat.getCdfValue(2);
		assertEquals(expectedValue, actualValue, tolerance*expectedValue);
	}

}//End class ServerTest
