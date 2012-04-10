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
package slave;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import stat.StatisticsCollection;

import core.Experiment;

/**
 * The top-level calss and main entry point for slaves
 * in a distributed simulation.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class Slave implements SimInterface {

    /** The RMI binding name. */
    private String bindName;

    /** The hostname of the server. */
    private String hostname;

    /** The experiment runner thread. */
    private ExperimentRunner experimentRunner;

    /**
     * Creates a new slave.
     *
     * @param theBindName - the RMI binding name
     */
    public Slave(final String theBindName) {
        this.bindName = theBindName;
        try {

            InetAddress addr = InetAddress.getLocalHost();
            this.hostname = addr.getHostName();

        } catch (UnknownHostException e) {
            //TODO Handle exception
        }
    }

    /**
     * Ready the slave to accept RMI binding.
     */
    public void setup() {

        try {
            SimInterface stub
                = (SimInterface) UnicastRemoteObject.exportObject(this, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(this.bindName, stub);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

    /**
     * A remote interface to ping the slave and say hello back.
     *
     * @return a hello message
     */
    public String sayHello() {
        return "Hello from " + hostname + " bound on " + bindName;
    }

    /**
     * The slave main function. Creates a slave and has it setup.
     * @param args - command line arguments.
     * The first should be the RMI bind name.
     */
    public static void main(final String[] args) {
        Slave slave = new Slave(args[0]);
        slave.setup();
    }

    /**
     * Stops the slave.
     *
     * @throws RemoteException - if the stop fails
     */
    public void stop() throws RemoteException {
        System.out.println("Goodbye!");
        this.experimentRunner.getExperiment().stop();
    }

    /**
     * has the slave run an experiment.
     *
     * @param experiment - the experiment to run
     * @throws RemoteException - an exception if the remote interface fails
     */
    public void runExperiment(final Experiment experiment)
            throws RemoteException {
        System.out.println(experiment.getName());
        System.out.println("Slave is starting simulation");
        this.experimentRunner = new ExperimentRunner(experiment);
        this.experimentRunner.start();
        System.out.println("Slave returned from run call");
    }

    /**
     * Gets the statistics collection of the slave.
     *
     * @return the statistics collection of the slave
     * @throws RemoteException - an exception if the remote interface fails
     */
    public StatisticsCollection getExperimentStats() throws RemoteException {
        return this.experimentRunner.getExperiment().getStats();
    }

    /**
     * An experiment runner is a thread of execution which
     * runs an experiment.
     */
    private class ExperimentRunner extends Thread {

        /** The experiment to run. */
        private Experiment experiment;

        /**
         * Creates a new ExperimentRunner.
         *
         * @param anExperiment - the experiment to run
         */
        public ExperimentRunner(final Experiment anExperiment) {
            this.experiment = anExperiment;
        }

        /**
         * Gets the experiment being run.
         *
         * @return - the experiment being run
         */
        public Experiment getExperiment() {
            return this.experiment;
        }

        /**
         * Start the thread and run the experiment.
         */
        public void run() {
            this.experiment.run();
        }

    }

}
