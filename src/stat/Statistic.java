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

import core.Constants;
import core.Sim;
import core.Constants.StatName;

/**
 * A statistic (or "statistical probe") tracks a variable in the simulation.
 * It tracks the statistical convergence of a variable and can report
 * when it has converged.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class Statistic implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /** The name of the statistic. */
    private StatName statName;

    /**
     * A flag indicating if other statistics have passed their
     * warmup phase.
     */
    private boolean otherStatsWarmed;

    /** The number of samples this statistic requires for warmup. */
    private int nWarmupSamples;

    /** The required accuracy of the mean estimate. */
    private double requiredMeanAccuracy;

    /** The requested quantile for convergence. */
    private double quantile;

    /** The required accuracy of the quantile estimate. */
    private double requiredQuantileAccuracy;

    /** Steady state samples used for estimates. */
    private long steadyStateSamples;

    /** Samples discarded during warm up. */
    private long discardedWarmupSamples;

    /** Samples discarded during steady state. */
    private long discardedSteadyStateSamples;

    /** Total samples seen by the statistic. */
    private long totalSamples;

    /** The number of samples that will be used for calibration. */
    private int calibrationSamples;

    /** The sequence of variables used for the calibration sequence. */
    private Sequence calibrationSequence;

    /** The maximum allowed lag spacing
     * (samples thrown away between used samples).
     */
    private int maxLagSpacing;

    /**
     * The maximum allowed "run" length, in the runs test.
     * A run is a sequence of a single value (e.g., all ones).
     */
    private int maxRun;

    /**
     * The confidence used for independence test
     * when determining the lag spacing.
     */
    private double lagConfidence;

    /**
     * The lag spacing.
     * The statistic uses only one every lag samples to determine estimates.
     */
    private int lag;

    //TODO Comment
    private boolean justBins;

    /**
     * The underlying histogram used to make quantile estimates.
     */
    private Histogram histogram;

    /** The simple statistic used for simple statistical quantities. */
    private SimpleStatistic simpleStat;

    /** The statistics collection used to track this statistic. */
    private StatisticsCollection statCollection;

    /** The phase this statistic is in. */
    protected Phase phase;

    /** If this statistic was created by combining statistics. */
    private boolean combined;

    /**
     * The possible states the statistic can be in.
     */
    protected enum Phase {
        /** The statistic is in warm up. All samples are discarded. */
        WARMUP,

        /**
         * The statistic is calibrating.
         * Samples are used to determine histogram bins.
         */
        CALIBRATION,

        /**
         * The statistic is in steady state.
         * Samples are used for estimates.
         */
        STEADYSTATE
    }

    /**
     * Creates a new Statistic.
     *
     * @param aStatCollection - the statistic collection
     * that this statistic belongs to.
     * @param aStatName - the name of the statistic
     * @param theNWarmupSamples - the number of warmup
     * samples the statistic needs
     * @param meanAccuracy - the accuracy desired for the mean estimate
     * @param theQuantile - the desired quantile
     * (should be the most difficult quantile to achieve)
     * @param quantileAccuracy - the accuracy desired for the quantile estimate
     * @param aSimpleStat - the simple statistic to copy
     * @param aHistogram - the histogram to copy
     * @param lagSpace - the lag spacing to use
     * @param theGoodSamples - the number of good samples seen
     * @param theTotalSamples - the total number of samples seen
     * @param discardedSamples - the number of samples discarded
     */
    public Statistic(final StatisticsCollection aStatCollection,
                     final StatName aStatName,
                     final int theNWarmupSamples,
                     final double meanAccuracy,
                     final double theQuantile,
                     final double quantileAccuracy,
                     final SimpleStatistic aSimpleStat,
                     final Histogram aHistogram,
                     final int lagSpace,
                     final long theGoodSamples,
                     final long theTotalSamples,
                     final long discardedSamples) {
        this(aStatCollection,
             aStatName,
             0,
             meanAccuracy,
             theQuantile,
             quantileAccuracy);
        this.lag = lagSpace;
        this.simpleStat = aSimpleStat;
        this.histogram = aHistogram;
        // TODO this isn't the right way to do this, fix it
        this.combined = true;
        this.steadyStateSamples = theGoodSamples;
        this.totalSamples = theTotalSamples;
        this.discardedWarmupSamples = discardedSamples;
    }

    /**
     * Creates a new Statistic.
     *
     * @param aStatCollection - the statistic collection
     * that this statistic belongs to.
     * @param theStatName - the name of the statistic
     * @param theNWarmupSamples - the number of warmup
     * samples the statistic needs
     * @param meanAccuracy - the accuracy desired for the mean estimate
     * @param theQuantile - the desired quantile
     * (should be the most difficult quantile to achieve)
     * @param quantileAccuracy - the accuracy desired for the quantile estimate
     * @param xValues - the x values to use for the histogram
     */
    public Statistic(final StatisticsCollection aStatCollection,
                     final StatName theStatName,
                     final int theNWarmupSamples,
                     final double meanAccuracy,
                     final double theQuantile,
                     final double quantileAccuracy,
                     final double[] xValues) {
        this(aStatCollection,
             theStatName,
             theNWarmupSamples,
             meanAccuracy,
             theQuantile,
             quantileAccuracy);
        this.histogram = new Histogram(xValues);
    }

    /**
     * Creates a new Statistic.
     *
     * @param aStatCollection - the statistic collection
     * that this statistic belongs to.
     * @param aStatName - the name of the statistic
     * @param theNWarmupSamples - the number of warmup samples
     * the statistic needs
     * @param meanAccuracy - the accuracy desired for the mean estimate
     * @param theQuantile - the desired quantile
     * (should be the most difficult quantile to achieve)
     * @param quantileAccuracy - the accuracy desired for the quantile estimate
     */
    public Statistic(final StatisticsCollection aStatCollection,
                     final StatName aStatName,
                     final int theNWarmupSamples,
                     final double meanAccuracy,
                     final double theQuantile,
                     final double quantileAccuracy) {
        this.justBins = false;
        this.statCollection = aStatCollection;
        this.combined = false;
        this.statName = aStatName;
        this.otherStatsWarmed = false;
        this.steadyStateSamples = 0;
        this.discardedWarmupSamples = 0;
        this.discardedSteadyStateSamples = 0;
        this.totalSamples = 0;
        this.phase = Phase.WARMUP;
        this.nWarmupSamples = theNWarmupSamples;
        this.requiredMeanAccuracy = meanAccuracy;
        this.quantile = theQuantile;
        this.requiredQuantileAccuracy = quantileAccuracy;
        this.maxLagSpacing = 40;
        this.maxRun = 50;
        this.lagConfidence = .99;
        this.lag = 1;
        this.simpleStat = new SimpleStatistic();
        this.calibrationSequence = new Sequence();
        this.calibrationSamples = 5000;
    }

    //TODO comment
    public void setJustBins(final boolean justBins) {
        this.justBins = justBins;
    }

    /**
     * Adds a samples to the statistc.
     * Handles this sample differently depending on
     * the state of the statics (e.g., if it's warmed up)
     *
     * @param value - the value of the sample
     */
    public void addSample(final double value) {

        if (this.combined) {
            Sim.fatalError("Shouldn't add samples after being combined");
        }

        if (this.phase == Phase.WARMUP) {

            this.discardWarmupSample(value);
            if (this.discardedWarmupSamples == this.nWarmupSamples) {
                this.statCollection.reportWarmed(this);
            }

            // Check if we're warmed
            if (this.discardedWarmupSamples >= this.nWarmupSamples
                    && this.otherStatsWarmed) {
                this.phase = Phase.CALIBRATION;
                System.out.println(this.statName + " entered calibration");

            }

        } else if (this.phase == Phase.CALIBRATION) {

            this.calibrationSequence.insert(value);

            if (this.calibrationSequence.getSize() > 100 && this.justBins) {

                double minValue = this.calibrationSequence.getMinValue();
                double maxValue = this.calibrationSequence.getMaxValue();
                System.out.println("Creating histogram with min " + minValue
                        + " maxValue " + maxValue);
             // let's delay creating the histogram
                this.histogram = new Histogram(10000,
                                               minValue / 2,
                                               maxValue * 2);
                this.phase = Phase.STEADYSTATE;
                System.out.println(this.statName
                        + " entered steady state and only took 100 samples");

            } else if (this.calibrationSequence.getSize()
                       >= this.calibrationSamples) {

                // Calibration is over
                this.lag
                    = this.calibrationSequence.calculateLagSpacing(
                                                    maxLagSpacing,
                                                    maxRun,
                                                    lagConfidence);

                if (this.lag < 0) {
                    Sim.fatalError("Couldn't find a valid lag spacing for "
                            + this.statName);
                }

                double minValue = this.calibrationSequence.getMinValue();
                double maxValue = this.calibrationSequence.getMaxValue();
                System.out.println("Creating histogram with min " + minValue
                                   + " maxValue " + maxValue);
                if (this.histogram == null) {
                    /** let's delay creating the histogram */
                    this.histogram = new Histogram(10000, minValue, maxValue);
                }

                this.phase = Phase.STEADYSTATE;
                System.out.println(this.statName
                        + " entered steady state, lag spacing of " + this.lag);
            }

        } else {

            if ((this.totalSamples % this.lag) == 0) {
                this.keepSample(value);
            } else {
                this.discardSteadyStateSample(value);
            }

        }

        this.totalSamples++;

        // Assert sample balance
        if (this.totalSamples
                != (this.steadyStateSamples
                        + this.discardedWarmupSamples
                        + this.discardedSteadyStateSamples
                        + this.calibrationSequence.getSize())) {
            Sim.fatalError("Total samples " + this.totalSamples + ""
                           + "!= good samples " + this.steadyStateSamples
                           + " calibration samples "
                           + this.calibrationSequence.getSize()
                           + " discarded steady state samples "
                           + this.discardedSteadyStateSamples
                           + " discarded warmup samples "
                           + this.discardedWarmupSamples);
        }

    }

    /**
     * Combines another statistic with this one.
     *
     * @param stat - the statistic to combine with this one.
     * @return the combined statistic
     */
    public CombinedStatistic combineStatistics(final Statistic stat) {

        if (!this.statName.equals(stat.getStatName())) {
            Sim.fatalError("Cannot combined statistics"
                           + " unless they're the same kind");
        }

        SimpleStatistic combinedSimpleStat
            = this.simpleStat.combineSimpleStatistics(stat.simpleStat);
        Histogram combinedHistogram
            = this.histogram.combineHistogram(stat.histogram);

        long combinedGoodSamples = this.steadyStateSamples
                                   + stat.steadyStateSamples;
        long combinedTotalSamples = this.totalSamples + stat.totalSamples;
        long combinedDiscardedSamples
            = Math.min(this.discardedWarmupSamples,
                       stat.discardedWarmupSamples);
        System.out.println("Going to set mean accuracy to "
                            + this.requiredMeanAccuracy);
        CombinedStatistic combinedStatistic
            = new CombinedStatistic(this.statCollection,
                                    this.statName,
                                    this.nWarmupSamples,
                                    this.requiredMeanAccuracy,
                                    this.quantile,
                                    this.requiredQuantileAccuracy,
                                    combinedSimpleStat,
                                    combinedHistogram,
                                    this.lag,
                                    combinedGoodSamples,
                                    combinedTotalSamples,
                                    combinedDiscardedSamples);

        return combinedStatistic;
    }

    /**
     * Get the number of good samples (used for estimates).
     *
     * @return the number of good samples (used for estimates)
     */
    public long getGoodSamples() {
        return this.steadyStateSamples;
    }

    /**
     * Get the name of the statistic.
     *
     * @return the name of the statistic
     */
    public StatName getStatName() {
        return this.statName;
    }

    /**
     * Get the total number of samples (including non-steady-state)
     * the statistic has seen.
     *
     * @return the total number of samples (including non-steady-state)
     * the statistic has seen
     */
    public long getTotalSamples() {
        return this.totalSamples;
    }

    /**
     * Provides a sample to the statistic to make its estimates.
     *
     * @param value - the value of sample
     */
    private void keepSample(final double value) {
        this.simpleStat.addSample(value);
        this.histogram.addSample(value);
        this.steadyStateSamples++;
    }

    /**
     * Accepts a sample at steady state but discards it.
     *
     * @param value - a sample value, which is discarded
     */
    private void discardSteadyStateSample(final double value) {
        this.discardedSteadyStateSamples++;
    }

    /**
     * Accepts a sample but discards it as warmup.
     *
     * @param value - a sample value, which is discarded
     */
    private void discardWarmupSample(final double value) {
        this.discardedWarmupSamples++;
    }

    /**
     * Sets a flag in the Statistic that lets it know other statistics
     * have passed the warm up phase.
     *
     * @param warmed - the value of the flag
     */
    public void setOtherStatsWarmed(final boolean warmed) {
        this.otherStatsWarmed = warmed;
    }

    /**
     * Gets the standard deviation estimate.
     *
     * @return the standard deviation estimate
     */
    public double getStdDev() {
        return this.simpleStat.getStdDev();
    }

    /**
     * Gets the mean estimate.
     *
     * @return the mean estimate
     */
    public double getAverage() {
        return this.simpleStat.getAverage();
    }

    /**
     * Gets the accuracy of the quantile estimate.
     *
     * @return the accuracy of the quantile estimate
     */
    public double getQuantileAccuracy() {
        double std = this.quantile * this.quantile;
        double z = Constants.Z_95_CONFIDENCE;
        double nRoot = Math.sqrt(this.steadyStateSamples);
        double range = z * std / nRoot;
        double accuracy = range / this.getAverage();

        return accuracy;
    }

    /**
     * Gets the accuracy of the mean estimate.
     *
     * @return the accuracy of the mean estimate
     */
    public double getMeanAccuracy() {
        double std = this.getStdDev();
        double z = Constants.Z_95_CONFIDENCE;
        double nRoot = Math.sqrt(this.steadyStateSamples);
        double range = z * std / nRoot;
        double accuracy = range / this.getAverage();

        return accuracy;
    }

    /**
     * Checks if the mean estimate of the statistic has converged.
     *
     * @return if the mean estimate of the statistic has converged
     */
    public boolean isMeanCoverged() {
        return this.getMeanAccuracy() < this.requiredMeanAccuracy;
    }

    /**
     * Checks if the quantile estimate of the statistic has converged.
     *
     * @return if the quantile estimate of the statistic has converged
     */
    public boolean isQuantileConverged() {
        return this.getQuantileAccuracy() < this.requiredQuantileAccuracy;
    }

    /**
     * Checks if the statistic is in steady state (mean and quantile).
     *
     * @return if the statistic is in steady state
     */
    public boolean isSteadyState() {
        return this.phase == Phase.STEADYSTATE;
    }

    /**
     * Check if the statistic is converged.
     *
     * @return if the statistic is converged
     */
    public boolean isConverged() {

        if (this.steadyStateSamples < Constants.MINIMUM_CONVERGE_SAMPLES) {
            return false;
        }

        return this.isMeanCoverged()
            && isQuantileConverged()
            && isSteadyState();
    }

    /**
     * Get the cdf value of the histogram for a given x value.
     *
     * @param xValue - the x value of the histogram
     * @return the cdf value
     */
    public double getCdfValue(final double xValue) {
        return this.histogram.getCdfValue(xValue);
    }

    /**
     * Get the x value of a quantile.
     *
     * @param theQuantile - the quantile requested
     * @return the x value of a quantile
     */
    public double getQuantile(final double theQuantile) {

        if (this.histogram == null) {
            return 0.0d;
        }

        return this.histogram.getQuantile(theQuantile);
    }

    /**
     * Get the value of the quantile needed for convergence.
     *
     * @return the value of the quantile needed for convergence
     */
    public double getQuantileSetting() {
        return quantile;
    }

    /**
     * Prints the cdf of the underlying histogram.
     */
    public void printCdf() {
        this.histogram.printCdf();
    }

    /**
     * Prints the underlying histogram.
     */
    public void printHistogram() {
        this.histogram.printHistogram();
    }

    /**
     * Set the values of the underlying histogram.
     *
     * @param xValues - the values of the underlying histogram
     */
    public void setHistogramXValues(final double[] xValues) {
        this.histogram = new Histogram(xValues);
    }

    /**
     * Get the values of the underlying histogram.
     *
     * @return the values of the underlying histogram
     */
    public double[] getHistogramXValues() {

        if (this.histogram == null) {
            return null;
        }

        return this.histogram.getXValues();
    }

    /**
     * Get the quantile this statistic is trying to reach
     * convergence on.
     *
     * @return the quantile this statistic is trying to reach
     * convergence on.
     */
    public double getTargetQuantile() {
        return this.quantile;
    }

    /**
     * Print information about the statistic.
     */
    public void printStatInfo() {
        String out = "name: " + this.statName + ", averageValue: "
                + getAverage() + ", averageAccuracy: " + this.getMeanAccuracy()
                + ", quatileTarget: " + this.getTargetQuantile()
                + ", quantileValue: "
                + this.getQuantile(this.getTargetQuantile())
                + ", quantileAccuracy: " + this.getQuantileAccuracy()
                + ", goodSamples: " + this.steadyStateSamples
                + ", warmupSamples: " + this.discardedWarmupSamples
                + ", calibrationSamples: " + this.calibrationSamples
                + ", stdDev: " + this.getStdDev()
                + ", lag: " + this.lag;
        System.out.println(out);
    }

}
