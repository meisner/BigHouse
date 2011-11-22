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

public interface Constants {
	
	/** 
	 * Outputs which can be monitored have to be defined here
	 */
	public static enum StatName {
		IDLE_PERIOD_TIME, //Per job response times
		SOJOURN_TIME, //Per job response times
		TOTAL_CAPPING, //Total power capping of a cluster
		FULL_SYSTEM_IDLE_FRACTION,
		BUSY_PERIOD_TIME,
		GENERATED_ARRIVAL_TIME,
		GENERATED_SERVICE_TIME,
		WAIT_TIME, SERVER_LEVEL_CAP,
		
	}
	
	public static enum TimeWeightedStatName {
		SERVER_POWER, SERVER_UTILIZATION, SERVER_IDLE_FRACTION
	}//End enum TimeWeightedStatName
	
	/** 
	 * Power numbers 
	 */
	public static final int SERVER_IDLE_OTHER_POWER = 1;
	public static final int SERVER_DYN_OTHER_POWER = 1;
	public static final int SERVER_IDLE_MEM_POWER = 1;
	public static final int SERVER_DYN_MEM_POWER = 1;
	public static final int SOCKET_IDLE_POWER = 1;
	public static final int SOCKET_TRANSITION_POWER = 1;
	public static final int SOCKET_PARK_POWER = 1;
	public static final int CORE_ACTIVE_POWER = 1;
	public static final int CORE_IDLE_POWER = 1;
	public static final int CORE_TRANSITION_POWER = 1;
	public static final int CORE_TPARK_POWER = 1;
	
	public static final int MAX_QUEUE_SIZE = 500000;
	
	/** Values for statistical tests */
	public static final double Z_95_CONFIDENCE = 1.96;
	public static final double CHI_2_95_TEST = 12.592;
	
	/** Time window for which to compute utilization of a machine */
	public static final double DEFAULT_UTILIZATION_WINDOW = .01; //10ms
	public static final double DEFAULT_IDLENESS_WINDOW = .01; //10ms
	
	/** Statistics constants */
	public static final int RUNS_TEST_BUFFER_SIZE = 50000;
	public static final long MINIMUM_CONVERGE_SAMPLES = 10;
	public static final int GIVE_UP_STRIDE = 100;

	public static final int DEBUG_VERBOSE = 5;

	public static final double QUANTILE_A = .3;;
	
}//End interface Constants
