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

if (security.hasPermission('ENTITY_MAINT', session)) {
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
            // NOTE: patterns that match a member access ('.name(', 'Type.name', '.name') carry an
            // optional ['"]? around the name so they also match Groovy's quoted-member syntax
            // (obj."name"(), Type."name"()) - a normal language feature that otherwise bypasses a
            // literal-text match.
            /(?s)Runtime\s*\.\s*['"]?getRuntime['"]?\s*\(\s*\)/,
            /(?s)['"]java\.lang\.Runtime['"]\.class/,
            /(?s)Runtime\s*\.\s*class\s*\.\s*['"]?getDeclaredMethod['"]?/,
            /(?s)getRuntime\s*\(\s*\)\.\s*['"]?exec['"]?/,
            /(?s)ProcessBuilder/,
            /(?s)\.\s*['"]?execute['"]?\s*\(/,
            /(?s)System\s*\.\s*['"]?exit['"]?/,
            // Reflection & ClassLoading
            /(?s)Class\s*\.\s*['"]?forName['"]?/,
            /(?s)\.\s*['"]?newInstance['"]?\s*\(/,
            /(?s)\.\s*['"]?getDeclaredMethod['"]?/,
            /(?s)\.\s*['"]?getDeclaredField['"]?/,
            /(?s)\.\s*['"]?getMethod['"]?\s*\(/,
            /(?s)\.\s*['"]?getField['"]?\s*\(/,
            /(?s)\.\s*['"]?invoke['"]?\s*\(/,
            /(?s)\.\s*['"]?loadClass['"]?\s*\(/,
            /(?s)\.\s*['"]?getClassLoader['"]?\s*\(/,
            /(?s)java\s*\.\s*lang\s*\.\s*reflect/,
            /(?s)URLClassLoader/,
            /(?s)GroovyClassLoader/,
            /(?s)ScriptEngineManager/,
            /(?s)javax\s*\.\s*script/,
            /(?s)sun\s*\.\s*misc\s*\.\s*Unsafe/,
            // Eval/GroovyShell Blocking
            /(?s)Eval\s*\.\s*['"]?me['"]?/,
            /(?s)Eval\s*\.\s*['"]?x['"]?/,
            /(?s)Eval\s*\.\s*['"]?xy['"]?/,
            /(?s)Eval\s*\.\s*['"]?xyz['"]?/,
            /(?s)GroovyShell/,
            /(?s)\.\s*['"]?evaluate['"]?\s*\(/,
            // File System Operations
            /(?s)java\s*\.\s*io\s*\.\s*File\s*\(/,
            /(?s)new\s+File\s*\(/,
            /(?s)Files\s*\.\s*['"]?readAllBytes['"]?/,
            /(?s)Paths\s*\.\s*['"]?get['"]?/,
            /(?s)\.\s*['"]?toFile['"]?\s*\(/,
            /(?s)\.\s*['"]?getResourceAsStream['"]?\s*\(/,
            /(?s)\.\s*['"]?getText['"]?\s*\(/,
            /(?s)\.\s*(?:bytes\b|['"]bytes['"])/,
            // Network Operations
            /(?s)Socket\s*\(/,
            /(?s)ServerSocket/,
            /(?s)DatagramSocket/,
            /(?s)InetSocketAddress/,
            /(?s)InetAddress/,
            /(?s)java\s*\.\s*net\s*\./,
            /(?s)URL\s*\(/,
            /(?s)NetworkInterface/,
            /(?s)\.\s*['"]?openConnection['"]?\s*\(/,
            /(?s)\.\s*['"]?connect['"]?\s*\(/,
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
        // Imports and static imports both use disallowed-list (blocklist) mode instead of an
        // allowed-list. setIndirectImportCheckEnabled(true) below makes SecureASTCustomizer check
        // the *receiver type* of every method call, constructor call, and method pointer expression
        // in the script against this same imports configuration - not just against explicit import
        // statements (which the script can never write anyway, since KEYWORD_IMPORT is disallowed
        // below). An allowed-list here would therefore have to include every legitimate type ever
        // used as a receiver (String, Delegator, EntityFindOptions, etc.), and a narrow allowed-list
        // combined with indirect checks rejects every expression in the script, not just imports.
        // Actual receiver-type restriction for method calls is still tightly enforced separately by
        // setAllowedReceivers below; this blocklist only needs to stop indirect FQCN construction
        // (e.g. "new java.lang.ProcessBuilder(...)") of genuinely dangerous classes, matching the
        // pattern already used in org.apache.ofbiz.base.util.GroovyUtil's eval() sandbox.
        // SecureASTCustomizer throws IllegalArgumentException if both an allowed list and a
        // disallowed list are set for the same allowed/disallowed pair, so only the disallowed
        // setters are used for imports, star imports, static imports and static star imports here -
        // do not add any setAllowed(Static)(Star)Imports call alongside these.
        setDisallowedImports([
                'java.lang.Runtime',
                'java.lang.ProcessBuilder',
                'java.lang.ClassLoader',
                'java.lang.Thread',
                'java.lang.reflect.Method',
                'java.lang.reflect.Field',
                'java.net.Socket',
                'java.net.ServerSocket',
                'groovy.lang.GroovyShell',
                'groovy.lang.GroovyClassLoader'
        ])
        setDisallowedStarImports([])
        setDisallowedStaticImports([
                'java.lang.Runtime.getRuntime',
                'java.lang.Runtime.exec',
                'java.lang.System.exit',
                'java.lang.Class.forName'
        ])
        setDisallowedStaticStarImports([])
        setIndirectImportCheckEnabled(true)
        // Constant types whitelist
        setAllowedConstantTypesClasses([
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
        setDisallowedTokens([KEYWORD_PACKAGE, KEYWORD_IMPORT])
        setDisallowedStatements([
                WhileStatement, ForStatement,
                SwitchStatement
        ])
        setDisallowedExpressions([MethodPointerExpression])
        // Receiver whitelist - only safe OFBiz entity operations
        setAllowedReceivers([
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
}
