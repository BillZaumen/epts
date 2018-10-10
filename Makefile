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
TARGET_FILE_ICON = application-vnd.bzdev.epts-state+xml.svg
TARGET_FILE_ICON_PNG = application-vnd.bzdev.epts-state+xml.png

SOURCE_CFILE_ICON = icons/eptcfile.svg
TARGET_CFILE_ICON = application-vnd.bzdev.epts-config+zip.svg
TARGET_CFILE_ICON_PNG = application-vnd.bzdev.epts-config+zip.png

SOURCE_TCFILE_ICON = icons/epttfile.svg
TARGET_TCFILE_ICON = application-vnd.bzdev.epts-template-config+zip.svg
TARGET_TCFILE_ICON_PNG = application-vnd.bzdev.epts-template-config+zip.png


JROOT_DOCDIR = $(JROOT)$(SYS_DOCDIR)
JROOT_JARDIR = $(JROOT)/jar
JROOT_MANDIR = $(JROOT)/man
JROOT_BIN = $(JROOT)/bin


EXTDIR = $(SYS_JARDIRECTORY)
EXTDIR_SED =  $(shell echo $(EXTDIR) | sed  s/\\//\\\\\\\\\\//g)
BZDEVDIR = $(DESTDIR)$(SYS_BZDEVDIR)
BZDEVDIR_SED = $(shell echo $(SYS_BZDEVDIR) | sed  s/\\//\\\\\\\\\\//g)

EXTLIBS=$(EXTDIR)/libbzdev.jar


MANS = $(JROOT_MANDIR)/man1/epts.1.gz $(JROOT_MANDIR)/man5/epts.5.gz


ICONS = $(SOURCEICON) $(SOURCE_FILE_ICON) $(SOURCE_CFILE_ICON) \
	$(SOURCE_TCFILE_ICON)

JFILES = $(wildcard src/*.java)
PROPERTIES = src/EPTS.properties

TEMPLATES = templates/distances.tpl \
	templates/ECMAScript.tpl templates/save.tpl \
	templates/ECMAScriptPaths.tpl templates/ECMAScriptLocations.tpl \
	templates/ECMAScriptLayers.tpl 	templates/JavaLayers.tpl \
	templates/JavaPathFactories.tpl templates/JavaLocations.tpl \
	templates/JavaPathBuilders.tpl templates/SVG.tpl \
	templates/area.tpl templates/circumference.tpl \
	templates/pathlength.tpl

SCRIPTS = scripts/grid.js scripts/polar.js

CRLF_TEMPLATES = templates/SegmentsCSV.tpl

RESOURCES = manual/manual.xml \
	manual/manual.xsl \
	manual/index.html \
	manual/manual.html \
	manual/manual.css \
	manual/desktop.png \
	manual/inputfiles.png \
	manual/openfile.png \
	manual/selectapp.png \
	manual/configSession1.png \
	manual/configSession2.png \
	manual/configSession3.png \
	manual/configSession4.png \
	manual/configSession5.png \
	manual/templateConfig1.png \
	manual/templateConfig2.png \
	manual/templateConfig3.png \
	manual/dialog.png \
	manual/drawing1.png \
	manual/drawing2.png \
	manual/drawing3.png \
	manual/drawing4.png \
	manual/drawing5.png \
	manual/filewindow.png \
	manual/menubar.png \
	manual/table2.png \
	manual/terminal.png \
	epts-1.0.dtd

FILES = $(JFILES) $(PROPERTIES)

PROGRAM = $(JROOT_BIN)/epts $(JROOT_JARDIR)/epts-$(VERSION).jar 
ALL = $(PROGRAM) epts.desktop $(MANS) $(JROOT_BIN)/epts

# program: $(JROOT_BIN)/epts $(JROOT_JARDIR)/epts-$(VERSION).jar 

program: clean $(PROGRAM)

BLDPOLICY = $(JROOT_JARDIR)/epts.policy
$(BLDPOLICY): epts.policy
	mkdir -p $(JROOT_JARDIR)
	sed s/LOCATION/$(EXTDIR_SED)/g epts.policy > $(BLDPOLICY)


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
$(JROOT_JARDIR)/epts-$(VERSION).jar: $(FILES) $(TEMPLATES) $(CRLF_TEMPLATES)\
	$(RESOURCES) $(BLDPOLICY) $(SCRIPTS)
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
	for i in $(TEMPLATES) ; do tname=`basename $$i .tpl`; \
		cp $$i $(CLASSES)/$$tname; done
	for i in $(CRLF_TEMPLATES) ; do tname=`basename $$i .tpl`; \
		cat $$i | sed -e 's/.*/\0\r/' > $(CLASSES)/$$tname; done
	for i in $(SCRIPTS) ; do sname=`basename $$i .js` ; \
		cp $$i $(CLASSES)/$$sname; done
	mkdir -p $(CLASSES)/manual
	for i in $(RESOURCES) ; do cp $$i $(CLASSES)/$$i ; done
	jar cfm $(JROOT_JARDIR)/epts.jar epts.mf -C $(CLASSES) .


$(JROOT_BIN)/epts: epts.sh MAJOR MINOR \
		$(JROOT_JARDIR)/epts-$(VERSION).jar
	(cd $(JROOT); mkdir -p $(JROOT_BIN))
	sed s/BZDEVDIR/$(BZDEVDIR_SED)/g epts.sh > $(JROOT_BIN)/epts
	chmod u+x $(JROOT_BIN)/epts
	if [ "$(DESTDIR)" = "" -a ! -f $(JROOT_JARDIR)/libbzdev.jar ] ; \
	then ln -sf $(EXTDIR)/libbzdev.jar $(JROOT_JARDIR)/libbzdev.jar ; \
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
	$(JROOT_MANDIR)/man5/* \
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
	install -d $(BZDEVDIR)
	install -d $(MANDIR)/man1
	install -d $(MANDIR)/man5
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
	install -m 0644 -T $(SOURCE_CFILE_ICON) \
		$(MIME_ICON_DIR)/$(TARGET_CFILE_ICON)
	install -m 0644 -T $(SOURCE_TCFILE_ICON) \
		$(MIME_ICON_DIR)/$(TARGET_TCFILE_ICON)
	for i in $(ICON_WIDTHS) ; do \
	    install -d $(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR) ; \
	done;
	for i in $(ICON_WIDTHS) ; do \
	  inkscape -w $$i -e tmp.png $(SOURCE_FILE_ICON) ; \
	  install -m 0644 -T tmp.png \
	  $(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR)/$(TARGET_FILE_ICON_PNG); \
	  rm tmp.png ; \
	  inkscape -w $$i -e tmp.png $(SOURCE_CFILE_ICON) ; \
	  install -m 0644 -T tmp.png \
	  $(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR)/$(TARGET_CFILE_ICON_PNG); \
	  inkscape -w $$i -e tmp.png $(SOURCE_TCFILE_ICON) ; \
	  install -m 0644 -T tmp.png \
	  $(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR)/$(TARGET_TCFILE_ICON_PNG); \
	  rm tmp.png ; \
	done
	install -m 0644 $(JROOT_JARDIR)/epts.jar $(BZDEVDIR)
	install -m 0644 $(JROOT_JARDIR)/epts.policy $(BZDEVDIR)
	install -m 0755 $(JROOT_BIN)/epts $(BIN)
	install -m 0644 epts.desktop $(APPDIR)
	install -m 0644 $(JROOT_MANDIR)/man1/epts.1.gz $(MANDIR)/man1
	install -m 0644 $(JROOT_MANDIR)/man5/epts.5.gz $(MANDIR)/man5

install-links:
	 [ -h $(BZDEVDIR)/libbzdev.jar ] || \
		ln -s $(EXTDIR)/libbzdev.jar $(BZDEVDIR)/libbzdev.jar

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
