#!/bin/sh

#
# Builds the http client.  Assumes the first arg is the workspace
#

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
ant

