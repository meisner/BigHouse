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
package slave;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import stat.StatsCollection;

import core.Experiment;

public class Slave implements SimInterface {

	private String bind_name;
	private String hostname;
	private ExperimentRunner experiment_runner;
	
	public Slave(String bindName) {

	     this.bind_name = bindName;
	      try {
	          InetAddress addr = InetAddress.getLocalHost();

	          // Get IP Address
	          byte[] ipAddr = addr.getAddress();

	          // Get hostname
	          this.hostname = addr.getHostName();
	      } catch (UnknownHostException e) {
	      }

		
	}//End Slave()
	
	public void setup(){
		
		try {			
			SimInterface stub = (SimInterface) UnicastRemoteObject.exportObject(this, 0);
			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(this.bind_name, stub);
			System.err.println("Server ready");
		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
		
	}//End setup()

	public String sayHello() {
        return "Hello from "+hostname+" bound on "+bind_name;
	}//End sayHello()

	public static void main(String args[]) {
		Slave slave = new Slave(args[0]);
		slave.setup();
	}//End main()
	
	public void stop() throws RemoteException{
		System.out.println("Goodbye!");
		this.experiment_runner.getExperiment().stop();
	}
	
	public void RunExperiment(Experiment experiment) throws RemoteException {
	  System.out.println(experiment.getName());	
	  System.out.println("Slave is starting simulation");
	  this.experiment_runner = new ExperimentRunner(experiment);
	  this.experiment_runner.start();
	  System.out.println("Slave returned from run call");
	}//End runExperiment()

	public StatsCollection getExperimentStats() throws RemoteException {
		return this.experiment_runner.getExperiment().getStats();		
	}//End stopExperiment()
	
	private class ExperimentRunner extends Thread{
		
		private Experiment experiment;
		
		public ExperimentRunner(Experiment experiment) {
			this.experiment = experiment;
		}
		
		public Experiment getExperiment() {
			return this.experiment;
		}

		public void run() {
			this.experiment.run();			
		}
		
	}
	
}//End class Slave
