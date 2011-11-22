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

/** 
 * This package is only used during development
 * It's not a part of final SQS 
 */
package experiments;

import generator.GammaGenerator;
import generator.Generator;
import generator.MTRandom;
import core.Experiment;
import core.ExperimentInput;
import core.ExperimentOutput;
import core.Constants.StatName;
import datacenter.DataCenter;
import datacenter.PowerCappingEnforcer;
import datacenter.Server;
import datacenter.Core.CorePowerPolicy;
import datacenter.Socket.SocketPowerPolicy;

public class CvSensitivity {

	public CvSensitivity(){

	}//End PowerCappingExperiment()

	public void run(double serviceCv, boolean capping) {
		System.out.println("Capping booealn is " + capping);

		ExperimentInput experimentInput = new ExperimentInput();		

		//
		//		String arrivalFile = workloadDir+"workloads/"+workload+".arrival.cdf";
		//		String serviceFile = workloadDir+"workloads/"+workload+".service.cdf";
		//		System.out.println("arrival file "+arrivalFile);
		//		System.out.println("service file "+arrivalFile);
		int cores = 4;
		int sockets = 1;
		double targetRho = .5;

		//		Distribution arrivalDistribution = Distribution.loadDistribution(arrivalFile, 1e-3);
		//		Distribution serviceDistribution = Distribution.loadDistribution(serviceFile, 1e-3);
		MTRandom rand = new MTRandom(1);

		//		double arrivalK = 1.0;  
		//		double arrivalTheta =1.0;

		double arrivalAvg = .200/4.0;
		double serviceAvg = .1;
		//		double serviceCv = 2.0;
		double arrivalCv = 5.0;

		double arrivalK = 1/(arrivalCv*arrivalCv);  
		double arrivalTheta = arrivalAvg/arrivalK;

		double serviceK = 1/(serviceCv*serviceCv);  
		double serviceTheta = serviceAvg/serviceK;

		//		Generator arrivalGenerator = new ExponentialGenerator(rand, lambda);
		Generator arrivalGenerator = new GammaGenerator(rand, arrivalK, arrivalTheta);
		Generator serviceGenerator = new GammaGenerator(rand, serviceK, serviceTheta);
		double averageInterarrival = arrivalAvg;
		double averageServiceTime = serviceAvg;

		double qps = 1/averageInterarrival;
		double rho = qps/(cores*(1/averageServiceTime));
		double arrivalScale = rho/targetRho;
		averageInterarrival = averageInterarrival*arrivalScale;
		double serviceRate = 1/averageServiceTime;
		double scaledQps =(qps/arrivalScale);


		System.out.println("Cores " + cores);
		System.out.println("rho " + rho);		
		System.out.println("recalc rho " + scaledQps/(cores*(1/averageServiceTime)));
		System.out.println("arrivalScale " + arrivalScale);
		System.out.println("Average interarrival time " + averageInterarrival);
		System.out.println("QPS as is " +qps);
		System.out.println("Scaled QPS " +scaledQps);
		System.out.println("Service rate as is " + serviceRate);
		System.out.println("Service rate x" + cores + " is: "+ (serviceRate)*cores);
		System.out.println("\n------------------\n");

		//		EmpiricalGenerator arrivalGenerator  = new EmpiricalGenerator(arrivalDistribution, "arrival", arrivalScale);
		//		EmpiricalGenerator serviceGenerator  = new EmpiricalGenerator(serviceDistribution, "service", 1.0);
		ExperimentOutput experimentOutput = new ExperimentOutput();
		experimentOutput.addOutput(StatName.SOJOURN_TIME, .05, .95, .05, 5000);
			System.out.println("Adding capping!");
			experimentOutput.addOutput(StatName.TOTAL_CAPPING, .05, .95, .05, 5000);
		//		experimentOutput.addTimeWeightedOutput(TimeWeightedStatName.SERVER_POWER, .01, .5, .01, 50000, .001);
		Experiment experiment = new Experiment("Power capping test", rand, experimentInput, experimentOutput);

		DataCenter dataCenter = new DataCenter();

		//		public PowerCappingEnforcer(Experiment experiment, double capPeriod, double globalCap, double maxPower, double minPower) {
		int nServers = 1000;
		double capPeriod = 1.0;
		double globalCap = 70*nServers;
		double maxPower = 100*nServers;
		double minPower = 59*nServers;
		PowerCappingEnforcer enforcer = new PowerCappingEnforcer(experiment, capPeriod, globalCap, maxPower, minPower);
		for(int i = 0; i < nServers; i++) {
			Server server = new Server(sockets, cores, experiment, arrivalGenerator, serviceGenerator);

			server.setSocketPolicy(SocketPowerPolicy.NO_MANAGEMENT);
			server.setCorePolicy(CorePowerPolicy.NO_MANAGEMENT);	
			double coreActivePower = 40 * (4.0/5)/cores;
			double coreHaltPower = coreActivePower*.2;
			double coreParkPower = 0;

			double socketActivePower = 40 * (1.0/5)/sockets;
			double socketParkPower = 0;

			server.setCoreActivePower(coreActivePower);
			server.setCoreParkPower(coreParkPower);
			server.setCoreHaltPower(coreHaltPower);

			server.setSocketActivePower(socketActivePower);
			server.setSocketParkPower(socketParkPower);
			dataCenter.addServer(server);
			enforcer.addServer(server);
		}
		experimentInput.addDataCenter(dataCenter);
		experiment.run();
		double responseTimeMean = experiment.getStats().getStat(StatName.SOJOURN_TIME).getAverage();
//		System.out.println("Response Mean: " + responseTimeMean);
		double responseTime95th = experiment.getStats().getStat(StatName.SOJOURN_TIME).getQuantile(.95);
		experiment.getStats().getStat(StatName.SOJOURN_TIME).printStatInfo();

//		System.out.println("Response 95: " + responseTime95th);
		double averageServerLevelCap = experiment.getStats().getStat(StatName.TOTAL_CAPPING).getAverage();
		experiment.getStats().getStat(StatName.TOTAL_CAPPING).printStatInfo();
		experiment.getStats().printAllStatInfo();
//		System.out.println("Average Server Cap : " + averageServerLevelCap);

	}//End run()

	public static void main(String[] args) {
		CvSensitivity exp  = new CvSensitivity();
		exp.run(Double.valueOf(args[0]),true);
	}

}//End PowerCappingExperiment
