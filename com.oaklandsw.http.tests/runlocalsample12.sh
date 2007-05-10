jikes -classpath .:http.jar:/usr/java/jdk1.2.2/jre/lib/rt.jar HttpGetSample.java
export LD_PRELOAD=/usr/java/jdk122waithack/libcwait.so
/usr/java/jdk1.2.2/jre/bin/i386/native_threads/java  -cp .:http.jar HttpGetSample $*
