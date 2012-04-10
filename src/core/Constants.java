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

/**
 * Constants for the entire simulator.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class Constants {

    /**
     * Should never be called.
     */
    private Constants() {

    }

    /**
     * Outputs which can be monitored as a Statistic have to be defined here.
     */
    public static enum StatName {
        /** Length in time of idle periods. */
        IDLE_PERIOD_TIME,

        /** Per job response time. */
        SOJOURN_TIME,

        /** Amount of capping (in watts) for a cluster. */
        TOTAL_CAPPING,

        /** Fraction of time a server is completely idle. */
        FULL_SYSTEM_IDLE_FRACTION,

        /** Amount of time a server is busy. */
        BUSY_PERIOD_TIME,

        /** The interarrival time as generated in the simulation. */
        GENERATED_ARRIVAL_TIME,

        /** The job service time as generated in the simulation. */
        GENERATED_SERVICE_TIME,

        /** Per-job wait time. */
        WAIT_TIME,

        /** Amount of capping (in watts) for an individual server. */
        SERVER_LEVEL_CAP,
    }

    /**
     * Outputs which can be monitored as a TimeWeightedStatistic have to be
     * defined here.
     */
    public static enum TimeWeightedStatName {
        /** Time-weighted server power. */
        SERVER_POWER,

        /** Time-weighted server utilization. */
        SERVER_UTILIZATION,

        /** Time-weighted fraction of time a server is idle. */
        SERVER_IDLE_FRACTION
    }

    /* Power breakdown for servers. */
    // TODO(meisner@umich.edu) Fill in these values.
    /** Power of "other" components at idle. */
    public static final int SERVER_IDLE_OTHER_POWER = 1;

    /** Dynamic power of "other" components at max. */
    public static final int SERVER_DYN_OTHER_POWER = 1;

    /** Power of the memory system at idle. */
    public static final int SERVER_IDLE_MEM_POWER = 1;

    /** Dynamic power of the memory system at max.*/
    public static final int SERVER_DYN_MEM_POWER = 1;

     /** Power of CPU "uncore" at idle. */
    public static final int SOCKET_IDLE_POWER = 1;

    /** Power transitioning the "uncore" of a CPU. */
    public static final int SOCKET_TRANSITION_POWER = 1;

    /** Power of CPU "uncore" while in socket parking. */
    public static final int SOCKET_PARK_POWER = 1;

    /** Dynamic power of CPU core at max. */
    public static final int CORE_ACTIVE_POWER = 1;

     /** Power of CPU core components at idle. */
    public static final int CORE_IDLE_POWER = 1;

    /** Power of transitioning a core in/out of park. */
    public static final int CORE_TRANSITION_POWER = 1;

    /** Power of CPU core while parked. */
    public static final int CORE_PARK_POWER = 1;

    /* Transition times */

    /** The time to transition the socket into park. */
    public static final double SOCKET_PARK_TRANSITION_TIME = 500e-6;

    /** Maximum length of queues. */
    public static final int MAX_QUEUE_SIZE = 500000;

    /* Values for statistical tests */
    /** Z value for 95th-percentile confidence from a normal distribution. */
    public static final double Z_95_CONFIDENCE = 1.96;

    /** Parameter for 95th-percentile in a Chi-squared test. */
    public static final double CHI_2_95_TEST = 12.592;

    /** Time window over which to compute utilization of a machine. */
    public static final double DEFAULT_UTILIZATION_WINDOW = .01;

    /** Time window over which to compute idleness of a machine. */
    public static final double DEFAULT_IDLENESS_WINDOW = .01;

    /* Constants for Statistics */
    /** Size of the buffer for the runs test. */
    public static final int RUNS_TEST_BUFFER_SIZE = 50000;

    /** The minimum number of samples to converge a statistic. */
    public static final long MINIMUM_CONVERGE_SAMPLES = 10;

    /** The maximum stride length between samples before giving up. */
    public static final int GIVE_UP_STRIDE = 100;

    /** The level of verbosity for debugging. */
    public static final int DEBUG_VERBOSE = 5;

}
