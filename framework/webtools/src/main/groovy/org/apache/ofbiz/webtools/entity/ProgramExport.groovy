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
package org.apache.ofbiz.webtools.entity

import static org.codehaus.groovy.syntax.Types.KEYWORD_IMPORT
import static org.codehaus.groovy.syntax.Types.KEYWORD_PACKAGE

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.entity.util.EntityFindOptions
import org.apache.ofbiz.entity.util.EntityQuery
import org.codehaus.groovy.ast.expr.MethodPointerExpression
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer

String groovyProgram = null
recordValues = []
errMsgList = []

if (parameters.groovyProgram) {
    groovyProgram = parameters.groovyProgram
} else {
    groovyProgram = '''
// Use the List variable recordValues to fill it with GenericValue maps.
// full groovy syntax is available
// Use full EntityQuery syntax instead of just the from method

import org.apache.ofbiz.entity.util.EntityFindOptions

// example:

// find the first three record in the product entity (if any)
EntityFindOptions findOptions = new EntityFindOptions()
findOptions.setMaxRows(3)

List products = delegator.findList(\'Product\', null, null, null, findOptions, false)
if (products != null) {
    recordValues.addAll(products)
}

// Get the last record created from the Product entity
condition = EntityCondition.makeCondition(\'productId\', EntityOperator.NOT_EQUAL, null)
product = EntityQuery.use(delegator).from(\'Product\').where(condition).orderBy(\'-productId\').queryFirst()
if (product) {
    recordValues << product
}

'''
    parameters.groovyProgram = groovyProgram
}

// Dangerous Pattern Detection
// (?s) flag for multi-line/dotall matching to prevent whitespace bypass
List<String> dangerousPatterns = [
        // Process & Command Execution + Runtime Variants
        /(?s)Runtime\s*\.\s*getRuntime\s*\(\s*\)/,
        /(?s)['"]java\.lang\.Runtime['"]\.class/,
        /(?s)Runtime\.class\.getDeclaredMethod/,
        /(?s)getRuntime\s*\(\s*\)\.exec/,
        /(?s)ProcessBuilder/,
        /(?s)\.\s*execute\s*\(/,
        /(?s)System\s*\.\s*exit/,
        // Reflection & ClassLoading
        /(?s)Class\s*\.\s*forName/,
        /(?s)\.newInstance\s*\(/,
        /(?s)\.getDeclaredMethod/,
        /(?s)\.getDeclaredField/,
        /(?s)\.getMethod\s*\(/,
        /(?s)\.getField\s*\(/,
        /(?s)\.invoke\s*\(/,
        /(?s)\.loadClass\s*\(/,
        /(?s)\.getClassLoader\s*\(/,
        /(?s)java\s*\.\s*lang\s*\.\s*reflect/,
        /(?s)URLClassLoader/,
        /(?s)GroovyClassLoader/,
        /(?s)ScriptEngineManager/,
        /(?s)javax\s*\.\s*script/,
        /(?s)sun\s*\.\s*misc\s*\.\s*Unsafe/,
        // Eval/GroovyShell Blocking
        /(?s)Eval\s*\.\s*me/,
        /(?s)Eval\s*\.\s*x/,
        /(?s)Eval\s*\.\s*xy/,
        /(?s)Eval\s*\.\s*xyz/,
        /(?s)GroovyShell/,
        /(?s)\.evaluate\s*\(/,
        // File System Operations
        /(?s)java\s*\.\s*io\s*\.\s*File\s*\(/,
        /(?s)new\s+File\s*\(/,
        /(?s)Files\s*\.\s*readAllBytes/,
        /(?s)Paths\s*\.\s*get/,
        /(?s)\.toFile\s*\(/,
        /(?s)\.getResourceAsStream\s*\(/,
        /(?s)\.getText\s*\(/,
        /(?s)\.bytes\b/,
        // Network Operations
        /(?s)Socket\s*\(/,
        /(?s)ServerSocket/,
        /(?s)DatagramSocket/,
        /(?s)InetSocketAddress/,
        /(?s)InetAddress/,
        /(?s)java\s*\.\s*net\s*\./,
        /(?s)URL\s*\(/,
        /(?s)NetworkInterface/,
        /(?s)\.openConnection\s*\(/,
        /(?s)\.connect\s*\(/,
        // OFBiz Multitenancy Bypass
        /(?s)DelegatorFactory/
]

for (String pattern : dangerousPatterns) {
    if (groovyProgram =~ pattern) {
        request.setAttribute('_ERROR_MESSAGE_', "Script contains prohibited pattern: ${pattern}")
        return
    }
}

// Groovy Sandbox with SecureASTCustomizer
SecureASTCustomizer secureCustomizer = new SecureASTCustomizer()
secureCustomizer.with {
    // Import whitelist - only safe OFBiz entity classes
    setImportsWhitelist([
            'org.apache.ofbiz.entity.GenericValue',
            'org.apache.ofbiz.entity.model.ModelEntity',
            'org.apache.ofbiz.entity.condition.EntityCondition',
            'org.apache.ofbiz.entity.condition.EntityOperator',
            'org.apache.ofbiz.entity.util.EntityQuery',
            'org.apache.ofbiz.entity.util.EntityFindOptions',
            'java.util.List',
            'java.util.Map',
            'java.util.Set'
    ])
    setStarImportsWhitelist([])
    setStaticImportsWhitelist([])
    setStaticStarImportsWhitelist([])
    setIndirectImportCheckEnabled(false)
    // Constant types whitelist
    setConstantTypesClassesWhiteList([
            Object, String, Integer, Long, Float, Double, Boolean,
            Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Boolean.TYPE,
            BigDecimal, BigInteger,
            Date, java.sql.Date, java.sql.Timestamp,
            Range, IntRange,
            GenericValue, ModelEntity,
            EntityCondition, EntityOperator,
            EntityQuery, EntityFindOptions,
            List, Map, Set
    ])
    // Token and statement restrictions
    setTokensBlacklist([KEYWORD_PACKAGE, KEYWORD_IMPORT])
    setStatementsBlacklist([
            WhileStatement, ForStatement,
            SwitchStatement
    ])
    setExpressionsBlacklist([MethodPointerExpression])
    // Receiver whitelist - only safe OFBiz entity operations
    setReceiversWhiteList([
            'java.lang.Object',
            'org.apache.ofbiz.entity.Delegator',
            'org.apache.ofbiz.entity.util.EntityQuery',
            'org.apache.ofbiz.entity.util.EntityFindOptions',
            'org.apache.ofbiz.entity.GenericValue',
            'org.apache.ofbiz.entity.condition.EntityCondition',
            'org.apache.ofbiz.entity.condition.EntityOperator',
            'org.apache.ofbiz.entity.model.ModelEntity',
            'java.util.List', 'java.util.Map', 'java.util.Set',
            'java.lang.String', 'java.lang.Integer',
            'java.lang.Long', 'java.lang.Boolean',
            'java.util.Date', 'java.sql.Date', 'java.sql.Timestamp',
            'java.math.BigDecimal', 'java.math.BigInteger',
            'groovy.lang.Range', 'groovy.lang.IntRange'
    ])
    setClosuresAllowed(true)
    setMethodDefinitionAllowed(false)
}

// Add imports for script.
ImportCustomizer importCustomizer = new ImportCustomizer()
importCustomizer.addImport('org.apache.ofbiz.entity.GenericValue')
importCustomizer.addImport('org.apache.ofbiz.entity.model.ModelEntity')
importCustomizer.addImport('org.apache.ofbiz.entity.condition.EntityCondition')
importCustomizer.addImport('org.apache.ofbiz.entity.condition.EntityOperator')
importCustomizer.addImport('org.apache.ofbiz.entity.util.EntityQuery')

// AST TRANSFORMATION BLOCKING - Disable Grape/Grab
CompilerConfiguration configuration = new CompilerConfiguration()
try {
    Class grabTransform = Thread.currentThread().contextClassLoader
            .loadClass('org.codehaus.groovy.transform.GrabAnnotationTransformation')
    configuration.setDisabledGlobalASTTransformations(
            [grabTransform.name] as Set)
} catch (ClassNotFoundException ignored) {
}
configuration.addCompilationCustomizers(importCustomizer)
configuration.addCompilationCustomizers(secureCustomizer)

Binding binding = new Binding()
binding.setVariable('delegator', delegator)
binding.setVariable('recordValues', recordValues)

ClassLoader loader = Thread.currentThread().getContextClassLoader()
GroovyShell shell = new GroovyShell(loader, binding, configuration)

/* codenarc-disable ReturnNullFromCatchBlock */
if (groovyProgram) {
    try {
        shell.parse(groovyProgram)
        shell.evaluate(groovyProgram)
        recordValues = shell.getVariable('recordValues')
        xmlDoc = GenericValue.makeXmlDocument(recordValues)
        context.put('xmlDoc', xmlDoc)
    } catch (MultipleCompilationErrorsException e) {
        request.setAttribute('_ERROR_MESSAGE_', e)
        return
    } catch (MissingPropertyException e) {
        request.setAttribute('_ERROR_MESSAGE_', e)
        return
    } catch (IllegalArgumentException e) {
        request.setAttribute('_ERROR_MESSAGE_', e)
        return
    } catch (SecurityException e) {
        request.setAttribute('_ERROR_MESSAGE_', 'Security violation: ' + e.message)
        return
    } catch (Exception e) {
        request.setAttribute('_ERROR_MESSAGE_', e)
        return
    }
}
/* codenarc-enable ReturnNullFromCatchBlock */
