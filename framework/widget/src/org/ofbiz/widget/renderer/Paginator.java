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
package org.ofbiz.widget.renderer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.model.ModelForm;

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

    public static void getListLimits(ModelForm modelForm, Map<String, Object> context, Object entryList) {
        int viewIndex = 0;
        int viewSize = 0;
        int lowIndex = 0;
        int highIndex = 0;
        int listSize = modelForm.getOverrideListSize(context);
        if (listSize > 0) {
            //setOverridenListSize(true);
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
            if (Debug.verboseOn())
                Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not running pagination.",
                        module);
            return;
        }
        // if list is empty, do not render rows
        Iterator<?> iter = null;
        if (obj instanceof Iterator<?>) {
            iter = (Iterator<?>) obj;
        } else if (obj instanceof List<?>) {
            iter = ((List<?>) obj).listIterator();
        }

        // set low and high index
        getListLimits(modelForm, context, obj);

        int listSize = ((Integer) context.get("listSize")).intValue();
        int lowIndex = ((Integer) context.get("lowIndex")).intValue();
        int highIndex = ((Integer) context.get("highIndex")).intValue();
        // Debug.logInfo("preparePager: low - high = " + lowIndex + " - " + highIndex, module);

        // we're passed a subset of the list, so use (0, viewSize) range
        if (modelForm.isOverridenListSize()) {
            lowIndex = 0;
            highIndex = ((Integer) context.get("viewSize")).intValue();
        }

        if (iter == null)
            return;

        // count item rows
        int itemIndex = -1;
        Object item = safeNext(iter);
        while (item != null && itemIndex < highIndex) {
            itemIndex++;
            item = safeNext(iter);
        }

        // Debug.logInfo("preparePager: Found rows = " + itemIndex, module);

        // reduce the highIndex if number of items falls short
        if ((itemIndex + 1) < highIndex) {
            highIndex = itemIndex + 1;
            // if list size is overridden, use full listSize
            context.put("highIndex", Integer.valueOf(modelForm.isOverridenListSize() ? listSize : highIndex));
        }
        context.put("actualPageSize", Integer.valueOf(highIndex - lowIndex));

        if (iter instanceof EntityListIterator) {
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
}
