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

import org.apache.commons.fileupload.ProgressListener;

import java.io.Serializable;

/**
 * FileUploadProgressListener - Commons FileUpload progress listener
 */
@SuppressWarnings("serial")
public class FileUploadProgressListener implements ProgressListener, Serializable {

    public static final String module = FileUploadProgressListener.class.getName();

    protected long contentLength = -1;
    protected long bytesRead = -1;
    protected int items = -1;
    protected boolean hasStarted = false;

    public void update(long bytesRead, long contentLength, int items) {
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
        this.items = items;
        if (!hasStarted) {
            hasStarted = true;
        }
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public int getItems() {
        return items;
    }

    public boolean hasStarted() {
        return hasStarted;
    }
}
