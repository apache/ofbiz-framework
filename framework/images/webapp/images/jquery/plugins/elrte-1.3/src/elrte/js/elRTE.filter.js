(function($) {
	/**
	 * @class Filter - clean editor content
	 * @param elRTE editor instance
	 * @author Dmitry (dio) Levashov, dio@std42.ru
	 */
	elRTE.prototype.filter = function(rte) {
		var self = this,
			n = $('<span/>').addClass('elrtetesturl').appendTo(document.body)[0];
		// media replacement image base url
		this.url = (typeof(n.currentStyle )!= "undefined" ? n.currentStyle['backgroundImage'] : document.defaultView.getComputedStyle(n, null)['backgroundImage']).replace(/^url\((['"]?)([\s\S]+\/)[\s\S]+\1\)$/i, "$2");
		$(n).remove();

		this.rte = rte;
		// flag - return xhtml tags?
		this.xhtml = /xhtml/i.test(rte.options.doctype);
		// boolean attributes
		this.boolAttrs = rte.utils.makeObject('checked,compact,declare,defer,disabled,ismap,multiple,nohref,noresize,noshade,nowrap,readonly,selected'.split(','));
		// tag regexp
		this.tagRegExp = /<(\/?)([\w:]+)((?:\s+[a-z\-]+(?:\s*=\s*(?:(?:"[^"]*")|(?:'[^']*')|[^>\s]+))?)*)\s*\/?>/g;
		// this.tagRegExp = /<(\/?)([\w:]+)((?:\s+\w+(?:\s*=\s*(?:(?:"[^"]*")|(?:'[^']*')|[^>\s]+))?)*)\s*\/?>/g;		
		// opened tag regexp
		this.openTagRegExp = /<([\w:]+)((?:\s+\w+(?:\s*=\s*(?:(?:"[^"]*")|(?:'[^']*')|[^>\s]+))?)*)\s*\/?>/g;
		// attributes regexp
		this.attrRegExp = /(\w+)(?:\s*=\s*(?:(?:"[^"]*")|(?:'[^']*')|[^\s]+))?/g;
		// script tag regexp
		this.scriptRegExp = /<script([^>]*)>([\s\S]*?)<\/script>/gi;
		// style tag regexp
		this.styleRegExp = /(<style([^>]*)>[\s\S]*?<\/style>)/gi;
		// link tag regexp
		this.linkRegExp = /(<link([^>]+)>)/gi;
		// cdata regexp
		this.cdataRegExp = /<!\[CDATA\[([\s\S]+)\]\]>/g;
		// object tag regexp
		this.objRegExp = /<object([^>]*)>([\s\S]*?)<\/object>/gi;
		// embed tag regexp
		this.embRegExp = /<(embed)((?:\s+\w+(?:\s*=\s*(?:(?:"[^"]*")|(?:'[^']*')|[^>\s]+))?)*)\s*>/gi;
		// param tag regexp
		this.paramRegExp = /<(param)((?:\s+\w+(?:\s*=\s*(?:(?:"[^"]*")|(?:'[^']*')|[^>\s]+))?)*)\s*>/gi;
		// iframe tag regexp
		this.iframeRegExp = /<iframe([^>]*)>([\s\S]*?)<\/iframe>/gi;

		// yandex maps regexp
		this.yMapsRegExp = /<div\s+([^>]*id\s*=\s*('|")?YMapsID[^>]*)>/gi;
		// google maps regexp
		this.gMapsRegExp = /<iframe\s+([^>]*src\s*=\s*"http:\/\/maps\.google\.\w+[^>]*)>([\s\S]*?)<\/iframe>/gi;
		// video hostings url regexp
		this.videoHostRegExp = /^(http:\/\/[\w\.]*)?(youtube|vimeo|rutube).*/i;
		// elrte services classes regexp
		this.serviceClassRegExp = /<(\w+)([^>]*class\s*=\s*"[^>]*elrte-[^>]*)>\s*(<\/\1>)?/gi;
		this.pagebreakRegExp = /<(\w+)([^>]*style\s*=\s*"[^>]*page-break[^>]*)>\s*(<\/\1>)?/gi;
		
		this.pbRegExp = new RegExp('<!-- pagebreak -->', 'gi');
		// allowed tags
		this.allowTags = rte.options.allowTags.length ? rte.utils.makeObject(rte.options.allowTags) : null;
		// denied tags
		this.denyTags = rte.options.denyTags.length ? rte.utils.makeObject(rte.options.denyTags) : null;
		// deny attributes
		this.denyAttr = rte.options.denyAttr ? rte.utils.makeObject(rte.options.denyAttr) : null;
		// deny attributes for pasted html
		this.pasteDenyAttr = rte.options.pasteDenyAttr ? rte.utils.makeObject(rte.options.pasteDenyAttr) : null;
		// font sizes to convert size attr into css property
		this.fontSize = ['medium', 'xx-small', 'small', 'medium','large','x-large','xx-large' ];
		// font families regexp to detect family by font name
		this.fontFamily = {
			'sans-serif' : /^(arial|tahoma|verdana)$/i,
			'serif'      : /^(times|times new roman)$/i,
			'monospace'  : /^courier$/i
		}
		// scripts storage
		this.scripts = {};
		// cached chains of rules
		this._chains = {};
		
		// cache chains
		$.each(this.chains, function(n) {
			self._chains[n] = [];
			$.each(this, function(i, r) {
				typeof(self.rules[r]) == 'function' && self._chains[n].push(self.rules[r]);
			});
		});

		/**
		 * filtering through required chain
		 *
		 * @param  String  chain name
		 * @param  String  html-code
		 * @return String
		 **/
		this.proccess = function(chain, html) {
			// remove whitespace at the begin and end
			html = $.trim(html).replace(/^\s*(&nbsp;)+/gi, '').replace(/(&nbsp;|<br[^>]*>)+\s*$/gi, '');
			// pass html through chain
			$.each(this._chains[chain]||[], function() {
				html = this.call(self, html);
			});
			html = html.replace(/\t/g, '  ').replace(/\r/g, '').replace(/\s*\n\s*\n+/g, "\n")+'  ';
			return $.trim(html) ? html : ' ';
		}
		
		/**
		 * wrapper for "wysiwyg" chain filtering
		 *
		 * @param  String  
		 * @return String
		 **/
		this.wysiwyg = function(html) {
			return this.proccess('wysiwyg', html);
		}
		
		/**
		 * wrapper for "source" chain filtering
		 *
		 * @param  String  
		 * @return String
		 **/
		this.source = function(html) {
			return this.proccess('source', html);
		}
		
		/**
		 * wrapper for "source2source" chain filtering
		 *
		 * @param  String  
		 * @return String
		 **/
		this.source2source = function(html) {
			return this.proccess('source2source', html);
		}
		
		/**
		 * wrapper for "wysiwyg2wysiwyg" chain filtering
		 *
		 * @param  String  
		 * @return String
		 **/
		this.wysiwyg2wysiwyg = function(html) {
			return this.proccess('wysiwyg2wysiwyg', html);
		}
		
		/**
		 * Parse attributes from string into object
		 *
		 * @param  String  string of attributes  
		 * @return Object
		 **/
		this.parseAttrs = function(s) {
			var a = {},
				b = this.boolAttrs,
				m = s.match(this.attrRegExp),
				t, n, v;
				// this.rte.log(s)
			// this.rte.log(m)
			m && $.each(m, function(i, s) {
				t = s.split('=');
				n = $.trim(t[0]).toLowerCase();

				if (t.length>2) {
					t.shift();
					v = t.join('=');
				} else {
					v = b[n] ||t[1]||'';
				}
				a[n] = $.trim(v).replace(/^('|")(.*)(\1)$/, "$2");
			});

			a.style = this.rte.utils.parseStyle(a.style);
			// rte.log(a.style)
			a['class'] = this.rte.utils.parseClass(a['class']||'')
			return a;
		}
		
		/**
		 * Restore attributes string from hash
		 *
		 * @param  Object  attributes hash
		 * @return String
		 **/
		this.serializeAttrs = function(a, c) {
			var s = [], self = this;

			$.each(a, function(n, v) {
				if (n=='style') {
					v = self.rte.utils.serializeStyle(v, c);
				} else if (n=='class') {
					// self.rte.log(v)
					// self.rte.log(self.rte.utils.serializeClass(v))
					v = self.rte.utils.serializeClass(v);
				} 
				v && s.push(n+'="'+v+'"');
			});
			return s.join(' ');
		}
		
		/**
		 * Remove/replace denied attributes/style properties
		 *
		 * @param  Object  attributes hash
		 * @param  String  tag name to wich attrs belongs 
		 * @return Object
		 **/
		this.cleanAttrs = function(a, t) {
			var self = this, ra = this.replaceAttrs;

			// remove safari and mso classes
			$.each(a['class'], function(n) {
				/^(Apple-style-span|mso\w+)$/i.test(n) && delete a['class'][n];
			});

			function value(v) {
				return v+(/\d$/.test(v) ? 'px' : '');
			}

			$.each(a, function(n, v) {
				// replace required attrs with css
				ra[n] && ra[n].call(self, a, t);
				// remove/fix mso styles
				if (n == 'style') {
					$.each(v, function(sn, sv) {
						switch (sn) {
							case "mso-padding-alt":
							case "mso-padding-top-alt":
							case "mso-padding-right-alt":
							case "mso-padding-bottom-alt":
							case "mso-padding-left-alt":
							case "mso-margin-alt":
							case "mso-margin-top-alt":
							case "mso-margin-right-alt":
							case "mso-margin-bottom-alt":
							case "mso-margin-left-alt":
							case "mso-table-layout-alt":
							case "mso-height":
							case "mso-width":
							case "mso-vertical-align-alt":
								a.style[sn.replace(/^mso-|-alt$/g, '')] = value(sv);
								delete a.style[sn];
								break;

							case "horiz-align":
								a.style['text-align'] = sv;
								delete a.style[sn];
								break;

							case "vert-align":
								a.style['vertical-align'] = sv;
								delete a.style[sn];
								break;

							case "font-color":
							case "mso-foreground":
								a.style.color = sv;
								delete a.style[sn];
							break;

							case "mso-background":
							case "mso-highlight":
								a.style.background = sv;
								delete a.style[sn];
								break;

							case "mso-default-height":
								a.style['min-height'] = value(sv);
								delete a.style[sn];
								break;

							case "mso-default-width":
								a.style['min-width'] = value(sv);
								delete a.style[sn];
								break;

							case "mso-padding-between-alt":
								a.style['border-collapse'] = 'separate';
								a.style['border-spacing'] = value(sv);
								delete a.style[sn];
								break;

							case "text-line-through":
								if (sv.match(/(single|double)/i)) {
									a.style['text-decoration'] = 'line-through';
								}
								delete a.style[sn];
								break;

							case "mso-zero-height":
								if (sv == 'yes') {
									a.style.display = 'none';
								}
								delete a.style[sn];
								break;

							case 'font-weight':
								if (sv == 700) {
									a.style['font-weight'] = 'bold';
								}
								break;

							default:
								if (sn.match(/^(mso|column|font-emph|lang|layout|line-break|list-image|nav|panose|punct|row|ruby|sep|size|src|tab-|table-border|text-(?!align|decor|indent|trans)|top-bar|version|vnd|word-break)/)) {
									delete a.style[sn]
								}
						}
					});
				}
			});
			return a;
		}
		
	}

	// rules to replace tags
	elRTE.prototype.filter.prototype.replaceTags = {
		b         : { tag : 'strong' },
		big       : { tag : 'span', style : {'font-size' : 'large'} },
		center    : { tag : 'div',  style : {'text-align' : 'center'} },
		i         : { tag : 'em' },
		font      : { tag : 'span' },
		nobr      : { tag : 'span', style : {'white-space' : 'nowrap'} },
		menu      : { tag : 'ul' },
		plaintext : { tag : 'pre' },
		s         : { tag : 'strike' },
		small     : { tag : 'span', style : {'font-size' : 'small'}},
		u         : { tag : 'span', style : {'text-decoration' : 'underline'} },
		xmp       : { tag : 'pre' }
	}
	
	// rules to replace attributes
	elRTE.prototype.filter.prototype.replaceAttrs = {
		align : function(a, n) {
			switch (n) {
				case 'img':
					a.style[a.align.match(/(left|right)/) ? 'float' : 'vertical-align'] = a.align;
					break;
				
				case 'table':
					if (a.align == 'center') {
						a.style['margin-left'] = a.style['margin-right'] = 'auto';
					} else {
						a.style['float'] = a.align;
					}
					break;
					
				default:
					a.style['text-align'] = a.align;
			}
			delete a.align;
		},
		border : function(a) {
			!a.style['border-width'] && (a.style['border-width'] = (parseInt(a.border)||1)+'px');
			!a.style['border-style'] && (a.style['border-style'] = 'solid');
			delete a.border;
		},
		bordercolor : function(a) {
			!a.style['border-color'] && (a.style['border-color'] = a.bordercolor);
			delete a.bordercolor;
		},
		background : function(a) {
			!a.style['background-image'] && (a.style['background-image'] = 'url('+a.background+')');
			delete a.background;
		},
		bgcolor : function(a) {
			!a.style['background-color'] && (a.style['background-color'] = a.bgcolor);
			delete a.bgcolor;
		},
		clear : function(a) {
			a.style.clear = a.clear == 'all' ? 'both' : a.clear;
			delete a.clear;
		},
		color : function(a) {
			!a.style.color && (a.style.color = a.color);
			delete a.color;
		},
		face : function(a) {
			var f = a.face.toLowerCase();
			$.each(this.fontFamily, function(n, r) {
				if (f.match(r)) {
					a.style['font-family'] = f+','+n;
				}
			});
			delete a.face;
		},
		hspace : function(a, n) {
			if (n == 'img') {
				var v = parseInt(a.hspace)||0;
				!a.style['margin-left'] && (a.style['margin-left'] = v+'px');
				!a.style['margin-right'] && (a.style['margin-right'] = v+'px')
				delete a.hspace;
			}
		},
		size : function(a, n) {
			if (n != 'input') {
				a.style['font-size'] = this.fontSize[parseInt(a.size)||0]||'medium';
				delete a.size;
			}
		},
		valign : function(a) {
			if (!a.style['vertical-align']) {
				a.style['vertical-align'] = a.valign;
			}
			delete a.valign;
		},
		vspace : function(a, n) {
			if (n == 'img') {
				var v = parseInt(a.vspace)||0;
				!a.style['margin-top'] && (a.style['margin-top'] = v+'px');
				!a.style['margin-bottom'] && (a.style['margin-bottom'] = v+'px')
				delete a.hspace;
			}
		}
	}
	
	// rules collection
	elRTE.prototype.filter.prototype.rules = {
		/**
		 * If this.rte.options.allowTags is set - remove all except this ones
		 *
		 * @param String  html code
		 * @return String
		 **/
		allowedTags : function(html) {
			var a = this.allowTags;
			
			return a ? html.replace(this.tagRegExp, function(t, c, n) { return a[n.toLowerCase()] ? t : ''; }) : html;
		},
		/**
		 * If this.rte.options.denyTags is set - remove all deny tags
		 *
		 * @param String  html code
		 * @return String
		 **/
		deniedTags : function(html) {
			var d = this.denyTags; 

			return d ? html.replace(this.tagRegExp, function(t, c, n) { return d[n.toLowerCase()] ? '' : t }) : html;
		},
		
		/**
		 * Replace not allowed tags/attributes
		 *
		 * @param String  html code
		 * @return String
		 **/
		clean : function(html) {
			var self = this, 
				rt   = this.replaceTags,
				ra   = this.replaceAttrs, 
				da   = this.denyAttr,
				n;
			
			
			html = html.replace(/<!DOCTYPE([\s\S]*)>/gi, '')
				.replace(/<p [^>]*class="?MsoHeading"?[^>]*>(.*?)<\/p>/gi, "<p><strong>$1</strong></p>")
				.replace(/<span\s+style\s*=\s*"\s*mso-spacerun\s*:\s*yes\s*;?\s*"\s*>([\s&nbsp;]*)<\/span>/gi, "$1")
				.replace(/(<p[^>]*>\s*<\/p>|<p[^>]*\/>)/gi, '<br>')
				.replace(/(<\/p>)(?:\s*<br\s*\/?>\s*|\s*&nbsp;\s*)+\s*(<p[^>]*>)/gi, function(t, b, e) {
					return b+"\n"+e;
				})
				.replace(this.tagRegExp, function(t, c, n, a) {
					n = n.toLowerCase();
					
					if (c) {
						return '</'+(rt[n] ? rt[n].tag : n)+'>';
					}
					// self.rte.log(t)
					// create attributes hash and clean it
					a = self.cleanAttrs(self.parseAttrs(a||''), n);
					// self.rte.log(a)
					if (rt[n]) {
						rt[n].style && $.extend(a.style, rt[n].style);
						n = rt[n].tag;
					}
					
					da && $.each(a, function(na) {
						if (da[na]) {
							delete a[na];
						}
					});
					a = self.serializeAttrs(a);
					// self.rte.log(a)
					return '<'+n+(a?' ':'')+a+'>';
				});
				
			
			n = $('<div>'+html+'</div>');
			
			// remove empty spans and merge nested spans
			n.find('span:not([id]):not([class])').each(function() {
				var t = $(this);
				
				if (!t.attr('style')) {
					
					$.trim(t.html()).length ? self.rte.dom.unwrap(this) : t.remove();
					// t.children().length ? self.rte.dom.unwrap(this) : t.remove();
				}
			}).end().find('span span:only-child').each(function() {
				var t   = $(this), 
					p   = t.parent().eq(0), 
					tid = t.attr('id'), 
					pid = p.attr('id'), id, s, c;

				if (self.rte.dom.isOnlyNotEmpty(this) && (!tid || !pid)) {
					c = $.trim(p.attr('class')+' '+t.attr('class'))
					c && p.attr('class', c);
					s = self.rte.utils.serializeStyle($.extend(self.rte.utils.parseStyle($(this).attr('style')||''), self.rte.utils.parseStyle($(p).attr('style')||'')));
					s && p.attr('style', s);
					id = tid||pid;
					id && p.attr('id', id);
					this.firstChild ? $(this.firstChild).unwrap() : t.remove();
				}
			})
			.end().find('a[name]').each(function() {
				$(this).addClass('elrte-protected elrte-anchor');
			});
			
			return n.html()	
		},
		
		/**
		 * Clean pasted html
		 *
		 * @param String  html code
		 * @return String
		 **/
		cleanPaste : function(html) {
			var self = this, d = this.pasteDenyAttr;

			html = html
				.replace(this.scriptRegExp, '')
				.replace(this.styleRegExp, '')
				.replace(this.linkRegExp, '')
				.replace(this.cdataRegExp, '')
				.replace(/\<\!--[\s\S]*?--\>/g, '');
			
			if (this.rte.options.pasteOnlyText) {
				html = html.replace(this.tagRegExp, function(t, c, n) {
					return /br/i.test(n) || (c && /h[1-6]|p|ol|ul|li|div|blockquote|tr/i) ? '<br>' : '';
				}).replace(/(&nbsp;|<br[^>]*>)+\s*$/gi, '');
			} else if (d) {
				html = html.replace(this.openTagRegExp, function(t, n, a) {
					a = self.parseAttrs(a);
					$.each(a, function(an) {
						if (d[an]) {
							delete a[an];
						}
					});
					a = self.serializeAttrs(a, true);
					return '<'+n+(a?' ':'')+a+'>';
				});
			}
			return html; 
		},
		
		/**
		 * Replace script/style/media etc with placeholders
		 *
		 * @param String  html code
		 * @return String
		 **/
		replace : function(html) {
			var self = this, r = this.rte.options.replace||[], n;

			// custom replaces if set
			if (r.length) {
				$.each(r, function(i, f) {
					if (typeof(f) == 'function') {
						html = f.call(self, html);
					}
				});
			}

			/**
			 * Return media replacement - img html code
			 *
			 * @param Object  object to store in rel attr
			 * @param String  media mime-type
			 * @return String
			 **/
			function img(o, t) {
				var s = src(),
					c = s && self.videoHostRegExp.test(s) ? s.replace(self.videoHostRegExp, "$2") : t.replace(/^\w+\/(.+)/, "$1"),
					w = parseInt((o.obj ? o.obj.width || o.obj.style.width : 0)||(o.embed ? o.embed.width || o.embed.style.width : 0))||150,
					h = parseInt((o.obj ? o.obj.height || o.obj.style.height : 0)||(o.embed ? o.embed.height || o.embed.style.height : 0))||100,
					id = 'media'+Math.random().toString().substring(2),
					style ='',
					l;

				// find media src
				function src() {
					if (o.embed && o.embed.src) {
						return o.embed.src;
					}
					if (o.params && o.params.length) {
						l = o.params.length;
						while (l--) {
							if (o.params[l].name == 'src' || o.params[l].name == 'movie') {
								return o.params[l].value;
							}
						}
					}
				}
				if (o.obj && o.obj.style && o.obj.style['float']) {
					style = ' style="float:'+o.obj.style['float']+'"';
				}
				self.scripts[id] = o;
				return '<img src="'+self.url+'pixel.gif" class="elrte-media elrte-media-'+c+' elrte-protected" title="'+(s ? self.rte.utils.encode(s) : '')+'" rel="'+id+'" width="'+w+'" height="'+h+'"'+style+'>';
			}
			
			html = html
				.replace(this.styleRegExp, "<!-- ELRTE_COMMENT$1 -->")
				.replace(this.linkRegExp,  "<!-- ELRTE_COMMENT$1-->")
				.replace(this.cdataRegExp, "<!--[CDATA[$1]]-->")
				.replace(this.scriptRegExp, function(t, a, s) {
					var id;
					if (self.denyTags.script) {
						return '';
					}
					id = 'script'+Math.random().toString().substring(2);
					a = self.parseAttrs(a);
					!a.type && (a.type = 'text/javascript');
					self.scripts[id] = '<script '+self.serializeAttrs(a)+">"+s+"</script>";
					return '<!-- ELRTE_SCRIPT:'+(id)+' -->';
				})
				.replace(this.yMapsRegExp, function(t, a) {
					a = self.parseAttrs(a);
					a['class']['elrte-yandex-maps'] = 'elrte-yandex-maps';
					a['class']['elrte-protected'] = 'elrte-protected';
					return '<div '+self.serializeAttrs(a)+'>';
				})
				.replace(this.gMapsRegExp, function(t, a) {
					var id = 'gmaps'+Math.random().toString().substring(2), w, h;
					a = self.parseAttrs(a);
					w = parseInt(a.width||a.style.width||100);
					h = parseInt(a.height||a.style.height||100);
					self.scripts[id] = t;
					return '<img src="'+self.url+'pixel.gif" class="elrte-google-maps elrte-protected" id="'+id+'" style="width:'+w+'px;height:'+h+'px">';
				})
				.replace(this.objRegExp, function(t, a, c) {
					var m = c.match(self.embRegExp),
						o = { obj : self.parseAttrs(a), embed : m && m.length ? self.parseAttrs(m[0].substring(7)) : null, params : [] },
						i = self.rte.utils.mediaInfo(o.embed ? o.embed.type||'' : '', o.obj.classid||'');
					
					if (i) {
						if ((m = c.match(self.paramRegExp))) {
							$.each(m, function(i, p) {
								o.params.push(self.parseAttrs(p.substring(6)));
							});
						}
						!o.obj.classid  && (o.obj.classid  = i.classid[0]);
						!o.obj.codebase && (o.obj.codebase = i.codebase);
						o.embed && !o.embed.type && (o.embed.type = i.type);
						// ie bug with empty attrs
						o.obj.width == '1' && delete o.obj.width;
						o.obj.height == '1' && delete o.obj.height;
						if (o.embed) {
							o.embed.width == '1' && delete o.embed.width;
							o.embed.height == '1' && delete o.embed.height;
						}
						return img(o, i.type);
					}
					return t;
				})
				.replace(this.embRegExp, function(t, n, a) {
					var a = self.parseAttrs(a),
						i = self.rte.utils.mediaInfo(a.type||'');
					// ie bug with empty attrs
					a.width == '1' && delete a.width;
					a.height == '1' && delete a.height;
					return i ? img({ embed : a }, i.type) : t;
				})
				.replace(this.iframeRegExp, function(t, a) {
					var a = self.parseAttrs(a);
					var w = a.style.width || (parseInt(a.width) > 1 ? parseInt(a.width)+'px' : '100px');
					var h = a.style.height || (parseInt(a.height) > 1 ? parseInt(a.height)+'px' : '100px');
					var id = 'iframe'+Math.random().toString().substring(2);
					self.scripts[id] = t;
					var img = '<img id="'+id+'" src="'+self.url+'pixel.gif" class="elrte-protected elrte-iframe" style="width:'+w+'; height:'+h+'">';
					return img;
				})
				.replace(this.vimeoRegExp, function(t, n, a) {
					a = self.parseAttrs(a);
					delete a.frameborder;
					a.width == '1' && delete a.width;
					a.height == '1' && delete a.height;
					a.type = 'application/x-shockwave-flash';
					return img({ embed : a }, 'application/x-shockwave-flash');
				})
				.replace(/<\/(embed|param)>/gi, '')
				.replace(this.pbRegExp, function() {
					return '<img src="'+self.url+'pixel.gif" class="elrte-protected elrte-pagebreak">';
				});


			n = $('<div>'+html+'</div>');
			
			// remove empty spans and merge nested spans
			// n.find('span:not([id]):not([class])').each(function() {
			// 	var t = $(this);
			// 	
			// 	if (!t.attr('style')) {
			// 		$.trim(t.html()).length ? self.rte.dom.unwrap(this) : t.remove();
			// 		// t.children().length ? self.rte.dom.unwrap(this) : t.remove();
			// 	}
			// }).end().find('span span:only-child').each(function() {
			// 	var t   = $(this), 
			// 		p   = t.parent().eq(0), 
			// 		tid = t.attr('id'), 
			// 		pid = p.attr('id'), id, s, c;
			// 
			// 	if (self.rte.dom.is(this, 'onlyChild') && (!tid || !pid)) {
			// 		c = $.trim(p.attr('class')+' '+t.attr('class'))
			// 		c && p.attr('class', c);
			// 		s = self.rte.utils.serializeStyle($.extend(self.rte.utils.parseStyle($(this).attr('style')||''), self.rte.utils.parseStyle($(p).attr('style')||'')));
			// 		s && p.attr('style', s);
			// 		id = tid||pid;
			// 		id && p.attr('id', id);
			// 		this.firstChild ? $(this.firstChild).unwrap() : t.remove();
			// 	}
			// })
			// .end().find('a[name]').each(function() {
			// 	$(this).addClass('elrte-anchor');
			// });


			if (!this.rte.options.allowTextNodes) {
				// wrap inline nodes with p
				var dom = this.rte.dom,
					nodes = [],
					w = [];
				
				if ($.browser.msie) {
					for (var i = 0; i<n[0].childNodes.length; i++) {
						nodes.push(n[0].childNodes[i])
					}
				} else {
					nodes = Array.prototype.slice.call(n[0].childNodes);
				}
				

				function wrap() {
					if (w.length && dom.filter(w, 'notEmpty').length) {
						dom.wrap(w, document.createElement('p'));
					}
					w = [];
				}	
				$.each(nodes, function(i, n) {
					if (dom.is(n, 'block')) {
						wrap();
					} else {
						if (w.length && n.previousSibling != w[w.length-1]) {
							wrap();
						}
						w.push(n);
					}
				});
				wrap();
			}
			
			return n.html();
		},
		/**
		 * Restore script/style/media etc from placeholders
		 *
		 * @param String  html code
		 * @return String
		 **/
		restore : function(html) {
			var self =this, r = this.rte.options.restore||[];

			// custom restore if set
			if (r.length) {
				$.each(r, function(i, f) {
					if (typeof(f) == 'function') {
						html = f.call(self, html);
					}
				});
			}
			
			html = html
				.replace(/\<\!--\[CDATA\[([\s\S]*?)\]\]--\>/gi, "<![CDATA[$1]]>")
				.replace(/\<\!--\s*ELRTE_SCRIPT\:\s*(script\d+)\s*--\>/gi, function(t, n) {
					if (self.scripts[n]) {
						t = self.scripts[n];
						delete self.scripts[n];
					}
					return t||'';
				})
				.replace(/\<\!-- ELRTE_COMMENT([\s\S]*?) --\>/gi, "$1")
				.replace(this.serviceClassRegExp, function(t, n, a, e) {

					var a = self.parseAttrs(a), j, o = '';
					// alert(t)
					if (a['class']['elrte-google-maps']) {
						var t = '';
						if (self.scripts[a.id]) {
							t = self.scripts[a.id];
							delete self.scripts[a.id]
						}
						return t;
					} else if (a['class']['elrte-iframe']) {
						return self.scripts[a.id] || '';
					} else if (a['class']['elrtebm']) {
						return '';
					} else if (a['class']['elrte-media']) {
						// alert(a.rel)
						// return ''
						// j = a.rel ? JSON.parse(self.rte.utils.decode(a.rel)) : {};
						j = self.scripts[a.rel]||{};
						j.params && $.each(j.params, function(i, p) {
							o += '<param '+self.serializeAttrs(p)+">\n";
						});
						j.embed && (o+='<embed '+self.serializeAttrs(j.embed)+">");
						j.obj && (o = '<object '+self.serializeAttrs(j.obj)+">\n"+o+"\n</object>\n");
						return o||t;
					} else if (a['class']['elrte-pagebreak']) {
						return '<!-- pagebreak -->';
					}
					$.each(a['class'], function(n) {
						if (/^elrte-\w+/i.test(n)) {
							delete(a['class'][n]);
						}
						// /^elrte\w+/i.test(n) && delete(a['class'][n]); 
					});
					return '<'+n+' '+self.serializeAttrs(a)+'>'+(e||'');

				});
			
			return html;
		},
		/**
		 * compact styles and move tags and attributes names in lower case(for ie&opera)
		 *
		 * @param String  html code
		 * return String
		 **/
		compactStyles : function(html) {
			var self = this;

			return html.replace(this.tagRegExp, function(t, c, n, a) {
				a = !c && a ? self.serializeAttrs(self.parseAttrs(a), true) : '';
				return '<'+c+n.toLowerCase()+(a?' ':'')+a+'>';
			});
		},
		/**
		 * return xhtml tags
		 *
		 * @param String  html code
		 * return String
		 **/
		xhtmlTags : function(html) {
			return this.xhtml ? html.replace(/<(img|hr|br|embed|param|link|area)([^>]*\/*)>/gi, "<$1$2 />") : html;
		}
	}
	
	/**
	 * Chains configuration
	 * Default chains 
	 * wysiwyg - proccess html from source for wysiwyg editor mode
	 * source  - proccess html from wysiwyg for source editor mode
	 * paste   - clean pasted html
	 * wysiwyg2wysiwyg - ciclyc rule to clean html from wysiwyg for wysiwyg paste
	 * source2source - ciclyc rule to clean html from source for source paste
	 * deniedTags is in the end of chain to protect google maps iframe from removed
	 **/
	elRTE.prototype.filter.prototype.chains = {
		wysiwyg         : ['replace', 'clean', 'allowedTags', 'deniedTags', 'compactStyles'],
		source          : ['clean', 'allowedTags', 'restore', 'compactStyles', 'xhtmlTags'],
		paste           : ['clean', 'allowedTags', 'cleanPaste', 'replace', 'deniedTags', 'compactStyles'],
		wysiwyg2wysiwyg : ['clean', 'allowedTags', 'restore', 'replace', 'deniedTags', 'compactStyles'],
		source2source   : ['clean', 'allowedTags', 'replace', 'deniedTags', 'restore', 'compactStyles', 'xhtmlTags']
	}
	

	
})(jQuery);
