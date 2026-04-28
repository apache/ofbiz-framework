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
var rawdata = [
        <#if (completedTree?has_content)>
            <@fillTree rootCat = completedTree/>
        </#if>

        <#macro fillTree rootCat>
            <#if (rootCat?has_content)>
                <#list rootCat as root>
                    {
                    "id": "${root.productCategoryId}",
                    "text": unescapeHtmlText("<#if root.categoryName??>${root.categoryName?js_string} [${root.productCategoryId}]<#else>${root.productCategoryId?js_string}</#if>"),
                    "a_attr": {"href": "<@ofbizUrl>/EditProdCatalog?prodCatalogId=${root.productCategoryId}</@ofbizUrl>", "onClick": "callDocument('${root.productCategoryId}', 'catalog');"},
                    "li_attr": {"rel": "root", "isCatalog": "${root.isCatalog?string}", "isCategoryType": "${root.isCategoryType?string}"}
                    <#if root.child??>
                    ,"children": true
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
        importLibrary(["/common/js/node_modules/jstree/dist/jstree.min.js"], function(){
            <#if stillInCatalogManager>
            localStorage.removeItem('product_category_tree');
            </#if>
            jQuery("#tree").jstree({
                "core": {
                    "themes": {
                        "url": "/common/js/node_modules/jstree/dist/themes/default/style.min.css"
                    },
                    "data": function(node, callback) {
                        var inst = this;
                        if (node.id === '#') {
                            callback.call(inst, rawdata);
                        } else {
                            jQuery.ajax({
                                url: "getChild",
                                type: "POST",
                                data: {
                                    "isCategoryType": node.li_attr ? node.li_attr.isCategoryType : "true",
                                    "isCatalog": node.li_attr ? node.li_attr.isCatalog : "true",
                                    "productCategoryId": node.id,
                                    "additionParam": "','category",
                                    "hrefString": "EditCategory?productCategoryId=",
                                    "onclickFunction": "callDocument"
                                },
                                success: function(data) {
                                    callback.call(inst, convertNodes(data.treeData));
                                }
                            });
                        }
                    },
                    "check_callback": true
                },
                "state": {"key": "product_category_tree"},
                "plugins": ["themes", "state"]
            });
            <#if !stillInCatalogManager>
            jQuery("#tree").on('ready.jstree', function() {
                <#if productCategoryId??>
                jQuery("#tree").jstree(true).select_node('${productCategoryId}');
                <#elseif prodCatalogId??>
                jQuery("#tree").jstree(true).select_node('${prodCatalogId}');
                <#elseif showProductCategoryId??>
                jQuery("#tree").jstree(true).select_node('${showProductCategoryId}');
                </#if>
            });
            </#if>
        });
    });
  }

  function callDocument(id,type) {
    //jQuerry Ajax Request
    var dataSet = {};
    if(type == "catalog") {
        URL = 'EditProdCatalogAjax';
        dataSet = {"prodCatalogId" : id, "ajaxUpdateEvent" : "Y"};
    } else {
        URL = 'EditCategoryAjax';
        dataSet = {"productCategoryId" : id, "ajaxUpdateEvent" : "Y"};
    }
    jQuery.ajax({
        url: URL,
        type: 'POST',
        data: dataSet,
        error: function(msg) {
            alert("An error occurred loading content! : " + msg);
        },
        success: function(msg) {
            jQuery('#centerdiv').html(msg);
        }
    });
    jQuery.ajax({
        url: 'listMiniproduct',
        type: 'POST',
        data: {"productCategoryId" : id},
        error: function(msg) {
            alert("An error occurred loading content! : " + msg);
        },
        success: function(msg) {
            jQuery('#miniproductlist').html(msg);
        }
    });
  }
</script>

<div id="tree"></div>
