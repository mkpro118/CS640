c0 echo "Starting Multiplexing Tests"

c0 echo "Booting up servers"

h1 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5001 &
h7 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5007 &
h8 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -s -p 5008 &

c0 echo "Servers up and running"

c0 echo "Running Tests: h4 -> h1  +  h9 -> h7"
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5001 -t 20 > MP_h4_h1_1 &
h9 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.7 -p 5007 -t 20 > MP_h9_h7_1 &

c0 echo "Waiting for tests to complete"
h4 wait ($jobs -p)
h9 wait ($jobs -p)

c0 echo "Running Tests: h4 -> h1  +  h9 -> h7 + h10 -> h8"
h4 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.1 -p 5001 -t 20 > MP_h4_h1_2 &
h9 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.7 -p 5007 -t 20 > MP_h9_h7_2 &
h10 java -cp /home/mininet/Private/CS640/P1/bin/ Iperfer -c -h 10.0.0.8 -p 5008 -t 20 > MP_h10_h8_2 &

c0 echo "Waiting for tests to complete"
h4 wait ($jobs -p)
h9 wait ($jobs -p)
h10 wait ($jobs -p)

c0 echo "Multiplexing Tests Complete!"
