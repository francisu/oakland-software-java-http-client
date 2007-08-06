#!/bin/sh


httpclient=$HOME/d/com.oaklandsw.http.jars/commons-httpclient*.jar
codec=$HOME/d/com.oaklandsw.http.jars/commons-codec*.jar
regexp=$HOME/d/com.oaklandsw.http.jars/jakarta-regexp*.jar
con=$HOME/d/com.oaklandsw.http.jars/backport-util*.jar
cl=$HOME/d/com.oaklandsw.util.jars/commons-logging*.jar

java -Xmx400m -Xss2m -classpath `echo $httpclient`\
:$HOME/d/com.oaklandsw.http.tests/bin\
:$HOME/d/com.oaklandsw.http/bin\
:$HOME/d/com.oaklandsw.util/bin\
:`echo $cl`\
:`echo $regexp`\
:`echo $codec`\
:`echo $con` \
  TestPerf $*



