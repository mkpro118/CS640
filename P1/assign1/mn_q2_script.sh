c0 rm -fr latency*
c0 rm -fr throughput*
c0 echo "Starting latency measurements"

h1 ping 10.0.0.2 -c 20 > latency_L1.txt &
h3 ping 10.0.0.2 -c 20 > latency_L2.txt &
h4 ping 10.0.0.2 -c 20 > latency_L3.txt &
h5 ping 10.0.0.3 -c 20 > latency_L4.txt &
h6 ping 10.0.0.3 -c 20 > latency_L5.txt &

c0 echo "Waiting for latency measurements"
h1 wait $(jobs -p); echo "Link 1 done!"
h3 wait $(jobs -p); echo "Link 2 done!"
h4 wait $(jobs -p); echo "Link 3 done!"
h5 wait $(jobs -p); echo "Link 4 done!"
h6 wait $(jobs -p); echo "Link 5 done!"
c0 echo "Starting throughput measurements"

c0 echo "Booting up servers"
h2 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
h3 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5002 &
c0 echo "Servers up and running"

h1 echo "Running client on link 1"
h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.2 -p 5001 -t 20 > throughput_L1.txt

h3 echo "Running client on link 2"
h3 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.2 -p 5001 -t 20 > throughput_L2.txt

h4 echo "Running client on link 3"
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.3 -p 5002 -t 20 > throughput_L3.txt

h5 echo "Running client on link 4"
h5 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.3 -p 5002 -t 20 > throughput_L4.txt

h6 echo "Running client on link 5"
h6 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.3 -p 5002 -t 20 > throughput_L5.txt

c0 echo "Stopping servers"
h2 pkill java
h3 pkill java

c0 echo "Running H1 to H4 tests"
h1 ping 10.0.0.4 -c 20 > latency_Q2.txt &
h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5001 -t 20 > throughput_Q2.txt

h1 pkill java

