#!/bin/bash
PID=`ps aux | grep rmiregistry | grep -vw grep | awk '{print $2}'`; for pid in $PID; do kill $pid; done
PID=`ps aux | grep slave.jar | grep -vw grep | awk '{print $2}'`; for pid in $PID; do kill $pid; done
