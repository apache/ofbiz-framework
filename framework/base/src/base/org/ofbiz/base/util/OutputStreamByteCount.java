/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.base.util;

import java.io.IOException;
import java.io.OutputStream;


public class OutputStreamByteCount extends OutputStream {

    protected long byteCount = 0;

    public OutputStreamByteCount() {
        super();
    }

    /**
     * @see java.io.OutputStream#write(int)
     */
    public void write(int arg0) throws IOException {
        byteCount++;
    }
    
    public long getByteCount() {
        return this.byteCount;
    }
}
