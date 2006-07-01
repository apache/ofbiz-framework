/*
 * $Id: TransitionCondition.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.workflow;

import java.util.Map;

import org.ofbiz.service.DispatchContext;

/**
 * TransitionCondition - Interface for implementing transition conditions
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @version    $Rev$
 * @since      2.1
 */
public interface TransitionCondition {
    
    /**
     * Evaluate a condition and return the result as a Boolean
     * @param context Map of environment info (processContext) for use in evaluation
     * @param attrs Map of transition's extended attributes
     * @param expression The expression from the transition condition
     * @param dctx The DispatchContext to be used in processing the condition
     * @return The result of the evaluation
     * @throws EvaluationException
     */
    public Boolean evaluateCondition(Map context, Map attrs, String expression, DispatchContext dctx) throws EvaluationException;

}
