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
	rm $(TARGET_DIR)/*.class
