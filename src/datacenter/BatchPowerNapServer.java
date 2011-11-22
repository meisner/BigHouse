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
package datacenter;

import java.util.Iterator;
import java.util.Vector;

import generator.Generator;
import core.Experiment;
import core.Job;

public class BatchPowerNapServer extends PowerNapServer {
	
	private Vector<Job> batch_buffer;
	
	private double batch_delay;
	private StartBatchEvent batch_event;

	public BatchPowerNapServer(int sockets, int coresPerSocket, Experiment experiment, Generator arrivalGenerator, 
			Generator serviceGenerator, double napTransitionTime,	double napPower, double batchTime) {
		super(sockets, coresPerSocket, experiment, arrivalGenerator, serviceGenerator, napTransitionTime, napPower);

		this.batch_buffer = new Vector<Job>();
		this.batch_delay = batchTime;
		
		  double firstBatchTime = this.batch_delay;
	        StartBatchEvent startBatchEvent = new StartBatchEvent(batchTime, this.experiment, this);
	        this.experiment.addEvent(startBatchEvent);
	        this.batch_event = startBatchEvent;
		
	}//MCPowerNapServer()
	
	
	@Override
	public void insertJob(double time, Job job) {
		
		this.batch_buffer.add(job);
		
	}//End insertJob()
	
	public void startBatch(double time) {
		
		Iterator<Job> iter = this.batch_buffer.iterator();
		
		while(iter.hasNext()) {
			Job job = iter.next();
			super.insertJob(time, job);
		}//End while
		
		this.batch_buffer.clear();
//		System.out.println(time+": Batch started jobs in system " + this.getJobsInSystem());
		
		
	    double batchTime = time + this.batch_delay;
        StartBatchEvent startBatchEvent = new StartBatchEvent(batchTime, this.experiment, this);
        this.experiment.addEvent(startBatchEvent);
        this.batch_event = startBatchEvent;
		
	}//End startBatch()
	
	
	@Override
	public void removeJob(double time, Job job) {
		super.removeJob(time, job);
	}//End removeJob()

}//End class MCPowerNapServer
