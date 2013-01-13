PACKAGE=	tzcode
VERSION=	2012j
LOCALTIME=	GMT
POSIXRULES=	America/New_York
TOPDIR=		/usr/local
TZDIR=		$(TOPDIR)/etc/zoneinfo
ETCDIR=		$(TOPDIR)/etc
BINDIR=		$(TOPDIR)/bin
MANDIR=		$(TOPDIR)/man
LIBDIR=		$(TOPDIR)/lib
TZLIB=		$(LIBDIR)/libtz.a
REDO=		posix_right
YEARISTYPE=	./yearistype
LDLIBS=
GCC_DEBUG_FLAGS = -Dlint -g3 -O3 -fno-common -fstrict-aliasing \
	-Wall -Wextra \
	-Wbad-function-cast -Wcast-align -Wcast-qual \
	-Wformat=2 -Winit-self \
	-Wmissing-declarations -Wmissing-noreturn -Wmissing-prototypes \
	-Wnested-externs \
	-Wno-format-nonliteral -Wno-sign-compare -Wno-sign-conversion \
	-Wno-type-limits \
	-Wno-unused-parameter -Woverlength-strings -Wpointer-arith \
	-Wshadow -Wstrict-prototypes -Wsuggest-attribute=const \
	-Wsuggest-attribute=noreturn -Wsuggest-attribute=pure -Wtrampolines \
	-Wwrite-strings
CFLAGS=
LDFLAGS=	$(LFLAGS)
zic=		./zic
ZIC=		$(zic) $(ZFLAGS)
AWK=		awk
KSHELL=		/bin/bash
SGML_TOPDIR= /usr
SGML_SEARCH_PATH= $(SGML_TOPDIR)/share/xml/xhtml/schema/dtd/REC-html401-19991224
SGML_CATALOG_FILES= HTML4.cat
VALIDATE = nsgmls
VALIDATE_FLAGS = -s -B -wall -wno-unused-param
VALIDATE_ENV = \
  SGML_CATALOG_FILES=$(SGML_CATALOG_FILES) \
  SGML_SEARCH_PATH=$(SGML_SEARCH_PATH) \
  SP_CHARSET_FIXED=YES \
  SP_ENCODING=UTF-8
GNUTARFLAGS=	--numeric-owner --owner=0 --group=0 --mode=go+u,go-w
TARFLAGS=	`if tar $(GNUTARFLAGS) --version >/dev/null 2>&1; \
		 then echo $(GNUTARFLAGS); \
		 else :; \
		 fi`
GZIPFLAGS=	-9n
cc=		cc
CC=		$(cc) -DTZDIR=\"$(TZDIR)\"
TZCSRCS=	zic.c localtime.c asctime.c scheck.c ialloc.c
TZCOBJS=	zic.o localtime.o asctime.o scheck.o ialloc.o
TZDSRCS=	zdump.c localtime.c ialloc.c
TZDOBJS=	zdump.o localtime.o ialloc.o
DATESRCS=	date.c localtime.c strftime.c asctime.c
DATEOBJS=	date.o localtime.o strftime.o asctime.o
LIBSRCS=	localtime.c asctime.c difftime.c
LIBOBJS=	localtime.o asctime.o difftime.o
HEADERS=	tzfile.h private.h
NONLIBSRCS=	zic.c zdump.c scheck.c ialloc.c
NEWUCBSRCS=	date.c strftime.c
SOURCES=	$(HEADERS) $(LIBSRCS) $(NONLIBSRCS) $(NEWUCBSRCS) tzselect.ksh
MANS=		newctime.3 newstrftime.3 newtzset.3 time2posix.3 \
			tzfile.5 tzselect.8 zic.8 zdump.8
COMMON=		Makefile
DOCS=		README Theory $(MANS) date.1
PRIMARY_YDATA=	africa antarctica asia australasia \
		europe northamerica southamerica
YDATA=		$(PRIMARY_YDATA) pacificnew etcetera backward
NDATA=		systemv factory
SDATA=		solar87 solar88 solar89
TDATA=		$(YDATA) $(NDATA) $(SDATA)
TABDATA=	iso3166.tab zone.tab
DATA=		$(YDATA) $(NDATA) $(SDATA) $(TABDATA) leapseconds yearistype.sh
WEB_PAGES=	tz-art.htm tz-link.htm
MISC=		usno1988 usno1989 usno1989a usno1995 usno1997 usno1998 \
			$(WEB_PAGES) checktab.awk workman.sh \
			zoneinfo2tdf.pl
ENCHILADA=	$(COMMON) $(DOCS) $(SOURCES) $(DATA) $(MISC)
SHELL=		/bin/sh
all:		tzselect zic zdump $(LIBOBJS)
ALL:		all date
install:	all $(DATA) $(REDO) $(TZLIB) $(MANS) $(TABDATA)
		$(ZIC) -y $(YEARISTYPE) \
			-d $(TZDIR) -l $(LOCALTIME) -p $(POSIXRULES)
		-rm -f $(TZDIR)/iso3166.tab $(TZDIR)/zone.tab
		cp iso3166.tab zone.tab $(TZDIR)/.
		-mkdir $(TOPDIR) $(ETCDIR)
		cp tzselect zic zdump $(ETCDIR)/.
		-mkdir $(TOPDIR) $(MANDIR) \
			$(MANDIR)/man3 $(MANDIR)/man5 $(MANDIR)/man8
		-rm -f $(MANDIR)/man3/newctime.3 \
			$(MANDIR)/man3/newtzset.3 \
			$(MANDIR)/man5/tzfile.5 \
			$(MANDIR)/man8/tzselect.8 \
			$(MANDIR)/man8/zdump.8 \
			$(MANDIR)/man8/zic.8
		cp newctime.3 newtzset.3 $(MANDIR)/man3/.
		cp tzfile.5 $(MANDIR)/man5/.
		cp tzselect.8 zdump.8 zic.8 $(MANDIR)/man8/.
INSTALL:	ALL install date.1
		-mkdir $(TOPDIR) $(BINDIR)
		cp date $(BINDIR)/.
		-mkdir $(TOPDIR) $(MANDIR) $(MANDIR)/man1
		-rm -f $(MANDIR)/man1/date.1
		cp date.1 $(MANDIR)/man1/.
version.h:
		(echo 'static char const PKGVERSION[]="($(PACKAGE)) ";' && \
		 echo 'static char const TZVERSION[]="$(VERSION)";') >$@
zdump:		$(TZDOBJS)
		$(CC) -o $@ $(CFLAGS) $(LDFLAGS) $(TZDOBJS) $(LDLIBS)
zic:		$(TZCOBJS) yearistype
		$(CC) -o $@ $(CFLAGS) $(LDFLAGS) $(TZCOBJS) $(LDLIBS)
yearistype:	yearistype.sh
		cp yearistype.sh yearistype
		chmod +x yearistype
posix_only:	zic $(TDATA)
		$(ZIC) -y $(YEARISTYPE) -d $(TZDIR) -L /dev/null $(TDATA)
right_only:	zic leapseconds $(TDATA)
		$(ZIC) -y $(YEARISTYPE) -d $(TZDIR) -L leapseconds $(TDATA)
other_two:	zic leapseconds $(TDATA)
		$(ZIC) -y $(YEARISTYPE) -d $(TZDIR)-posix -L /dev/null $(TDATA)
		$(ZIC) -y $(YEARISTYPE) \
			-d $(TZDIR)-leaps -L leapseconds $(TDATA)
posix_right:	posix_only other_two
right_posix:	right_only other_two
zones:		$(REDO)
$(TZLIB):	$(LIBOBJS)
		-mkdir $(TOPDIR) $(LIBDIR)
		ar ru $@ $(LIBOBJS)
		if [ -x /usr/ucb/ranlib ] || [ -x /usr/bin/ranlib ]; \
			then ranlib $@ ; fi
date:		$(DATEOBJS)
		$(CC) -o $@ $(CFLAGS) $(LDFLAGS) $(DATEOBJS) $(LDLIBS)
tzselect:	tzselect.ksh
		sed \
			-e 's|#!/bin/bash|#!$(KSHELL)|g' \
			-e 's|AWK=[^}]*|AWK=$(AWK)|g' \
			-e 's|\(PKGVERSION\)=.*|\1='\''($(PACKAGE)) '\''|' \
			-e 's|TZDIR=[^}]*|TZDIR=$(TZDIR)|' \
			-e 's|\(TZVERSION\)=.*|\1=$(VERSION)|' \
			<$? >$@
		chmod +x $@
check:		check_tables check_web
check_tables:	checktab.awk $(PRIMARY_YDATA)
		$(AWK) -f checktab.awk $(PRIMARY_YDATA)
check_web:	$(WEB_PAGES)
		$(VALIDATE_ENV) $(VALIDATE) $(VALIDATE_FLAGS) $(WEB_PAGES)
clean:
		rm -f core *.o *.out \
		  date tzselect version.h zdump zic yearistype
		rm -f -r tzpublic
maintainer-clean: clean
		@echo 'This command is intended for maintainers to use; it'
		@echo 'deletes files that may need special tools to rebuild.'
		rm -f *.[1-8].txt *.asc *.tar.gz
names:
		@echo $(ENCHILADA)
public:		check check_public set-timestamps tarballs signatures
set-timestamps:
		-TZ=UTC0 && export TZ && files=`git ls-files` && \
		touch -d @1 test.out && rm -f test.out && \
		for file in $$files; do \
		  test -z "`git diff --name-only $$file`" || continue; \
		  cmd="touch -d @`git log -1 --format='format:%ct' $$file \
			` $$file" && \
		  echo "$$cmd" && \
		  $$cmd || exit; \
		done
check_public:	$(ENCHILADA)
		make maintainer-clean
		make "CFLAGS=$(GCC_DEBUG_FLAGS)"
		mkdir tzpublic
		for i in $(TDATA) ; do \
		  $(zic) -v -d tzpublic $$i 2>&1 || exit; \
		done
		$(zic) -v -d tzpublic $(TDATA)
		rm -f -r tzpublic
tarballs:	tzcode$(VERSION).tar.gz tzdata$(VERSION).tar.gz
tzcode$(VERSION).tar.gz: $(COMMON) $(DOCS) $(SOURCES) $(MISC)
		for i in *.[1-8] ; do \
		  LC_ALL=C sh workman.sh $$i > $$i.txt && \
		  touch -r $$i $$i.txt || exit; \
		done
		LC_ALL=C && export LC_ALL && \
		tar $(TARFLAGS) -cf - \
		    $(COMMON) $(DOCS) $(SOURCES) $(MISC) *.[1-8].txt | \
		  gzip $(GZIPFLAGS) > $@
tzdata$(VERSION).tar.gz: $(COMMON) $(DATA)
		LC_ALL=C && export LC_ALL && \
		tar $(TARFLAGS) -cf - $(COMMON) $(DATA) | \
		  gzip $(GZIPFLAGS) > $@
signatures:	tzcode$(VERSION).tar.gz.asc tzdata$(VERSION).tar.gz.asc
tzcode$(VERSION).tar.gz.asc: tzcode$(VERSION).tar.gz
		gpg --armor --detach-sign $?
tzdata$(VERSION).tar.gz.asc: tzdata$(VERSION).tar.gz
		gpg --armor --detach-sign $?
typecheck:
		make clean
		for i in "long long" unsigned double; \
		do \
			make CFLAGS="-DTYPECHECK -D__time_t_defined -D_TIME_T \"-Dtime_t=$$i\"" ; \
			./zdump -v Europe/Rome ; \
			make clean ; \
		done
zonenames:	$(TDATA)
		@$(AWK) '/^Zone/ { print $$2 } /^Link/ { print $$3 }' $(TDATA)
asctime.o:	private.h tzfile.h
date.o:		private.h
difftime.o:	private.h
ialloc.o:	private.h
localtime.o:	private.h tzfile.h
scheck.o:	private.h
strftime.o:	tzfile.h
zdump.o:	version.h
zic.o:		private.h tzfile.h version.h
.KEEP_STATE:
