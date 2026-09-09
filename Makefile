JAVAC ?= javac
BUILD := build
SOURCES := $(wildcard src/*.java)

.PHONY: all clean
all:
	mkdir -p $(BUILD)
	$(JAVAC) -d $(BUILD) $(SOURCES)

clean:
	rm -rf $(BUILD)
