/*
 * $Id: GenericPK.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.entity;

import java.util.Map;

import javolution.realtime.ObjectFactory;

import org.ofbiz.entity.model.ModelEntity;

/**
 * Generic Entity Primary Key Object
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class GenericPK extends GenericEntity {

    protected static final ObjectFactory genericPKFactory = new ObjectFactory() {
        protected Object create() {
            return new GenericPK();
        }
    };
    
    protected GenericPK() { }
    
    /** Creates new GenericPK */
    public static GenericPK create(ModelEntity modelEntity) {
        GenericPK newPK = (GenericPK) genericPKFactory.object();
        newPK.init(modelEntity);
        return newPK;
    }

    /** Creates new GenericPK from existing Map */
    public static GenericPK create(ModelEntity modelEntity, Map fields) {
        GenericPK newPK = (GenericPK) genericPKFactory.object();
        newPK.init(modelEntity, fields);
        return newPK;
    }

    /** Creates new GenericPK from existing GenericPK */
    public static GenericPK create(GenericPK value) {
        GenericPK newPK = (GenericPK) genericPKFactory.object();
        newPK.init(value);
        return newPK;
    }

    /** Clones this GenericPK, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericPK
     */
    public Object clone() {
        GenericPK newEntity = GenericPK.create(this);
        newEntity.setDelegator(internalDelegator);
        return newEntity;
    }
}
