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

package core;

import java.io.Serializable;
import java.util.HashMap;

import datacenter.DataCenter;

/** ExperimentInput contains datacenter used in the experiment */
public class ExperimentInput implements Serializable{
	
	private DataCenter data_center;
	private HashMap<String, String> input_params;
	
	public ExperimentInput(){			
		this.input_params = new HashMap<String, String>();
	}
	
	public void setParamValue(String name, double value){
		this.input_params.put(name, Double.toString(value));
	}
	
	public void setParamValue(String name, int value){
		this.input_params.put(name, Integer.toString(value));
	}
	
	public String getParamValue(String name){
		String paramValue = this.input_params.get(name);
		if(paramValue == null){
			paramValue = "";
		}
		return paramValue;
	}

	public void addDataCenter(DataCenter dataCenter){
		this.data_center = dataCenter;
	}
	
	public DataCenter getDataCenter(){
		return this.data_center;
	}
	
}//End class ExperimentInput
