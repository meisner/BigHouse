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

import java.util.Random;

import math.Distribution;


public class EmpiricalGenerator extends Generator {

  private double scale;
  private String name;
//  private Random generator;
  private Distribution cdf;

  public EmpiricalGenerator(MTRandom mtRandom,Distribution cdf){

	  this(mtRandom, cdf, "");

  }

  public EmpiricalGenerator(MTRandom mtRandom, Distribution cdf, String name){
	  super(mtRandom);
//	  this.generator = new MTRandom();
	  this.cdf = cdf;
	  this.scale = 1.0;
	  this.name = name;

  }

  public EmpiricalGenerator(MTRandom mtRandom, Distribution cdf, String name, double scale){
	  
    this(mtRandom, cdf, name);
    this.scale = scale;
    
  }

  @Override
  public double next() {

    double rand = this.generator.nextDouble();
    
    double nextVal = this.cdf.getQuantile(rand);
//    System.out.println(" rand = " + rand+" generated " +nextVal);
    return this.scale * nextVal;
    
  }

  @Override
  public String getName() {
    return this.name;
  }


}
