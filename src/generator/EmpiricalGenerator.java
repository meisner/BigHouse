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
package generator;

import math.EmpiricalDistribution;

/**
 * An EmpiricalGenerator generates random numbers drawn from an
 * empirical distribution. It uses inverse transform sampling:
 * http://en.wikipedia.org/wiki/Inverse_transform_sampling
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class EmpiricalGenerator extends Generator {

    /** The serialization id. */
    private static final long serialVersionUID = 1L;

    /** The scaling factor to multiply generated numbers by. */
    private double scale;

    /** The name of the distribution. */
    private String name;

    /** The empirical distribution to draw from. */
    private EmpiricalDistribution cdf;

    /**
     * Creates a new EmpiricalGenerator.
     *
     * @param mtRandom - the random number generator to
     * get uniform random number from.
     * @param aCdf - the empirical distribution to draw from
     */
    public EmpiricalGenerator(final MTRandom mtRandom,
                              final EmpiricalDistribution aCdf) {
        this(mtRandom, aCdf, "");
    }

    /**
     * Creates a new EmpiricalGenerator.
     *
     * @param mtRandom - the random number generator to
     * get uniform random number from.
     * @param aCdf - the empirical distribution to draw from
     * @param theName - the name of the distribution
     */
    public EmpiricalGenerator(final MTRandom mtRandom,
                              final EmpiricalDistribution aCdf,
                              final String theName) {
        super(mtRandom);

        this.cdf = aCdf;
        this.scale = 1.0;
        this.name = theName;
    }

  /**
   * Creates a new EmpiricalGenerator.
   *
   * @param mtRandom - the random number generator
   * @param aCdf - the distribution from which to draw random numbers
   * @param theName - the name of the distribution
   * @param theScale - a scaling factor to modulate the distribution by
   * (random numbers are multiplied by this scaling factor)
   */
  public EmpiricalGenerator(final MTRandom mtRandom,
                            final EmpiricalDistribution aCdf,
                            final String theName,
                            final double theScale) {
    this(mtRandom, aCdf, theName);
    this.scale = theScale;
  }

  /**
   * Generates the next value.
   *
   * @return the next value
   */
  @Override
  public double next() {
    double rand = this.generator.nextDouble();
    double nextVal = this.cdf.getQuantile(rand);

    return this.scale * nextVal;
  }

  /**
   * Gets the name of the generator.
   *
   * @return the name of the generator
   */
  @Override
  public String getName() {
    return this.name;
  }

}
