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

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class will hold all the physical objects in the datacenter for now.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class DataCenter implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The servers in the datacenter.
     */
    private Vector<Server> servers;

    /**
     * Creates a new datacenter.
     */
    public DataCenter() {
        this.servers = new Vector<Server>();
    }

    /**
     * Adds a server to the datacenter.
     *
     * @param server - the server to add
     */
    public void addServer(final Server server) {
        this.servers.add(server);
    }

    /**
     * Gets the servers in the datacenter.
     *
     * @return the server in the datacenter
     */
    public Vector<Server> getServers() {
        return this.servers;
    }

    /**
     * Updates the statistics of all the objects in the datacenter.
     *
     * @param time
     *            - the time the statistics are updated
     */
    public void updateStatistics(final double time) {
        Iterator<Server> iter = this.servers.iterator();
        while (iter.hasNext()) {
            iter.next().updateStatistics(time);
        }
    }

}
