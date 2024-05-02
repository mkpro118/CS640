JC = javac
JV = java
BIN_DIR := bin
CP := -cp $(BIN_DIR)
FLAGS = -d $(BIN_DIR) $(CP)

CLASSES := bin/ArgParser.class\
           bin/PeriodicTask.class\
           bin/ITCPPacket.class\
           bin/TCPPacket.class\
           bin/SendConfig.class\
           bin/RecvConfig.class\
           bin/Sender.class\
           bin/TCPFlag.class

SENDER_OPTIONS := -p 23451 -s 127.0.01 -a 23450 -f in. -m 1500 -c 4
RECEIVER_OPTIONS := -p 23450 -m 1500 -c 4 -f out.txt

all : $(CLASSES) bin/TCPend.class
.PHONY : all

bin/ArgParser.class: ArgParser.java
	$(JC) $(FLAGS) ArgParser.java

bin/PeriodicTask.class: PeriodicTask.java
	$(JC) $(FLAGS) PeriodicTask.java

bin/ITCPPacket.class: ITCPPacket.java bin/TCPFlag.class
	$(JC) $(FLAGS) ITCPPacket.java

bin/TCPPacket.class: TCPPacket.java bin/TCPFlag.class
	$(JC) $(FLAGS) TCPPacket.java

bin/Receiver.class: Receiver.java
	$(JC) $(FLAGS) Receiver.java

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

sender:
	$(JV) $(CP) TCPend $(SENDER_OPTIONS)

receiver:
	$(JV) $(CP) TCPend $(RECEIVER_OPTIONS)

clean:
	rm -fr bin
