package org.ofbiz.jcr.orm.jackrabbit.file;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.NTCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrType = "nt:folder", extend = JackrabbitHierarchyNode.class)
public class JackrabbitFolder extends JackrabbitHierarchyNode {
    @Collection(autoUpdate = true, jcrSameNameSiblings = false, elementClassName = JackrabbitHierarchyNode.class, collectionConverter = NTCollectionConverterImpl.class)
    private List<JackrabbitHierarchyNode> children;

    public List<JackrabbitHierarchyNode> getChildren() {
        return children;
    }

    public void setChildren(List<JackrabbitHierarchyNode> children) {
        this.children = children;
    }

    public void addChild(JackrabbitHierarchyNode node) {
        if (children == null) {
            children = new ArrayList<JackrabbitHierarchyNode>();
        }
        children.add(node);
    }

}
