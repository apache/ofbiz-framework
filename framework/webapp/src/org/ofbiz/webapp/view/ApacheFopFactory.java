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
package org.ofbiz.webapp.view;

import org.apache.fop.apps.FopFactory;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;

import java.io.File;

/**
 * Apache FOP Factory used to provide a singleton instance of the FopFactory.  Best pratices recommended
 * the reuse of the factory because of the startup time.
 *
 */

public class ApacheFopFactory {

    public static final String module = ApacheFopFactory.class.getName();
    
    private static final FopFactory fopFactory;

    static {
        // Create the factory
        fopFactory = FopFactory.newInstance();

        // Limit the validation for backwards compatibility
        fopFactory.setStrictValidation(false);
        
        try {
            String fopPath = UtilProperties.getPropertyValue("fop.properties", "fop.path","framework/widget/config");
            File userConfigFile = new File(fopPath + "/fop.xconf");
            fopFactory.setUserConfig(userConfigFile);
            String ofbizHome = System.getProperty("ofbiz.home");
            String fopFontBaseUrl = UtilProperties.getPropertyValue("fop.properties", "fop.font.base.url",
                                    "file://" + ofbizHome + "/framework/widget/config/");
            Debug.log("FOP-FontBaseURL: " + fopFontBaseUrl, module);
            fopFactory.setFontBaseURL(fopFontBaseUrl);
        } catch (Exception e) {
            Debug.logWarning("Error reading FOP configuration", module);
        }
    }

    public static FopFactory instance() {
        return fopFactory;
    }

}
