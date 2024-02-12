c0 rm -fr answersL.txt

c0 echo -e '\nmeasuring latency for simultaneous communication between hosts..\n'

h8 ping 10.0.0.4 -c 20 -q > answersL.txt &
h8 echo 'pinging h4 from h8'
h7 ping 10.0.0.9 -c 20 -q >> annswersL.txt &
h7 echo 'pinging h9 from h7'
h1 ping 10.0.0.10 -c 20 -q >> answersL.txt &
h1 echo 'pinging h10 from h1'

c0 echo -e '\nlatency measured for simultaneous communcation between hosts\n'

c0 echo -e '\nWaiting for hosts to finish\n'
h8 wait $(jobs -p); echo 'h8 done!'
h7 wait $(jobs -p); echo 'h7 done!'
h1 wait $(jobs -p); echo 'h1 done!'

c0 echo -e '\nmeasuring throughput for simultaneous communication between hosts..n'

c0 echo -e 'Booting up Iperfer servers...\n'
h8 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
h8 echo 'Server on h8 is up!'
h7 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5002 &
h7 echo 'Server on h7 is up!'
h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5003 &
h1 echo 'Server on h1 is up!'
c0 echo -e '\nAll servers up and running\n'


h4 echo -n 'Running Iperfer client between h8 and h4 ... '
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.8 -p 5001 -t 20 > answersT.txt
h4 echo -e 'Done!\n'

h9 echo -n 'Running Iperfer client between h9 and h7 ... '
h9 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.7 -p 5002 -t 20 > answersT.txt
h9 echo -e 'Done!\n'

h10 echo -n 'Running Iperfer client between h10 and h1 ... '
h10 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5003 -t 20 > answersT.txt
h10 echo -e 'Done!\n'






