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
package master;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Vector;

import stat.StatisticsCollection;
import core.Experiment;

/**
 * A Master controls the overall state of the simulation and is in
 * charge of commanding slaves.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class Master {

    /**
     * Information about simulation slave are saved in this vector. This
     * information is found in a .cfg file.
     */
    private Vector<SlaveInfo> slaves;

    /**
     * The starting time of an experiment.
     */
    private long startTime;

    /**
     * The master has its own experiment to run at the beginning of simulation.
     * This information is later passed onto slaves.
     */
    private Experiment masterExperiment;

    /**
     * The Experiments for each Slave in the simulation.
     */
    private Experiment[] slaveExperiments;

    /**
     * Constructs a new Master.
     */
    public Master() {
        this.slaves = new Vector<SlaveInfo>();
    }

    /**
     * Connects to all slaves the Master knows off.
     */
    public void connectAll() {
        Iterator<SlaveInfo> iter = this.slaves.iterator();
        while (iter.hasNext()) {
            iter.next().connect();
        }
    }

    /**
     * Checks that the Master can connect to its slaves.
     * Prints a simple output for each.
     */
    public void checkConnectivity() {
        Iterator<SlaveInfo> iter = this.slaves.iterator();
        while (iter.hasNext()) {
            SlaveInfo slaveInfo = iter.next();
            String returnedString = "";
            try {
                returnedString = slaveInfo.getInterface().sayHello();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            System.out.println("Master got output: " + returnedString + "#");
        }
    }

    /**
     * Adds SlaveInfo about a slave to the master.
     * @param serverName - The name to refer to the server by.
     * @param rmiBinding - A RMI binding id.
     */
    public void addSlave(final String serverName, final String rmiBinding) {
        SlaveInfo slaveInfo = new SlaveInfo(serverName, rmiBinding);
        this.slaves.add(slaveInfo);
    }

    /**
     * Gets info about all the slaves of the master.
     * @return An Iterator of SlaveInfo of all the slaves
     */
    public Iterator<SlaveInfo> getSlavesInfo() {
        return this.slaves.iterator();
    }

    /**
     * Gets the name of the Master node.
     * @return The name of the master.
     */
    public String getMasterName() {
        return this.slaves.firstElement().getServerName();
    }

    /**
     * Parses the configuration file specifying the master/slave setup.
     *
     * @param file - The configuration file
     * @return number of slaves in the configuration
     */
    public int parseConfigFile(final String file) {
        System.out.println("Parsing config file: " + file);
        final int partsPerLine = 4;
        final int instancesIndex = 3;

        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                // This line is a comment.
                if (strLine.startsWith("#")) {
                    continue;
                }
                // Valid lines have exactly PARTS_PER_LINE parts.
                String[] parts = strLine.split("\\s+");
                if (parts.length != partsPerLine) {
                    continue;
                }

                String host = parts[0];
                int instances = Integer.valueOf(parts[instancesIndex]);
                for (int i = 0; i < instances; i++) {
                    this.addSlave(host, "sim_" + i);
                }
            }

            in.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        return slaves.size();

    }

    /**
     * Locally run the experiment on the Master until it reaches steady state.
     * @param experiment - The experiment to run
     */
    public void runMasterExperiment(final Experiment experiment) {
        startTime = System.currentTimeMillis();
        masterExperiment = experiment;
        System.out.println("Master starting simulation");
        this.connectAll();
        System.out.println("Checking connectivity");
        this.checkConnectivity();
        System.out.println("Running to steady state on master");
        masterExperiment.runToSteadyState();
        System.out.println("Done running to steady state");
    }

    /**
     * This method runs the slave experiments.
     *
     * @param experiments - an array of experiments
     */
    public void runSlaveExperiment(final Experiment[] experiments) {
        //TODO Do we really need the experiments array?
        final long sleepTime = 10000;
        final double millisecondsPerSecond = 1000.0;
        slaveExperiments = experiments;

        System.out.println("Starting up slaves");
        this.startAllSlaves();
        boolean sleepHold = true;
        while (sleepHold) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean done = true;

            Iterator<SlaveInfo> iter = this.slaves.iterator();
            StatisticsCollection combinedStats = null;
            System.out.println("Checking if combined we're done");
            while (iter.hasNext()) {
                SlaveInfo slave = iter.next();
                try {
                    StatisticsCollection stats = slave.getInterface()
                            .getExperimentStats();
                    stats.printAllStatInfo();
                    if (combinedStats == null) {
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
            System.out.println("combinedStats.allStatsConverged() =="
                    + combinedStats.allStatsConverged());
            if (!combinedStats.allStatsConverged()) {
                done = false;
            }

            if (done) {
                System.out.println("I think i'm done!");
                sleepHold = false;
            }
        }

        System.out.println("Done sleeping");
        this.combine();
        long endTime = System.currentTimeMillis();
        double execTime = (endTime - startTime) / millisecondsPerSecond;
        System.out.println("Combined Experiment time: " + execTime + " (s)");

        Iterator<SlaveInfo> iter = this.slaves.iterator();
        while (iter.hasNext()) {
            SlaveInfo slave = iter.next();
            try {
                slave.getInterface().stop();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method combines and outputs the final statistics.
     */
    public void combine() {
        System.out.println("***** Starting combine phase ****");
        StatisticsCollection totalStats = null;

        Iterator<SlaveInfo> iter = this.slaves.iterator();
        while (iter.hasNext()) {
            SlaveInfo slave = iter.next();
            try {
                StatisticsCollection stats = slave.getInterface()
                        .getExperimentStats();
                if (totalStats == null) {
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
     * This method runs experiments on Slaves.
     */
    private void startAllSlaves() {

        int uniqueSeed = 2;
        Iterator<SlaveInfo> iter = this.slaves.iterator();
        int i = 0;
        while (iter.hasNext()) {
            SlaveInfo slave = iter.next();
            slaveExperiments[i].setSeed(uniqueSeed);
            System.out.println("Running experiment on " + slave.getServerName()
                    + "-" + slave.getRmiBinding());
            try {
                slave.getInterface().runExperiment(slaveExperiments[i]);
            } catch (RemoteException e) {
                System.out.println("Exception as string: " + e.toString());
                e.printStackTrace();

            }
            uniqueSeed++;
            i++;
        }
    }

}
