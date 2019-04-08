(function($) {
	
	elRTE.prototype.ui.prototype.buttons.flash = function(rte, name) {
		this.constructor.prototype.constructor.call(this, rte, name);
		var self = this;
		this.swf = null;
		this.placeholder = null;
		this.src = {
			url    : $('<input type="text" name="url" />').css('width', '99%'),
			type   : $('<select name="type"/>')
						.append('<option value="application/x-shockwave-flash">Flash</option>')
						.append('<option value="video/quicktime">Quicktime movie</option>')
						.append('<option value="application/x-mplayer2">Windows media</option>'),
			width  : $('<input type="text" />').attr('size', 5).css('text-align', 'right'),
			height : $('<input type="text" />').attr('size', 5).css('text-align', 'right'),
			wmode  : $('<select />')
						.append($('<option />').val('').text(this.rte.i18n('Not set', 'dialogs')))
						.append($('<option />').val('transparent').text(this.rte.i18n('Transparent'))),
			align  : $('<select />')
						.append($('<option />').val('').text(this.rte.i18n('Not set', 'dialogs')))
						.append($('<option />').val('left'       ).text(this.rte.i18n('Left')))
						.append($('<option />').val('right'      ).text(this.rte.i18n('Right')))
						.append($('<option />').val('top'        ).text(this.rte.i18n('Top')))
						.append($('<option />').val('text-top'   ).text(this.rte.i18n('Text top')))
						.append($('<option />').val('middle'     ).text(this.rte.i18n('middle')))
						.append($('<option />').val('baseline'   ).text(this.rte.i18n('Baseline')))
						.append($('<option />').val('bottom'     ).text(this.rte.i18n('Bottom')))
						.append($('<option />').val('text-bottom').text(this.rte.i18n('Text bottom'))),
			margin : $('<div />')
		}
		
		this.command = function() {

			var n = this.rte.selection.getEnd(), opts, url='', w='', h='', f, a, d, mid, o, wm;
			this.rte.selection.saveIERange();
			this.src.margin.elPaddingInput({ type : 'margin' });
			this.placeholder = null;
			this.swf = null;
			if ($(n).hasClass('elrte-media') && (mid = $(n).attr('rel')) &&  this.rte.filter.scripts[mid]) {
				this.placeholder = $(n);
				o = this.rte.filter.scripts[mid];
				url = '';
				if (o.embed && o.embed.src) {
					url = o.embed.src;
				}
				if (o.params && o.params.length) {
					l = o.params.length;
					while (l--) {
						if (o.params[l].name == 'src' || o.params[l].name == 'movie') {
							url =  o.params[l].value;
						}
					}
				}
				
				if (o.embed) {
					w = o.embed.width||parseInt(o.embed.style.width)||'';
					h = o.embed.height||parseInt(o.embed.style.height)||'';
					wm = o.embed.wmode||'';
				} else if (o.obj) {
					w = o.obj.width||parseInt(o.obj.style.width)||'';
					h = o.obj.height||parseInt(o.obj.style.height)||'';
					wm = o.obj.wmode||'';
				}
				
				if (o.obj) {
					f = o.obj.style['float']||'';
					a = o.obj.style['vertical-align']||'';
				} else if (o.embed) {
					f = o.embed.style['float']||'';
					a = o.embed.style['vertical-align']||'';
				}
				this.src.margin.val(n);
				this.src.type.val(o.embed ? o.embed.type : '');
			}
			if ($(n).hasClass('elrte-swf-placeholder')) {
				this.placeholder = $(n);
				url = $(n).attr('rel');
				w = parseInt($(n).css('width'))||'';
				h = parseInt($(n).css('height'))||'';
				f = $(n).css('float');
				a = $(n).css('vertical-align');
				this.src.margin.val(n);
				this.src.wmode.val($(n).attr('wmode'));
			} 
			this.src.url.val(url);
			this.src.width.val(w);
			this.src.height.val(h);
			this.src.align.val(f||a);
			this.src.wmode.val(wm);
			

			

			var opts = {
				rtl : this.rte.rtl,
				submit : function(e, d) { e.stopPropagation(); e.preventDefault(); self.set(); d.close(); },
				dialog : {
					width    : 580,
					position : 'top',
					title    : this.rte.i18n('Flash')
				}
			}
			var d = new elDialogForm(opts);
			
			if (this.rte.options.fmAllow && this.rte.options.fmOpen) {
				var src = $('<span />').append(this.src.url.css('width', '85%'))
						.append(
							$('<span />').addClass('ui-state-default ui-corner-all')
								.css({'float' : 'right', 'margin-right' : '3px'})
								.attr('title', self.rte.i18n('Open file manger'))
								.append($('<span />').addClass('ui-icon ui-icon-folder-open'))
									.click( function() {
										self.rte.options.fmOpen( function(url) { self.src.url.val(url).change(); } );
									})
									.hover(function() {$(this).addClass('ui-state-hover')}, function() { $(this).removeClass('ui-state-hover')})
							);
			} else {
				var src = this.src.url;
			}
			
			d.append([this.rte.i18n('URL'), src], null, true);
			d.append([this.rte.i18n('Type'), this.src.type], null, true);
			d.append([this.rte.i18n('Size'), $('<span />').append(this.src.width).append(' x ').append(this.src.height).append(' px')], null, true)
			d.append([this.rte.i18n('Wmode'), this.src.wmode], null, true);
			d.append([this.rte.i18n('Alignment'), this.src.align], null, true);
			d.append([this.rte.i18n('Margins'), this.src.margin], null, true);
			
			
			
			d.open();
			// setTimeout( function() {self.src.url.focus()}, 100)
			
			
			var fs = $('<fieldset />').append($('<legend />').text(this.rte.i18n('Preview')))
			d.append(fs, 'main');
			var frame = document.createElement('iframe');
			$(frame).attr('src', '#').addClass('el-rte-preview').appendTo(fs);
			html = this.rte.options.doctype+'<html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8" /></head><body style="padding:0;margin:0;font-size:9px"> Proin elit arcu, rutrum commodo, vehicula tempus, commodo a, risus. Curabitur nec arcu. Donec sollicitudin mi sit amet mauris. Nam elementum quam ullamcorper ante. Etiam aliquet massa et lorem. Mauris dapibus lacus auctor risus. Aenean tempor ullamcorper leo. Vivamus sed magna quis ligula eleifend adipiscing. Duis orci. Aliquam sodales tortor vitae ipsum. Aliquam nulla. Duis aliquam molestie erat. Ut et mauris vel pede varius sollicitudin</body></html>';
			frame.contentWindow.document.open();
			frame.contentWindow.document.write(html);
			frame.contentWindow.document.close();
			this.frame = frame.contentWindow.document;
			this.preview = $(frame.contentWindow.document.body);
			 				 
			this.src.type.change(function() {
				self.src.url.change();
			});

			this.src.width.change(function() {
				if (self.swf) {
					var w = parseInt($(this).val())||'';
					$(this).val(w);
					self.swf.css('width', w);
					self.swf.children('embed').css('width', w);
				} else {
					$(this).val('');
				}
			});

			this.src.height.change(function() {
				if (self.swf) {
					var h = parseInt($(this).val())||'';
					$(this).val(h);
					self.swf.css('height', h);
					self.swf.children('embed').css('height', h);
				} else {
					$(this).val('');
				}
			});
			
			this.src.wmode.change(function() {
				if (self.swf) {
					var wm = $(this).val();
					if (wm) {
						self.swf.attr('wmode', wm);
						self.swf.children('embed').attr('wmode', wm);
					} else {
						self.swf.removeAttr('wmode');
						self.swf.children('embed').removeAttr('wmode');
					}
				}
			});
			
			this.src.align.change(function() {
				var v = $(this).val(), f = v=='left' || v=='right';
				if (self.swf) {
					self.swf.css({
						'float' : f ? v : '',
						'vertical-align' : f ? '' : v
					});
				} else {
					$(this).val('');
				}
			});
			
			this.src.margin.change(function() {
				if (self.swf) {
					var m = self.src.margin.val();
					if (m.css) {
						self.swf.css('margin', m.css);
					} else {
						self.swf.css('margin-top', m.top);
						self.swf.css('margin-right', m.right);
						self.swf.css('margin-bottom', m.bottom);
						self.swf.css('margin-left', m.left);						
					}
				}
			});
			
			this.src.url.change(function() {
				var url = self.rte.utils.absoluteURL($(this).val()), i, swf;
				if (url) {
					i = self.rte.utils.mediaInfo(self.src.type.val());
					if (!i) {
						i = self.rte.util.mediaInfo('application/x-shockwave-flash');
					}
					swf = '<object classid="'+i.classid+'" codebase="'+i.codebase+'"><param name="src" value="'+url+'" /><embed quality="high" src="'+url+'" type="'+i.type+'"></object>';
					self.preview.children('object').remove().end().prepend(swf);
					self.swf = self.preview.children('object').eq(0);
				} else if (self.swf){
					self.swf.remove();
					self.swf = null;
				}
				self.src.width.trigger('change');
				self.src.height.trigger('change');
				self.src.align.trigger('change');

			}).trigger('change');
		};
		
		this.set = function() {
			self.swf = null
			var url = this.rte.utils.absoluteURL(this.src.url.val()),
				w = parseInt(this.src.width.val()) || '',
				h = parseInt(this.src.height.val()) || '',
				wm = this.src.wmode.val(),
				a = this.src.align.val(),
				f = a == 'left' || a == 'right' ? a : '',
				mid = this.placeholder ? this.placeholder.attr('rel') : '', o, _o, c, 
				m = this.src.margin.val(), margin;

			
			
			if (!url) {
				if (this.placeholder) {
					this.placeholder.remove();
					delete this.rte.filter.scripts[mid];
				}
			} else {
				i = self.rte.utils.mediaInfo(self.src.type.val());
				if (!i) {
					i = self.rte.util.mediaInfo('application/x-shockwave-flash');
				}
				c = this.rte.filter.videoHostRegExp.test(url) ? url.replace(this.rte.filter.videoHostRegExp, "$2") : i.type.replace(/^\w+\/(.+)/, "$1");

				o = {
					obj : {
						classid : i.classid[0],
						codebase : i.codebase,
						style : {}
					},
					params :[ { name : 'src', value : url } ],
					embed :{
						src : url,
						type : i.type,
						quality : 'high',
						wmode : wm,
						style : {}
					}
				};
				
				if (w) {
					o.obj.width = w;
					o.embed.width = w;
				}
				if (h) {
					o.obj.height = h;
					o.embed.height = h;
				}
				if (f) {
					o.obj.style['float'] = f;
				} else if (a) {
					o.obj.style['vertical-align'] = a;
				}
				
				if (m.css) {
					margin = { margin : m.css };
				} else {
					margin = {
						'margin-top' : m.top,
						'margin-right' : m.right,
						'margin-bottom' : m.bottom,
						'margin-left' : m.left
					};
				}
				
				o.obj.style = $.extend({}, o.obj.style, margin);
				
				if (this.placeholder && mid) {
					_o = this.rte.filter.scripts[mid]||{};

					o = $.extend(true, _o, o);
					delete o.obj.style.width;
					delete o.obj.style.height;
					delete o.embed.style.width;
					delete o.embed.style.height;
					this.rte.filter.scripts[mid] = o;
					this.placeholder.removeAttr('class');
				} else {
					var id = 'media'+Math.random().toString().substring(2);
					this.rte.filter.scripts[id] = o;
					this.placeholder = $(this.rte.dom.create('img')).attr('rel', id).attr('src', this.rte.filter.url+'pixel.gif');
					var ins = true;
				}
				this.placeholder.attr('title', this.rte.utils.encode(url)).attr('width', w||150).attr('height', h||100).addClass('elrte-protected elrte-media elrte-media-'+c).css(o.obj.style);
				if (f) {
					this.placeholder.css('float', f).css('vertical-align', '');
				} else if (a) {
					this.placeholder.css('float', '').css('vertical-align', a);
				} else {
					this.placeholder.css('float', '').css('vertical-align', '');
				}
				
				if (ins) {
					this.rte.window.focus();
					this.rte.selection.restoreIERange();
					this.rte.selection.insertNode(this.placeholder.get(0));
				}
			}
		}
		
		this.update = function() {
			this.domElem.removeClass('disabled');
			var n = this.rte.selection.getNode();
			this.domElem.toggleClass('active', n && n.nodeName == 'IMG' && $(n).hasClass('elrte-media'))
			
		}
		
		
	}
})(jQuery);