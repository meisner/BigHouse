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

import java.io.Serializable;

import core.Constants.StatName;
import core.Constants.TimeWeightedStatName;
import stat.Statistic;
import stat.StatisticsCollection;
import stat.TimeWeightedStatistic;

/**
 * Defines the outputs of an experiment.
 * Effectively a big wrapper around a StatsCollection.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class ExperimentOutput implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The collection of statistics for the simulation.
     */
    private StatisticsCollection statisticsCollection;

    /**
     * Creates a new ExperimentOutput.
     */
    public ExperimentOutput() {
        this.statisticsCollection = new StatisticsCollection();

    }

    /**
     * Adds an output to be observed by the simulation.
     *
     * @param name - The name of the simulation
     * @param meanPrecision - The precision on the mean estimate
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param quantile - The quantile to ensure precision on
     * @param quantilePrecision - the precision for the quantile
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param warmupSamples - The number of warmup samples.
     * There is no de facto way to determine what this value should be.
     */
    public void addOutput(final StatName name,
                          final double meanPrecision,
                          final double quantile,
                          final double quantilePrecision,
                          final int warmupSamples) {
        Statistic stat = new Statistic(statisticsCollection,
                                       name,
                                       warmupSamples,
                                       meanPrecision,
                                       quantile,
                                       quantilePrecision);
        this.statisticsCollection.addStatistic(name, stat);
    }

    /**
     * Adds an output to be observed by the simulation.
     *
     * @param name - The name of the simulation
     * @param meanPrecision - The precision on the mean estimate
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param quantile - The quantile to ensure precision on
     * @param quantilePrecision - the precision for the quantile
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param warmupSamples - The number of warmup samples.
     * There is no de facto way to determine what this value should be.
     * @param xValues - The x values for the histogram of the output
     */
    public void addOutput(final StatName name,
                          final double meanPrecision,
                          final double quantile,
                          final double quantilePrecision,
                          final int warmupSamples,
                          final double[] xValues) {
        Statistic stat = new Statistic(statisticsCollection,
                                       name,
                                       warmupSamples,
                                       meanPrecision,
                                       quantile,
                                       quantilePrecision,
                                       xValues);
        this.statisticsCollection.addStatistic(name, stat);
    }

    /**
     * Adds a time-weigthed output to be observed by the simulation.
     *
     * @param name - The name of the simulation
     * @param meanPrecision - The precision on the mean estimate
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param quantile - The quantile to ensure precision on
     * @param quantilePrecision - the precision for the quantile
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param warmupSamples - The number of warmup samples.
     * There is no de facto way to determine what this value should be.
     * @param window - The window (in seconds) over which to take samples.

     */
    public void addTimeWeightedOutput(final TimeWeightedStatName name,
                                      final double meanPrecision,
                                      final double quantile,
                                      final double quantilePrecision,
                                      final int warmupSamples,
                                      final double window) {
        TimeWeightedStatistic stat
            = new TimeWeightedStatistic(statisticsCollection,
                                        name,
                                        warmupSamples,
                                        meanPrecision,
                                        quantile,
                                        quantilePrecision,
                                        window);
        this.statisticsCollection.addTimeWeightedStatistic(name, stat);
    }

    /**
     * Gets the statistics collection for the output.
     * @return - the statistics collection
     */
    public StatisticsCollection getStats() {
        return this.statisticsCollection;
    }

    /**
     * Get an individual statistic of the output.
     * @param statName - the name of the individual statistic
     * @return the statistic
     */
    public Statistic getStat(final StatName statName) {
        return this.statisticsCollection.getStat(statName);
    }

    /**
     * Get an individual statistic of the output.
     * @param statName - the name of the individual statistic
     * @return the statistic
     */
    public Statistic getTimeWeightedStat(final TimeWeightedStatName statName) {
        return this.statisticsCollection.getTimeWeightedStat(statName);
    }

}
