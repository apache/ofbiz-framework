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
package org.ofbiz.webtools.artifactinfo;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.webtools.artifactinfo.ArtifactInfoFactory;
import org.ofbiz.widget.screen.ModelScreen;
import org.xml.sax.SAXException;

/**
 *
 */
public class ScreenWidgetArtifactInfo {
    protected ArtifactInfoFactory aif;
    protected ModelScreen modelScreen;
    
    public ScreenWidgetArtifactInfo(String screenName, String screenLocation, ArtifactInfoFactory aif) throws ParserConfigurationException, SAXException, IOException {
        this.aif = aif;
        this.modelScreen = aif.getModelScreen(screenName, screenLocation);
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ScreenWidgetArtifactInfo) {
            return (this.modelScreen.getName().equals(((ScreenWidgetArtifactInfo) obj).modelScreen.getName()) && 
                    this.modelScreen.getSourceLocation().equals(((ScreenWidgetArtifactInfo) obj).modelScreen.getSourceLocation()));
        } else {
            return false;
        }
    }
}
