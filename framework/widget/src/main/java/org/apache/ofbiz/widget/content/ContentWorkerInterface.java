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
package org.apache.ofbiz.widget.content;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * ContentWorkerInterface
 */
public interface ContentWorkerInterface {

    // helper methods
    public GenericValue getCurrentContentExt(Delegator delegator, List<Map<String, ? extends Object>> trail, GenericValue userLogin, Map<String, Object> ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId)  throws GeneralException;
    public GenericValue getWebSitePublishPointExt(Delegator delegator, String contentId, boolean ignoreCache) throws GenericEntityException;
    public String getMimeTypeIdExt(Delegator delegator, GenericValue view, Map<String, Object> ctx);

    // new rendering methods
    public void renderContentAsTextExt(LocalDispatcher dispatcher, String contentId, Appendable out, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;
    public String renderContentAsTextExt(LocalDispatcher dispatcher, String contentId, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;

    public void renderSubContentAsTextExt(LocalDispatcher dispatcher, String contentId, Appendable out, String mapKey, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;
    public String renderSubContentAsTextExt(LocalDispatcher dispatcher, String contentId, String mapKey, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException;
}
