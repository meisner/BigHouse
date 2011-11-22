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

public class Job implements Constants{
	
	/**
	 * When the job arrived in the system
	 * This variable should be set only once
	 */
	private double arrival_time;
	/** 
	 * When the job actually started (i.e. after queuing delays)
	 * This variable should be set only once
	 */
	private double start_time;
	/** 
	 * When the job completes
	 * This variable should be set only once
	 */
	private double finish_time;

	/** Every job has a unique monotonically increasing id */
	private long job_id;
	
	/** The "length" of the job. Divide by server rate to get the tim to completion */
	private double job_size;
	
	/** If the job is suspended, this will give the amount that has been completed */
	private double amount_completed;
	private double amount_delayed;
	static long current_id;
	private boolean at_limit;
	private JobFinishEvent job_finish_event;
	private double last_resume_time;
	
	public Job(double jobSize){

		this.amount_completed = 0.0;
		this.amount_delayed = 0.0;
		this.job_size = jobSize;
				
		this.job_id = assignId();
		this.at_limit = false;
		this.job_finish_event = null;
		this.last_resume_time = 0.0;
		
	}
	
	public void setAtLimit(boolean atLimit){
		this.at_limit = atLimit;
	}
	
	public boolean getAtLimit(){
		return this.at_limit;
	}
	
	public double getAmountDelayed(){
		return this.amount_delayed;
	}
	
	public void setAmountDelayed(double amount){
		this.amount_delayed = amount;
	}
	
	public void setAmountCompleted(double completed){
		this.amount_completed = completed;
	}
	
	public double getAmountCompleted(){
		return this.amount_completed;
	}
	
	private long assignId(){
		long toReturn = Job.current_id;
		Job.current_id++;
		
		return toReturn;	
	}
	
	 public long getJobId(){
		 return this.job_id;
	 }
	
	public void markArrival(double time){
		if (this.arrival_time > 0) {
			Sim.fatalError("Job arrival marked twice!");
		}
		this.arrival_time = time;
	}
	
	public void markStart(double time){
		if (this.start_time > 0) {
			Sim.fatalError("Job start marked twice!");
		}
		this.start_time = time;
	}
	
	public void markFinish(double time){
		if (this.finish_time > 0) {
			Sim.fatalError("Job " + this.getJobId() + " finsih marked twice!");
		}
		this.finish_time = time;
	}
	
	public double getArrivalTime(){
		return this.arrival_time;
	}
	
	public double getStartTime(){
		return this.start_time;
	}
	
	public double getFinishTime(){
		return this.finish_time;
	}

	public double getSize() {
		return this.job_size;
	}
	
	@Override
	public boolean equals(Object obj ){
		
		boolean objectEqual = super.equals(obj);
		if(!objectEqual){
			return false;
		}
		
		//TODO this may be exessive
		boolean idEqual = ((Job)obj).getJobId() == this.job_id;
		
		if(!idEqual){
			return false;
		}
		
		return true;
		
	}

	public void setJobFinishEvent(JobFinishEvent jobFinishEvent) {
		Sim.debug(666, "job "+this.getJobId() + " finish even set");
		this.job_finish_event = jobFinishEvent;
	}
	
	public JobFinishEvent getJobFinishEvent(){
		return this.job_finish_event;
	}
	
	public void setLastResumeTime(double time) {
		this.last_resume_time = time;
	}
	
	public double getLastResumeTime(){
		return this.last_resume_time;
	}

}//End class Job
