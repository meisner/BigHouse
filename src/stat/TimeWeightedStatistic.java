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
 */
package stat;

import core.Sim;
import core.Constants.StatName;
import core.Constants.TimeWeightedStatName;

/**
 * A time weighted statistic is used for sequences of values
 * which should not be weighted equally. For example, if the
 * utilization of a machine is sampled at non-uniform time periods,
 * the utilization of each should be weighted by the amount of time
 * the machine was at that utilization.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class TimeWeightedStatistic extends Statistic {

    /** The serialization id. */
    private static final long serialVersionUID = 1L;

    /** The size of the window to weight statistics. */
    private double windowSize;

    //TODO document these
    private double sampleWindowStart;

    private double lastSampleTime;

    private double averageAccum;

    private double oldValue;

    private boolean oldValueSet;

    /** Used for sanity checking. */
    private double accumWeight;

    /** The name of the time weighted statistic. */
    private TimeWeightedStatName name;

    /**
     * Creates a new time weighted statistic.
     *
     * @param statsCollection - the statistic collection this is part of
     * @param theName - the name of the statistic
     * @param nWarmupSamples - the number of warm up samples to discard
     * @param meanPrecision - the required mean precision
     * @param quantile - the quantile desired for estimates
     * @param quantilePrecision - the required quantile precision
     * @param theWindowSize - the window size to weight over
     */
    public TimeWeightedStatistic(final StatisticsCollection statsCollection,
                                 final TimeWeightedStatName theName,
                                 final int nWarmupSamples,
                                 final double meanPrecision,
                                 final double quantile,
                                 final double quantilePrecision,
                                 final double theWindowSize) {
        super(statsCollection,
                null,
                nWarmupSamples,
                meanPrecision,
                quantile,
                quantilePrecision);
        this.name = theName;
        this.sampleWindowStart = 0.0d;
        this.windowSize = theWindowSize;
        this.lastSampleTime = 0.0d;
        this.averageAccum = 0.0d;
        this.accumWeight = 0.0d;
        this.oldValue = 0.0d;
        this.oldValueSet = false;
    }

    /**
     * Add a sample to the time weighted statistic.
     *
     * @param newValue - the value of the sample
     * @param time - the time the sample is added
     */
    public void addSample(final double newValue, final double time) {

        if (!this.oldValueSet) {
            this.oldValue = newValue;
            this.oldValueSet = true;
            return;
        }

        double value = this.oldValue;
        this.oldValue = newValue;
        double currentPeriodLength = time - this.sampleWindowStart;

        if (currentPeriodLength > this.windowSize) {

            // There are three pieces
            // The part that falls in the first window
            // |
            // -------------------------------------------|---------------------------|----------------------|
            // sample_window_start ^last_sample_time ^time
            double weight = (this.windowSize
                            - (this.lastSampleTime - this.sampleWindowStart));
            if (weight / this.windowSize > 1.0) {
                System.out.println("weight " + weight + " window_size "
                        + this.windowSize + " last_sample_time "
                        + this.lastSampleTime + " sample_window_start "
                        + this.sampleWindowStart);
                Sim.fatalError("This ratio shouldn't be > 1");
            }

            if (Math.abs(weight) < 1e-8) {
                weight = 0;
            }
            this.accumWeight += weight;
            double firstWindowPart = weight * value;
            averageAccum += firstWindowPart;

            double overallAverage = averageAccum / this.windowSize;

            if (overallAverage < 0) {
                System.out.println("average_accum " + averageAccum
                        + ",  weight " + weight + ", last_sample_time "
                        + lastSampleTime + ", sample_window_start "
                        + sampleWindowStart + ", value " + value
                        + ", currentPeriodLength " + currentPeriodLength
                        + ", time " + time);
                Sim.fatalError("overallAverage is < 0: " + overallAverage);
            }
            super.addSample(overallAverage);

            double remainder = currentPeriodLength - this.windowSize;

            // A window that is just one value
            int wholePeriods = (int) Math.floor(remainder / this.windowSize);

            for (int i = 0; i < wholePeriods; i++) {
                super.addSample(value);
            }

            // A new window with just a portion filled
            remainder = remainder - this.windowSize * (wholePeriods);
            this.averageAccum = remainder * value;
            this.accumWeight = remainder;

            this.sampleWindowStart = this.windowSize * (wholePeriods + 1)
                    + this.sampleWindowStart;
            this.lastSampleTime = time;

        } else {

            double timeWeight = time - this.lastSampleTime;
            this.averageAccum += timeWeight * value;
            this.lastSampleTime = time;

        }

    }

    /**
     * Sets the window (in seconds) over which samples are weighted.
     *
     * @param size - the size of the window (in seconds)
     */
    public void setWindowSize(final double size) {
        this.windowSize = size;
    }

}
