SOURCE_DIR := .
TARGET_DIR := bin

SRCS := $(wildcard $(SOURCE_DIR)/*.java)
CLS := $(SRCS:$(SOURCE_DIR)/%.java=$(TARGET_DIR)/%.class)

JC := javac
JV := java
JCFLAGS := -d $(TARGET_DIR)/ -cp $(SOURCE_DIR)/ -Werror -Xlint:all
JVFLAGS := -cp $(TARGET_DIR)

IPERFER := Iperfer
IPERFER_CLIENT_ARGS := -c -h 127.0.0.1 -p 5001 -t 10
IPERFER_SERVER_ARGS := -s -p 5001

PY := python
MN_SCRIPT := assign1/assign1_topo.py

.SUFFIXES: .java

.PHONY: all clean

all: $(CLS)

$(CLS): $(SRCS)
	$(JC) $(JCFLAGS) $(SRCS)

server: all
	$(JV) $(JVFLAGS) $(IPERFER) $(IPERFER_SERVER_ARGS)

client: all
	$(JV) $(JVFLAGS) $(IPERFER) $(IPERFER_CLIENT_ARGS)

clean:
	rm -fr $(TARGET_DIR)/*.class
	rm -fr latency*
	rm -fr throughput*
	rm -fr MP*

mn: all
	sudo $(PY) $(MN_SCRIPT)

q2: all
	sudo $(PY) $(MN_SCRIPT) q2

q3: all
	sudo $(PY) $(MN_SCRIPT) q3

q4: all
	sudo $(PY) $(MN_SCRIPT) q4

qs: all
	sudo $(PY) $(MN_SCRIPT) q2 q3 q4
