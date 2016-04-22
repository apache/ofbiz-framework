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
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/jsTree/jquery.jstree.js</@ofbizContentUrl>"></script>

<script type="application/javascript">
<#-- some labels are not unescaped in the JSON object so we have to do this manuely -->
function unescapeHtmlText(text) {
    return jQuery('<div />').html(text).text()
}


<#-- creating the JSON Data -->
var rawdata = [
      <#if (contentAssoc?has_content)>
          <@fillTree assocList = contentAssoc/>
      </#if>

      <#macro fillTree assocList>
          <#if (assocList?has_content)>
            <#list assocList as assoc>
                <#assign content  = delegator.findOne("Content",Static["org.ofbiz.base.util.UtilMisc"].toMap("contentId",assoc.contentIdTo), false)/>
                {
                "data": {"title" : unescapeHtmlText("${content.contentName!assoc.contentIdTo}"), "attr": {"href": "javascript:void(0);", "onClick" : "callDocument('${assoc.contentIdTo}');"}},
                <#assign assocChilds  = delegator.findByAnd("ContentAssoc",Static["org.ofbiz.base.util.UtilMisc"].toMap("contentId",assoc.contentIdTo,"contentAssocTypeId", "TREE_CHILD"), null, false)/>
                    "attr": {"id" : "${assoc.contentIdTo}", "contentId" : "${assoc.contentId}", "AssocType" : "${assoc.contentAssocTypeId}", "fromDate" : "${assoc.fromDate}"}
                <#if assocChilds?has_content>
                    ,"children": [
                        <@fillTree assocList = assocChilds/>
                    ]
                </#if>
                <#if assoc_has_next>
                },
                <#else>
                }
                </#if>
            </#list>
          </#if>
        </#macro>
     ];

jQuery(document).ready(createTree());

 <#-------------------------------------------------------------------------------------define Requests-->
  var editDocumentTreeUrl = '<@ofbizUrl>/views/EditDocumentTree</@ofbizUrl>';
  var listDocument =  '<@ofbizUrl>/views/ListDocument</@ofbizUrl>';
  var editDocumentUrl = '<@ofbizUrl>/views/EditDocument</@ofbizUrl>';
  var deleteDocumentUrl = '<@ofbizUrl>removeDocumentFromTree</@ofbizUrl>';

 <#-------------------------------------------------------------------------------------create Tree-->
  function createTree() {
    jQuery(function () {
        jQuery("#tree").jstree({
            "plugins" : [ "themes", "json_data", "ui", "contextmenu", "crrm"],
            "json_data" : {
                "data" : rawdata,
                "progressive_render" : false
            },
            'contextmenu': {
                'items': {
                    'ccp' : false,
                    'create' : false,
                    'rename' : false,
                    'remove' : false,
                    'create1' : {
                        'label' : "New Folder",
                        'action' : function(obj) {
                            callCreateDocumentTree(obj.attr('id'));
                        }
                    },
                    'create2' : {
                        'label' : "New Content in Folder",
                        'action' : function(obj) {
                            callCreateDocument(obj.attr('id'));
                        }
                    },
                    'rename1' : {
                        'label' : "Rename Folder",
                        'action' : function(obj) {
                            callRenameDocumentTree(obj.attr('id'));
                        }
                    },
                    'delete1' : {
                        'label' : "Delete Folder",
                        'action' : function(obj) {
                            callDeleteDocument(obj.attr('id'), obj.attr('contentId'), obj.attr('AssocType'), obj.attr('fromDate'));
                        }
                    },
                }
            }
        });
    });
  }

<#-------------------------------------------------------------------------------------callDocument function-->
    function callDocument(contentId) {
        var tabitem='${tabButtonItem!}';
        if (tabitem=="navigateContent")
            listDocument = '<@ofbizUrl>/views/ListDocument</@ofbizUrl>';
        if (tabitem=="LookupContentTree")
            listDocument = '<@ofbizUrl>/views/ListContentTree</@ofbizUrl>';
        if (tabitem=="LookupDetailContentTree")
            listDocument = '<@ofbizUrl>/views/ViewContentDetail</@ofbizUrl>';

        //jQuerry Ajax Request
        jQuery.ajax({
            url: listDocument,
            type: 'POST',
            data: {"contentId" : contentId},
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#Document').html(msg);
            }
        });
     }
<#-------------------------------------------------------------------------------------callCreateDocumentTree function-->
      function callCreateDocumentTree(contentId) {
        jQuery.ajax({
            url: editDocumentTreeUrl,
            type: 'POST',
            data: {contentId: contentId,
                        contentAssocTypeId: 'TREE_CHILD'},
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#Document').html(msg);
            }
        });
    }
<#-------------------------------------------------------------------------------------callCreateSection function-->
    function callCreateDocument(contentId) {
        jQuery.ajax({
            url: editDocumentUrl,
            type: 'POST',
            data: {contentId: contentId},
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#Document').html(msg);
            }
        });
    }
<#-------------------------------------------------------------------------------------callEditSection function-->
    function callEditDocument(contentIdTo) {
        jQuery.ajax({
            url: editDocumentUrl,
            type: 'POST',
            data: {contentIdTo: contentIdTo},
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#Document').html(msg);
            }
        });

    }
<#-------------------------------------------------------------------------------------callDeleteItem function-->
    function callDeleteDocument(contentId, contentIdTo, contentAssocTypeId, fromDate) {
        jQuery.ajax({
            url: deleteDocumentUrl,
            type: 'POST',
            data: {contentId : contentId, contentIdTo : contentIdTo, contentAssocTypeId : contentAssocTypeId, fromDate : fromDate},
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                location.reload();
            }
        });
    }
 <#-------------------------------------------------------------------------------------callRename function-->
    function callRenameDocumentTree(contentId) {
        jQuery.ajax({
            url: editDocumentTreeUrl,
            type: 'POST',
            data: {  contentId: contentId,
                     contentAssocTypeId:'TREE_CHILD',
                     rename: 'Y'
                     },
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#Document').html(msg);
            }
        });
    }
 <#------------------------------------------------------pagination function -->
    function nextPrevDocumentList(url){
        url= '<@ofbizUrl>'+url+'</@ofbizUrl>';
         jQuery.ajax({
            url: url,
            type: 'POST',
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#Document').html(msg);
            }
        });
    }

</script>

<style>
<#if tabButtonItem?has_content>
    <#if tabButtonItem=="LookupContentTree"||tabButtonItem=="LookupDetailContentTree">
        body{background:none;}
        .left-border{float:left;width:25%;}
        .contentarea{margin: 0 0 0 0.5em;padding:0 0 0 0.5em;}
        .leftonly{float:none;min-height:25em;}
    </#if>
</#if>
</style>

<div id="tree"></div>


