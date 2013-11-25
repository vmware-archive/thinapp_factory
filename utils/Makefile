SRCROOT := ..
include $(SRCROOT)/mk/defs.mk

BUILDDIR := $(SRCROOT)/build
CXXFLAGS := -DUNICODE
SOURCES := setproxy.cpp setoption.cpp
LIBS := -lwininet
BINARIES := $(SOURCES:.cpp=.exe)

%.exe: %.cpp
	$(CXX) $(CXXFLAGS) $< -o $(BUILDDIR)/$@ $(LIBS)

all: $(BINARIES)

clean:
	$(RM) -f $(BINARIES)

.PHONY: all clean
