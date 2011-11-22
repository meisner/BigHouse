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
package generator;

public class MyHyperExponentialGenerator extends Generator {

	private HyperExponentialGenerator hyper_exponential_generator;
	
	public MyHyperExponentialGenerator(MTRandom random, int k, double lambda){
		super(random);
		
		int numSource = 30;
		double weight = 1.0;
		
		double[][] lambdas = new double[numSource][2];

		double newLamb = lambda;
		for(int i = 0; i < numSource; i++){
			weight = weight/2;
			newLamb = newLamb*3;
			lambdas[i][0] = weight;
			//System.out.println("weight + " + weight);
			//System.out.println("newLamb + " + newLamb);
			lambdas[i][1] = newLamb;
		}
		
		this.hyper_exponential_generator = new HyperExponentialGenerator(random, lambdas);
	}

	@Override
	public double next() {
		return this.hyper_exponential_generator.next();
	}
	
	@Override
	public String getName() {
		return "MyHyperExponential";
	}
	
	
}
