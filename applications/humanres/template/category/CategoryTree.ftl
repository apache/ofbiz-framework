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
                    "id": "${root.partyId}",
                    "text": unescapeHtmlText("<#if root.groupName??>${root.groupName?js_string} [${root.partyId}]<#else>${root.partyId?js_string}</#if>"),
                    "a_attr": {"href": "<@ofbizUrl>/viewprofile?partyId=${root.partyId}</@ofbizUrl>", "onClick": "callDocument('${root.partyId}');"},
                    "li_attr": {"rel": "Y"}
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
                                url: "<@ofbizUrl>getHRChild</@ofbizUrl>",
                                type: "POST",
                                data: {
                                    "partyId": node.id,
                                    "additionParam": "','category",
                                    "hrefString": "viewprofile?partyId=",
                                    "onclickFunction": "callDocument"
                                },
                                success: function(data) {
                                    callback.call(inst, convertNodes(data.hrTree));
                                }
                            });
                        }
                    },
                    "check_callback": true
                },
                "state": {"key": "humanres_party_tree"},
                "contextmenu": {"items": customMenu},
                "plugins": ["themes", "state", "contextmenu"]
            });

            jQuery("#tree").on('ready.jstree', function() {
                jQuery("#tree").jstree(true).open_node('${partyId}');
            });
        });
      });
  }
  
  function callDocument(id,type) {
    window.location = "viewprofile?partyId=" + id + "&<@csrfTokenPair>viewprofile</@csrfTokenPair>";
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
    var nodeRel = node.li_attr && node.li_attr.rel;
    var nodeId = node.id;
    var items = {};

    if (nodeRel === 'Y') {
        items = {
            EmpPosition: {
                label: "Add Employee Position",
                action: function() {
                    jQuery.ajax({
                        type: "GET",
                        url: "EditEmplPosition",
                        data: {"partyId": nodeId},
                        error: function(msg) { alert("An error occurred loading content! : " + msg); },
                        success: function(msg) { jQuery('div.page-container').html(msg); }
                    });
                }
            },
            AddIntOrg: {
                label: "Add Internal Organization",
                action: function() {
                    jQuery.ajax({
                        type: "GET",
                        url: "EditInternalOrgFtl",
                        data: {"headpartyId": nodeId},
                        error: function(msg) { alert("An error occurred loading content! : " + msg); },
                        success: function(msg) { jQuery('#dialog').html(msg); }
                    });
                }
            },
            RemoveIntOrg: {
                label: "Remove Internal Organization",
                action: function() {
                    var tree = jQuery("#tree").jstree(true);
                    var parentId = tree.get_parent(node);
                    jQuery.ajax({
                        type: "GET",
                        url: "RemoveInternalOrgFtl",
                        data: {"partyId": nodeId, "parentpartyId": parentId},
                        error: function(msg) { alert("An error occurred loading content! : " + msg); },
                        success: function(msg) { jQuery('#dialog').html(msg); }
                    });
                }
            }
        };
    }

    if (nodeRel === 'N') {
        items = {
            AddPerson: {
                label: "Add Person",
                action: function() {
                    jQuery.ajax({
                        type: "GET",
                        url: "EditEmplPositionFulfillments",
                        data: {"emplPositionId": nodeId},
                        error: function(msg) { alert("An error occurred loading content! : " + msg); },
                        success: function(msg) { jQuery('div.page-container').html(msg); }
                    });
                }
            }
        };
    }

    return items;
}


</script>
<div id="dialog" title="Basic dialog">
</div>
<div id="tree"></div>
