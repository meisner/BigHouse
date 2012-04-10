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

/**
 * An Abstract implementation of the Event interface.
 * Provides basic time and experiment methods.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public abstract class AbstractEvent
       implements Event, Comparable<Event>, Serializable {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    /** The time the event takes place. */
    protected double time;

    /** The experiment the even is associated with. */
    private Experiment experiment;

    /**
     * A constructor for subclasses to use.
     * @param theTime - The time the event occurs at
     * @param anExperiment - The experiment the event happens in
     */
    public AbstractEvent(final double theTime,
                         final Experiment anExperiment) {
        this.time = theTime;
        this.experiment = anExperiment;
    }

    /**
     * Get the time the event occurs.
     *
     * @return the time the event occurs
     */
    public final double getTime() {
        return this.time;
    }

    /**
     * Get the experiment the event happens in.
     * @return the experiment the event happens in
     */
    public final Experiment getExperiment() {
        return this.experiment;
    }

    /**
     * Checks if an event takes place before or after this one.
     * @param otherEvent - the event to compare to this one
     * @return the value of
     * {@link java.lang.Double#compareTo(Double anotherDouble)}
     * comparing the times of the two events
     */
    public final int compareTo(final Event otherEvent) {
        Double thisTime = this.time;
        Double otherTime = new Double(otherEvent.getTime());
        return thisTime.compareTo(otherTime);
    }

}
