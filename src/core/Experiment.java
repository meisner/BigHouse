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
 *         Junjie Wu (wujj@umich.edu)
 *
 */

package core;

import generator.Generator;
import generator.MTRandom;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import core.Constants.StatName;

import stat.Statistic;
import stat.StatsCollection;
import datacenter.DataCenter;
import datacenter.Server;

/** This class contains all components of an experiment */
public class Experiment implements Serializable,Constants,Cloneable {

	private static final long serialVersionUID = 1L;
	private EventQueue event_queue;
	private long nRan;
	private double current_time;
	
	private DataCenter data_center;
	private ExperimentInput experiment_input;
	private ExperimentOutput expriment_output;
	private String experiment_name;
	private int event_limit;
	private MTRandom random;
	private boolean stop_at_steady_state;
	private boolean stop;
	
	public Experiment(String experimentName, MTRandom random, ExperimentInput experimentInput, ExperimentOutput experimentOutput){
		this.stop = false;
		this.random = random;
		
		this.current_time = 0.0d;
		this.event_limit = 0;
		
		this.experiment_name = experimentName;
		this.experiment_input = experimentInput;
		this.expriment_output = experimentOutput;
		
		this.event_queue = new EventQueue();
		this.stop_at_steady_state = false;
	}
	
	public void setSeed(long newSeed) {
		this.random.setSeed(newSeed);
	}
	
	public void initialize(){
		this.data_center = this.experiment_input.getDataCenter();
		
		Vector<Server> servers = data_center.getServers();
		//Make sure all the arrival processes have begun
		Iterator<Server> iterator = servers.iterator();
		while(iterator.hasNext()){
			Server server = iterator.next();
			server.createNewArrival(0.0);
		}
	}
	
	public String getName(){
		return this.experiment_name;
	}
	
	public ExperimentInput getInput(){
		return this.experiment_input;
	}
	
	public ExperimentOutput getOutput(){
		return this.expriment_output;
	}
	
	public StatsCollection getStats(){
		return this.expriment_output.getStats();
	}
	
	public void setEventLimit(int eventLimit){
		this.event_limit = eventLimit;
	}
	
	public void run(){
		this.initialize();
		long startTime = System.currentTimeMillis();
		
		this.nRan = 0;
		System.out.println("Starting simulation");
		int orderOfMag = 5;
		long printSamples = (long)Math.pow(10,orderOfMag);
		while(true && !stop){		
			Event currentEvent = this.event_queue.nextEvent();
//			Sim.debug(6,"Queue size " + this.event_queue.size());
			this.current_time = currentEvent.getTime();
//			Sim.debug(DEBUG_VERBOSE, "  processing " + currentEvent.toString());
			currentEvent.process();
			double time = currentEvent.getTime();
//			this.data_center.updateStatistics(time);
			this.nRan++;
			if(this.nRan > printSamples){
				System.out.println("Processed "+this.nRan+" events");
				Iterator<Statistic> statIter = this.expriment_output.getStats().getAllStats();
				while(statIter.hasNext()){
					Statistic currentStat = statIter.next();
					if(!currentStat.isConverged()){
						System.out.println("Still waiting for " + currentStat.getStatName() + " at mean converge of " +currentStat.getMeanAccuracy()+ " and quantile converge of " +currentStat.getQuantileAccuracy());
						currentStat.printStatInfo();
					}
				}
				orderOfMag++;
				printSamples = (long)Math.pow(10,orderOfMag);
			}

			if(this.getStats().allStatsConverged()){
				System.out.println("Ending from convergence");
				break;
			}
			
			if(this.getStats().allStatsSteadyState() && this.stop_at_steady_state){
				System.out.println("Halting at steady state");
				break;
			}
			
			if(event_limit > 0 && nRan > event_limit){
				break;				
			}
		}
		
		long endTime = System.currentTimeMillis();
		double execTime = (endTime - startTime)/1000.0;	

	}
	
	public long getnEventsSimulated(){
		return nRan;
	}

	public void addEvent(Event event) {
		this.event_queue.addEvent(event);			
	}
	
	public void cancelEvent(Event event) {
		this.event_queue.cancelEvent(event);
	}

	public double getEndTime() {
		return this.current_time;
	}

	public void runToSteadyState() {
		Iterator<Statistic> stats = this.getStats().getAllStats();
		while(stats.hasNext()) {
			Statistic stat = stats.next();
			stat.setJustBins(true);
		}
		this.stop_at_steady_state = true;
		this.run();
	}

	public synchronized void stop() {
		this.stop = true;
	}
	
	

}
