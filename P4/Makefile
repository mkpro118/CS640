JC = javac
JV = java
BIN_DIR := src
CP := -cp $(BIN_DIR)
FLAGS = -d $(BIN_DIR) $(CP)

CLASSES := src/Option.class\
           src/ArgParser.class\
           src/PeriodicTask.class\
           src/IClient.class\
           src/IServer.class\
           src/ITCPPacket.class\
           src/TCPPacket.class\
           src/SendConfig.class\
           src/RecvConfig.class\
           src/Sender.class\
           src/Recv.class\
           src/TCPFlag.class

SENDER_OPTIONS := -p 23453 -s 127.0.01 -a 23452 -f tests/in.txt -m 500 -c 4
RECEIVER_OPTIONS := -p 23452 -m 500 -c 4 -f tests/out.txt

all : $(CLASSES) src/TCPend.class
.PHONY : all

src/Option.class: src/Option.java
	$(JC) $(FLAGS) src/Option.java

src/ArgParser.class: src/ArgParser.java
	$(JC) $(FLAGS) src/ArgParser.java

src/PeriodicTask.class: src/PeriodicTask.java
	$(JC) $(FLAGS) src/PeriodicTask.java

src/IClient.class: src/IClient.java
	$(JC) $(FLAGS) src/IClient.java

src/IServer.class: src/IServer.java
	$(JC) $(FLAGS) src/IServer.java

src/ITCPPacket.class: src/ITCPPacket.java src/TCPFlag.class
	$(JC) $(FLAGS) src/ITCPPacket.java

src/TCPPacket.class: src/TCPPacket.java src/TCPFlag.class
	$(JC) $(FLAGS) src/TCPPacket.java

src/Receiver.class: src/Receiver.java
	$(JC) $(FLAGS) src/Receiver.java

src/Recv.class: src/Recv.java
	$(JC) $(FLAGS) src/Recv.java

src/Sender.class: src/Sender.java
	$(JC) $(FLAGS) src/Sender.java

src/RecvConfig.class: src/RecvConfig.java
	$(JC) $(FLAGS) src/RecvConfig.java

src/SendConfig.class: src/SendConfig.java
	$(JC) $(FLAGS) src/SendConfig.java

src/TCPFlag.class: src/TCPFlag.java
	$(JC) $(FLAGS) src/TCPFlag.java

src/TCPend.class: src/TCPend.java $(CLASSES)
	$(JC) $(FLAGS) src/TCPend.java

sender: src/TCPend.class $(CLASSES)
	$(JV) $(CP) TCPend $(SENDER_OPTIONS)

receiver: src/TCPend.class $(CLASSES)
	$(JV) $(CP) TCPend $(RECEIVER_OPTIONS)

clean:
	rm -fr src
