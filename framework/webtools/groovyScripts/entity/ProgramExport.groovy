/*
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
 */
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.base.util.*
import org.w3c.dom.Document

import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.ErrorCollector

String groovyProgram = null
recordValues = []
errMsgList = []

if (!parameters.groovyProgram) {
    
    groovyProgram = '''
// Use the List variable recordValues to fill it with GenericValue maps.
// full groovy syntax is available
// Use full EntityQuery syntax instead of just the from method

import org.apache.ofbiz.entity.util.EntityFindOptions

// example:

// find the first three record in the product entity (if any)
EntityFindOptions findOptions = new EntityFindOptions()
findOptions.setMaxRows(3)

List products = delegator.findList("Product", null, null, null, findOptions, false)
if (products != null) {  
    recordValues.addAll(products)
}

// Get the last record created from the Product entity
condition = EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, null)
product = EntityQuery.use(delegator).from("Product").where(condition).orderBy("-productId").queryFirst()
if (product) {
    recordValues << product
}

'''
    parameters.groovyProgram = groovyProgram
} else {
    groovyProgram = parameters.groovyProgram
}

// Add imports for script.
def importCustomizer = new ImportCustomizer()
importCustomizer.addImport("org.apache.ofbiz.entity.GenericValue")
importCustomizer.addImport("org.apache.ofbiz.entity.model.ModelEntity")
importCustomizer.addImport("org.apache.ofbiz.entity.condition.EntityCondition")
importCustomizer.addImport("org.apache.ofbiz.entity.condition.EntityOperator")
importCustomizer.addImport("org.apache.ofbiz.entity.util.EntityQuery")
def configuration = new CompilerConfiguration()
configuration.addCompilationCustomizers(importCustomizer)

Binding binding = new Binding()
binding.setVariable("delegator", delegator)
binding.setVariable("recordValues", recordValues)

ClassLoader loader = Thread.currentThread().getContextClassLoader()
def shell = new GroovyShell(loader, binding, configuration)

if (groovyProgram) {
    try {
        shell.parse(groovyProgram)
        shell.evaluate(groovyProgram)
        recordValues = shell.getVariable("recordValues")
        xmlDoc = GenericValue.makeXmlDocument(recordValues)
        context.put("xmlDoc", xmlDoc)
    } catch(MultipleCompilationErrorsException e) {
        request.setAttribute("_ERROR_MESSAGE_", e)
        return
    } catch(groovy.lang.MissingPropertyException e) {
        request.setAttribute("_ERROR_MESSAGE_", e)
        return
    } catch(IllegalArgumentException e) {
        request.setAttribute("_ERROR_MESSAGE_", e)
        return
    } catch(NullPointerException e) {
        request.setAttribute("_ERROR_MESSAGE_", e)
        return
    } catch(Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e)
        return
    } 
}
