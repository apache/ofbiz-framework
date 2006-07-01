/*
 * $Id: RunningService.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service;

import java.sql.Timestamp;

import org.ofbiz.base.util.UtilDateTime;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
public class RunningService {

    protected ModelService model;
    protected String name;
    protected int mode;

    protected Timestamp startStamp;
    protected Timestamp endStamp;

    private RunningService() {
        this.startStamp = UtilDateTime.nowTimestamp();
        this.endStamp = null;
    }

    public RunningService(String localName, ModelService model, int mode) {
        this();
        this.name = localName;
        this.model = model;
        this.mode = mode;
    }

    public ModelService getModelService() {
        return this.model;
    }

    public String getLocalName() {
        return this.name;
    }
    
    public int getMode() {
        return mode;
    }

    public Timestamp getStartStamp() {
        return this.startStamp;
    }

    public Timestamp getEndStamp() {
        return this.endStamp;
    }

    public void setEndStamp() {
        this.endStamp = UtilDateTime.nowTimestamp();
    }

    public boolean equals(Object o) {
        if (o != null && o instanceof RunningService) {
            RunningService x = (RunningService) o;
            if (this.model.equals(x) && this.mode == x.getMode() && this.startStamp.equals(x.getStartStamp())) {
                return true;
            }
        }
        return false;
    }
}
