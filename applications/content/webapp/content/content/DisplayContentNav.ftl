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

    dojo.require("dojo.widget.*");
    dojo.require("dojo.event.*");
    dojo.require("dojo.io.*");

    var treeSelected = false;
    var editDocumentTreeUrl = '<@ofbizUrl>/views/EditDocumentTree</@ofbizUrl>';
    var listDocument =  '<@ofbizUrl>/views/ShowDocument</@ofbizUrl>';
    var editDocumentUrl = '<@ofbizUrl>/views/EditDocument</@ofbizUrl>';
    var deleteDocumentUrl = '<@ofbizUrl>removeDocumentFromTree</@ofbizUrl>';
<#-------------------------------------------------------------------------------------Load function-->
    dojo.addOnLoad(function() {
        dojo.event.topic.subscribe("showDocument",
            function(message) {
                   treeSelected = true;
                   var ctx = new Array();
                   ctx['contentId'] = message.node.widgetId;
                   callDocument(ctx);
            }

        );
        dojo.event.topic.subscribe("NewDocumentTree/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callCreateDocumentTree(node.widgetId);
            }

        );
        dojo.event.topic.subscribe("NewDocument/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callCreateDocument(node.widgetId);
            }

        );
        dojo.event.topic.subscribe("EditDocument/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callEditDocument(node.widgetId);
            }
        );
       dojo.event.topic.subscribe("RenameDocumentTree/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callRenameDocumentTree(node.widgetId);
            }

        );
        dojo.event.topic.subscribe("DeleteDocument/engage",
            function (menuItem) {
                var node = menuItem.getTreeNode();
                callDeleteDocument(node.widgetId, node.object);
            }
        );
        }
    );

<#-------------------------------------------------------------------------------------call ofbiz function-->
    function callDocument(ctx) {
        var bindArgs = {
            url: listDocument,
            method: 'POST',
            mimetype: 'text/html',
            content: ctx,
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
            load: function(type, data, evt) {
                var innerPage = dojo.byId('Document');
                innerPage.innerHTML = data;
            }
        };
        dojo.io.bind(bindArgs);
    }
<#-------------------------------------------------------------------------------------callCreateFolder function-->
    function callCreateDocumentTree(contentId) {
        var bindArgs = {
            url: editDocumentTreeUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: {  contentId: contentId,
                        contentAssocTypeId:'TREE_CHILD'},
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
             load: function(type, data, evt) {
                var innerPage = dojo.byId('EditDocumentTree');
                innerPage.innerHTML = data;
            }
        };
        dojo.io.bind(bindArgs);
    }
<#-------------------------------------------------------------------------------------callCreateSection function-->
    function callCreateDocument(ctx) {
        var bindArgs = {
            url: editDocumentUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: {contentId: ctx},
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
            load: function(type, data, evt) {
                var innerPage = dojo.byId('Document');
                innerPage.innerHTML = data;
            }
        };
        dojo.io.bind(bindArgs);
    }
<#-------------------------------------------------------------------------------------callEditSection function-->
    function callEditDocument(ctx) {

        var bindArgs = {
            url: editDocumentUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: {contentIdTo: ctx},
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
            load: function(type, data, evt) {
                var innerPage = dojo.byId('Document');
                innerPage.innerHTML = data;
            }
        };
        dojo.io.bind(bindArgs);

    }
<#-------------------------------------------------------------------------------------callDeleteItem function-->
    function callDeleteDocument(contentIdTo,objstr) {
        var ctx = new Array();
        if (objstr != null && objstr.length > 0) {
            var obj = objstr.split("|");
            ctx['contentId'] = obj[0];
            ctx['contentAssocTypeId'] = obj[1];
            ctx['fromDate'] = obj[2];
        }
        ctx['contentIdTo']=contentIdTo;
        var bindArgs = {
            url: deleteDocumentUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: ctx,
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
            load: function(type, data, evt) {
                location.reload();
            }
        };
        dojo.io.bind(bindArgs);
    }
 <#-------------------------------------------------------------------------------------callRename function-->
    function callRenameDocumentTree(contentId) {
        var bindArgs = {
            url: editDocumentTreeUrl,
            method: 'POST',
            mimetype: 'text/html',
            content: {  contentId: contentId,
                        contentAssocTypeId:'TREE_CHILD',
                        rename: 'Y'
                     },
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
             load: function(type, data, evt) {
                var innerPage = dojo.byId('EditDocumentTree');
                innerPage.innerHTML = data;
            }
        };
        dojo.io.bind(bindArgs);
    }
</script>

<style type="text/css">
.dojoContextMenu {
    background-color: #ccc;
    font-size: 10px;
}
</style>

<#-- looping macro -->
<#macro fillTree assocList>
  <#if (assocList?has_content)>
    <#list assocList as assoc>
        <#assign content  = delegator.findByPrimaryKey("Content",Static["org.ofbiz.base.util.UtilMisc"].toMap("contentId",assoc.contentIdTo))/>
        <div dojoType="TreeNode" title="${content.contentName?default(assoc.contentIdTo)}" widgetId="${assoc.contentIdTo}"
             object="${assoc.contentId}|${assoc.contentAssocTypeId}|${assoc.fromDate}">
            <#assign assocChilds  = delegator.findByAnd("ContentAssoc",Static["org.ofbiz.base.util.UtilMisc"].toMap("contentId",assoc.contentIdTo,"contentAssocTypeId", "TREE_CHILD"))/>
            <#if assocChilds?has_content>
                <@fillTree assocList = assocChilds/>
            </#if>
        </div>
    </#list>
  </#if>
</#macro>

<dl dojoType="TreeContextMenu" id="contentContextMenu" style="font-size: 1em; color: #ccc;">
    <dt dojoType="TreeMenuItem" id="NewDocumentTree" caption="${uiLabelMap.ContentNewFolder}"/>
    <dt dojoType="TreeMenuItem" id="NewDocument" caption="${uiLabelMap.ContentNewContentInFolder}"/>
    <#--<dt dojoType="TreeMenuItem" id="EditDocument" caption="Edit Document"/> -->
    <dt dojoType="TreeMenuItem" id="RenameDocumentTree" caption="${uiLabelMap.ContentRenameFolder}"/>
    <dt dojoType="TreeMenuItem" id="DeleteDocument" caption="${uiLabelMap.ContentDeleteFolder}"/>
</dl>


<dojo:TreeSelector widgetId="contentTreeSelector" eventNames="select:showDocument"></dojo:TreeSelector>
<div dojoType="Tree" menu="contentContextMenu" widgetId="contentTree" selector="contentTreeSelector" toggler="fade" toggleDuration="500">
    <#if (contentAssoc?has_content)>
        <@fillTree assocList = contentAssoc/>
    </#if>
</div>

