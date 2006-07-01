// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: Classpath.java 5462 2005-08-05 18:35:48Z jonesde $
// ========================================================================

//package org.mortbay.start;
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
 * @author Jan Hlavat
 */
public class Classpath {

    private List _elements = new ArrayList();

    public Classpath() {}

    public Classpath(String initial) {
        addClasspath(initial);
    }

    public boolean addComponent(String component) {
        if ((component != null) && (component.length() > 0)) {
            try {
                File f = new File(component);
                if (f.exists()) {
                    File key = f.getCanonicalFile();
                    if (!_elements.contains(key)) {
                        _elements.add(key);
                        return true;
                    }
                }
            } catch (IOException e) {}
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

    public String toString() {
        StringBuffer cp = new StringBuffer(1024);
        int cnt = _elements.size();
        if (cnt >= 1) {
            cp.append(((File) (_elements.get(0))).getPath());
        }
        for (int i = 1; i < cnt; i++) {
            cp.append(File.pathSeparatorChar);
            cp.append(((File) (_elements.get(i))).getPath());
        }
        return cp.toString();
    }

    public URL[] getUrls() {
        int cnt = _elements.size();
        URL[] urls = new URL[cnt];
        for (int i = 0; i < cnt; i++) {
            try {
                urls[i] = ((File) (_elements.get(i))).toURL();
            } catch (MalformedURLException e) {}
        }
        return urls;
    }

    public ClassLoader getClassLoader() {
        URL[] urls = getUrls();

        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = Classpath.class.getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        return new URLClassLoader(urls, parent);
    }

    public List getElements() {
        return _elements;
    }
}
