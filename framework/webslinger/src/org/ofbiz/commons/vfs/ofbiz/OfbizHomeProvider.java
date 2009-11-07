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
package org.ofbiz.commons.vfs.ofbiz;

import java.net.URL;
import java.util.Collection;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.AbstractFileProvider;
import org.apache.commons.vfs.provider.local.DefaultLocalFileProvider;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.UtilMisc;

import org.webslinger.commons.vfs.VFSUtil;

public class OfbizHomeProvider extends AbstractFileProvider {
    public Collection<?> getCapabilities() {
        return DefaultLocalFileProvider.capabilities;
    }

    public FileObject findFile(FileObject base, String name, FileSystemOptions properties) throws FileSystemException {
        //new Exception("findFile(" + base + ", " + name + ")").printStackTrace();
        try {
            URL location = FlexibleLocation.resolveLocation("ofbizhome://.");
            FileObject ofbizBase = getContext().resolveFile(location.toString(), properties);
            return VFSUtil.toFileObject(ofbizBase.getFileSystem().getFileSystemManager(), ofbizBase.resolveFile(name.substring(13)).getURL().toString(), properties);
        } catch (Exception e) {
            throw UtilMisc.initCause(new FileSystemException(e.getMessage(), null, e), e);
        }
    }
}
