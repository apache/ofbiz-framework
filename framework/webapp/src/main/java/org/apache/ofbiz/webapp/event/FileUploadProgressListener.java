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
package org.apache.ofbiz.webapp.event;

import java.io.Serializable;

import org.apache.commons.fileupload.ProgressListener;

/**
 * FileUploadProgressListener - Commons FileUpload progress listener
 */
@SuppressWarnings("serial")
public class FileUploadProgressListener implements ProgressListener, Serializable {

    private static final String MODULE = FileUploadProgressListener.class.getName();

    private long contentLength = -1;
    private long bytesRead = -1;
    private int items = -1;
    private boolean hasStarted = false;

    @Override
    public void update(long bytesRead, long contentLength, int items) {
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
        this.items = items;
        if (!hasStarted) {
            hasStarted = true;
        }
    }

    /**
     * Gets content length.
     * @return the content length
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * Gets bytes read.
     * @return the bytes read
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * Gets items.
     * @return the items
     */
    public int getItems() {
        return items;
    }

    /**
     * Has started boolean.
     * @return the boolean
     */
    public boolean hasStarted() {
        return hasStarted;
    }
}
