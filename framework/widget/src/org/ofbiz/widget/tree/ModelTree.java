/*******************************************************************************
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
 *******************************************************************************/
package org.ofbiz.widget.tree;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.widget.screen.ModelScreen;
import org.ofbiz.widget.screen.ScreenFactory;
import org.ofbiz.widget.screen.ScreenStringRenderer;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

//import com.clarkware.profiler.Profiler;

/**
 * Widget Library - Tree model class
 */
public class ModelTree {

    public static final String module = ModelTree.class.getName();

    protected String name;
    protected String rootNodeName;
    protected String defaultRenderStyle;
    protected FlexibleStringExpander defaultWrapStyleExdr;
    protected List nodeList = new ArrayList();
    protected Map nodeMap = new HashMap();
    protected GenericDelegator delegator;
    protected LocalDispatcher dispatcher;
    protected FlexibleStringExpander expandCollapseRequestExdr;
    protected FlexibleStringExpander trailNameExdr;
    protected List trail = new ArrayList();
    protected List currentNodeTrail;
    protected int openDepth;
    protected int postTrailOpenDepth;
    protected int [] nodeIndices = new int[20];
    protected String defaultEntityName;
    protected String defaultPkName;
    protected boolean forceChildCheck;
    
// ===== CONSTRUCTORS =====
    /** Default Constructor */

    /** XML Constructor */
    public ModelTree() {}
    
    public ModelTree(Element treeElement, GenericDelegator delegator, LocalDispatcher dispatcher) {

        this.name = treeElement.getAttribute("name");
        this.rootNodeName = treeElement.getAttribute("root-node-name");
        this.defaultRenderStyle = UtilFormatOut.checkEmpty(treeElement.getAttribute("default-render-style"), "simple");
        // A temporary hack to accommodate those who might still be using "render-style" instead of "default-render-style"
        if (UtilValidate.isEmpty(this.defaultRenderStyle) || this.defaultRenderStyle.equals("simple")) {
            String rStyle = treeElement.getAttribute("render-style");
            if (UtilValidate.isNotEmpty(rStyle))
                this.defaultRenderStyle = rStyle;
        }
        this.defaultWrapStyleExdr = new FlexibleStringExpander(treeElement.getAttribute("default-wrap-style"));
        this.expandCollapseRequestExdr = new FlexibleStringExpander(treeElement.getAttribute("expand-collapse-request"));
        this.trailNameExdr = new FlexibleStringExpander(UtilFormatOut.checkEmpty(treeElement.getAttribute("trail-name"), "trail"));
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.forceChildCheck = !"false".equals(treeElement.getAttribute("force-child-check"));
        setDefaultEntityName(treeElement.getAttribute("entity-name"));
        try {
            openDepth = Integer.parseInt(treeElement.getAttribute("open-depth"));
        } catch(NumberFormatException e) {
            openDepth = 0;
        }

        try {
            postTrailOpenDepth = Integer.parseInt(treeElement.getAttribute("post-trail-open-depth"));
        } catch(NumberFormatException e) {
            postTrailOpenDepth = 999;
        }

        List nodeElements = UtilXml.childElementList(treeElement, "node");
        Iterator nodeElementIter = nodeElements.iterator();
        while (nodeElementIter.hasNext()) {
            Element nodeElementEntry = (Element) nodeElementIter.next();
            ModelNode node = new ModelNode(nodeElementEntry, this);
            String nodeName = node.getName();
            nodeList.add(node);
            nodeMap.put(nodeName,node);
        }
    
        if (nodeList.size() == 0) {
            throw new IllegalArgumentException("No node elements found for the tree definition with name: " + this.name);
        }

    }
    
    public String getName() {
        return name;
    }

    public void setDefaultEntityName(String name) {
        String nm = name;
        if (UtilValidate.isEmpty(nm)) {
            nm = "Content";
        }
        this.defaultEntityName = nm;
        ModelEntity modelEntity = delegator.getModelEntity(this.defaultEntityName);
        if (modelEntity.getPksSize() == 1) {
            ModelField modelField = modelEntity.getOnlyPk();
            this.defaultPkName = modelField.getName();
        }
    }
    
    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }
    
    public String getDefaultPkName() {
        return this.defaultPkName;
    }
    
    public String getRootNodeName() {
        return rootNodeName;
    }

    public String getWrapStyle(Map context) {
        return this.defaultWrapStyleExdr.expandString(context);
    }
    
    public int getOpenDepth() {
        return openDepth;
    }
    
    public int getPostTrailOpenDepth() {
        return postTrailOpenDepth;
    }
    
    public int getNodeIndexAtDepth(int i) {
        return nodeIndices[i];
    }
    
    public void setNodeIndexAtDepth(int i, int val) {
        nodeIndices[i] = val;
    }
    
    public String getExpandCollapseRequest(Map context) {
        String expColReq = this.expandCollapseRequestExdr.expandString(context);
        if (UtilValidate.isEmpty(expColReq)) {
            HttpServletRequest request = (HttpServletRequest)context.get("request");
            String s1 = request.getRequestURI();
            int pos = s1.lastIndexOf("/");
            if (pos >= 0)
                expColReq = s1.substring(pos + 1);
            else 
                expColReq = s1;
        }
        return expColReq;
    }
    
    public String getTrailName(Map context) {
        return this.trailNameExdr.expandString(context);
    }
    
    public List getTrailList() {
        return trail;
    }
    
    public void setTrailList(List trailList) {
        this.trail = trailList;
    }
    
    public List getCurrentNodeTrail() {
        return currentNodeTrail;
    }
    

    /**
     * Renders this tree to a String, i.e. in a text format, as defined with the
     * TreeStringRenderer implementation.
     *
     * @param writer The Writer that the tree text will be written to
     * @param context Map containing the tree context; the following are
     *   reserved words in this context: parameters (Map), isError (Boolean),
     *   itemIndex (Integer, for lists only, otherwise null), bshInterpreter,
     *   treeName (String, optional alternate name for tree, defaults to the
     *   value of the name attribute)
     * @param treeStringRenderer An implementation of the TreeStringRenderer
     *   interface that is responsible for the actual text generation for
     *   different tree elements; implementing your own makes it possible to
     *   use the same tree definitions for many types of tree UIs
     */
    public void renderTreeString(StringBuffer buf, Map context, TreeStringRenderer treeStringRenderer) throws GeneralException {
        ModelNode node = (ModelNode)nodeMap.get(rootNodeName);
        /*
        List parentNodeTrail = (List)context.get("currentNodeTrail");
        if (parentNodeTrail != null)
            currentNodeTrail = new ArrayList(parentNodeTrail);
        else
        */
            currentNodeTrail = new ArrayList();
            
        //Map requestParameters = (Map)context.get("requestParameters");
        //String treeString = (String)requestParameters.get("trail");
        String trailName = trailNameExdr.expandString(context);
        String treeString = (String)context.get(trailName);
        if (UtilValidate.isEmpty(treeString)) {
            Map parameters = (Map)context.get("parameters");
            treeString = (String)parameters.get(trailName);
        }
        if (UtilValidate.isNotEmpty(treeString)) {
            trail = StringUtil.split(treeString, "|");
            if (trail == null || trail.size() == 0)
                throw new RuntimeException("Tree 'trail' value is empty.");
            
            context.put("rootEntityId", trail.get(0));
            context.put(defaultPkName, trail.get(0));
            context.put("targetNodeTrail", trail);
        } else {
                Debug.logError("Trail value is empty.", module);
        }
        StringWriter writer = new StringWriter();
        try {
            node.renderNodeString(writer, context, treeStringRenderer, 0, true);
            buf.append(writer.toString());
        } catch (IOException e2) {
                String errMsg = "Error rendering included label with name [" + name + "] : " + e2.toString();
                Debug.logError(e2, errMsg, module);
                throw new RuntimeException(errMsg);
        }
//        try {
//            FileOutputStream fw = new FileOutputStream(new File("/usr/local/agi/ofbiz/hot-deploy/ofbizdoc/misc/profile.data"));
//            Profiler.print(fw);
//            fw.close();
//        } catch (IOException e) {
//           Debug.logError("[PROFILER] " + e.getMessage(),"");
//        }


    }

    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public GenericDelegator getDelegator() {
        return this.delegator;
    }

    public String getRenderStyle() {
        return this.defaultRenderStyle;
    }
    

    public static class ModelNode {

        protected FlexibleStringExpander screenNameExdr;
        protected FlexibleStringExpander screenLocationExdr;
        protected String shareScope;
        protected Label label;
        protected Link link;
        protected Image image;
        protected List subNodeList = new ArrayList();
        protected List actions = new ArrayList();
        protected String name;
        protected ModelTree modelTree;
        protected List subNodeValues;
        protected String expandCollapseStyle;
        protected FlexibleStringExpander wrapStyleExdr;
        protected ModelTreeCondition condition;
        protected String renderStyle;
        protected String entryName;
        protected String entityName;
        protected String pkName;

        public ModelNode() {}

        public ModelNode(Element nodeElement, ModelTree modelTree) {

            this.modelTree = modelTree;
            this.name = nodeElement.getAttribute("name");
            this.expandCollapseStyle = nodeElement.getAttribute("expand-collapse-style");
            this.wrapStyleExdr = new FlexibleStringExpander(nodeElement.getAttribute("wrap-style"));
            this.renderStyle = nodeElement.getAttribute("render-style");
            this.entryName = UtilFormatOut.checkEmpty(nodeElement.getAttribute("entry-name"), null); 
            setEntityName(nodeElement.getAttribute("entity-name"));
            if (this.pkName == null || nodeElement.hasAttribute("join-field-name"))
                this.pkName = nodeElement.getAttribute("join-field-name");
    
            Element actionElement = UtilXml.firstChildElement(nodeElement, "entity-one");
            if (actionElement != null) {
               actions.add(new ModelTreeAction.EntityOne(this, actionElement));
            }
            
            actionElement = UtilXml.firstChildElement(nodeElement, "service");
            if (actionElement != null) {
                actions.add(new ModelTreeAction.Service(this, actionElement));
            }
            
            actionElement = UtilXml.firstChildElement(nodeElement, "script");
            if (actionElement != null) {
                actions.add(new ModelTreeAction.Script(this, actionElement));
            }
        
            Element screenElement = UtilXml.firstChildElement(nodeElement, "include-screen");
            if (screenElement != null) {
                this.screenNameExdr =  new FlexibleStringExpander(screenElement.getAttribute("name"));
                this.screenLocationExdr =  new FlexibleStringExpander(screenElement.getAttribute("location"));
                this.shareScope =  screenElement.getAttribute("share-scope");
            }
            
            Element labelElement = UtilXml.firstChildElement(nodeElement, "label");
            if (labelElement != null) {
                this.label = new Label(labelElement);
            }
    
            Element linkElement = UtilXml.firstChildElement(nodeElement, "link");
            if (linkElement != null) {
                this.link = new Link(linkElement);
            }
            
            Element imageElement = UtilXml.firstChildElement(nodeElement, "image");
            if (imageElement != null) {
                this.image = new Image(imageElement);
            }
    
            /* there are situations in which nothing should be displayed
            if (screenElement == null && labelElement == null && linkElement == null) {
                throw new IllegalArgumentException("Neither 'screen' nor 'label' nor 'link' found for the node definition with name: " + this.name);
            }
            */
        Element conditionElement = UtilXml.firstChildElement(nodeElement, "condition");
        if (conditionElement != null) {
            this.condition = new ModelTreeCondition(modelTree, conditionElement);
        }

            List subNodeElements = UtilXml.childElementList(nodeElement, "sub-node");
            Iterator subNodeElementIter = subNodeElements.iterator();
            while (subNodeElementIter.hasNext()) {
                Element subNodeElementEntry = (Element) subNodeElementIter.next();
                ModelSubNode subNode = new ModelSubNode(subNodeElementEntry, this);
                subNodeList.add(subNode);
            }
    
            
        }
    
        public void renderNodeString(Writer writer, Map context,
                TreeStringRenderer treeStringRenderer, int depth, boolean isLast)
                throws IOException, GeneralException {
            boolean passed = true;
            if (this.condition != null) {
                if (!this.condition.eval(context)) {
                    passed = false;
                }
            }
            //Debug.logInfo("in ModelMenu, name:" + this.getName(), module);
            if (passed) {
                //this.subNodeValues = new ArrayList();
                //context.put("subNodeValues", new ArrayList());
                //if (Debug.infoOn()) Debug .logInfo(" renderNodeString, " + modelTree.getdefaultPkName() + " :" + context.get(modelTree.getdefaultPkName()), module);
                context.put("processChildren", Boolean.TRUE);
                // this action will usually obtain the "current" entity
                ModelTreeAction.runSubActions(this.actions, context);
                String pkName = getPkName();
                String id = null;
                if (UtilValidate.isNotEmpty(this.entryName)) {
                    Map map = (Map)context.get(this.entryName);
                    id = (String)map.get(pkName);
                } else {
                    id = (String) context.get(pkName);
                }
                if (id != null) { 
                    modelTree.currentNodeTrail.add(id);
                }
                context.put("currentNodeTrail", modelTree.currentNodeTrail);
                String currentNodeTrailPiped = StringUtil.join(modelTree.currentNodeTrail, "|");
                context.put("currentNodeTrailPiped", currentNodeTrailPiped);
                treeStringRenderer.renderNodeBegin(writer, context, this, depth, isLast);
                //if (Debug.infoOn()) Debug.logInfo(" context:" +
                // context.entrySet(), module);
                try {
                    String screenName = null;
                    if (screenNameExdr != null)
                        screenName = screenNameExdr.expandString(context);
                    String screenLocation = null;
                    if (screenLocationExdr != null)
                        screenLocation = screenLocationExdr.expandString(context);
                    if (screenName != null && screenLocation != null) {
                        ScreenStringRenderer screenStringRenderer = treeStringRenderer .getScreenStringRenderer(context);
                        ModelScreen modelScreen = ScreenFactory .getScreenFromLocation(screenLocation, screenName);
                        modelScreen.renderScreenString(writer, context, screenStringRenderer);
                    }
                    if (label != null) {
                        label.renderLabelString(writer, context, treeStringRenderer);
                    }
                    if (link != null) {
                        link.renderLinkString(writer, context, treeStringRenderer);
                    }
                    treeStringRenderer.renderLastElement(writer, context, this);
                    Boolean processChildren = (Boolean) context .get("processChildren");
                    //if (Debug.infoOn()) Debug.logInfo(" processChildren:" + processChildren, module);
                    if (processChildren.booleanValue()) {
                        getChildren(context);
                        Iterator nodeIter = this.subNodeValues.iterator();
                        int nodeIndex = -1;
                        int newDepth = depth + 1;
                        while (nodeIter.hasNext()) {
                            nodeIndex++;
                            modelTree.setNodeIndexAtDepth(newDepth, nodeIndex);
                            Object[] arr = (Object[]) nodeIter.next();
                            ModelNode node = (ModelNode) arr[0];
                            Map val = (Map) arr[1];
                            //GenericPK pk = val.getPrimaryKey();
                            //if (Debug.infoOn()) Debug.logInfo(" pk:" + pk,
                            // module);
                            String thisPkName = node.getPkName();
                            String thisEntityId = (String) val.get(thisPkName);
                            Map newContext = ((MapStack) context) .standAloneChildStack();
                            String nodeEntryName = node.getEntryName();
                            if (UtilValidate.isNotEmpty(nodeEntryName)) {
                                newContext.put(nodeEntryName, val);                        
                            } else {
                                newContext.putAll(val);
                            }
                            newContext.put("currentNodeIndex", new Integer(nodeIndex));
                            String targetEntityId = null;
                            List targetNodeTrail = this.modelTree .getTrailList();
                            if (newDepth < targetNodeTrail.size()) {
                                targetEntityId = (String) targetNodeTrail .get(newDepth);
                            }
                            if ((targetEntityId != null && targetEntityId .equals(thisEntityId)) || this.showPeers(newDepth)) {
                                boolean lastNode = !nodeIter.hasNext();
                                newContext.put("lastNode", new Boolean(lastNode));
                                node.renderNodeString(writer, newContext, treeStringRenderer, newDepth, lastNode);
                            }
                        }
                    }
                } catch (SAXException e) {
                    String errMsg = "Error rendering included label with name ["
                            + name + "] : " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                } catch (ParserConfigurationException e3) {
                    String errMsg = "Error rendering included label with name ["
                            + name + "] : " + e3.toString();
                    Debug.logError(e3, errMsg, module);
                    throw new RuntimeException(errMsg);
                } catch (IOException e2) {
                    String errMsg = "Error rendering included label with name ["
                            + name + "] : " + e2.toString();
                    Debug.logError(e2, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
                treeStringRenderer.renderNodeEnd(writer, context, this);
                int removeIdx = modelTree.currentNodeTrail.size() - 1;
                if (removeIdx >= 0) modelTree.currentNodeTrail.remove(removeIdx);
            }
        }

        public boolean hasChildren(Map context) {

             boolean hasChildren = false;
             Long nodeCount = null;
             String countFieldName = "childBranchCount";
               Object obj = null;
             if (UtilValidate.isNotEmpty(this.entryName)) {
                    Map map = (Map)context.get(this.entryName);
                    if (map instanceof GenericValue) {
                    	ModelEntity modelEntity = ((GenericValue)map).getModelEntity();
                    	if (modelEntity.isField(countFieldName)) {
                    		obj = map.get(countFieldName);
                    	}
                    }
             } else {
                   obj = context.get(countFieldName);
             }
             if (obj != null) {
                nodeCount = (Long)obj;
             }
             String entName = this.getEntityName();
             GenericDelegator delegator = modelTree.getDelegator();
             ModelEntity modelEntity = delegator.getModelEntity(entName);
             ModelField modelField = null;
         	 if (modelEntity.isField(countFieldName)) {
                 modelField = modelEntity.getField(countFieldName); 
        	 }
             if (nodeCount == null && modelField != null || this.modelTree.forceChildCheck) {
                 getChildren(context);
                 /*
                 String id = (String)context.get(modelTree.getPkName());
                 if (UtilValidate.isNotEmpty(id)) {
                     try {
                         int leafCount = ContentManagementWorker.updateStatsTopDown(delegator, id, UtilMisc.toList("SUB_CONTENT", "PUBLISH_LINK"));
                         GenericValue entity = delegator.findByPrimaryKeyCache(entName, UtilMisc.toMap(modelTree.getPkName(), id));
                         obj = entity.get("childBranchCount");
                        if (obj != null)
                            nodeCount = (Long)obj;
                     } catch(GenericEntityException e) {
                         Debug.logError(e, module); 
                        throw new RuntimeException(e.getMessage());
                     }
                 }
                 */
                 nodeCount = new Long(this.subNodeValues.size());
                String pkName = this.getPkName();
                String id = null;
                if (UtilValidate.isNotEmpty(this.entryName)) {
                    Map map = (Map)context.get(this.entryName);
                    id = (String)map.get(pkName);
                } else {
                    id = (String) context.get(pkName);
                }
                 try {
                	 if (id != null && modelEntity.getPksSize() == 1) {
                		 GenericValue entity = delegator.findByPrimaryKey(entName, UtilMisc.toMap(pkName, id));
                         if (modelEntity.isField("childBranchCount")) {
                    		 entity.put("childBranchCount", nodeCount);
                         }
                		 entity.store();
                	 }
                 } catch(GenericEntityException e) {
                     Debug.logError(e, module); 
                    throw new RuntimeException(e.getMessage());
                 }
             } else if (nodeCount == null) {
                 getChildren(context);
                if (subNodeValues != null)
                    nodeCount = new Long(subNodeValues.size());
             }
             
             if (nodeCount != null && nodeCount.intValue() > 0) 
                 hasChildren = true;
                
             
             return hasChildren;
        }

        public void getChildren(Map context) {
             this.subNodeValues = new ArrayList();
             Iterator nodeIter = subNodeList.iterator();
             while (nodeIter.hasNext()) {
                 ModelSubNode subNode = (ModelSubNode)nodeIter.next();
                 String nodeName = subNode.getNodeName(context);
                 ModelNode node = (ModelNode)modelTree.nodeMap.get(nodeName);
                 List subNodeActions = subNode.getActions();
                 //if (Debug.infoOn()) Debug.logInfo(" context.currentValue:" + context.get("currentValue"), module);
                 ModelTreeAction.runSubActions(subNodeActions, context);
                 // List dataFound = (List)context.get("dataFound");
                 ListIterator dataIter =  subNode.getListIterator();
                 if (dataIter instanceof EntityListIterator) {
                     EntityListIterator eli = (EntityListIterator) dataIter;
                     Map val = null;
                     while ((val = (Map) eli.next()) != null) {
                         Object [] arr = {node, val};
                         this.subNodeValues.add(arr);
                     }
                     try {
                         eli.close();
                     } catch(GenericEntityException e) {
                         Debug.logError(e, module);
                         throw new RuntimeException(e.getMessage());
                     }
                 } else if (dataIter != null) {
                     while (dataIter.hasNext()) {
                         Map val = (Map) dataIter.next();
                         Object [] arr = {node, val};
                         this.subNodeValues.add(arr);
                     }
                 }
             }
        }

        public String getName() {
            return name;
        }

        public String getEntryName() {
            return this.entryName;
        }

        public String getRenderStyle() {
            String rStyle = this.renderStyle;
            if (UtilValidate.isEmpty(rStyle))
                rStyle = modelTree.getRenderStyle();
            return rStyle;
        }
    
        public boolean isExpandCollapse() {
            boolean isExpCollapse = false;
            String rStyle = getRenderStyle();
            if (rStyle != null && rStyle.equals("expand-collapse"))
                isExpCollapse = true;
            
            return isExpCollapse;
        }
        
        public boolean isFollowTrail() {
            boolean isFollowTrail = false;
            String rStyle = getRenderStyle();
            if (rStyle != null && (rStyle.equals("follow-trail") || rStyle.equals("show-peers") || rStyle.equals("follow-trail"))) {
                isFollowTrail = true;
            }
            
            return isFollowTrail;
        }
    
        public boolean isRootNode() {
            return name.equals(modelTree.getRootNodeName());
        }
        
        public boolean showPeers(int currentDepth) {
            int trailSize = 0;
            List trail = modelTree.getTrailList();
            int openDepth = modelTree.getOpenDepth();
            int postTrailOpenDepth = modelTree.getPostTrailOpenDepth();
            if (trail != null) trailSize = trail.size();
                
            boolean showPeers = false;
            String rStyle = getRenderStyle();
            if (rStyle == null) {
                showPeers = true;
            } else if (!isFollowTrail()) {
                showPeers = true;
            } else if ((currentDepth < trailSize) && (rStyle != null) &&  (rStyle.equals("show-peers") || rStyle.equals("expand-collapse"))) {
                showPeers = true;
            } else if (openDepth >= currentDepth) {
                showPeers = true;
            } else {
                int depthAfterTrail = currentDepth - trailSize;
                if (depthAfterTrail >= 0 && depthAfterTrail <= postTrailOpenDepth) showPeers = true;
            }
            
            return showPeers;
        }
    
        public String getExpandCollapseStyle() {
            return expandCollapseStyle;
        }

        public String getWrapStyle(Map context) {
            String val = this.wrapStyleExdr.expandString(context);
            if (UtilValidate.isEmpty(val)) {
                val = this.modelTree.getWrapStyle(context);
            }
            return val;
        }
    
        public ModelTree getModelTree() {
            return this.modelTree;
        }
        
        public void setEntityName(String name) {
            this.entityName = name;
            if (UtilValidate.isNotEmpty(this.entityName)) {
                ModelEntity modelEntity = modelTree.delegator.getModelEntity(this.entityName);
                if (modelEntity.getPksSize() == 1) {
                    ModelField modelField = modelEntity.getOnlyPk();
                    this.pkName = modelField.getName();
                } else {
                    // TODO: what to do here?
                }
            }
        }
        
        public String getEntityName() {
            if (UtilValidate.isNotEmpty(this.entityName)) {
                return this.entityName;
            } else {
                return this.modelTree.getDefaultEntityName();
            }
        }
    
        public String getPkName() {
            if (UtilValidate.isNotEmpty(this.pkName)) {
                return this.pkName;
            } else {
                return this.modelTree.getDefaultPkName();
            }
        }
    
        public void setPkName(String pkName) {
        	this.pkName = pkName;
        }
        
        public static class ModelSubNode {
    
            protected ModelNode rootNode;
            protected FlexibleStringExpander nodeNameExdr;
            protected List actions = new ArrayList();
            protected List outFieldMaps;
            protected ListIterator listIterator;
    
            public ModelSubNode() {}
    
            public ModelSubNode(Element nodeElement, ModelNode modelNode) {
    
                this.rootNode = modelNode;
                this.nodeNameExdr = new FlexibleStringExpander(nodeElement.getAttribute("node-name"));
        
                Element actionElement = UtilXml.firstChildElement(nodeElement, "entity-and");
                if (actionElement != null) {
                   actions.add(new ModelTreeAction.EntityAnd(this, actionElement));
                }
                
                actionElement = UtilXml.firstChildElement(nodeElement, "service");
                if (actionElement != null) {
                    actions.add(new ModelTreeAction.Service(this, actionElement));
                }
                
                actionElement = UtilXml.firstChildElement(nodeElement, "entity-condition");
                if (actionElement != null) {
                    actions.add(new ModelTreeAction.EntityCondition(this, actionElement));
                }
                
                actionElement = UtilXml.firstChildElement(nodeElement, "script");
                if (actionElement != null) {
                    actions.add(new ModelTreeAction.Script(this, actionElement));
                }
        
            }
            
            public ModelTree.ModelNode getNode() {
                return this.rootNode;
            }
        
            public String getNodeName(Map context) {
                return this.nodeNameExdr.expandString(context);
            }
    
            public List getActions() {
                return actions;
            }
            
            public void setListIterator(ListIterator iter) {
                listIterator = iter;
            }
        
            public ListIterator getListIterator() {
                return listIterator;
            }
        }
    
        public static class Label {
            protected FlexibleStringExpander textExdr;
            
            protected FlexibleStringExpander idExdr;
            protected FlexibleStringExpander styleExdr;
            
            public Label(Element labelElement) {
    
                // put the text attribute first, then the pcdata under the element, if both are there of course
                String textAttr = UtilFormatOut.checkNull(labelElement.getAttribute("text"));
                String pcdata = UtilFormatOut.checkNull(UtilXml.elementValue(labelElement));
                this.textExdr = new FlexibleStringExpander(textAttr + pcdata);
    
                this.idExdr = new FlexibleStringExpander(labelElement.getAttribute("id"));
                this.styleExdr = new FlexibleStringExpander(labelElement.getAttribute("style"));
            }
    
            public void renderLabelString(Writer writer, Map context, TreeStringRenderer treeStringRenderer) {
                try {
                    treeStringRenderer.renderLabel(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering label with id [" + getId(context) + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            }
            
            public String getText(Map context) {
                return this.textExdr.expandString(context);
            }
            
            public String getId(Map context) {
                return this.idExdr.expandString(context);
            }
            
            public String getStyle(Map context) {
                return this.styleExdr.expandString(context);
            }
        }

    
        public static class Link {
            protected FlexibleStringExpander textExdr;
            protected FlexibleStringExpander idExdr;
            protected FlexibleStringExpander styleExdr;
            protected FlexibleStringExpander targetExdr;
            protected FlexibleStringExpander targetWindowExdr;
            protected FlexibleStringExpander prefixExdr;
            protected FlexibleStringExpander nameExdr;
            protected FlexibleStringExpander titleExdr;
            protected Image image;
            protected String urlMode = "intra-app";
            protected boolean fullPath = false;
            protected boolean secure = false;
            protected boolean encode = false;
            
            public Link() {
                setText(null);
                setId(null);
                setStyle(null);
                setTarget(null);
                setTargetWindow(null);
                setPrefix(null);
                setUrlMode(null);
                setFullPath(null);
                setSecure(null);
                setEncode(null);
                setName(null);
                setTitle(null);
            }

            public Link(Element linkElement) {
    
                setText(linkElement.getAttribute("text"));
                setId(linkElement.getAttribute("id"));
                setStyle(linkElement.getAttribute("style"));
                setTarget(linkElement.getAttribute("target"));
                setTargetWindow(linkElement.getAttribute("target-window"));
                setPrefix(linkElement.getAttribute("prefix"));
                setUrlMode(linkElement.getAttribute("url-mode"));
                setFullPath(linkElement.getAttribute("full-path"));
                setSecure(linkElement.getAttribute("secure"));
                setEncode(linkElement.getAttribute("encode"));
                setName(linkElement.getAttribute("name"));
                setTitle(linkElement.getAttribute("title"));
                Element imageElement = UtilXml.firstChildElement(linkElement, "image");
                if (imageElement != null) {
                    this.image = new Image(imageElement);
                }
    
            }
    
            public void renderLinkString(Writer writer, Map context, TreeStringRenderer treeStringRenderer) {
                try {
                    treeStringRenderer.renderLink(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering link with id [" + getId(context) + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            }
            
            public String getText(Map context) {
                return this.textExdr.expandString(context);
            }
            
            public String getId(Map context) {
                return this.idExdr.expandString(context);
            }
            
            public String getStyle(Map context) {
                return this.styleExdr.expandString(context);
            }
            
            public String getName(Map context) {
                return this.nameExdr.expandString(context);
            }
            public String getTitle(Map context) {
                return this.titleExdr.expandString(context);
            }
        
            public String getTarget(Map context) {
                return this.targetExdr.expandString(context);
            }
            
            public String getTargetWindow(Map context) {
                return this.targetWindowExdr.expandString(context);
            }
            
            public String getUrlMode() {
                return this.urlMode;
            }
            
            public String getPrefix(Map context) {
                return this.prefixExdr.expandString(context);
            }
            
            public boolean getFullPath() {
                return this.fullPath;
            }
            
            public boolean getSecure() {
                return this.secure;
            }
            
            public boolean getEncode() {
                return this.encode;
            }
            
            public Image getImage() {
                return this.image;
            }

            public void setText(String val) {
                String textAttr = UtilFormatOut.checkNull(val);
                this.textExdr = new FlexibleStringExpander(textAttr);
            }
            public void setId(String val) {
                this.idExdr = new FlexibleStringExpander(val);
            }
            public void setStyle(String val) {
                this.styleExdr = new FlexibleStringExpander(val);
            }
            public void setName(String val) {
                this.nameExdr = new FlexibleStringExpander(val);
            }
            public void setTitle(String val) {
                this.titleExdr = new FlexibleStringExpander(val);
            }
            public void setTarget(String val) {
                this.targetExdr = new FlexibleStringExpander(val);
            }
            public void setTargetWindow(String val) {
                this.targetWindowExdr = new FlexibleStringExpander(val);
            }
            public void setPrefix(String val) {
                this.prefixExdr = new FlexibleStringExpander(val);
            }
            public void setUrlMode(String val) {
                if (UtilValidate.isNotEmpty(val))
                    this.urlMode = val;
            }
            public void setFullPath(String val) {
                String sFullPath = val;
                if (sFullPath != null && sFullPath.equalsIgnoreCase("true"))
                    this.fullPath = true;
                else
                    this.fullPath = false;
            }

            public void setSecure(String val) {
                String sSecure = val;
                if (sSecure != null && sSecure.equalsIgnoreCase("true"))
                    this.secure = true;
                else
                    this.secure = false;
            }

            public void setEncode(String val) {
                String sEncode = val;
                if (sEncode != null && sEncode.equalsIgnoreCase("true"))
                    this.encode = true;
                else
                    this.encode = false;
            }
            public void setImage(Image img) {
                this.image = img;
            }
                
        }

        public static class Image {

            protected FlexibleStringExpander srcExdr;
            protected FlexibleStringExpander idExdr;
            protected FlexibleStringExpander styleExdr;
            protected FlexibleStringExpander widthExdr;
            protected FlexibleStringExpander heightExdr;
            protected FlexibleStringExpander borderExdr;
            protected String urlMode;
            
            public Image() {

                setSrc(null);
                setId(null);
                setStyle(null);
                setWidth(null);
                setHeight(null);
                setBorder("0");
                setUrlMode(null);
            }

            public Image(Element imageElement) {
    
                setSrc(imageElement.getAttribute("src"));
                setId(imageElement.getAttribute("id"));
                setStyle(imageElement.getAttribute("style"));
                setWidth(imageElement.getAttribute("width"));
                setHeight(imageElement.getAttribute("height"));
                setBorder(UtilFormatOut.checkEmpty(imageElement.getAttribute("border"), "0"));
                setUrlMode(UtilFormatOut.checkEmpty(imageElement.getAttribute("url-mode"), "content"));
    
            }
    
            public void renderImageString(Writer writer, Map context, TreeStringRenderer treeStringRenderer) {
                try {
                    treeStringRenderer.renderImage(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering image with id [" + getId(context) + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            }
            
            public String getSrc(Map context) {
                return this.srcExdr.expandString(context);
            }
            
            public String getId(Map context) {
                return this.idExdr.expandString(context);
            }
            
            public String getStyle(Map context) {
                return this.styleExdr.expandString(context);
            }

            public String getWidth(Map context) {
                return this.widthExdr.expandString(context);
            }

            public String getHeight(Map context) {
                return this.heightExdr.expandString(context);
            }

            public String getBorder(Map context) {
                return this.borderExdr.expandString(context);
            }
            
            public String getUrlMode() {
                return this.urlMode;
            }
            
            public void setSrc(String val) {
                String textAttr = UtilFormatOut.checkNull(val);
                this.srcExdr = new FlexibleStringExpander(textAttr);
            }
            public void setId(String val) {
                this.idExdr = new FlexibleStringExpander(val);
            }
            public void setStyle(String val) {
                this.styleExdr = new FlexibleStringExpander(val);
            }
            public void setWidth(String val) {
                this.widthExdr = new FlexibleStringExpander(val);
            }
            public void setHeight(String val) {
                this.heightExdr = new FlexibleStringExpander(val);
            }
            public void setBorder(String val) {
                this.borderExdr = new FlexibleStringExpander(val);
            }
            public void setUrlMode(String val) {
                if (UtilValidate.isEmpty(val))
                    this.urlMode = "content";
                else
                    this.urlMode = val;
            }
                
        }

    }
}

