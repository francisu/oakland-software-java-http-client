#!/bin/sh

#
# Builds the http client.  Assumes the first arg is the workspace
#

# Run this with JDK 1.4, nothing higher

cd $1/com.oaklandsw.util
ant clean
ant

# Have to build this 2x because the compile does not pick
# everything up correctly the first time
cd $1/com.oaklandsw.http
ant clean
ant
ant
ant dist

cd $1/com.oaklandsw.http.tests
ant clean
# First time compiles everything that can be compiled with 1.2, this will
# fail on TextAxis
ant -Dtarget.java.version=1.2
# This compiles the rest (TextAxis requires 1.3 to compile)
ant
