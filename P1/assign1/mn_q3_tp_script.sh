c0 echo -e '\nStarting Throughput tests\n'
c0 echo -e 'Booting up servers\n'

h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5004 > H4_LOGS &
h9 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5009 > H9_LOGS &

h4 echo 'Server on h4 is up!'
h9 echo 'Server on h9 is up!'

c0 echo -e '\nAll servers up and running'

c0 echo -e '\nRunning Tests: h1 -> h4  +  h7 -> h9 \n'
h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.4 -p 5004 -t 20 &
h7 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.9 -p 5009 -t 20 &
h1 echo 'Started client on h4'
h7 echo 'Started client on h9'

c0 echo -e '\nWaiting for tests to complete'
h1 wait; echo 'Client on h4 terminated'
h7 wait; echo 'Client on h9 terminated'

c0 echo -e -n '\nStarting additional server on h10... '
h10 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5010 > H10_LOGS &
c0 echo 'Done!'

c0 echo -e '\nRunning Tests: h4 -> h1  +  h9 -> h7  +  h10 -> h8 \n'
h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.4 -p 5004 -t 20 &
h7 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.9 -p 5009 -t 20 &
h8 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.10 -p 5010 -t 20 &
h4 echo 'Started client on h4'
h9 echo 'Started client on h9'
h10 echo 'Started client on h10'

c0 echo -e '\nWaiting for tests to complete'
h1 wait; echo 'Client on h1 terminated!'
h7 wait; echo 'Client on h7 terminated!'
h8 wait; echo 'Client on h8 terminated!'

c0 echo -e '\nThroughput Tests Finished!'
c0 echo -e '\nMultiplexing Tests Complete!'

h4 pkill -INT java
h9 pkill -INT java
h10 pkill -INT java
