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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import core.Experiment;
import core.Sim;
import core.Constants.StatName;
import core.Constants.TimeWeightedStatName;


public class StatsCollection implements Serializable{
	
	private HashMap<StatName, Statistic> stats_map;
	private HashMap<TimeWeightedStatName, TimeWeightedStatistic> tw_stats_map;

	private Vector<Statistic> converge_stats;
	private Vector<Statistic> warm_stats;
		
	private FakeStatistic fake_statistic;
	private FakeTimeWeightedStatistic tw_fake_statistic;
	
	public StatsCollection(){	
		
		this.converge_stats = new Vector<Statistic>();
		this.warm_stats = new Vector<Statistic>();
		this.stats_map = new HashMap<StatName, Statistic>();
		this.tw_stats_map = new HashMap<TimeWeightedStatName, TimeWeightedStatistic>();
		
		this.fake_statistic = new FakeStatistic();
		this.tw_fake_statistic = new FakeTimeWeightedStatistic();
		
	}//End StatsCollection	

	public StatsCollection(HashMap<StatName, Statistic> statsMap, Vector<Statistic> convergeStats) {
		this();
		this.stats_map = statsMap;
		this.converge_stats = convergeStats;
	}

	public Statistic getStat(StatName name) {

		Statistic stat = this.stats_map.get(name);

		if(stat == null){
			stat = this.fake_statistic;
		}//End getStat

		return stat;
		
	}//End getStat()

	public TimeWeightedStatistic getTimeWeightedStat(TimeWeightedStatName name) {
		
		TimeWeightedStatistic stat = this.tw_stats_map.get(name);

		if(stat == null){
			stat = this.tw_fake_statistic;
		}//End if

		return stat;		
	}//End getTimeWeightedStat()
	

	public boolean allStatsConverged() {

		boolean allConverged = true;
		Iterator<Statistic> iter = this.converge_stats.iterator();
		
		while(iter.hasNext()){
			
			Statistic stat = iter.next();
			
			if(stat.isConverged() == false){
				allConverged = false;
			}//End if
			
		}//End while
		
		return allConverged;
		
	}//End allStatsConverged()

	public void reportWarmed(Statistic varStat) {

		System.out.println(varStat.getStatName() + " reported it is warm");
		this.warm_stats.remove(varStat);
		
		if(warm_stats.isEmpty()){
			Iterator<Statistic> iter = this.converge_stats.iterator();
			while(iter.hasNext()){
				Statistic stat = iter.next();
				stat.setOtherStatsWarmed(true);
			}
		}
	}


	public void setWarmupStat(StatName stat) {
		this.warm_stats.add(this.getStat(stat));		
	}


	public void printConvergedOutputs() {
		Iterator<Statistic> iter = this.converge_stats.iterator();
		while(iter.hasNext()){
			Statistic stat = iter.next();
			System.out.println(stat.getStatName() + " Average " + stat.getAverage());
			System.out.println(stat.getStatName() + " Quantile(" + stat.getQuantileSetting() + "): " + stat.getQuantile(stat.getQuantileSetting()));
		}		
	}

	public Iterator<Statistic> getAllStats() {
		return this.converge_stats.iterator();
	}
	
	public void addStatistic(StatName name, Statistic stat) {
	
		if(this.stats_map.get(name) != null  ) {
			Sim.fatalError("Already added " + name);
		}//End if
		
		this.stats_map.put(name, stat);
		this.converge_stats.add(stat);
		
	}//End addStatistic()

	public void addTimeWeightedStatistic(TimeWeightedStatName name, TimeWeightedStatistic stat) {
		
		if(this.tw_stats_map.get(name) != null  ) {
			Sim.fatalError("Already added " + name);
		}//End if
		
		this.tw_stats_map.put(name, stat);
		this.converge_stats.add(stat);
		
	}//End addTimeWeightedStatistic()


	private class FakeStatistic extends Statistic {

		public FakeStatistic() {
			super(null, null, 0, 0, 0, 0);

		}//End FakeStatistic()
		
		@Override 
		public void addSample(double value) {
			
		}//End addSample()
		
	}//End class FakeStatistic

	private class FakeTimeWeightedStatistic extends TimeWeightedStatistic {

		public FakeTimeWeightedStatistic() {
			super(null, null, 0, 0, 0, 0, 0);
			
		}//End FakeTimeWeightedStatistic()

		@Override 
		public void addSample(double value, double time) {
			
		}//End addSample()
		
	}//End class FakeStatistic

	public void printAllStatInfo() {
		
		Iterator<Statistic> iter = this.converge_stats.iterator();		
		while(iter.hasNext()){
			
			Statistic stat = iter.next();
			stat.printStatInfo();
			
		}//End while		
	}

	public boolean allStatsSteadyState() {
		
		boolean allSteadyState = true;
		
		Iterator<Statistic> iter = this.converge_stats.iterator();
		
		while(iter.hasNext()){
			
			Statistic stat = iter.next();
			
			if(stat.isSteadyState() == false){
				allSteadyState = false;
			}//End if
			
		}//End while
		
		return allSteadyState;
		
	}

	public StatsCollection combine(StatsCollection stats) {

		HashMap<StatName, Statistic> statsMap = new HashMap<StatName, Statistic>();
		Set<StatName> keys = this.stats_map.keySet();
		Iterator<StatName> iter = keys.iterator();
		Vector<Statistic> convergeStats = new Vector<Statistic>();
		while(iter.hasNext()){
			StatName key = iter.next();
			Statistic myStat = this.stats_map.get(key);
			Statistic theirStat = stats.getStat(key);
			Statistic combinedStat = myStat.combineStatistics(theirStat);
			statsMap.put(key, combinedStat);
			convergeStats.add(combinedStat);
		}
		StatsCollection combinedCollection = new StatsCollection(statsMap,convergeStats);
		
		return combinedCollection;
		
	}
	
}//End StatsCollection
