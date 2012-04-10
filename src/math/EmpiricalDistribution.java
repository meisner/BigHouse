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
package math;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import core.Sim;

/**
 * An empirical distribution represents a distribution as a histogram
 * with measured values.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class EmpiricalDistribution implements Serializable {

    /**
     * A helper inner class to store XY pairs.
     */
    private static class XYPair {
        /** The x value. */
        public double x;

        /** The y value. */
        public double y;
    }

    /** The serialization id. */
    private static final long serialVersionUID = 1L;

    /** The x values. */
    private double[] xs;

    /** The y values (CDF).
     * Store the distribution as a CDF because it's easy to get the PDF value
     * from it.
     */
    private double[] ys;

    /** The average value. */
    private double mean;

    /**
     * Creates a new EmpiricalDistribution.
     *
     * @param theXs - the x values of the distribution
     * @param theYs - the y values of the distribution (CDF values)
     */
    public EmpiricalDistribution(final double[] theXs, final double[] theYs) {
        if (theXs.length != theYs.length) {
            Sim.fatalError("X and Y vector must be the same"
                    + " length in a distribution");
        }
        double lastY = 0.0d;
        double expectedValue = 0.0d;
        for (int i = 0; i < theYs.length; i++) {

            if (lastY > theYs[i]) {
                Sim.fatalError("Y (CDF) values must be"
                        + " monotonically increasing");
            }

            if (theYs[i] > 1.0) {
                Sim.fatalError("Probablity can't be greater than 1.0 ys["
                        + i + "] = " + theYs[i]);
            }

            double diff = theYs[i] - lastY;
            expectedValue += theXs[i] * diff;
            lastY = theYs[i];
        }
        this.xs = theXs;
        this.ys = theYs;
        this.mean = expectedValue;
    }

    /**
     * Get the mean (average) value.
     *
     * @return the mean value
     */
    public double getMean() {
        return this.mean;
    }

    /**
     * Get the x values of the distribution.
     *
     * @return the x values of the distribution
     */
    public double[] getXs() {
        return this.xs;
    }

    /**
     * Get the y values of the distribution.
     *
     * @return the y values of the distribution
     */
    public double[] getYs() {
        return this.ys;
    }

    /**
     * Loads an empirical from a file.
     * Files are in the format: "xValue cdfValue"
     * One entry per line.
     * The CDF should have 0 0 at the beginning and X 1.0 at the end.
     *
     * @param fileName - the name of the file
     * @return the empirical distribution represented by the file
     */
    public static EmpiricalDistribution loadDistribution(
                                            final String fileName) {
        return EmpiricalDistribution.loadDistribution(fileName, 1.0);
    }

    /**
     * Loads an empirical from a file.
     * Files are in the format: "xValue cdfValue"
     * One entry per line.
     * The CDF should have 0 0 at the beginning and X 1.0 at the end.
     *
     * @param fileName - the name of the file
     * @param scalingFactor - a scaling factor to multiply x values in the
     * distribution by
     * @return the empirical distribution represented by the file
     */
    public static EmpiricalDistribution loadDistribution(
                                            final String fileName,
                                            final double scalingFactor) {

        // Use a vector since we don't know how many points there are
        Vector<XYPair> xypairs = new Vector<XYPair>();

        try {
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(fileName);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                String[] parts = strLine.split(" ");
                XYPair pair = new XYPair();
                pair.x = Double.valueOf(parts[0]);
                pair.y = Double.valueOf(parts[1]);
                xypairs.add(pair);
            }
            // Close the input stream
            br.close();
            in.close();
            fstream.close();

        } catch (Exception e) {
            // Catch exception if any
            System.err.println("Error: " + e.getMessage());
            System.err.println("File is " + fileName);
            Sim.fatalError("Couldn't load distribution file");
        }
        // Convert to arrays
        int entries = xypairs.size();
        double[] ys = new double[entries];
        double[] xs = new double[entries];
        Iterator<XYPair> xyiter = xypairs.iterator();
        int i = 0;
        while (xyiter.hasNext()) {
            XYPair pair = xyiter.next();
            xs[i] = pair.x * scalingFactor;
            ys[i] = pair.y;
            i++;
        }

        if (xs[0] != 0 || ys[0] != 0) {
            System.out.println("xs[0] = " + xs[0]);
            System.out.println("ys[0] = " + ys[0]);
            Sim.fatalError("The first line of a cdf file needs to be 0 0");
        }
        EmpiricalDistribution distribution = new EmpiricalDistribution(xs, ys);

        return distribution;
    }

    /**
     *  Get the x value of a quantile of the CDF.
     *
     *  @param quantile - the quantile of the distribution
     *  @return the x value corresponding to the quantile
     */
    public double getQuantile(final double quantile) {
        int bin = searchForBin(this.ys, quantile);
        // Note x and y are reversed
        double xValue = this.linearlyInterpolateBin(this.ys,
                                                    this.xs,
                                                    bin,
                                                    quantile);
        return xValue;
    }

    /**
     * Find the CDF value at which the x value occurs.
     * This is an inverse distribution lookup.
     *
     * @param x  - the x value
     * @return the CDF value
     */
    public double getCdfValue(final double x) {
        // Get the bin with the closest value
        int bin = searchForBin(this.xs, x);
        double y = linearlyInterpolateBin(this.xs, this.ys, bin, x);

        return y;
    }

    /**
     * Performs a linear interpolation to find the y value
     * that corresponds with an x value.
     *
     * @param yValues - the y values to interpolate form
     * @param xValues - the x values to interpolate form
     * @param bin - The histogram bin at which the x value resides
     * @param x - the x value
     * @return the y value from the interpolation
     */
    private double linearlyInterpolateBin(final double[] xValues,
                                          final double[] yValues,
                                          final int bin,
                                          final double x) {
        double y = 0.0d;
        // Handle corner case
        if (bin == xs.length - 1) {

            y = yValues[bin];

        } else {

            double x0 = xValues[bin];
            double y0 = yValues[bin];
            double x1 = xValues[bin + 1];
            double y1 = yValues[bin + 1];
            y = (y1 - y0) / (x1 - x0) * (x - x0) + y0;

        }

        return y;
    }

    /**
     * Returns the biggest bin which is smaller than the value.
     *
     * @param valueArray - an array of histogram bins
     * @param value - the value to search for
     * @return the position of the bin which represent the largest
     * values smaller than the value
     */
    public static int searchForBin(final double[] valueArray,
                                   final double value) {
        int bin = Arrays.binarySearch(valueArray, value);

        if (bin < 0) {
            bin = -bin - 2;
        }

        if (bin < 0) {
            bin = 0;
        }

        if (bin > valueArray.length - 1) {
            bin = valueArray.length - 1;
        }

        return bin;
    }

    /**
     * Creates an empirical Distribution based on an exponential distribution.
     *
     * @param lambda - the lambda paramter of the exponential distribution
     * @param bins - the number of bins to use for the distribution
     * @param xMin - the minimum x value
     * @param xMax - the maximum x value
     * @return the empirical distribution
     */
    public static EmpiricalDistribution getExponentialDistribution(
                                                final double lambda,
                                                final int bins,
                                                final double xMin,
                                                final double xMax) {
        double stepSize = (xMax - xMin) / (bins - 2);
        double[] xs = new double[bins];
        double[] ys = new double[bins];

        for (int i = 0; i < bins - 1; i++) {
            double x = stepSize * i;
            double cdfValue = (1 - Math.exp(-lambda * x));
            xs[i] = x;
            ys[i] = cdfValue;
        }

        xs[bins - 1] = stepSize * (bins - 1);
        ys[bins - 1] = (1 - Math.exp(-lambda * xs[bins - 1]));

        EmpiricalDistribution exponentialDistribution
            = new EmpiricalDistribution(xs, ys);

        return exponentialDistribution;
    }

}
