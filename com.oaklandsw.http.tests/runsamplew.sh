javac -classpath .\;../com.oaklandsw.http/http.jar HttpGetSample.java
java -Dxxorg.apache.commons.logging.diagnostics.dest=STDOUT -Djava.net.preferIPv4Stack=true -cp .\;../com.oaklandsw.http/http.jar HttpGetSample $*
