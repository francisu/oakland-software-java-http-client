javac -classpath .:../com.oaklandsw.http/httpOb.jar:../com.oaklandsw.http.tests.jars/jce-jdk12-115.jar HttpGetSample.java
java -Djava.net.preferIPv4Stack=true -cp .:../com.oaklandsw.http/httpOb.jar:../com.oaklandsw.http.tests.jars/jce-jdk12-115.jar HttpGetSample $*
