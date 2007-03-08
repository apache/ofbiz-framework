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
package org.ofbiz.widget;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

/**
 * ContentWorkerInterface
 */
public interface ContentWorkerInterface {

    // helper methods
    public GenericValue getCurrentContentExt(GenericDelegator delegator, List trail, GenericValue userLogin, Map ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId)  throws GeneralException;
    public GenericValue getWebSitePublishPointExt(GenericDelegator delegator, String contentId, boolean ignoreCache) throws GenericEntityException;
    public String getMimeTypeIdExt(GenericDelegator delegator, GenericValue view, Map ctx);

    // new rendering methods
    public void renderContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Writer out, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;
    public String renderContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;

    public void renderSubContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Writer out, String mapKey, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;
    public String renderSubContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, String mapKey, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;            
}
