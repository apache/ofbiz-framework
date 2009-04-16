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
package org.ofbiz.webslinger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.cache.Cache;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.SecurityFactory;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.webslinger.AbstractMappingWebslingerServletContextFactory;
import org.webslinger.WebslingerServletContext;
import org.webslinger.collections.CollectionUtil;
import org.webslinger.lang.ObjectUtil;

public class WebslingerContextMapper extends AbstractMappingWebslingerServletContextFactory {
    protected ServletContext servletContext;
    protected GenericDelegator delegator;
    protected final ArrayList<URL> globalReaderURLs = new ArrayList<URL>();

    public void init(ServletConfig config) throws ServletException, IOException {
        System.err.println(org.webslinger.commons.vfs.flat.FlatFileProvider.class);
        servletContext = config.getServletContext();
        String delegatorName = servletContext.getInitParameter("entityDelegatorName");
        delegator = GenericDelegator.getGenericDelegator(delegatorName);
        String readerFiles = servletContext.getInitParameter("serviceReaderUrls");
        if (readerFiles != null) {
            for (String reader: CollectionUtil.split(readerFiles, ";")) {
                URL url =  config.getServletContext().getResource(reader);
                if (url != null) globalReaderURLs.add(url);
            }
        }
        super.init(config, UtilProperties.getPropertyValue("webslinger.properties", "moduleBase"));
    }

    protected Layout[] getStartLayouts() throws Exception {
        ArrayList<Layout> layouts = new ArrayList<Layout>();
        try {
            for (GenericValue value: delegator.findByAnd("WebslingerServer", UtilMisc.toMap("loadAtStart", "Y"))) {
                layouts.add(new OfbizLayout(value));
            }
        } catch (GenericEntityException e) {
        }
        return layouts.toArray(new Layout[layouts.size()]);
    }

    public void initializeRequest(WebslingerServletContext context, HttpServletRequest request) {
        request.setAttribute("servletContext", context);
        Object delegator = context.getAttribute("delegator");
        Object dispatcher = context.getAttribute("dispatcher");
        Object security = context.getAttribute("security");
        request.setAttribute("delegator", delegator);
        request.setAttribute("dispatcher", dispatcher);
        request.setAttribute("security", security);
        // FIXME!!! These next two are a hack until proper fake/wrapped session support is done in webslinger
        servletContext.setAttribute("delegator", delegator);
        servletContext.setAttribute("dispatcher", dispatcher);
        servletContext.setAttribute("security", security);
    }

    protected void initializeContext(WebslingerServletContext context, Layout layout) throws Exception {
        OfbizLayout ofbizLayout = (OfbizLayout) layout;
        GenericDelegator delegator = GenericDelegator.getGenericDelegator(ofbizLayout.delegatorName);
        context.setAttribute("delegator", delegator);
        context.setAttribute("dispatcher", createLocalDispatcher(context, layout.getTarget(), delegator));
        context.setAttribute("security", SecurityFactory.getInstance(delegator));
    }

    protected LocalDispatcher createLocalDispatcher(WebslingerServletContext context, String name, GenericDelegator delegator) throws IOException {
        ArrayList<URL> readerURLs = new ArrayList<URL>(globalReaderURLs);
        String readerFiles = context.getInitParameter("serviceReaderUrls");
        if (readerFiles != null) {
            for (String reader: CollectionUtil.split(readerFiles, ";")) {
                URL url =  context.getResource(reader);
                if (url != null) readerURLs.add(url);
            }
        }
        System.err.println(readerURLs);
        try {
            return GenericDispatcher.newInstance(name, delegator, readerURLs, true, true, true);
        } catch (GenericServiceException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    protected Set<String> getSuffixes() throws Exception {
        Cache cache = delegator.getCache();
        Set<String> suffixes;
        synchronized (WebslingerContextMapper.class) {
            suffixes = (Set<String>) cache.get("WebslingerHostSuffix", null, "WebslingerContextMapper.Suffixes");
            if (suffixes == null) {
                suffixes = new HashSet<String>();
                for (GenericValue value: delegator.findList("WebslingerHostSuffix", null, null, null, null, false)) {
                    suffixes.add(value.getString("hostSuffix"));
                }
                cache.put("WebslingerHostSuffix", null, "WebslingerContextMapper.Suffixes", suffixes);
            }
        }
        return suffixes;
    }

    protected Layout lookupLayout(String hostName, String contextPath) throws Exception {
        GenericValue layout = EntityUtil.getOnly(delegator.findByAndCache("WebslingerLayout", UtilMisc.toMap("hostName", hostName, "contextPath", contextPath)));
        if (layout == null) return null;
        return new OfbizLayout(layout);
    }

    protected class OfbizLayout implements Layout {
        private final String contextPath;
        private final String id;
        private final String target;
        private final String[] bases;
        private final int hashCode;
        protected final String delegatorName;
        protected final String dispatcherName;

        protected OfbizLayout(GenericValue server) throws GenericEntityException {
            contextPath = server.getString("contextPath");
            id = server.getString("webslingerServerId");
            target = server.getString("target");
            List<GenericValue> baseValues = server.getRelatedCache("WebslingerServerBase", UtilMisc.toList("seqNum"));
            bases = new String[baseValues.size()];
            for (int i = 0; i < bases.length; i++) {
                GenericValue baseValue = baseValues.get(i);
                bases[i] = baseValue.getString("baseName");
            }
            delegatorName = server.getString("delegatorName");
            dispatcherName = server.getString("dispatcherName");
            hashCode = target.hashCode() ^ ObjectUtil.hashCodeHelper(delegatorName) ^ Arrays.hashCode(bases);
        }

        public String getContextPath() {
            return contextPath;
        }

        public String getId() {
            return id;
        }

        public String getTarget() {
            return target;
        }

        public String[] getBases() {
            return bases;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object o) {
            if (!(o instanceof OfbizLayout)) return false;
            OfbizLayout other = (OfbizLayout) o;
            if (!contextPath.equals(other.contextPath)) return false;
            if (!target.equals(other.target)) return false;
            if (!ObjectUtil.equalsHelper(delegatorName, other.delegatorName)) return false;
            return Arrays.equals(bases, other.bases);
        }
    }
}
