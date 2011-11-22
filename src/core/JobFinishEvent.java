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

package core;

import java.io.BufferedWriter;
import java.io.FileWriter;

import stat.Statistic;
import datacenter.Core;
import datacenter.Server;

public class JobFinishEvent extends JobEvent implements Constants{
	
	private double finishing_speed; //The speed at which the job finish time was calculated
	private double finish_time_set;
	private Server server;
	
	public JobFinishEvent(double time, Experiment experiment, Job job, Server server, double finishTimeSet, double finishSpeed){
		super(time, experiment, job);
		this.server = server;
		job.setJobFinishEvent(this);
		this.finishing_speed = finishSpeed;
		this.finish_time_set = finishTimeSet;
		//System.out.println("Created finish event at " + time);
	}
	
	public double getFinishTimeSet() {
		return this.finish_time_set;
	}

	public void setFinishSpeed(double finishSpeed){
		this.finishing_speed = finishSpeed;
	}
	
	public double getFinishSpeed(){
		return this.finishing_speed;
	}
	
	@Override
	public void process() {
		//System.out.println("Job finished at  " + time);
		this.job.markFinish(this.time);	

		this.server.removeJob(this.time, this.job);

		double sojournTime = this.job.getFinishTime() - this.job.getArrivalTime();
		Statistic sojournStat = this.experiment.getStats().getStat(StatName.SOJOURN_TIME);
		sojournStat.addSample(sojournTime);

//		Sim.debug(DEBUG_VERBOSE, "Job " + this.job.getJobId() + " finish. SoujournTime " + sojournTime);
//		Sim.debug(DEBUG_VERBOSE, "Job " + this.job.getJobId() + " finish: StartTime " + this.job.getStartTime());
//		Sim.debug(DEBUG_VERBOSE, "Job " + this.job.getJobId() + " finish: ArrivalTime " + this.job.getArrivalTime());

		
		double waitTime = this.job.getStartTime() - this.job.getArrivalTime(); 
		Statistic waitStat = this.experiment.getStats().getStat(StatName.WAIT_TIME);
		waitStat.addSample(waitTime);
//		Sim.debug(DEBUG_VERBOSE, "Job finish. waitTime " + waitTime);
		
		if(sojournTime < 0){
			
			System.out.println("Job " +this.job.getJobId()+" Finish time "+this.job.getFinishTime() + " arrival time " + this.job.getArrivalTime());
			Sim.fatalError("JobFinishEvent.java: This should never happen sojournTime = " + sojournTime);
		}
		
		if(waitTime < 0){
			Sim.fatalError("JobFinishEvent.java: This should never happen waitTime = " + waitTime);
		}
		
	}//End process()


}//End class JobFinishEvent
