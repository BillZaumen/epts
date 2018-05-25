#
# GNU Make file.
#

DATE = $(shell date -R)

#
# Set this if  'make install' should install its files into a
# user directory - useful for package systems that will grab
# all the files they see.  Setting this will allow a package
# to be built without requiring root permissions.
#
DESTDIR :=

JROOT := $(shell while [ ! -d src -a `pwd` != / ] ; do cd .. ; done ; pwd)

include VersionVars.mk


CLASSES = $(JROOT)/classes

APPS_DIR = apps
MIMETYPES_DIR = mimetypes

#
# System directories (that contains JAR files, etc.)
#
SYS_BIN = /usr/bin
SYS_MANDIR = /usr/share/man
SYS_DOCDIR = /usr/share/doc/epts
SYS_MIMEDIR = /usr/share/mime
SYS_APPDIR = /usr/share/applications
SYS_ICON_DIR = /usr/share/icons/hicolor
SYS_APP_ICON_DIR = $(SYS_ICON_DIR)/scalable/$(APPS_DIR)
SYS_MIME_ICON_DIR =$(SYS_ICON_DIR)/scalable/$(MIMETYPES_DIR)
SYS_JARDIRECTORY = /usr/share/java
SYS_BZDEVDIR = /usr/share/bzdev

ICON_WIDTHS = 16 20 22 24 32 36 48 64 72 96 128 192 256

# Target JARDIRECTORY - where 'make install' actually puts the jar
# file (DESTDIR is not null when creating packages)
#
JARDIRECTORY = $(DESTDIR)$(SYS_JARDIRECTORY)
#
# JARDIRECTORY modified so that it can appear in a sed command
#
JARDIR=$(shell echo $(SYS_JARDIRECTORY) | sed  s/\\//\\\\\\\\\\//g)

# Other target directories

BIN = $(DESTDIR)$(SYS_BIN)
MANDIR = $(DESTDIR)$(SYS_MANDIR)
DOCDIR = $(DESTDIR)$(SYS_DOCDIR)
MIMEDIR = $(DESTDIR)$(SYS_MIMEDIR)
APPDIR = $(DESTDIR)$(SYS_APPDIR)
MIME_ICON_DIR = $(DESTDIR)$(SYS_MIME_ICON_DIR)
# Icon directory for applications
#
APP_ICON_DIR = $(DESTDIR)$(SYS_APP_ICON_DIR)
ICON_DIR = $(DESTDIR)$(SYS_ICON_DIR)

# Full path name of for where epts.desktop goes
#
# APPDIR = $(DESTDIR)/usr/share/applications

# Installed name of the icon to use for the EPTS application
#
SOURCEICON = icons/epts.svg
TARGETICON = epts.svg
TARGETICON_PNG = epts.png

# Installed names of icons to use for epts document types
# (originals in Icons subdirectory)
#
SOURCE_FILE_ICON = icons/eptsfile.svg
TARGET_FILE_ICON = application-x.epts+xml.svg
TARGET_FILE_ICON_PNG = application-x.epts+xml.png


JROOT_DOCDIR = $(JROOT)$(SYS_DOCDIR)
JROOT_JARDIR = $(JROOT)/jar
JROOT_MANDIR = $(JROOT)/man
JROOT_BIN = $(JROOT)/bin

EXTDIR = $(SYS_JARDIRECTORY)
BZDEVDIR = $(SYS_BZDEVDIR)

EXTLIBS=$(EXTDIR)/libbzdev.jar


MANS = $(JROOT_MANDIR)/man1/epts.1.gz $(JROOT_MANDIR)/man5/epts.5.gz


ICONS = $(SOURCEICON) $(SOURCE_FILE_ICON)

JFILES = $(wildcard src/*.java)
PROPERTIES = src/EPTS.properties
TEMPLATES = templates/ECMAScript.tpl templates/save.tpl \
	templates/ECMAScriptPaths.tpl templates/ECMAScriptLocations.tpl \
	templates/ECMAScriptLayers.tpl templates/SegmentsCSV.tpl \
	templates/SVG.tpl
RESOURCES = manual/manual.xml \
	manual/manual.xsl \
	manual/index.html \
	manual/manual.html \
	manual/manual.css \
	epts-1.0.dtd

FILES = $(JFILES) $(PROPERTIES)

PROGRAM = $(JROOT_BIN)/epts $(JROOT_JARDIR)/epts-$(VERSION).jar 
ALL = $(PROGRAM) epts.desktop $(MANS) $(JROOT_BIN)/epts

# program: $(JROOT_BIN)/epts $(JROOT_JARDIR)/epts-$(VERSION).jar 

program: clean $(PROGRAM)

#
# Before using, set up a symbolic link for bzdevlib.jar in the ./jar directory.
# This is useful for testing that requires modifying files in bzdev-jlib.
#
testversion:
	make program EXTDIR=$(JROOT_JARDIR)

all: $(ALL)

include MajorMinor.mk

$(CLASSES):
	(cd $(JROOT); mkdir classes)

# The action for this rule removes all the epts-*.jar files
# because the old ones would otherwise still be there and end up
# being installed.
#
$(JROOT_JARDIR)/epts-$(VERSION).jar: $(FILES) $(TEMPLATES) $(RESOURCES)
	mkdir -p $(CLASSES)
	javac -Xlint:unchecked -Xlint:deprecation \
		-d $(CLASSES) -classpath $(CLASSES):$(EXTLIBS) \
		-sourcepath src $(JFILES)
	cp $(PROPERTIES) $(CLASSES)
	for i in $(ICON_WIDTHS) 512 ; do \
		inkscape -w $$i -e $(CLASSES)/eptsicon$${i}.png \
		icons/epts.svg ; \
	done
	mkdir -p $(JROOT_JARDIR)
	rm -f $(JROOT_JARDIR)/epts-*.jar
	cp templates/*.tpl $(CLASSES)
	mkdir -p $(CLASSES)/manual
	for i in $(RESOURCES) ; do cp $$i $(CLASSES)/$$i ; done
	jar cfm $(JROOT_JARDIR)/epts-$(VERSION).jar epts.mf \
		-C $(CLASSES) .
	( cd jar ; rm -f epts.jar ; ln -s epts-$(VERSION).jar epts.jar )


$(JROOT_BIN)/epts: epts.sh MAJOR MINOR \
		$(JROOT_JARDIR)/epts-$(VERSION).jar
	(cd $(JROOT); mkdir -p $(JROOT_BIN))
	sed s/VERSION/$(VERSION)/g epts.sh | \
	sed s/JARDIRECTORY/$(JARDIR)/g > $(JROOT_BIN)/epts
	chmod u+x $(JROOT_BIN)/epts
	if [ "$(DESTDIR)" = "" ] ; \
	then ln -sf $(EXTDIR)/libbzdev.jar $(JROOT_JARDIR)/libbzdev.jar ; \
	     ln -sf $(BZDEVDIR)/libbzdev.policy \
		$(JROOT_JARDIR)/libbzdev.policy ; \
	fi

$(JROOT_MANDIR)/man1/epts.1.gz: epts.1
	mkdir -p $(JROOT_MANDIR)/man1
	sed s/VERSION/$(VERSION)/g epts.1 | \
	gzip -n -9 > $(JROOT_MANDIR)/man1/epts.1.gz

$(JROOT_MANDIR)/man5/epts.5.gz: epts.5
	mkdir -p $(JROOT_MANDIR)/man5
	sed s/VERSION/$(VERSION)/g epts.5 | \
	gzip -n -9 > $(JROOT_MANDIR)/man5/epts.5.gz


clean:
	rm -f $(CLASSES)/epts/* $(JROOT_JARDIR)/epts-$(VERSION).jar \
	$(JROOT_MANDIR)/man1/* \
	$(JROOT_BIN)/epts \
	$(CLASSES)/*.dtd
	[ -d $(CLASSES)/epts ] && rmdir $(CLASSES)/epts || true
	[ -d man/man1 ] && rmdir man/man1 || true
	[ -d man/man5 ] && rmdir man/man5 || true
	[ -d man ] && rmdir man || true

install: all 
	install -d $(APP_ICON_DIR)
	install -d $(MIME_ICON_DIR)
	install -d $(MIMEDIR)
	install -d $(MIMEDIR)/packages
	install -d $(APPDIR)
	install -d $(BIN)
	install -d $(MANDIR)
	install -d $(MANDIR)/man1
	install -d $(MANDIR)/man5
	install -d $(JARDIRECTORY)
	install -m 0644 -T $(SOURCEICON) $(APP_ICON_DIR)/$(TARGETICON)
	for i in $(ICON_WIDTHS) ; do \
		install -d $(ICON_DIR)/$${i}x$${i}/$(APPS_DIR) ; \
		inkscape -w $$i -e tmp.png $(SOURCEICON) ; \
		install -m 0644 -T tmp.png \
			$(ICON_DIR)/$${i}x$${i}/$(APPS_DIR)/$(TARGETICON_PNG); \
		rm tmp.png ; \
	done
	install -m 0644 -T mime/epts.xml $(MIMEDIR)/packages/epts.xml
	install -m 0644 -T $(SOURCE_FILE_ICON) \
		$(MIME_ICON_DIR)/$(TARGET_FILE_ICON)
	for i in $(ICON_WIDTHS) ; do \
	    install -d $(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR) ; \
	done;
	for i in $(ICON_WIDTHS) ; do \
	    inkscape -w $$i -e tmp.png $(SOURCE_FILE_ICON) ; \
	    install -m 0644 -T tmp.png \
	    $(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR)/$(TARGET_FILE_ICON_PNG); \
	    rm tmp.png ; \
	done
	install -m 0644 $(JROOT_JARDIR)/epts-$(VERSION).jar \
		$(JARDIRECTORY)
	install -m 0755 $(JROOT_BIN)/epts $(BIN)
	install -m 0644 epts.desktop $(APPDIR)
	install -m 0644 $(JROOT_MANDIR)/man1/epts.1.gz $(MANDIR)/man1
	install -m 0644 $(JROOT_MANDIR)/man5/epts.5.gz $(MANDIR)/man5

uninstall:
	@rm $(MANDIR)/man1/epts.1.gz || echo ... rm epts.1.gz  FAILED
	@rm $(APPDIR)/epts.desktop || echo ... rm epts.desktop FAILED
	@rm $(BIN)/epts   || echo ... rm $(BIN)/epts FAILED
	@rm $(APP_ICON_DIR)/$(TARGETICON)  || echo ... rm $(TARGETICON) FAILED
	@for i in $(ICON_WIDTHS) ; do \
	   rm $(ICON_DIR)/$${i}x$${i}/$(APPS_DIR)/$(TARGETICON_PNG) \
		|| echo .. rm $(TARGETICON_PNG) from $${i}x$${i} FAILED; \
	done
	@rm $(MIME_ICON_DIR)/$(TARGET_FILE_ICON)  || \
		echo ... rm $(TARGET_FILE_ICON) FAILED
	@for i in $(ICON_WIDTHS) ; do \
	  rm $(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR)/$(TARGET_FILE_ICON_PNG) \
		|| echo rm $(TARGET_FILE_ICON_PNG) from $${i}x$${i} FAILED; \
	done
	@(cd $(MIMEDIR)/packages ; \
	 rm epts.xml || echo rm .../webail.xml FAILED)
	@rm $(JARDIRECTORY)/epts-$(VERSION).jar \
		|| echo ... rm epts-$(VERSION).jar FAILED
