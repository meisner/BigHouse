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
import java.util.Arrays;

import core.Sim;

/**
 * A histogram which puts samples into bins.
 * 
 * @author David Meisner (meisner@umich.edu)
 */
public final class Histogram implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /** The x values of the bins of the histogram. */
    private double[] xValues;

    /** The y values (the counts) of the bins of the histogram. */
    private double[] yValues;

    /**
     * The cdf values of the bins of the histogram.
     * Is null until {@link #createCdf()} is called.
     */
    private double[] cdf;

    /**
     * Creates a new histogram.
     *
     * @param nBins - the number of bins in the histogram
     * @param minValue - the minimum value of the histogram
     * @param maxValue - the maximum value of the histogram
     */
    public Histogram(final int nBins,
                     final double minValue,
                     final double maxValue) {
        this.xValues = new double[nBins];
        this.yValues = new double[nBins];
        double deltaX = (maxValue - minValue) / nBins;
        for (int i = 0; i < nBins; i++) {
            this.xValues[i] = (i + 1) * deltaX;
            this.yValues[i] = 0.0d;
        }
        this.cdf = null;
    }

    /**
     * Creates a new histogram.
     *
     * @param theXValues - the x values to use (y values are 0)
     */
    public Histogram(final double[] theXValues) {
        this.xValues = theXValues.clone();
        this.yValues = new double[theXValues.length];
        this.cdf = null;
    }

    /**
     * Creates a new histogram.
     *
     * @param theXValues - the x values to use
     * @param theYValues - the y values to use
     */
    public Histogram(final double[] theXValues, final double[] theYValues) {
        this.xValues = theXValues.clone();
        this.yValues = theYValues.clone();
        this.cdf = null;
    }

    /**
     * Find the bin closest to the searched value.
     *
     * @param values - the array of values to search in
     * @param searchValue - the value to search for
     * @return the closest bin
     */
    private int findClosestBin(final double[] values,
                               final double searchValue) {
        int bin = Arrays.binarySearch(values, searchValue);

        if (bin < 0) {
            bin = -bin - 1;
        }

        return bin;
    }

    /**
     * Evaluates the cdf values for this histogram internally.
     */
    private void createCdf() {
        this.cdf = new double[this.xValues.length];
        double total = 0;
        for (int i = 0; i < this.xValues.length; i++) {
            total += this.yValues[i];
        }

        double runningCdf = 0.0d;
        for (int i = 0; i < this.xValues.length; i++) {
            double pdf = this.yValues[i] / total;
            runningCdf += pdf;
            this.cdf[i] = runningCdf;
        }
    }

    /**
     * Adds a sample to the histogram.
     *
     * @param value - the sample value
     */
    public void addSample(final double value) {
        this.cdf = null;
        int bin = findClosestBin(this.xValues, value);

        if (bin > this.yValues.length - 1) {
            bin = this.yValues.length - 1;
        }

        this.yValues[bin] += 1;
    }

    /**
     * Combine a histogram with this one.
     *
     * @param histogram - the histogram to combine with this one
     * @return the combined histogram
     */
    public Histogram combineHistogram(final Histogram histogram) {
        // Make sure we can combine these histograms
        double[] combinedYs = new double[xValues.length];
        for (int i = 0; i < this.xValues.length; i++) {
            if (Double.compare(this.xValues[i], histogram.xValues[i]) != 0) {
                Sim.fatalError("Cannot combine histograms"
                                + "with different x values");
            }
            combinedYs[i] = this.yValues[i] + histogram.yValues[i];
        }

        Histogram combinedHistogram = new Histogram(this.xValues, combinedYs);

        return combinedHistogram;
    }

    /**
     * Perform a linear interpolation.
     *
     * @param bottomX - the smaller x value
     * @param topX - the bigger x value
     * @param bottomY - the y value corresponding to the smaller x value
     * @param topY - the y value corresponding to the larger x value
     * @param xValue - the x value to interpolate with
     * @return the y value from interpolating
     */
    private double interpolate(final double bottomX,
                               final double topX,
                               final double bottomY,
                               final double topY,
                               final double xValue) {
        return bottomY + (topY - bottomY) / (topX - bottomX)
               * (xValue - bottomX);
    }

    /**
     * Get the x value of a quantile form the histogram.
     *
     * @param quantile - the quantile
     * @return the x value of the quantile from the histogram
     */
    public double getQuantile(final double quantile) {

        if (this.cdf == null) {
            this.createCdf();
        }

        int bin = findClosestBin(this.cdf, quantile);
        double topX = this.cdf[bin];
        double topY = this.xValues[bin];
        double bottomX = 0.0d;
        double bottomY = 0.0d;

        if (bin != 0) {
            bottomX = this.cdf[bin - 1];
            bottomY = this.xValues[bin - 1];
        }

        double xValue = interpolate(bottomX, topX, bottomY, topY, quantile);

        return xValue;
    }

    /**
     * Prints the cdf of the histogram to standard out.
     */
    public void printCdf() {

        if (this.cdf == null) {
            System.out.println("CDF is null");
        } else {
            System.out.println("Bin, X, CDF");
            for (int i = 0; i < this.cdf.length; i++) {
                System.out.println(i + ", " + this.xValues[i] + ", "
                                   + this.cdf[i]);
            }
        }

    }

    /**
     * Prints the histogram to standard out.
     */
    public void printHistogram() {
        System.out.println("Bin, X, Count");
        for (int i = 0; i < this.cdf.length; i++) {
            System.out.println(i + ", " + this.xValues[i] + ", "
                               + this.yValues[i]);
        }
    }

    /**
     * Get the y values of the histogram.
     *
     * @return the y values of the histogram
     */
    public double[] getYValues() {
        return this.yValues;
    }

    /**
     * Get the cdf value of the histogram for a given x value.
     *
     * @param xValue - the x value of the histogram
     * @return the cdf value
     */
    public double getCdfValue(final double xValue) {

        if (this.cdf == null) {
            this.createCdf();
        }

        int bin = findClosestBin(this.xValues, xValue);
        double topX = this.xValues[bin];
        double topY = this.cdf[bin];
        double bottomX = 0.0d;
        double bottomY = 0.0d;

        if (bin != 0) {
            bottomX = this.xValues[bin - 1];
            bottomY = this.cdf[bin - 1];
        }

        double cdfValue = interpolate(bottomX, topX, bottomY, topY, xValue);

        return cdfValue;
    }

    /**
     * Get the x values of the histogram.
     * @return the x values of the histogram
     */
    public double[] getXValues() {
        return this.xValues;
    }

}
