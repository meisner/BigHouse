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

import core.Constants.StatName;

/**
 * Represents the merge of two {@link Statistic}s.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class CombinedStatistic extends Statistic {

    /** The serialization id. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CombinedStatistic.
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
     * @param combinedGoodSamples - the number of good samples seen
     * @param combinedTotalSamples - the total number of samples seen
     * @param combinedDiscardedSamples - the number of samples discarded
     */
    public CombinedStatistic(final StatisticsCollection aStatCollection,
                             final StatName aStatName,
                             final int theNWarmupSamples,
                             final double meanAccuracy,
                             final double theQuantile,
                             final double quantileAccuracy,
                             final SimpleStatistic aSimpleStat,
                             final Histogram aHistogram,
                             final int lagSpace,
                             final long combinedGoodSamples,
                             final long combinedTotalSamples,
                             final long combinedDiscardedSamples) {
        super(aStatCollection,
              aStatName,
              theNWarmupSamples,
              meanAccuracy,
              theQuantile,
              quantileAccuracy,
              aSimpleStat,
              aHistogram,
              lagSpace,
              combinedGoodSamples,
              combinedTotalSamples,
              combinedDiscardedSamples);

        super.phase = Phase.STEADYSTATE;
    }

}
