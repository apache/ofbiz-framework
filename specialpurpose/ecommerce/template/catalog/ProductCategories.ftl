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
<script type="text/javascript" src="<@ofbizContentUrl>/images/jquery/ui/js/jquery.cookie-1.4.0.js</@ofbizContentUrl>"></script>
  
<script type="text/javascript">
<#-- some labels are not unescaped in the JSON object so we have to do this manuely -->
function unescapeHtmlText(text) {
    return jQuery('<div />').html(text).text()
}

jQuery(window).load(createTree());

<#-- creating the JSON Data -->
var rawdata = [
  <#if (requestAttributes.topLevelList)??>
    <#assign topLevelList = requestAttributes.topLevelList>
  </#if>
  <#if (topLevelList?has_content)>
    <@fillTree rootCat=completedTree/>
  </#if>
  
  <#macro fillTree rootCat>
  <#if (rootCat?has_content)>
    <#list rootCat?sort_by("productCategoryId") as root>
            {
            "data": {"title" : unescapeHtmlText("<#if root.categoryName??>${root.categoryName?js_string}<#elseif root.categoryDescription??>${root.categoryDescription?js_string}<#else>${root.productCategoryId?js_string}</#if>"), "attr": { "href":"javascript: void(0);", "onClick":"callDocument('${root.productCategoryId}', '${root.parentCategoryId}')" , "class" : "${root.cssClass!}"}},
            "attr": {"id" : "${root.productCategoryId}"}
            <#if root.child?has_content>
                ,"children": [
                    <@fillTree rootCat=root.child/>
                    ]
            </#if>
            <#if root_has_next>
                },
            <#else>
                }
            </#if>
    </#list>
  </#if>
</#macro>
     ];

 <#-------------------------------------------------------------------------------------define Requests-->
  var editDocumentTreeUrl = '<@ofbizUrl>/views/EditDocumentTree</@ofbizUrl>';
  var listDocument =  '<@ofbizUrl>/views/ListDocument</@ofbizUrl>';
  var editDocumentUrl = '<@ofbizUrl>/views/EditDocument</@ofbizUrl>';
  var deleteDocumentUrl = '<@ofbizUrl>removeDocumentFromTree</@ofbizUrl>';

 <#-------------------------------------------------------------------------------------create Tree-->
  function createTree() {
    jQuery(function () {
        jQuery("#tree").jstree({
        "themes" : {
            "theme" : "classic",
            "icons" : false
        },
        "cookies" : {
            "cookie_options" : {path: '/'} 
        },
       "plugins" : [ "themes", "json_data", "cookies"],
            "json_data" : {
                "data" : rawdata
            }
        });
    });
  }

<#-------------------------------------------------------------------------------------callDocument function-->
    function callDocument(id, parentCategoryStr) {
        var checkUrl = '<@ofbizUrl>productCategoryList</@ofbizUrl>';
        if(checkUrl.search("http"))
            var ajaxUrl = '<@ofbizUrl>productCategoryList</@ofbizUrl>';
        else
            var ajaxUrl = '<@ofbizUrl>productCategoryListSecure</@ofbizUrl>';

        //jQuerry Ajax Request
        jQuery.ajax({
            url: ajaxUrl,
            type: 'POST',
            data: {"category_id" : id, "parentCategoryStr" : parentCategoryStr},
            error: function(msg) {
                alert("An error occurred loading content! : " + msg);
            },
            success: function(msg) {
                jQuery('#div3').html(msg);
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
                alert("An error occurred loading content! : " + msg);
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
                alert("An error occurred loading content! : " + msg);
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
                alert("An error occurred loading content! : " + msg);
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
                alert("An error occurred loading content! : " + msg);
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
                alert("An error occurred loading content! : " + msg);
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
                alert("An error occurred loading content! : " + msg);
            },
            success: function(msg) {
                jQuery('#Document').html(msg);
            }
        });
    }

</script>




<div id="quickadd" class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductCategories}</li>
        </ul>
    </div>
    <div class="screenlet-body" id="tree">
    </div>
</div>
