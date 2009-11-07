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
package org.ofbiz.commons.vfs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.UtilMisc;
import org.webslinger.commons.vfs.VFSUtil;

public class CommonsVfsContainer implements Container {
    private static StandardFileSystemManager sfsm;

    public void init(String[] args, String configFile) throws ContainerException {
    }

    public boolean start() throws ContainerException {
        try {
            StandardFileSystemManager sfsm = VFSUtil.createStandardFileSystemManager();
            FileObject currentDir = sfsm.resolveFile(new File(".").toURI().toURL().toString());
            sfsm.setBaseFile(currentDir);
            CommonsVfsContainer.sfsm = sfsm;
        } catch (FileSystemException e) {
            throw UtilMisc.initCause(new ContainerException("Initializing StandardFileSystemManager"), e);
        } catch (MalformedURLException e) {
            throw UtilMisc.initCause(new ContainerException("Initializing StandardFileSystemManager"), e);
        }
        return true;
    }

    public void stop() throws ContainerException {
        sfsm.close();
        sfsm = null;
    }

    public static FileObject resolveFile(String uri) throws IOException {
        return sfsm.resolveFile(uri);
    }

    public static FileSystemManager getFileSystemManager() {
        return sfsm;
    }
}
