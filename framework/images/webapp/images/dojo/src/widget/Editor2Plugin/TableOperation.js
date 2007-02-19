/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.Editor2Plugin.TableOperation");

dojo.require("dojo.widget.Editor2");

//subscribe to dojo.widget.RichText::init, not onLoad because after onLoad
//the stylesheets for the editing areas are already applied and the prefilters
//are executed, so we have to insert our own trick before that point
dojo.event.topic.subscribe("dojo.widget.RichText::init", function(editor){
	if(dojo.render.html.ie){
		//add/remove a class to a table with border=0 to show the border when loading/saving
		editor.contentDomPreFilters.push(dojo.widget.Editor2Plugin.TableOperation.showIETableBorder);
		editor.contentDomPostFilters.push(dojo.widget.Editor2Plugin.TableOperation.removeIEFakeClass);
	}
	//create a toggletableborder command for this editor so that tables without border can be seen
	editor.getCommand("toggletableborder");
});

dojo.lang.declare("dojo.widget.Editor2Plugin.deleteTableCommand", dojo.widget.Editor2Command,
{
	execute: function(){
		var table = dojo.withGlobal(this._editor.window, "getAncestorElement", dojo.html.selection, ['table']);
		if(table){
			dojo.withGlobal(this._editor.window, "selectElement", dojo.html.selection, [table]);
			this._editor.execCommand("inserthtml", " "); //Moz does not like an empty string, so a space here instead
		}
	},
	getState: function(){
		if(this._editor._lastStateTimestamp > this._updateTime || this._state == undefined){
			this._updateTime = this._editor._lastStateTimestamp;
			var table = dojo.withGlobal(this._editor.window, "hasAncestorElement", dojo.html.selection, ['table']);
			this._state = table ? dojo.widget.Editor2Manager.commandState.Enabled : dojo.widget.Editor2Manager.commandState.Disabled;
		}
		return this._state;
	},
	getText: function(){
		return 'Delete Table';
	}
});

dojo.lang.declare("dojo.widget.Editor2Plugin.toggleTableBorderCommand", dojo.widget.Editor2Command,
	function(){
		this._showTableBorder = false;
		dojo.event.connect(this._editor, "editorOnLoad", this, 'execute');
	},
{
	execute: function(){
		if(this._showTableBorder){
			this._showTableBorder = false;
			if(dojo.render.html.moz){
				this._editor.removeStyleSheet(dojo.uri.dojoUri("src/widget/templates/Editor2/showtableborder_gecko.css"));
			}else if(dojo.render.html.ie){
				this._editor.removeStyleSheet(dojo.uri.dojoUri("src/widget/templates/Editor2/showtableborder_ie.css"));
			}
		}else{
			this._showTableBorder = true;
			if(dojo.render.html.moz){
				this._editor.addStyleSheet(dojo.uri.dojoUri("src/widget/templates/Editor2/showtableborder_gecko.css"));
			}else if(dojo.render.html.ie){
				this._editor.addStyleSheet(dojo.uri.dojoUri("src/widget/templates/Editor2/showtableborder_ie.css"));
			}
		}
		
	},
	getText: function(){
		return 'Toggle Table Border';
	},
	getState: function(){
		return this._showTableBorder ? dojo.widget.Editor2Manager.commandState.Latched : dojo.widget.Editor2Manager.commandState.Enabled;
	}
});

dojo.widget.Editor2Plugin.TableOperation = {
	getCommand: function(editor, name){
		switch(name.toLowerCase()){
			case 'toggletableborder':
				return new dojo.widget.Editor2Plugin.toggleTableBorderCommand(editor, name);
			case 'inserttable':
				return new dojo.widget.Editor2DialogCommand(editor, 'inserttable',
					{contentFile: "dojo.widget.Editor2Plugin.InsertTableDialog",
					contentClass: "Editor2InsertTableDialog",
					title: "Insert/Edit Table", width: "450px", height: "250px"})
			case 'deletetable':
				return new dojo.widget.Editor2Plugin.deleteTableCommand(editor, name);
		}
	},
	getToolbarItem: function(name){
		var name = name.toLowerCase();

		var item;
		switch(name){
			case 'inserttable':
			case 'toggletableborder':
				item = new dojo.widget.Editor2ToolbarButton(name);
		}

		return item;
	},
	getContextMenuGroup: function(name, contextmenuplugin){
		return new dojo.widget.Editor2Plugin.TableContextMenuGroup(contextmenuplugin);
	},
	showIETableBorder: function(dom){
		var tables = dom.getElementsByTagName('table');
		dojo.lang.forEach(tables, function(t){
			dojo.html.addClass(t, "dojoShowIETableBorders");
		});
		return dom;
	},
	removeIEFakeClass: function(dom){
		var tables = dom.getElementsByTagName('table');
		dojo.lang.forEach(tables, function(t){
			dojo.html.removeClass(t, "dojoShowIETableBorders");
		});
		return dom;
	}
}

//register commands: toggletableborder, inserttable, deletetable
dojo.widget.Editor2Manager.registerHandler(dojo.widget.Editor2Plugin.TableOperation.getCommand);

//register toggletableborder and inserttable as toolbar item
dojo.widget.Editor2ToolbarItemManager.registerHandler(dojo.widget.Editor2Plugin.TableOperation.getToolbarItem);

//add context menu support if dojo.widget.Editor2Plugin.ContextMenu is included before this plugin
if(dojo.widget.Editor2Plugin.ContextMenuManager){
	dojo.widget.Editor2Plugin.ContextMenuManager.registerGroup('Table', dojo.widget.Editor2Plugin.TableOperation.getContextMenuGroup);

	dojo.declare("dojo.widget.Editor2Plugin.TableContextMenuGroup",
		dojo.widget.Editor2Plugin.SimpleContextMenuGroup,
	{
		createItems: function(){
			this.items.push(dojo.widget.createWidget("Editor2ContextMenuItem", {caption: "Delete Table", command: 'deletetable'}));
			this.items.push(dojo.widget.createWidget("Editor2ContextMenuItem", {caption: "Table Property", command: 'inserttable', iconClass: "TB_Button_Icon TB_Button_Table"}));
		},
		checkVisibility: function(){
			var curInst = dojo.widget.Editor2Manager.getCurrentInstance();
			var table = dojo.withGlobal(curInst.window, "hasAncestorElement", dojo.html.selection, ['table']);

			if(dojo.withGlobal(curInst.window, "hasAncestorElement", dojo.html.selection, ['table'])){
				this.items[0].show();
				this.items[1].show();
				return true;
			}else{
				this.items[0].hide();
				this.items[1].hide();
				return false;
			}
		}
	});
}