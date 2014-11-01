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
<#-- some labels are not unescaped in the JSON object so we have to do this manualy -->
function unescapeHtmlText(text) {
    return jQuery('<div />').html(text).text()
}

jQuery(window).load(createTree());

<#-- creating the JSON Data -->
<#if parentProductStoreGroup?has_content>
    <#assign parentGroupList = [parentProductStoreGroup]>
<#else>
    <#assign parentGroupList = parentProductStoreGroups>
</#if>
var rawdata = [
    <#list parentGroupList as parentGroup>
                   {
                    "data": {"title" : unescapeHtmlText("<#if parentGroup.productStoreGroupName??>${parentGroup.productStoreGroupName?js_string} [${parentGroup.productStoreGroupId}]</#if>"),
                                  "attr": {"href" : "<@ofbizUrl>/EditProductStoreGroupAndAssoc</@ofbizUrl>","onClick" : "callDocument('${parentGroup.productStoreGroupId}');"}},
                    "attr": {"parentGroupId" : "${parentGroup.productStoreGroupId}"}, 
                    "state" : "closed"
                    }<#if parentGroup_has_next>,</#if>
     </#list>
     ];

 <#-- create Tree-->
  function createTree() {
    jQuery(function () {
        jQuery("#tree").jstree({
        "plugins" : [ "themes", "json_data","ui" ,"cookies", "types"],
            "json_data" : {
                "data" : rawdata,
                "ajax" : { "url" : "<@ofbizUrl>getProductStoreGroupRollupHierarchy</@ofbizUrl>",
                           "type" : "POST",
                           "data" : function (n) {
                               return {
                                   "parentGroupId" :  n.attr ? n.attr("parentGroupId").replace("node_","") : 1,
                                   "onclickFunction" : "callDocument"
                               };
                           },
                           success : function (data) {
                               return data.storeGroupTree;
                           }
                }
            },
            "types" : {
                "valid_children" : [ "root" ]
            }
        });
    });
  }
  
  function callDocument(id) {
    //jQuerry Ajax Request
    var dataSet = {};
    dataSet = {"productStoreGroupId" : id, "ajaxUpdateEvent" : "Y"};
    jQuery.ajax({
        url: 'EditProductStoreGroupAndAssoc',
        type: 'POST',
        data: dataSet,
        error: function(msg) {
            alert("An error occurred loading content! : " + msg);
        },
        success: function(msg) {
            jQuery('#centerdiv').html(msg);
        }
    });
  }
</script>

<div id="tree"></div>
