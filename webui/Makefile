all: war-all

SRCROOT ?= ..
include $(SRCROOT)/mk/defs.mk
include $(SRCROOT)/support/gobuild/make/auto-components.mk

WAR_VERSION := $(PRODUCT_VERSION)
WAR_NAME := webui
WAR_OUT := $(WAR_NAME)-$(WAR_VERSION).war
WAR_DIR := $(BUILDROOT)/$(OBJDIR)/$(WAR_NAME)
MVN_SETTINGS := --settings $(CURDIR)/../settings.xml
MVN_FLAGS := -o $(MVN_SETTINGS) -Ddirectory=$(WAR_DIR) -Dmaven.test.skip=true

BUILD_RESOURCES := $(BUILDROOT)/$(OBJDIR)/resources

pom := pom.xml

setup_exe := $(BUILD_RESOURCES)/setup.exe
ifndef REMOTE_COPY_SCRIPT
setup_exe_deps := $(SRCROOT)/bin/setup.exe
endif

$(pom): pom.xml.in $(SRCROOT)/support/gobuild/specs/appfactory.py Makefile
	$(XML) ed -N x="http://maven.apache.org/POM/4.0.0" \
		-u "/x:project/x:properties/x:version" \
		-v "$(WAR_VERSION)" \
		-u "/x:project/x:properties/x:build.nonFilteredResources" \
		-v "$(BUILD_RESOURCES)" \
		-u "/x:project/x:properties/x:build.productVersion" \
		-v "$(PRODUCT_VERSION)" \
		-u "/x:project/x:properties/x:build.number" \
		-v "$(BUILD_NUMBER)" \
		-u "/x:project/x:properties/x:build.devModeFlag" \
		-v "false" \
			< $< > $@

$(setup_exe): $(setup_exe_deps)
	mkdir -p $(dir $@)
ifdef REMOTE_COPY_SCRIPT
	@echo "Copying setup.exe from Windows build"
	$(REMOTE_COPY_SCRIPT) download setup.exe $@
else
	@echo "*****************************************************************" >&2
	@echo "WARNING: Using LOCAL setup.exe!! For proper testing, use sandbox!" >&2
	@echo "*****************************************************************" >&2
	sleep 2
	cp -f $< $@
endif

war-all: $(pom) $(setup_exe)
	$(MVN) $(MVN_FLAGS) -f parent/pom.xml package
	mkdir -p $(GOBUILD_OUTPUT)/dist
	cp -fr $(BUILDROOT)/$(OBJDIR)/$(WAR_NAME)/$(WAR_OUT) $(GOBUILD_OUTPUT)/dist

.PHONY: all war-all
