/*
 * $Id: WfActivityImplementationFact.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.workflow.impl;

/**
 * WfActivityImplementationFact.java
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @author     Oswin Ondarza and Manuel Soto
 * @version    $Rev$
 * @since      2.0
 */
public class WfActivityImplementationFact {

    public static final String module = WfActivityImplementationFact.class.getName();

    /**
     * Gets the implementation class to be used.
     * @param type
     * @param wfActivity
     * @return WfActivityAbstractImplementation
     */
    public static WfActivityAbstractImplementation getConcretImplementation(String type, WfActivityImpl wfActivity) {
        if (type.equals("WAT_NO"))
            return new WfActivityNoImplementation(wfActivity); // NO implementation requires MANUAL FinishMode
        else if (type.equals("WAT_ROUTE"))
            return new WfActivityRouteImplementation(wfActivity); // ROUTE goes directly to complete status
        else if (type.equals("WAT_TOOL"))
            return new WfActivityToolImplementation(wfActivity);
        else if (type.equals("WAT_SUBFLOW"))
            return new WfActivitySubFlowImplementation(wfActivity);
        else if (type.equals("WAT_LOOP"))
            return new WfActivityLoopImplementation(wfActivity);
        else
            return null;
    }
}
