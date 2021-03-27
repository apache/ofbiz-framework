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
                    "data": {"title" : unescapeHtmlText("<#if root.groupName??>${root.groupName?js_string} [${root.partyId}]<#else>${root.partyId?js_string}</#if>"), "attr": {"href" : "<@ofbizUrl>/viewprofile?partyId=${root.partyId}</@ofbizUrl>","onClick" : "callDocument('${root.partyId}');"}},
                    "attr": {"id" : "${root.partyId}", "rel" : "Y"}
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
        $.cookie('jstree_select', null);
        $.cookie('jstree_open', null);
        
        jQuery("#tree").jstree({
        "core" : { "initially_open" : [ "${partyId}" ] },
        "plugins" : [ "themes", "json_data","ui" ,"cookies", "types", "crrm", "contextmenu"],
            "json_data" : {
                "data" : rawdata,
                          "ajax" : { "url" : "<@ofbizUrl>getHRChild</@ofbizUrl>", "type" : "POST",
                          "data" : function (n) {
                            return { 
                                "partyId" : n.attr ? n.attr("id").replace("node_","") : 1 ,
                                "additionParam" : "','category" ,
                                "hrefString" : "viewprofile?partyId=" ,
                                "onclickFunction" : "callDocument"
                        }; 
                    },
                              success : function(data) {
                                  return data.hrTree;
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
            },
            "contextmenu": {items: customMenu}
        });
    });
  }
  
  function callDocument(id,type) {
    window.location = "viewprofile?partyId=" + id;
  }
  
  function callEmplDocument(id,type) {
    //jQuerry Ajax Request
    var dataSet = {};
        URL = 'emplPositionView';
        dataSet = {"emplPositionId" : id, "ajaxUpdateEvent" : "Y"};
        
    jQuery.ajax({
        url: URL,
        type: 'POST',
        data: dataSet,
        error: function(msg) {
            alert("An error occurred loading content! : " + msg);
        },
        success: function(msg) {
            jQuery('div.contentarea').html(msg);
        }
    });
  }
  
  function customMenu(node) {
    // The default set of all items
    if(node.attr('rel')=='Y'){ 
    var items = {
        EmpPosition: { 
            label: "Add Employee Position",
            action: function (NODE, TREE_OBJ) {
                window.location.href = "EditEmplPosition?partyId=" + NODE.attr("id");
            }
        },
        AddIntOrg: { 
            label: "Add Internal Organization",
            action: function (NODE, TREE_OBJ) {
                var dataSet = {};
                dataSet = {"headpartyId" : NODE.attr("id")};
                jQuery.ajax({
                    type: "GET",
                    url: "EditInternalOrgFtl",
                    data: dataSet,
                    error: function(msg) {
                        alert("An error occurred loading content! : " + msg);
                    },
                    success: function(msg) {
                        jQuery('#dialog').html(msg);
                    }
                });
            }
        },
        RemoveIntOrg: { 
            label: "Remove Internal Organization",
            action: function (NODE, TREE_OBJ) {
                var dataSet = {};
                dataSet = {"partyId" : NODE.attr("id"),"parentpartyId" : $.jstree._focused()._get_parent(node).attr("id")};
                jQuery.ajax({
                    type: "GET",
                    url: "RemoveInternalOrgFtl",
                    data: dataSet,
                    error: function(msg) {
                        alert("An error occurred loading content! : " + msg);
                    },
                    success: function(msg) {
                        jQuery('#dialog').html(msg);
                    }
                });
            }
        }
    };}
    if(node.attr('rel')=='N'){ 
        var items = {
            AddPerson: { 
                label: "Add Person",
                action: function (NODE, TREE_OBJ) {
                    var dataSet = {};
                    dataSet = {"emplPositionId" : NODE.attr("id")};
                    jQuery.ajax({
                        type: "GET",
                        url: "EditEmplPositionFulfillments",
                        data: dataSet,
                        error: function(msg) {
                            alert("An error occurred loading content! : " + msg);
                        },
                        success: function(msg) {
                            jQuery('div.page-container').html(msg);
                        }
                    });
                }
            }
        }
    }

    if ($(node).hasClass("folder")) {
        // Delete the "delete" menu item
        delete items.deleteItem;
    }

    return items;
}


</script>
<div id="dialog" title="Basic dialog">
</div>
<div id="tree"></div>
