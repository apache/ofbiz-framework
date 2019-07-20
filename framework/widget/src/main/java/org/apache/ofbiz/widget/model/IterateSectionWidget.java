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
package org.apache.ofbiz.widget.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.w3c.dom.Element;


/**
 * Widget Library - Screen model HTML class
 */
@SuppressWarnings("serial")
public class IterateSectionWidget extends ModelScreenWidget {

    public static final String module = IterateSectionWidget.class.getName();
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10000;

    private final List<ModelScreenWidget.Section> sectionList;
    private final FlexibleMapAccessor<Object> listNameExdr;
    private final FlexibleStringExpander entryNameExdr;
    private final FlexibleStringExpander keyNameExdr;
    private final FlexibleStringExpander paginateTarget;
    private final FlexibleStringExpander paginate;
    private final int viewSize;

    public IterateSectionWidget(ModelScreen modelScreen, Element iterateSectionElement) {
        super(modelScreen, iterateSectionElement);
        String listName = iterateSectionElement.getAttribute("list");
        if (listName.isEmpty()) {
            listName = iterateSectionElement.getAttribute("list-name");
        }
        this.listNameExdr = FlexibleMapAccessor.getInstance(listName);
        String entryName = iterateSectionElement.getAttribute("entry");
        if (entryName.isEmpty()) {
            entryName = iterateSectionElement.getAttribute("entry-name");
        }
        this.entryNameExdr = FlexibleStringExpander.getInstance(entryName);
        String keyName = iterateSectionElement.getAttribute("key");
        if (keyName.isEmpty()) {
            keyName = iterateSectionElement.getAttribute("key-name");
        }
        this.keyNameExdr = FlexibleStringExpander.getInstance(keyName);
        this.paginateTarget = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("paginate-target"));
        this.paginate = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("paginate"));
        int viewSize = DEFAULT_PAGE_SIZE;
        String viewSizeStr = iterateSectionElement.getAttribute("view-size");
        if (!viewSizeStr.isEmpty()) {
            viewSize = Integer.parseInt(viewSizeStr);
        }
        this.viewSize = viewSize;
        List<? extends Element> childElementList = UtilXml.childElementList(iterateSectionElement);
        if (childElementList.isEmpty()) {
            this.sectionList = Collections.emptyList();
        } else {
            List<ModelScreenWidget.Section> sectionList = new ArrayList<>(childElementList.size());
            for (Element sectionElement: childElementList) {
                ModelScreenWidget.Section section = new ModelScreenWidget.Section(modelScreen, sectionElement, false);
                sectionList.add(section);
            }
            this.sectionList = Collections.unmodifiableList(sectionList);
        }
    }

    public List<ModelScreenWidget.Section> getSectionList() {
        return sectionList;
    }

    @Override
    public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
        int viewIndex = 0;
        int locViewSize = this.viewSize;
        int lowIndex = -1;
        int highIndex = -1;
        int listSize = 0;
        int actualPageSize = 0;

        boolean isEntrySet = false;
        // create a standAloneStack, basically a "save point" for this SectionsRenderer, and make a new "screens" object just for it so it is isolated and doesn't follow the stack down
        MapStack<String> contextMs = MapStack.create(context);

        String entryName = this.entryNameExdr.expandString(context);
        String keyName = this.keyNameExdr.expandString(context);
        Object obj = listNameExdr.get(context);
        if (obj == null) {
            Debug.logError("No object found for listName:" + listNameExdr.toString(), module);
            return;
        }
        List<?> theList = null;
        if (obj instanceof Map<?, ?>) {
            Set<Map.Entry<String, Object>> entrySet = UtilGenerics.<Map<String, Object>>cast(obj).entrySet();
            Object [] a = entrySet.toArray();
            theList = Arrays.asList(a);
            isEntrySet = true;
        } else if (obj instanceof List<?>) {
            theList = (List<?>)obj;
        } else {
            Debug.logError("Object not list or map type", module);
            return;
        }
        listSize = theList.size();
        WidgetWorker.incrementPaginatorNumber(context);
        int startPageNumber = WidgetWorker.getPaginatorNumber(context);

        if (getPaginate(context)) {
            try {
                Map<String, String> params = UtilGenerics.cast(context.get("parameters"));
                String viewIndexString = params.get("VIEW_INDEX" + "_" + WidgetWorker.getPaginatorNumber(context));
                String viewSizeString = params.get("VIEW_SIZE" + "_" + WidgetWorker.getPaginatorNumber(context));
                viewIndex = Integer.parseInt(viewIndexString);
                locViewSize = Integer.parseInt(viewSizeString);
            } catch (NumberFormatException e) {
                try {
                    viewIndex = (Integer) context.get("viewIndex");
                } catch (Exception e2) {
                    viewIndex = 0;
                }
            }
            context.put("viewIndex", viewIndex);
            lowIndex = viewIndex * locViewSize;
            highIndex = (viewIndex + 1) * locViewSize;
        } else {
            viewIndex = 0;
            locViewSize = MAX_PAGE_SIZE;
            lowIndex = 0;
            highIndex = MAX_PAGE_SIZE;
        }
        Iterator<?> iter = theList.iterator();
        int itemIndex = -1;
        int iterateIndex = 0;
        while (iter.hasNext()) {
            itemIndex++;
            if (itemIndex >= highIndex) {
                break;
            }
            Object item = iter.next();
            if (itemIndex < lowIndex) {
                continue;
            }
            if (isEntrySet) {
                Map.Entry<String, ?> entry = UtilGenerics.cast(item);
                contextMs.put(entryName, entry.getValue());
                contextMs.put(keyName, entry.getKey());
            } else {
                contextMs.put(entryName, item);
            }
            contextMs.put("itemIndex", itemIndex);

            if (iterateIndex < listSize) {
                contextMs.put("iterateId",String.valueOf(entryName+iterateIndex));
                iterateIndex++;
            }
            for (ModelScreenWidget.Section section: this.sectionList) {
                section.renderWidgetString(writer, contextMs, screenStringRenderer);
            }
        }

        if ((itemIndex + 1) < highIndex) {
            highIndex = itemIndex + 1;
        }
        actualPageSize = highIndex - lowIndex;
        if (getPaginate(context)) {
            try {
                Integer lastPageNumber = null;
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                if (globalCtx != null) {
                    lastPageNumber = (Integer)globalCtx.get("PAGINATOR_NUMBER");
                    globalCtx.put("PAGINATOR_NUMBER", startPageNumber);
                }
                renderNextPrev(writer, context, listSize, actualPageSize);
                if (globalCtx != null) {
                    globalCtx.put("PAGINATOR_NUMBER", lastPageNumber);
                }
            } catch (IOException e) {
                Debug.logError(e, module);
                throw new RuntimeException(e.getMessage());
            }
        }

    }
    /*
     * @return
     */
    public String getPaginateTarget(Map<String, Object> context) {
        return this.paginateTarget.expandString(context);
    }

    public boolean getPaginate(Map<String, Object> context) {
        if (!this.paginate.isEmpty() && UtilValidate.isNotEmpty(this.paginate.expandString(context))) {
            return Boolean.valueOf(this.paginate.expandString(context));
        }
        return true;
    }

    public int getViewSize() {
        return viewSize;
    }

    public void renderNextPrev(Appendable writer, Map<String, Object> context, int listSize, int actualPageSize) throws IOException {
        String targetService = this.getPaginateTarget(context);
        if (targetService == null) {
            targetService = "${targetService}";
        }

        Map<String, Object> inputFields = UtilGenerics.checkMap(context.get("requestParameters"));
        Object obj = context.get("queryStringMap");
        Map<String, Object> queryStringMap = (obj instanceof Map) ? UtilGenerics.cast(obj) : null;
        if (UtilValidate.isNotEmpty(queryStringMap)) {
            inputFields.putAll(queryStringMap);
        }

        String queryString = UtilHttp.urlEncodeArgs(inputFields);
        int paginatorNumber = WidgetWorker.getPaginatorNumber(context);
        queryString = UtilHttp.stripViewParamsFromQueryString(queryString, "" + paginatorNumber);




        if (UtilValidate.isEmpty(targetService)) {
            Debug.logWarning("TargetService is empty.", module);
            return;
        }

        int viewIndex = -1;
        try {
            viewIndex = (Integer) context.get("viewIndex");
        } catch (Exception e) {
            viewIndex = 0;
        }

        int viewSize = -1;
        try {
            viewSize = (Integer) context.get("viewSize");
        } catch (Exception e) {
            viewSize = this.getViewSize();
        }

        int lowIndex = viewIndex * viewSize;
        int highIndex = (viewIndex + 1) * viewSize;
        // if this is all there seems to be (if listSize < 0, then size is unknown)
        if (actualPageSize >= listSize && listSize > 0) {
            return;
        }

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");

        RequestHandler rh = RequestHandler.from(request);

        writer.append("<table border=\"0\" width=\"100%\" cellpadding=\"2\">\n");
        writer.append("  <tr>\n");
        writer.append("    <td align=\"right\">\n");
        writer.append("      <b>\n");
        if (viewIndex > 0) {
            writer.append(" <a href=\"");
            StringBuilder linkText = new StringBuilder(targetService);
            if (linkText.indexOf("?") < 0) {
                linkText.append("?");
            } else {
                linkText.append("&amp;");
            }
            if (UtilValidate.isNotEmpty(queryString)) {
                linkText.append(queryString).append("&amp;");
            }
            linkText.append("VIEW_SIZE_"+ paginatorNumber + "=").append(viewSize).append("&amp;VIEW_INDEX_" + paginatorNumber + "=").append(viewIndex - 1).append("\"");

            // make the link
            writer.append(rh.makeLink(request, response, linkText.toString(), false, false, false));
            String previous = UtilProperties.getMessage("CommonUiLabels", "CommonPrevious", (Locale) context.get("locale"));
            writer.append(" class=\"buttontext\">[").append(previous).append("]</a>\n");

        }
        if (listSize > 0) {
            Map<String, Integer> messageMap = UtilMisc.toMap("lowCount", lowIndex + 1, "highCount", lowIndex + actualPageSize, "total", listSize);
            String commonDisplaying = UtilProperties.getMessage("CommonUiLabels", "CommonDisplaying", messageMap, (Locale) context.get("locale"));
            writer.append(" <span class=\"tabletext\">").append(commonDisplaying).append("</span> \n");
        }
        if (highIndex < listSize) {
            writer.append(" <a href=\"");
            StringBuilder linkText = new StringBuilder(targetService);
            if (linkText.indexOf("?") < 0) {
                linkText.append("?");
            } else {
                linkText.append("&amp;");
            }
            if (UtilValidate.isNotEmpty(queryString)) {
                linkText.append(queryString).append("&amp;");
            }
            linkText.append("VIEW_SIZE_" + paginatorNumber + "=").append(viewSize).append("&amp;VIEW_INDEX_" + paginatorNumber + "=").append(viewIndex + 1).append("\"");

            // make the link
            writer.append(rh.makeLink(request, response, linkText.toString(), false, false, false));
            String next = UtilProperties.getMessage("CommonUiLabels", "CommonNext", (Locale) context.get("locale"));
            writer.append(" class=\"buttontext\">[").append(next).append("]</a>\n");

        }
        writer.append("      </b>\n");
        writer.append("    </td>\n");
        writer.append("  </tr>\n");
        writer.append("</table>\n");

    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    public FlexibleMapAccessor<Object> getListNameExdr() {
        return listNameExdr;
    }

    public FlexibleStringExpander getEntryNameExdr() {
        return entryNameExdr;
    }

    public FlexibleStringExpander getKeyNameExdr() {
        return keyNameExdr;
    }

    public FlexibleStringExpander getPaginateTarget() {
        return paginateTarget;
    }

    public FlexibleStringExpander getPaginate() {
        return paginate;
    }

}