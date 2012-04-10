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

import java.util.Iterator;
import java.util.Vector;

import generator.Generator;
import core.Experiment;
import core.Job;

/**
 * A BatchPowerNapServer is like a normal server except that jobs are
 * not directly admitted to the server. Instead they are put in a buffer
 * and released at regular batch intervals.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class BatchPowerNapServer extends PowerNapServer {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /** The buffer in which jobs are put until the batch is released. */
    private Vector<Job> batchBuffer;

    /** The time between releasing batches. */
    private double batchInterval;

    /**
     * Creates a new BatchPowerNapServer.
     *
     * @param sockets - the number of sockets in the server
     * @param coresPerSocket - the number of cores per socket
     * @param experiment - the experiment the server is part of
     * @param arrivalGenerator - the interarrival time generator for the server
     * @param serviceGenerator - the service time generator for the server
     * @param napTransitionTime - the transition time in and out
     * of the nap state
     * @param napPower - the power of the server while in the nap state
     * @param theBatchInterval - the amount of time to wait before releasing
     * a batch of jobs
     */
    public BatchPowerNapServer(final int sockets,
                               final int coresPerSocket,
                               final Experiment experiment,
                               final Generator arrivalGenerator,
                               final Generator serviceGenerator,
                               final double napTransitionTime,
                               final double napPower,
                               final double theBatchInterval) {
        super(sockets,
              coresPerSocket,
              experiment,
              arrivalGenerator,
              serviceGenerator,
              napTransitionTime,
              napPower);
        this.batchBuffer = new Vector<Job>();
        this.batchInterval = theBatchInterval;
        StartBatchEvent startBatchEvent = new StartBatchEvent(theBatchInterval,
                                                             this.experiment,
                                                             this);
        this.experiment.addEvent(startBatchEvent);
    }

    /**
     * Inserts a job into the server. This will put the job into a buffer than
     * directly admitting it.
     *
     * @param time - the time the job is inserted
     * @param job - the job to insert
     */
    @Override
    public void insertJob(final double time, final Job job) {
        this.batchBuffer.add(job);
    }

    /**
     * Starts a batch of jobs. All jobs in the batch buffer will be admitted to
     * the server.
     * @param time - the time the batch is started
     */
    public void startBatch(final double time) {
        Iterator<Job> iter = this.batchBuffer.iterator();
        while (iter.hasNext()) {
            Job job = iter.next();
            super.insertJob(time, job);
        }

        this.batchBuffer.clear();
        double batchTime = time + this.batchInterval;
        StartBatchEvent startBatchEvent = new StartBatchEvent(batchTime,
                this.experiment, this);
        this.experiment.addEvent(startBatchEvent);
    }

    /**
     * Removes a job from the server.
     *
     * @param time - the time the job is removed
     * @param job - the job being removed
     */
    @Override
    public void removeJob(final double time, final Job job) {
        super.removeJob(time, job);
    }

}
