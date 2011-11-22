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
import junit.framework.TestCase;

import org.junit.Test;

import stat.Histogram;
import stat.SimpleStatistic;
import stat.Statistic;
import core.Constants.StatName;

public class HistogramTest extends TestCase {

	@Test
	public void testInsert(){
		
		Histogram histogram = new Histogram(10, 0, 100);
		histogram.addSample(-1.0);
		assertEquals(1, histogram.getYValues()[0], .001); 
		
		histogram.addSample(1000);
		double[] yValues = histogram.getYValues(); 
		assertEquals(1, yValues[yValues.length-1], .001); 
		
	}//End testInsert()
	
	@Test
	public void testGetQuantile() {
		
		Histogram histogram = new Histogram(10, 0, 100);
		for(int i = 0; i < 10; i++) {
			histogram.addSample(10.0 * (i+1));
		}//End for i

		for(int i = 0; i < 10; i++) {
			double quantileValue = histogram.getQuantile(i/10.0);
			assertEquals(10.0*i, quantileValue, .001);
		}//End for i;
		
		double quantileValue = histogram.getQuantile(.99);
		assertEquals(99, quantileValue, .001);
		
	}//End testGetQuantile()

	
	@Test
	public void testGetCdfValue() {
		Histogram histogram = new Histogram(10, 0, 100);
		
		for(int i = 0; i < 10; i++) {
			histogram.addSample(10.0 * (i+1));
		}//End for i
		
		for(int i = 0; i < 10; i++) {
			double cdfValue = histogram.getCdfValue(10.0 * i);
			assertEquals(i/10.0, cdfValue, .001);
		}//End for i
		
	}//End testGetCdfValue()
	
	@Test
	public void testCombine(){
		
		Histogram histogram1 = new Histogram(10, 0, 100);
		histogram1.addSample(1.0);
		histogram1.addSample(1.0);
		histogram1.addSample(91.0);
		histogram1.addSample(91.0);
		
		Histogram histogram2 = new Histogram(10, 0, 100);
		histogram2.addSample(15.0);
		histogram2.addSample(15.0);
		histogram2.addSample(91.0);
		histogram2.addSample(91.0);
		
		Histogram histogram3 = histogram1.combineHistogram(histogram2);
		double[] yValues = histogram3.getYValues();
		
		assertEquals(2, yValues[0], .001);
		assertEquals(2, yValues[1], .001);
		assertEquals(4, yValues[9], .001);
		
	}//End testInsert()

}//End class ServerTest
