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
 *         Junjie Wu (wujj@umich.edu)
 *
 */

package core;

import generator.MTRandom;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import stat.Statistic;
import stat.StatisticsCollection;
import datacenter.DataCenter;
import datacenter.Server;

/**
 * This class contains all components of an experiment.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class Experiment implements Serializable, Cloneable {

    /** The Serialization id. */
    private static final long serialVersionUID = 1L;

    /** The experiment's event queue. */
    private EventQueue eventQueue;

    /** The number of events that have been processed. */
    private long nEventsProccessed;

    /** The current time of the simulation. */
    private double currentTime;

    /**
     * The datacenter for the experiment.
     */
    private DataCenter dataCenter;

    /**
     * The input to the experiment.
     */
    private ExperimentInput experimentInput;

    /**
     * The output to the experiment.
     */
    private ExperimentOutput exprimentOutput;

    /**
     * The name of the experiment.
     */
    private String experimentName;

    /**
     * The limit (in number of events) on how many events can be processed.
     */
    private int eventLimit;

    /**
     * THe random number generator for this experiment.
     */
    private MTRandom random;

    /**
     * A flag determining if this experiment should stop once it
     * reaches steady state. Used to just run the characterization phase
     * of the experiment (for the master).
     */
    private boolean stopAtSteadyState;

    /**
     * A flag indicating the simulation should stop at the next possible step.
     */
    private boolean stop;

    /**
     * Constructs a new experiment.
     *
     * @param theExperimentName - the name of the experiment
     * @param aRandom - the random number generator
     * @param theExperimentInput - inputs to the experiment
     * @param thExperimentOutput - outputs of the experiment
     */
    public Experiment(final String theExperimentName,
                      final MTRandom aRandom,
                      final ExperimentInput theExperimentInput,
                      final ExperimentOutput thExperimentOutput) {
        this.stop = false;
        this.random = aRandom;
        this.currentTime = 0.0d;
        this.eventLimit = 0;
        this.experimentName = theExperimentName;
        this.experimentInput = theExperimentInput;
        this.exprimentOutput = thExperimentOutput;
        this.eventQueue = new EventQueue();
        this.stopAtSteadyState = false;
    }

    /**
     * Sets the random seed for this experiment's random number generator.
     * @param newSeed - the random seed for this experiment's
     * random number generator
     */
    public void setSeed(final long newSeed) {
        this.random.setSeed(newSeed);
    }

    /**
     * Initializes the experiment so it is ready to run.
     * This entails priming every server with an initial arrival event.
     */
    public void initialize() {
        this.dataCenter = this.experimentInput.getDataCenter();
        Vector<Server> servers = dataCenter.getServers();
        // Make sure all the arrival processes have begun
        Iterator<Server> iterator = servers.iterator();
        while (iterator.hasNext()) {
            Server server = iterator.next();
            server.createNewArrival(0.0);
        }
    }

    /**
     * Gets the name of the experiment.
     *
     * @return the name of the experiment
     */
    public String getName() {
        return this.experimentName;
    }

    /**
     * Gets the input to the experiment.
     *
     * @return the input to the experiment
     */
    public ExperimentInput getInput() {
        return this.experimentInput;
    }

    /**
     * Gets the output of the experiment.
     *
     * @return the output of the experiment
     */
    public ExperimentOutput getOutput() {
        return this.exprimentOutput;
    }

    /**
     * Gets the statistics collection for the experiment.
     *
     * @return the statistics collection for the experiment
     */
    public StatisticsCollection getStats() {
        return this.exprimentOutput.getStats();
    }

    /**
     * Sets a limit on the number of events the experiment will process.
     *
     * @param theEventLimit - the limit in event on processed events
     */
    public void setEventLimit(final int theEventLimit) {
        this.eventLimit = theEventLimit;
    }

    /**
     * Runs the experiment.
     * The builk of simulation happens in this.
     */
    public void run() {
        this.initialize();
        long startTime = System.currentTimeMillis();

        this.nEventsProccessed = 0;
        Sim.printBanner();
        System.out.println("Starting simulation");
        //TODO fix magic numbers
        int orderOfMag = 5;
        long printSamples = (long) Math.pow(10, orderOfMag);
        while (!stop) {
            Event currentEvent = this.eventQueue.nextEvent();
            this.currentTime = currentEvent.getTime();
            currentEvent.process();
            this.nEventsProccessed++;
            if (this.nEventsProccessed > printSamples) {
                System.out.println("Processed " + this.nEventsProccessed
                            + " events");
                Iterator<Statistic> statIter = this.exprimentOutput.getStats()
                        .getAllStats();
                while (statIter.hasNext()) {
                    Statistic currentStat = statIter.next();
                    if (!currentStat.isConverged()) {
                        System.out.println("Still waiting for "
                                + currentStat.getStatName()
                                + " at mean converge of "
                                + currentStat.getMeanAccuracy()
                                + " and quantile converge of "
                                + currentStat.getQuantileAccuracy());
                        currentStat.printStatInfo();
                    }
                }
                orderOfMag++;
                printSamples = (long) Math.pow(10, orderOfMag);
            }

            if (this.getStats().allStatsConverged()) {
                System.out.println("Ending from convergence");
                break;
            }

            if (this.getStats().allStatsSteadyState()
                    && this.stopAtSteadyState) {
                System.out.println("Halting at steady state");
                break;
            }

            if (eventLimit > 0 && nEventsProccessed > eventLimit) {
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        double execTime = (endTime - startTime) / 1000.0;
        System.out.println("The experiment took " + execTime
                        + " seconds to run");
    }

    /**
     * Gets the number of events that have been simulated.
     *
     * @return the number of events that have been simulated
     */
    public long getNEventsSimulated() {
        return nEventsProccessed;
    }

    /**
     * Adds an event to the experiment's event queue.
     *
     * @param event - the event to add
     */
    public void addEvent(final Event event) {
        this.eventQueue.addEvent(event);
    }

    /**
     * Cancels an event so that it no longer occurs.
     *
     * @param event - the event to cancel
     */
    public void cancelEvent(final Event event) {
        this.eventQueue.cancelEvent(event);
    }

    /**
     * Get the current time of the simulation.
     *
     * @return the current time of the simulation
     */
    public double getCurrentTime() {
        return this.currentTime;
    }

    /**
     * Runs the experiment to steady state, but no further.
     */
    public void runToSteadyState() {
        Iterator<Statistic> stats = this.getStats().getAllStats();
        while (stats.hasNext()) {
            Statistic stat = stats.next();
            stat.setJustBins(true);
        }
        this.stopAtSteadyState = true;
        this.run();
    }

    /**
     * Stops the simulation.
     */
    public synchronized void stop() {
        this.stop = true;
    }

}
