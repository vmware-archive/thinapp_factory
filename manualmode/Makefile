SRCROOT ?= ..

include $(SRCROOT)/mk/defs.mk
include $(SRCROOT)/support/gobuild/make/auto-components.mk

MVN_SETTINGS := --settings $(CURDIR)/../settings.xml
MVN_FLAGS := -o $(MVN_SETTINGS) -Dmaven.test.skip=true

version := 1.0-SNAPSHOT
parent_pom := ../defs/pom.xml
non_filtered_resources := $(BUILDROOT)/resources/nonfiltered
setproxy_exe := $(BUILDROOT)/resources/nonfiltered/setproxy.exe
setoption_exe := $(BUILDROOT)/resources/nonfiltered/setoption.exe
unzipsfx_exe := $(BUILDROOT)/resources/nonfiltered/unzipsfx.exe
backupeventlogs_vbs := $(BUILDROOT)/resources/nonfiltered/backupeventlogs.vbs
ifndef REMOTE_COPY_SCRIPT
setproxy_deps := bin/setproxy.exe
setoption_deps := bin/setoption.exe
endif

all: deploy

installer := $(notdir $(wildcard $(GOBUILD_THINAPP_ROOT)/VMware-ThinApp-Enterprise-*.exe))
installer_dir := $(GOBUILD_THINAPP_ROOT)

src := $(shell $(FIND) src -type f)
src += Makefile

$(parent_pom): ../defs/pom.xml.in
	$(CP) -f $< $@

pom.xml: pom.xml.in Makefile
	$(XML) ed -N x="http://maven.apache.org/POM/4.0.0" \
		-u "/x:project/x:properties/x:build.nonFilteredResources" \
		-v $(non_filtered_resources) < $< > $@

$(setproxy_exe): $(setproxy_deps)
	$(MKDIR) -p $(dir $@)
ifdef REMOTE_COPY_SCRIPT
	@echo "Copying setproxy.exe from Windows build"
	$(REMOTE_COPY_SCRIPT) download setproxy.exe $@
else
	@echo "*****************************************************************" >&2
	@echo "WARNING: Using LOCAL setproxy.exe!! For proper testing, use sandbox!" >&2
	@echo "*****************************************************************" >&2
	$(CP) -f $< $@
endif

$(setoption_exe): $(setoption_deps)
	$(MKDIR) -p $(dir $@)
ifdef REMOTE_COPY_SCRIPT
	@echo "Copying setoption.exe from Windows build"
	$(REMOTE_COPY_SCRIPT) download setoption.exe $@
else
	@echo "*****************************************************************" >&2
	@echo "WARNING: Using LOCAL setoption.exe!! For proper testing, use sandbox!" >&2
	@echo "*****************************************************************" >&2
	$(CP) -f $< $@
endif

$(unzipsfx_exe): $(SRCROOT)/toolchain/win32/unzip-6.0/unzipsfx.exe
	$(MKDIR) -p $(dir $@)
	$(CP) -f $< $@

$(backupeventlogs_vbs): bin/backupeventlogs.vbs
	$(MKDIR) -p $(dir $@)
	$(CP) -f $< $@

deploy: $(GOBUILD_OUTPUT) $(parent_pom) pom.xml $(setproxy_exe) $(setoption_exe) $(unzipsfx_exe) $(backupeventlogs_vbs)
	(cd parent && $(MVN) $(MVN_FLAGS) package -Dmaven.javadoc.skip=true)
	mkdir -p $(GOBUILD_OUTPUT)/dist
	$(CP) web/target/manualmode-web-$(version).war $(GOBUILD_OUTPUT)/dist

eclipse: pom.xml
	(cd parent && $(MVN) eclipse:eclipse -DdownloadSources=true -DdownloadJavadocs=true -Dwtpversion=2.0)

clean:
	$(RM) -f $(war_output)
	(cd parent && $(MVN) $(MVN_FLAGS) clean)
	$(RM) -f pom.xml

.PHONY: all clean war eclipse deploy
.DEFAULT_GOAL := all
