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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/*
 * Instrumenting class loader. 
 */
final class InstrumentingClassLoader extends NativeLibClassLoader {

    private static final void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) != -1) {
            out.write(buf, 0, r);
        }
    }

    private final Instrumenter instrumenter;

    InstrumentingClassLoader(URL[] urls, ClassLoader parent, String instrumenterFileName, String instrumenterClassName)
            throws Exception {
        super(new URL[0], parent);
        URLClassLoader tmpLoader = new URLClassLoader(urls, InstrumenterWorker.class.getClassLoader());
        try {
            instrumenter = (Instrumenter) tmpLoader.loadClass(instrumenterClassName).newInstance();
        } finally {
            tmpLoader.close();
        }
        File instrumenterFile = new File(instrumenterFileName);
        instrumenterFile.delete();
        instrumenter.open(instrumenterFile, true);
        System.out.println("Instrumenter file opened");
        for (URL url : urls) {
            addURL(url);
        }
    }

    @Override
    public void addURL(URL url) {
        File file;
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            file = new File(url.getPath());
        }
        String path = file.getPath();
        if (path.matches(".*/ofbiz[^/]*\\.(jar|zip)")) {
            String prefix = path.substring(0, path.length() - 4);
            int slash = prefix.lastIndexOf("/");
            if (slash != -1)
                prefix = prefix.substring(slash + 1);
            prefix += "-";
            File zipTmp = null;
            try {
                zipTmp = File.createTempFile("instrumented-" + prefix, path.substring(path.length() - 4));
                zipTmp.deleteOnExit();
                ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
                ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipTmp));
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    InputStream in;
                    long size;
                    if (entry.getName().endsWith(".class")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        copy(zin, baos);
                        byte[] bytes = instrumenter.instrumentClass(baos.toByteArray());
                        size = bytes.length;
                        in = new ByteArrayInputStream(bytes);
                    } else {
                        in = zin;
                        size = entry.getSize();
                    }
                    ZipEntry newEntry = new ZipEntry(entry);
                    newEntry.setSize(size);
                    newEntry.setCompressedSize(-1);
                    zout.putNextEntry(newEntry);
                    copy(in, zout);
                    if (entry.getName().endsWith(".class")) {
                        in.close();
                    }
                }
                zout.close();
                System.out.println("Instrumented file: " + zipTmp.getCanonicalPath());
                super.addURL(zipTmp.toURI().toURL());
            } catch (IOException e) {
                System.err.println("Exception thrown while instrumenting " + file + ": ");
                e.printStackTrace(System.err);
                if (zipTmp != null) {
                    zipTmp.delete();
                }
            }
        } else {
            super.addURL(url);
        }
    }

    void closeInstrumenter() throws IOException {
        instrumenter.close();
    }
}
