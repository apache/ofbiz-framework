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
package org.ofbiz.base.component;

/**
 * Component Already Loaded Exception
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
 */
public class AlreadyLoadedException extends ComponentException {

    public AlreadyLoadedException() {
        super();
    }

    public AlreadyLoadedException(String str) {
        super(str);
    }

    public AlreadyLoadedException(Throwable nested) {
        super(nested);
    }

    public AlreadyLoadedException(String str, Throwable nested) {
        super(str, nested);
    }
}

