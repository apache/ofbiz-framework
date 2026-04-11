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
                    "id": "${parentGroup.productStoreGroupId}",
                    "text": unescapeHtmlText("<#if parentGroup.productStoreGroupName??>${parentGroup.productStoreGroupName?js_string} [${parentGroup.productStoreGroupId}]</#if>"),
                    "a_attr": {"href": "<@ofbizUrl>/EditProductStoreGroupAndAssoc</@ofbizUrl>", "onClick": "callDocument('${parentGroup.productStoreGroupId}');"},
                    "li_attr": {"parentGroupId": "${parentGroup.productStoreGroupId}"},
                    "children": true
                    }<#if parentGroup_has_next>,</#if>
     </#list>
     ];

 <#-- helper to transform jstree 1.x AJAX response nodes to 3.x format -->
  function convertNodes(nodes) {
      if (!nodes || !nodes.length) return [];
      return jQuery.map(nodes, function(n) {
          var result = {
              id: (n.attr && n.attr.id) || n.id || '',
              text: n.data ? (typeof n.data === 'string' ? n.data : n.data.title) : (n.text || ''),
              children: (n.state === 'closed' || n.state === 'open') ? true : (n.children || false)
          };
          if (n.data && n.data.attr) result.a_attr = n.data.attr;
          if (n.attr) {
              result.li_attr = {};
              for (var k in n.attr) { if (k !== 'id') result.li_attr[k] = n.attr[k]; }
          }
          return result;
      });
  }

 <#-- create Tree-->
  function createTree() {
    jQuery(function () {
        importLibrary(["/common/js/node_modules/jstree/dist/jstree.min.js",
            "/common/js/node_modules/jstree/dist/themes/default/style.min.css"], function(){
            jQuery("#tree").jstree({
                "core": {
                    "data": function(node, callback) {
                        var inst = this;
                        if (node.id === '#') {
                            callback.call(inst, rawdata);
                        } else {
                            jQuery.ajax({
                                url: "<@ofbizUrl>getProductStoreGroupRollupHierarchy</@ofbizUrl>",
                                type: "POST",
                                data: {
                                    "parentGroupId": node.li_attr ? node.li_attr.parentGroupId : node.id,
                                    "onclickFunction": "callDocument"
                                },
                                success: function(data) {
                                    callback.call(inst, convertNodes(data.storeGroupTree));
                                }
                            });
                        }
                    },
                    "check_callback": true
                },
                "state": {"key": "product_store_group_tree"},
                "plugins": ["themes", "state"]
            });
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
