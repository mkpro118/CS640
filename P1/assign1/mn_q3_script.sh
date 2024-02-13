c0 rm -fr MP*
c0 echo -e '\nStarting Multiplexing Tests\n'

c0 echo 'Starting Latency tests'

c0 echo -e 'Running Tests: h4 -> h1  +  h9 -> h7 \n'
h4 ping 10.0.0.1 -c 20 -q > MP_h4_h1_1 &
h9 ping 10.0.0.7 -c 20 -q > MP_h9_h7_1 &
h4 echo 'Ping started on h4'
h9 echo 'Ping started on h9'

c0 echo -e '\nWaiting for tests to complete'
h4 wait; echo 'Ping on h4 terminated!'
h9 wait; echo 'Ping on h9 terminated!'

c0 echo -e '\nRunning Tests: h4 -> h1  +  h9 -> h7  +  h10 -> h8 \n'
h4 ping 10.0.0.1 -c 20 -q > MP_h4_h1_2 &
h9 ping 10.0.0.7 -c 20 -q > MP_h9_h7_2 &
h10 ping 10.0.0.8 -c 20 -q > MP_h10_h8_2 &
h4 echo 'Ping started on h4'
h9 echo 'Ping started on h9'
h10 echo 'Ping started on h10'

c0 echo -e '\nWaiting for tests to complete'
h4 wait; echo 'Ping on h4 terminated!'
h9 wait; echo 'Ping on h9 terminated!'
h10 wait; echo 'Ping on h10 terminated!'

c0 echo -e '\nLatency Tests Finished!'
c0 echo -e '\nStarting Throughput tests\n'
c0 echo -e 'Booting up servers\n'

h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
h7 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5007 &
h8 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5008 &
h1 echo 'Server on h1 is up!'
h7 echo 'Server on h7 is up!'
h8 echo 'Server on h8 is up!'

c0 echo -e '\nAll servers up and running'

c0 echo -e '\nRunning Tests: h4 -> h1  +  h9 -> h7 \n'
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5001 -t 20 >> MP_h4_h1_1 &
h9 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.7 -p 5007 -t 20 >> MP_h9_h7_1 &
h4 echo 'Started client on h4'
h9 echo 'Started client on h9'

c0 echo -e '\nWaiting for tests to complete'
h4 wait; echo 'Client on h4 terminated'
h9 wait; echo 'Client on h9 terminated'

c0 echo -e '\nRunning Tests: h4 -> h1  +  h9 -> h7  +  h10 -> h8 \n'
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5001 -t 20 >> MP_h4_h1_2 &
h9 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.7 -p 5007 -t 20 >> MP_h9_h7_2 &
h10 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.8 -p 5008 -t 20 >> MP_h10_h8_2 &
h4 echo 'Started client on h4'
h9 echo 'Started client on h9'
h10 echo 'Started client on h10'

c0 echo -e '\nWaiting for tests to complete'
h4 wait; echo 'Client on h4 terminated!'
h9 wait; echo 'Client on h9 terminated!'
h10 wait; echo 'Client on h10 terminated!'

c0 echo -e '\nThroughput Tests Finished!'
c0 echo -e '\nMultiplexing Tests Complete!'

