c0 rm -fr *_LOGS
c0 echo -e '\nStarting Multiplexing Tests\n'

c0 echo 'Starting Latency tests'

c0 echo -e '\n2 Pair Ping\n' > H4_LATENCY_LOGS
c0 echo -e '\n2 Pair Ping\n' > H9_LATENCY_LOGS

c0 echo -e 'Running Tests: h4 -> h1  +  h9 -> h7 \n'
h4 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh ping 10.0.0.1 -c 20 -q >> H4_LATENCY_LOGS &
h9 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh ping 10.0.0.7 -c 20 -q >> H9_LATENCY_LOGS &
h4 echo 'Ping started on h4'
h9 echo 'Ping started on h9'

c0 echo -e '\nWaiting for tests to complete'
h4 wait; echo 'Ping on h4 terminated!'
h9 wait; echo 'Ping on h9 terminated!'

c0 echo -e '\n3 Pair Ping\n' >> H4_LATENCY_LOGS
c0 echo -e '\n3 Pair Ping\n' >> H9_LATENCY_LOGS

c0 echo -e '\nRunning Tests: h4 -> h1  +  h9 -> h7  +  h10 -> h8 \n'
h4 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh ping 10.0.0.1 -c 20 -q >> H4_LATENCY_LOGS &
h9 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh ping 10.0.0.7 -c 20 -q >> H9_LATENCY_LOGS &
h10 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh ping 10.0.0.8 -c 20 -q >> H10_LATENCY_LOGS &
h4 echo 'Ping started on h4'
h9 echo 'Ping started on h9'
h10 echo 'Ping started on h10'

c0 echo -e '\nWaiting for tests to complete'
h4 wait; echo 'Ping on h4 terminated!'
h9 wait; echo 'Ping on h9 terminated!'
h10 wait; echo 'Ping on h10 terminated!'

c0 echo -n 'Waiting for 5 seconds for servers to flush output '
c0 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh sleep 5

c0 echo -e '\nLatency Tests Finished!'

c0 echo -e '\n2 Pair Iperfer\n' > H4_SERVER_LOGS
c0 echo -e '\n2 Pair Iperfer\n' > H9_SERVER_LOGS
c0 echo -e '\n2 Pair Iperfer\n' > H10_SERVER_LOGS

c0 echo -e '\nStarting Throughput tests\n'
c0 echo -e 'Booting up servers\n'

h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5004 >> H4_SERVER_LOGS &
h9 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5009 >> H9_SERVER_LOGS &

h4 echo 'Server on h4 is up!'
h9 echo 'Server on h9 is up!'

c0 echo -e '\nAll servers up and running'

c0 echo -e '\nRunning Tests: h1 -> h4  +  h7 -> h9 \n'
h1 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.4 -p 5004 -t 20 &
h7 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.9 -p 5009 -t 20 &
h1 echo 'Started client on h4'
h7 echo 'Started client on h9'

c0 echo -e '\nWaiting for tests to complete'
h1 wait; echo 'Client on h4 terminated'
h7 wait; echo 'Client on h9 terminated'

c0 echo -n 'Waiting for 5 seconds for servers to flush output '
c0 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh sleep 5

c0 echo -e '\n3 Pair Iperfer\n' >> H4_LOGS
c0 echo -e '\n3 Pair Iperfer\n' >> H9_LOGS
c0 echo -e '\n3 Pair Iperfer\n' >> H10_LOGS

c0 echo -e -n '\nStarting additional server on h10... '
h10 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5010 > H10_LOGS &
c0 echo 'Done!'

c0 echo -e '\nRunning Tests: h4 -> h1  +  h9 -> h7  +  h10 -> h8 \n'
h1 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.4 -p 5004 -t 20 &
h7 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.9 -p 5009 -t 20 &
h8 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.10 -p 5010 -t 20 &
h4 echo 'Started client on h4'
h9 echo 'Started client on h9'
h10 echo 'Started client on h10'

c0 echo -e '\nWaiting for tests to complete'
h1 wait; echo 'Client on h1 terminated!'
h7 wait; echo 'Client on h7 terminated!'
h8 wait; echo 'Client on h8 terminated!'

c0 echo -e '\nThroughput Tests Finished!'
c0 echo -e '\nMultiplexing Tests Complete!'

c0 echo -n 'Waiting for 10 seconds for servers to flush output '
c0 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh sleep 10

h4 pkill -INT java
h9 pkill -INT java
h10 pkill -INT java
