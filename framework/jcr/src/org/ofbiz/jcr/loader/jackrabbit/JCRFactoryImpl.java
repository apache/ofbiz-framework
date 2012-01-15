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
package org.ofbiz.jcr.loader.jackrabbit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.ocm.nodemanagement.impl.RepositoryUtil;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.jcr.loader.JCRFactory;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitArticle;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitFile;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitFolder;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitHierarchyNode;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitLocalizedContent;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitNews;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitResource;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitUnstructured;
import org.w3c.dom.Element;

public class JCRFactoryImpl implements JCRFactory {

    public static final String module = JCRFactoryImpl.class.getName();

    private static String CUSTOM_NODE_TYPES = "framework/jcr/config/custom-jackrabbit-nodetypes.xml";

    private static String homeDir = null;
    private static String jackrabbitConfigFile = null;
    private static String CREDENTIALS_USERNAME = null;
    private static char[] CREDENTIALS_PASSWORD = null;

    protected static Repository repository = null;
    protected Session session = null;
    protected static Mapper mapper = null;

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.JCRFactory#initialize(org.w3c.dom.Element)
     */
    @Override
    public void initialize(Element configRootElement) throws RepositoryException {
        Element childElement = UtilXml.firstChildElement(configRootElement, "jcr-credentials");
        CREDENTIALS_USERNAME = UtilXml.elementAttribute(childElement, "username", null);
        CREDENTIALS_PASSWORD = UtilXml.elementAttribute(childElement, "password", null).toCharArray();

        jackrabbitConfigFile = UtilXml.childElementAttribute(configRootElement, "config-file-path", "path", "framework/jcr/config/jackrabbit.xml");
        homeDir = UtilXml.childElementAttribute(configRootElement, "home-dir", "path", "runtime/data/jcr/");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.JCRFactory#start()
     */
    @Override
    public void start() throws RepositoryException {
        // Transient repositories closes automatically when the last session is
        // closed
        repository = new TransientRepository(jackrabbitConfigFile, homeDir);
        createSession();

        List<Class> classes = new ArrayList<Class>();
        // put this in an xml configuration file
        // should the ocm classes be loaded in during the container startup?
        classes.add(JackrabbitUnstructured.class);
        classes.add(JackrabbitHierarchyNode.class);
        classes.add(JackrabbitNews.class);
        classes.add(JackrabbitFile.class);
        classes.add(JackrabbitFolder.class);
        classes.add(JackrabbitResource.class);
        classes.add(JackrabbitLocalizedContent.class);
        classes.add(JackrabbitArticle.class);

        mapper = new AnnotationMapperImpl(classes);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.JCRFactory#stop(boolean)
     */
    @Override
    public void stop(boolean removeRepositoryOnShutdown) throws RepositoryException {
        if (session != null && session.isLive()) {
            session.logout();
        }

        if (removeRepositoryOnShutdown) {
            if (UtilValidate.isNotEmpty(homeDir)) {
                File homeDirFile = new File(homeDir);
                homeDirFile.deleteOnExit();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.JCRFactory#createSession()
     */
    @Override
    public Session createSession() throws RepositoryException {
        if (session == null || !session.isLive()) {
            Credentials credentials = new SimpleCredentials(CREDENTIALS_USERNAME, CREDENTIALS_PASSWORD);
            try {
                session = repository.login(credentials);
                // register NameSpaces
                RepositoryUtil.setupSession(session);
                try {
                    // register the cool new noteTypes
                    registerNodeTypes(session);
                } catch (InvalidNodeTypeDefException e) {
                    Debug.logError(e, module);
                } catch (IOException e) {
                    Debug.logError(e, module);
                }

            } catch (RepositoryException e) {
                Debug.logError(e, "Could not login to the workspace");
                throw e;
            }

        }
        return session;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.JCRFactory#getInstance()
     */
    @Override
    public Repository getInstance() {
        return repository;
    }

    public static Mapper getMapper() {
        return mapper;
    }

    /*
     * Register some new node types
     */
    protected void registerNodeTypes(Session session) throws InvalidNodeTypeDefException, javax.jcr.RepositoryException, IOException {
        InputStream xml = new FileInputStream(CUSTOM_NODE_TYPES);

        // HINT: throws InvalidNodeTypeDefException, IOException
        QNodeTypeDefinition[] types = NodeTypeReader.read(xml);

        Workspace workspace = session.getWorkspace();
        NodeTypeManager ntMgr = workspace.getNodeTypeManager();
        NodeTypeRegistry ntReg = ((NodeTypeManagerImpl) ntMgr).getNodeTypeRegistry();

        for (int j = 0; j < types.length; j++) {
            QNodeTypeDefinition def = types[j];

            try {
                ntReg.getNodeTypeDef(def.getName());
            } catch (NoSuchNodeTypeException nsne) {
                // HINT: if not already registered than register custom node
                // type
                ntReg.registerNodeType(def);
            }

        }
    }
}
