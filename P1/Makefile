SOURCE_DIR := .
TARGET_DIR := bin

SRCS := $(wildcard $(SOURCE_DIR)/*.java)
CLS := $(SRCS:$(SOURCE_DIR)/%.java=$(TARGET_DIR)/%.class)

JC := javac
JV := java
JCFLAGS := -d $(TARGET_DIR)/ -cp $(SOURCE_DIR)/ -Wall -Xlint:all
JVFLAGS := -cp $(SOURCE_DIR)/

IPERFER := Iperfer

.SUFFIXES: .java

.PHONY: all clean

all: $(CLS)

$(CLS): $(TARGET_DIR)/%.class: $(SOURCE_DIR)/%.java
	$(JC) $(JCFLAGS) $<

run:
	JV $(JVFLAGS)/$(IPERFER)

clean:
	rm $(TARGET_DIR)/*.class
