c0 rm -fr latency_h*
c0 rm -fr throughput_h*

h1 ping 10.0.0.4 -c 20 -q > latency_h1-h4.txt &
h1 echo 'pinging h4 from h1'
h5 ping 10.0.0.6 -c 20 -q > latency_h5-h6.txt &
h5 echo 'pinging h6 from h5'

c0 echo -e '\nWaiting for hosts to finish\n'
h5 wait $(jobs -p); echo 'h5 done!'
h1 wait $(jobs -p); echo 'h1 done!'

c0 echo -e 'Booting up Iperfer servers...\n'
h5 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
h5 echo 'Server on h5 is up!'
h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5002 &
h1 echo 'Server on h1 is up!'

h6 echo -n 'Running Iperfer client between h6 and h5 ... '
h6 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.5 -p 5001 -t 20 > throughput_h1-h4.txt &

h4 echo -n 'Running Iperfer client between h4 and h1 ... '
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5002 -t 20 > throughput_h5-h6.txt &

c0 echo -e '\nWaiting for hosts to finish\n'
h5 wait $(jobs -p); echo 'h5 done!'
h1 wait $(jobs -p); echo 'h1 done!'

c0 echo -n 'Stopping server on h5... '
h5 pkill java
c0 echo -e 'Done\n'

c0 echo -n 'Stopping server on h1... '
h1 pkill java
c0 echo -e 'Done\n'

