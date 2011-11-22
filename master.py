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
#

# This script defines and runs a distributed SQS experiment
# You shouldn't directly run this script from command line 
# Instead, you should use sqs.py to launch SQS, which will invoke this file
# This script uses JPype to access SQS Java library (Check README for JPype configuration)

# Most of the codes are JPype and SQS setups
# Due to the limition of JPype, everything has to happen in one file during one JVM session
# The only things you need to change to script an experiment are stat_config and createExperiment()

import os.path
import sys
import time

# JPype: import jar files
print "========== JPype Output Begin =========="
import jpype
jarpath = os.path.abspath('.')
jpype.startJVM(jpype.getDefaultJVMPath(), "-Djava.ext.dirs=%s" % jarpath)
print "========== JPype Output End ============"

# JPype: import java packages
java = jpype.JPackage('java')
sqs_core = jpype.JPackage('core')
sqs_datacenter = jpype.JPackage('datacenter')
sqs_generator = jpype.JPackage('generator')
sqs_math = jpype.JPackage('math')
sqs_stat = jpype.JPackage('stat')
sqs_master = jpype.JPackage('master')
sqs_StatName = jpype.JClass('core.Constants$StatName')
sqs_SocketPowerPolicy = jpype.JClass('datacenter.Socket$SocketPowerPolicy')
sqs_CorePowerPolicy = jpype.JClass('datacenter.Core$CorePowerPolicy')

# ========== Don't change code above unless you need to import new packages ==========

# ========== Change stat_config and createExperiment() to define you own experiment ==========

# global statistic requirements for convergence
# SQS will run until the aggregated result meets the statistical requirements
# stat_config is a list of target statistics, each element is a 5-tuple
# (stat name, mean precision requirement, quantile setting, quantile precision requirement, warmup samples)
# stat name is the output you are interested in. Supported statname is defined in src/core/Constants.StatName
# precision requirements are denoted by alpha value
# warmup samples are the number of samples discarded before collecting statistics
stat_config = [ (sqs_StatName.TOTAL_CAPPING, 0.05, 0.95, 0.05, 500),
				(sqs_StatName.SOJOURN_TIME, 0.05, 0.95, 0.05, 500) ]

# this function defines an experiment (input distribution and datacenter)
# when creating the master experiment, xValues should be zero
# when creating slave experiments, xValues should be extracted from warmed-up master experiment
def createExperiment(xValues = []):

	global stat_config
	
	# setup experiment skeleton
	experimentInput = sqs_core.ExperimentInput()
	experimentOutput = sqs_core.ExperimentOutput()
	
	# insert stat_config into experiment output
	for i in range(0, len(stat_config)):
		if (xValues == []):
			experimentOutput.addOutput(stat_config[i][0], stat_config[i][1], stat_config[i][2], stat_config[i][3], stat_config[i][4])
		else: 
			experimentOutput.addOutput(stat_config[i][0], stat_config[i][1], stat_config[i][2], stat_config[i][3], stat_config[i][4], xValues[i])
	
	rand = sqs_generator.MTRandom(long(1))
	experiment = sqs_core.Experiment("Power capping test", rand, experimentInput, experimentOutput)
	
	# service file used by generators
	arrivalFile = "workloads/www.arrival.cdf"
	serviceFile = "workloads/www.service.cdf"

	# specify input distribution
	cores = 4
	sockets = 1
	targetRho = 0.5

	arrivalDistribution = sqs_math.Distribution.loadDistribution(arrivalFile, 1e-3)
	serviceDistribution = sqs_math.Distribution.loadDistribution(serviceFile, 1e-3)

	averageInterarrival = arrivalDistribution.getMean()
	averageServiceTime = serviceDistribution.getMean()
	qps = 1/averageInterarrival
	rho = qps/(cores*(1/averageServiceTime))
	arrivalScale = rho/targetRho
	averageInterarrival = averageInterarrival*arrivalScale
	serviceRate = 1/averageServiceTime
	scaledQps = (qps/arrivalScale)
	
	# debug output
	#print "Cores: %s" % cores
	#print "rho: %s"	% rho
	#print "recalc rho: %s" % (scaledQps/(cores*(1/averageServiceTime)))
	#print "arrivalScale: %s" % arrivalScale
	#print "Average interarrival time: %s" % averageInterarrival
	#print "QPS as is %s" % qps
	#print "Scaled QPS: %s" % scaledQps
	#print "Service rate as is %s" % serviceRate
	#print "Service rate x: %s" % cores + " is: %s" % ((serviceRate)*cores)
	#print "\n------------------\n"
	
	arrivalGenerator = sqs_generator.EmpiricalGenerator(rand, arrivalDistribution, "arrival", arrivalScale)
	serviceGenerator = sqs_generator.EmpiricalGenerator(rand, serviceDistribution, "service", 1.0)

	#setup datacenter	
	dataCenter = sqs_datacenter.DataCenter()

	nServers = 100	
	capPeriod = 1.0
	globalCap = 70.0 * nServers
	maxPower = 100.0 * nServers
	minPower = 59.0 * nServers
	enforcer = sqs_datacenter.PowerCappingEnforcer(experiment, capPeriod, globalCap, maxPower, minPower)
	for i in range(0, nServers):
		server = sqs_datacenter.Server(sockets, cores, experiment, arrivalGenerator, serviceGenerator)

		server.setSocketPolicy(sqs_SocketPowerPolicy.NO_MANAGEMENT)
		server.setCorePolicy(sqs_CorePowerPolicy.NO_MANAGEMENT)	
		coreActivePower = 40 * (4.0/5)/cores
		coreHaltPower = coreActivePower*0.2
		coreParkPower = 0.0

		socketActivePower = 40 * (1.0/5)/sockets
		socketParkPower = 0.0

		server.setCoreActivePower(coreActivePower)
		server.setCoreParkPower(coreParkPower)
		server.setCoreHaltPower(coreHaltPower)

		server.setSocketActivePower(socketActivePower)
		server.setSocketParkPower(socketParkPower)
		enforcer.addServer(server)
		dataCenter.addServer(server)
		
	experimentInput.addDataCenter(dataCenter)
	
	return experiment

# ========== Don't change code below ==========

# this function runs a distributed experiment
# it passes experiments and results between Python and Java
# don't change this function
def runExperiment(cfg):
	try:
		# instantiate master
		master = sqs_master.Master()

		# load machine config
		numberOfSlaves = master.parseConfigFile(cfg)
		print "Number of Slaves: %s" % numberOfSlaves

		# create master experiment
		masterExperiment = createExperiment()

		# run master experiment to steady state
		master.runMasterExperiment(masterExperiment)

		# extract xValues, distribute and run slave experiments
		xValues = []
		for i in range(0, len(stat_config)):
			xValues.append(masterExperiment.getStats().getStat(stat_config[i][0]).getHistogramXValues());
			
		slaveExperiments = []
		for i in range(0, numberOfSlaves):
			slaveExperiments.append(createExperiment(xValues))
			
		master.runSlaveExperiment(slaveExperiments)
		
		# simulation finish
		time.sleep(10)

	except jpype.JavaException as ex:
		print "Caught Java Exception, exit. "
		print ex.stacktrace()
		exit(1)

def usage():
	print "Usage: ./master.py <machine config>"
	exit(1)
	
def exit(x):
	print "========== JPype Output Begin =========="
	jpype.shutdownJVM()
	print "========== JPype Output End ============"
	sys.exit(x)

def main(argv):
	if len(argv) == 1:
		runExperiment(argv[0])
	else:
		usage()
	exit(0)
	
if __name__ == "__main__":
    main(sys.argv[1:])
