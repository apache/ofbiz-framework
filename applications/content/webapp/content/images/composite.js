
var parentContentId = "${parentContentId}";
</script>
<script language="Javascript">
var INDENT_PIXS = 10;
var imagePlus = new Image();
var imageMinus = new Image();
    imagePlus.src = "/images/imgPlus.gif";
    imageMinus.src = "/images/imgMinus.gif";
var NodeLookup = new Array();
var gRootNode = null;
//var gRootNode = new RootNode(parentContentId);
    //gRootNode.root.expanded=false;
var mainContentDiv = null;
var ContentList = new Array();

function RootNode(rootId) {
    this.expandCollapse = expandCollapse;
/*
    var oldDiv = document.getElementById("divChildren_0");
    var newDiv = document.createElement("div");
    newDiv.setAttribute("id","divChildren_" + rootId);
    var bodies = document.getElementsByTagName("body");
    var bod = bodies[0];
    var replaced = bod.replaceChild(newDiv, oldDiv);
    var replacedDiv = document.getElementById("divChildren_0");
*/

    this.root = new ContentNode(rootId, "ROOT", 0, 4, 10, null);
    NodeLookup[rootId] = this.root;
    return;
}

function expandCollapse(cNodeId) {
    var cNode = NodeLookup[cNodeId];
    if (cNode.expanded)
        cNode.collapse();
    else
        cNode.expand();
}

function ContentNode(strId, strText, indent , kidCount, descendantCount, dataResourceId) {
    this.id = strId;
    this.text = strText;
    this.kidCount = kidCount;
    this.descendantCount = descendantCount;
    this.dataResourceId = dataResourceId;
    if (this.kidCount == null) this.kidCount = 0;
    if (this.descendantCount == null) this.descendantCount = 0;
    this.expanded= (kidCount > 0) ? false : false;
    this.indent=indent;
    this.divObj=null;
    if (this.indent == null) this.indent = 0;
    this.kids = new Array();
    //this.build = build;
    //this.addChild = addChild;
    //this.collapse = collapse;
    //this.expand = expand;
}

ContentNode.prototype.collapse = function () {
 
    //check to see if the node is already collapsed 
    if (!this.expanded) {
    
        //throw an error 
        throw "Node is already collapsed"
 
    } else {
    
        //change the state of the node 
        this.expanded = false;
        
        //change the plus/minus image to be plus 
        document.images["img_" + this.id].src = imagePlus.src;
        
        //hide the child nodes 
        document.getElementById("divChildren_" + this.id).style.display = "none";
    }
}
 

function loadKids(evalStr) {
    var obj = new Object();
    eval("obj = " + evalStr);
    var kidArray = obj.kids;
    var id = obj.id;
    var contentNode = NodeLookup[id];
    var kidNode = null;
    var parentDivObj = document.getElementById("divChildren_" + id);
        parentDivObj.style.display = "block";
    for (var i=0; i < kidArray.length; i++) {
       kidNode = new ContentNode(kidArray[i][0], kidArray[i][1], contentNode.indent + 1, 
                                 kidArray[i][2], 0,  kidArray[i][3]);
       contentNode.kids[contentNode.kids.length] = kidNode;
       kidNode.build(parentDivObj, generateContent);
    }
   return; 
}

ContentNode.prototype.expand = function () {
 
    //check to see if the node is already expanded 
    if (this.expanded) {
    
        //throw an error 
        throw "Node is already expanded"
    
    } else {
    
        if (this.kidCount > this.kids.length) {
            loadRemote(this.id);
        } else {
            //change the state of the node 
            this.expanded = true;
        
            //change the plus/minus image to be minus 
            document.images["img_" + this.id].src = imageMinus.src;
        
            //show the child nodes 
            document.getElementById("divChildren_" + this.id).style.display = "block";
        }
    }
}

ContentNode.prototype.build = function (parentObj, genContent) {
        
    var kidDiv = document.createElement("div");
    kidDiv.style.position = "relative";
    //kidDiv.style.display = (this.expanded ? "block" : "none");
    kidDiv.style.display = "block";
    kidDiv.setAttribute("class", "oneline");
    kidDiv.setAttribute("id", this.id);
    //var tNode = document.createTextNode(this.text);
    var s = genContent(this);
    kidDiv.innerHTML = s;
    parentObj.appendChild(kidDiv);
    NodeLookup[this.id] = this;
//if (this.id == "100") alert("content:" + s);
    //create the layer for the children 
    var objChildNodesLayer = document.createElement("div");
    objChildNodesLayer.setAttribute("id", "divChildren_" + this.id);
    objChildNodesLayer.style.position = "relative";
    objChildNodesLayer.style.display = (this.expanded ? "block" : "none");
    kidDiv.appendChild(objChildNodesLayer);


    for(var i=0; i<this.kids.length; i++) {
        cNode = this.kids[i];
        cNode.build(objChildNodesLayer, genContent);
    }
    return; 
}

ContentNode.prototype.addChild = function (strId, strText, kidCount, descendantCount, dataResourceId ) {
    var kid = new ContentNode(strId, strText, this.indent + 1, kidCount, descendantCount, dataResourceId);
    this.kids[this.kids.length] = kid;
    return;       
}

function loadRemote(id) {
    jsrsExecute("getResponsesRemote", loadKids, "std", id, true);
}

function loadMainContent(evalStr) {
    //evalStr = evalStr.replace(/\n/g, "");
    var obj = new Object();
    eval("obj = " + evalStr);
    var mainContent = obj.content;
    var contentId = obj.id;
/*
    while (mainContentDiv.hasChildNodes()) {
        mainContentDiv.removeChild(mainContentDiv.childNodes[0]);
    }
    mainContentDiv.appendChild(document.createTextNode(mainContent));
*/
    mainContentDiv.innerHTML = mainContent;

    // Set response form to have necessary parent id, if they choose to attach to this content
    <#assign singleFormName = page.getProperty("singleFormName") />
    document.${singleFormName}.responseContentId.value = contentId;
    return; 
}


function remoteGet(id, dataResourceId) {
    //var evalStr = ContentList[id];
    //loadMainContent(evalStr);
    var a = new Array();
    a[0] = id;
    a[1] = dataResourceId;
    jsrsExecute("getMainContent", loadMainContent, "std", a, true);
    return; 
}

function generateContent(contentObj) {
    var s = "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr>";
    s += "<td width=\"" + contentObj.indent * INDENT_PIXS + "\" > </td>";
    s += "<td width=\"18\" align=\"center\" >";
if (contentObj.kidCount > 0) {
    s += "<a href=\"javascript:gRootNode.expandCollapse('" + contentObj.id + "')\">";
    s += "<img src=\"";
    s += contentObj.expanded ? imageMinus.src : imagePlus.src;
    s += "\" hspace\"1\" border=\"0\" id=\"";
    s += "img_" + contentObj.id;
    s += "\"\></a>";
}
    s += "</td>";
    s += "<th>";
    s += "<a href=\"javascript:remoteGet('" + contentObj.id + "', '"
                                           +  contentObj.dataResourceId + "')\">";
    s += contentObj.text;
    s += "</a>";
    s += "</th>";
    s += "</tr></table>"; 
    return s;
}
