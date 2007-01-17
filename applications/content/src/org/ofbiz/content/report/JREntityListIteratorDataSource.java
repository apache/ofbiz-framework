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

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityListIterator;

/**
 * <code>JREntityListIteratorDataSource</code>
 */
public class JREntityListIteratorDataSource implements JRDataSource {
    
    public static final String module = JREntityListIteratorDataSource.class.getName();

    private EntityListIterator entityListIterator = null;
    private GenericEntity currentEntity = null;

    public JREntityListIteratorDataSource(EntityListIterator entityListIterator) {
        this.entityListIterator = entityListIterator;
    }

    public boolean next() throws JRException {
        if (this.entityListIterator == null) {
            return false;
        }
        Object nextObj = this.entityListIterator.next();
        if (nextObj != null) {
            if (nextObj instanceof GenericEntity) {
                this.currentEntity = (GenericEntity) nextObj;
            } else {
                throw new JRException("Current collection object does not seem to be a GenericEntity (or GenericValue or GenericPK).");
            }
            return true;
        } else {
            // nothing left, close here...
            try {
                this.entityListIterator.close();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error closing EntityListIterator in Jasper Reports DataSource", module);
                throw new JRException(e);
            }
            this.entityListIterator = null;
            return false;
        }
    }

    public Object getFieldValue(JRField jrField) throws JRException {
        Object value = null;
        if (this.currentEntity != null) {
            try {
                value = this.currentEntity.get(jrField.getName());
            } catch (IllegalArgumentException e) {
                try {
                    value = this.currentEntity.get(org.ofbiz.entity.model.ModelUtil.dbNameToVarName(jrField.getName()));
                }  catch (IllegalArgumentException ex) {
                    throw new JRException("The specified field name [" + jrField.getName() + "] is not a valid field-name for the entity: " + this.currentEntity.getEntityName(), e);
                }
            }
        }
        return value;
    }
}
