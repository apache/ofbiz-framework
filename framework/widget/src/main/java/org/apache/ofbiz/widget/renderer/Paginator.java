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
package org.apache.ofbiz.widget.renderer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.MapUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.PagedList;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.ModelForm;

/**
 * Utility methods for handling list pagination.
 *
 */
public final class Paginator {

    public static final String module = Paginator.class.getName();

    public static int getActualPageSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("actualPageSize");
        return value != null ? value.intValue() : (getHighIndex(context) - getLowIndex(context));
    }

    public static int getHighIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("highIndex");
        return value != null ? value.intValue() : 0;
    }

    // entryList might be an  EntityListIterator. It will then be closed at the end of FormRenderer.renderItemRows()
    public static void getListLimits(ModelForm modelForm, Map<String, Object> context, Object entryList) {
        int viewIndex = 0;
        int viewSize = 0;
        int lowIndex = 0;
        int highIndex = 0;
        int listSize = modelForm.getOverrideListSize(context);
        if (listSize > 0) {
        } else if (entryList instanceof EntityListIterator) {
            EntityListIterator iter = (EntityListIterator) entryList;
            try {
                listSize = iter.getResultsSizeAfterPartialList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting list size", module);
                listSize = 0;
            }
        } else if (entryList instanceof List<?>) {
            List<?> items = (List<?>) entryList;
            listSize = items.size();
            if(context.containsKey("result")){
                Map<String, Object> resultMap = UtilGenerics.checkMap(context.get("result"));
                if(resultMap.containsKey("listSize")){
                    listSize = (int)resultMap.get("listSize");
                }
            }
        } else if (entryList instanceof PagedList) {
            PagedList<?> pagedList = (PagedList<?>) entryList;
            listSize = pagedList.getSize();
        }
        if (modelForm.getPaginate(context)) {
            viewIndex = getViewIndex(modelForm, context);
            viewSize = getViewSize(modelForm, context);
            lowIndex = viewIndex * viewSize;
            highIndex = (viewIndex + 1) * viewSize;
        } else {
            viewIndex = 0;
            viewSize = ModelForm.MAX_PAGE_SIZE;
            lowIndex = 0;
            highIndex = ModelForm.MAX_PAGE_SIZE;
        }
        context.put("listSize", Integer.valueOf(listSize));
        context.put("viewIndex", Integer.valueOf(viewIndex));
        context.put("viewSize", Integer.valueOf(viewSize));
        context.put("lowIndex", Integer.valueOf(lowIndex));
        context.put("highIndex", Integer.valueOf(highIndex));
    }

    public static int getListSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("listSize");
        return value != null ? value.intValue() : 0;
    }

    public static int getLowIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("lowIndex");
        return value != null ? value.intValue() : 0;
    }

    public static int getViewIndex(ModelForm modelForm, Map<String, Object> context) {
        String field = modelForm.getMultiPaginateIndexField(context);
        int viewIndex = 0;
        try {
            Object value = context.get(field);
            if (value == null) {
                // try parameters.VIEW_INDEX as that is an old OFBiz convention
                Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get("VIEW_INDEX" + "_" + WidgetWorker.getPaginatorNumber(context));

                    if (value == null) {
                        value = parameters.get(field);
                    }
                }
            }
            // try paginate index field without paginator number
            if (value == null) {
                field = modelForm.getPaginateIndexField(context);
                value = context.get(field);
            }
            if (value instanceof Integer) {
                viewIndex = ((Integer) value).intValue();
            } else if (value instanceof String) {
                viewIndex = Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting paginate view index: " + e.toString(), module);
        }
        return viewIndex;
    }

    public static int getViewSize(ModelForm modelForm, Map<String, Object> context) {
        String field = modelForm.getMultiPaginateSizeField(context);
        int viewSize = modelForm.getDefaultViewSize();
        try {
            Object value = context.get(field);
            if (value == null) {
                // try parameters.VIEW_SIZE as that is an old OFBiz convention
                Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get("VIEW_SIZE" + "_" + WidgetWorker.getPaginatorNumber(context));

                    if (value == null) {
                        value = parameters.get(field);
                    }
                }
            }
            // try the page size field without paginator number
            if (value == null) {
                field = modelForm.getPaginateSizeField(context);
                value = context.get(field);
            }
            if (value instanceof Integer) {
                viewSize = ((Integer) value).intValue();
            } else if (value instanceof String && UtilValidate.isNotEmpty(value)) {
                viewSize = Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting paginate view size: " + e.toString(), module);
        }
        return viewSize;
    }

    public static void preparePager(ModelForm modelForm, Map<String, Object> context) {

        String lookupName = modelForm.getListName();
        if (UtilValidate.isEmpty(lookupName)) {
            Debug.logError("No value for list or iterator name found.", module);
            return;
        }
        Object obj = context.get(lookupName);
        if (obj == null) {
            if (Debug.verboseOn()) {
                 Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not running pagination.", module);
            }
            return;
        }
        // if list is empty, do not render rows
        Iterator<?> iter = null;
        if (obj instanceof Iterator<?>) {
            iter = (Iterator<?>) obj;
        } else if (obj instanceof List<?>) {
            iter = ((List<?>) obj).listIterator();
        } else if (obj instanceof PagedList<?>) {
            iter = ((PagedList<?>) obj).iterator();
        }

        // set low and high index
        getListLimits(modelForm, context, obj);

        int listSize = ((Integer) context.get("listSize")).intValue();
        int lowIndex = ((Integer) context.get("lowIndex")).intValue();
        int highIndex = ((Integer) context.get("highIndex")).intValue();

        // we're passed a subset of the list, so use (0, viewSize) range
        if (modelForm.isOverridenListSize()) {
            lowIndex = 0;
            highIndex = ((Integer) context.get("viewSize")).intValue();
        }

        if (iter == null) {
            return;
        }

        // count item rows
        int itemIndex = -1;
        Object item = safeNext(iter);
        while (item != null && itemIndex < highIndex) {
            itemIndex++;
            item = safeNext(iter);
        }

        // reduce the highIndex if number of items falls short
        if ((itemIndex + 1) < highIndex) {
            highIndex = itemIndex + 1;
            // if list size is overridden, use full listSize
            context.put("highIndex", Integer.valueOf(modelForm.isOverridenListSize() ? listSize : highIndex));
        }
        context.put("actualPageSize", Integer.valueOf(highIndex - lowIndex));

        if (iter instanceof EntityListIterator) {
            // The EntityListIterator will be closed at the end of FormRenderer.renderItemRows()
            // Note: it's also used in MacroScreenRenderer.renderScreenletPaginateMenu() but I could not find where it's then closed, nor issues...
            try {
                ((EntityListIterator) iter).beforeFirst();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error rewinding list form render EntityListIterator: " + e.toString(), module);
            }
        }
    }

    private static <X> X safeNext(Iterator<X> iterator) {
        try {
            return iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * @param context Map
     * @param viewIndexName
     * @return value of viewIndexName in context map (as an int) or return 0 as default
     */
    public static Integer getViewIndex(final Map<String, ? extends Object> context, final String viewIndexName) {
        return getViewIndex(context, viewIndexName, 0);
    }

    /**
     * @param context
     * @param viewIndexName
     * @param defaultValue
     * @return value of viewIndexName in context map (as an int) or return defaultValue
     */
    public static Integer getViewIndex(final Map<String, ? extends Object> context, final String viewIndexName, final int defaultValue) {
        return MapUtils.getInteger(context, viewIndexName, defaultValue);
    }

    /**
     * @param context
     * @param viewSizeName
     * @return value of viewSizeName in context map (as an int) or return
     *         default value from widget.properties
     */
    public static Integer getViewSize(Map<String, ? extends Object> context, String viewSizeName) {
        int defaultSize = UtilProperties.getPropertyAsInteger("widget", "widget.form.defaultViewSize", 20);
        if (context.containsKey(viewSizeName)) {
            return MapUtils.getInteger(context, viewSizeName, defaultSize);
        }
        return defaultSize;
    }

}
