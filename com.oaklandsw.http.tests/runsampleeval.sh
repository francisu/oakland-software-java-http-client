javac -classpath .:../com.oaklandsw.http/httpEval.jar HttpGetSample.java
java -Djava.net.preferIPv4Stack=true -cp .:../com.oaklandsw.http/httpEval.jar HttpGetSample $*
