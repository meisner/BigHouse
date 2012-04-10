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
import java.util.Iterator;
import java.util.Vector;

import math.HelperFunctions;

import core.Sim;

// From ssj
import umontreal.iro.lecuyer.probdist.ChiSquareDist;

/**
 * Represents a sequence of values. Can be used to find independent
 * sequences within the original sequences.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class Sequence implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /** The sequence of numbers. */
    private Vector<Double> sequence;

    /** A simple statistic of the sequence. */
    private SimpleStatistic simpleStat;

    /**
     * Creates a new, empty sequence.
     */
    public Sequence() {
        this.sequence = new Vector<Double>();
        this.simpleStat = new SimpleStatistic();
    }

    /**
     * Insert a value into the end of the sequence.
     * @param value - the value
     */
    public void insert(final double value) {
        this.sequence.add(value);
        this.simpleStat.addSample(value);
    }

    /**
     * Get the values of the sequence.
     *
     * @return an array of the values
     */
    public double[] getValues() {
        double[] values = new double[this.sequence.size()];
        Iterator<Double> iter = this.sequence.iterator();
        int i = 0;
        while (iter.hasNext()) {
            values[i] = iter.next().doubleValue();
            i++;
        }

        return values;
    }

    // TODO Document the library this depends on
    /**
     * Get a quantile x value from the chi squared distribution.
     *
     * @param quantile - the quantile requested
     * @param degreesFreedom - the number of degrees freedom parameter
     * for the distribution
     * @return the x value of the quantile
     */
    public static double chiSquaredQuantile(final double quantile,
                                            final int degreesFreedom) {
        return ChiSquareDist.inverseF(degreesFreedom, quantile);
    }

    /**
     * The state of a runs test.
     */
    private enum RunState {
        /**
         * Have only seen one value in a run.
         * We dont know if it's increasing or decreasing.
         *  */
        FIRST,

        /**
         * Have seen two values in a run.
         * No can say if it's increasing or decreasing.
         */
        SECOND,

        /** The run is increasing. */
        UP,

        /** The run is decreasing.*/
        DOWN,

        /** Skipping a value between runs. */
        SKIP
    }

    /**
     * Gets the run counts for a sequence of values.
     * (How many value in each run).
     * A run is a sequence of monotonically increasing or decreasing values.
     * We use Knuth's version of the runs test.
     * The sample after a run is discarded.
     *
     * Also see: http://en.wikipedia.org/wiki/Wald%E2%80%93Wolfowitz_runs_test
     *
     * @param values - the values in the sequence
     * @param maxRun - the maximum allowed run
     * @return the run counts
     */
    public static int[] getRunCounts(final double[] values, final int maxRun) {

        int[] runCounts = new int[maxRun];

        double lastValue = 0;
        int runLength = 0;

        RunState state = RunState.FIRST;

        for (int i = 0; i < values.length; i++) {

            double currentValue = values[i];
            switch (state) {

            case SKIP:
                state = RunState.FIRST;
                break;

            case FIRST:
                state = RunState.SECOND;
                break;

            case SECOND:

                if (currentValue > lastValue) {
                    state = RunState.UP;
                    runLength = 1;
                } else if (currentValue < lastValue) {
                    state = RunState.DOWN;
                    runLength = 1;
                } else {
                    state = RunState.SECOND;
                }

                break;

            case UP:

                if (currentValue > lastValue) {
                    runLength++;
                } else {
                    if (runLength > maxRun) {
                        runCounts[maxRun - 1] += 1;
                    } else {
                        runCounts[runLength - 1] += 1;
                    }
                    state = RunState.FIRST;
                    runLength = 0;
                }

                break;

            case DOWN:

                if (currentValue < lastValue) {
                    runLength++;
                } else {

                    if (runLength > maxRun) {
                        runCounts[maxRun - 1] += 1;
                    } else {
                        runCounts[runLength - 1] += 1;
                    }
                    state = RunState.FIRST;
                    runLength = 0;
                }
                break;

             default:
                 Sim.fatalError("Unknown case");
            }

            lastValue = currentValue;
        }

        return runCounts;
    }

    //TODO add a reference to the runs test
    /**
     * Determines if a a sequence of run counts indicates a sequence is
     * independent by the runs test. See the runs test.
     *
     * @param runCounts - an array of run counts
     * @param confidence - the confidence to use in the test of independence
     * @return if the sequence was independent
     */
    public static boolean isIndependentByRunsTest(final int[] runCounts,
                                                  final double confidence) {
        double totalCount = 0;
        double[] runProb = new double[runCounts.length];
        for (int i = 0; i < runCounts.length; i++) {
            totalCount += runCounts[i];
            int runLength = i + 1;
            runProb[i] = runLength
                        / ((double) HelperFunctions.factorial(runLength + 1));
        }

        double testStatistic = 0;
        for (int i = 0; i < runCounts.length; i++) {
            double rootNumerator = runCounts[i] - totalCount * runProb[i];
            testStatistic += (rootNumerator * rootNumerator)
                                / (totalCount * runProb[i]);
        }

        double chiSquaredQuantile
            = Sequence.chiSquaredQuantile(confidence, runCounts.length);

        return testStatistic < chiSquaredQuantile;
    }

    /**
     * Takes a sequence and returns a sequence with only every nth element.
     *
     * @param seq - the original sequence
     * @param spacing - the nth element spacing
     * @return the new sequence
     */
    public static double[] getSpacedSequence(final double[] seq,
                                             final int spacing) {
        int nItems = (int) seq.length / spacing;
        double[] newSequence = new double[nItems];
        for (int i = 0; i < nItems; i++) {
            newSequence[i] = seq[i * spacing];
        }

        return newSequence;
    }

    /**
     * Gets the size (number of values) of the sequence.
     *
     * @return the size (number of values) of the sequence
     */
    public int getSize() {
        return this.sequence.size();
    }

    //TODO add a reference to the runs test
    /**
     * Calculate the lag spacing needed to make this sequence appear
     * independent. The lag spacing means that every lag number of samples
     * is actually used.
     *
     * @param maxLagSpacing - the maximum lag spacing value this can return
     * @param maxRun - the maximum run to consider.
     * @param confidence - the confidence used for the test of independence
     * @return the minimum lag spacing required
     */
    public int calculateLagSpacing(final int maxLagSpacing,
                                   final int maxRun,
                                   final double confidence) {
        int spacing = 1;
        double[] values = this.getValues();
        double[] spaced = Sequence.getSpacedSequence(values, spacing);
        int[] runCounts = Sequence.getRunCounts(spaced, maxRun);
        while (!Sequence.isIndependentByRunsTest(runCounts, confidence)) {
            spacing++;
            if (spacing > maxLagSpacing) {
                Sim.fatalError("Needed lag spacing is too good");
            }
            spaced = Sequence.getSpacedSequence(values, spacing);
            runCounts = Sequence.getRunCounts(spaced, maxRun);
        }

        return spacing;
    }

    //TODO Possibly delete these
    public double getMinValue() {
        return this.simpleStat.getMinValue();
    }

    public double getMaxValue() {
        return this.simpleStat.getMaxValue();
    }

}
