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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import generator.Generator;
import core.Experiment;
import core.Job;
import core.DreamWeaverJobTimeoutEvent;
import core.Sim;

/**
 * A DreamWeaver server is a server which intelligently
 * reschedules jobs to create idle opportunities for PowerNap.
 * See DreamWeaver: Architectural support for deep sleep ASPLOS 2012
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class DreamWeaverServer extends PowerNapServer {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /** The maximum delay (in seconds) a job may be delayed. */
    private double maxDelay;

    /** A mapping of jobs to time out events. */
    private HashMap<Job, DreamWeaverJobTimeoutEvent> timeouteventMap;

    /** The jobs which have timed out. */
    private Vector<Job> timedOutJobs;

    /**
     * Creates a new DreamWeaverServer.
     *
     * @param nSockets - the number of cores per socket
     * @param coresPerSocket - the number of cores per socket
     * @param experiment - the experiment the server is part of
     * @param arrivalGenerator - the interarrival time generator
     * @param serviceGenerator - the service time generator
     * @param napTransitionTime - the transition time (in seconds)
     * in and out of nap
     * @param napPower - the power while in nap (in watts)
     * @param theMaxDelay - the maximum delay for a job (in seconds)
     */
    public DreamWeaverServer(final int nSockets,
                             final int coresPerSocket,
                             final Experiment experiment,
                             final Generator arrivalGenerator,
                             final Generator serviceGenerator,
                             final double napTransitionTime,
                             final double napPower,
                             final double theMaxDelay) {
        super(nSockets,
              coresPerSocket,
              experiment,
              arrivalGenerator,
              serviceGenerator,
              napTransitionTime,
              napPower);
        this.maxDelay = theMaxDelay;
        this.timeouteventMap = new HashMap<Job, DreamWeaverJobTimeoutEvent>();
        this.pause();
        this.timedOutJobs = new Vector<Job>();
    }

    /**
     * This method is called when a job times out.
     * i.e., it has exceeded its maximum delay.
     * The server will then transition to active.
     *
     * @param time - the time the job times out
     * @param timeoutJob - the job that has timed out
     */
    public void handleJobTimeout(final double time,
                                 final Job timeoutJob) {
        this.timedOutJobs.add(timeoutJob);
        this.timeouteventMap.remove(timeoutJob);

        if (!this.isTransitioningToActive()) {
            this.transistionToActive(time);
        }
    }

    /**
     * Insert a job into the server.
     *
     * @param time - the time the job is inserted
     * @param job - the job that is inserted
     */
    @Override
    public void insertJob(final double time, final Job job) {

        if (!this.isNapping() && !this.isPaused()) {

            super.insertJob(time, job);

        } else if (this.getJobsInService() + this.queue.size() + 1
                    >= this.getTotalCapacity()) {

            if (!this.isTransitioningToActive()) {
                this.transistionToActive(time);
            }
            super.insertJob(time, job);

        } else if (this.isTransitioningToActive()) {
            super.insertJob(time, job);
        } else {

            if (this.getRemainingCapacity() > 0) {
            	// TODO This bypassing behavior is a bit of hack and
            	// should probably be improved at some point.
                super.directlyInsertJob(time, job);
                this.experiment.cancelEvent(job.getJobFinishEvent());               
                double timeout = time + this.maxDelay;
                DreamWeaverJobTimeoutEvent timeoutEvent
                    = new DreamWeaverJobTimeoutEvent(timeout,
                                                     this.experiment,
                                                     job,
                                                     this);
                this.experiment.addEvent(timeoutEvent);
                this.timeouteventMap.put(job, timeoutEvent);
            } else {
                this.queue.add(job);
            }

        }

        this.jobsInServerInvariant++;
        checkInvariants();
    }

    /**
     * Remove a job from the server.
     *
     * @param time - the time the job is removed
     * @param job - the job to remove
     */
    @Override
    public void removeJob(final double time, final Job job) {
        if (this.isPaused()) {
            Sim.fatalError("time " + time + " job " + job.getJobId()
                    + " Shouldn't be removing jobs when the server is paused");
        }

        if (this.isNapping()) {
            Sim.fatalError("time " + time + " job " + job.getJobId()
                    + " Shouldn't be removing jobs when the server is napping");
        }

        this.timedOutJobs.remove(job);
        super.removeJob(time, job);

        if (this.getJobsInService() + this.queue.size() < this
                .getTotalCapacity()
                && this.timedOutJobs.size() == 0
                && !this.isTransitioningToNap()) {
            this.transistionToNap(time);

        }
    }

    /**
     * Cancels the timeout event for a job. If a job completes, there will still
     * be a timeout scheduled. Since this refers to a job that no longer is in
     * the system it should be canceled.
     *
     * @param time
     *            - the time the job is canceled
     */
    public void cancelTimeoutEvent(final double time) {
        for (int i = 0; i < sockets.length; i++) {
            Socket socket = sockets[i];
            Vector<Core> cores = socket.getCores();
            Iterator<Core> iter = cores.iterator();
            while (iter.hasNext()) {
                Core core = iter.next();
                Job coreJob = core.getJob();

                if (coreJob != null) {
                    DreamWeaverJobTimeoutEvent timeoutEvent
                        = this.timeouteventMap.get(coreJob);
                    if (timeoutEvent == null) {
                        if (!this.timedOutJobs.contains(coreJob)) {
                            Sim.fatalError("The timeout event was null"
                                    + " and wasn't in the timedout event list");
                        }
                    } else {
                        this.timeouteventMap.remove(coreJob);
                        this.experiment.cancelEvent(timeoutEvent);
                    }
                }
            }
        }
    }

    @Override
    public void setToActive(final double time) {
        this.cancelTimeoutEvent(time);
        super.setToActive(time);
    }

    @Override
    public void transistionToActive(final double time) {
        super.transistionToActive(time);
    }

    @Override
    public void transistionToNap(final double time) {
        super.transistionToNap(time);
        // Make sure to schedule timeouts for jobs
        Socket[] sockets = this.getSockets();
        for (int i = 0; i < sockets.length; i++) {
            Socket socket = sockets[i];
            Vector<Core> cores = socket.getCores();
            Iterator<Core> iter = cores.iterator();
            while (iter.hasNext()) {
                Core core = iter.next();
                Job coreJob = core.getJob();

                if (coreJob != null) {
                    double amountDelayed = time - coreJob.getStartTime()
                            - coreJob.getAmountCompleted();
                    if (amountDelayed < 0) {
                        Sim.fatalError("the amount delayed can't be negative");
                    }

                    double timeoutTime = time
                            + (this.maxDelay - amountDelayed);

                    if (this.maxDelay - amountDelayed < 0) {
                        System.out.println("at time " + time + " max_delay "
                                + maxDelay + " amount Delayed "
                                + amountDelayed + " time delta"
                                + (this.maxDelay - amountDelayed));
                        Sim.fatalError("I should never have a negative"
                                + " delta for my timeout ");
                    }

                    DreamWeaverJobTimeoutEvent timeoutEvent
                        = new DreamWeaverJobTimeoutEvent(timeoutTime,
                                                         this.experiment,
                                                         coreJob,
                                                         this);
                    this.timeouteventMap.put(coreJob, timeoutEvent);
                    this.experiment.addEvent(timeoutEvent);
                }
            }
        }
    }

}
