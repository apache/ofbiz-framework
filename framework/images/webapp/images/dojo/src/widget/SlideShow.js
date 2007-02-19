/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.SlideShow");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.lfx.*");
dojo.require("dojo.html.display");


dojo.widget.defineWidget(
	"dojo.widget.SlideShow",
	dojo.widget.HtmlWidget,
	{
		/*
		summary
			Takes a bunch of pictures and displays them one by one, like a slide show.
		Usage
			<img dojoType="SlideShow" 
				imgUrls="images/1.jpg;images/2.jpg;images/3.jpg;images/4.jpg;images/5.jpg;images/6.jpg" 
				imgUrlBase="/foo/bar/images/"
				transitionInterval="700"
				delay="7000" 
				src="images/1.jpg"
				imgWidth="400" imgHeight="300" />
		*/

		templatePath: dojo.uri.dojoUri("src/widget/templates/SlideShow.html"),
		templateCssPath: dojo.uri.dojoUri("src/widget/templates/SlideShow.css"),

		// imgUrls: String[]
		//	List of images to use
		//	Ex: "1.jpg;2.jpg;3.jpg"
		imgUrls: [],
		
		// imgUrlBase: String
		//	Path prefix to prepend to each file specified in imgUrls
		//	Ex: "/foo/bar/images/"
		imgUrlBase: "",

		// delay: Integer
		//	Number of milliseconds to display each image
		delay: 4000,

		// transitionInterval: Integer
		//	Number of milliseconds to transition between pictures
		transitionInterval: 2000,
		
		// imgWidth: Integer
		//	Width of image in pixels
		imgWidth: 800,
		
		// imgHeight: Integer
		//	Height of image in pixels
		imgHeight: 600,

		// preventCache: Boolean
		//	If true, download the image every time, rather than using cached version in browser
		preventCache: false,
		
		// stopped: Boolean
		//	is Animation paused?
		stopped: false,

		////// Properties
		_urlsIdx: 0, 		// where in the images we are
		_background: "img2", // what's in the bg
		_foreground: "img1", // what's in the fg
		fadeAnim: null, // references our animation

		///////// our DOM nodes 
		startStopButton: null,
		img1: null,
		img2: null,

		postMixInProperties: function(){
			this.width = this.imgWidth + "px";
			this.height = this.imgHeight + "px";
		},

		fillInTemplate: function(){
			// safari will cache the images and not fire an image onload event if
			// there are only two images in the slideshow
			if(dojo.render.html.safari && this.imgUrls.length == 2) {
				this.preventCache = true;
			}
			dojo.html.setOpacity(this.img1, 0.9999);
			dojo.html.setOpacity(this.img2, 0.9999);
			if(this.imgUrls.length>1){
				this.img2.src = this.imgUrlBase+this.imgUrls[this._urlsIdx++] + this._getUrlSuffix();
				this._endTransition();
			}else{
				this.img1.src = this.imgUrlBase+this.imgUrls[this._urlsIdx++] + this._getUrlSuffix();
			}
		},

		_getUrlSuffix: function() {
			if(this.preventCache) {
				return "?ts=" + (new Date()).getTime();
			} else {
				return "";
			}
		},
		
		togglePaused: function(){
			// summary: pauses or restarts the slideshow
			if(this.stopped){
				this.stopped = false;
				this._backgroundImageLoaded();
				this.startStopButton.value= "pause";
			}else{
				this.stopped = true;
				this.startStopButton.value= "play";
			}
		},

		_backgroundImageLoaded: function(){
			// start fading out the _foreground image
			if(this.stopped){ return; }

			// actually start the fadeOut effect
			// NOTE: if we wanted to use other transition types, we'd set them up
			// 		 here as well
			if(this.fadeAnim) {
				this.fadeAnim.stop();
			}
			this.fadeAnim = dojo.lfx.fadeOut(this[this._foreground], 
				this.transitionInterval, null);
			dojo.event.connect(this.fadeAnim,"onEnd",this,"_endTransition");
			this.fadeAnim.play();
		},

		_endTransition: function(){
			// move the _foreground image to the _background 
			with(this[this._background].style){ zIndex = parseInt(zIndex)+1; }
			with(this[this._foreground].style){ zIndex = parseInt(zIndex)-1; }

			// fg/bg book-keeping
			var tmp = this._foreground;
			this._foreground = this._background;
			this._background = tmp;
			// keep on truckin
			this._loadNextImage();
		},

		_loadNextImage: function(){
			// summary
			//	after specified delay,
			//	load a new image in that container, and call _backgroundImageLoaded()
			//	when it finishes loading
			dojo.event.kwConnect({
				srcObj: this[this._background],
				srcFunc: "onload",
				adviceObj: this,
				adviceFunc: "_backgroundImageLoaded",
				once: true, // kill old connections
				delay: this.delay
			});
			dojo.html.setOpacity(this[this._background], 1.0);
			this[this._background].src = this.imgUrlBase+this.imgUrls[this._urlsIdx++];
			if(this._urlsIdx>(this.imgUrls.length-1)){
				this._urlsIdx = 0;
			}
		}
	}
);
