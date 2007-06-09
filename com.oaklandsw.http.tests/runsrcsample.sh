javac -classpath .:http.jar examples/HttpGetSample.java
cp examples/HttpGetSample.class .
java -Djava.net.preferIPv4Stack=true -cp .:http.jar  HttpGetSample $*
