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

package org.ofbiz.base.start;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * Native library class loader. This class is necessary because the
 * bootstrap ClassLoader caches the native library path - so any
 * changes to the library path are ignored (changes that might have
 * been made by loading OFBiz components). 
 */
public class NativeLibClassLoader extends URLClassLoader {

    private final CopyOnWriteArrayList<String> libPaths = new CopyOnWriteArrayList<String>();

    NativeLibClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addNativeClassPath(File path) throws IOException {
        if (path != null) {
            libPaths.addIfAbsent(path.getCanonicalPath());
        }
    }

    public void addNativeClassPath(String path) {
        if (path != null) {
            StringTokenizer t = new StringTokenizer(path, File.pathSeparator);
            while (t.hasMoreTokens()) {
                libPaths.addIfAbsent(t.nextToken());
            }
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    protected String findLibrary(String libname) {
        String libFileName = System.mapLibraryName(libname);
        for (String path : libPaths) {
            File libFile = new File(path, libFileName);
            if (libFile.exists()) {
                return libFile.getAbsolutePath();
            }
        }
        return null;
    }

    public List<String> getNativeLibPaths() {
        return new ArrayList<String>(libPaths);
    }
}
