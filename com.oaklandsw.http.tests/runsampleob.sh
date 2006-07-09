javac -classpath .:../com.oaklandsw.http/httpOb.jar HttpGetSample.java
java -Djava.net.preferIPv4Stack=true -cp .:../com.oaklandsw.http/httpOb.jar HttpGetSample $*
