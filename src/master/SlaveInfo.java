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
package master;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import slave.SimInterface;

/**
 * Tracks info about slaves in a simulation.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class SlaveInfo {

    /**
     * The name of the slave.
     */
    private String serverName;

    /**
     * The RMI binding of the slave.
     */
    private String rmiBinding;

    /**
     * The interface by which the slave is accessed.
     */
    private SimInterface simInterface;

    /**
     * Creates a SlaveInfo to represent a slave.
     * @param name - The name of the slave.
     * @param binding - The RMI binding to connect to the slave.
     */
    public SlaveInfo(final String name, final String binding) {
        this.serverName = name;
        this.rmiBinding = binding;
    }

    /**
     * Gets the name of the server.
     * @return The server name
     */
    public String getServerName() {
        return this.serverName;
    }

    /**
     * Gets the RMI binding to access a slave.
     * @return The RMI binding string
     */
    public String getRmiBinding() {
        return this.rmiBinding;
    }

    /**
     * Gets the interface to the slave.
     * @return The slave interface
     */
    public SimInterface getInterface() {
        return this.simInterface;
    }

    /**
     * Connects to the server referred to by this SlaveInfo.
     */
    public void connect() {
        try {
            Registry registry = LocateRegistry.getRegistry(this.serverName);
            SimInterface stub = (SimInterface) registry
                                .lookup(this.rmiBinding);
            this.simInterface = stub;
            System.out.println("Connected " + this.serverName + " to "
                               + this.rmiBinding);
        } catch (Exception e) {
            System.out.println("Slave connection failed");
            e.printStackTrace();
        }
    }

}
