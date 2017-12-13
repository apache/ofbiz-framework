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
package org.apache.ofbiz.base.util;

import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;

/**
 * A RegEx compiled pattern factory.
 *
 */
public class PatternFactory {

    public static final String module = PatternFactory.class.getName();
    private static final UtilCache<String, Pattern> compiledPerl5Patterns = UtilCache.createUtilCache("regularExpression.compiledPerl5Patterns", false);

    /**
     * Compiles and caches a Perl5 regexp pattern for the given string pattern.
     * This would be of no benefits (and may bloat memory usage) if stringPattern is never the same.
     * @param stringPattern a Perl5 pattern string
     * @param caseSensitive case sensitive true/false
     * @return a <code>Pattern</code> instance for the given string pattern
     * @throws MalformedPatternException
     */

    public static Pattern createOrGetPerl5CompiledPattern(String stringPattern, boolean caseSensitive) throws MalformedPatternException {
        Pattern pattern = compiledPerl5Patterns.get(stringPattern);
        if (pattern == null) {
            Perl5Compiler compiler = new Perl5Compiler();
            if (caseSensitive) {
                pattern = compiler.compile(stringPattern, Perl5Compiler.READ_ONLY_MASK); // READ_ONLY_MASK guarantees immutability
            } else {
                pattern = compiler.compile(stringPattern, Perl5Compiler.CASE_INSENSITIVE_MASK | Perl5Compiler.READ_ONLY_MASK);
            }
            pattern = compiledPerl5Patterns.putIfAbsentAndGet(stringPattern, pattern);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Compiled and cached the pattern: '" + stringPattern, module);
            }
        }
        return pattern;
    }
}
