/*
 * $Id: JRMapCollectionDataSource.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2002-2003 The Open For Business Project - www.ofbiz.org
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
 */

package org.ofbiz.content.report;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * <code>JRMapCollectionDataSource</code>
 * 
 * @author <a href="mailto:gielen@aixcept.de">Rene Gielen</a>
 * @version $Rev$
 */
public class JRMapCollectionDataSource implements JRDataSource {

    private Collection data = null;
    private Iterator iterator = null;
    private Map currentMap = null;

    public JRMapCollectionDataSource(Collection mapCollection) {
        this.data = mapCollection;

        if (data != null) {
            this.iterator = data.iterator();
        }
    }

    public boolean next() throws JRException {
        boolean hasNext = false;

        if (this.iterator != null) {
            hasNext = this.iterator.hasNext();

            if (hasNext) {
                try {
                    this.currentMap = (Map) this.iterator.next();
                } catch (Exception e) {
                    throw new JRException("Current collection object does not seem to be a Map.", e);
                }
            }
        }

        return hasNext;
    }

    public Object getFieldValue(JRField jrField) throws JRException {
        Object value = null;

        if (currentMap != null) {
            value = currentMap.get(jrField.getName());
        }

        return value;
    }

}
