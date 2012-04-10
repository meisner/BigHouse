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
package datacenter;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import stat.SimpleStatistic;

import core.Experiment;
import core.Sim;
import core.Constants.StatName;

/**
 * Decides and assigns power caps for a set of servers.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class PowerCappingEnforcer implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The servers that are assigned caps by the enforcer. */
    private Vector<Server> servers;

    /** The total power cap for the data center. */
    private double globalCap;

    /** The data center's maximum power draw (i.e. all servers at 100%) */
    private double maxPower;

    /** The data center's minimum power draw (i.e. all servers at 0%) */
    private double minPower;

    /** The time period at which to recalculate server budgets. */
    private double capPeriod;

    /** the experiment the enforcer is part of. */
    private Experiment experiment;

    /**
     * Creates a new PowerCappingEnforcer.
     *
     * @param anExperiment - the experiment the enforcer is part of
     * @param theCapPeriod - the period (in seconds) at which
     * caps are recalculated
     * @param theGlobalCap - the max power cap across all servers
     * @param theMaxPower - the minimum possible power draw across all servers
     * @param theMinPower - the maximum possible power draw across all servers
     */
    public PowerCappingEnforcer(final Experiment anExperiment,
                                final double theCapPeriod,
                                final double theGlobalCap,
                                final double theMaxPower,
                                final double theMinPower) {
        this.servers = new Vector<Server>();
        this.experiment = anExperiment;
        this.capPeriod = theCapPeriod;
        this.globalCap = theGlobalCap;
        this.minPower = theMinPower;
        this.maxPower = theMaxPower;
        this.experiment.addEvent(
                new RecalculateCapsEvent(
                        this.capPeriod,
                        this.experiment,
                        this));
    }

    /**
     * Adds a server to this enforcer.
     *
     * @param server - the server to add to the enforcer
     */
    public void addServer(final Server server) {
        this.servers.add(server);
    }

    /**
     * Recalculates power caps for all servers.
     *
     * @param time - the time at which the recalculation takes place
     */
    public void recalculateCaps(final double time) {
        double totalPower = 0.0d;
        double totalUtil = 0.0d;
        Iterator<Server> iter = this.servers.iterator();
        while (iter.hasNext()) {
            Server server = iter.next();
            totalPower += server.getPower();
            totalUtil += server.getInstantUtilization();
        }

        double overLimit = totalPower - this.globalCap;
        double fungiblePower = this.globalCap - this.minPower;
        double powerRate = fungiblePower / totalUtil;
        if (totalUtil == 0) {
            powerRate = 1.0;
        }

        iter = this.servers.iterator();
        SimpleStatistic serverCapStat = new SimpleStatistic();
        while (iter.hasNext()) {
            Server server = iter.next();
            double allocatedPower = powerRate * server.getInstantUtilization()
                    + server.getIdlePower();
            double idealPower = server.getPower();
            if (Double.isNaN(allocatedPower)) {
                Sim.fatalError("NaN!? powerRate " + powerRate + " total util "
                        + totalUtil);
            }
            serverCapStat.addSample(Math.max(idealPower - allocatedPower, 0));
            server.assignPowerBudget(time, allocatedPower);
        }
        this.experiment.getStats().getStat(StatName.SERVER_LEVEL_CAP)
                .addSample(Math.max(serverCapStat.getAverage(), 0));
        this.experiment.getStats().getStat(StatName.TOTAL_CAPPING)
                .addSample(serverCapStat.getTotalAccumulation());
        this.experiment.addEvent(new RecalculateCapsEvent(time
                + this.capPeriod, this.experiment, this));
    }

}
