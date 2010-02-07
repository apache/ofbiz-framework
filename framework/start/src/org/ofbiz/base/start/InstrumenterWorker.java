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
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class InstrumenterWorker {

    public static final void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) != -1) {
            out.write(buf, 0, r);
        }
    }

    public static List<File> instrument(List<File> srcPaths, String instrumenterFileName, String instrumenterClassName) {
        if (instrumenterFileName == null) return srcPaths;
        if (instrumenterClassName == null) return srcPaths;
        Instrumenter instrumenter;
        try {
            List<URL> tmpUrls = new ArrayList<URL>();
            for (File file: srcPaths) {
                tmpUrls.add(file.toURI().toURL());
            }
            ClassLoader tmpLoader = new URLClassLoader(tmpUrls.toArray(new URL[tmpUrls.size()]), InstrumenterWorker.class.getClassLoader());
            instrumenter = (Instrumenter) tmpLoader.loadClass(instrumenterClassName).newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return srcPaths;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return srcPaths;
        } catch (IOException e) {
            e.printStackTrace();
            return srcPaths;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return srcPaths;
        }
        try {
            File instrumenterFile = new File(instrumenterFileName);
            instrumenterFile.delete();
            instrumenter.open(instrumenterFile, true);
            List<File> result = new ArrayList<File>();
            for (File file: srcPaths) {
                String path = file.getPath();
                if (path.matches(".*/ofbiz[^/]*\\.(jar|zip)")) {
                    System.err.println("instrumenting " + path);
                    String prefix = path.substring(0, path.length() - 4);
                    int slash = prefix.lastIndexOf("/");
                    if (slash != -1) prefix = prefix.substring(slash + 1);
                    prefix += "-";
                    File zipTmp = File.createTempFile("instrumented-" + prefix, path.substring(path.length() - 4));
                    try {
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
                        file = zipTmp;
                    } catch (IOException e) {
                        zipTmp.delete();
                        throw e;
                    }
                }
                result.add(file);
            }
            instrumenter.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return srcPaths;
        }
    }
}

