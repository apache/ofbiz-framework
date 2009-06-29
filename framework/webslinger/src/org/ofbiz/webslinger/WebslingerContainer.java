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
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.bsf.BSFException;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.commons.vfs.CommonsVfsContainer;

import org.webslinger.bsf.LanguageManager;
import org.webslinger.template.CompiledTemplate;
import org.webslinger.template.TemplateManager;
import org.webslinger.vfs.CommonsVfsFileNameVFSDelegate;
import org.webslinger.vfs.TypeVFSDelegate;

public class WebslingerContainer implements Container {
    private static TypeVFSDelegate vfsDelegate;
    private static LanguageManager languageManager;
    private static TemplateManager templateManager;
    private static final String[] templateParamNames = new String[] {"writer", "context"};
    private static final Class<?>[] templateParamTypes = new Class<?>[] {Writer.class, Map.class};

    public void init(String[] args, String configFile) throws ContainerException {
    }

    public boolean start() throws ContainerException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        TypeVFSDelegate.Resolver resolver = new TypeVFSDelegate.Resolver() {
            public Object resolve(String name) throws IOException {
                return CommonsVfsContainer.resolveFile(name);
            }
        };
        try {
            vfsDelegate = new TypeVFSDelegate(resolver);
            vfsDelegate.addVFSDelegate(FileName.class, new CommonsVfsFileNameVFSDelegate(vfsDelegate, CommonsVfsContainer.getFileSystemManager()));
            languageManager = new LanguageManager(vfsDelegate, null);
            languageManager.setClassLoader(loader);
            templateManager = new TemplateManager(vfsDelegate, null);
            templateManager.setClassLoader(loader);
        } catch (BSFException e) {
            throw UtilMisc.initCause(new ContainerException("Initializing StandardFileSystemManager"), e);
        }
        return true;
    }

    public void stop() throws ContainerException {
    }

    public static LanguageManager getLanguageManager() {
        return languageManager;
    }

    public static TemplateManager getTemplateManager() {
        return templateManager;
    }

    public static Object runEvent(String language, String name, String[] paramNames, Class<?>[] paramTypes, Object[] params) throws BSFException, IOException {
        return getLanguageManager().apply(language, "top", 0, 0, CommonsVfsContainer.resolveFile(name), paramNames, params, paramTypes);
    }

    public static String runTemplate(String language, String name, Map<String, Object> context) throws IOException {
        StringWriter writer = new StringWriter();
        runTemplate(language, name, writer, context);
        return writer.toString();
    }

    public static void runTemplate(String language, String name, Writer writer, Map<String, Object> context) throws IOException {
        FileObject file = CommonsVfsContainer.resolveFile(name);
        CompiledTemplate template = getTemplate(language, file);
        template.run(file, writer, context);
    }

    public static CompiledTemplate getTemplate(String language, FileObject file) throws IOException {
        return getTemplateManager().compileTemplate(language, "top", 0, 0, file);
    }
}
