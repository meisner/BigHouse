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

import java.util.Iterator;
import java.util.Vector;

import generator.Generator;
import core.Experiment;
import core.Job;
import core.Sim;

public class PowerNapServer extends Server {

	protected double nap_transition_time;
	private double nap_power;
	
//	protected Vector<Job> nap_job_queue;
	
	public enum PowerNapState{ACTIVE, TRANSITIONING_TO_ACTIVE, TRANSITIONING_TO_NAP, NAP};
	protected PowerNapState power_nap_state;
	private PowerNapTransitionedToNapEvent transition_event;
	private boolean transitioning_to_active;
	private boolean transitioning_to_nap;
	
	public PowerNapServer(int sockets, int coresPerSocket, Experiment experiment, Generator arrivalGenerator, Generator serviceGenerator, double napTransitionTime, double napPower) {
		super(sockets, coresPerSocket, experiment, arrivalGenerator, serviceGenerator);
		
		this.nap_transition_time = napTransitionTime;
		this.nap_power = napPower;
		this.power_nap_state = PowerNapState.NAP;
		this.transitioning_to_active = false;
		this.transitioning_to_nap = false;
		this.pauseProcessing(0);
	}//End PowerNapServer()
	
	
	public boolean isNapping() {
		if(this.power_nap_state == PowerNapState.NAP || this.power_nap_state == PowerNapState.TRANSITIONING_TO_NAP || this.transitioning_to_nap) {
			return true;
		} else {
			return false;
		}
	}
	
	public void superDuberInsertJob(double time, Job job) {
		Sim.debug(666, "PowerNapServer.superDuperInsertJob()");
		super.insertJob(time, job);
	}

	
	@Override
	public void insertJob(double time, Job job) {
		
		Sim.debug(666, "PowerNapServer.insertJob()");
		Sim.debug(666, "Jobs in service " + this.getJobsInService() + " jobs in queue" +this.getQueueLength() + " server paused: " + this.isPaused()+ " server napping: " + this.isPaused());

		if( this.power_nap_state == PowerNapState.ACTIVE ) {
			
			Sim.debug(666, "Server is active so we're going to just insert");
			super.insertJob(time, job);
			
		}else if( this.power_nap_state == PowerNapState.TRANSITIONING_TO_NAP ) {
			
			Sim.debug(666, "Server is transitioning to nap so we need to transition to active");
			this.transistionToActive(time);
			this.queue.add(job);
			
			//Job has entered the system
			this.jobs_in_server_invariant++;		

		}else if( this.power_nap_state == PowerNapState.TRANSITIONING_TO_ACTIVE ) {
			
			this.queue.add(job);
			
			//Job has entered the system
			this.jobs_in_server_invariant++;

		}else if( this.power_nap_state == PowerNapState.NAP ) {
			Sim.debug(666, "Server was napping so we need to transitioning to active");
			this.transistionToActive(time);
			this.queue.add(job);
			
			//Job has entered the system
			this.jobs_in_server_invariant++;

		} else {
			Sim.fatalError("Uknown power state");
		}//End if
		

	}//End insertJob()
	
	@Override
	public int getJobsInSystem() {
		
		int jobsInSystem = this.getQueueLength() + this.getJobsInService();
		
		return jobsInSystem;
		
	}//End getJobsInSystem()
	
	
	public double getNapTransitionTime() {
		return this.nap_transition_time;
	}//End getTransitionTime()
	
	public void transistionToActive(double time) {
		
		Sim.debug(666, "PowerNapServer.transistionToActive()");

		if(!this.isNapping()) { 
			Sim.fatalError("Trying to transition to active when not napping");
		}
		
		if(!this.isPaused()) { 
			Sim.fatalError("Trying to transition to active when not paused");
		}
		
		Sim.debug(666, "...time: " +time + " Server is transitioning to active");
		
		double extraDelay = 0;
		if(this.transition_event != null){
			double timeServerWouldHaveReachedNap=this.transition_event.getTime();
			extraDelay += timeServerWouldHaveReachedNap - time;
			this.experiment.cancelEvent( this.transition_event);
			this.transitioning_to_nap = false;
		}
		this.transitioning_to_active = true;

		this.power_nap_state = PowerNapState.TRANSITIONING_TO_ACTIVE;
		double napTime = time + extraDelay +this.nap_transition_time ;
		PowerNapTransitionedToActiveEvent napEvent = new PowerNapTransitionedToActiveEvent(napTime, this.experiment, this);
		this.experiment.addEvent(napEvent);
		Sim.debug(666, "...|...time " + time + " Inserted PowerNapTransitionedToActiveEvent");
		
	}
	
	public void transistionToNap(double time) {
		Sim.debug(666, "PowerNapServer.transistionToNap()");
		if(this.isNapping()) { 
			Sim.fatalError("Trying to transition to nap when napping");
		}
		
		if(this.isPaused()) { 
			Sim.fatalError("Trying to transition to nap when paused");
		}
		
		Sim.debug(666, "...|...time: " +time + " Server is transitioning to nap");

		this.power_nap_state = PowerNapState.TRANSITIONING_TO_NAP;
		this.transitioning_to_nap = true;
		double napTime = time + this.nap_transition_time;
		PowerNapTransitionedToNapEvent napEvent = new PowerNapTransitionedToNapEvent(napTime, this.experiment, this);
		this.transition_event = napEvent;
		this.experiment.addEvent(napEvent);
		this.pauseProcessing(time);
		Sim.debug(666, "...|...time " + time + " Inserted transistionToNap");

	}
	
	@Override
	public void removeJob(double time, Job job) {
		Sim.debug(666, "PowerNapServer.removeJob()");
		super.removeJob(time, job);
		
		if(this.getJobsInService() == 0) {
			this.transistionToNap(time);			
		}//End if
		

	}//End removeJob()

	public void setToActive(double time) {
		
		Sim.debug(666, "PowerNap.setToActive()");
		Sim.debug(666, "...|...time: " +time + " Server became active");

		this.transitioning_to_active = false;	
		
		//Server is now fully in the  active mode
		this.power_nap_state = PowerNapState.ACTIVE;
		
		//Start all the jobs possible and queue the ones that aren't
		this.resumeProcessing(time);
		
	}//End setToActive()
	
	public boolean isTransitioningToActive() {
		return transitioning_to_active;
	}
	
	public boolean isTransitioningToNap() {
		return transitioning_to_nap;
	}
	
	public void setToNap(double time) {
		Sim.debug(666, "PowerNap.setToNap()");
		Sim.debug(666, "...|...time: " +time + " Server became nap");
		//Server is now fully in the nap mode
		this.transitioning_to_nap = false;
		this.power_nap_state = PowerNapState.NAP;
		this.transition_event=null;

	}//End setTopNap()
	
	@Override
	public double getPower() {
		
		double power = 0.0d;

		if(this.power_nap_state == PowerNapState.ACTIVE) {

			power = super.getPower();
			Sim.debug(666,"state: active: "+power);			

		}else if(this.power_nap_state == PowerNapState.TRANSITIONING_TO_ACTIVE) {
			power = super.getPower();
			Sim.debug(666,"state: transitioning to active power: "+power);


		} else if(this.power_nap_state == PowerNapState.TRANSITIONING_TO_NAP) {
			power = super.getPower();
			Sim.debug(666,"state: transitioning to nap power: "+power);

		} else if (this.power_nap_state==PowerNapState.NAP) {

			power = this.nap_power;
			Sim.debug(666,"state: nap power : "+power);

		}//End if
	
		return power;
		
	}//End getPower()
	

	
}//End class PowerNapServer