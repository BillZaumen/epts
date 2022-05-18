#
# GNU Make file for epts.
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
SYS_POPICON_DIR = /usr/share/icons/Pop
SYS_APP_ICON_DIR = $(SYS_ICON_DIR)/scalable/$(APPS_DIR)
SYS_APP_POPICON_DIR = $(SYS_POPICON_DIR)/scalable/$(APPS_DIR)
SYS_MIME_ICON_DIR =$(SYS_ICON_DIR)/scalable/$(MIMETYPES_DIR)
SYS_MIME_POPICON_DIR =$(SYS_POPICON_DIR)/scalable/$(MIMETYPES_DIR)
SYS_JARDIRECTORY = /usr/share/java
SYS_BZDEVDIR = /usr/share/bzdev

# ICON_WIDTHS = 16 20 22 24 32 36 48 64 72 96 128 192 256

ICON_WIDTHS = 8 16 20 22 24 32 36 48 64 72 96 128 192 256 512
ICON_WIDTHS2x = 16 24 32 48 64 128 256

POPICON_WIDTHS = 8 16 24 32 48 64 128 256
POPICON_WIDTHS2x = 8 16 24 32 48 64 128 256

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
POPICON_DIR = $(DESTDIR)$(SYS_POPICON_DIR)
MIME_POPICON_DIR=$(DESTDIR)$(SYS_MIME_POPICON_DIR)

# Icon directory for applications
#
APP_ICON_DIR = $(DESTDIR)$(SYS_APP_ICON_DIR)
APP_POPICON_DIR = $(DESTDIR)$(SYS_APP_POPICON_DIR)
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

EXTLIB1 = $(EXTDIR)/libbzdev-base.jar
EXTLIB2 = $(EXTDIR)/libbzdev-obnaming.jar
EXTLIB3 = $(EXTDIR)/libbzdev-desktop.jar
EXTLIB4 = $(EXTDIR)/libbzdev-devqsim.jar
EXTLIB5 = $(EXTDIR)/libbzdev-anim2d.jar
EXTLIB6 = $(EXTDIR)/libbzdev-ejws.jar
EXTLIB7 = $(EXTDIR)/libbzdev-math.jar
EXTLIB8 = $(EXTDIR)/libbzdev-graphics.jar
EXTLIB9 = $(EXTDIR)/libbzdev-esp.jar

EXTLIB1to6 = $(EXTLIB1):$(EXTLIB2):$(EXTLIB3):$(EXTLIB4):$(EXTLIB5):$(EXTLIB6)
EXTLIBS = $(EXTLIB1to6):$(EXTLIB7):$(EXTLIB8):$(EXTLIB9)

MANS = $(JROOT_MANDIR)/man1/epts.1.gz $(JROOT_MANDIR)/man5/epts.5.gz

ICONS = $(SOURCEICON) $(SOURCE_FILE_ICON) $(SOURCE_CFILE_ICON) \
	$(SOURCE_TCFILE_ICON)

EPTS_DIR = mods/org.bzdev.epts
EPTS_JDIR = $(EPTS_DIR)/org/bzdev/epts

JFILES = $(wildcard src/org.bzdev.epts/org/bzdev/epts/*.java)
PROPERTIES = src/org.bzdev.epts/org/bzdev/epts/EPTS.properties

TEMPLATES = templates/distances.tpl \
	templates/ECMAScript.tpl templates/save.tpl \
	templates/ECMAScriptPaths.tpl templates/ECMAScriptLocations.tpl \
	templates/ECMAScriptLayers.tpl 	templates/JavaLayers.tpl \
	templates/JavaPathFactories.tpl templates/JavaLocations.tpl \
	templates/JavaPathBuilders.tpl templates/SVG.tpl templates/SVGmm.tpl \
	templates/YAMLLayers.tpl templates/YAMLLocations.tpl \
	templates/YAMLPaths.tpl templates/YAML.tpl \
	templates/area.tpl templates/circumference.tpl \
	templates/pathlength.tpl templates/HTMLImageMap.tpl

SCRIPTS = scripts/grid.js scripts/polar.js \
	scripts/grid.esp scripts/polar.esp

CRLF_TEMPLATES = templates/SegmentsCSV.tpl

RESOURCES = manual/manual.xml \
	manual/manual.xsl \
	docs/manual/manual.dm.xsl \
	docs/manual/index.html \
	manual/manual.html \
	manual/manual.css \
	docs/manual/manual.dm.css \
	docs/manual/print.css \
	docs/manual/DesktopIcons.png \
	docs/manual/OpeningDialog.png \
	docs/manual/desktop.png \
	docs/manual/inputfiles.png \
	docs/manual/openfile.png \
	docs/manual/selectapp.png \
	docs/manual/configSession1.png \
	docs/manual/configSession2.png \
	docs/manual/configSession3.png \
	docs/manual/configSession4.png \
	docs/manual/configSession5.png \
	docs/manual/templateConfig1.png \
	docs/manual/templateConfig2.png \
	docs/manual/templateConfig3.png \
	docs/manual/dialog.png \
	docs/manual/drawing1.png \
	docs/manual/drawing2.png \
	docs/manual/drawing3.png \
	docs/manual/drawing4.png \
	docs/manual/drawing5.png \
	docs/manual/filewindow.png \
	docs/manual/menubar.png \
	docs/manual/offsetPane.png \
	docs/manual/table2.png \
	docs/manual/terminal.png \
	docs/manual/LocationDialog.png \
	docs/manual/NTPDialog.png \
	docs/manual/NTPWindow.png \
	docs/manual/AddVector.png \
	docs/manual/AddArc.png

DTD = epts-1.0.dtd

FILES = $(JFILES) $(PROPERTIES)

PROGRAM = $(JROOT_BIN)/epts $(JROOT_JARDIR)/epts.jar $(BLDPOLICY)
ALL = $(PROGRAM) epts.desktop $(MANS) docs


all: $(ALL)

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


#
# Use this to set up the links to the libraries for the jar subdirectory
# Needed for testing.
#
jardirlibs:
	(cd jar ; rm libbzdev-*.jar)
	ln -s $(EXTLIB1) $(EXTLIB2) $(EXTLIB3) $(EXTLIB4) \
		$(EXTLIB5) $(EXTLIB6) $(EXTLIB7) $(EXTLIB8) $(EXTLIB9) jar/


include MajorMinor.mk

org:
	ln -s src/org.bzdev.epts/org org

NOSCRIPT=<NOSCRIPT><FONT size="-2"> \
	(When viewed from a browser with Javascript turned \
	off, the table of contents in the other frame may not have any \
	contents.)</FONT></NOSCRIPT>

nst:
	echo '<!-- NOSCRIPT -->' | sed -e 's%<!-- NOSCRIPT -->%$(NOSCRIPT)%'
# The action for this rule removes all the epts-*.jar files
# because the old ones would otherwise still be there and end up
# being installed.
#
$(JROOT_JARDIR)/epts.jar: $(FILES) $(TEMPLATES) $(CRLF_TEMPLATES)\
	$(RESOURCES) $(DTD) $(BLDPOLICY) $(SCRIPTS) $(ICONS)
	mkdir -p $(EPTS_JDIR)
	javac -Xlint:unchecked -Xlint:deprecation \
		-d mods/org.bzdev.epts -p $(EXTLIBS) \
		src/org.bzdev.epts/module-info.java $(JFILES)
	cp $(PROPERTIES) $(EPTS_JDIR)
	for i in $(ICON_WIDTHS) ; do \
		inkscape -w $$i \
			--export-filename=$(EPTS_JDIR)/eptsicon$${i}.png \
		icons/epts.svg ; \
	done
	mkdir -p $(JROOT_JARDIR)
	rm -f $(JROOT_JARDIR)/epts-*.jar
	for i in $(TEMPLATES) ; do tname=`basename $$i .tpl`; \
		cp $$i $(EPTS_JDIR)/$$tname; done
	for i in $(CRLF_TEMPLATES) ; do tname=`basename $$i .tpl`; \
		cat $$i | sed -e 's/.*/\0\r/' > $(EPTS_JDIR)/$$tname; done
	for i in $(SCRIPTS) ; do sname=`basename $$i` ; \
		cp $$i $(EPTS_JDIR)/$$sname; done
	mkdir -p $(EPTS_JDIR)/manual
	cp $(DTD) $(EPTS_JDIR)/$(DTD)
	for i in $(RESOURCES) ; do \
		j=manual/`basename $$i` ; \
		cp $$i $(EPTS_JDIR)/$$j ; done
	sed -e s/manual.css/manual.dm.css/ manual/manual.html \
		| sed -e 's%<!-- NOSCRIPT -->%$(NOSCRIPT)%' \
		> $(EPTS_JDIR)/manual/manual.dm.html
	grep -v '<LINK' manual/manual.html > $(EPTS_JDIR)/manual/print.html
	sed -e s/manual.html/manual.dm.html/ manual/manual.xml \
		| sed -e s/manual.xsl/manual.dm.xsl/ > \
		$(EPTS_JDIR)/manual/manual.dm.xml
	jar cfe $(JROOT_JARDIR)/epts.jar org.bzdev.epts.EPTS \
		-C $(EPTS_DIR) .

docs:  docs/manual/manual.dm.html docs/manual/manual.dm.xml

docs/manual/manual.dm.html: manual/manual.html Makefile
	sed -e s/manual.css/manual.dm.css/ manual/manual.html \
	| sed -e 's%<!-- NOSCRIPT -->%$(NOSCRIPT)%' \
	> docs/manual/manual.dm.html

docs/manual/manual.dm.xml:  manual/manual.xml Makefile
	sed -e s/manual.html/manual.dm.html/ manual/manual.xml \
		| sed -e s/manual.xsl/manual.dm.xsl/ \
		> docs/manual/manual.dm.xml


$(JROOT_BIN)/epts: epts.sh MAJOR MINOR \
		$(JROOT_JARDIR)/epts.jar
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
	rm -fr mods
	rm -f $(JROOT_JARDIR)/epts.jar \
	$(JROOT_MANDIR)/man1/* \
	$(JROOT_MANDIR)/man5/* \
	$(JROOT_BIN)/epts;
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
		inkscape -w $$i --export-filename=tmp.png $(SOURCEICON) ; \
		install -m 0644 -T tmp.png \
			$(ICON_DIR)/$${i}x$${i}/$(APPS_DIR)/$(TARGETICON_PNG); \
		rm tmp.png ; \
	done
	for i in $(ICON_WIDTHS2x) ; do \
		install -d $(ICON_DIR)/$${i}x$${i}@2x/$(APPS_DIR) ; \
		ii=`expr 2 '*' $$i` ; \
		inkscape -w $$ii --export-filename=tmp.png $(SOURCEICON) ; \
		install -m 0644 -T tmp.png \
		    $(ICON_DIR)/$${i}x$${i}@2x/$(APPS_DIR)/$(TARGETICON_PNG); \
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
	  inkscape -w $$i --export-filename=tmp.png $(SOURCE_FILE_ICON) ; \
	  dir=$(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR) ; \
	  install -d $$dir ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_FILE_ICON_PNG); \
	  rm tmp.png ; \
	  inkscape -w $$i --export-filename=tmp.png $(SOURCE_CFILE_ICON) ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_CFILE_ICON_PNG); \
	  inkscape -w $$i --export-filename=tmp.png $(SOURCE_TCFILE_ICON) ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_TCFILE_ICON_PNG); \
	  rm tmp.png ; \
	done
	for i in $(ICON_WIDTHS2x) ; do \
	  ii=`expr 2 '*' $$i` ; \
	  inkscape -w $$ii --export-filename=tmp.png $(SOURCE_FILE_ICON) ; \
	  dir=$(ICON_DIR)/$${i}x$${i}@2x/$(MIMETYPES_DIR) ; \
	  install -d $$dir ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_FILE_ICON_PNG); \
	  rm tmp.png ; \
	  inkscape -w $$ii --export-filename=tmp.png $(SOURCE_CFILE_ICON) ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_CFILE_ICON_PNG);\
	  inkscape -w $$ii --export-filename=tmp.png $(SOURCE_TCFILE_ICON) ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_TCFILE_ICON_PNG);\
	  rm tmp.png ; \
	done
	install -m 0644 $(JROOT_JARDIR)/epts.jar $(BZDEVDIR)
	install -m 0644 $(JROOT_JARDIR)/epts.policy $(BZDEVDIR)
	install -m 0755 $(JROOT_BIN)/epts $(BIN)
	install -m 0644 epts.desktop $(APPDIR)
	install -m 0644 $(JROOT_MANDIR)/man1/epts.1.gz $(MANDIR)/man1
	install -m 0644 $(JROOT_MANDIR)/man5/epts.5.gz $(MANDIR)/man5

install-pop: all
	install -d $(APP_POPICON_DIR)
	install -d $(MIME_POPICON_DIR)
	install -m 0644 -T $(SOURCEICON) $(APP_POPICON_DIR)/$(TARGETICON)
	install -m 0644 -T $(SOURCE_FILE_ICON) \
		$(MIME_POPICON_DIR)/$(TARGET_FILE_ICON)
	install -m 0644 -T $(SOURCE_CFILE_ICON) \
		$(MIME_POPICON_DIR)/$(TARGET_CFILE_ICON)
	install -m 0644 -T $(SOURCE_TCFILE_ICON) \
		$(MIME_POPICON_DIR)/$(TARGET_TCFILE_ICON)

# Save these until we are sure we don't need them. The actions
# were part of install-pop but were deleted because the PNG files
# ssem to be causing a problem when installed in the Pop icon directory
install-pop-saved-actions:
	for i in $(POPICON_WIDTHS) ; do \
	    install -d $(POPICON_DIR)/$${i}x$${i}/$(APPS_DIR) ; \
	    inkscape -w $$i --export-filename=tmp.png $(SOURCEICON) ; \
	    install -m 0644 -T tmp.png \
		$(POPICON_DIR)/$${i}x$${i}/$(APPS_DIR)/$(TARGETICON_PNG); \
	    rm tmp.png ; \
	done
	for i in $(POPICON_WIDTHS2x) ; do \
	    install -d $(POPICON_DIR)/$${i}x$${i}@2x/$(APPS_DIR) ; \
	    ii=`expr 2 '*' $$i` ; \
	    inkscape -w $$ii --export-filename=tmp.png $(SOURCEICON) ; \
	    install -m 0644 -T tmp.png \
		$(POPICON_DIR)/$${i}x$${i}@2x/$(APPS_DIR)/$(TARGETICON_PNG); \
	    rm tmp.png ; \
	done
	for i in $(POPICON_WIDTHS) ; do \
	    inkscape -w $$i --export-filename=tmp.png $(SOURCE_FILE_ICON) ; \
	    dir=$(POPICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR) ; \
	    install -d $$dir ; \
	    install -m 0644 -T tmp.png $$dir/$(TARGET_FILE_ICON_PNG); \
	    rm tmp.png ; \
	    inkscape -w $$i --export-filename=tmp.png $(SOURCE_CFILE_ICON) ; \
	    install -m 0644 -T tmp.png $$dir/$(TARGET_CFILE_ICON_PNG); \
	    inkscape -w $$i --export-filename=tmp.png $(SOURCE_TCFILE_ICON) ; \
	    install -m 0644 -T tmp.png $$dir/$(TARGET_TCFILE_ICON_PNG); \
	    rm tmp.png ; \
	done
	for i in $(POPICON_WIDTHS2x) ; do \
	    ii=`expr 2 '*' $$i` ; \
	    inkscape -w $$ii --export-filename=tmp.png $(SOURCE_FILE_ICON) ; \
	    dir=$(POPICON_DIR)/$${i}x$${i}@2x/$(MIMETYPES_DIR) ; \
	    install -d $$dir ; \
	    install -m 0644 -T tmp.png $$dir/$(TARGET_FILE_ICON_PNG); \
	    rm tmp.png ; \
	    inkscape -w $$ii --export-filename=tmp.png $(SOURCE_CFILE_ICON) ; \
	    install -m 0644 -T tmp.png $$dir/$(TARGET_CFILE_ICON_PNG);\
	    inkscape -w $$ii --export-filename=tmp.png $(SOURCE_TCFILE_ICON) ; \
	    install -m 0644 -T tmp.png $$dir/$(TARGET_TCFILE_ICON_PNG);\
	    rm tmp.png ; \
	done


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
