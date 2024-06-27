# CS640

## Table of contents

- [P1: Sockets, Mininet, and Performance](#p1-sockets-mininet-and-performance)
- [P2: Link and Network Layer Forwarding](#p2-link-and-network-layer-forwarding)
- [P3: Routing](#p3-routing)
- [P4: Transport Layer](#p4-transport-layer)

## P1: Sockets, Mininet, and Performance

Replicate `iperf` in Java to measure network bandwidth between a pair of hosts using TCP sockets.

### Usage:[^order]

To operate Iperfer in client mode, it should be invoked as follows:

```bash
java Iperfer -c -h <server_hostname> -p <server_port> -t <time>
```

- `-c` indicates this is the client which should generate data.
- `server_hostname` is the hostname or IP address of the iperf server which will consume data.
- `server_port` is the port on which the remote host is waiting to consume data; the port should be in the range 1024 ≤ server port ≤ 65535.
- `time` is the duration in seconds for which data should be generated.

To operate Iperfer in server mode, it should be invoked as follows:

```bash
java Iperfer -s -p <listen_port>
```

- `-s` indicates this is the iperf server which should consume data
- `listen_port` is the port on which the host is waiting to consume data; the port should be in the range 1024 ≤ listen port ≤ 65535.

## P2: Link and Network Layer Forwarding

Implement the forwarding behavior of a switch and a router.

### Switches

Implement a learning switch which forwards packets at the link layer based on destination MAC addresses.\
Uses the flood fill algorithm and a soft-state cache with timeout-based entries.

### Routers

Implement a router which forwards packets at the network layer based on destination IP addresses.\
Uses a predetermined static routing table and ARP cache.\
Does not implement sending an ICMP response on error.

## P3: Routing

Modify the virtual router from [P2](#p2-link-and-network-layer-forwarding) to build a routing table using distance vector routing.\
With this change, the virtual router will no longer depend on a static route table.

Specifically, implement a simplified version of the [Routing Information Protocol v2](https://www.rfc-editor.org/rfc/rfc2453.txt) (RIPv2).

## P4: Transport Layer

Implement a Transmission Control Protocol (TCP) which should incorporate only the following features on top of the unreliable UDP sockets.:
- Reliability (with proper re-transmissions in the event of packet losses / corruption)
- Data Integrity (with checksums)
- Connection Management (SYN and FIN)
- Optimizations (fast retransmit on 3 or more duplicate ACKs)

### Usage:[^order]
  
To operate TCPend in sender mode

```bash
java TCPend -p <port> -s <remote_ip> -a <remote_port> -f <file_name> -m <mtu> -c <sws>
```

- `port`: Port number at which the client will run.
- `remote_ip`: The IP address of the remote receiver.
- `remote_port`: The port at which the remote receiver is running.
- `file_name`: The file to be sent.
- `mtu`: Maximum Transmission Unit in bytes.[^mtu]
- `sws`: Sliding Window Size in number of segments.

To opearate TCPend in receiver mode

```bash
java TCPend -p <port> -m <mtu> -c <sws> -f <file_name>
```

- `port`: Port number at which the receiver will listen at.
- `file_name`: The path where the incoming file should be written.
- `mtu`: Maximum Transmission Unit in bytes.[^mtu]
- `sws`: Sliding Window Size in number of segments.

[^order]: *Note*: While the assignment specifications state that the arguments must follow the order specified by the usage description, we developed a flexible argument parser that does not enforce this. For all the tools that take command line arguments, they may be passed in any order.
[^mtu]: Do not use a value larger than 1430 for the MTU if your network does not support Ethernet Jumbo frame. If you do not know what that means, your network likely does not support it.
