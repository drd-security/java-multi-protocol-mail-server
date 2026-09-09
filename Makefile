JAVAC ?= javac
JAVA ?= java
SRC := $(wildcard src/*.java)
BUILD := build

.PHONY: all run clean
all:
	mkdir -p $(BUILD)
	$(JAVAC) -d $(BUILD) $(SRC)

run: all
	$(JAVA) -cp $(BUILD) MailServer example.test 20

clean:
	rm -rf $(BUILD)
