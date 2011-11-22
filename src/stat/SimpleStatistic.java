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

public class SimpleStatistic implements Serializable{

	private long s0;

	private double s1;
	private double s2; 
	private double min_value;
	private double max_value;
	
	
	public SimpleStatistic() {
		this(0,0,0,Double.MAX_VALUE,Double.MIN_VALUE);
	}//End SimpleStatistic()
	
	public SimpleStatistic(long s0, double s1, double s2, double minValue, double maxValue) {
		
		this.s0 = s0;
		this.s1 = s1;
		this.s2 = s2;
		this.min_value = minValue;
		this.max_value = maxValue;
		
	}//End SimpleStatistic()
	
	public SimpleStatistic combineSimpleStatistics(SimpleStatistic stat) {
		
		long combinedS0 = this.s0 + stat.s0;
		double combinedS1 = this.s1 + stat.s1;
		double combinedS2 = this.s2 + stat.s2;
		double combinedMinValue = Math.min(this.min_value, stat.min_value);
		double combinedMaxValue = Math.max(this.max_value, stat.max_value);
		
		SimpleStatistic combinedStat = new SimpleStatistic(combinedS0, combinedS1, combinedS2, combinedMinValue, combinedMaxValue);
		
		return combinedStat;
		
	}//End combineSimpleStatistics()
	
	public void addSample(double value) {
		this.s0 += 1;
		this.s1 += value;
		this.s2 += value*value;
		this.min_value = Math.min(this.min_value, value);
		this.max_value = Math.max(this.max_value, value);
	}//End insert()
	
	public long getS0() {
		return s0;
	}//End getS0()

	public double getS1() {
		return s1;
	}//End getS1()

	public double getS2() {
		return s2;
	}//End getS2()

	public double getMinValue() {
		return min_value;
	}//End getMinValue()

	public double getMaxValue() {
		return max_value;
	}//End getMaxValue()

	public double getAverage() {
		return s1/s0;		
	}//End getAverage()

	//http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
	public double getStdDev() {
		return Math.sqrt((s0*s2 - s1*s1)/(s0*(s0-1)));		
	}//End getStdDev()

	public double getTotal() {
		return this.s1;		
	}
	
}//End class SimpleStatistic
