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

import java.util.Random;

/**
 * Generates random numbers from the gamma distribution.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class GammaGenerator extends Generator {

    /** The serialization id. */
    private static final long serialVersionUID = 1L;

    /** The gamma distribution's k parameter. */
    private double k;

    /** The gamma distribution's theta parameter. */
    private double theta;

    /**
     * Creates a new GammaGenerator.
     *
     * @param generator - the random number generator to
     * get uniform random number from.
     * @param theK - the gamma distribution's k parameter
     * @param theTheta - the gamma distribution's theta parameter
     */
    public GammaGenerator(final MTRandom generator,
                          final double theK,
                          final double theTheta) {
        super(generator);

        this.k = theK;
        this.theta = theTheta;
    }

    /**
     * Generates the next value.
     *
     * @return the next value
     */
    @Override
    public double next() {
        //TODO cite the source of this code
        //TODO Suppress magic number warnings
        Random rng = this.generator;
        boolean accept = false;
        if (k < 1) {
            // Weibull algorithm
            double c = (1 / k);
            double d = ((1 - k) * Math.pow(k, (k / (1 - k))));
            double u, v, z, e, x;
            do {
                u = rng.nextDouble();
                v = rng.nextDouble();
                z = -Math.log(u);
                e = -Math.log(v);
                x = Math.pow(z, c);
                if ((z + e) >= (d + x)) {
                    accept = true;
                }
            } while (!accept);
            return (x * theta);
        } else {
            // Cheng's algorithm
            double b = (k - Math.log(4));
            double c = (k + Math.sqrt(2 * k - 1));
            double lam = Math.sqrt(2 * k - 1);
            double cheng = (1 + Math.log(4.5));
            double u, v, x, y, z, r;
            do {
                u = rng.nextDouble();
                v = rng.nextDouble();
                y = ((1 / lam) * Math.log(v / (1 - v)));
                x = (k * Math.exp(y));
                z = (u * v * v);
                r = (b + (c * y) - x);
                if ((r >= ((4.5 * z) - cheng))
                        || (r >= Math.log(z))) {
                    accept = true;
                }
            } while (!accept);
            return (x * theta);
        }
    }

    /**
     * Gets the name of the generator.
     *
     * @return the name of the generator
     */
    @Override
    public String getName() {
        return "Gamma";
    }

}
