c0 rm -fr latency*
c0 rm -fr throughput*
c0 echo -e '\nStarting latency measurements\n'

h1 ping 10.0.0.2 -c 20 > latency_L1.txt &
h1 echo 'Pinging h2 from h1 '
h3 ping 10.0.0.2 -c 20 > latency_L2.txt &
h3 echo 'Pinging h2 from h3 '
h4 ping 10.0.0.3 -c 20 > latency_L3.txt &
h4 echo 'Pinging h2 from h4 '
h5 ping 10.0.0.2 -c 20 > latency_L4.txt &
h5 echo 'Pinging h3 from h5 '
h6 ping 10.0.0.3 -c 20 > latency_L5.txt &
h6 echo 'Pinging h3 from h6 '

c0 echo -e '\nWaiting for latency measurements'
h1 wait $(jobs -p); echo 'Link 1 done!'
h3 wait $(jobs -p); echo 'Link 2 done!'
h4 wait $(jobs -p); echo 'Link 3 done!'
h5 wait $(jobs -p); echo 'Link 4 done!'
h6 wait $(jobs -p); echo 'Link 5 done!'
c0 echo -e '\nStarting throughput measurements\n'

c0 echo -e 'Booting up Iperfer servers...\n'
h2 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
h2 echo 'Server on h2 is up!'
h3 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5002 &
h3 echo 'Server on h3 is up!'
c0 echo -e '\nAll servers up and running\n'

h1 echo -n 'Running Iperfer client on link 1'
h1 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.2 -p 5001 -t 20 > throughput_L1.txt
h1 echo -e ' Done!\n'

h3 echo -n 'Running Iperfer client on link 2'
h3 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.2 -p 5001 -t 20 > throughput_L2.txt
h3 echo -e ' Done!\n'

h4 echo -n 'Running Iperfer client on link 3'
h4 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.3 -p 5002 -t 20 > throughput_L3.txt
h4 echo -e ' Done!\n'

h5 echo -n 'Running Iperfer client on link 4'
h5 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.3 -p 5002 -t 20 > throughput_L4.txt
h5 echo -e ' Done!\n'

h6 echo -n 'Running Iperfer client on link 5'
h6 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.3 -p 5002 -t 20 > throughput_L5.txt
h6 echo -e 'Done!\n'

c0 echo -n 'Waiting for 5 seconds for servers to flush output '
c0 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh sleep 5

c0 echo -n -e '\nStopping servers... '
h2 pkill -INT java
h3 pkill -INT java
c0 echo -e 'Done!\n'

c0 echo -e 'Running H1 to H4 latency and throughput tests\n'
c0 echo -e 'Pinging h4 from h1 \n'
h1 ping 10.0.0.4 -c 20 > latency_Q2.txt

c0 echo -n 'Starting up Iperfer server on h1... '
h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
c0 echo -e 'Done!\n'

c0 echo -n 'Running Iperfer client on h4'
h4 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5001 -t 20 > throughput_Q2.txt
c0 echo -e ' Done!\n'

c0 echo -n 'Waiting for 5 seconds for servers to flush output '
c0 /home/mininet/Private/CS640/P1/assign1/prog_dots.sh sleep 5

c0 echo -n -e '\nStopping server on h1... '
h1 pkill -INT java
c0 echo -e 'Done\n'

c0 echo -e 'Link Latency and Throughput tests finished!\n'
