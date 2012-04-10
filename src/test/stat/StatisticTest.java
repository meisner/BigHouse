/**
 Copyright (c) 2011 The Regents of The University of Michigan
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

import generator.ExponentialGenerator;
import generator.MTRandom;
import junit.framework.TestCase;

import org.junit.Test;

import stat.SimpleStatistic;
import stat.Statistic;
import stat.StatisticsCollection;
import core.Constants.StatName;

/**
 * Test the functionality of the {@link Statistic} class.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class StatisticTest extends TestCase {

    /**
     * Tests {@link Statistic#combineStatistics(Statistic)}.
     */
    @Test
    public void testCombineStatistics() {
        // TODO write me
    }

    //TODO fix magic nubmers
    /**
     * Tests if sampling an exponential distribution works.
     */
    @Test
    public void testExponentialSampling() {

        double lambda = .5;
        ExponentialGenerator exp = new ExponentialGenerator(new MTRandom(),
                lambda);
        int nWarmupSamples = 10;
        double meanAccuracy = .05;
        double quantile = .95;
        double quantileAccuracy = .05;

        StatisticsCollection statCollection = new StatisticsCollection();

        Statistic stat = new Statistic(statCollection, StatName.SOJOURN_TIME,
                nWarmupSamples, meanAccuracy, quantile, quantileAccuracy);
        stat.setOtherStatsWarmed(true);
        while (!stat.isConverged()) {
            stat.addSample(exp.next());
        }

        double tolerance = .05;
        double expectedValue = 2;
        double actualValue = stat.getAverage();
        assertEquals(expectedValue, actualValue, tolerance * expectedValue);

        tolerance = .05;
        expectedValue = -Math.log(1 - .95) / lambda;
        actualValue = stat.getQuantile(.95);
        assertEquals(expectedValue, actualValue, tolerance * expectedValue);

        tolerance = .05;
        expectedValue = 1.0 - Math.exp(-lambda * 2);
        actualValue = stat.getCdfValue(2);
        assertEquals(expectedValue, actualValue, tolerance * expectedValue);
    }

}
