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
<script type="application/javascript" src="<@ofbizContentUrl>/common/js/jquery/plugins/jsTree/jquery.jstree.js</@ofbizContentUrl>"></script>

<script type="application/javascript">
<#-- some labels are not unescaped in the JSON object so we have to do this manuely -->
function unescapeHtmlText(text) {
    return jQuery('<div />').html(text).text()
}

jQuery(document).ready(createTree());

<#-- creating the JSON Data -->
var rawdata = [
    <#if (subCategories?has_content)>
        <@fillTree assocList = subCategories/>
    </#if>

      <#macro fillTree assocList>
          <#if (assocList?has_content)>
            <#list assocList as assoc>
                {
                "data": {"title" : unescapeHtmlText("${assoc.categoryName!assoc.dataCategoryId!}"), "attr": {"href": "javascript:void(0);", "onClick" : "callDocument('${assoc.dataCategoryId!}');"}}
                <#assign assocs = assoc.getRelated("ChildDataCategory", null, null, false)!/>
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
  var listDocument =  '<@ofbizUrl>listDataResources</@ofbizUrl>';

 <#-------------------------------------------------------------------------------------create Tree-->
  function createTree() {
    jQuery(function () {
        jQuery("#tree").jstree({
            "plugins" : [ "themes", "json_data", "ui", "crrm"],
            "json_data" : {
                "data" : rawdata,
                "progressive_render" : false
            }
        });
    });
  }

<#-------------------------------------------------------------------------------------callDocument function-->
    function callDocument(dataCategoryId) {
        //jQuerry Ajax Request
        jQuery.ajax({
            url: listDocument,
            type: 'POST',
            data: {"dataCategoryId" : dataCategoryId},
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);
            }
        });
     }

</script>

<div id="tree"></div>
