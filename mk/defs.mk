# Paths to build utilities
# They can either be in your PATH, or specified with full paths here.
CC := i686-w64-mingw32-gcc
STRIP := i686-w64-mingw32-strip
RM := rm
CUT := cut
CP := cp
MKDIR := mkdir
CURL := curl
PYTHON := python
EASY_INSTALL := easy_install
UNZIP := unzip
ISOINFO := isoinfo
MVN := mvn

# Tests are skipped right now because they do not work.
# Additionally, by default we access online repositories to fetch
# POM dependencies. If you use your own repository, remove
# the -DuseOnlineRepositories=true flag.
MVN_FLAGS := -Dmaven.test.skip=true -DuseOnlineRepositories=true

# This config variable shouldn't need to be changed. It points to the freeware
# Info-ZIP unzip binary installer. We get unzipsfx.exe from it for use in
# Manual Mode. Alternatively, if LOCAL_UNZIPSFX_EXE is set and exists, the
# download step is skipped.
UNZIPSFX_EXE := unzipsfx.exe
LOCAL_UNZIPSFX_EZE :=

# We unzip unzipsfx.exe from this
UNZIP_INSTALLER := unz600xn.exe
UNZIP_INSTALLER_URL := ftp://ftp.info-zip.org/pub/infozip/win32/$(UNZIP_INSTALLER)

# This config variable is used to grab the open-source VIJava, which doesn't
# exist in Maven repos. You can set LOCAL_VIJAVA_ZIP here just as you do with
# LOCAL_UNZIPSFX_EXE above.
VIJAVA_VERSION := 2120100824
VIJAVA_ZIP := vijava$(VIJAVA_VERSION).zip
VIJAVA_JAR := $(VIJAVA_ZIP:.zip=.jar)
LOCAL_VIJAVA_ZIP :=

VIJAVA_ZIP_URL := http://superb-dca2.dl.sourceforge.net/project/vijava/vijava/VI%20Java%20API%202.1/$(VIJAVA_ZIP)
