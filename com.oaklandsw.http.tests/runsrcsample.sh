javac -classpath .:http.jar:jakarta-regexp-1.3.jar:commons-logging-1.1.jar HttpGetSample.java
java -Djava.net.preferIPv4Stack=true -cp .:http.jar:jakarta-regexp-1.3.jar:commons-logging-1.1.jar  HttpGetSample $*
