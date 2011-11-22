#!/usr/bin/python

#
# Copyright (c) 2011 The Regents of The University of Michigan
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met: redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer;
# redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution;
# neither the name of the copyright holders nor the names of its
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
# Authors: Junjie Wu (wujj@umich.edu)
#          David Meisner (meisner@umich.edu)
#

# Filename: sqs.py
# This is the launching script of SQS, it can: 
#	1) setup auto ssh from localhost (master) to slaves
#	2) copy files from current directory to slaves's directory
#	3) start SQS simulations on localhost (master experiment) and slaves
#	4) clean up SQS simulations
# Please refer to machine.cfg for slave configurations

import os
import sys
import subprocess
import getpass
import pexpect
import time

machineList = []
machineCfg = 'machine.cfg'
experimentCfg = 'master.py'
passwd = ''

def usage():
	print('''Usage: ./sqs.py <setup|kill|copy|run> <machineconfig> [experimentconfig]
	- setup: setup auto-ssh connection
	- kill: kill all running slave.jar, rmiregistry processes
	- copy: push binaries from master to slaves without running the simulation
	- run: run the simulation
	- default experimentconfig is master.py''')
	sys.exit(1)

def readCfg(cfg):
	f = open(cfg, 'r')
	cfgContent = f.readline()
	while cfgContent != '':
		if cfgContent[0] == '#':
			cfgContent = f.readline()
		else:
			cfgContent = cfgContent.split()
			if len(cfgContent) == 4:
				machineList.append(cfgContent)
			cfgContent = f.readline()
	f.close()
	print "Machine List: %s" % machineList
	print "========================================"

def runInteractive(cmd):
	print ">> "+cmd
	p = subprocess.Popen(cmd, shell=True, executable='/bin/bash', stderr=subprocess.STDOUT)
	p.wait()
	return p.returncode

def runSSHCmd(cmd):
	# http://linux.byexamples.com/archives/346/python-how-to-access-ssh-with-pexpect/
	global passwd
	ssh_newkey = 'Are you sure you want to continue connecting'
	p=pexpect.spawn(cmd)
	i=p.expect([ssh_newkey,'password:',pexpect.EOF])
	if i==0:
		print "I say yes"
		p.sendline('yes')
		i=p.expect([ssh_newkey,'password:',pexpect.EOF])
	elif i==1:
		p.sendline(passwd)
		j=p.expect(['Permission denied, please try again.', pexpect.EOF])
		if j==0:
			print "Common password failed, Please enter the password"
			p.sendeof()
			runInteractive(cmd)
		else:
			print "Common password passed"
	elif i==2:
		pass
	print p.before # print out the result

def runBackground(cmd):
	print ">> "+cmd+" (background)"
	p = subprocess.Popen(cmd, shell=True, executable='/bin/bash',stderr=subprocess.STDOUT)
	time.sleep(1)

def scpMasterToSlave():
	if len(machineList) == 0:
		sys.exit("Machine List empty")
	else:
		print "=============================================="
		print "Copying files from current directory to slaves"
		print "=============================================="
		for i in range(0,len(machineList)):
			print "Slave #%d:" % i
			slave = machineList[i][1] + '@' + machineList[i][0]
			cmd = 'ssh ' + slave + " 'rm -rf %s/%s' " % (machineList[i][2], machineList[i][1])
			#print "Slave #%d: %s" % (i, cmd)
			runInteractive(cmd)
			cmd = 'ssh ' + slave + " 'mkdir -p %s/%s' " % (machineList[i][2], machineList[i][1])
			#print "Slave #%d: %s" % (i, cmd)
			runInteractive(cmd)
			cmd = 'scp -r -q . ' + machineList[i][1] + '@' + machineList[i][0] + ':' + machineList[i][2] + '/' + machineList[i][1]
			runInteractive(cmd)
			
def startSim():
	genScript()
	scpLaunchScript()
	startSimHelper()
	cleanLocalDir()

def startSimHelper():

	global experimentFile

	print "========================================"
	print "Starting Simulation"
	print "========================================"
	# slave
	for i in range(0,len(machineList)):
		cmd = "cd /tmp && sh slave_%d_%s.sh" % (i, machineList[i][1])
		cmd = 'ssh ' + machineList[i][1] + '@' + machineList[i][0] + " '" + cmd + "' &"
		print "Slave #%d: %s" % (i, cmd)
		runBackground(cmd)
	# master
	time.sleep(3)
	runInteractive("./%s %s" % (experimentCfg, machineCfg))
	
def scpLaunchScript():
	# call genScript() before this function
	print "========================================"
	print "Copying launch scripts to machines"
	print "========================================"
	for i in range(0,len(machineList)):
		machine = machineList[i][1] + '@' + machineList[i][0]
		cmd = 'scp -r ' + "slave_%d.sh" % i + ' ' + machineList[i][1] + '@' + machineList[i][0] + ":/tmp/slave_%d_%s.sh" % (i, machineList[i][1])
		print "Slave #%d: %s" % (i, cmd)
		runInteractive(cmd)
	print "Done"
	
def cleanLocalDir():
	print "========================================"
	print "Cleaning generated scripts"
	print "========================================"
	for i in range(0,len(machineList)):
		os.remove("slave_%d.sh" % i)
		print "slave_%d.sh removed" % i
	print "Done"
	
def genScript():
	print "========================================"
	print "Generating launch scripts"
	print "========================================"
	for i in range(0,len(machineList)):
		f = open("slave_%d.sh" % i, 'w')
		f.write("#!/bin/bash\n\n")
		cmd = 'cd ' + "\"" + machineList[i][2] + '/' + machineList[i][1] + '/bin' + "\" && rmiregistry &"
		f.write(cmd + '\n')
		for j in range(0, int(machineList[i][3])):
			cmd = '''java -Djava.rmi.slave.codebase=file://%s/%s/slave.jar -Djava.security.policy=slave.policy -jar slave.jar sim_%d &''' % (machineList[i][2], machineList[i][1], j)
			cmd = 'cd ' + machineList[i][2] + '/' + machineList[i][1] + " && %s" % cmd
			f.write(cmd + '\n')
		f.close()
		print "slave_%d.sh generated" % i
		
def killRMIRegs():
	print "========================================"
	print "Killing all previous simulations"
	print "========================================"
	for i in range(0,len(machineList)):
		print "Slave #%d: Killing rmiregistry, slave.jar" % i
		machine = machineList[i][1] + '@' + machineList[i][0]
		cmd = 'scp -r ' + "cleanup.sh" + ' ' + machineList[i][1] + '@' + machineList[i][0] + ":/tmp/cleanup_%s.sh" % machineList[i][1]
		runInteractive(cmd)
		cmd = "cd /tmp && ./cleanup_%s.sh" % machineList[i][1]
		cmd = 'ssh ' + machineList[i][1] + '@' + machineList[i][0] + " '" + cmd + "'"
		runBackground(cmd)
		time.sleep(1)
	print "\nPrevious simulation processes cleaned up"

def checkRSAFile():
	sshPath = os.path.expanduser('~') + "/.ssh/"
	if os.path.isfile(sshPath + 'id_rsa') and os.path.isfile(sshPath + 'id_rsa.pub'):
		print "Using %sid_rsa and %sid_rsa" % (sshPath, sshPath)
	else:
		print "No existing RSA pair found under %s" % sshPath
		print "Generating RSA pair...(hit enter whenever prompted)"
		runInteractive('ssh-keygen')

def pushPublicKey():
	PubKey = os.path.expanduser('~') + "/.ssh/id_rsa.pub"
	PrivKey = os.path.expanduser('~') + "/.ssh/id_rsa"
	for i in range(0, len(machineList)):
		print "Slave #%d:" % i
		slave = machineList[i][1] + '@' + machineList[i][0]
		cmd = '''scp ''' + PubKey + ' ' + slave + ':~'
		print ">> " + cmd
		runSSHCmd(cmd)
		cmd = 'ssh ' + slave + " 'if [ ! -e ~/.ssh ]; then mkdir ~/.ssh; fi && cat ~/id_rsa.pub >> ~/.ssh/authorized_keys' "
		print ">> " + cmd
		runSSHCmd(cmd)
	print "SSH setup finished. "

def runSimulation():
	readCfg(machineCfg)
	scpMasterToSlave()
	killRMIRegs()
	startSim()
	
def setupSSH():
	global passwd
	print "========================================"
	print('''Setup ssh connection for %s''' % machineCfg)
	print "========================================"
	checkRSAFile()
	print '''Please input the most commonly used password across the slaves'''
	passwd=getpass.getpass()
	readCfg(machineCfg)
	pushPublicKey()

def main(argv):

	global machineCfg, experimentCfg
	
	if len(argv) < 2:
		usage()
	else: 
		machineCfg = argv[1]
		print('''Machine config file: %s''' % machineCfg)
		if argv[0] == "run":
			if len(argv) == 3:
				experimentCfg = argv[2]
			print('''Experiment config file: %s''' % experimentCfg)
			runSimulation()
		elif argv[0] == "setup":
			setupSSH()
		elif argv[0] == "kill":
			readCfg(machineCfg)
			killRMIRegs()
		elif argv[0] == "copy":
			readCfg(machineCfg)
			scpMasterToSlave()
		else:
			usage()
	
if __name__ == "__main__":
    main(sys.argv[1:])
	
