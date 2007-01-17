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
package org.ofbiz.webapp.ftl;

import java.io.IOException;
import java.io.Writer;

import freemarker.template.TemplateModelException;
import freemarker.template.TransformControl;

public class LoopWriter extends Writer implements TransformControl {

    public LoopWriter(Writer out) {
    }

    public int onStart() throws TemplateModelException, IOException {  
        return TransformControl.EVALUATE_BODY;
    }

    public int afterBody() throws TemplateModelException, IOException {  
        return TransformControl.END_EVALUATION;
    }

    public void onError(Throwable t) throws Throwable {
        throw t;
    }

    public void close() throws IOException {  
    }

    public void write(char cbuf[], int off, int len) {
    }

    public void flush() throws IOException {
    }

}
