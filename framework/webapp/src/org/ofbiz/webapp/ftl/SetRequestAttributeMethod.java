/*
 * $Id: SetRequestAttributeMethod.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.webapp.ftl;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/**
 * SetRequestAttributeMethod - Freemarker Method for setting request attributes
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.1
 */
public class SetRequestAttributeMethod implements TemplateMethodModelEx {
        
    public static final String module = SetRequestAttributeMethod.class.getName();        

    /* 
     * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
     */
    public Object exec(List args) throws TemplateModelException {
        if (args == null || args.size() != 2)
            throw new TemplateModelException("Invalid number of arguements");  
        if (!(args.get(0) instanceof TemplateScalarModel))    
            throw new TemplateModelException("First argument not an instance of TemplateScalarModel");
        if (!(args.get(1) instanceof BeanModel) && !(args.get(1) instanceof TemplateNumberModel) && !(args.get(1) instanceof TemplateScalarModel)) 
            throw new TemplateModelException("Second argument not an instance of BeanModel nor TemplateNumberModel nor TemplateScalarModel");
                          
        Environment env = Environment.getCurrentEnvironment();
        BeanModel req = (BeanModel)env.getVariable("request");
        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
        
        String name = ((TemplateScalarModel) args.get(0)).getAsString();
        Object value = null;
        if (args.get(1) instanceof TemplateScalarModel)
            value = ((TemplateScalarModel) args.get(1)).getAsString();
        if (args.get(1) instanceof TemplateNumberModel)
            value = ((TemplateNumberModel) args.get(1)).getAsNumber();
        if (args.get(1) instanceof BeanModel)
            value = ((BeanModel) args.get(1)).getWrappedObject();        
                       
        request.setAttribute(name, value);               
        return new SimpleScalar("");
    }

}
