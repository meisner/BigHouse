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
package test.stat;

import junit.framework.TestCase;

import org.junit.Test;

import stat.Sequence;

/**
 * Tests for the {@link Sequence} class.
 *
 * @author meisner@umich.edu
 */
public class SequenceTest extends TestCase {

    // TODO fix magic numbers
    /**
     * Tests {@link Sequence#getValues()}.
     */
    @Test
    public void testGetValues() {
        Sequence sequence = new Sequence();

        for (int i = 0; i < 10; i++) {
            sequence.insert(i);
        }

        double[] values = sequence.getValues();

        for (int i = 0; i < 10; i++) {
            assertEquals(i, values[i], .001);
        }
    }

    // TODO fix magic numbers
    /**
     * Tests {@link Sequence#chiSquaredQuantile(double, int)}.
     */
    @Test
    public void testChiSquaredQuantile() {
        assertEquals(12.592, Sequence.chiSquaredQuantile(.95, 6), .001);
        assertEquals(9.488, Sequence.chiSquaredQuantile(.95, 4), .001);
        assertEquals(37.653, Sequence.chiSquaredQuantile(.95, 25), .001);
        assertEquals(6.346, Sequence.chiSquaredQuantile(.5, 7), .001);
    }

    /**
     * Tests {@link Sequence#getRunCounts(double[], int)}.
     */
    @Test
    public void testGetRunCounts() {
        Sequence sequence = new Sequence();

        double[] insertValues = {1, 2, 3, 4, 5, 6, 7, 8, 9, 5, 3, 2, 1, 5, 11,
                12, 3, 13, 14, 3, 100, 99, 98, 97, 96, 95, 94, 93, 92, 101};

        for (int i = 0; i < insertValues.length; i++) {
            sequence.insert(insertValues[i]);
        }

        int[] runCounts = Sequence.getRunCounts(sequence.getValues(), 6);

        assertEquals(2, runCounts[0]);
        assertEquals(1, runCounts[1]);
        assertEquals(2, runCounts[5]);
    }

    /**
     * Tests {@link Sequence#calculateLagSpacing(int, int, double)}.
     */
    @Test
    public void testCalculateLagSpacing() {
        Sequence sequence = new Sequence();

        double[] insertValues = { 1, 1, 1, 1 };
        for (int i = 0; i < insertValues.length; i++) {
            sequence.insert(insertValues[i]);
        }
    }

    /**
     * Tests {@link Sequence#isIndependentByRunsTest(int[], double)}.
     */
    @Test
    public void testIsIndependentByRunsTest() {
        int[] runCounts = {500, 333, 125};
        int maxRun = 3;
        double confidence = .95;
        assertEquals(true,
                Sequence.isIndependentByRunsTest(runCounts, confidence));
        int[] runCounts2 = {100, 100, 800};
        assertEquals(false,
                Sequence.isIndependentByRunsTest(runCounts2, confidence));
    }

    /**
     * Tests {@link Sequence#getSpacedSequence(double[], int)}.
     */
    @Test
    public void testGetSpacedSequence() {
        double[] seq = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        double[] spaced = Sequence.getSpacedSequence(seq, 2);
        assertEquals(5, spaced.length);
        assertEquals(1, spaced[0], .001);
        assertEquals(3, spaced[1], .001);
        assertEquals(5, spaced[2], .001);
        assertEquals(7, spaced[3], .001);
        assertEquals(9, spaced[4], .001);

        spaced = Sequence.getSpacedSequence(seq, 3);
        assertEquals(3, spaced.length);
        assertEquals(1, spaced[0], .001);
        assertEquals(4, spaced[1], .001);
        assertEquals(7, spaced[2], .001);
    }

}
