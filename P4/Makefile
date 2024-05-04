JC = javac
JV = java
BIN_DIR := src
CP := -cp $(BIN_DIR)
FLAGS = -d $(BIN_DIR) $(CP)

CLASSES := src/Option.class\
           src/ArgParser.class\
           src/PeriodicTask.class\
           src/Metrics.class\
           src/IClient.class\
           src/IServer.class\
           src/ITCPPacket.class\
           src/TCPPacket.class\
           src/SendConfig.class\
           src/RecvConfig.class\
           src/Sender.class\
           src/Recv.class\
           src/TCPFlag.class

SENDER_OPTIONS := -p 23453 -s 127.0.01 -a 23452 -f src/tests/in.txt -m 500 -c 4
RECEIVER_OPTIONS := -p 23452 -m 500 -c 4 -f src/tests/out.txt

all : $(CLASSES) src/TCPend.class
.PHONY : all

src/Option.class:
	$(JC) $(FLAGS) src/Option.java

src/ArgParser.class:
	$(JC) $(FLAGS) src/ArgParser.java

src/PeriodicTask.class:
	$(JC) $(FLAGS) src/PeriodicTask.java

src/Metrics.class:
	$(JC) $(FLAGS) src/Metrics.java

src/IClient.class:
	$(JC) $(FLAGS) src/IClient.java

src/IServer.class:
	$(JC) $(FLAGS) src/IServer.java

src/ITCPPacket.class: src/TCPFlag.class
	$(JC) $(FLAGS) src/ITCPPacket.java

src/TCPPacket.class: src/TCPFlag.class
	$(JC) $(FLAGS) src/TCPPacket.java

src/Receiver.class:
	$(JC) $(FLAGS) src/Receiver.java

src/Recv.class:
	$(JC) $(FLAGS) src/Recv.java

src/Sender.class:
	$(JC) $(FLAGS) src/Sender.java

src/RecvConfig.class:
	$(JC) $(FLAGS) src/RecvConfig.java

src/SendConfig.class:
	$(JC) $(FLAGS) src/SendConfig.java

src/TCPFlag.class:
	$(JC) $(FLAGS) src/TCPFlag.java

src/TCPend.class: $(CLASSES)
	$(JC) $(FLAGS) src/TCPend.java

sender: src/TCPend.class $(CLASSES)
	$(JV) $(CP) TCPend $(SENDER_OPTIONS)

receiver: src/TCPend.class $(CLASSES)
	$(JV) $(CP) TCPend $(RECEIVER_OPTIONS)

clean:
	rm -fr src/*.class
