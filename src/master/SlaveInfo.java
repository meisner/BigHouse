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
package master;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import slave.SimInterface;

public class SlaveInfo {

	public String server_name;
	public String rmi_binding;
	public SimInterface sim_interface;
	
	public SlaveInfo(String serverName, String rmiBinding) {
		this.server_name = serverName;
		this.rmi_binding = rmiBinding;
	}//End SlaveInfo()
	
	public String getServerName(){
		return this.server_name;
	}//End getServerName()
	
	public String getRmiBinding(){
		return this.rmi_binding;
	}//End getRmiBinding()
	
	public SimInterface getInterface() {
		return this.sim_interface;
	}//End getInterface()
	
	public void connect() {
		
		try {
			Registry registry = LocateRegistry.getRegistry(this.server_name);
			SimInterface stub = (SimInterface) registry.lookup(this.rmi_binding);		
			this.sim_interface = stub;
			System.out.println("Connected "+this.server_name+" to "+this.rmi_binding);
		} catch (Exception e) {
			System.out.println("Slave connection failed");
			e.printStackTrace();
		}

	}//End connect()
	
}//End class SlaveInfo
