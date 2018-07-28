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

<script type="application/javascript">
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
                    "data": {"title" : unescapeHtmlText("<#if root.categoryName??>${root.categoryName?js_string} [${root.productCategoryId}]<#else>${root.productCategoryId?js_string}</#if>"), "attr": {"href" : "<@ofbizUrl>/EditProdCatalog?prodCatalogId=${root.productCategoryId}</@ofbizUrl>","onClick" : "callDocument('${root.productCategoryId}', 'catalog');"}},
                    "attr": {"id" : "${root.productCategoryId}", "rel" : "root", "isCatalog" : "${root.isCatalog?string}" ,"isCategoryType" : "${root.isCategoryType?string}"}
                    <#if root.child??>
                    ,"state" : "closed"
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

 <#-- create Tree-->
  function createTree() {
    jQuery(function () {
        <#-- reset the tree when user browsing out of scope of catalog manager -->
        <#if stillInCatalogManager>
            $.cookie('jstree_select', null);
            $.cookie('jstree_open', null);
        <#else>
        <#-- Coloring the category when type the product categoryId manualy at the url bar -->
            $.cookie('jstree_select', "<#if productCategoryId??>${productCategoryId}<#elseif prodCatalogId??>${prodCatalogId}<#elseif showProductCategoryId??>${showProductCategoryId}</#if>");
        </#if>
        jQuery("#tree").jstree({
            "plugins" : [ "themes", "json_data","ui" ,"cookies", "types"],
            "json_data" : {
                "data" : rawdata,
                "ajax" : { "url" : "<@ofbizUrl>getChild</@ofbizUrl>",
                           "type" : "POST",
                           "data" : function (n) {
                                        return {
                                            "isCategoryType" :  n.attr ? n.attr("isCatalog").replace("node_","") : 1 ,
                                            "isCatalog" :  n.attr ? n.attr("isCatalog").replace("node_","") : 1 ,
                                            "productCategoryId" : n.attr ? n.attr("id").replace("node_","") : 1 ,
                                            "additionParam" : "','category" ,
                                            "hrefString" : "EditCategory?productCategoryId=" ,
                                            "onclickFunction" : "callDocument"
                                        };
                                    },
                           success : function(data) {
                               return data.treeData;
                           }
                }
            },
            "types" : {
             "valid_children" : [ "root" ],
             "types" : {
                 "CATEGORY" : {
                     "icon" : { 
                         "image" : "/common/js/jquery/plugins/jsTree/themes/apple/d.png",
                         "position" : "10px40px"
                     }
                 }
             }
         }
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
