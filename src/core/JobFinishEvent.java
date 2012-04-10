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

package core;

import stat.Statistic;
import datacenter.Server;

/**
 * Represents a job finishing on a server.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class JobFinishEvent extends JobEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The speed at which the job finish time was calculated.
     */
    private double finishingSpeed;

    // TODO (meisner@umich.edu) Figure out exactly how this works
    /**
     * ...
     */
    private double finishTimeSet;

    /**
     * The server on which the job finished.
     */
    private Server server;

    // TODO (meisner@umich.edu) Figure out exactly how finishTimeSet works

    /**
     * Creates a new JobFinishEvent.
     *
     * @param time - the time the job finishes
     * @param experiment - the experiment the event is in
     * @param job - the finishing job
     * @param aServer - the server the job finished on
     * @param theFinishTimeSet - double check this
     * @param theFinishSpeed - the normalized speed at which the job finishes
     */
    public JobFinishEvent(final double time,
                          final Experiment experiment,
                          final Job job,
                          final Server aServer,
                          final double theFinishTimeSet,
                          final double theFinishSpeed) {
        super(time, experiment, job);
        this.server = aServer;
        job.setJobFinishEvent(this);
        this.finishTimeSet = theFinishTimeSet;
        this.finishingSpeed = theFinishSpeed;
    }

    // TODO (meisner@umich.edu) Figure out exactly how this works
    /**
     * ...
     * @return ...
     */
    public double getFinishTimeSet() {
        return this.finishTimeSet;
    }

    /**
     * Sets the normalized speed at which the job finishes.
     * 1.0  is no change in speed. 2.0 is twice as fast etc...
     * @param theFinishSpeed - the normalized speed at which the job finishes.
     */
    public void setFinishSpeed(final double theFinishSpeed) {
        this.finishingSpeed = theFinishSpeed;
    }

    /**
     * Get the speed at which the job finishes.
     * @return the finish speed
     */
    public double getFinishSpeed() {
        return this.finishingSpeed;
    }

    @Override
    public void process() {
        this.getJob().markFinish(this.getTime());

        this.server.removeJob(this.getTime(), this.getJob());

        double sojournTime = this.getJob().getFinishTime()
                                - this.getJob().getArrivalTime();
        Statistic sojournStat = this.getExperiment().getStats().getStat(
                                    Constants.StatName.SOJOURN_TIME);
        sojournStat.addSample(sojournTime);


        double waitTime = this.getJob().getStartTime()
                              - this.getJob().getArrivalTime();
        Statistic waitStat = this.getExperiment().getStats().getStat(
                                                Constants.StatName.WAIT_TIME);
        waitStat.addSample(waitTime);

        if (sojournTime < 0) {
            System.out.println("Job " + this.getJob().getJobId()
                               + " Finish time "
                               + this.getJob().getFinishTime()
                               + " arrival time "
                               + this.getJob().getArrivalTime());
            Sim.fatalError("JobFinishEvent.java:"
                           + " This should never happen sojournTime = "
                           + sojournTime);
        }

        if (waitTime < 0) {
            Sim.fatalError("JobFinishEvent.java:"
                           + " This should never happen waitTime = "
                           + waitTime);
        }
    }

}
