#!/bin/sh
#
# Runs an example HTTP client program
#
# usage:  runsample.sh <progName> <args>
#
# Run this within the examples directory
#
java -Djava.net.preferIPv4Stack=true -cp .:../http.jar $*
