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

// NOTE: Portions Originally Copyright 2002 Mort Bay Consulting (Australia) Pty. Ltd. (this was taken from a code base released under the Apache License, though no header was on this file)

package org.ofbiz.base.start;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class to handle CLASSPATH construction
 */
public class Classpath {

    private List<File> _elements = new ArrayList<File>();

    public Classpath() {}

    public Classpath(String initial) {
        addClasspath(initial);
    }

    public boolean addComponent(String component) {
        if ((component != null) && (component.length() > 0)) {
            return addComponent(new File(component));
        }
        return false;
    }

    public boolean addComponent(File component) {
        if (component != null) {
            try {
                if (component.exists()) {
                    File key = component.getCanonicalFile();
                    if (!_elements.contains(key)) {
                        _elements.add(key);
                        return true;
                    }
                }
            } catch (IOException e) {}
        }
        return false;
    }

    public boolean addClasspath(String s) {
        boolean added = false;
        if (s != null) {
            StringTokenizer t = new StringTokenizer(s, File.pathSeparator);
            while (t.hasMoreTokens()) {
                added |= addComponent(t.nextToken());
            }
        }
        return added;
    }


    private void appendPath(StringBuilder cp, String path) {
        if (path.indexOf(' ') >= 0) {
            cp.append('\"');
            cp.append(path);
            cp.append('"');
        }
        else {
            cp.append(path);
        }
     }

    public void instrument(String instrumenterFile, String instrumenterClassName) {
        _elements = InstrumenterWorker.instrument(_elements, instrumenterFile, instrumenterClassName);
    }

    @Override
    public String toString() {
        StringBuilder cp = new StringBuilder(1024);
        int cnt = _elements.size();
        if (cnt >= 1) {
            cp.append(_elements.get(0).getPath());
        }
        for (int i = 1; i < cnt; i++) {
            cp.append(File.pathSeparatorChar);
            appendPath(cp, _elements.get(i).getPath());
        }
        return cp.toString();
    }

    public URL[] getUrls() {
        int cnt = _elements.size();
        URL[] urls = new URL[cnt];
        for (int i = 0; i < cnt; i++) {
            try {
                urls[i] = _elements.get(i).toURI().toURL();
            } catch (MalformedURLException e) {
                // note: this is printing right to the console because at this point we don't have the rest of the system up, not even the logging stuff
                System.out.println("Error adding classpath entry: " + e.toString());
                e.printStackTrace();
            }
        }
        return urls;
    }

    public ClassLoader getClassLoader() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = Classpath.class.getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        return getClassLoader(parent);
    }

    public ClassLoader getClassLoader(ClassLoader parent) {
        return new NativeLibClassLoader(getUrls(), parent);
    }

    public List<File> getElements() {
        return _elements;
    }

    /*
     * Native library class loader. This class is necessary because the
     * bootstrap ClassLoader caches the native library path - so any
     * changes to the library path are ignored (changes that might have
     * been made by loading OFBiz components). 
     */
    private class NativeLibClassLoader extends URLClassLoader {

        private NativeLibClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        @Override
        protected String findLibrary(String libname) {
            String[] libPaths = System.getProperty("java.library.path").split(File.pathSeparator);
            String libFileName = System.mapLibraryName(libname);
            for (String path : libPaths) {
                File libFile = new File(path, libFileName);
                if (libFile.exists()) {
                    return libFile.getAbsolutePath();
                }
            }
            return null;
        }
    }
}
