/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

//Drag & Drop Functions for myPortal

//global Var for controlling hover Boxes
var hoverDivBefore = -1;
var hoverColumnBefore = -1;

//init KeyListener
window.onload=function(){
    //Observe
    Event.observe(document, "keypress", key_event);

    //set a column droppable when it's empty
    checkIfTabelsContainsDivs();
}

//if ESC is pressed, remove draged portlet + hoverDiv
function key_event(evt)
{
    if(evt.keyCode == 27)  {
        //removes the hover div after the portlet is moved to another position
        if((hd = document.getElementById('hoverDiv')) != null){
            Droppables.remove(hd);
            hd.parentNode.removeChild(hd);
            //the global var must disabled
            hoverColumnBefore = -1;
            hoverDivBefore = -1;
        }
    }
}

//makes portlets dragable
function makeDragable(portletId){
    drag = document.getElementById(portletId);
    new Draggable(drag,
            {revert: true, ghosting: false, reverteffect :
            function(drag, top_offset, left_offset){
                    new Effect.MoveBy(drag, -top_offset, -left_offset, {duration:0});
                    var hd = document.getElementById('hoverDiv');
                    if(hd != null){
                        Droppables.remove(hd);
                        hd.parentNode.removeChild(hd);
                    }
                }
            });
}

//makes columns and portlets droppable
function makeDroppable(id){
    //DragDrop mode
    var mode = null;
    var drop = document.getElementById(id);
    if(drop.nodeName == "DIV"){
        Droppables.add(drop, {
            accept: ['portlet-config','noClass'],
            //Hover effekt
            onHover: function(element){

                //vertical overlapping position
                var pos = Position.overlap('vertical', drop);
                //gets the position of the droppable element
                var hd = document.getElementById('hoverDiv');

                if(pos >= 0.5){
                    mode = "DRAGDROPBEFORE";
                    //if the previous DIV is the hoverDiv do nothing
                    try {
                        var previous = (drop.previous('DIV')).getAttribute('id');
                    } catch (ex) {
                        previous = 'undi';
                    }

                    if(previous != 'hoverDiv' && previous != element.id){
                        if(hd != null){
                            Droppables.remove(hd);
                            hd.parentNode.removeChild(hd);
                        }

                        //create previewDiv
                        var divTopOffset = document.getElementById(element.id).offsetHeight;
                        var hoverDiv = document.createElement('DIV');
                        hoverDiv.setAttribute('style', 'height: ' + divTopOffset + 'px; border: 1px dashed #aaa; background:#ffffff; margin-bottom: 5px;')
                        hoverDiv.setAttribute('id', 'hoverDiv');
                        var elem = document.getElementById(id);

                        //inseret Before
                        elem.parentNode.insertBefore(hoverDiv, elem);
                        hoverDivBefore = id;

                        //the new div have to be droppable
                        Droppables.add(hoverDiv, {
                            onDrop: function(element) {
                                getDestinationInformationPortlets(element.id, id, mode);
                            }
                        });
                    }
                }else if(pos < 0.5){
                    mode = "DRAGDROPAFTER";
                    //if the next DIV is the hoverDiv do nothing
                    try {
                        var next = (drop.next('DIV')).getAttribute('id');
                    }catch (ex){
                        next = 'undi';
                    }

                    if(next != 'hoverDiv' && next != element.id){
                        if(hd != null){
                            Droppables.remove(hd);
                            hd.parentNode.removeChild(hd);
                        }

                        //create previewDiv
                        var divTopOffset = document.getElementById(element.id).offsetHeight;
                        var hoverDiv = document.createElement('DIV');
                        hoverDiv.setAttribute('style', 'height: ' + divTopOffset + 'px; border: 1px dashed #aaa; background:#ffffff; margin-bottom: 5px;')
                        hoverDiv.setAttribute('id', 'hoverDiv');
                        var elem = document.getElementById(id);

                        //insert After
                        elem.parentNode.insertBefore(hoverDiv, elem.nextSibling);
                        hoverDivBefore = id;

                        //the new div have to be droppable
                        Droppables.add(hoverDiv, {
                            onDrop: function(element) {
                                getDestinationInformationPortlets(element.id, id, mode);
                            }
                        });
                    }
                }
            },
            onDrop: function(element) {
                getDestinationInformationPortlets(element.id, id, mode);
            }
        });
    }else{
        mode = "DRAGDROPBOTTOM";
        //Makes ColumnDroppable
        Droppables.add(drop, {
            accept: ['portlet-config','noClass'],
            //Hover effekt
            onHover: function(element){
                var hd = document.getElementById('hoverDiv');
                if(hd != null){
                    hd.parentNode.removeChild(hd);
                }

                //create previewDiv
                var divTopOffset = document.getElementById(element.id).offsetHeight;
                var hoverDiv = document.createElement('DIV');
                hoverDiv.setAttribute('style', 'height: ' + divTopOffset + 'px; border: 1px dashed #aaa; background:#ffffff; margin-bottom: 5px;')
                hoverDiv.setAttribute('id', 'hoverDiv');
                var elem = document.getElementById(id);

                //insert After
                elem.appendChild(hoverDiv);
                hoverDivBefore = id;

                //the new div have to be droppable
                Droppables.add(hoverDiv, {
                    onDrop: function(element) {
                        getDestinationInformationColumn(element.id, id);
                    }
                });
            },

            onDrop: function(element) {
                getDestinationInformationColumn(element.id, id);
            }
            });
    }

}

//calls ajax request for dropping container on a portlet
function getDestinationInformationPortlets(originId, destinationId, mode){
    loadingImage();
 
    // extract integer part of arguments for freeMove_<id>
    var destId = destinationId.replace(/.*_([0-9]+)/, "\$1");
    var origId = originId.replace(/.*_([0-9]+)/, "\$1");
    
    var move = document.forms['freeMove_' + destId];
    var d_portalPageId = move.elements['portalPageId'].value;
    var d_portalPortletId = move.elements['portalPortletId'].value;
    var d_portletSeqId =  move.elements['portletSeqId'].value;

    var move = document.forms['freeMove_' + origId];
    var o_portalPageId = move.elements['portalPageId'].value;
    var o_portalPortletId = move.elements['portalPortletId'].value;
    var o_portletSeqId =  move.elements['portletSeqId'].value;

    new Ajax.Request('/myportal/control/updatePortalPagePortletSeqAjax',{
        method: "post",
        parameters: {o_portalPageId: o_portalPageId, o_portalPortletId: o_portalPortletId, o_portletSeqId: o_portletSeqId,
        d_portalPageId: d_portalPageId, d_portalPortletId: d_portalPortletId, d_portletSeqId: d_portletSeqId, mode: mode },

        onLoading: function(transport){
        },

        onSuccess: function(transport){
            var destination = document.getElementById(destinationId);
            var origin = document.getElementById(originId);

            if(mode == 'DRAGDROPBEFORE'){
                destination.parentNode.insertBefore(origin, destination);
            }else if(mode == 'DRAGDROPAFTER'){
                destination.parentNode.insertBefore(origin, destination.nextSibling);
            }
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
            parameters: {destinationColumn: destination, o_portalPageId: portalPageId, o_portalPortletId: portalPortletId, o_portletSeqId: portletSeqId, mode: "DRAGDROPBOTTOM"},

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

    //removes the hover div after the portlet is moved to another position
    if((hd = document.getElementById('hoverDiv')) != null){
        hd.parentNode.removeChild(hd);
        //the global var must disabled
        hoverColumnBefore = -1;
        hoverDivBefore = -1;
    }

    //set a column droppable when it's empty
    checkIfTabelsContainsDivs()

    var loading = document.getElementById("loading");
    if(loading != null){
        //IE Fix (IE treats DOM objects and Javascript objects separately, and you can't extend the DOM objects using Object.prototype)
        loading.parentNode.removeChild(loading);
    }
}

//safely get height of whole document
function getDocHeight() {
    var D = document;
    return Math.max(
        Math.max(D.body.scrollHeight, D.documentElement.scrollHeight),
        Math.max(D.body.offsetHeight, D.documentElement.offsetHeight),
        Math.max(D.body.clientHeight, D.documentElement.clientHeight)
    );
}

//displays the loading image
function loadingImage() {	
    var p = document.createElement("div");
    p.setAttribute("id", "loading");    
    p.setAttribute("style", "height: " + getDocHeight() + "px;" )
    
    var img = document.createElement("img");
    img.setAttribute("src", "/images/loader.gif");
    img.setAttribute("id", "loaderImg");

    //place loader image somwhere in the middle of the viewport
    img.setAttribute("style", "top: " + (document.viewport.getHeight() / 2 + document.viewport.getScrollOffsets().top - 50) + "px;");    
    
    p.appendChild(img);
    
    var container = document.getElementById("portalContainerId");
    container.appendChild(p);
}

//set the MousePointer
function setMousePointer(elem){
    var elements = document.getElementById(elem).getElementsByTagName('div');
    for(i=0; i<elements.length; i++){
        att = elements[i].getAttribute("class");
        if(att == 'screenlet-title-bar'){
            elements[i].setAttribute('onMouseOver','javascript:this.style.cursor="move";');
            break;
        }
    }
}

//checked if a Tabel contains a portlet, if not make it Droppable
function checkIfTabelsContainsDivs(){
    var td = new Array();
    td = getElementsByName_iefix('TD', 'portalColumn');
    for(var i=0; i<td.length; i++){
        //if the next DIV is the hoverDiv do nothing

            var next = td[i].getElementsByTagName('DIV');
            if(next.length == 0){
                //make a column droppable when it's empty
                makeDroppable(td[i].getAttribute('id'));
            }else{
                //removes the column droppable attribute when it's not empty
                Droppables.remove(td[i]);
            }
    }
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
