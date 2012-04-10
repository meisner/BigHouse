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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import core.Constants;
import core.Event;
import core.Experiment;
import core.Job;
import core.Sim;
import core.SocketEnteredParkEvent;
import core.SocketExitedParkEvent;
import datacenter.Core.CorePowerPolicy;

/**
 * This class represents a single socket (physical processor chip) in a server.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class Socket implements Powerable, Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The server this Socket belongs to.
     */
    private Server server;

    /**
     * The number of cores in this socket.
     */
    private int nCores;

    /**
     * The experiment the socket is part of.
     */
    private Experiment experiment;

    /**
     * A mapping of jobs to cores.
     * This allows bookeeping of cores when jobs finish.
     */
    private HashMap<Job, Core> jobToCoreMap;

    /**
     * Available socket power states.
     */
    private enum SocketPowerState {
        /**
         * Socket is active and can process jobs.
         */
        ACTIVE,

        /**
         * Socket is in the process of transitioning to idle.
         */
        TRANSITIONG_TO_LOW_POWER_IDLE,

        /**
         * Socket is in the process of transitioning to active.
         */
        TRANSITIONG_TO_ACTIVE,

        /**
         * Socket is in a low-power idle state.
         */
        LOW_POWER_IDLE
    };

    /**
     * Available power management policies for the socket.
     */
    public static enum SocketPowerPolicy {
        /**
         * No power management policy is being used.
         */
        NO_MANAGEMENT,

        /**
         * Socket goes into socket parking.
         */
        SOCKET_PARKING
    };

    /**
     * The policy used by the socket for power management.
     */
    private SocketPowerPolicy powerPolicy;

    /**
     * The power state of the socket.
     */
    private SocketPowerState powerState;

    /**
     * Cores are available to process jobs.
     */
    private Vector<Core> availableCores;

    /**
     *  Cores that are busy processing a job.
     */
    private Vector<Core> busyCores;

    /**
     * A temporary queue for jobs while a socket is transitioning.
     */
    private Vector<Job> transitionQueue;

    /** The power consumed by the socket in park (in watts). */
    private double socketParkPower;

    /** The power consumed by the socket while idle (in watts). */
    private double socketActiveIdlePower;

    /** The transition time of the socket in and out of park (in seconds) .*/
    private double socketParkTransitionTime;

    /**
     * The event transitioning the socket.
     * Used to allow cancellation if a job arrival interrupts the transition.
     */
    private Event trasitionEvent;

    /**
     * Instantiate a socket with nCores cores.
     *
     * @param anExperiment - the experiment the socket is part of
     * @param aServer - the server the socket is part of
     * @param theNCores - the number of cores in the socket
     */
    public Socket(final Experiment anExperiment,
                  final Server aServer,
                  final int theNCores) {

        this.experiment = anExperiment;
        this.server = aServer;
        this.nCores = theNCores;

        this.jobToCoreMap = new HashMap<Job, Core>();
        this.availableCores = new Vector<Core>();
        this.busyCores = new Vector<Core>();
        this.transitionQueue = new Vector<Job>();

        // Create nCores Cores and put them on the free list
        for (int i = 0; i < nCores; i++) {
            Core core = new Core(experiment, this);
            this.availableCores.add(core);
        }

        this.powerPolicy = SocketPowerPolicy.NO_MANAGEMENT;
        this.powerState = SocketPowerState.ACTIVE;
    }

    /**
     * Start a job for the first time on the socket.
     * It will be assigned to a random core.
     *
     * @param time - the time the job is inserted
     * @param job - the job being inserted
     */
    public void insertJob(final double time, final Job job) {

        if (this.powerState == SocketPowerState.ACTIVE) {
            // Pick the first core off the available cores
            Core core = this.availableCores.remove(0);
            core.insertJob(time, job);
            this.busyCores.add(core);

            // Save the core the job is on so we can remove it later
            this.jobToCoreMap.put(job, core);
        } else if (this.powerState
                   == SocketPowerState.TRANSITIONG_TO_LOW_POWER_IDLE) {
            this.transitionQueue.add(job);
            this.powerState = SocketPowerState.TRANSITIONG_TO_ACTIVE;

            if (this.trasitionEvent != null) {
                this.experiment.cancelEvent(this.trasitionEvent);
            }

            double exitParkTime = time + Constants.SOCKET_PARK_TRANSITION_TIME;
            SocketExitedParkEvent socketExitedParkEvent
                    = new SocketExitedParkEvent(exitParkTime,
                                                this.experiment,
                                                this);
            this.experiment.addEvent(socketExitedParkEvent);
        } else if (this.powerState == SocketPowerState.TRANSITIONG_TO_ACTIVE) {
            this.transitionQueue.add(job);
        } else if (this.powerState == SocketPowerState.LOW_POWER_IDLE) {
            this.transitionQueue.add(job);
            this.powerState = SocketPowerState.TRANSITIONG_TO_ACTIVE;
            double exitParkTime = time + Constants.SOCKET_PARK_TRANSITION_TIME;
            SocketExitedParkEvent socketExitedParkEvent
                = new SocketExitedParkEvent(exitParkTime,
                                            this.experiment,
                                            this);
            this.experiment.addEvent(socketExitedParkEvent);
        }

    }

    /**
     * Removes a job from the socket due to completion.
     *
     * @param time - the time the job is removed
     * @param job - the job being removed
     * @param jobWaiting - If there is a job waiting for this to be removed
     */
    public void removeJob(final double time,
                          final Job job,
                          final boolean jobWaiting) {

        // Find out which socket this job was running on
        Core core = this.jobToCoreMap.remove(job);

        // Error check we got a real socket
        if (core == null) {
            Sim.fatalError("Couldn't resolve which core this job belonged to");
        }

        core.removeJob(time, job, jobWaiting);

        // Mark that the job is no longer busy
        boolean found = this.busyCores.remove(core);

        // Error check the socket was considered busy
        if (!found) {
            Sim.fatalError("Could take core off the busy list");
        }

        // Core is now available
        this.availableCores.add(core);

        if (this.busyCores.size() == 0 && !jobWaiting) {
            if (this.powerPolicy == SocketPowerPolicy.SOCKET_PARKING) {
                this.powerState
                    = SocketPowerState.TRANSITIONG_TO_LOW_POWER_IDLE;
                double enterParkTime
                    = time + Constants.SOCKET_PARK_TRANSITION_TIME;
                SocketEnteredParkEvent socketEnteredParkEvent
                        = new SocketEnteredParkEvent(enterParkTime,
                                                     this.experiment,
                                                     this);
                this.experiment.addEvent(socketEnteredParkEvent);
                this.trasitionEvent = socketEnteredParkEvent;
            }
            // Otherwise the socket stays active
        }
    }

    /**
     * Gets the number of cores that have slots for jobs.
     *
     * @return the number of cores that are available for jobs
     */
    public int getRemainingCapacity() {
        return this.availableCores.size() - this.transitionQueue.size();
    }

    /**
     * Gets the number of jobs this socket can ever support.
     *
     * @return the total number of cores/jobs the socket can support.
     */
    public int getTotalCapacity() {
        return this.nCores;
    }

    /**
     * Gets the instant utilization of the socket (busy cores/ total cores).
     *
     * @return the instant utilization of the core
     */
    public double getInstantUtilization() {
        return ((double) this.busyCores.size() + this.transitionQueue.size())
                / this.nCores;
    }

    /**
     * Gets an Vector of cores on this socket.
     *
     * @return a vector of the cores on the socket
     */
    public Vector<Core> getCores() {
        Vector<Core> combined = new Vector<Core>();
        combined.addAll(this.availableCores);
        combined.addAll(this.busyCores);

        return combined;
    }

    /**
     * Gets the server the socket is on.
     *
     * @return the server the socket belongs to
     */
    public Server getServer() {
        return this.server;
    }

    /**
     * Get the number of jobs being serviced.
     *
     * @return The number of jobs being serviced
     */
    public int getJobsInService() {
        return this.busyCores.size();
    }

    /**
     * Set the power management policy of the cores in the socket.
     *
     * @param corePowerPolicy - the power management policy to use on the cores
     */
    public void setCorePolicy(final CorePowerPolicy corePowerPolicy) {
        Vector<Core> cores = this.getCores();
        Iterator<Core> iter = cores.iterator();
        while (iter.hasNext()) {
            Core core = iter.next();
            core.setPowerPolicy(corePowerPolicy);
        }
    }

    /**
     * Set the power management policy of the socket.
     *
     * @param policy - the power management policy to set the socket to
     */
    public void setPowerPolicy(final SocketPowerPolicy policy) {
        this.powerPolicy = policy;
    }

    /**
     * Put the socket into park.
     * @param time - the time the socket is put into park
     */
    public void enterPark(final double time) {
        if (this.busyCores.size() != 0) {
            Sim.fatalError("Socket tried to enter park when it shouldn't have");
        }

        this.powerState = SocketPowerState.LOW_POWER_IDLE;
    }

    /**
     * Take the socket out of park.
     *
     * @param time - the time the socket comes out of park
     */
    public void exitPark(final double time) {
        this.powerState = SocketPowerState.ACTIVE;
        Iterator<Job> iter = this.transitionQueue.iterator();
        while (iter.hasNext()) {
            Job job = iter.next();
            this.insertJob(time, job);
        }
        this.transitionQueue.clear();
    }

    /**
     * Gets the number of jobs waiting for the socket to transition.
     *
     * @return the number of jobs waiting for the socket to transition
     */
    public int getNJobsWaitingForTransistion() {
        return this.transitionQueue.size();
    }

    /**
     * Pause processing at the socket.
     *
     * @param time - the time the socket processing is paused
     */
    public void pauseProcessing(final double time) {
        Vector<Core> cores = this.getCores();
        Iterator<Core> iter = cores.iterator();
        while (iter.hasNext()) {
            Core core = iter.next();
            core.pauseProcessing(time);
        }
    }

    /**
     * Resume processing at the socket.
     *
     * @param time - the time the socket resumes processing
     */
    public void resumeProcessing(final double time) {
        Vector<Core> cores = this.getCores();
        Iterator<Core> iter = cores.iterator();
        while (iter.hasNext()) {
            Core core = iter.next();
            core.resumeProcessing(time);
        }
    }

    /**
     * Set the socket's active power.
     *
     * @param socketActivePower - the power consumed by the socket while active
     */
    public void setSocketActivePower(final double socketActivePower) {
        this.socketActiveIdlePower = socketActivePower;
    }

    /**
     * Set the socket's power while parked.
     *
     * @param theSocketParkPower - the power of the socket while parked
     */
    public void setSocketParkPower(final double theSocketParkPower) {
        this.socketParkPower = theSocketParkPower;
    }

    /**
     * Set the idle power of the socket's cores (in watts).
     *
     * @param coreHaltPower - the idle power of the socket's cores (in watts)
     */
    public void setCoreIdlePower(final double coreHaltPower) {
        Iterator<Core> iter = this.getCores().iterator();
        while (iter.hasNext()) {
            Core core = iter.next();
            core.setIdlePower(coreHaltPower);
        }
    }

    /**
     * Set the park power of the socket's core.
     *
     * @param coreParkPower - the power of the socket's cores whil parked
     */
    public void setCoreParkPower(final double coreParkPower) {
        Iterator<Core> iter = this.getCores().iterator();
        while (iter.hasNext()) {
            Core core = iter.next();
            core.setParkPower(coreParkPower);
        }
    }

    /**
     * Sets the active power of the socket's cores (in watts).
     *
     * @param coreActivePower - the active power of the
     * socket's cores (in watts)
     */
    public void setCoreActivePower(final double coreActivePower) {
        Iterator<Core> iter = this.getCores().iterator();
        while (iter.hasNext()) {
            Core core = iter.next();
            core.setActivePower(coreActivePower);
        }
    }

    /**
     * Sets the DVFS speed for all the socket's cores.
     *
     * @param time - the time at which the speed is set
     * @param speed - the speed to set the cores to (relative to 1.0)
     */
    public void setDvfsSpeed(final double time, final double speed) {
        Iterator<Core> iter = this.getCores().iterator();
        while (iter.hasNext()) {
            iter.next().setDvfsSpeed(time, speed);
        }
    }

    /**
     * Get the current power consumption of the socket (dynamic + leakage).
     *
     * @return the current power consumption of the socket
     */
    public double getPower() {
        return this.getDynamicPower() + this.getIdlePower();
    }

    /**
     * Get the current idle power of the socket and its cores.
     *
     * @return the current idle power of the socket and its cores
     */
    public double getIdlePower() {

        double idlePower = 0.0d;
        if (this.powerState == SocketPowerState.ACTIVE) {

            Iterator<Core> coreIter = this.getCores().iterator();
            while (coreIter.hasNext()) {
                Core core = coreIter.next();
                double corePower = core.getIdlePower();
                idlePower += corePower;
            }
            idlePower += Constants.SOCKET_IDLE_POWER;

        } else if (this.powerState == SocketPowerState.TRANSITIONG_TO_ACTIVE) {

            idlePower = Constants.SOCKET_IDLE_POWER;

        } else if (this.powerState
                        == SocketPowerState.TRANSITIONG_TO_LOW_POWER_IDLE) {

            idlePower = Constants.SOCKET_IDLE_POWER;

        } else if (this.powerState == SocketPowerState.LOW_POWER_IDLE) {

            idlePower = Constants.SOCKET_PARK_POWER;

        }

        return idlePower;
    }

    /**
     * Get the current dynamic power consumption of the socket.
     * Modeled as the sum of the core dynamic power.
     * Uncore power is all leakage.
     *
     * @return the current dynamic power consumption of the socket
     */
    public double getDynamicPower() {

        double dynamicPower = 0.0d;

        Iterator<Core> coreIter = this.getCores().iterator();
        while (coreIter.hasNext()) {
            Core core = coreIter.next();
            double corePower = core.getDynamicPower();
            dynamicPower += corePower;
        }

        return dynamicPower;
    }

}
