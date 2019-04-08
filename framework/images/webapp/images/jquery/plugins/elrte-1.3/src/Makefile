# build relese
# author: Troex Nevelin <troex@fury.scancode.ru>

Q=   @
CAT= cat
RM=  rm
CP=  cp
SRC= .
DST= ..
CSS=  ${DST}/css
JS=   ${DST}/js
I18N= ${DST}/js/i18n
IMG=  ${DST}/images

# YUI Compressor
COMPRESSOR=	1
COMPRESSOR_JS=	java -jar ../../../_old/yuicompressor-2.4.6/build/yuicompressor-2.4.6.jar \
			--charset utf8 --type js --line-break 500 \
			-o ${elrte_js_min} ${elrte_js}
COMPRESSOR_CSS=	java -jar ../../../_old/yuicompressor-2.4.6/build/yuicompressor-2.4.6.jar \
			--charset utf8 --type css --line-break 1 \
			-o ${elrte_css_min} ${elrte_css}

# Closure Compiler
# ADVANCED_OPTIMIZATIONS maybe someday
#COMPRESSOR= java -jar ../../../compiler.jar \
			--charset UTF-8 --compilation_level SIMPLE_OPTIMIZATIONS \
			--js_output_file ${elrte_js_min} --js ${elrte_js}

PHONY:     help
all:       elrte elrte-install
clean:     clean-elrte
install:   elrte-install
uninstall: ellib-uninstall elrte-uninstall
distclean: uninstall clean

help:
	@echo 'Makefile for release build automation'
	@echo ' Packages:'
	@echo '   elrte         - elRTE WYSIWYG html editor'
	@echo ''
	@echo ' MAKE targets:'
	@echo '   all           - build and install all packages'
	@echo '   help          - show this message'
	@echo '   install       - install all packages, will auto build'
	@echo '   uninstall     - remove all generated files from DST (${DST})'
	@echo '   clean         - remove generated files from SRC (${SRC})'
	@echo '   distclean     - preform uninstall and clean'
	@echo ''
	@echo ' Individual package targets are possible. To build single package replace PACKAGE'
	@echo ' with name from "Packages" section:'
	@echo '   PACKAGE'
	@echo '   PACKAGE-install'
	@echo '   PACKAGE-unstall'
	@echo '   clean-PACKAGE'


# define objects

# elLib
ellib_img_obj=		${SRC}/ellib/images/loading.gif
ellib_img_obj_dst=	${IMG}/loading.gif


# elRTE
elrte_css=			elrte.full.css
elrte_css_min=		elrte.min.css
elrte_css_obj=		${SRC}/about.txt \
			${SRC}/ellib/css/elcommon.css \
			${SRC}/ellib/css/elcolorpicker.css \
			${SRC}/ellib/css/eldialogform.css \
			${SRC}/ellib/css/elpaddinginput.css \
			${SRC}/ellib/css/elselect.css \
			${SRC}/elrte/css/elrte.css

elrte_js=			elrte.full.js
elrte_js_min=		elrte.min.js
elrte_js_obj=		\
			${SRC}/ellib/js/eli18n.js \
			${SRC}/ellib/js/eldialogform.js \
			${SRC}/ellib/js/jquery.elcolorpicker.js \
			${SRC}/ellib/js/jquery.elborderselect.js \
			${SRC}/ellib/js/jquery.elpaddinginput.js \
			${SRC}/ellib/js/jquery.elselect.js \
			${SRC}/elrte/js/elRTE.js \
			${SRC}/elrte/js/elRTE.*.js \
			${SRC}/elrte/js/ui/*.js


# elLib
ellib-install:
	${CP} ${ellib_img_obj} ${IMG}/

ellib-uninstall:
	${RM} -f ${ellib_img_obj_dst}


# elRTE
elrte: ${elrte_css} ${elrte_js}

elrte-install: elrte ellib-install
	${CP} ${elrte_css} ${SRC}/elrte/css/elrte-inner.css ${CSS}/
	${CP} ${elrte_js}  ${JS}/
	${CP} ${SRC}/elrte/js/i18n/elrte.??.js        ${I18N}/
	${CP} ${SRC}/elrte/js/i18n/elrte.??_??.js     ${I18N}/
	${CP} ${SRC}/elrte/js/i18n/elrte.YOUR_LANG.js ${I18N}/
	${CP} ${SRC}/elrte/images/*.png               ${IMG}/
	${CP} ${SRC}/elrte/images/*.gif               ${IMG}/
	${CP} -R ${SRC}/elrte/images/smileys          ${IMG}/

${elrte_css}:
	${CAT} ${elrte_css_obj} > $@

${elrte_js}:
	${CAT} ${elrte_js_obj} > $@

elrte-uninstall:
	${RM} -f ${CSS}/${elrte_css} ${CSS}/${elrte_css_min} ${CSS}/elrte-inner.css \
	${JS}/${elrte_js} ${JS}/${elrte_js_min} \
	${I18N}/elrte.??.js ${I18N}/elrte.??_??.js ${I18N}/elrte.YOUR_LANG.js \
	${IMG}/*.png ${IMG}/*.gif
	${RM} -rf ${IMG}/smileys

clean-elrte:
	${RM} -f ${elrte_css} ${elrte_css_min} ${elrte_js} ${elrte_js_min} 


# compressor
elrte-compress:
	${COMPRESSOR_JS}
	${COMPRESSOR_CSS}

elrte-compress-install:
	${CP} ${SRC}/${elrte_js_min} ${JS}/
	${CP} ${SRC}/${elrte_css_min} ${CSS}/


ifdef COMPRESSOR
elrte:                elrte-compress
elrte-install:        elrte-compress-install
endif

