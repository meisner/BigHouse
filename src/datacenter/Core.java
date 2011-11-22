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

import core.CoreEnteredParkEvent;
import core.CoreExitedParkEvent;
import core.Event;
import core.Experiment;
import core.Job;
import core.JobFinishEvent;
import core.JobHandler;
import core.Sim;


/**
 * This class represents a single core on a processor (socket). It can only run one job at a time. 
 */
public class Core implements Powerable, Serializable{

	private Server server;
	private Job job; //The currently running job, null if idle
	private enum PowerState{ACTIVE, TRANSITIONG_TO_LOW_POWER_IDLE, TRANSITIONG_TO_ACTIVE, HALT, LOW_POWER_IDLE}; //Available power states
	public static enum CorePowerPolicy{NO_MANAGEMENT, CORE_PARKING}; //Available power management policies

	private PowerState power_state; //Current power state
	private CorePowerPolicy power_policy; //Current power management policy

	// The relative (1.0 is no slowdown) speed the core is operating at (determines job completion times)
	private double speed; 

	private Experiment experiment;
	private Socket socket;//The socket this core belongs to

	//Power values
	private  double active_power;
	private double park_power;
	private double halt_power;
	private double transition_time = 100e-6;

	private Event transition_event;
	
	private boolean paused;

	/**
	 * Constructs a new Core
	 */
	public Core(Experiment experiment, Socket socket, Server server){
		this.experiment = experiment;
		this.job = null; //Core starts without a job
		this.socket = socket;
		this.power_state = PowerState.HALT;
		//		this.power_policy = PowerPolicy.CORE_PARKING;
		this.power_policy = CorePowerPolicy.NO_MANAGEMENT;

		this.speed = 1.0; //No slowdown or speedup
		this.server = server;

		active_power = 40.0*(4.0/5.0)/2;
		park_power = 0;
		halt_power = active_power/5.0;
		transition_time = 100e-6;
		this.paused = false;
	}

	public void setPowerPolicy(CorePowerPolicy policy) {
		this.power_policy = policy;
	}//End setPowerPolicy()

	public CorePowerPolicy getPowerPolicy() {
		return this.power_policy;
	}//End getPowerPolicy()

	//HACK HACK HACK
	public void setJob(double time, Job job){
		Sim.debug(666, "Core.setJob()");
		this.job = job;
		this.job.markStart(time);
	}
	
	
	/**
	 * Puts a job on the core for the first time
	 */
	public void insertJob(double time, Job job){
		Sim.debug(666, "Core.insertJob()");

		//Error check that we never try to put two jobs on one core
		if(this.job != null){
			Sim.fatalError("Tried to insert a job into a core that was already busy");
		}

		//Assign job to core
		this.job = job;

		if(this.power_state == PowerState.TRANSITIONG_TO_LOW_POWER_IDLE) {
			//We need to interrupt transitioning to low power idle

			if(this.transition_event.getClass() != CoreEnteredParkEvent.class){
				Sim.fatalError("Tried to cancel the wrong type of event");
			}//End if

			this.experiment.cancelEvent(this.transition_event);

		}//End if


		if(this.power_state == PowerState.LOW_POWER_IDLE || this.power_state == PowerState.TRANSITIONG_TO_LOW_POWER_IDLE) {
			//We need to transition out of low power

			double exitTime = time + this.transition_time;
			CoreExitedParkEvent coreExitedParkEvent = new CoreExitedParkEvent(exitTime, this.experiment, this);
			this.experiment.addEvent(coreExitedParkEvent);

		} else { 


			double alpha = .9;
			double slowdown = (1 - alpha) + alpha/this.speed;
			double finishTime = time + this.job.getSize()/slowdown;	
			Server server = this.socket.getServer();
			
			JobFinishEvent finishEvent = new JobFinishEvent(finishTime, experiment, job, server, time, this.speed);
			job.setLastResumeTime(time);
			this.experiment.addEvent(finishEvent);

			//Core now goes into full power state
			this.power_state = PowerState.ACTIVE;

		}//End if

	}//End insertJob()

	/**
	 * Removes a job from the core because of job completion
	 */
	public void removeJob(double time, Job job, boolean jobWaiting){

		//Error check we're not trying to remove a job from an empty core
		if(this.job == null){
			Sim.fatalError("Tried to remove a job from a core when there wasn't one");
		}

		//Error check we're removing the correct job
		if(!this.job.equals(job)){
			Sim.fatalError("Tried to remove a job, but it didn't match the job on the core");
		}

		//Null signifies the core is idle
		this.job = null;

		//If no job is waiting, we can begin transitioning to a low power state
		if(!jobWaiting){

			if(this.power_policy == CorePowerPolicy.CORE_PARKING) {
				this.power_state = PowerState.TRANSITIONG_TO_LOW_POWER_IDLE;
				double enteredLowPowerTime = time + this.transition_time ;
				CoreEnteredParkEvent coreEnteredParkEvent = new CoreEnteredParkEvent(enteredLowPowerTime, this.experiment, this);
				this.transition_event = coreEnteredParkEvent;
				this.experiment.addEvent(coreEnteredParkEvent);

			} else { 
				this.power_state = PowerState.HALT;
			}

		}

	}//End removeJob()

	/**
	 * Gets the number of jobs this core can currently support.
	 * 1 if idle, 0 if busy
	 */
	public int getRemainingCapacity(){
		if(this.power_state==PowerState.HALT){
			return 1;
		} else {
			return 0;
		}//End if
	}//End getRemainingCapacity()

	/**
	 * Gets the number of jobs this core can ever support.
	 * Returns 1.
	 */
	public int getTotalCapacity(){
		return 1;
	}//End getTotalCapactiy

	/**
	 * Gets the instant utilization of the core.
	 */
	public double getInstantUtilization(){
		if(this.job == null){
			return 0.0d;
		} else {
			return 1.0d;
		}
	}//End getInstantUtilization()

	public Job getJob() {
		return this.job;
	}//End getJob()

	public void enterPark(double time) {

		this.power_state = PowerState.LOW_POWER_IDLE;

	}//End enterPark()

	public void exitPark(double time) {
		Sim.debug(666, "Core.exitPark()");

		if(this.job == null) {
			Sim.fatalError("Job is null when trying to go to active");
		}//End if

		double finishTime = time + this.job.getSize();	
		Server server = this.socket.getServer();
		JobFinishEvent finishEvent = new JobFinishEvent(finishTime, experiment, job, server, time, this.speed);
		job.setLastResumeTime(time);
		this.experiment.addEvent(finishEvent);
		
		this.power_state = PowerState.ACTIVE;

	}//End exitPark()



	//From 0 to 1.0
	public void setDvfsSpeed(double time, double speed) {
		this.speed = speed;
		//Figure out it's new completion time
		if(this.job != null) {

			JobFinishEvent finishEvent = this.job.getJobFinishEvent();
			this.experiment.cancelEvent(finishEvent);
			
			//	public JobFinishEvent(double time, Experiment experiment, Job job, Server server, double finishSpeed){

			Job job = finishEvent.getJob();
			double finishSpeed = finishEvent.getFinishSpeed();
			double finishStartTime = finishEvent.getFinishTimeSet();
			double duration = time - finishStartTime;
			double alpha = .9;
			double previousSlowdown = (1 - alpha) + alpha/finishSpeed;			
			double workCompleted = duration/previousSlowdown;
			job.setAmountCompleted(job.getAmountCompleted() + workCompleted);
					
			double slowdown = (1 - alpha) + alpha/speed;			
			double finishTime = time + (job.getSize() - job.getAmountCompleted())/slowdown;
			
			JobFinishEvent newFinishEvent = new JobFinishEvent(finishTime, this.experiment, finishEvent.getJob(), this.socket.getServer(), time, this.speed);
			this.experiment.addEvent(newFinishEvent);
			
		}
		
	}//End setDvfsSpeed

	public void pauseProcessing(double time) {
		
		if(this.paused == true) {
			Sim.fatalError("Core paused when it was already paused");
		}		
		
		Sim.debug(666, "...|...|...|...|...PAUSING core  at " + time);

		this.paused = true;
		
		
		if(this.job != null) {
			Sim.debug(666, "...|...|...|...|...Cancelling finish event for " + this.job.getJobId());

			double totalCompleted = this.job.getAmountCompleted() + (time - this.job.getLastResumeTime())/this.speed;	
			Sim.debug(666, "...|...|...|...|...time " + time +" job " +this.job.getJobId() + " totalCompleted " + totalCompleted + " lastresume "+ this.job.getLastResumeTime() +  " previously completed " +this.job.getAmountCompleted());

			if( totalCompleted  > this.job.getSize() + 1e-5){
				System.out.println("time " + time + " job "+this.job.getJobId() + " job size " + job.getSize()  + " totalCompleted " + totalCompleted + " lastresume "+ this.job.getLastResumeTime() +  " previously completed " +this.job.getAmountCompleted());
				Sim.fatalError("totalCompleted can't be more than the job size");			
			}
			if(totalCompleted < 0){
				Sim.fatalError("totalCompleted can't be less than 0");			
			}

			if(this.job.getAmountCompleted()  < 0){
				Sim.fatalError("amountCompleted can't be less than 0");			
			}
			
			this.job.setAmountCompleted(totalCompleted);
			JobFinishEvent finishEvent = this.job.getJobFinishEvent();
			this.experiment.cancelEvent(finishEvent);
			
			
		}
	}

	public void resumeProcessing(double time) {
		Sim.debug(666, "Core.resumeProcessing");

		Sim.fatalError("Need to compnesate for changing speeds here");
		
		if(this.paused == false) {
			Sim.fatalError("Core resumed when it was already running");
		}		
		
		Sim.debug(666, "...|...|...|...|...RESUMING core  at " + time);

		this.paused = false;

		if(this.job != null) {
	
			double timeLeft = (this.job.getSize() - this.job.getAmountCompleted())/this.speed;		
			
			Sim.debug(666, "...|...|...|...|...At time " + time + " job " + this.job.getJobId() + " resume is creating a finish event, timeLeft is "+timeLeft + " job size "+this.job.getSize() + " amount completed " + this.job.getAmountCompleted());
			double finishTime = time + timeLeft;	
			Server server = this.socket.getServer();

			
			if( this.job.getAmountCompleted() < 0){
				System.out.println("At time " + time + " job " + this.job.getJobId() + " resume is creating a finish event, timeLeft is "+timeLeft + " job size "+this.job.getSize() + " amount completed " + this.job.getAmountCompleted());

				Sim.fatalError("amountCompleted can't be less than 0");			
			}
			
			//FISHY
			if(timeLeft > this.job.getSize() + 1e-6|| timeLeft < -1e6){
				System.out.println("At time " + time + " job " + this.job.getJobId() + " resume is creating a finish event, timeLeft is "+timeLeft + " job size "+this.job.getSize() + " amount completed " + this.job.getAmountCompleted());

				Sim.fatalError("time left has been miscalculated");			
			}
			
			JobFinishEvent finishEvent = new JobFinishEvent(finishTime, experiment, job, server, time, this.speed);
			job.setLastResumeTime(time);
			this.experiment.addEvent(finishEvent);
		}
		
	}

	public void setHaltPower(double coreHaltPower) {

		this.halt_power = coreHaltPower;
		
	}

	public void setParkPower(double coreParkPower) {
		this.park_power = coreParkPower;
		
	}

	public void setActivePower(double coreActivePower) {

		this.active_power = coreActivePower;
		
	}
	
	public double getPower() {
		return this.getDynamicPower() + this.getIdlePower();

	}//End getPower()

	public double getDynamicPower(){
		if( this.power_state == PowerState.ACTIVE){
			return this.active_power - this.halt_power;
		} else {
			return 0.0d;
		}
	}//End getDynamicPower()
	
	public double getIdlePower() {
		
		if( this.power_state == PowerState.ACTIVE){
			return this.halt_power;
		} else if(this.power_state == PowerState.LOW_POWER_IDLE) {
			return this.park_power;			
		} else if(this.power_state == PowerState.TRANSITIONG_TO_ACTIVE) {
			//No power is saved during transitions
			return this.active_power;
		} else if(this.power_state == PowerState.TRANSITIONG_TO_LOW_POWER_IDLE) {
			//No power is saved during transitions
			return this.active_power;
		} else if(this.power_state == PowerState.HALT) {
			//		  System.out.println("Halt = " +this.halt_power);
			return this.halt_power;
		} else {
			Sim.fatalError("Unknown power setting");
			return 0;
		}
	}//End getIdlePower()

}//End class Core
