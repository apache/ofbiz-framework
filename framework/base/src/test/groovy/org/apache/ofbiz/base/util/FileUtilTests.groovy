/*
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
 */
package org.apache.ofbiz.base.util;

import org.apache.commons.io.FileUtils
import org.junit.Test

public class FileUtilTests {
    /**
     * Test FileUtil zipFileStream and unzipFileToFolder methods, using README.adoc
     */
    @Test
    void zipReadme() {
        String zipFilePath = UtilProperties.getPropertyValue("general", "http.upload.tmprepository", "runtime/tmp")
        String zipName = 'README.adoc.zip'
        String fileName = 'README.adoc'
        File originalReadme = new File(fileName)

        //validate zipStream from README.adoc is not null
        def zipStream = FileUtil.zipFileStream(originalReadme.newInputStream(), fileName)
        assert zipStream

        //ensure no zip already exists
        File readmeZipped = new File(zipFilePath, zipName)
        if (readmeZipped.exists()) readmeZipped.delete()

        //write it down into tmp folder
        OutputStream out = new FileOutputStream(readmeZipped)
        byte[] buf = new byte[8192]
        int len
        while ((len = zipStream.read(buf)) > 0) {
            out.write(buf, 0, len)
        }
        out.close()
        zipStream.close()

        //ensure no README.adoc exist in tmp folder
        File readme = new File(zipFilePath, fileName)
        if (readme.exists()) readme.delete()

        //validate unzip and compare the two files
        FileUtil.unzipFileToFolder(readmeZipped, zipFilePath)

        assert FileUtils.contentEquals(originalReadme, new File(zipFilePath, fileName))
    }
}
