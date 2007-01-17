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

package org.ofbiz.content.report;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * <code>JRMapCollectionDataSource</code>
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
