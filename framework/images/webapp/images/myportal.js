//Drag & Drop Functions for myPortal

//makes portlets dragable
function makeDragable(portletId){
    drag = document.getElementById(portletId);
    new Draggable(drag,
            {revert: true, reverteffect :
            function(drag, top_offset, left_offset)
                { new Effect.MoveBy(drag, -top_offset, -left_offset, {duration:0}); }
            });
}

//makes columns and portlets droppable
function makeDroppable(id){
    var drop = document.getElementById(id);
    if(drop.nodeName == "DIV"){
        Droppables.add(drop, {
            accept: 'portlet-config',
            hoverclass: 'hover',
                onDrop: function(element) {
                getDestinationInformationPortlets(element.id, id);
            }
        });
    }else{
        Droppables.add(drop, {
        accept: 'portlet-config',
        hoverclass: 'hover',
            onDrop: function(element) {
            getDestinationInformationColumn(element.id, id);
        }
        });
    }
}

//calls ajax request for dropping container on a portlet
function getDestinationInformationPortlets(originId, destinationId){
	loadingImage();

    var move = document.forms['freeMove_' + destinationId];
    var d_portalPageId = move.elements['portalPageId'].value;
    var d_portalPortletId = move.elements['portalPortletId'].value;
    var d_portletSeqId =  move.elements['portletSeqId'].value;

    var move = document.forms['freeMove_' + originId];
    var o_portalPageId = move.elements['portalPageId'].value;
    var o_portalPortletId = move.elements['portalPortletId'].value;
    var o_portletSeqId =  move.elements['portletSeqId'].value;

    new Ajax.Request('/myportal/control/updatePortalPagePortletSeqAjax',{
        method: "post",
        parameters: {o_portalPageId: o_portalPageId, o_portalPortletId: o_portalPortletId, o_portletSeqId: o_portletSeqId,
        d_portalPageId: d_portalPageId, d_portalPortletId: d_portalPortletId, d_portletSeqId: d_portletSeqId, mode: "DRAGDROP" },

        onLoading: function(transport){
        },

        onSuccess: function(transport){
            var destination = document.getElementById(destinationId);
            var origin = document.getElementById(originId);

            destination.parentNode.insertBefore(origin, destination);
            //Fix for layout Bug
            origin.style.left = destination.style.left;
            origin.style.top = 0;
        },

        onComplete: function(transport){
            onCompleteRequest();
        }
    });
}

//calls ajax request for dropping container on a column
function getDestinationInformationColumn(id, destination){
    loadingImage();

    var destiCol = destination;
    var move = document.forms['freeMove_' + id];
    var portalPageId = move.elements['portalPageId'].value;
    var portalPortletId = move.elements['portalPortletId'].value;
    var portletSeqId =  move.elements['portletSeqId'].value;
    var mode = move.elements['mode'].value;

    new Ajax.Request('/myportal/control/updatePortalPagePortletSeqAjax',{
            method: "post",
            parameters: {destinationColumn: destination, o_portalPageId: portalPageId, o_portalPortletId: portalPortletId, o_portletSeqId: portletSeqId, mode: "ColBOTTOM"},

        onLoading: function(transport){
        },

        onSuccess: function(transport){
            //loadingImage();
            var destination = document.getElementById(destiCol);
            var origin = document.getElementById(id);
            destination.appendChild(origin);

            origin.style.left = destination.style.left;
            origin.style.top = 0;
        },

        onComplete: function(transport){
            onCompleteRequest();
        }
    });
}

//removes the loading image
function onCompleteRequest() {
    var loading = document.getElementById("loading");
    if(loading != null){
        //IE Fix (IE treats DOM objects and Javascript objects separately, and you can't extend the DOM objects using Object.prototype)
        loading.parentNode.removeChild(loading);
    }
}

//displays the loading image
function loadingImage() {
    var container = document.getElementById("portalContainerId");
    var p = document.createElement("p");
    p.setAttribute("id", "loading");
    var img = document.createElement("img");
    img.setAttribute("src", "/images/loader.gif");
    img.setAttribute("id", "loaderImg");
    p.appendChild(img);
    container.appendChild(p);
}

//Workaround for IE getElementsByName Bug
function getElementsByName_iefix(tag, name) {
    var elem = document.getElementsByTagName(tag);
    var arr = new Array();
    for(i = 0,iarr = 0; i < elem.length; i++) {
         att = elem[i].getAttribute("name");
         if(att == name) {
              arr[iarr] = elem[i];
              iarr++;
         }
    }
    return arr;
}

