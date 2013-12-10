# VMware ThinApp Factory
# Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

include mk/defs.mk

# Used to build converter and dependent eggs.
TARGETS := cifsmount syscfg converter workpool utils

all: stage-deps $(TARGETS) stage-resources maven

$(TARGETS):
	$(MAKE) -C $@

CALL_MVN := $(MVN) $(MVN_FLAGS)

maven:
	$(CALL_MVN) install

clean:
	$(CALL_MVN) clean
	for subdir in $(TARGETS); do $(MAKE) -C $$subdir clean; done
	$(RM) -f manualmode/src/main/resources/setoption.exe
	$(RM) -f manualmode/src/main/resources/setproxy.exe
	$(RM) -rf build

install:
	$(EASY_INSTALL) build/dist/*.egg

stage-resources:
# Stage setoption.exe and setproxy.exe in manualmode resources
	$(CP) -f build/setoption.exe build/setproxy.exe manualmode/src/main/resources/

DEPS_UNZIPSFX := manualmode/src/main/resources/unzipsfx.exe
DEPS_VIJAVA := build/$(VIJAVA_JAR)

stage-deps: $(DEPS_UNZIPSFX) $(DEPS_VIJAVA)

clean-deps:
	$(RM) -f $(DEPS_UNZIPSFX)

$(DEPS_VIJAVA):
# Retrieves VIJava and runs 'mvn install' on it
	$(MKDIR) -p build
	if test -f "$(LOCAL_VIJAVA_ZIP)"; then \
	   $(UNZIP) -o "$(LOCAL_VIJAVA_ZIP)" -d build $(VIJAVA_JAR); \
	elif ! test -f "build/$(VIJAVA_ZIP)"; then \
	   echo "*** Attempting to download $(VIJAVA_ZIP_URL)"; \
	   if ! $(CURL) -s -o build/"$(VIJAVA_ZIP)" $(VIJAVA_ZIP_URL); then \
	      echo "Error: $(VIJAVA_ZIP) does not exist and cannot be downloaded. Update mk/defs.mk." >&2; \
	      exit 1; \
	   fi; \
	   echo "*** Download OK, extracting $(VIJAVA_ZIP)"; \
	   $(UNZIP) -o build/"$(VIJAVA_ZIP)" -d build $(VIJAVA_JAR); \
	fi

	$(CALL_MVN) install:install-file -DgroupId=net.sourceforge.vijava -DartifactId=vijava -Dversion=$(VIJAVA_VERSION) -Dpackaging=jar -Dfile="$@"

$(DEPS_UNZIPSFX):
# Retrieves the Windows version of unzipsfx.exe
# You probably only have the Linux one if you're here
	$(MKDIR) -p build
	@if test -f "$(LOCAL_UNZIPSFX_EXE)"; then \
	   $(CP) -f "$(LOCAL_UNZIPSFX_EXE)" $@; \
	elif ! test -f "build/$(UNZIPSFX_EXE)"; then \
	   echo "*** Attempting to download $(UNZIP_INSTALLER_URL)"; \
	   if ! $(CURL) -s -o build/"$(UNZIP_INSTALLER)" $(UNZIP_INSTALLER_URL); then \
	      echo "Error: $(UNZIPSFX_EXE) does not exist and cannot be downloaded. Update mk/defs.mk." >&2; \
	      exit 1; \
	   fi; \
	   echo "*** Download OK, extracting $(UNZIPSFX_EXE)"; \
	   $(UNZIP) -o build/"$(UNZIP_INSTALLER)" -d build $(UNZIPSFX_EXE) && \
	   $(CP) -f build/$(UNZIPSFX_EXE) $@; \
	fi

setup: setup-real deploy

setup-real:
	if [ ! -e setup.cfg ]; then echo "Please read and revise setup.cfg.example and then save it as setup.cfg."; exit 1; fi
	$(SHELL) setup.sh setup.cfg

deploy:
	if [ ! -e setup.cfg ]; then echo "Please read and revise setup.cfg.example and then save it as setup.cfg."; exit 1; fi
	$(SHELL) deploy.sh setup.cfg

.PHONY: all stage-resources stage-deps clean-deps maven setup setup-real deploy $(TARGETS)
