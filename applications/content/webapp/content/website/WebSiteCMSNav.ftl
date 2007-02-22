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
    var contentRoot = '${contentRoot}';
	var webSiteId = '${webSiteId}';        
    var editorUrl = '<@ofbizUrl>/views/WebSiteCMSContent</@ofbizUrl>';
    var aliasUrl = '<@ofbizUrl>/views/WebSiteCMSPathAlias</@ofbizUrl>';
        
    dojo.addOnLoad(function() {
		dojo.event.topic.subscribe("webCmsNodeSelected",
			 function(message) {
			    treeSelected = true;
                callEditor(false, message.node.widgetId, message.node.object);
			 }
		);

		var cmsdata = dojo.byId("cmsdata");
		createEditor(cmsdata.value);		
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

        if (text) {
            editorNode.innerHTML = text;
        }

        // create the widget
        dojo.widget.createWidget("Editor2", { id: 'w_editor', minHeight: '300px',
                htmlEditing: true }, editorNode);
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
        dojo.html.hide("editorcontainer");

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

	function callEditor(sub, contentId, objstr) {
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
            ctx['contentIdFrom'] = contentId;
            ctx['contentAssocTypeId'] = 'SUB_CONTENT';

            // deselect the tree
            var tree = dojo.widget.byId("webCmsTreeSelector");
            tree.deselect();
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

                //dojo.byId("raw").value = data;

                // make sure the editor is displayed
                //dojo.html.show("editorcontainer");

                // load the data
                var cmsdata = dojo.byId("cmsdata");

                // create the editor
                createEditor(cmsdata.value);                                
            }
        };
        dojo.io.bind(bindArgs);        
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
</script>

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

<div class="tableheadtext">
    Website Content
    <#-- &nbsp;<a href="javascript:void(0);" onclick="javascript:callEditor();" class="buttontext">New Content</a>  -->
</div>
<dojo:TreeSelector widgetId="webCmsTreeSelector" eventNames="select:webCmsNodeSelected"></dojo:TreeSelector>
<div dojoType="Tree" widgetId="webCmsTree" selector="webCmsTreeSelector" toggler="fade" toggleDuration="500">
    <#if (subsites?has_content)>
        <@fillTree assocList = subsites/>
    </#if>
    
    <#--
    <#list subsites as subsite>
        <#assign thisContent = subsite.getRelatedOne("ToContent")/>
        <div dojoType="TreeNode" title="${thisContent.contentName?default(subsite.contentIdTo)}" widgetId="${subsite.contentIdTo}"
                object="${subsite.contentId}|${subsite.contentAssocTypeId}|${subsite.fromDate}">
            <#assign assocs = thisContent.getRelated("ContentAssoc")?if_exists/>
            <#if (assocs?has_content)>

            </#if>
        </div>
    </#list>
    -->
</div>