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
package datacenter;

import core.AbstractEvent;
import core.Experiment;

/**
 * Represents a PowerCappingEnforcer recalculating caps for the servers
 * it monitors and sends caps to.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class RecalculateCapsEvent extends AbstractEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The PowerCappingEnforcer that is going to recalculate caps.
     */
    private PowerCappingEnforcer enforcer;

    /**
     * Creates a new RecalculateCapsEvent.
     *
     * @param time - the time the recalculation occurs
     * @param experiment - the experiment the event occurs in
     * @param anEnforcer - the enforcer to recalculate caps
     */
    public RecalculateCapsEvent(final double time,
                                final Experiment experiment,
                                final PowerCappingEnforcer anEnforcer) {
        super(time, experiment);
        this.enforcer = anEnforcer;
    }

    /**
     * Has the PowerCappingEnforcer recalculate power caps for the servers.
     */
    @Override
    public void process() {
        this.enforcer.recalculateCaps(this.getTime());
    }

}
