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
package org.ofbiz.widget.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Element;


/**
 * Widget Library - Screen model HTML class
 */
@SuppressWarnings("serial")
public class IterateSectionWidget extends ModelScreenWidget {
    public static final String module = IterateSectionWidget.class.getName();

    protected ModelScreenWidget childWidget;
    protected List<ModelScreenWidget.Section> sectionList;
    protected FlexibleMapAccessor<Object> listNameExdr;
    protected FlexibleStringExpander entryNameExdr;
    protected FlexibleStringExpander keyNameExdr;
    protected FlexibleStringExpander paginateTarget;
    protected FlexibleStringExpander paginate;

    public static int DEFAULT_PAGE_SIZE = 5;
    public static int MAX_PAGE_SIZE = 10000;
    protected int viewIndex = 0;
    protected int viewSize = DEFAULT_PAGE_SIZE;
    protected int lowIndex = -1;
    protected int highIndex = -1;
    protected int listSize = 0;
    protected int actualPageSize = 0;


    public IterateSectionWidget(ModelScreen modelScreen, Element iterateSectionElement) {
        super(modelScreen, iterateSectionElement);
        listNameExdr = FlexibleMapAccessor.getInstance(iterateSectionElement.getAttribute("list"));
        if (listNameExdr.isEmpty()) listNameExdr = FlexibleMapAccessor.getInstance(iterateSectionElement.getAttribute("list-name"));
        entryNameExdr = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("entry"));
        if (entryNameExdr.isEmpty()) entryNameExdr = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("entry-name"));
        keyNameExdr = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("key"));
        if (keyNameExdr.isEmpty()) keyNameExdr = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("key-name"));

        if (this.paginateTarget == null || iterateSectionElement.hasAttribute("paginate-target")) {
            this.paginateTarget = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("paginate-target"));
        }

        if (this.paginate == null || iterateSectionElement.hasAttribute("paginate")) {
            this.paginate = FlexibleStringExpander.getInstance(iterateSectionElement.getAttribute("paginate"));
        }

        if (iterateSectionElement.hasAttribute("view-size")) {
            setViewSize(iterateSectionElement.getAttribute("view-size"));
        }
        sectionList = new ArrayList<ModelScreenWidget.Section>();
        List<? extends Element> childElementList = UtilXml.childElementList(iterateSectionElement);
        for (Element sectionElement: childElementList) {
            ModelScreenWidget.Section section = new ModelScreenWidget.Section(modelScreen, sectionElement);
            sectionList.add(section);
        }
    }

    @Override
    public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {

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
        WidgetWorker.incrementPaginatorNumber(context);
        int startPageNumber = WidgetWorker.getPaginatorNumber(context);
        getListLimits(context, theList);
        int rowCount = 0;
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
            contextMs.put("itemIndex", Integer.valueOf(itemIndex));

            if (iterateIndex < listSize) {
                contextMs.put("iterateId",String.valueOf(entryName+iterateIndex));
                iterateIndex++;
            }
            rowCount++;
            for (ModelScreenWidget.Section section: this.sectionList) {
                section.renderWidgetString(writer, contextMs, screenStringRenderer);
            }
        }

        if ((itemIndex + 1) < highIndex) {
            setHighIndex(itemIndex + 1);
        }
        setActualPageSize(highIndex - lowIndex);
        if (getPaginate(context)) {
            try {
                Integer lastPageNumber = null;
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                if (globalCtx != null) {
                    lastPageNumber = (Integer)globalCtx.get("PAGINATOR_NUMBER");
                    globalCtx.put("PAGINATOR_NUMBER", Integer.valueOf(startPageNumber));
                }

                renderNextPrev(writer, context);

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
        if (this.paginate != null && !this.paginate.isEmpty() && UtilValidate.isNotEmpty(this.paginate.expandString(context))) {
            return Boolean.valueOf(this.paginate.expandString(context)).booleanValue();
        } else {
            return true;
        }
    }

    public void setPaginate(boolean val) {
        this.paginate = FlexibleStringExpander.getInstance(Boolean.valueOf(val).toString());
    }

    public void setViewIndex(int val) {
        viewIndex = val;
    }

    public void setViewSize(int val) {
        viewSize = val;
    }

    public void setViewSize(String val) {
        try {
            viewSize = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            viewSize = DEFAULT_PAGE_SIZE;
        }
    }

    public void setListSize(int val) {
        listSize = val;
    }

    public void setLowIndex(int val) {
        lowIndex = val;
    }

    public void setHighIndex(int val) {
        highIndex = val;
    }
    public void setActualPageSize(int val) {
        actualPageSize = val;
    }

    public int getViewIndex() {
        return viewIndex;
    }

    public int getViewSize() {
        return viewSize;
    }

    public int getListSize() {
        return listSize;
    }

    public int getLowIndex() {
        return lowIndex;
    }

    public int getHighIndex() {
        return highIndex;
    }

    public int getActualPageSize() {
        return actualPageSize;
    }

    public <X> void getListLimits(Map<String, Object> context, List<X> items) {
        listSize = items.size();

        if (getPaginate(context)) {
            try {
                Map<String, String> params = UtilGenerics.cast(context.get("parameters"));
                String viewIndexString = params.get("VIEW_INDEX" + "_" + WidgetWorker.getPaginatorNumber(context));
                String viewSizeString = params.get("VIEW_SIZE" + "_" + WidgetWorker.getPaginatorNumber(context));
                viewIndex = Integer.parseInt(viewIndexString);
                viewSize = Integer.parseInt(viewSizeString);
            } catch (Exception e) {
                try {
                    viewIndex = ((Integer) context.get("viewIndex")).intValue();
                } catch (Exception e2) {
                    viewIndex = 0;
                }
            }
            context.put("viewIndex", Integer.valueOf(this.viewIndex));

            lowIndex = viewIndex * viewSize;
            highIndex = (viewIndex + 1) * viewSize;
        } else {
            viewIndex = 0;
            viewSize = MAX_PAGE_SIZE;
            lowIndex = 0;
            highIndex = MAX_PAGE_SIZE;
        }
    }


    public void renderNextPrev(Appendable writer, Map<String, Object> context) throws IOException {
        String targetService = this.getPaginateTarget(context);
        if (targetService == null) {
            targetService = "${targetService}";
        }

        Map<String, Object> inputFields = UtilGenerics.checkMap(context.get("requestParameters"));
        Map<String, Object> queryStringMap = UtilGenerics.toMap(context.get("queryStringMap"));
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
            viewIndex = ((Integer) context.get("viewIndex")).intValue();
        } catch (Exception e) {
            viewIndex = 0;
        }

        int viewSize = -1;
        try {
            viewSize = ((Integer) context.get("viewSize")).intValue();
        } catch (Exception e) {
            viewSize = this.getViewSize();
        }

        int listSize = -1;
        try {
            listSize = this.getListSize();
        } catch (Exception e) {
            listSize = -1;
        }

        /*
        int highIndex = -1;
        try {
            highIndex = modelForm.getHighIndex();
        } catch (Exception e) {
            highIndex = 0;
        }

        int lowIndex = -1;
        try {
            lowIndex = modelForm.getLowIndex();
        } catch (Exception e) {
            lowIndex = 0;
        }
         */

        int lowIndex = viewIndex * viewSize;
        int highIndex = (viewIndex + 1) * viewSize;
        int actualPageSize = this.getActualPageSize();
        // if this is all there seems to be (if listSize < 0, then size is unknown)
        if (actualPageSize >= listSize && listSize > 0) {
            return;
        }

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");

        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");

        writer.append("<table border=\"0\" width=\"100%\" cellpadding=\"2\">\n");
        writer.append("  <tr>\n");
        writer.append("    <td align=\"right\">\n");
        writer.append("      <b>\n");
        if (viewIndex > 0) {
            writer.append(" <a href=\"");
            StringBuilder linkText = new StringBuilder(targetService);
            if (linkText.indexOf("?") < 0)  linkText.append("?");
            else linkText.append("&amp;");
            //if (queryString != null && !queryString.equals("null")) linkText += queryString + "&";
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
            Map<String, Integer> messageMap = UtilMisc.toMap("lowCount", Integer.valueOf(lowIndex + 1), "highCount", Integer.valueOf(lowIndex + actualPageSize), "total", Integer.valueOf(listSize));
            String commonDisplaying = UtilProperties.getMessage("CommonUiLabels", "CommonDisplaying", messageMap, (Locale) context.get("locale"));
            writer.append(" <span class=\"tabletext\">").append(commonDisplaying).append("</span> \n");
        }
        if (highIndex < listSize) {
            writer.append(" <a href=\"");
            StringBuilder linkText = new StringBuilder(targetService);
            if (linkText.indexOf("?") < 0)  linkText.append("?");
            else linkText.append("&amp;");
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
    public String rawString() {
        // TODO: something more than the empty tag
        return "<iterate-section/>";
    }
}


