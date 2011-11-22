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
import java.util.Iterator;
import java.util.Vector;

import stat.SimpleStatistic;

import core.Experiment;
import core.Sim;
import core.Constants.StatName;

public class PowerCappingEnforcer implements Serializable{

	private Vector<Server> servers;
	
	/** The total power cap for the data center */
	private double global_cap;
	/** The data center's maximum power draw (i.e. all servers at 100%) */
	private double max_power;
	/** The data center's minimum power draw (i.e. all servers at 0%) */
	private double min_power;
	/** The time period at which to recalculate server budgets */
	private double cap_period;
	private Experiment experiment;
	
	public PowerCappingEnforcer(Experiment experiment, double capPeriod, double globalCap, double maxPower, double minPower) {
		
		this.servers = new Vector<Server>();
		this.experiment = experiment;
		this.cap_period = capPeriod;
		this.global_cap = globalCap;
		this.min_power = minPower;
		this.max_power = maxPower;
		
		this.experiment.addEvent(new RecalculateCapsEvent(this.cap_period, this.experiment, this));

		
	}//End PowerCappingEnforcer()
	
	public void addServer(Server server) {
		this.servers.add(server);
	}//End addServer()
	
	public void recalculateCaps(double time) {
		
		double totalPower = 0.0d;
		double totalUtil = 0.0d;
		Iterator<Server> iter = this.servers.iterator();
		while(iter.hasNext()){
			Server server = iter.next();
			totalPower += server.getPower();
			totalUtil += server.getInstantUtilization();
		}//End while
		
		double overLimit = totalPower - this.global_cap;
		
		double fungiblePower = this.global_cap  - this.min_power;		
		double powerRate = fungiblePower/totalUtil;
		if(totalUtil == 0){
			powerRate = 1.0;
		}
		
		iter = this.servers.iterator();
		SimpleStatistic serverCapStat = new SimpleStatistic();
		while(iter.hasNext()){
			Server server = iter.next();
			double allocatedPower = powerRate * server.getInstantUtilization() + server.getIdlePower();	
			double idealPower = server.getPower();
			if(Double.isNaN(allocatedPower)){
				Sim.fatalError("NaN!? powerRate " + powerRate + " total util "+totalUtil);
			}
			serverCapStat.addSample(Math.max(idealPower - allocatedPower, 0));			
			server.assignPowerBudget(time, allocatedPower);
		}//End while
		this.experiment.getStats().getStat(StatName.SERVER_LEVEL_CAP).addSample(Math.max(serverCapStat.getAverage(), 0));
		this.experiment.getStats().getStat(StatName.TOTAL_CAPPING).addSample(serverCapStat.getTotal());
		this.experiment.addEvent(new RecalculateCapsEvent(time + this.cap_period, this.experiment, this));
		
	}//End recalculateCaps()
	
}//End class PowerCappingEnforcer
