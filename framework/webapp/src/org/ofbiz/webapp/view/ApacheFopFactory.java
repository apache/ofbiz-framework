/*
 * Copyright 2006 The Apache Software Foundation
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
package org.ofbiz.webapp.view;

import org.apache.fop.apps.FopFactory;

/**
 * Apache FOP Factory used to provide a singleton instance of the FopFactory.  Best pratices recommended
 * the reuse of the factory because of the startup time.
 *
 */

public class ApacheFopFactory {

    private static final FopFactory fopFactory;

    static {
        // Create the factory
        fopFactory = FopFactory.newInstance();

        // Limit the validation for backwards compatibility
        fopFactory.setStrictValidation(false);
    }

    public static FopFactory instance() {
        return fopFactory;
    }

}
