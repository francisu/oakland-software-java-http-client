javac -classpath .:../com.oaklandsw.http/http.jar HttpGetSample.java
java -Djava.net.preferIPv4Stack=true -cp .:../com.oaklandsw.http/http.jar HttpGetSample $*
