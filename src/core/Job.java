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

package core;

/**
 * A job is the basic unit of work that servers process.
 * The amount of "work" they represent is quantified in seconds.
 * For example, if a job is 2 seconds big. It will complete in
 * 2 seconds from starting, given that nothing interrupts it.
 * This may be modulated by many things (e.g., slowing the CPU).
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class Job {

    /**
     * When the job arrived in the system This variable should be set only once.
     */
    private double arrivalTime;

    /**
     * When the job actually started (i.e. after queuing delays).
     * This variable should be set only once.
     */
    private double startTime;

    /**
     * When the job completes This variable should be set only once.
     */
    private double finishTime;

    /**
     * Every job has a unique monotonically increasing id.
     */
    private long jobId;

    /**
     * The "length" of the job.
     * Divide by server rate to get the time to completion.
     */
    private double jobSize;

    /**
     * If the job is suspended, this will give the amount that has been
     * completed.
     */
    private double amountCompleted;

    /**
     * Commulatively tracks how many seconds this job has been delayed.
     */
    private double amountDelayed;

    /**
     * A static incrementing variable for creating job IDs.
     */
    private static long currentId;

//    /**
//     *
//     */
//    private boolean atLimit;

    /**
     * The event that will occur when the job finishes.
     */
    private JobFinishEvent jobFinishEvent;

    /**
     * The last time this job was resumed.
     * Useful when jobs are paused.
     */
    private double lastResumeTime;

    /**
     * Constructs a new job.
     * @param theJobSize - The size of the job in seconds.
     */
    public Job(final double theJobSize) {
        this.amountCompleted = 0.0;
        this.amountDelayed = 0.0;
        this.jobSize = theJobSize;
        this.jobId = assignId();
//        this.atLimit = false;
        this.jobFinishEvent = null;
        this.lastResumeTime = 0.0;
    }

//    public void setAtLimit(boolean atLimit) {
//        this.atLimit = atLimit;
//    }
//
//    public boolean getAtLimit() {
//        return this.atLimit;
//    }

    /**
     * Gets the amount (in seconds) the job has been delayed.
     * @return the amount (in seconds) the job has been delayed.
     */
    public final double getAmountDelayed() {
        return this.amountDelayed;
    }

    /**
     * Set the amount the job has been delayed (in seconds).
     * @param amount - how much the job has been delayed (in seconds).
     */
    public final void setAmountDelayed(final double amount) {
        this.amountDelayed = amount;
    }

    /**
     * Sets how much of the job has been completed (in seconds).
     * @param completed - how much of the job has been completed (in seconds).
     */
    public final void setAmountCompleted(final double completed) {
        this.amountCompleted = completed;
    }

    /**
     * Gets how much of the job has been completed (in seconds).
     * @return how much of the job has been completed (in seconds).
     */
    public final double getAmountCompleted() {
        return this.amountCompleted;
    }

    /**
     * Assigns an id to this job.
     * Increments the current id for future jobs.
     * @return the id assigned to the calling job
     */
    private long assignId() {
        long toReturn = Job.currentId;
        Job.currentId++;
        return toReturn;
    }

    /**
     * Gets the job id of the job.
     * @return the job's job id
     */
    public final long getJobId() {
        return this.jobId;
    }

    /**
     * Marks the arrival time of the job.
     * Should only ever need to be called once and the simulation
     * will fail if this is called twice or more on a given job.
     * @param time - the time the job arrives
     */
    public final void markArrival(final double time) {
        if (this.arrivalTime > 0) {
            Sim.fatalError("Job arrival marked twice!");
        }
        this.arrivalTime = time;
    }

    /**
     * Mark the start time of the job.
     * Should only ever need to be called once and the simulation
     * will fail if this is called twice or more on a given job.
     * @param time - the start time of the job
     */
    public final void markStart(final double time) {
        if (this.startTime > 0) {
            Sim.fatalError("Job start marked twice!");
        }
        this.startTime = time;
    }

    /**
     * Marks the time the job finished.
     * Should only ever need to be called once and the simulation
     * will fail if this is called twice or more on a given job.
     * @param time - the finish time of the job
     */
    public final void markFinish(final double time) {
        if (this.finishTime > 0) {
            Sim.fatalError("Job " + this.getJobId() + " finsih marked twice!");
        }
        this.finishTime = time;
    }

    /**
     * Gets the time the job arrived at its server.
     * @return the arrival time of the job at its server
     */
    public final double getArrivalTime() {
        return this.arrivalTime;
    }

    /**
     * Gets the time at which the job started.
     * @return the start time of the job
     */
    public final double getStartTime() {
        return this.startTime;
    }

    /**
     * Gets the time at which the job finished.
     * @return the finish time of the job
     */
    public final double getFinishTime() {
        return this.finishTime;
    }

    /**
     * Gets the size of the job (in seconds).
     * @return the size of the job (in seconds)
     */
    public final double getSize() {
        return this.jobSize;
    }

    /**
     * Checks if another job is equal to this one.
     * @param obj - the object that is compared to this one.
     * @return true if the job is equal to this one.
     */
    @Override
    public final boolean equals(final Object obj) {
        boolean objectEqual = super.equals(obj);
        if (!objectEqual) {
            return false;
        }

        // TODO (meisner@umich.edu) this may be exessive
        boolean idEqual = ((Job) obj).getJobId() == this.jobId;

        if (!idEqual) {
            return false;
        }

        return true;
    }

    /**
     * Function to hash the object by.
     * @return the hash code of the object
     */
    @Override
    public final int hashCode() {
        return (int) this.jobId;
    }

    /**
     * Set the even that finishes the job.
     * @param aJobFinishEvent - The even that finishes the job.
     */
    public final void setJobFinishEvent(final JobFinishEvent aJobFinishEvent) {
        this.jobFinishEvent = aJobFinishEvent;
    }

    /**
     * Gets the even representing when the job finishes.
     * May be null initially if it hasn't been set yet.
     * @return the job finish event.
     */
    public final JobFinishEvent getJobFinishEvent() {
        return this.jobFinishEvent;
    }

    /**
     * Sets the last time the job was resumed.
     * (Set this when the job is resumed)
     * @param time - the time the job is resumed.
     */
    public final void setLastResumeTime(final double time) {
        this.lastResumeTime = time;
    }

    /**
     * Gets that last time job was resumed.
     * @return the last resume time
     */
    public final double getLastResumeTime() {
        return this.lastResumeTime;
    }

}
