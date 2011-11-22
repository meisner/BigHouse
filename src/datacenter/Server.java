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

import generator.Generator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import stat.Statistic;
import stat.TimeWeightedStatistic;
import core.Constants;
import core.Experiment;
import core.Job;
import core.JobArrivalEvent;
import core.Sim;
import core.Constants.StatName;
import datacenter.Core.CorePowerPolicy;
import datacenter.Socket.SocketPowerPolicy;

/**
 * This class represents a physical server in a data center
 * It behaves much like a queuing theory queue with servers equal to the number of cores
 * A server has a set of sockets which are the physical chips, each with a number of cores
 */
public class Server implements Powerable, Constants, Serializable{

	private enum Scheduler{ 
		BIN_PACK, 
		LOAD_BALANCE
	};

	private Scheduler scheduler;
	protected Socket[] sockets;
	private HashMap<Job, Socket> jobToSocketMap; //Map that saves which socket has a job to avoid searching
	protected Experiment experiment; //The experiment this server is running in

	private boolean paused;
	protected LinkedList<Job> queue;

	private Generator arrival_generator;
	private Generator service_generator;

	protected int jobs_in_server_invariant;

	/**
	 * Creates a new Server
	 */
	public Server(int sockets, int coresPerSocket, Experiment experiment, Generator arrivalGenerator, Generator serviceGenerator){

		this.experiment = experiment;
		this.arrival_generator = arrivalGenerator;
		this.service_generator = serviceGenerator;
		this.queue = new LinkedList<Job>();
		this.sockets = new Socket[sockets];
		for(int i = 0; i < sockets; i++){
			this.sockets[i] = new Socket(experiment, this, coresPerSocket);
		}
		this.jobToSocketMap = new HashMap<Job, Socket>();
		this.scheduler = Scheduler.LOAD_BALANCE;
		this.jobs_in_server_invariant = 0;
		this.paused = false;

	}//End Server()


	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	
	public boolean isPaused() {
		return this.paused;
	}
 	
	public void createNewArrival(double time) {

		double interarrivalTime = this.arrival_generator.next();
		double arrivalTime = time + interarrivalTime;
		double serviceTime = this.service_generator.next();
		Statistic arrivalStat = this.experiment.getStats().getStat(StatName.GENERATED_ARRIVAL_TIME);
		arrivalStat.addSample(interarrivalTime);
		Statistic serviceStat = this.experiment.getStats().getStat(StatName.GENERATED_SERVICE_TIME);
		serviceStat.addSample(serviceTime);

		Job job = new Job(serviceTime);
		JobArrivalEvent jobArrivalEvent = new JobArrivalEvent(arrivalTime, experiment, job, this, interarrivalTime);
		this.experiment.addEvent(jobArrivalEvent);		

	}//End createNewArrivals()


	public void setSocketJobMapping(double time, Socket socket, Job job){
		this.jobToSocketMap.put(job, socket);
	}
	
	/**
	 * This method is called when a job FIRST arrives at a server
	 */
	public void insertJob(double time, Job job) {
		Sim.debug(666, "Server.insertJob()");

		//Check if the job should be serviced now or put in the queue
		if(this.getRemainingCapacity() == 0){
			//There was no room in the server, put it in the queue
			Sim.debug(666, "There's no room so job is going to be put in the queue");

			this.queue.add(job);
		}else{
			//The job can start service immediately
			Sim.debug(666, "The job can start immediately");

			this.startJobService(time, job);			
		}//End if

		//Job has entered the system
		this.jobs_in_server_invariant++;
		checkInvariants();

	}//End insertJob()

	public int getJobsInSystem() {

		//Jobs that need to be counted for socket parking
		int transJobs = 0 ;
		for(int i = 0; i < this.sockets.length; i++){
			transJobs += this.sockets[i].getJobsInTransistion();
		}

		int jobsInSystem = this.getQueueLength() + this.getJobsInService() + transJobs;
		//		System.out.println("There are " + " in the queue" + " in service" + " in transition");
		return jobsInSystem;

	}//End getJobsInSystem()

	public void checkInvariants() {

		int jobsInSystem = this.getJobsInSystem(); 
		if(jobsInSystem != this.jobs_in_server_invariant){
//			Sim.fatalError("From insert: Job balance is off. There should be "+ this.jobs_in_server_invariant + " jobs in the system but there are "+jobsInSystem);
		}

	}//End checkInvariants()

	public void updateStatistics(double time){

		TimeWeightedStatistic serverPowerStat = this.experiment.getStats().getTimeWeightedStat(TimeWeightedStatName.SERVER_POWER);
		serverPowerStat.addSample(this.getPower(), time);

		TimeWeightedStatistic serverUtilStat = this.experiment.getStats().getTimeWeightedStat(TimeWeightedStatName.SERVER_UTILIZATION);
		serverUtilStat.addSample(this.getInstantUtilization(), time);

		double idleness = 1.0;
		if(this.isIdle()){
			idleness = 0.0;
		}//End if

		TimeWeightedStatistic serverIdleStat = this.experiment.getStats().getTimeWeightedStat(TimeWeightedStatName.SERVER_IDLE_FRACTION);
		serverIdleStat.addSample(idleness, time);

	}//End updateStatistics()

	public boolean isIdle() {
		if(this.getJobsInSystem() > 0){
			return true;
		} else {
			return false;
		}//End if

	}//End isIdle()

	public int getQueueLength(){

		return this.queue.size();		
	}//End getQueueLength()

	public int getRemainingCapacity(){

		int capacity = 0;

		for(int i = 0; i < this.sockets.length; i++){
			capacity += this.sockets[i].getRemainingCapacity();
		}

		return capacity;
	}//End getRemainingCapacity()

	/*
	 * Gets the number of jobs this server can ever support.
	 */
	public int getTotalCapacity(){

		int nJobs = 0;
		for(int i = 0; i < this.sockets.length; i++){
			nJobs += this.sockets[i].getTotalCapacity();
		}

		return nJobs;

	}//End getTotalCapcity()

	public int getJobsInService(){

		int nInService = 0;

		for(int i = 0; i < this.sockets.length; i++){
			nInService += this.sockets[i].getJobsInService();
		}//End for i

		return nInService;

	}//End getJobsInService()

	/** 
	 * This method is called when the job first starts service
	 * (When is first comes in and there's spare capacity
	 *  or when it is taken out of the queue for the first time)
	 */
	public void startJobService(double time, Job job){

		Socket targetSocket = null;
		Socket mostUtilizedSocket = null;
		double highestUtilization = Double.MIN_VALUE;
		Socket leastUtilizedSocket = null;
		double lowestUtilization = Double.MAX_VALUE;

		for(int i = 0; i < this.sockets.length; i++) {

			Socket currentSocket = this.sockets[i];
			double currentUtilization = currentSocket.getInstantUtilization();

			if(currentUtilization > highestUtilization && currentSocket.getRemainingCapacity() > 0){
				highestUtilization = currentUtilization;
				mostUtilizedSocket = currentSocket;
			}//End if

			if(currentUtilization < lowestUtilization && currentSocket.getRemainingCapacity() > 0){
				lowestUtilization = currentUtilization;
				leastUtilizedSocket = currentSocket;
			}//End if

		}//End for i

		//Pick a socket to put the job on depending on the scheduling policy
		if(this.scheduler == Scheduler.BIN_PACK){			
			targetSocket = mostUtilizedSocket;
		} else if(this.scheduler == Scheduler.LOAD_BALANCE){
			targetSocket = leastUtilizedSocket;
		} else {
			Sim.fatalError("Bad scheduler");
		}//End if

		job.markStart(time);
		targetSocket.insertJob(time, job);
		this.jobToSocketMap.put(job, targetSocket);

	}//End startJobService()

	/**
	 * This method is called when a job leaves the server
	 * because it has finished service
	 */
	public void removeJob(double time, Job job) {

		//Remove the job from the socket it is running on
		Socket socket = this.jobToSocketMap.remove(job);

		//Error check we could resolve which socket the job was on
		if(socket == null){
			Sim.fatalError("Job to Socket mapping failed");
		}//End if

		//See if we're going to schedule another job or if it can go to sleep
		boolean jobWaiting = !this.queue.isEmpty();

		//Remove the job from the socket (which will remove it from the core)
		socket.removeJob(time, job, jobWaiting);

		//There is now a spot for a job, see if there's one waiting
		if(jobWaiting){
			Job dequeuedJob = this.queue.poll();
			this.startJobService(time, dequeuedJob);
		}		

		//Job has left the systems
		this.jobs_in_server_invariant--;

		this.checkInvariants();
		//		this.updateStatistics(time);

	}//End removeJob()

	/**
	 * Gets the instant utilization of the server.
	 * utilization = (jobs running)/(total capacity)
	 */
	public double getInstantUtilization(){

		double avg = 0.0d;

		for(int i = 0; i < this.sockets.length; i++){
			avg += this.sockets[i].getInstantUtilization();
		}//End for i

		avg /= this.sockets.length;

		return avg;

	}//End getInstantUtilization()

	public Experiment getExperiment(){
		return this.experiment;
	}//End getExperiment()

	public Socket[] getSockets() {
		return this.sockets;
	}//End getSockets()

	public void setCorePolicy(CorePowerPolicy corePowerPolicy) {
		for(int i = 0; i < sockets.length; i++){
			this.sockets[i].setCorePolicy(corePowerPolicy);
		}
	}//End setCorePolicy

	public double getPower() {

		double totalPower = this.getDynamicPower() + this.getIdlePower();

		return totalPower;

	}//End getPower()
	
	public double getDynamicPower() {
		
		double dynamicPower = 0.0d;				

		for(int i = 0; i < this.sockets.length; i++){
			dynamicPower += this.sockets[i].getDynamicPower();
		}//End for i


		double util = this.getInstantUtilization();
		double memoryPower = 10*util;
		double diskPower = 1.0*util;
		double otherPower = 5.0*util;
		
		dynamicPower += memoryPower + diskPower + otherPower;
		
		return dynamicPower;
		
	}//End getDynamicPower()
	
	private double getMaxCpuDynamicPower() {
		return 25.0;//End getMaxCpuPower
	}
	
	public double getIdlePower() {

		double idlePower = 0.0d;
		
		for(int i = 0; i < this.sockets.length; i++){
			idlePower += this.sockets[i].getIdlePower() ;
		}//End for i

		double memoryPower = 25;
		double diskPower = 9;
		double otherPower = 10;
		idlePower += memoryPower + diskPower + otherPower;
		
		return idlePower;		
	}//End getIdlePower()

	public void resumeProcessing(double time) {
		
		this.paused = false;
		
		Sim.debug(666, "...|...|...RESUMING server  at " + time);
		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].resumeProcessing(time);
		}
		Sim.debug(666, "...|...|...Checking if there was spare capacity " + time);
		while(this.getRemainingCapacity() > 0 && this.queue.size() != 0 ) {
			Sim.debug(666, "...|...|...I'm inserting jobs from the queue " + time);

			Job job = this.queue.poll();

			this.startJobService(time, job);
		}

	}

	public void pauseProcessing(double time) {
		
		this.paused = true;		
		
		Sim.debug(666, "PAUSING server  at " + time);

		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].pauseProcessing(time);
		}

	}


	public void setSocketPolicy(SocketPowerPolicy socketPolicy) {
		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setPowerPolicy(socketPolicy);
		}

	}


	public void setCoreActivePower(double coreActivePower) {
		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setCoreActivePower(coreActivePower);
		}

	}


	public void setCoreParkPower(double coreParkPower) {
		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setCoreParkPower(coreParkPower);
		}


	}


	public void setCoreHaltPower(double coreHaltPower) {

		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setCoreHaltPower(coreHaltPower);
		}

	}


	public void setSocketActivePower(double socketActivePower) {
		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setSocketActivePower(socketActivePower);
		}

	}


	public void setSocketParkPower(double socketParkPower) {
		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setSocketParkPower(socketParkPower);
		}

	}

	public void setDvfsSpeed(double time, double speed) {
		for(int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setDvfsSpeed(time, speed);
		}//End for i
	}//End setDvfsSpeed() 

	public void assignPowerBudget(double time, double allocatedPower) {

		double dvfsSpeed = 0.0;
		double nonScalablePower = this.getMaxPower() - this.getMaxCpuDynamicPower();
		if(allocatedPower < nonScalablePower){
			dvfsSpeed = 0.5;
		} else if ( allocatedPower > this.getMaxPower()) {
			dvfsSpeed = 1.0;
		} else {
			double targetCpuPower = allocatedPower - nonScalablePower;
			dvfsSpeed = Math.pow(targetCpuPower/this.getMaxCpuDynamicPower(), 1/3.0);
			dvfsSpeed = Math.max(dvfsSpeed, .5);
		}//End if/else
		
		this.setDvfsSpeed(time, dvfsSpeed);

	}//End assignPowerBudget()
	
	public double getMaxPower() {
		return 100.0;		
	}//End getMaxPower()


}//End class Server
