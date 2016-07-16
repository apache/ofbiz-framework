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
package org.apache.ofbiz.webapp.webdav;

import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.base.util.UtilValidate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** PROPFIND HTTP method helper class. This class provides helper methods for
 * working with WebDAV PROPFIND requests and responses.*/
public class PropFindHelper extends ResponseHelper {

    protected final Document requestDocument;

    public PropFindHelper(Document requestDocument) {
        this.requestDocument = requestDocument;
    }

    public Element createPropElement(List<Element> propList) {
        Element element = this.responseDocument.createElementNS(DAV_NAMESPACE_URI, "D:prop");
        if (UtilValidate.isNotEmpty(propList)) {
            for (Element propElement : propList) {
                element.appendChild(propElement);
            }
        }
        return element;
    }

    public Element createPropStatElement(Element prop, String stat) {
        Element element = this.responseDocument.createElementNS(DAV_NAMESPACE_URI, "D:propstat");
        element.appendChild(prop);
        element.appendChild(createStatusElement(stat));
        return element;
    }

    public List<Element> getFindPropsList(String nameSpaceUri) {
        List<Element> result = new LinkedList<Element>();
        NodeList nodeList = this.requestDocument.getElementsByTagNameNS(nameSpaceUri == null ? "*" : nameSpaceUri, "prop");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i).getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.add((Element) node);
                }
                node = node.getNextSibling();
            }
        }
        return result;
    }

    public boolean isAllProp() {
        NodeList nodeList = this.requestDocument.getElementsByTagNameNS(DAV_NAMESPACE_URI, "allprop");
        return nodeList.getLength() > 0;
    }

    public boolean isPropName() {
        NodeList nodeList = this.requestDocument.getElementsByTagNameNS(DAV_NAMESPACE_URI, "propname");
        return nodeList.getLength() > 0;
    }

}
