<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
  -->

<script type="text/javascript">
    //var djConfig = {
    //    isDebug: false
    //};
    
    dojo.require("dojo.widget.*");
    dojo.require("dojo.event.*");
    dojo.require("dojo.io.*");

    var treeSelected = false;
    var contentRoot = '${contentRoot?if_exists}';
	var webSiteId = '${webSiteId?if_exists}';        
    var editorUrl = '<@ofbizUrl>/views/WebSiteCMSContent</@ofbizUrl>';
    var aliasUrl = '<@ofbizUrl>/views/WebSiteCMSPathAlias</@ofbizUrl>';
    var metaUrl = '<@ofbizUrl>/views/WebSiteCMSMetaInfo</@ofbizUrl>';
        
    dojo.addOnLoad(function() {
		dojo.event.topic.subscribe("webCmsNodeSelected",
			 function(message) {
			    treeSelected = true;
                callEditor(false, message.node.widgetId, message.node.object);
			 }
		);
		dojo.event.topic.subscribe("newLong/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditor(true, node.widgetId, '', 'ELECTRONIC_TEXT');
            }
        );
        dojo.event.topic.subscribe("newShort/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditor(true, node.widgetId, '', 'SHORT_TEXT');
            }
        );
        dojo.event.topic.subscribe("newUrl/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditor(true, node.widgetId, '', 'URL_RESOURCE');
            }
        );
        dojo.event.topic.subscribe("newImage/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditor(true, node.widgetId, '', 'IMAGE_OBJECT');
            }
        );
        dojo.event.topic.subscribe("newVideo/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditor(true, node.widgetId, '', 'VIDEO_OBJECT');
            }
        );
        dojo.event.topic.subscribe("newAudio/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditor(true, node.widgetId, '', 'AUDIO_OBJECT');
            }
        );
        dojo.event.topic.subscribe("newObject/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditor(true, node.widgetId, '', 'OTHER_OBJECT');
            }
        );

		var cmsdata = dojo.byId("cmsdata");
		if (cmsdata) {
		    createEditor(cmsdata.value);
		} else {
		    createEditor();
        }
        //alert("On load called!");
	});

    function createEditor(text) {
        var currentEditor = dojo.widget.byId("w_editor");
        if (currentEditor) {
            currentEditor.destroy(true);           
        }

        // display parent div
        dojo.html.show("editorcontainer");
        
        // get the editor tag
        var editorNode = dojo.byId("cmseditor");        

        if (editorNode) {
            if (text) {
                editorNode.innerHTML = text;
            }

            // create the widget
            dojo.widget.createWidget("Editor2", { id: 'w_editor', minHeight: '300px',
                    htmlEditing: true }, editorNode);
        }
    }

    function callMetaInfo(contentId) {
        var ctx = new Array();
        ctx['contentId'] = contentId;
        ctx['webSiteId'] = webSiteId;

        // deselect the tree
        var tree = dojo.widget.byId("webCmsTreeSelector");
        if (tree && treeSelected) {
            tree.deselect();
            treeSelected = false;
        }

        // destroy the editor
        var editor = dojo.widget.byId("w_editor");
        if (editor) {
            editor.destroy(true);
        }
        //dojo.html.hide("editorcontainer");

        // get the meta-info screen
        var bindArgs = {
            url: metaUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: ctx,
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
            load: function(type, data, evt) {
                var innerPage = dojo.byId('cmscontent');
                innerPage.innerHTML = data;                
            }
        };
        dojo.io.bind(bindArgs);
    }

    function callPathAlias(contentId) {
        var ctx = new Array();
        ctx['contentId'] = contentId;
        ctx['webSiteId'] = webSiteId;

        // deselect the tree
        var tree = dojo.widget.byId("webCmsTreeSelector");
        if (tree && treeSelected) {
            tree.deselect();
            treeSelected = false;
        }

        // destroy the editor
        var editor = dojo.widget.byId("w_editor");
        if (editor) {
            editor.destroy(true);
        }
        //dojo.html.hide("editorcontainer");

        // get the alias screen
        var bindArgs = {
            url: aliasUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: ctx,
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
            load: function(type, data, evt) {
                var innerPage = dojo.byId('cmscontent');
                innerPage.innerHTML = data;                
            }
        };
        dojo.io.bind(bindArgs);
    }

	function callEditor(sub, contentId, objstr, dataResourceTypeId) {
	    var ctx = new Array();
	    if (objstr != null && objstr.length > 0) {
	        var obj = objstr.split("|");
            ctx['contentIdFrom'] = obj[0];
            ctx['contentAssocTypeId'] = obj[1];
            ctx['fromDate'] = obj[2];
        }
        
        ctx['contentRoot'] = contentRoot;
        ctx['webSiteId'] = webSiteId;

        if (sub && contentId) {
            if (dataResourceTypeId) {
                ctx['dataResourceTypeId'] = dataResourceTypeId;
            }

            ctx['contentIdFrom'] = contentId;
            ctx['contentAssocTypeId'] = 'SUB_CONTENT';

            // deselect the tree
            var tree = dojo.widget.byId("webCmsTreeSelector");
            if (tree && treeSelected) {
                tree.deselect();
                treeSelected = false;
            }
        } else {
            if (contentId != null && contentId.length > 0) {
                ctx['contentId'] = contentId;
            } else {
                // deselect the tree
                var tree = dojo.widget.byId("webCmsTreeSelector");
                if (tree && treeSelected) {
                    tree.deselect();
                    treeSelected = false;
                }
            }
        }
                
        var bindArgs = {
            url: editorUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: ctx,
            error: function(type, data, evt) {
                alert("An error occured loading editor! : " + data);
            },
            load: function(type, data, evt) {
                var editPage = dojo.byId('cmscontent');
                editPage.innerHTML = data;

                // load the data
                var cmsdata = dojo.byId("cmsdata");

                // create the editor
                if (cmsdata) {
                    createEditor(cmsdata.value);
                } else {
                    createEditor();
                }
            }
        };
        dojo.io.bind(bindArgs);        
    }

    function saveMetaInfo(form) {
        // save title
        document.cmsmeta_title.objectInfo.value = form.title.value;
        ajaxSubmitForm(document.cmsmeta_title);

        // save title property
        document.cmsmeta_titleProperty.objectInfo.value = form.titleProperty.value;
        ajaxSubmitForm(document.cmsmeta_titleProperty);

        // save meta-description
        document.cmsmeta_metaDescription.objectInfo.value = form.metaDescription.value;
        ajaxSubmitForm(document.cmsmeta_metaDescription);

        // save meta-keywords
        document.cmsmeta_metaKeywords.objectInfo.value = form.metaKeywords.value;
        ajaxSubmitForm(document.cmsmeta_metaKeywords);
    }

    function pathSave(contentId) {
        //dojo.html.hide("submit");
        
        var form = document.cmspathform;
        if (form != null) {
            var url = form.action;
            var bindArgs = {
                url: url,
                method: "POST",
                mimetype: "text/json",
                formNode: form,
                error: function(type, data, evt) {
                    alert("An error occurred.");
                },
                load: function(type, data, evt) {
                    callPathAlias(contentId);
                }
            };
            dojo.io.bind(bindArgs);
        }
    }

    function pathRemove(websiteId, pathAlias, contentId) {
        var remAliasUrl = '<@ofbizUrl>/removeWebSitePathAliasJson</@ofbizUrl>';
        var ctx = new Array();

        ctx['pathAlias'] = pathAlias;
        ctx['webSiteId'] = webSiteId;

        // get the alias screen
        var bindArgs = {
            url: remAliasUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: ctx,
            error: function(type, data, evt) {
                alert("An error occured! : " + data);
            },
            load: function(type, data, evt) {
                callPathAlias(contentId);
            }
        };
        dojo.io.bind(bindArgs);
    }

    function ajaxSubmitForm(form) {
        if (form != null) {
            var url = form.action;
            var bindArgs = {
                url: url,
                method: "POST",
                mimetype: "text/json",
                formNode: form,
                error: function(type, data, evt) {
                    alert("An error occurred submitting form.");
                },
                load: function(type, data, evt) {                    
                }
            };
            dojo.io.bind(bindArgs);
        }
    }
</script>

<style>
.dojoContextMenu {
	background-color: #ccc;
	font-size: 10px;
}
</style>

<#-- looping macro -->
<#macro fillTree assocList>
  <#if (assocList?has_content)>
    <#list assocList as assoc>
        <#assign thisContent = assoc.getRelatedOne("ToContent")/>
        <div dojoType="TreeNode" title="${thisContent.contentName?default(assoc.contentIdTo)}" widgetId="${assoc.contentIdTo}"
                object="${assoc.contentId}|${assoc.contentAssocTypeId}|${assoc.fromDate}">
            <#assign assocs = thisContent.getRelated("FromContentAssoc")?if_exists/>
            <#if (assocs?has_content)>
                <@fillTree assocList = assocs/>
            </#if>
        </div>
    </#list>
  </#if>
</#macro>

<dl dojoType="TreeContextMenu" id="webCmsContextMenu" style="font-size: 1em; color: #ccc;">
    <dt dojoType="TreeMenuItem" id="newLong" caption="New Long Text"/>
    <dt dojoType="TreeMenuItem" id="newShort" caption="New Short Text"/>
    <dt dojoType="TreeMenuItem" id="newUrl" caption="New URL"/>
    <dt dojoType="TreeMenuItem" id="newImage" caption="New Image"/>
    <dt dojoType="TreeMenuItem" id="newVideo" caption="New Video"/>
    <dt dojoType="TreeMenuItem" id="newAudio" caption="New Audio"/>
    <dt dojoType="TreeMenuItem" id="newObject" caption="New Object"/>
</dl>

<div class="tableheadtext">
    Website Content
</div>
<div class="tabletext">
  *Right click to add sub-content
</div>
<div>&nbsp;</div>

<dojo:TreeSelector widgetId="webCmsTreeSelector" eventNames="select:webCmsNodeSelected"></dojo:TreeSelector>
<div dojoType="Tree" menu="webCmsContextMenu" widgetId="webCmsTree" selector="webCmsTreeSelector" toggler="fade" toggleDuration="500">
    <#if (subsites?has_content)>
        <@fillTree assocList = subsites/>    
    </#if>
</div>
<#if (!subsites?has_content)>
    <a href="javascript:void(0);" class="linktext">Add Tree</a>
</#if>

<div>&nbsp;</div>
<div>&nbsp;</div>

<dl dojoType="TreeContextMenu" id="webMenuContextMenu" style="font-size: 1em; color: #ccc;">
    <dt dojoType="TreeMenuItem" id="newItem" caption="New Menu Item"/>
    <dt dojoType="TreeMenuItem" id="newMenu" caption="New Menu"/>    
</dl>

<div class="tableheadtext">
    Website Menus
</div>
<div class="tabletext">
  *Right click to add new menus
</div>
<div>&nbsp;</div>

<dojo:TreeSelector widgetId="webMenuTreeSelector" eventNames="select:webMenuNodeSelected"></dojo:TreeSelector>
<div dojoType="Tree" menu="webMenuContextMenu" widgetId="webMenuTree" selector="webMenuTreeSelector" toggler="fade" toggleDuration="500">
    <#if (menus?has_content)>
        ${menus}
        <@fillTree assocList = menus/>
    </#if>
</div>
<#if (!menus?has_content)>
    <a href="javascript:void(0);" class="linktext">Add Menu</a>
</#if>

