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

//TODO delete this
public class HyperExponentialGenerator extends Generator {


	private double lambdas[][];
//	private Random generator;

	
	public HyperExponentialGenerator(MTRandom mtRandom, double[][] lambdas){
		super(mtRandom);
		this.lambdas = lambdas;
//		this.generator = new Random();
		if(this.lambdas[0].length != 2){
			System.out.println("Hyperexponential needs a 2d array of lambda weight pairs!");
			System.exit(-1);
		}

		double sum = 0.0;
		for(int i = 0; i < this.lambdas.length; i++){
			sum += this.lambdas[i][0];
			//System.out.println("adding "+this.lambdas[i][0]);
		}
		
		if( sum - 1.0 > .1){
			System.out.println("Hyperexponential needs weights that add up to 1.0! sum = " + sum);
			System.exit(-1);
		}
	
	}
	
	@Override
	public double next() {

		double sum = 0.0;
		
		for(int i = 0; i < this.lambdas.length; i++){
			double lambda = this.lambdas[i][1];
			double weight = this.lambdas[i][0];
			//System.out.println("weight  "+this.lambdas[i][0] + " valu" + this.lambdas[i][1]);

		    double random = this.generator.nextDouble();
		    double expVar = -Math.log(random)/lambda;
		    sum += weight * expVar;
		    assert(expVar != Float.NaN);
		}
	    		
		return sum;
	}
	
    /**
     * Gets the name of the generator.
     *
     * @return the name of the generator
     */
	@Override
	public String getName() {
		return "HyperExponential";
	}

}
