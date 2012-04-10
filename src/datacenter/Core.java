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

import core.CoreEnteredParkEvent;
import core.CoreExitedParkEvent;
import core.AbstractEvent;
import core.Experiment;
import core.Job;
import core.JobFinishEvent;
import core.Sim;

/**
 * This class represents a single core on a processor (socket). It can only run
 * one job at a time.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class Core implements Powerable, Serializable {

    /**
     * The serializaiton id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The job running on the core.
     * Is null if there is no job.
     */
    private Job job;

    /**
     * THe possible power states the core can be in.
     */
    private enum PowerState {

        /** The core is actively processing. */
        ACTIVE,

        /** Transitioning to park. */
        TRANSITIONINGG_TO_LOW_POWER_IDLE,

        /** Transitioning to active from park. */
        TRANSITIONINGG_TO_ACTIVE,

        /** The core is in the "halt" mode (idle). */
        HALT,

        /** The core is in park mode.*/
        LOW_POWER_IDLE
    };

    /**
     * The possible core power management policies.
     */
    public static enum CorePowerPolicy {
        /** No power management. Simply go to halt when idle. */
        NO_MANAGEMENT,
        /** Transition to core parking when the core is idle. */
        CORE_PARKING
    };

    /** The current core power state. */
    private PowerState powerState;

    /** The current power management policy. */
    private CorePowerPolicy powerPolicy;

    /**
     * The speed at which the core is running.
     * The relative (1.0 is no slowdown) speed the core is operating at
     * (determines job completion times)
     */
    private double speed;

    /**
     * The experiment the core is part of.
     */
    private Experiment experiment;

    /**
     * The socket the core is part of.
     */
    private Socket socket;


    /**
     * The dynamic power consumption of the core.
     */
    private double dynamicPower;

    /**
     * The power of the core while parked.
     */
    private double parkPower;

    /**
     * The idle power of the core.
     */
    private double idlePower;

    /**
     * The transition time of the core to halt.
     */
    private double transitionToParkTime;

    /**
     * An event representing the core transitioning, if it is happening.
     * null otherwise.
     */
    private AbstractEvent transitionEvent;

    /**
     * If the core is paused.
     */
    private boolean paused;

    /**
     * Constructs a new Core.
     *
     * @param anExperiment - the experiment the core is part of
     * @param aSocket - the socket the core is part of
     */
    public Core(final Experiment anExperiment, final Socket aSocket) {
        this.experiment = anExperiment;
        // Core starts without a job
        this.job = null;
        this.socket = aSocket;
        this.powerState = PowerState.HALT;
        this.powerPolicy = CorePowerPolicy.NO_MANAGEMENT;

        // No slowdown or speedup
        this.speed = 1.0;

        //TODO fix the magic numbers
        dynamicPower = 40.0 * (4.0 / 5.0) / 2;
        parkPower = 0;
        idlePower = dynamicPower / 5.0;
        transitionToParkTime = 100e-6;
        this.paused = false;
    }

    /**
     * Sets the power management currently used by the core.
     * @param policy - the power management policy used by the core
     */
    public void setPowerPolicy(final CorePowerPolicy policy) {
        this.powerPolicy = policy;
    }

    /**
     * Gets the power management currently used by the core.
     * @return the power management policy used by the core
     */
    public CorePowerPolicy getPowerPolicy() {
        return this.powerPolicy;
    }

    /**
     * Puts a job on the core for the first time.
     *
     * @param time - the time the job is inserted
     * @param aJob - the job to add to the core
     */
    public void insertJob(final double time, final Job aJob) {
        // Error check that we never try to put two jobs on one core
        if (this.job != null) {
            Sim.fatalError("Tried to insert a job into a core"
                           + " that was already busy");
        }

        // Assign job to core
        this.job = aJob;

        if (this.powerState == PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE) {
            // We need to interrupt transitioning to low power idle
            if (this.transitionEvent.getClass()
                != CoreEnteredParkEvent.class) {
                Sim.fatalError("Tried to cancel the wrong type of event");
            }
            this.experiment.cancelEvent(this.transitionEvent);
        }

        if (this.powerState == PowerState.LOW_POWER_IDLE
            || this.powerState == PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE) {
            // We need to transition out of low power
            double exitTime = time + this.transitionToParkTime;
            CoreExitedParkEvent coreExitedParkEvent = new CoreExitedParkEvent(
                    exitTime, this.experiment, this);
            this.experiment.addEvent(coreExitedParkEvent);
        } else {
            double alpha = .9;
            double slowdown = (1 - alpha) + alpha / this.speed;
            double finishTime = time + this.job.getSize() / slowdown;
            Server server = this.socket.getServer();
            JobFinishEvent finishEvent = new JobFinishEvent(finishTime,
                    experiment, aJob, server, time, this.speed);
            aJob.setLastResumeTime(time);
            this.experiment.addEvent(finishEvent);
            // Core now goes into full power state
            this.powerState = PowerState.ACTIVE;
        }
    }

    /**
     * Removes a job from the core because of job completion.
     *
     * @param time - the time the job is removed
     * @param aJob - the job that is being removed
     * @param jobWaiting - if there is a job waiting after this is removed
     */
    public void removeJob(final double time,
                          final Job aJob,
                          final boolean jobWaiting) {
        // Error check we're not trying to remove a job from an empty core
        if (this.job == null) {
            Sim.fatalError("Tried to remove a job from a core"
                           + " when there wasn't one");
        }

        // Error check we're removing the correct job
        if (!this.job.equals(aJob)) {
            Sim.fatalError("Tried to remove a job,"
                           + "but it didn't match the job on the core");
        }

        // Null signifies the core is idle
        this.job = null;

        // If no job is waiting, we can begin transitioning to a low power state
        if (!jobWaiting) {

            if (this.powerPolicy == CorePowerPolicy.CORE_PARKING) {
                this.powerState = PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE;
                double enteredLowPowerTime = time + this.transitionToParkTime;
                CoreEnteredParkEvent coreEnteredParkEvent
                    = new CoreEnteredParkEvent(enteredLowPowerTime,
                                               this.experiment,
                                               this);
                this.transitionEvent = coreEnteredParkEvent;
                this.experiment.addEvent(coreEnteredParkEvent);

            } else {
                this.powerState = PowerState.HALT;
            }
        }
    }

    /**
     * Gets the number of jobs this core can currently support.
     * 1 if idle, 0 if busy.
     *
     * @return the number of jobs the core can currently support
     */
    public int getRemainingCapacity() {
        if (this.powerState == PowerState.HALT) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Gets the number of jobs this core can ever support. Returns 1.
     *
     * @return the number of jobs this core can support at once (is 1)
     */
    public int getTotalCapacity() {
        return 1;
    }

    /**
     * Gets the instant utilization of the core.
     *
     * @return the instant utilization of the core
     */
    public double getInstantUtilization() {
        if (this.job == null) {
            return 0.0d;
        } else {
            return 1.0d;
        }
    }

    /**
     * Get the job running on the core.
     * Is null if there is no job.
     *
     * @return the job running on the core or null
     */
    public Job getJob() {
        return this.job;
    }

    /**
     * Puts the core into park mode.
     *
     * @param time - the time the core enters park
     */
    public void enterPark(final double time) {
        this.powerState = PowerState.LOW_POWER_IDLE;
    }

    /**
     * Takes the core out of park.
     *
     * @param time - the time the core exits park
     */
    public void exitPark(final double time) {
        if (this.job == null) {
            Sim.fatalError("Job is null when trying to go to active");
        }

        double finishTime = time + this.job.getSize();
        Server server = this.socket.getServer();
        JobFinishEvent finishEvent = new JobFinishEvent(finishTime, experiment,
                job, server, time, this.speed);
        job.setLastResumeTime(time);
        this.experiment.addEvent(finishEvent);
        this.powerState = PowerState.ACTIVE;
    }

    /**
     * Sets the DVFS speed of the core.
     *
     * @param time - the time the speed is changed
     * @param theSpeed - the speed to change the core (relative to 1.0)
     */
    public void setDvfsSpeed(final double time, final double theSpeed) {
        this.speed = theSpeed;
        // Figure out it's new completion time
        if (this.job != null) {

            JobFinishEvent finishEvent = this.job.getJobFinishEvent();
            this.experiment.cancelEvent(finishEvent);

            Job theJob = finishEvent.getJob();
            double finishSpeed = finishEvent.getFinishSpeed();
            double finishStartTime = finishEvent.getFinishTimeSet();
            double duration = time - finishStartTime;
            //TODO Fix this magic number
            double alpha = 0.9;
            double previousSlowdown = (1 - alpha) + alpha / finishSpeed;
            double workCompleted = duration / previousSlowdown;
            theJob.setAmountCompleted(theJob.getAmountCompleted()
                                        + workCompleted);
            double slowdown = (1 - alpha) + alpha / theSpeed;
            double finishTime = time
                    + (theJob.getSize() - theJob.getAmountCompleted())
                        / slowdown;

            JobFinishEvent newFinishEvent = new JobFinishEvent(finishTime,
                    this.experiment, finishEvent.getJob(),
                    this.socket.getServer(), time, this.speed);
            this.experiment.addEvent(newFinishEvent);
        }
    }

    /**
     * Pauses processing at the core.
     *
     * @param time - the time the processing is paused
     */
    public void pauseProcessing(final double time) {
        if (this.paused) {
            Sim.fatalError("Core paused when it was already paused");
        }

        this.paused = true;

        if (this.job != null) {
            double totalCompleted = this.job.getAmountCompleted()
                + (time - this.job.getLastResumeTime()) / this.speed;

            // TODO fix this fudge factor
            if (totalCompleted > this.job.getSize() + 1e-5) {
                System.out.println("time " + time + " job "
                        + this.job.getJobId() + " job size " + job.getSize()
                        + " totalCompleted " + totalCompleted + " lastresume "
                        + this.job.getLastResumeTime()
                        + " previously completed "
                        + this.job.getAmountCompleted());
                Sim.fatalError("totalCompleted can't be"
                               + "more than the job size");
            }

            if (totalCompleted < 0) {
                Sim.fatalError("totalCompleted can't be less than 0");
            }

            if (this.job.getAmountCompleted() < 0) {
                Sim.fatalError("amountCompleted can't be less than 0");
            }
            this.job.setAmountCompleted(totalCompleted);
            JobFinishEvent finishEvent = this.job.getJobFinishEvent();
            this.experiment.cancelEvent(finishEvent);
        }
    }

    /**
     * Resumes processing at the core.
     *
     * @param time - the time the processing resumes
     */
    public void resumeProcessing(final double time) {
        if (!this.paused) {
            Sim.fatalError("Core resumed when it was already running");
        }

        this.paused = false;
        if (this.job != null) {
            double timeLeft = (this.job.getSize() - this.job
                    .getAmountCompleted()) / this.speed;

            double finishTime = time + timeLeft;
            Server server = this.socket.getServer();

            if (this.job.getAmountCompleted() < 0) {
                System.out.println("At time " + time + " job "
                        + this.job.getJobId()
                        + " resume is creating a finish event, timeLeft is "
                        + timeLeft + " job size " + this.job.getSize()
                        + " amount completed " + this.job.getAmountCompleted());
                Sim.fatalError("amountCompleted can't be less than 0");
            }

            // TODO this is FISHY
            if (timeLeft > this.job.getSize() + 1e-6 || timeLeft < -1e6) {
                System.out.println("At time " + time + " job "
                        + this.job.getJobId()
                        + " resume is creating a finish event, timeLeft is "
                        + timeLeft + " job size " + this.job.getSize()
                        + " amount completed " + this.job.getAmountCompleted());
                Sim.fatalError("time left has been miscalculated");
            }

            JobFinishEvent finishEvent = new JobFinishEvent(finishTime,
                    experiment, job, server, time, this.speed);
            job.setLastResumeTime(time);
            this.experiment.addEvent(finishEvent);
        }
    }

    /**
     * Sets the power of the core while idle (in watts).
     *
     * @param coreIdlePower - the power of the core while idle (in watts)
     */
    public void setIdlePower(final double coreIdlePower) {
        this.idlePower = coreIdlePower;
    }

    /**
     * Sets the park power of the core (in watts).
     *
     * @param coreParkPower - the park power of the core (in watts)
     */
    public void setParkPower(final double coreParkPower) {
        this.parkPower = coreParkPower;
    }

    /**
     * Sets the dynamic power consumption of the core (in watts).
     *
     * @param coreDynamicPower - the dynamic power consumption
     * of the core (in watts)
     */
    public void setActivePower(final double coreDynamicPower) {
        this.dynamicPower = coreDynamicPower;
    }

    /**
     * Gets the total instantaneous power consumption of the core (in watts).
     *
     * @return the total instantaneous power consumption of the core (in watts)
     */
    public double getPower() {
        return this.getDynamicPower() + this.getIdlePower();
    }

    /**
     * Gets the instantaneous dynamic power component of the core (in watts).
     *
     * @return the instantaneous dynamic power component of the core (in watts).
     */
    public double getDynamicPower() {
        if (this.powerState == PowerState.ACTIVE) {
            return this.dynamicPower - this.idlePower;
        } else {
            return 0.0d;
        }
    }

    /**
     * Gets the instantaneous idle power component
     * of the core (leakage) (in watts).
     *
     * @return the instantaneous idle power component
     * of the core (leakage) (in watts).
     */
    public double getIdlePower() {
        if (this.powerState == PowerState.ACTIVE) {
            return this.idlePower;
        } else if (this.powerState == PowerState.LOW_POWER_IDLE) {
            return this.parkPower;
        } else if (this.powerState
                   == PowerState.TRANSITIONINGG_TO_ACTIVE) {
            // No power is saved during transitions
            return this.dynamicPower;
        } else if (this.powerState
                   == PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE) {
            // No power is saved during transitions
            return this.dynamicPower;
        } else if (this.powerState == PowerState.HALT) {
            return this.idlePower;
        } else {
            Sim.fatalError("Unknown power setting");
            return 0;
        }
    }

}
