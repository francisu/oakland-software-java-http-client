# Run the tests using the 12 vm which requires a different way of executing

# tests with non-obfuscated version since many internal
# methods/classes are used
export LD_PRELOAD=/usr/java/jdk122waithack/libcwait.so

/usr/java/jdk1.2.2/jre/bin/i386/native_threads/java  \
-DOAKLANDSW_ROOT=$HOME/d  \
-Djava.net.preferIPv4Stack=true \
-cp "$HOME/d/com.oaklandsw.http.tests/httptests.jar\
:$HOME/d/com.oaklandsw.http.tests.jars/junit.jar\
:$HOME/d/com.oaklandsw.http.jars/axis.jar\
:$HOME/d/com.oaklandsw.http.jars/wsdl4j-1.5.1.jar\
:$HOME/d/com.oaklandsw.http.jars/saaj.jar\
:$HOME/d/com.oaklandsw.http.jars/jaxrpc.jar\
:$HOME/d/com.oaklandsw.http.jars/activation.jar\
:$HOME/d/com.oaklandsw.http.jars/commons-discovery-0.2.jar\
:$HOME/d/com.oaklandsw.http/httpNonObf.jar\
:$HOME/d/com.oaklandsw.util.jars/log4j.jar\
:$HOME/d/com.oaklandsw.util.jars/commons-logging-1.1.1-SNAPSHOT.jar\
:$HOME/d/com.oaklandsw.util/util.jar" \
com.oaklandsw.http.AllHttpTests
