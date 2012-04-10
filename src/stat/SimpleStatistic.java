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
package stat;

import java.io.Serializable;

/**
 * A simple statistic tracks the count, average,
 * standard deviation, min and max of a statistic.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class SimpleStatistic implements Serializable {

    /** The serialization id. */
    private static final long serialVersionUID = 1L;

    /** The count of samples. */
    private long s0;

    /** The cumulative value of samples. */
    private double s1;

    /** The cumulative value of squares of samples.*/
    private double s2;

    /** The min value observed. */
    private double minValue;

    /** The max value observed. */
    private double maxValue;

    /**
     * Creates a new, empty statistic.
     */
    public SimpleStatistic() {
        this(0, 0, 0, Double.MAX_VALUE, Double.MIN_VALUE);
    }

    /**
     * Creates a new SimpleStatistic.
     *
     * @param theS0 - the s0 value
     * @param theS1 - the s1 value
     * @param theS2 - the s2 value
     * @param theMinValue - the min value
     * @param theMaxValue - the max value
     */
    public SimpleStatistic(final long theS0,
                           final double theS1,
                           final double theS2,
                           final double theMinValue,
                           final double theMaxValue) {
        this.s0 = theS0;
        this.s1 = theS1;
        this.s2 = theS2;
        this.minValue = theMinValue;
        this.maxValue = theMaxValue;
    }

    /**
     * Combines a SimpleStatistic with this one.
     *
     * @param stat - the SimpleStatistic to combine with this one
     * @return the combined SimpleStatistic.
     */
    public SimpleStatistic combineSimpleStatistics(final SimpleStatistic stat) {
        long combinedS0 = this.s0 + stat.s0;
        double combinedS1 = this.s1 + stat.s1;
        double combinedS2 = this.s2 + stat.s2;
        double combinedMinValue = Math.min(this.minValue, stat.minValue);
        double combinedMaxValue = Math.max(this.maxValue, stat.maxValue);
        SimpleStatistic combinedStat = new SimpleStatistic(combinedS0,
                                                           combinedS1,
                                                           combinedS2,
                                                           combinedMinValue,
                                                           combinedMaxValue);

        return combinedStat;
    }

    /**
     * Add a sample to the simple statistic.
     *
     * @param value - the value of the sample
     */
    public void addSample(final double value) {
        this.s0 += 1;
        this.s1 += value;
        this.s2 += value * value;
        this.minValue = Math.min(this.minValue, value);
        this.maxValue = Math.max(this.maxValue, value);
    }

    /**
     * Get the number of samples.
     *
     * @return the number of samples
     */
    public long getCount() {
        return s0;
    }

    /**
     * Get the minimum observed value.
     *
     * @return the minimum observed value
     */
    public double getMinValue() {
        return minValue;
    }

    /**
     * Get the maximum observed value.
     *
     * @return the maximum observed value
     */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * Get the average of the values.
     * @return the average of the values.
     */
    public double getAverage() {
        return s1 / s0;
    }

    /**
     * Gets the standard deviation.
     * See: http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
     * @return the standard deviation
     */
    public double getStdDev() {
        return Math.sqrt((s0 * s2 - s1 * s1) / (s0 * (s0 - 1)));
    }

    /**
     * Gets the total accumulation of values.
     * @return the total accumulation of values
     */
    public double getTotalAccumulation() {
        return this.s1;
    }

}
