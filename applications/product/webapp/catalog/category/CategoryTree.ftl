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
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/ui/development-bundle/external/jquery.cookie.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/jsTree/jquery.jstree.js</@ofbizContentUrl>"></script>

<script type="application/javascript">
<#-- some labels are not unescaped in the JSON object so we have to do this manuely -->
function unescapeHtmlText(text) {
    return jQuery('<div />').html(text).text()
}

createTree();

<#-- creating the JSON Data -->
var rawdata = [
      <#if (prodCatalogList?has_content)>
          <@fillCatalogTree prodCatalogs = prodCatalogList/>
      </#if>
      
      <#macro fillCatalogTree prodCatalogs>
          <#if (prodCatalogs?has_content)>
            <#list prodCatalogs as catalog>
                <#assign catalogId = catalog.prodCatalogId/>
                <#if !catalogName?has_content>
                    
                </#if>
                <#assign categoryList = catalog.rootCategoryList/>
                {
                <#if catalogId?has_content>
                    "data": {"title" : unescapeHtmlText("<#if catalog.catalogName?has_content>${catalog.catalogName}<#else>${catalogId}</#if> <#if catalog.catalogName?has_content>[${catalogId}]</#if>"), "attr": {"href": "<@ofbizUrl>/EditProdCatalog?prodCatalogId=${catalogId}</@ofbizUrl>", "onClick" : "callDocument('<@ofbizUrl>/EditProdCatalog?prodCatalogId=${catalogId}</@ofbizUrl>');"}},
                    "attr": {"id" : "${catalogId}", "rel" : "root"},
                    
                </#if>
                <#if categoryList?has_content>
                    "children": [
                        <@fillCategoryTree childCategoryList = categoryList/>
                    ]
                </#if>
                <#if catalog_has_next>
                },
                <#else>
                }
                </#if>
            </#list>
          </#if>
        </#macro>
        
        <#macro fillCategoryTree childCategoryList>
            <#if childCategoryList?has_content>
                <#list childCategoryList as childCategory>
                    {
                    <#local productCategoryId = childCategory.productCategoryId/>
                    <#local childCategorys = Static["org.ofbiz.product.category.CategoryWorker"].getRelatedCategoriesRet(request, "childCategoryList", productCategoryId, true)>
                    "data": {"title" : unescapeHtmlText("<#if childCategory.categoryName?has_content>${childCategory.categoryName}<#else>${productCategoryId}</#if> <#if childCategory.categoryName?has_content>[${productCategoryId}]</#if>"), "attr": {"href": "<@ofbizUrl>/EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>", "onClick" : "callDocument('<@ofbizUrl>/EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>');"}},
                    "attr": {"id" : "${productCategoryId}", "rel" : "CATEGORY"},
                    
                    <#if childCategoryList?has_content>
                        "children": [
                            <@fillCategoryTree childCategoryList = childCategorys/>
                        ]
                    </#if>
                    <#if childCategory_has_next>
                        },
                    <#else>
                        }
                    </#if>
                </#list>
            </#if>
        </#macro>
     ];
     

 <#-------------------------------------------------------------------------------------create Tree-->
  function createTree() {
    jQuery(function () {
        <#if !openTree>
            $.cookie('jstree_select', null);
            $.cookie('jstree_open', null);
        <#else>
            $.cookie("jstree_select", "${productCategoryId}");
        </#if>
        jQuery("#tree").jstree({
        "plugins" : [ "themes", "json_data", "cookies", "ui", "types"],
            "json_data" : {
                "data" : rawdata
            },
            "themes" : {
                "icons" : true
            },
            "cookies" : {
                "save_opened" : false
            },
        "types" : {
            "valid_children" : [ "root" ],
            "types" : {
                "CATEGORY" : {
                    "icon" : { 
                        "image" : "/images/jquery/plugins/jsTree/themes/apple/d.png",
                        "position" : "10px40px"
                    }
                }
            }
        }
        });
        
    });
    
  }
  
  function callDocument(url) {
    $(location).attr('href', url);
  }

</script>

<div id="tree"></div>
<style type="text/css">
    .jstree-default a 
        {
            white-space:normal !important;
            height: auto;
        }
    .jstree-default .jstree-leaf > ins
        {
            background-position:-36px 0;
            vertical-align: top;
        }
</style> 
