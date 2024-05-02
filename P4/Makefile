JC = javac
JV = java
BIN_DIR := .
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

all : $(CLASSES) bin/TCPend.class
.PHONY : all

bin/Option.class: Option.java
	$(JC) $(FLAGS) Option.java

bin/ArgParser.class: ArgParser.java
	$(JC) $(FLAGS) ArgParser.java

bin/PeriodicTask.class: PeriodicTask.java
	$(JC) $(FLAGS) PeriodicTask.java

bin/IClient.class: IClient.java
	$(JC) $(FLAGS) IClient.java

bin/IServer.class: IServer.java
	$(JC) $(FLAGS) IServer.java

bin/ITCPPacket.class: ITCPPacket.java bin/TCPFlag.class
	$(JC) $(FLAGS) ITCPPacket.java

bin/TCPPacket.class: TCPPacket.java bin/TCPFlag.class
	$(JC) $(FLAGS) TCPPacket.java

bin/Receiver.class: Receiver.java
	$(JC) $(FLAGS) Receiver.java

bin/Recv.class: Recv.java
	$(JC) $(FLAGS) Recv.java

bin/Sender.class: Sender.java
	$(JC) $(FLAGS) Sender.java

bin/RecvConfig.class: RecvConfig.java
	$(JC) $(FLAGS) RecvConfig.java

bin/SendConfig.class: SendConfig.java
	$(JC) $(FLAGS) SendConfig.java

bin/TCPFlag.class: TCPFlag.java
	$(JC) $(FLAGS) TCPFlag.java

bin/TCPend.class: TCPend.java $(CLASSES)
	$(JC) $(FLAGS) TCPend.java

sender: bin/TCPend.class $(CLASSES)
	$(JV) $(CP) TCPend $(SENDER_OPTIONS)

receiver: bin/TCPend.class $(CLASSES)
	$(JV) $(CP) TCPend $(RECEIVER_OPTIONS)

clean:
	rm -fr bin
