BUILDDIR := $(SRCROOT)/build/$(pkg)
dist_dir := $(SRCROOT)/build/dist

PY_TAG := $(shell $(PYTHON) --version 2>&1 | $(CUT) -d' ' -f1 | $(CUT) -d. -f1,2)
sdist := $(dist_dir)/$(pkg)-$(vers).tar.gz
bdist_egg := $(dist_dir)/$(pkg)-$(vers)-py$(PY_TAG).egg

all: $(sdist) $(bdist_egg)

# Helpers
sdist: $(sdist)
bdist_egg : $(bdist_egg)

clean:
	$(RM) -rf build *.egg*

$(bdist_egg): setup.py
	$(PYTHON) setup.py bdist_egg --dist-dir $(dist_dir)

$(sdist): setup.py
	$(PYTHON) setup.py sdist --dist-dir $(dist_dir)

.PHONY: clean all sdist bdist_egg
