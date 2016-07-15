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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A class path accumulator.
 * <p>You can build a class path by repeatedly calling the addXxx methods,
 * then use the getXxx methods to get the accumulated class path.</p>
 */
public final class Classpath {

    private static final String nativeLibExt = System.mapLibraryName("someLib").replace("someLib", "").toLowerCase();
    private List<File> elements = new ArrayList<File>();
    private final List<File> nativeFolders = new ArrayList<File>();

    /**
     * Default constructor.
     */
    public Classpath() {
    }

    /**
     * Adds a class path string. The string may include multiple paths separated by
     * a path separator.
     * 
     * @param path
     * @return <code>true</code> if any path elements were added
     * @throws IOException if there was a problem parsing the class path
     * @throws IllegalArgumentException if <code>path</code> is null or empty
     */
    public boolean addClassPath(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path cannot be null or empty");
        }
        boolean added = false;
        StringTokenizer t = new StringTokenizer(path, File.pathSeparator);
        while (t.hasMoreTokens()) {
            added |= addComponent(t.nextToken());
        }
        return added;
    }

    /**
     * Adds a class path component. The component may be a directory or a file.
     * If <code>component</code> does not exist, the method does nothing.
     * 
     * @param component The class path component to add
     * @return <code>true</code> if the component was added
     * @throws IOException if there was a problem parsing the component
     * @throws IllegalArgumentException if <code>component</code> is null
     */
    public boolean addComponent(File component) throws IOException {
        if (component == null) {
            throw new IllegalArgumentException("component cannot be null");
        }
        if (component.exists()) {
            File key = component.getCanonicalFile();
            synchronized (elements) {
                if (!elements.contains(key)) {
                    elements.add(key);
                    return true;
                }
            }
        } else {
            System.out.println("Warning : Module classpath component '" + component + "' is not valid and will be ignored...");
        }
        return false;
    }

    /**
     * Adds a class path component. The component may be a directory or a file.
     * If <code>component</code> does not exist, the method does nothing.
     * 
     * @param component The class path component to add
     * @return <code>true</code> if the component was added
     * @throws IOException if there was a problem parsing the component
     * @throws IllegalArgumentException if <code>component</code> is null or empty
     */
    public boolean addComponent(String component) throws IOException {
        if (component == null || component.isEmpty()) {
            throw new IllegalArgumentException("component cannot be null or empty");
        }
        return addComponent(new File(component));
    }

    /**
     * Scans a directory and adds all files ending with ".jar" or ".zip" to
     * the class path.
     * If <code>path</code> is not a directory, the method does nothing.
     * 
     * @param path the directory to scan
     * @throws IOException if there was a problem processing the directory
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public void addFilesFromPath(File path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (path.isDirectory() && path.exists()) {
            // load all .jar, .zip files and native libs in this directory
            boolean containsNativeLibs = false;
            for (File file : path.listFiles()) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                    File key = file.getCanonicalFile();
                    synchronized (elements) {
                        if (!elements.contains(key)) {
                            elements.add(key);
                        }
                    }
                } else if (fileName.endsWith(nativeLibExt)) {
                    containsNativeLibs = true;
                }
            }
            if (containsNativeLibs) {
                File key = path.getCanonicalFile();
                synchronized (nativeFolders) {
                    if (!nativeFolders.contains(key)) {
                        nativeFolders.add(key);
                    }
                }
            }
        } else {
            System.out.println("Warning : Module classpath component '" + path + "' is not valid and will be ignored...");
        }
    }

    /**
     * Adds a directory that contains native libraries.
     * If <code>path</code> does not exist, the method does nothing.
     * 
     * @param path
     * @return
     * @throws IOException
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public boolean addNativeClassPath(File path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (path.exists()) {
            File key = path.getCanonicalFile();
            synchronized (nativeFolders) {
                if (!nativeFolders.contains(key)) {
                    nativeFolders.add(key);
                    return true;
                }
            }
        } else {
            System.out.println("Warning : Module classpath component '" + path + "' is not valid and will be ignored...");
        }
        return false;
    }

    private void appendPath(StringBuilder cp, String path) {
        if (path.indexOf(' ') >= 0) {
            cp.append('\"');
            cp.append(path);
            cp.append('"');
        } else {
            cp.append(path);
        }
    }

    /**
     * Returns a list of folders containing native libraries.
     * 
     * @return A list of folders containing native libraries
     */
    public List<File> getNativeFolders() {
        synchronized (nativeFolders) {
            return new ArrayList<File>(nativeFolders);
        }
    }

    /**
     * Returns a list of class path component URLs.
     * 
     * @return A list of class path component URLs
     * @throws MalformedURLException
     */
    public URL[] getUrls() throws MalformedURLException {
        synchronized (elements) {
            int cnt = elements.size();
            URL[] urls = new URL[cnt];
            for (int i = 0; i < cnt; i++) {
                urls[i] = elements.get(i).toURI().toURL();
            }
            return urls;
        }
    }

    @Override
    public String toString() {
        StringBuilder cp = new StringBuilder(1024);
        synchronized (elements) {
            int cnt = elements.size();
            if (cnt >= 1) {
                cp.append(elements.get(0).getPath());
            }
            for (int i = 1; i < cnt; i++) {
                cp.append(File.pathSeparatorChar);
                appendPath(cp, elements.get(i).getPath());
            }
        }
        return cp.toString();
    }
}
