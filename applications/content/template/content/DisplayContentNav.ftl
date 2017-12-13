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

<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/common/js/jquery/plugins/jsTree/jquery.jstree.js</@ofbizContentUrl>"></script>

<script type="application/javascript">
<#-- some labels are not unescaped in the JSON object so we have to do this manuely -->
function unescapeHtmlText(text) {
    return jQuery('<div />').html(text).text()
}

jQuery(document).ready(createTree());

/*creating the JSON Data*/
var rawdata = [
      <#if (contentAssoc?has_content)>
          <@fillTree assocList = contentAssoc/>
      </#if>

      <#macro fillTree assocList>
          <#if (assocList?has_content)>
            <#list assocList as assoc>
                <#assign content  = delegator.findOne("Content",{"contentId":assoc.contentIdTo},true)/>
                <#if locale != "en">
                  <#assign content = Static["org.apache.ofbiz.content.content.ContentWorker"].findAlternateLocaleContent(delegator, content, locale)/>
                </#if>
                {
                "data": {"title" : unescapeHtmlText("${content.contentName!assoc.contentIdTo}"), "attr": {"href": "javascript:void(0);", "onClick" : "callDocument('${assoc.contentIdTo}');"}},
                <#assign assocChilds  = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", assoc.contentIdTo!, "contentAssocTypeId", "TREE_CHILD").orderBy("sequenceNum").queryList()!/>
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

 <#-------------------------------------------------------------------------------------define Requests-->
    var treeSelected = false;
    var listDocument =  '<@ofbizUrl>/views/ShowDocument</@ofbizUrl>';

 <#-------------------------------------------------------------------------------------create Tree-->
  function createTree() {
    jQuery(function () {
        jQuery("#tree").jstree({
            "plugins" : [ "themes", "json_data", "ui", "crrm"],
            "json_data" : {
                "data" : rawdata,
                "progressive_render" : false
            },
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
