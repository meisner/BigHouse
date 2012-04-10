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
package core;

import datacenter.Socket;

/**
 * Event representing a CPU socket exiting park.
 *
 * @author meisner@umich.edu
 */
public final class SocketExitedParkEvent extends AbstractEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The socket which will be parked.
     */
    private Socket socket;

    /**
     * Creates a new SocketExitedParkEvent.
     * @param time - The time the socket exits park
     * @param experiment - The experiment the event takes place in
     * @param theSocket - The socket being taken out of park
     */
    public SocketExitedParkEvent(final double time,
                                 final Experiment experiment,
                                 final Socket theSocket) {
        super(time, experiment);
        this.socket = theSocket;
    }

    /**
     * Takes the socket out of park.
     */
    @Override
    public void process() {
        this.socket.exitPark(this.getTime());
    }

}
