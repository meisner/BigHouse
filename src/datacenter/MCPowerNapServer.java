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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import generator.Generator;
import core.Experiment;
import core.Job;
import core.MCPowerNapJobTimeoutEvent;
import core.Sim;

public class MCPowerNapServer extends PowerNapServer {


	private double max_delay;
	private HashMap<Job,MCPowerNapJobTimeoutEvent> timeoutevent_map;
	private Vector<Job> timed_out_jobs;

	public MCPowerNapServer(int sockets, int coresPerSocket, Experiment experiment, Generator arrivalGenerator, 
			Generator serviceGenerator, double napTransitionTime,	double napPower, double maxDelay) {
		super(sockets, coresPerSocket, experiment, arrivalGenerator, serviceGenerator, napTransitionTime, napPower);


		this.max_delay = maxDelay;
		this.timeoutevent_map = new HashMap<Job, MCPowerNapJobTimeoutEvent>();
		this.setPaused(true);
		this.timed_out_jobs = new Vector<Job>();

	}//MCPowerNapServer()





	public void handleJobTimeout(double time, Job timeoutJob, MCPowerNapJobTimeoutEvent timeoutEvent) {
		Sim.debug(666, "MCPowerNapServer.handleJobTimeout()");

		Sim.debug(666, "timeout " + timeoutJob.getJobId() + " at " + time +" put in timedout list");
		
		this.timed_out_jobs.add(timeoutJob);
		this.timeoutevent_map.remove(timeoutJob);

		if(!this.isTransitioningToActive()){
			this.transistionToActive(time);
		}

	}

	@Override
	public void insertJob(double time, Job job) {
		Sim.debug(666, "MCPowerNapServer.insertJob()");

		//Set timeout for job
		Sim.debug(666, "...job " + job.getJobId() + " inserted  at " + time);

		if(!this.isNapping() && !this.isPaused()){
			Sim.debug(666, "...The system is active so i'm just going to insert the job");

			super.insertJob(time, job);		

		} else if(this.getJobsInService() + this.queue.size() + 1 >= this.getTotalCapacity()) {
			Sim.debug(666, "...With this job and " + this.getJobsInService() + " in service and "+this.queue.size() + " in the queue I am starting service");

			if(!this.isTransitioningToActive()){
				this.transistionToActive(time);											
			}
			super.insertJob(time, job);

		} else if( this.isTransitioningToActive()){
			super.insertJob(time, job);
		}else {


			Sim.debug(666, "...Check if there is a free core ");

			if(this.getRemainingCapacity() > 0) {
				Sim.debug(666, "...there is a free core, let's find it ");

				super.superDuberInsertJob(time,job);
				this.experiment.cancelEvent(job.getJobFinishEvent());
				double timeout = time + this.max_delay;
				MCPowerNapJobTimeoutEvent timeoutEvent = new MCPowerNapJobTimeoutEvent(timeout, this.experiment, job, this);
				this.experiment.addEvent(timeoutEvent);
				this.timeoutevent_map.put(job, timeoutEvent);

			} else {		
				this.queue.add(job);
				Sim.debug(666, "...didn't find a free core so I'm assigning it to a queue with this many jobs " +this.queue.size());
			}

		}


		this.jobs_in_server_invariant++;
		Sim.debug(666, "...After insertion there are now this many jobs " + this.getJobsInSystem());

		checkInvariants();


	}//End insertJob()

	@Override
	public void removeJob(double time, Job job) {
		Sim.debug(666, "MCPowerNapServer.removeJob()");

		if(this.isPaused()) {
			Sim.fatalError("time " + time + " job " + job.getJobId() + " Shouldn't be removing jobs when the server is paused");			
		}

		if(this.isNapping()) {
			Sim.fatalError("time " + time + " job " + job.getJobId() + " Shouldn't be removing jobs when the server is napping");			
		}

		Sim.debug(666, "...job " + job.getJobId() + " removed  at " + time + " is server paused: " +this.isPaused());

		this.timed_out_jobs.remove(job);
		super.removeJob(time, job);

		if(this.getJobsInService() + this.queue.size() < this.getTotalCapacity() && this.timed_out_jobs.size() == 0 && !this.isTransitioningToNap()) {
			//			this.pauseProcessing(time);
			Sim.debug(666, "...Triggering nap since i don't have enough jobs to fill all the slots, service: " +this.getJobsInService() + " in queue " +this.queue.size() + " and there are no time out jobs "+this.timed_out_jobs.size());

			this.transistionToNap(time);

		}

	}//End removeJob()

	public void cancelTimeoutEvent(double time) {
		Sim.debug(666, "MCPowerNapServer.cancelTimeoutEvent()");

		for(int i = 0; i < sockets.length; i++){
			Socket socket = sockets[i];
			Vector<Core> cores = socket.getCores();
			Iterator<Core> iter = cores.iterator();
			while(iter.hasNext()) {
				Core core = iter.next();
				Job coreJob = core.getJob();

				if(coreJob != null) {

					MCPowerNapJobTimeoutEvent timeoutEvent =  this.timeoutevent_map.get(coreJob);
					if(timeoutEvent == null){
						Sim.debug(666, "The event was null for job "+coreJob.getJobId());
						if(!this.timed_out_jobs.contains(coreJob)){
							Sim.fatalError("The timeout event was null and wasn't in the timedout event list");
						}
					} else {
						Sim.debug(666, "The event was not null for job "+coreJob.getJobId());
						this.timeoutevent_map.remove(coreJob);
						this.experiment.cancelEvent(timeoutEvent);
						Sim.debug(666, "Canelled timeout for job  "+coreJob.getJobId());

					}

				}
			}
		}
	}//End cancelTimeoutEvent()

	@Override
	public void setToActive(double time) {
		this.cancelTimeoutEvent(time);
		super.setToActive(time);
	}

	@Override
	public void transistionToActive(double time) {
		super.transistionToActive(time);
	}

	@Override
	public void transistionToNap(double time) {

		super.transistionToNap(time);

		Sim.debug(666, "Assigning timeouts to jobs");
		//Make sure to schedule timeouts for jobs
		Socket[] sockets = this.getSockets();
		for(int i = 0; i < sockets.length; i++){
			Socket socket = sockets[i];
			Vector<Core> cores = socket.getCores();
			Iterator<Core> iter = cores.iterator();
			while(iter.hasNext()) {
				Core core = iter.next();
				Job coreJob = core.getJob();

				if(coreJob != null) {

					double amountDelayed = time - coreJob.getStartTime() - coreJob.getAmountCompleted();
					if(amountDelayed < 0) {
						Sim.fatalError("the amount delayed can't be negative");
					}

					Sim.debug(666, "...job " + coreJob.getJobId()+" has been delayed " +amountDelayed);
					double timeoutTime = time + (this.max_delay-amountDelayed) ;

					if(this.max_delay-amountDelayed < 0) {
						System.out.println("at time " + time +" max_delay "+max_delay + " amount Delayed " +amountDelayed + " time delta" + (this.max_delay-amountDelayed) );
						Sim.fatalError("I should never have a negative delta for my timeout ");
					}

					Sim.debug(666, "...job " + coreJob.getJobId()+" will timeoout at " + timeoutTime);

					MCPowerNapJobTimeoutEvent timeoutEvent = new MCPowerNapJobTimeoutEvent(timeoutTime, this.experiment, coreJob, this);
					this.timeoutevent_map.put(coreJob, timeoutEvent);
					this.experiment.addEvent(timeoutEvent);
					Sim.debug(666, "...Giving the job "+coreJob.getJobId()+ " at timeout at "+timeoutTime);

				}
			}
		}	
	}

}//End class MCPowerNapServer
