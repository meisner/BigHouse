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
import java.util.PriorityQueue;

/**
 * The EvenQueue manages events in the discrete event simulation.
 * The events are ordered by when they occur in time, so the
 * head of the queue represents the next event to occur.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class EventQueue implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The queue of events. Events are time ordered in a priority queue.
     */
    private PriorityQueue<Event> queue;

    /**
     * Creates a new EventQueue.
     */
    public EventQueue() {
        this.queue = new PriorityQueue<Event>();
    }

    /**
     * Get and remove the next event from the queue.
     * @return the next event
     */
    public Event nextEvent() {
        return this.queue.poll();
    }

    /**
     * Add an event to the event queue.
     * This event will now happen sometime in the future.
     * @param event - the event to add
     */
    public void addEvent(final Event event) {
        this.queue.add(event);
    }

    /**
     * Remove an event from the event queue.
     * @param event - the event to remove
     */
    public void cancelEvent(final Event event) {
        boolean removeWorked = this.queue.remove(event);
        // Make sure the event was actually removed
        if (!removeWorked || this.queue.contains(event)) {
            // TODO - we have a loop where a Timeout event will try
            // to remove itself while it is in process,
            // so cancel will fail
            Sim.fatalError("Tried to remove an event" + "and it failed: "
                    + event.getClass());
        }
    }

    /**
     * Get the size of the event queue.
     * @return the size of the event queue
     */
    public int size() {
        return this.queue.size();
    }

}
