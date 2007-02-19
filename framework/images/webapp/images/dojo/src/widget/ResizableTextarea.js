/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.ResizableTextarea");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.LayoutContainer");
dojo.require("dojo.widget.ResizeHandle");

dojo.widget.defineWidget(
	"dojo.widget.ResizableTextarea",
	dojo.widget.HtmlWidget,
{
	// summary
	//	A resizable textarea.
	//	Takes all the parameters (name, value, etc.) that a vanilla textarea takes.
	// usage
	//	<textarea dojoType="ResizableTextArea">...</textarea>

	templatePath: dojo.uri.dojoUri("src/widget/templates/ResizableTextarea.html"),
	templateCssPath: dojo.uri.dojoUri("src/widget/templates/ResizableTextarea.css"),

	fillInTemplate: function(args, frag){
		this.textAreaNode = this.getFragNodeRef(frag).cloneNode(true);

		// FIXME: Safari apparently needs this!
		dojo.body().appendChild(this.domNode);

		this.rootLayout = dojo.widget.createWidget(
			"LayoutContainer",
			{
				minHeight: 50,
				minWidth: 100
			},
			this.rootLayoutNode
		);

		// TODO: all this code should be replaced with a template
		// (especially now that templates can contain subwidgets)
		this.textAreaContainer = dojo.widget.createWidget(
			"LayoutContainer",
			{ layoutAlign: "client" },
			this.textAreaContainerNode
		);
		this.rootLayout.addChild(this.textAreaContainer);

		this.textAreaContainer.domNode.appendChild(this.textAreaNode);
		with(this.textAreaNode.style){
			width="100%";
			height="100%";
		}

		this.statusBar = dojo.widget.createWidget(
			"LayoutContainer",
			{ 
				layoutAlign: "bottom", 
				minHeight: 28
			},
			this.statusBarContainerNode
		);
		this.rootLayout.addChild(this.statusBar);

		this.statusLabel = dojo.widget.createWidget(
			"LayoutContainer",
			{ 
				layoutAlign: "client", 
				minWidth: 50
			},
			this.statusLabelNode
		);
		this.statusBar.addChild(this.statusLabel);

		this.resizeHandle = dojo.widget.createWidget(
			"ResizeHandle", 
			{ targetElmId: this.rootLayout.widgetId },
			this.resizeHandleNode
		);
		this.statusBar.addChild(this.resizeHandle);
	}
});
