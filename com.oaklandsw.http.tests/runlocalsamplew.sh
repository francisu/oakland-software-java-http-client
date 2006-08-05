javac -classpath .\;http.jar HttpGetSample.java
java -Djava.net.preferIPv4Stack=true -cp .\;http.jar HttpGetSample $*
