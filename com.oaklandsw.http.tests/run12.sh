java -verbose -Dlog4j.configuration=../com.oaklandsw.build/src/logprops/ljp.none  -Doaklandsw.localhost=repoman -Djava.net.preferIPv4Stack=true -cp ../com.oaklandsw.http.tests/httptests.jar:../com.oaklandsw.http.tests.jars/jsse103.jar:../com.oaklandsw.http.tests.jars/junit.jar:../com.oaklandsw.http/http.jar com.oaklandsw.http.TestAll
