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

import generator.Generator;
import core.Experiment;
import core.Job;
import core.Sim;

/**
 * A PowerNap server transitions to a low-power "nap" state when idle.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class PowerNapServer extends Server {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    //TODO check this is in fact in seconds not milliseconds
    /**
     * The transition time in and out of nap (in seconds).
     */
    protected double napTransitionTime;

    /**
     * The power of the server while in nap mode (in watts).
     */
    private double napPower;

    /**
     * The power state of the PowerNap server.
     */
    public enum PowerNapState {
        /** The PowerNap server is active. */
        ACTIVE,

        /** The PowerNap server is transitioning to active. */
        TRANSITIONING_TO_ACTIVE,

        /**  The PowerNap server is transitioning to nap. */
        TRANSITIONING_TO_NAP,

        /** The PowerNap server is napping. */
        NAP
    };

    /** The current power state of the PowerNap server. */
    protected PowerNapState powerNapState;

    /** The transition event if the server is transitioning. */
    private PowerNapTransitionedToNapEvent transitionEvent;

    /** Whether the PowerNap server is transitioning to active. */
    private boolean transitioningToActive;

    /** Whether the PowerNap server is transitioning to nap. */
    private boolean transitioningToNap;

    /**
     * Creates a new PowerNapServer.
     *
     * @param theNumberOfSockets - the number of sockets the server has
     * @param coresPerSocket - the number of cores per sockets
     * @param experiment - the experiment the server is part of
     * @param arrivalGenerator - The interarrival generator for the server
     * @param serviceGenerator - The service time generator for the server
     * @param theNapTransitionTime - the transition time in
     * and out of the the nap mode (in seconds)
     * @param theNapPower - the power of the server while in nap mode (in watts)
     */
    public PowerNapServer(final int theNumberOfSockets,
                          final int coresPerSocket,
                          final Experiment experiment,
                          final Generator arrivalGenerator,
                          final Generator serviceGenerator,
                          final double theNapTransitionTime,
                          final double theNapPower) {
        super(theNumberOfSockets, coresPerSocket, experiment, arrivalGenerator,
                serviceGenerator);

        this.napTransitionTime = theNapTransitionTime;
        this.napPower = theNapPower;
        this.powerNapState = PowerNapState.NAP;
        this.transitioningToActive = false;
        this.transitioningToNap = false;
        this.pauseProcessing(0);
    }

    /**
     * Checks if the PowerNap server is napping.
     *
     * @return if the PowerNap server is napping
     */
    public boolean isNapping() {
        return this.powerNapState == PowerNapState.NAP
                || this.powerNapState == PowerNapState.TRANSITIONING_TO_NAP
                || this.transitioningToNap;
    }

    /**
     * Inserts the job into the server.
     *
     * @param time - the time the job is inserted
     * @param job - the job to be inserted
     */
    @Override
    public void insertJob(final double time, final Job job) {
        if (this.powerNapState == PowerNapState.ACTIVE) {

            super.insertJob(time, job);

        } else if (this.powerNapState == PowerNapState.TRANSITIONING_TO_NAP) {

            this.transistionToActive(time);
            this.queue.add(job);

            // Job has entered the system
            this.jobsInServerInvariant++;

        } else if (this.powerNapState
                    == PowerNapState.TRANSITIONING_TO_ACTIVE) {

            this.queue.add(job);
            // Job has entered the system
            this.jobsInServerInvariant++;

        } else if (this.powerNapState == PowerNapState.NAP) {

            this.transistionToActive(time);
            this.queue.add(job);
            // Job has entered the system
            this.jobsInServerInvariant++;

        } else {

            Sim.fatalError("Uknown power state");

        }
    }
    
    /**
     * Directly inserts the job into the server, bypassing PowerNap logic.
     * This is a bit of hack and should be changed eventually.
     *
     * @param time - the time the job is inserted
     * @param job - the job to be inserted
     */
    public void directlyInsertJob(final double time, final Job job) {
    	super.insertJob(time, job);
   
    }

    /**
     * Get the time for the PowerNap server to transition.
     *
     * @return the time for the PowerNap server to transition
     */
    public double getNapTransitionTime() {
        return this.napTransitionTime;
    }

    /**
     * Transitions the server to the active state.
     *
     * @param time - the time to start transitioning
     */
    public void transistionToActive(final double time) {
        if (!this.isNapping()) {
            Sim.fatalError("Trying to transition to active when not napping");
        }

        if (!this.isPaused()) {
            Sim.fatalError("Trying to transition to active when not paused");
        }

        double extraDelay = 0;
        if (this.transitionEvent != null) {
            double timeServerWouldHaveReachedNap = this.transitionEvent
                    .getTime();
            extraDelay += timeServerWouldHaveReachedNap - time;
            this.getExperiment().cancelEvent(this.transitionEvent);
            this.transitioningToNap = false;
        }
        this.transitioningToActive = true;
        this.powerNapState = PowerNapState.TRANSITIONING_TO_ACTIVE;
        double napTime = time + extraDelay + this.napTransitionTime;
        PowerNapTransitionedToActiveEvent napEvent
            = new PowerNapTransitionedToActiveEvent(napTime,
                                                    this.getExperiment(),
                                                    this);
        this.getExperiment().addEvent(napEvent);
    }

    /**
     * Transition the server to the nap state.
     *
     * @param time - the time the server is transitioned
     */
    public void transistionToNap(final double time) {
        // Make sure this transition is valid
        if (this.isNapping()) {
            Sim.fatalError("Trying to transition to nap when napping");
        }
        // Make sure this transition is valid
        if (this.isPaused()) {
            Sim.fatalError("Trying to transition to nap when paused");
        }

        this.powerNapState = PowerNapState.TRANSITIONING_TO_NAP;
        this.transitioningToNap = true;
        double napTime = time + this.napTransitionTime;
        PowerNapTransitionedToNapEvent napEvent
            = new PowerNapTransitionedToNapEvent(napTime,
                                                 this.getExperiment(),
                                                 this);
        this.transitionEvent = napEvent;
        this.getExperiment().addEvent(napEvent);
        this.pauseProcessing(time);
    }

    /**
     * Removes a job from the server.
     *
     * @param time
     *            - the time the job is removed
     * @param job
     *            - the job to be removed
     */
    @Override
    public void removeJob(final double time, final Job job) {
        super.removeJob(time, job);
        if (this.getJobsInService() == 0) {
            this.transistionToNap(time);
        }
    }

    /**
     * Sets the server to active.
     *
     * @param time
     *            - the time the server becomes active
     */
    public void setToActive(final double time) {
        this.transitioningToActive = false;
        // Server is now fully in the active mode
        this.powerNapState = PowerNapState.ACTIVE;
        // Start all the jobs possible and queue the ones that aren't
        this.resumeProcessing(time);
    }

    /**
     * Checks if the server is currently transitioning to the active state.
     *
     * @return if the server is currently transitioning to the active state
     */
    public boolean isTransitioningToActive() {
        return transitioningToActive;
    }

    /**
     * Checks if the server is currently transitioning to the nap state.
     *
     * @return if the server is currently transitioning to the nap state
     */
    public boolean isTransitioningToNap() {
        return transitioningToNap;
    }

    /**
     * Puts the server in the nap mode.
     *
     * @param time - the time the server is put in the nap mode.
     */
    public void setToNap(final double time) {
        // Server is now fully in the nap mode
        this.transitioningToNap = false;
        this.powerNapState = PowerNapState.NAP;
        this.transitionEvent = null;
    }

    /**
     * Gets the instantaneous power of the PowerNap server.
     *
     * @return the instantaneous power of the PowerNap server
     */
    @Override
    public double getPower() {
        double power = 0.0d;

        if (this.powerNapState == PowerNapState.ACTIVE) {

            power = super.getPower();

        } else if (this.powerNapState
                    == PowerNapState.TRANSITIONING_TO_ACTIVE) {

            power = super.getPower();

        } else if (this.powerNapState == PowerNapState.TRANSITIONING_TO_NAP) {

            power = super.getPower();

        } else if (this.powerNapState == PowerNapState.NAP) {

            power = this.napPower;

        }

        return power;
    }

}
