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
package org.ofbiz.minilang.method;

import org.w3c.dom.*;

import org.ofbiz.minilang.*;

/**
 * A single operation, does the specified operation on the given field
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public abstract class MethodOperation {
    
    protected SimpleMethod simpleMethod;

    public MethodOperation(Element element, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;
    }

    /** Execute the operation; if false is returned then no further operations will be executed */
    public abstract boolean exec(MethodContext methodContext);

    /** Create a raw string representation of the operation, would be similar to original XML */
    public abstract String rawString();
    /** Create an expanded string representation of the operation, is for the current context */
    public abstract String expandedString(MethodContext methodContext);
}
