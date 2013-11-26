SRCROOT ?= ..

include $(SRCROOT)/mk/defs.mk

package:
	(cd .. && $(MVN) -o --settings settings.xml reactor:make -Dmake.folders=tests -Dmake.goals=package,-Dmaven.test.skip=true,-o,--settings=settings.xml)

deploy:
	$(MVN) --settings ../settings.xml deploy -Dmaven.test.skip=true

.PHONY: deploy package
.DEFAULT_GOAL := package
