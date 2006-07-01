/*
 * $Id: MapProcessor.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.minilang.operation;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

/**
 * Map Processor Main Class
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class MapProcessor {
    
    String name;
    List makeInStrings = new LinkedList();
    List simpleMapProcesses = new LinkedList();

    public MapProcessor(Element simpleMapProcessorElement) {
        name = simpleMapProcessorElement.getAttribute("name");

        List makeInStringElements = UtilXml.childElementList(simpleMapProcessorElement, "make-in-string");
        Iterator misIter = makeInStringElements.iterator();

        while (misIter.hasNext()) {
            Element makeInStringElement = (Element) misIter.next();
            MakeInString makeInString = new MakeInString(makeInStringElement);

            makeInStrings.add(makeInString);
        }

        List simpleMapProcessElements = UtilXml.childElementList(simpleMapProcessorElement, "process");
        Iterator strProcIter = simpleMapProcessElements.iterator();

        while (strProcIter.hasNext()) {
            Element simpleMapProcessElement = (Element) strProcIter.next();
            SimpleMapProcess strProc = new SimpleMapProcess(simpleMapProcessElement);

            simpleMapProcesses.add(strProc);
        }
    }

    public String getName() {
        return name;
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {
        if (makeInStrings != null && makeInStrings.size() > 0) {
            Iterator misIter = makeInStrings.iterator();

            while (misIter.hasNext()) {
                MakeInString makeInString = (MakeInString) misIter.next();

                makeInString.exec(inMap, results, messages, locale, loader);
            }
        }

        if (simpleMapProcesses != null && simpleMapProcesses.size() > 0) {
            Iterator strPrsIter = simpleMapProcesses.iterator();

            while (strPrsIter.hasNext()) {
                SimpleMapProcess simpleMapProcess = (SimpleMapProcess) strPrsIter.next();

                simpleMapProcess.exec(inMap, results, messages, locale, loader);
            }
        }
    }
}
