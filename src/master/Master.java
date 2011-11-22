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
package master;

import generator.EmpiricalGenerator;
import generator.MTRandom;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Vector;

import math.Distribution;

import core.Experiment;
import core.ExperimentInput;
import core.ExperimentOutput;
import core.Sim;
import core.Constants.StatName;
import datacenter.DataCenter;
import datacenter.PowerCappingEnforcer;
import datacenter.Server;
import datacenter.Core.CorePowerPolicy;
import datacenter.Socket.SocketPowerPolicy;

import slave.SimInterface;
import stat.Statistic;
import stat.StatsCollection;

public class Master {

	/**
	 * parsed info for slaves are saved in this vector
	 */
	private Vector<SlaveInfo> slaves;
	
	private long startTime;
	private long endTime;
	
	private Experiment master_experiment;
	private Experiment slave_experiments[];
	
	public Master() {
		this.slaves = new Vector<SlaveInfo>();
	}
	
	public void connectAll() {
		
		Iterator<SlaveInfo> iter = this.slaves.iterator();
		while(iter.hasNext()) {
			iter.next().connect();
		}
	}
		
	public void checkConnectivity() {
	
		Iterator<SlaveInfo> iter = this.slaves.iterator();
		while(iter.hasNext()) {
			
			SlaveInfo slaveInfo = iter.next();
			String returnedString = "";
			try {
				returnedString = slaveInfo.getInterface().sayHello();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			System.out.println("Master got output: "+returnedString +"#");
			
		}//End while
		
	}//End checkConnectivity()
	
	public void addSlave(String serverName, String rmiBinding) {
		
		SlaveInfo slaveInfo = new  SlaveInfo(serverName, rmiBinding);
		this.slaves.add(slaveInfo);

	}//End addSlave()
	
	public Iterator<SlaveInfo> getSlavesInfo(){
		return this.slaves.iterator();
	}
	
	public String getMasterName() {
		return this.slaves.firstElement().server_name;
	}

	/** 
	 * This method parses the machine configuration file
	 * @param configuration file
	 * @return number of slaves in the configuration
	 */
	public int parseConfigFile(String file) {
		System.out.println("Parsing config file: " + file);
		
		try{
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while ((strLine = br.readLine()) != null)   {
//				System.out.println("String is " + strLine);
				if(strLine.startsWith("#")){
//					System.out.println("skipping");
					continue;
				}
				String[] parts = strLine.split("\\s+");
				if(parts.length != 4){
					continue;
				}
//				for(int i = 0; i < parts.length; i++) {
//					System.out.println("Part "+i+ " "+parts[i]);
//				}

				String host = parts[0];
				int instances = Integer.valueOf(parts[3]);
				for(int i = 0; i < instances; i++) {
					this.addSlave(host, "sim_"+i);
				}
			}

			in.close();
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		
		return slaves.size();
		
	}//End parseConfigFile()
	
	/** 
	 * This method runs the master experiment
	 * @param x is a well-formed experiment
	 */
	public void runMasterExperiment(Experiment x) {
		startTime = System.currentTimeMillis();
		
		master_experiment = x;
		System.out.println("Master starting simulation");
		this.connectAll();
		System.out.println("Checking connectivity");
		this.checkConnectivity();
		System.out.println("Running to steady state on master");
		master_experiment.runToSteadyState();
		System.out.println("Done running to steady state");
		
	}
	
	/** 
	 * This method runs the slave experiments
	 * @param x is an array of well-formed experiments, derived from the master experiment
	 */
	public void runSlaveExperiment(Experiment[] x) {
		slave_experiments = x;
		
		System.out.println("Starting up slaves");
		this.startAllSlaves();
		boolean sleepHold = true;
		while(sleepHold){
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean done = true;

			Iterator<SlaveInfo> iter = this.slaves.iterator();
			StatsCollection combinedStats = null;
			System.out.println("Checking if combined we're done");
			while(iter.hasNext()) {
				SlaveInfo slave = iter.next();
				try {
					StatsCollection stats = slave.getInterface().getExperimentStats();
					stats.printAllStatInfo();
					if(combinedStats == null){
						combinedStats = stats;
					} else {
						combinedStats = combinedStats.combine(stats);
					}

				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Combined info:");
			combinedStats.printAllStatInfo();
			System.out.println("combinedStats.allStatsConverged() =="+combinedStats.allStatsConverged());
			if(combinedStats.allStatsConverged() == false) {
				done = false;
			}
			
			if(done){
				System.out.println("I think i'm done!");
				sleepHold = false;
			}
		}
				
		System.out.println("Done sleeping");
		this.combine();
		endTime = System.currentTimeMillis();
		double execTime = (endTime - startTime)/1000.0;
		System.out.println("Combined Experiment time: " + execTime + " (s)");
		
		Iterator<SlaveInfo> iter = this.slaves.iterator();
		while(iter.hasNext()) {
			SlaveInfo slave = iter.next();
			try {
				slave.getInterface().stop();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** 
	 * This method combines and outputs the final statistics
	 */
	public void combine() {
		System.out.println("***** Starting combine phase ****");
		StatsCollection totalStats = null;
		
		Iterator<SlaveInfo> iter = this.slaves.iterator();
		while(iter.hasNext()) {
			SlaveInfo slave = iter.next();
			try {
				StatsCollection stats = slave.getInterface().getExperimentStats();
				if(totalStats == null){
					totalStats = stats;
				} else {
					totalStats = totalStats.combine(stats);
				}
				
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("========== Final Statistics ==========");
		totalStats.printConvergedOutputs();
		System.out.println("======================================");
	}
	
	/**
	 *  This method runs experiments on Slaves
	 */
	private void startAllSlaves() {
		
		int uniqueSeed = 2;
		Iterator<SlaveInfo> iter = this.slaves.iterator();
		int i = 0;
		while(iter.hasNext()) {
			SlaveInfo slave = iter.next();
			slave_experiments[i].setSeed(uniqueSeed);	
			System.out.println("Running experiment on " + slave.getServerName() + "-" + slave.getRmiBinding());
			try {
				slave.getInterface().RunExperiment(slave_experiments[i]);
			} catch (RemoteException e) {
				System.out.println("Exception as string: "+e.toString());
				e.printStackTrace();
				
			}
			uniqueSeed++;
			i++;
		}		
	}
	
}//End class Master
