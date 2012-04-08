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
experimentCfg = 'powercap.py'
passwd = ''
log = open('sqs.log','w')

def usage():
	print('''Usage: ./sqs.py <setup|kill|copy|run> <machineconfig> [experimentconfig]
	- setup: setup auto-ssh connection
	- kill: kill all running slave.jar, rmiregistry processes
	- copy: push binaries from master to slaves without running the simulation
	- run: run the simulation
	- default experimentconfig is powercap.py''')
	sys.exit(1)

def dPrint(text):
	print text
	log.write(text + '\n')

def fPrint(text):
	log.write(text + '\n')

def readCfg(cfg):
	f = open(cfg, 'r')
	cfgContent = f.readline()
	while cfgContent != '':
		if cfgContent[0] == '#' or cfgContent == '\n':
			cfgContent = f.readline()
		else:
			cfgContent = cfgContent.split()
			if len(cfgContent) == 4:
				machineList.append(cfgContent)
			else:
				dPrint("Malformatted machine configure file, quit")
				dPrint("Malformatted line: %s" % cfgContent)
				sys.exit(1)
			cfgContent = f.readline()
	f.close()
	fPrint("Machine List: %s" % machineList)
	fPrint("========================================")

def runInteractive(cmd):
	fPrint(">> " + cmd)
	p = subprocess.Popen(cmd, shell=True, executable='/bin/bash', stderr=subprocess.STDOUT)
	p.wait()
	if p.returncode != 0:
		print "SQS failed when running: %s\nPlease refer to sqs.log for details" % cmd
		sys.exit(1)
	return p.returncode

def runInteractiveS(cmd):
	fPrint(">> " + cmd)
	p = subprocess.Popen(cmd, shell=True, executable='/bin/bash', stderr=subprocess.STDOUT, stdout=log)
	p.wait()
	if p.returncode != 0:
		print "SQS failed when running: %s\nPlease refer to sqs.log for details" % cmd
		sys.exit(1)
	return p.returncode

def runBackground(cmd):
	fPrint(">> " + cmd + " (background)")
	p = subprocess.Popen(cmd, shell=True, executable='/bin/bash', stderr=subprocess.STDOUT)

def runSSHCmd(cmd):
	# http://linux.byexamples.com/archives/346/python-how-to-access-ssh-with-pexpect/
	global passwd
	ssh_newkey = 'Are you sure you want to continue connecting'
	p=pexpect.spawn(cmd)
	i=p.expect([ssh_newkey,'password:',pexpect.EOF])
	if i==0:
		#print "I say yes"
		p.sendline('yes')
		i=p.expect([ssh_newkey,'password:',pexpect.EOF])
	elif i==1:
		p.sendline(passwd)
		j=p.expect(['Permission denied, please try again.', pexpect.EOF])
		if j==0:
			dPrint("Common password failed, Please enter the password")
			p.sendeof()
			runInteractiveS(cmd)
		else:
			fPrint("Common password passed")
	elif i==2:
		pass
	fPrint(p.before) # print out the result


def scpMasterToSlave():
	if len(machineList) == 0:
		sys.exit("Machine List empty")
	else:
		dPrint("==============================================")
		dPrint("Copying files from current directory to slaves")
		dPrint("==============================================")
		for i in range(0,len(machineList)):
			dPrint("Copying files for slave #%d" % i)
			slave = machineList[i][1] + '@' + machineList[i][0]
#			cmd = 'ssh ' + slave + " 'rm -rf %s/%s' " % (machineList[i][2], machineList[i][1])
#			runInteractiveS(cmd)
#			cmd = 'ssh ' + slave + " 'mkdir -p %s/%s' " % (machineList[i][2], machineList[i][1])
#			runInteractiveS(cmd)
			cmd = 'rsync -r --delete . ' + machineList[i][1] + '@' + machineList[i][0] + ':' + machineList[i][2] + '/' + machineList[i][1]
			runInteractiveS(cmd)
			
def startSim():
	genScript()
	scpLaunchScript()
	startSimHelper()
	cleanLocalDir()

def startSimHelper():

	global experimentFile

	dPrint("========================================")
	dPrint("Starting Simulation")
	dPrint("========================================")
	# slave
	for i in range(0,len(machineList)):
		cmd = "cd /tmp && sh slave_%d_%s.sh" % (i, machineList[i][1])
		cmd = 'ssh ' + machineList[i][1] + '@' + machineList[i][0] + " '" + cmd + "' &"
		dPrint("Starting slave #%d..." % i)
		fPrint("Slave #%d: %s" % (i, cmd))
		runBackground(cmd)
	# master
	time.sleep(3)
	runInteractive("./%s %s" % (experimentCfg, machineCfg))
	
def scpLaunchScript():
	# call genScript() before this function
	dPrint("========================================")
	dPrint("Copying launch scripts to machines")
	dPrint("========================================")
	for i in range(0,len(machineList)):
		machine = machineList[i][1] + '@' + machineList[i][0]
		cmd = 'rsync -r --delete ' + "slave_%d.sh" % i + ' ' + machineList[i][1] + '@' + machineList[i][0] + ":/tmp/slave_%d_%s.sh" % (i, machineList[i][1])
		dPrint("Copying launch scripts to slave #%d" % i)
		fPrint("Slave #%d: %s" % (i, cmd))
		runInteractiveS(cmd)
	dPrint("Done")
	
def cleanLocalDir():
	dPrint("========================================")
	dPrint("Cleaning generated scripts")
	dPrint("========================================")
	for i in range(0,len(machineList)):
		os.remove("slave_%d.sh" % i)
		fPrint("slave_%d.sh removed" % i)
	dPrint("Done")
	
def genScript():
	dPrint("========================================")
	dPrint("Generating launch scripts")
	dPrint("========================================")
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
		dPrint("slave_%d.sh generated" % i)
		
def killRMIRegs():
	dPrint("========================================")
	dPrint("Killing all previous simulations")
	dPrint("========================================")
	for i in range(0,len(machineList)):
		dPrint("Slave #%d: Killing rmiregistry, slave.jar" % i)
		machine = machineList[i][1] + '@' + machineList[i][0]
		cmd = 'rsync -r --delete ' + "cleanup.sh" + ' ' + machineList[i][1] + '@' + machineList[i][0] + ":/tmp/cleanup_%s.sh" % machineList[i][1]
		runInteractiveS(cmd)
		cmd = "cd /tmp && ./cleanup_%s.sh" % machineList[i][1]
		cmd = 'ssh ' + machineList[i][1] + '@' + machineList[i][0] + " '" + cmd + "'"
		runBackground(cmd)
		time.sleep(1)
	dPrint("Done")

def checkRSAFile():
	sshPath = os.path.expanduser('~') + "/.ssh/"
	if os.path.isfile(sshPath + 'id_rsa') and os.path.isfile(sshPath + 'id_rsa.pub'):
		dPrint("Using %sid_rsa and %sid_rsa" % (sshPath, sshPath))
	else:
		dPrint("No existing RSA pair found under %s" % sshPath)
		dPrint("Generating RSA pair...(hit enter whenever prompted)")
		runInteractive('ssh-keygen')

def pushPublicKey():
	PubKey = os.path.expanduser('~') + "/.ssh/id_rsa.pub"
	PrivKey = os.path.expanduser('~') + "/.ssh/id_rsa"
	for i in range(0, len(machineList)):
		slave = machineList[i][1] + '@' + machineList[i][0]
		dPrint("Slave #%d: %s" % (i, slave))
		cmd = '''scp ''' + PubKey + ' ' + slave + ':~'
		fPrint(">> " + cmd)
		runSSHCmd(cmd)
		cmd = 'ssh ' + slave + " 'if [ ! -e ~/.ssh ]; then mkdir ~/.ssh; fi && cat ~/id_rsa.pub >> ~/.ssh/authorized_keys' "
		fPrint(">> " + cmd)
		runSSHCmd(cmd)
	dPrint("SSH setup finished. ")

def runSimulation():
	readCfg(machineCfg)
	scpMasterToSlave()
	killRMIRegs()
	startSim()
	
def setupSSH():
	global passwd
	dPrint("========================================")
	dPrint('''Setup ssh connection for %s''' % machineCfg)
	dPrint("========================================")
	checkRSAFile()
	dPrint("Please input the most commonly used password across the slaves")
	passwd=getpass.getpass()
	readCfg(machineCfg)
	pushPublicKey()

def main(argv):

	global machineCfg, experimentCfg, log
	
	if len(argv) < 2:
		usage()
	else: 
		machineCfg = argv[1]
		dPrint('''Machine config file: %s''' % machineCfg)
		if argv[0] == "run":
			if len(argv) == 3:
				experimentCfg = argv[2]
			dPrint('''Experiment config file: %s''' % experimentCfg)
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

	log.close()
	
if __name__ == "__main__":
    main(sys.argv[1:])
	
