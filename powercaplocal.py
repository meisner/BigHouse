#!/usr/bin/python

import jpype
import os.path

# import jar files
jarpath = os.path.abspath('.')
jpype.startJVM(jpype.getDefaultJVMPath(), "-Djava.ext.dirs=%s" % jarpath)

# import java packages
java = jpype.JPackage('java')
core = jpype.JPackage('core')
datacenter = jpype.JPackage('datacenter')
generator = jpype.JPackage('generator')
math = jpype.JPackage('math')
stat = jpype.JPackage('stat')
StatName = jpype.JClass('core.Constants$StatName')
SocketPowerPolicy = jpype.JClass('datacenter.Socket$SocketPowerPolicy')
CorePowerPolicy = jpype.JClass('datacenter.Core$CorePowerPolicy')

# global stat requirements for statistical convergence
meanPrecision = .05
quantileSetting = .95
quantilePrecision = .05
warmupSamples = 5000

# this function creates an experiment
def createExperiment(xValues = []):

	global meanPrecision, quantileSetting, quantilePrecision, warmupSamples
	
	# service file
	arrivalFile = "workloads/csedns.arrival.cdf"
	serviceFile = "workloads/csedns.service.cdf"

	#specify distribution
	cores = 4
	sockets = 1
	targetRho = 0.5

	arrivalDistribution = math.Distribution.loadDistribution(arrivalFile, 1e-3)
	serviceDistribution = math.Distribution.loadDistribution(serviceFile, 1e-3)

	averageInterarrival = arrivalDistribution.getMean()
	averageServiceTime = serviceDistribution.getMean()
	qps = 1/averageInterarrival
	rho = qps/(cores*(1/averageServiceTime))
	arrivalScale = rho/targetRho
	averageInterarrival = averageInterarrival*arrivalScale
	serviceRate = 1/averageServiceTime
	scaledQps = (qps/arrivalScale)

	# debug output
	print "Cores: %s" % cores
	print "rho: %s"	% rho
	print "recalc rho: %s" % (scaledQps/(cores*(1/averageServiceTime)))
	print "arrivalScale: %s" % arrivalScale
	print "Average interarrival time: %s" % averageInterarrival
	print "QPS as is %s" % qps
	print "Scaled QPS: %s" % scaledQps
	print "Service rate as is %s" % serviceRate
	print "Service rate x: %s" % cores + " is: %s" % ((serviceRate)*cores)
	print "\n------------------\n"

	# setup experiment
	experimentInput = core.ExperimentInput()

	rand = generator.MTRandom(long(1))
	arrivalGenerator = generator.EmpiricalGenerator(rand, arrivalDistribution, "arrival", arrivalScale)
	serviceGenerator = generator.EmpiricalGenerator(rand, serviceDistribution, "service", 1.0)
	experimentOutput = core.ExperimentOutput()
	experimentOutput.addOutput(StatName.SOJOURN_TIME, meanPrecision, quantileSetting, quantilePrecision, warmupSamples)
	experimentOutput.addOutput(StatName.SERVER_LEVEL_CAP, meanPrecision, quantileSetting, quantilePrecision, warmupSamples)
	experiment = core.Experiment("Power capping test", rand, experimentInput, experimentOutput)

	#setup datacenter	
	dataCenter = datacenter.DataCenter()

	nServers = 100	
	capPeriod = 1.0
	globalCap = 65.0 * nServers
	maxPower = 100.0 * nServers
	minPower = 59.0 * nServers
	enforcer = datacenter.PowerCappingEnforcer(experiment, capPeriod, globalCap, maxPower, minPower)
	for i in range(0, nServers):
		server = datacenter.Server(sockets, cores, experiment, arrivalGenerator, serviceGenerator)

		server.setSocketPolicy(SocketPowerPolicy.NO_MANAGEMENT)
		server.setCorePolicy(CorePowerPolicy.NO_MANAGEMENT)	
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

experiment = createExperiment()
experiment.run()

# experiment finished
responseTimeMean = experiment.getStats().getStat(StatName.SOJOURN_TIME).getAverage()
print "SOJOURN_TIME mean : %s" % responseTimeMean
responseTimeQuantile = experiment.getStats().getStat(StatName.SOJOURN_TIME).getQuantile(quantileSetting)
print "%s quantile SOJOURN_TIME : %s" % (quantileSetting, responseTimeQuantile)
cappingMean = experiment.getStats().getStat(StatName.SERVER_LEVEL_CAP).getAverage()
print "Average Server Cap : %s" % cappingMean

print "========== JPype Output Begin =========="
jpype.shutdownJVM() 
print "========== JPype Output End ============"
