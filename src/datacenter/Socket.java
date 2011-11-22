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
package datacenter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import core.Event;
import core.Experiment;
import core.Job;
import core.Sim;
import core.SocketEnteredParkEvent;
import core.SocketExitedParkEvent;
import datacenter.Core.CorePowerPolicy;

/**
 * This class represents a single socket (physical processor chip) in a server
 */
public class Socket implements Powerable, Serializable{

	/** The server this Socket belongs to */
	private Server server;
	/** The number of cores in this chip */
	private int nCores;

	private Experiment experiment;

	private HashMap<Job, Core> jobToCoreMap;
	
	/** Available power states */
	private enum SocketPowerState{ACTIVE, TRANSITIONG_TO_LOW_POWER_IDLE, TRANSITIONG_TO_ACTIVE, LOW_POWER_IDLE}; 
	/** Available power management policies */
	public static enum SocketPowerPolicy{NO_MANAGEMENT, SOCKET_PARKING}; 
	
	private SocketPowerPolicy power_policy;
	private SocketPowerState power_state;

	/** Cores which aren't busy */
	private Vector<Core> available_cores;
	/** Cores that are busy with a job */
	private Vector<Core> busy_cores; 
	private Vector<Job> transistion_queue;

	private double socket_park_power = 0.0d;
	private double socket_active_idle_power = 0.0d;
	private double socket_park_transition_time = 500e-6;
	private Event trasition_event;

	/**
	 * Instantiate a socket with nCores cores
	 */
	public Socket(Experiment experiment, Server server, int nCores){

		this.experiment = experiment;
		this.server = server;
		this.nCores = nCores;

		this.jobToCoreMap = new HashMap<Job, Core>(); 

		this.available_cores = new Vector<Core>();
		this.busy_cores = new Vector<Core>();
		this.transistion_queue = new Vector<Job>();
	
		//Create nCores Cores and put them on the free list
		for(int i = 0; i < nCores; i++){
			Core core = new Core(experiment,this,this.server);
			this.available_cores.add(core);
		}//End for i
		
		this.power_policy = SocketPowerPolicy.NO_MANAGEMENT;
		this.power_state = SocketPowerState.ACTIVE;

	}//End Socket()

	/**
	 * Start a job for the first time on the socket.
	 * It will be assigned to a random core.
	 */
	public void insertJob(double time, Job job){

		if(this.power_state == SocketPowerState.ACTIVE) {
			//Pick the first core off the available cores
			Core core = this.available_cores.remove(0);
			core.insertJob(time, job);
			this.busy_cores.add(core);

			//Memoize where the job was put
			this.jobToCoreMap.put(job, core);
		} else if(this.power_state == SocketPowerState.TRANSITIONG_TO_LOW_POWER_IDLE) {
						
			this.transistion_queue.add(job);
			this.power_state = SocketPowerState.TRANSITIONG_TO_ACTIVE;
			
			if(this.trasition_event != null){
				this.experiment.cancelEvent(this.trasition_event);
			}

			double exitParkTime = time + this.socket_park_transition_time;
			SocketExitedParkEvent socketExitedParkEvent = new SocketExitedParkEvent(exitParkTime, this.experiment, this);
			this.experiment.addEvent(socketExitedParkEvent);
			
		} else if(this.power_state == SocketPowerState.TRANSITIONG_TO_ACTIVE) {
			
			this.transistion_queue.add(job);
			
		} else if(this.power_state == SocketPowerState.LOW_POWER_IDLE) {
			
			this.transistion_queue.add(job);
			this.power_state = SocketPowerState.TRANSITIONG_TO_ACTIVE;
			double exitParkTime = time + this.socket_park_transition_time;
			SocketExitedParkEvent socketExitedParkEvent = new SocketExitedParkEvent(exitParkTime, this.experiment, this);
			this.experiment.addEvent(socketExitedParkEvent);
			
		}

	}//End insertJob()

	/**
	 * Removes a job from the socket from completion
	 */
	public void removeJob(double time, Job job, boolean jobWaiting){

		//Find out which socket this job was running on
		Core core = this.jobToCoreMap.remove(job);
		//		System.out.println("Map size is " + this.jobToCoreMap.size());
		core.removeJob(time, job, jobWaiting);

		//Error check we got a real socket
		if(core == null){
			Sim.fatalError("Couldn't resolve which core this job belonged to");
		}

		//Mark that the job is no longer busy
		boolean found = this.busy_cores.remove(core);

		//Error check the socket was considered busy
		if(!found){
			Sim.fatalError("Could take core off the busy list");
		}

		//Core is now available
		this.available_cores.add(core);
		
		if(this.busy_cores.size() == 0 && jobWaiting == false) {
			if(this.power_policy == SocketPowerPolicy.SOCKET_PARKING) {
				this.power_state = SocketPowerState.TRANSITIONG_TO_LOW_POWER_IDLE;
				double enterParkTime = time + this.socket_park_transition_time;
				SocketEnteredParkEvent socketEnteredParkEvent = new SocketEnteredParkEvent(enterParkTime, this.experiment, this);
				this.experiment.addEvent(socketEnteredParkEvent);
				this.trasition_event = socketEnteredParkEvent;
			}
			//Otherwise the socket stays active	
			
		}

	}//End removeJob()

	/**
	 * Gets the number of cores that have slots for jobs
	 */
	public int getRemainingCapacity(){
		return this.available_cores.size() - this.transistion_queue.size();
	}

	/**
	 * Gets the number of jobs this socket can ever support.
	 */
	public int getTotalCapacity(){
		return this.nCores;
	}

	/**
	 * Gets the instant utilization of the socket (busy cores/ total cores).
	 */
	public double getInstantUtilization(){
		return ((double)this.busy_cores.size() + this.transistion_queue.size())/this.nCores;
	}//End getInstantUtilization()

	/**
	 * Gets an Vector of cores on this socket
	 */
	public Vector<Core> getCores(){

		Vector<Core> combined = new Vector<Core>(); 
		combined.addAll(this.available_cores);
		combined.addAll(this.busy_cores);

		return combined;

	}//End getCores()

	public Server getServer() {
		return this.server;
	}//End getServer()

	public int getJobsInService() {
		return this.busy_cores.size();
	}//End getJobsInService()


	public void setCorePolicy(CorePowerPolicy corePowerPolicy) {
		Vector<Core> cores = this.getCores();
		Iterator<Core> iter = cores.iterator();
		while(iter.hasNext()) {
			Core core = iter.next();
			core.setPowerPolicy(corePowerPolicy);
		}//End while

	}//End setCorePolicy()

	public void setPowerPolicy(SocketPowerPolicy policy) {
		this.power_policy = policy;
	}
	
	
	public void enterPark(double time) {

		if(this.busy_cores.size() != 0) {
			Sim.fatalError("Socket tried to enter park when it shouldn't have");
		}

		this.power_state = SocketPowerState.LOW_POWER_IDLE;
	}


	public void exitPark(double time) {

		this.power_state = SocketPowerState.ACTIVE;
		Iterator<Job> iter = this.transistion_queue.iterator();
		while(iter.hasNext()) {
			Job job = iter.next();
			this.insertJob(time, job);
		}
		this.transistion_queue.clear();
	}

	public int getJobsInTransistion() {
		return this.transistion_queue.size();
		
	}

	public void pauseProcessing(double time) {
		
		Sim.debug(666, "...|...|...|...PAUSING socket  at " + time);

		Vector<Core> cores = this.getCores();
		Iterator<Core> iter = cores.iterator();
		while(iter.hasNext()) {
			Core core = iter.next();
			core.pauseProcessing(time);
		}
	}

	public void resumeProcessing(double time) {
		Sim.debug(666, "Socket.resumeProcessing");

		Sim.debug(666, "...|...|...|...RESUMING socket  at " + time);

		Vector<Core> cores = this.getCores();
		Iterator<Core> iter = cores.iterator();
		while(iter.hasNext()) {
			Core core = iter.next();
			core.resumeProcessing(time);
		}
		
	}

	public void setSocketActivePower(double socketActivePower) {
		this.socket_active_idle_power = socketActivePower;

	}

	public void setSocketParkPower(double socketParkPower) {
		this.socket_park_power = socketParkPower;		
	}

	public void setCoreHaltPower(double coreHaltPower) {
		Iterator<Core> iter = this.getCores().iterator();
		while(iter.hasNext()) {
			Core core = iter.next();
			core.setHaltPower(coreHaltPower);
		}
	}

	public void setCoreParkPower(double coreParkPower) {

		Iterator<Core> iter = this.getCores().iterator();
		while(iter.hasNext()) {
			Core core = iter.next();
			core.setParkPower(coreParkPower);
		}
		
	}

	public void setCoreActivePower(double coreActivePower) {
		Iterator<Core> iter = this.getCores().iterator();
		while(iter.hasNext()) {
			Core core = iter.next();
			core.setActivePower(coreActivePower);
		}
	}
	
	public void setDvfsSpeed(double time, double speed) {
		Iterator<Core> iter = this.getCores().iterator();
		while(iter.hasNext()) {
			iter.next().setDvfsSpeed(time, speed);
		}//End while
	}//End setDvfsSpeed()
	
	public double getPower() {

		return this.getDynamicPower() + this.getIdlePower();

	}//End getPower()


	public double getIdlePower() {

		double idlePower = 0.0d;
		
		if(this.power_state == SocketPowerState.ACTIVE) { 
			
			Iterator<Core> coreIter = this.getCores().iterator();
			while(coreIter.hasNext()) {
				Core core = coreIter.next();
				double corePower = core.getIdlePower();
				idlePower += corePower;
			}//End while

			idlePower += this.socket_active_idle_power;
			
		} else if (this.power_state == SocketPowerState.TRANSITIONG_TO_ACTIVE) {
			
			idlePower =  this.socket_active_idle_power;			
			
		} else if (this.power_state == SocketPowerState.TRANSITIONG_TO_LOW_POWER_IDLE) {
			
			idlePower =  this.socket_active_idle_power;			
			
		} else if (this.power_state == SocketPowerState.LOW_POWER_IDLE) {
			
			idlePower =  this.socket_park_power;
			
		}//End if/else
		
		return idlePower;
		
	}//End getIdlePower()

	public double getDynamicPower() {

		double dynamicPower = 0.0d;

		Iterator<Core> coreIter = this.getCores().iterator();
		while(coreIter.hasNext()) {
			Core core = coreIter.next();
			double corePower = core.getDynamicPower();
			dynamicPower += corePower;
		}//End while

		return dynamicPower;

	}//End getDynamicPower()

}///End class Socket
