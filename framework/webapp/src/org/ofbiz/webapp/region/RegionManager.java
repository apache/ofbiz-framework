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
package org.ofbiz.webapp.region;

import java.net.URL;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class to manage the region cache and read a region XML file
 */
public class RegionManager {

    public static final String module = RegionManager.class.getName();

    protected static UtilCache<URL, Map<String, Region>> regionCache = UtilCache.createUtilCache("webapp.Regions.Config", 0, 0);

    protected URL regionFile = null;

    public RegionManager(URL regionFile) {
        this.regionFile = regionFile;
        if (regionFile == null) throw new IllegalArgumentException("regionFile cannot be null");

        //This may seem a bit funny, but we want to keep it in the cache so that it can be reloaded easily
        // Also note that we do not check to see if it is already there, in all cases we want to re-load the definition
        regionCache.put(regionFile, readRegionXml(regionFile));
    }

    public Map<String, Region> getRegions() {
        Map<String, Region> regions = regionCache.get(regionFile);
        if (regions == null) {
            synchronized (this) {
                regions = regionCache.get(regionFile);
                if (regions == null) {
                    if (Debug.verboseOn()) Debug.logVerbose("Regions not loaded for " + regionFile + ", loading now", module);
                    regions = readRegionXml(regionFile);
                    regionCache.put(regionFile, regions);
                }
            }
        }
        return regions;
    }

    public Region getRegion(String regionName) {
        if (regionFile == null) return null;
        return getRegions().get(regionName);
    }

    public void putRegion(Region region) {
        getRegions().put(region.getId(), region);
    }

    public Map<String, Region> readRegionXml(URL regionFile) {
        Map<String, Region> regions = FastMap.newInstance();

        Document document = null;

        try {
            document = UtilXml.readXmlDocument(regionFile, true);
        } catch (java.io.IOException e) {
            Debug.logError(e, module);
        } catch (org.xml.sax.SAXException e) {
            Debug.logError(e, module);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            Debug.logError(e, module);
        }

        if (document == null) return regions;

        Element rootElement = document.getDocumentElement();

        for (Element defineElement: UtilXml.childElementList(rootElement, "define")) {

            addRegion(defineElement, regions);
        }

        return regions;
    }

    protected void addRegion(Element defineElement, Map<String, Region> regions) {
        Region newRegion = null;

        String idAttr = defineElement.getAttribute("id");
        String templateAttr = defineElement.getAttribute("template");
        String regionAttr = defineElement.getAttribute("region");

        if (UtilValidate.isNotEmpty(templateAttr) && UtilValidate.isNotEmpty(regionAttr)) {
            throw new IllegalArgumentException("Cannot use both template and region attributes");
        }

        if (UtilValidate.isNotEmpty(templateAttr)) {
            newRegion = new Region(idAttr, templateAttr, null);
        } else {
            if (UtilValidate.isNotEmpty(regionAttr)) {
                Region parentRegion = regions.get(regionAttr);

                if (parentRegion == null) {
                    throw new IllegalArgumentException("can't find page definition attribute with this key: " + regionAttr);
                }
                newRegion = new Region(idAttr, parentRegion.getContent(), parentRegion.getSections());
            } else {
                throw new IllegalArgumentException("Must specify either the template or the region attribute");
            }
        }

        regions.put(idAttr, newRegion);

        for (Element putElement: UtilXml.childElementList(defineElement, "put")) {

            newRegion.put(makeSection(putElement));
        }
    }

    protected Section makeSection(Element putElement) {
        String bodyContent = UtilXml.elementValue(putElement);
        String section = putElement.getAttribute("section");
        String info = putElement.getAttribute("info");
        String content = putElement.getAttribute("content");
        String type = putElement.getAttribute("type");

        if (UtilValidate.isEmpty(type)) type = "default";

        if (UtilValidate.isNotEmpty(bodyContent) && UtilValidate.isNotEmpty(content)) {
            throw new IllegalArgumentException("Cannot use both content attribute and tag body text");
        }

        if (UtilValidate.isNotEmpty(bodyContent)) {
            content = bodyContent;
            type = "direct";
        }

        return new Section(section, info, content, type, this);
    }

    public static Region getRegion(URL regionFile, String regionName) {
        if (regionFile == null) return null;
        Map<String, Region> regions = regionCache.get(regionFile);
        if (regions == null) return null;
        return regions.get(regionName);
    }
}
