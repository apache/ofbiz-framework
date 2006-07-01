/*
 * $Id: JREntityListIteratorDataSource.java 5462 2005-08-05 18:35:48Z jonesde $
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

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityListIterator;

/**
 * <code>JREntityListIteratorDataSource</code>
 * 
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:gielen@aixcept.de">Rene Gielen</a>
 * @version $Rev$
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
