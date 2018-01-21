/**
 * @class button - insert/edit image (open dialog window)
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * Copyright: Studio 42, http://www.std42.ru
 **/
(function($) {
elRTE.prototype.ui.prototype.buttons.image = function(rte, name) {
	this.constructor.prototype.constructor.call(this, rte, name);
	var self = this,
		rte  = self.rte,
		proportion = 0,
		width = 0,
		height = 0,
		bookmarks = null,
		reset = function(nosrc) {
			$.each(self.src, function(i, elements) {
				$.each(elements, function(n, el) {
					if (n == 'src' && nosrc) {
						return;
					}
					el.val('');
				});
			});
		},
		values = function(img) {
			$.each(self.src, function(i, elements) {
				$.each(elements, function(n, el) {
					var val, w, c, s, border;
					
					if (n == 'width') {
						val = img.width();
					} else if (n == 'height') {
						val = img.height();
					} else if (n == 'border') {
						val = '';
						border = img.css('border') || rte.utils.parseStyle(img.attr('style')).border || '';

						if (border) {
							w = border.match(/(\d(px|em|%))/);
							c = border.match(/(#[a-z0-9]+)/);
							val = {
								width : w ? w[1] : border,
								style : border,
								color : rte.utils.color2Hex(c ? c[1] : border)
							}
						} 
					} else if (n == 'margin') {
						val = img;
					} else if (n == 'align') { 
						val = img.css('float');

						if (val != 'left' && val != 'right') {
							val = img.css('vertical-align');
						}
					 }else {
						val = img.attr(n)||'';
					}
					
					if (i == 'events') {
						val = rte.utils.trimEventCallback(val);
					}

					el.val(val);
				});
			});
		},
		preview = function() {
			var src = self.src.main.src.val();
			
			reset(true);
			
			if (!src) {
				self.preview.children('img').remove();
				self.prevImg = null;
			} else {
				if (self.prevImg) {
					self.prevImg
						.removeAttr('src')
						.removeAttr('style')
						.removeAttr('class')
						.removeAttr('id')
						.removeAttr('title')
						.removeAttr('alt')
						.removeAttr('longdesc');
						
					$.each(self.src.events, function(name, input) {
						self.prevImg.removeAttr(name);
					});
				} else {
					self.prevImg = $('<img/>').prependTo(self.preview);
				}
				self.prevImg.load(function() {
					self.prevImg.unbind('load');
					setTimeout(function() {
						width      = self.prevImg.width();
						height     = self.prevImg.height();
						proportion = (width/height).toFixed(2);
						self.src.main.width.val(width);
						self.src.main.height.val(height);
						
					}, 100);
				})
				.attr('src', src);
			}
			
		},
		size = function(e) {
			var w = parseInt(self.src.main.width.val())||0,
				h = parseInt(self.src.main.height.val())||0;
				
			if (self.prevImg) {
				if (w && h) {
					if (e.target === self.src.main.width[0]) {
						h = parseInt(w/proportion);
					} else {
						w = parseInt(h*proportion);
					}
				} else {
					w = width;
					h = height;
				}
				self.src.main.height.val(h);
				self.src.main.width.val(w);
				self.prevImg.width(w).height(h);
				self.src.adv.style.val(self.prevImg.attr('style'));
			}
		}
		;
	
	this.img     = null;
	this.prevImg = null;
	this.preview = $('<div class="elrte-image-preview"/>').text('Proin elit arcu, rutrum commodo, vehicula tempus, commodo a, risus. Curabitur nec arcu. Donec sollicitudin mi sit amet mauris. Nam elementum quam ullamcorper ante. Etiam aliquet massa et lorem. Mauris dapibus lacus auctor risus. Aenean tempor ullamcorper leo. Vivamus sed magna quis ligula eleifend adipiscing. Duis orci. Aliquam sodales tortor vitae ipsum. Aliquam nulla. Duis aliquam molestie erat. Ut et mauris vel pede varius sollicitudin');
	
	this.init = function() {	
		this.labels = {
			main   : 'Properies',
			link   : 'Link',
			adv    : 'Advanced',
			events : 'Events',
			id       : 'ID',
			'class'  : 'Css class',
			style    : 'Css style',
			longdesc : 'Detail description URL',
			href    : 'URL',
			target  : 'Open in',
			title   : 'Title'
		}
		
		this.src = {
			main : {
				src    : $('<input type="text" />').css('width', '100%').change(preview),
				title  : $('<input type="text" />').css('width', '100%'),
				alt    : $('<input type="text" />').css('width', '100%'),
				width  : $('<input type="text" />').attr('size', 5).css('text-align', 'right').change(size),
				height : $('<input type="text" />').attr('size', 5).css('text-align', 'right').change(size),
				margin : $('<div />').elPaddingInput({
					type : 'margin', 
					change : function() {
						var margin = self.src.main.margin.val();
					
						if (self.prevImg) {
							if (margin.css) {
								self.prevImg.css('margin', margin.css)
							} else {
								self.prevImg.css({
									'margin-left'   : margin.left,
									'margin-top'    : margin.top,
									'margin-right'  : margin.right,
									'margin-bottom' : margin.bottom
								});
							}
						}
					} 
				}), 
				align  : $('<select />').css('width', '100%')
							.append($('<option />').val('').text(this.rte.i18n('Not set', 'dialogs')))
							.append($('<option />').val('left'       ).text(this.rte.i18n('Left')))
							.append($('<option />').val('right'      ).text(this.rte.i18n('Right')))
							.append($('<option />').val('top'        ).text(this.rte.i18n('Top')))
							.append($('<option />').val('text-top'   ).text(this.rte.i18n('Text top')))
							.append($('<option />').val('middle'     ).text(this.rte.i18n('middle')))
							.append($('<option />').val('baseline'   ).text(this.rte.i18n('Baseline')))
							.append($('<option />').val('bottom'     ).text(this.rte.i18n('Bottom')))
							.append($('<option />').val('text-bottom').text(this.rte.i18n('Text bottom')))
							.change(function() {
								var val = $(this).val(),
									css = {
										'float' : '',
										'vertical-align' : ''
									};
								if (self.prevImg) {
									if (val == 'left' || val == 'right') {
										css['float'] = val;
										css['vertical-align'] = '';
									} else if (val) {
										css['float'] = '';
										css['vertical-align'] = val;
									} 
									self.prevImg.css(css);
								}
							})
						,
				border : $('<div />').elBorderSelect({
					name : 'border',
					change : function() {
						var border = self.src.main.border.val();
						if (self.prevImg) {
							self.prevImg.css('border', border.width ? border.width+' '+border.style+' '+border.color : '');
						}
					}
				})
			},

			adv : {},
			events : {}
		}
		
		$.each(['id', 'class', 'style', 'longdesc'], function(i, name) {
			self.src.adv[name] = $('<input type="text" style="width:100%" />');
		});
		
		this.src.adv['class'].change(function() {
			if (self.prevImg) {
				self.prevImg.attr('class', $(this).val());
			}
		});
		
		this.src.adv.style.change(function() {
			if (self.prevImg) {
				self.prevImg.attr('style', $(this).val());
				values(self.prevImg);
			}
		});
		
		$.each(
			['onblur', 'onfocus', 'onclick', 'ondblclick', 'onmousedown', 'onmouseup', 'onmouseover', 'onmouseout', 'onmouseleave', 'onkeydown', 'onkeypress', 'onkeyup'], 
			function() {
				self.src.events[this] = $('<input type="text"  style="width:100%"/>');
		});
	}
	
	this.command = function() {
		!this.src && this.init();
		
		var img, 
			opts = {
				rtl : rte.rtl,
				submit : function(e, d) { 
					e.stopPropagation(); 
					e.preventDefault(); 
					self.set(); 

					dialog.close(); 
				},
				close : function() {

					bookmarks && rte.selection.moveToBookmark(bookmarks)
				},
				dialog : {
					autoOpen  : false,
					width     : 500,
					position  : 'top',
					title     : rte.i18n('Image'),
					resizable : true,
					open      : function() {
						$.fn.resizable && $(this).parents('.ui-dialog:first').resizable('option', 'alsoResize', '.elrte-image-preview');
					}
				}
			},
			dialog = new elDialogForm(opts),
			fm = !!rte.options.fmOpen,
			src = fm
				? $('<div class="elrte-image-src-fm"><span class="ui-state-default ui-corner-all"><span class="ui-icon ui-icon-folder-open"/></span></div>')
					.append(this.src.main.src.css('width', '87%'))
				: this.src.main.src;
			
			;
		
		reset();
		this.preview.children('img').remove();
		this.prevImg = null;
		img = rte.selection.getEnd();
		
		this.img = img.nodeName == 'IMG' && !$(img).is('.elrte-protected')
			? $(img)
			: $('<img/>');
		
		bookmarks = rte.selection.getBookmark();

		if (fm) {
			src.children('.ui-state-default')
				.click( function() {
					rte.options.fmOpen( function(url) { self.src.main.src.val(url).change() } );
				})
				.hover(function() {
					$(this).toggleClass('ui-state-hover');
				});
		}
		
		dialog.tab('main', this.rte.i18n('Properies'))
			.append([this.rte.i18n('Image URL'), src],                 'main', true)
			.append([this.rte.i18n('Title'),     this.src.main.title], 'main', true)
			.append([this.rte.i18n('Alt text'),  this.src.main.alt],   'main', true)
			.append([this.rte.i18n('Size'), $('<span />').append(this.src.main.width).append(' x ').append(this.src.main.height).append(' px')], 'main', true)
			.append([this.rte.i18n('Alignment'), this.src.main.align],  'main', true)
			.append([this.rte.i18n('Margins'),   this.src.main.margin], 'main', true)
			.append([this.rte.i18n('Border'),    this.src.main.border], 'main', true)
		
		dialog.append($('<fieldset><legend>'+this.rte.i18n('Preview')+'</legend></fieldset>').append(this.preview), 'main');
		
		
		
		$.each(this.src, function(tabname, elements) {
		
			if (tabname == 'main') {
				return;
			}
			dialog.tab(tabname, rte.i18n(self.labels[tabname]));
			
			$.each(elements, function(name, el) {
				self.src[tabname][name].val(tabname == 'events' ? rte.utils.trimEventCallback(self.img.attr(name)) : self.img.attr(name)||'');
				dialog.append([rte.i18n(self.labels[name] || name), self.src[tabname][name]], tabname, true);
			});
		});
		
		dialog.open();		
		
		if (this.img.attr('src')) {
			values(this.img);
			this.prevImg = this.img.clone().prependTo(this.preview);
			proportion   = (this.img.width()/this.img.height()).toFixed(2);
			width        = parseInt(this.img.width());
			height       = parseInt(this.img.height());
		}
	}
		
	this.set = function() {
		var src = this.src.main.src.val(),
			link;
		
		this.rte.history.add();
		bookmarks && rte.selection.moveToBookmark(bookmarks);
		
		if (!src) {
			link = rte.dom.selfOrParentLink(this.img[0]);
			link && link.remove();
			return this.img.remove();
		}
		
		!this.img[0].parentNode && (this.img = $(this.rte.doc.createElement('img')));
		
		this.img.attr('src', src)
			.attr('style', this.src.adv.style.val());
		
		$.each(this.src, function(i, elements) {
			$.each(elements, function(name, el) {
				var val = el.val(), style;
				
				switch (name) {
					case 'width':
						self.img.css('width', val);
						break;
					case 'height':
						self.img.css('height', val);
						break;
					case 'align':
						self.img.css(val == 'left' || val == 'right' ? 'float' : 'vertical-align', val);
						break;
					case 'margin':
						if (val.css) {
							self.img.css('margin', val.css);
						} else {
							self.img.css({
								'margin-left'   : val.left,
								'margin-top'    : val.top,
								'margin-right'  : val.right,
								'margin-bottom' : val.bottom
							});
						}
						break;
					case 'border':
						if (!val.width) {
							val = '';
						} else {
							val = 'border:'+val.css+';'+$.trim((self.img.attr('style')||'').replace(/border\-[^;]+;?/ig, ''));
							name = 'style';
							self.img.attr('style', val)
							return;
						}

						break;
					case 'src':
					case 'style':
						return;
					default:
						val ? self.img.attr(name, val) : self.img.removeAttr(name);
				}
			});
		});
		
		!this.img[0].parentNode && rte.selection.insertNode(this.img[0]);
		this.rte.ui.update();
	}

	this.update = function() {
		this.domElem.removeClass('disabled');
		var n = this.rte.selection.getEnd(),
			$n = $(n);
		if (n.nodeName == 'IMG' && !$n.hasClass('elrte-protected')) {
			this.domElem.addClass('active');
		} else {
			this.domElem.removeClass('active');
		}
	}
	
}
})(jQuery);
