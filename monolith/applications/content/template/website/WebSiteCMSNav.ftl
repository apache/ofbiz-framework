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
    jQuery(document).ready(loadTrees);

    var contentRoot = '${contentRoot!}';
    var menuRoot = '${menuRoot!}';
    var errorRoot = '${errorRoot!}';
    var webSiteId = '${webSiteId!}';
    var editorUrl = '<@ofbizUrl>/views/WebSiteCMSContent</@ofbizUrl>';
    var aliasUrl = '<@ofbizUrl>/views/WebSiteCMSPathAlias</@ofbizUrl>';
    var metaUrl = '<@ofbizUrl>/views/WebSiteCMSMetaInfo</@ofbizUrl>';

    // No drag'n'drop for nodes in subtrees with these contentIds.
    // Adding "false" prevents creating new "root" nodes.
    // we have no such subtrees OOTB so those are only as examples
    var unmovableSubtrees = [false, "homeContentsRoot", "merchantContentsRoot", "categoryChildren"];

    function cutNameLength(name) {
        var leng = 27;
        return name.substring(0, leng);
    }

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

    var contextmenu = { 'items': function(node) {
                    return {
                    'create1' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceLongText}",
                        'action' : function(data) {
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_node(data.reference);
                            callDocument(true, sel.id, null, 'ELECTRONIC_TEXT');
                        }
                    },
                    'create2' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceShortText}",
                        'action' : function(data) {
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_node(data.reference);
                            callDocument(true, sel.id, null, 'SHORT_TEXT');
                        }
                    },
                    'create3' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceUrlResource}",
                        'action' : function(data) {
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_node(data.reference);
                            callDocument(true, sel.id, null, 'URL_RESOURCE');
                        }
                    },
                    'create4' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentDataResourceImage}",
                        'action' : function(data) {
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_node(data.reference);
                            callDocument(true, sel.id, null, 'IMAGE_OBJECT');
                        }
                    },
                    'create5' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceVideo}",
                        'action' : function(data) {
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_node(data.reference);
                            callDocument(true, sel.id, null, 'VIDEO_OBJECT');
                        }
                    },
                    'create6' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceAudio}",
                        'action' : function(data) {
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_node(data.reference);
                            callDocument(true, sel.id, null, 'AUDIO_OBJECT');
                        }
                    },
                    'create7' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceOther}",
                        'action' : function(data) {
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_node(data.reference);
                            callDocument(true, sel.id, null, 'OTHER_OBJECT');
                        }
                    },
                    'delete' : {
                        'label'  : "${uiLabelMap.CommonDelete}",
                        'action' : function(data) {
                            <#assign message=StringUtil.wrapString(uiLabelMap.ConfirmDeleteContent) />
                            if (!window.confirm('${message}')) { return false; }
                            var ref = jQuery.jstree.reference(data.reference);
                            var sel = ref.get_selected();
                            if (!sel.length) { sel = [ref.get_node(data.reference).id]; }
                            ref.delete_node(sel);
                        }
                    }
                };
            }
        }

<#-------------------------------------------------------------------------------------create Tree-->
    function createSubsitesTree() {
        <#if contentRoot?has_content>
        jQuery("#${contentRoot}").jstree({
            "core": {
                "data": function(node, callback) {
                    var inst = this;
                    var contentId = node.id === '#' ? contentRoot : node.id;
                    jQuery.ajax({
                        url: "<@ofbizUrl>/getContentAssocsJson</@ofbizUrl>",
                        type: "GET",
                        data: {"contentId": contentId},
                        success: function(data) {
                            callback.call(inst, convertNodes(Array.isArray(data) ? data : []));
                        }
                    });
                },
                "check_callback": function(operation, node, parent, position, more) {
                    if (operation === 'move_node') {
                        if (more && more.dnd && more.dnd.is_copy) return false;
                        var inst = jQuery.jstree.reference(node);
                        var nodesToRoot = [];
                        jQuery.merge(nodesToRoot, inst.get_path(node, false, true) || []);
                        jQuery.merge(nodesToRoot, inst.get_path(parent, false, true) || []);
                        if (position === -1 && jQuery.inArray(false, unmovableSubtrees) >= 0) return false;
                        for (var i = 0; i < unmovableSubtrees.length; i++) {
                            if (jQuery.inArray(unmovableSubtrees[i], nodesToRoot) >= 0) return false;
                        }
                    }
                    return true;
                }
            },
            "state": {"key": "cms_subsites_tree"},
            'contextmenu': contextmenu,
            "plugins": ["themes", "state", "contextmenu", "dnd"]
        }).on("move_node.jstree", moveContent)
          .on("delete_node.jstree", deleteContent)
          .on("select_node.jstree", selectNode);

        jQuery("#${contentRoot}").on('ready.jstree', function() {
            var contentIdFrom = '${requestParameters.contentIdFrom!}';
            if (typeof newContentId !== 'undefined' && contentIdFrom) {
                jQuery("#${contentRoot}").jstree(true).open_node(contentIdFrom, function() {
                    jQuery("#${contentRoot}").jstree(true).select_node(newContentId);
                });
            }
        });
        </#if>
    }

    function loadTrees() {
        importLibrary(["/common/js/node_modules/jstree/dist/jstree.min.js",
            "/common/js/node_modules/jstree/dist/themes/default/style.min.css"], function() {
            createSubsitesTree();
            createMenusTree();
            createErrorTree();
        });
    }

    function createMenusTree() {
        <#if menuRoot?has_content>
            jQuery(function () {
                jQuery("#${menuRoot}").jstree({
                    "core": {
                        "data": function(node, callback) {
                            var inst = this;
                            var contentId = node.id === '#' ? menuRoot : node.id;
                            jQuery.ajax({
                                url: "<@ofbizUrl>/getContentAssocsJson</@ofbizUrl>",
                                type: "GET",
                                data: {"contentId": contentId},
                                success: function(data) {
                                    callback.call(inst, convertNodes(Array.isArray(data) ? data : []));
                                }
                            });
                        },
                        "check_callback": true
                    },
                    "state": {"key": "cms_menus_tree"},
                    'contextmenu': contextmenu,
                    "plugins": ["themes", "state", "contextmenu", "dnd"]
                }).on("move_node.jstree", moveContent)
                  .on("delete_node.jstree", deleteContent)
                  .on("select_node.jstree", selectNode);
            });
        </#if>
  }

  function createErrorTree() {
        <#if errorRoot?has_content>
        jQuery(function () {
                jQuery("#${errorRoot}").jstree({
                    "core": {
                        "data": function(node, callback) {
                            var inst = this;
                            var contentId = node.id === '#' ? errorRoot : node.id;
                            jQuery.ajax({
                                url: "<@ofbizUrl>/getContentAssocsJson</@ofbizUrl>",
                                type: "GET",
                                data: {"contentId": contentId},
                                success: function(data) {
                                    callback.call(inst, convertNodes(Array.isArray(data) ? data : []));
                                }
                            });
                        },
                        "check_callback": true
                    },
                    "state": {"key": "cms_errors_tree"},
                    'contextmenu': contextmenu,
                    "plugins": ["themes", "state", "contextmenu", "dnd"]
                }).on("move_node.jstree", moveContent)
                  .on("delete_node.jstree", deleteContent)
                  .on("select_node.jstree", selectNode);
            });
        </#if>
    }

<#-------------------------------------------------------------------------------------selectNode function-->
    function selectNode(event, data) {
        var node = data.node;
        callDocument('', node.id, node.li_attr, '');
    }

<#-------------------------------------------------------------------------------------moveContent function-->
    function moveContent(event, data) {
        var node = data.node;
        var treeId = '#' + jQuery(event.target).attr('id');
        var inst = jQuery(treeId).jstree(true);
        var newParentNode = inst.get_node(data.parent);

        var ctx = {};
        ctx['contentIdTo'] = node.id;
        ctx['contentIdFrom'] = node.li_attr ? node.li_attr.contentid : '';
        ctx['contentIdFromNew'] = newParentNode ? newParentNode.id : '';
        ctx['fromDate'] = node.li_attr ? node.li_attr.fromdate : '';
        ctx['contentAssocTypeId'] = node.li_attr ? node.li_attr.contentassoctypeid : '';

        //jQuerry Ajax Request
        jQuery.ajax({
            url:  '<@ofbizUrl>/moveContentJson</@ofbizUrl>',
            type: 'POST',
            data: ctx,
            error: function(msg) {
                inst.refresh();
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}", "${uiLabelMap.ErrorMovingContent} : " + JSON.stringify(msg));
            },
            success: function(msg) {
                if (msg._ERROR_MESSAGE_) {
                    inst.refresh();
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}", "${uiLabelMap.ErrorMovingContent} : " + msg._ERROR_MESSAGE_);
                } else {
                    var result = JSON.parse(msg);
                    if (node.li_attr) {
                        node.li_attr.fromdate = result.attr.fromDate;
                        node.li_attr.contentid = result.attr.contentId;
                    }
                    inst.select_node(node.id);
                }
            }
        });
    }

<#-------------------------------------------------------------------------------------deleteDocument function-->
    function deleteContent(event, data) {
        var node = data.node;
        var treeId = '#' + jQuery(event.target).attr('id');

        var ctx = {};
        ctx['contentRoot'] = treeId.replace('#', '');
        ctx['webSiteId'] = webSiteId;
        ctx['contentId'] = node.id;

        //jQuerry Ajax Request
        jQuery.ajax({
            url: '<@ofbizUrl>/deleteContentJson</@ofbizUrl>',
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorDeletingContent} : " + JSON.stringify(msg));
                jQuery(treeId).jstree(true).refresh();
            },
            success: function(msg) {
                callDocument(false, node.li_attr ? node.li_attr.contentid : '', null, '');
            }
        });
  }

<#-------------------------------------------------------------------------------------callDocument function-->
    function callDocument(sub, contentId, nodeAttrs, dataResourceTypeId) {
        var ctx = {};
        ctx['contentRoot'] = contentRoot;
        ctx['webSiteId'] = webSiteId;

        if (sub && contentId) {
            if (dataResourceTypeId) {
                ctx['dataResourceTypeId'] = dataResourceTypeId;
            }

            ctx['contentIdFrom'] = contentId;
            ctx['contentAssocTypeId'] = 'SUB_CONTENT';

        } else {
            if (contentId != null && contentId.length) {
                ctx['contentId'] = contentId;
            }
            if (nodeAttrs) {
                ctx['contentIdFrom'] = nodeAttrs.contentid;
                ctx['fromDate'] = nodeAttrs.fromdate;
                ctx['contentAssocTypeId'] = nodeAttrs.contentassoctypeid;
            }
        }

        //jQuerry Ajax Request
        jQuery.ajax({
            url: editorUrl,
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + JSON.stringify(msg));
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);
                createEditor();
            }
        });
     }

<#-------------------------------------------------------------------------------------createEditor function-->
function createEditor() {
    if($('#cmseditor').length) {
        var libraryFiles = ["/common/js/node_modules/trumbowyg/dist/trumbowyg.min.js",
            "/common/js/node_modules/trumbowyg/dist/plugins/indent/trumbowyg.indent.min.js"];
        importLibrary(libraryFiles, function() {
            var element = jQuery(self);
            var language = element.data('language');
            var buttons = [['viewHTML'],
                ['undo', 'redo'],
                ['formatting'],
                ['strong', 'em', 'del'],
                ['superscript', 'subscript'],
                ['link'],
                ['insertImage'],
                ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
                ['unorderedList', 'orderedList'],
                ['horizontalRule'],
                ['removeformat'],
                ['indent', 'outdent'],
                ['fullscreen']
            ]
            var opts = {
                lang     : language,
                btns    : buttons,
                semantic: false,
                tagsToRemove: ['script', 'link'],
                svgPath : '/common/js/node_modules/trumbowyg/dist/ui/icons.svg'
            }
            jQuery('#cmseditor').trumbowyg(opts);
        });
    }
}
<#-------------------------------------------------------------------------------------callMetaInfo function-->
function callMetaInfo(contentId) {
        var ctx = {"contentId" : contentId, "webSiteId" : webSiteId};

        jQuery.ajax({
            url: metaUrl,
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + JSON.stringify(msg));
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);
            }
        });
    }

<#-------------------------------------------------------------------------------------callPathAlias function-->
    function callPathAlias(contentId) {
        var ctx = {"contentId" : contentId, "webSiteId" : webSiteId};

        // get the alias screen
        jQuery.ajax({
            url: aliasUrl,
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + JSON.stringify(msg));
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);
            }
        });
    }

<#-------------------------------------------------------------------------------------saveMetaInfo function-->
    function saveMetaInfo(form) {
        // save title
        document.cmsmeta_title.objectInfo.value = form.title.value;
        ajaxSubmitForm(document.cmsmeta_title);

        // save title property
        document.cmsmeta_titleProperty.objectInfo.value = form.titleProperty.value;
        ajaxSubmitForm(document.cmsmeta_titleProperty);

        // save meta-description
        document.cmsmeta_metaDescription.objectInfo.value = form.metaDescription.value;
        ajaxSubmitForm(document.cmsmeta_metaDescription);

        // save meta-keywords
        document.cmsmeta_metaKeywords.objectInfo.value = form.metaKeywords.value;
        ajaxSubmitForm(document.cmsmeta_metaKeywords);
    }

<#-------------------------------------------------------------------------------------pathSave function-->
    function pathSave(contentId) {
        var form = document.cmspathform;
        if (form != null) {
            var url = form.action;
            jQuery.ajax({
                url: url,
                type: 'POST',
                data: jQuery(form).serialize(),
                error: function(msg) {
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonUnspecifiedErrorOccurred}");
                },
                success: function(msg) {
                    callPathAlias(contentId);
                }
            });
        }
    }

<#-------------------------------------------------------------------------------------pathRemove function-->
    function pathRemove(websiteId, pathAlias, fromDate, contentId) {
        var remAliasUrl = '<@ofbizUrl>/removeWebSitePathAliasJson</@ofbizUrl>';

        jQuery.ajax({
                url: remAliasUrl,
                type: 'POST',
            data: { "webSiteId" : webSiteId, "pathAlias" : pathAlias, "fromDate": fromDate},
                error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonUnspecifiedErrorOccurred} : " + JSON.stringify(msg));
                },
                success: function(msg) {
                    callPathAlias(contentId);
                }
        });
    }

<#-------------------------------------------------------------------------------------ajaxSubmitForm function-->
    function ajaxSubmitForm(form, contentId) {
        if (form != null) {
            var url = form.action;
            jQuery.ajax({
                url: url,
                type: 'POST',
                async: false,
                data: jQuery(form).serialize(),
                success: function(data) {
                    // if the content id is set reload the contentScreen and tree
                    if (contentId && contentId.length) {
                        callDocument('', contentId, null, '');
                        jQuery("#${contentRoot}").jstree(true).refresh_node(form.contentIdFrom.value);
                    }
                },
                error: function(msg) {
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonErrorSubmittingForm} : " + JSON.stringify(msg));
                }
            });
        }
    }

</script>


<div class="label">
    ${uiLabelMap.ContentWebSiteContent}
</div>
<div>
    ${uiLabelMap.ContentWebSiteAddSubdContent}
</div>
<div>&nbsp;</div>

<#if (content?has_content)>
    <div id="${contentRoot}"></div>
<#else>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddTree}</a>
</#if>
<div class="label">
    ${uiLabelMap.ContentWebSiteMenus}
</div>
<div>
    ${uiLabelMap.ContentWebSiteAddNewMenus}
</div>
<div>&nbsp;</div>

<#if (menus?has_content)>
    <div id="${menuRoot}"></div>
<#else>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddMenu}</a>
</#if>

<div>&nbsp;</div>
<div>&nbsp;</div>

<div class="label">
    ${uiLabelMap.ContentWebSiteErrors}
</div>
<div>
    ${uiLabelMap.ContentWebSiteAddNewErrors}
</div>
<div>&nbsp;</div>
<#if (errorRoot?has_content)>
    <div id="${errorRoot}"></div>
<#else>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddError}</a>
</#if>
