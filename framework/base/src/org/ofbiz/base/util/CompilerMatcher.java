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
package org.ofbiz.base.util;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.ofbiz.base.util.cache.UtilCache;

/**
 * Performs regular expression matching and compiled regular expression
 * pattern caching.
 */
public class CompilerMatcher {

    public static final String module = CompilerMatcher.class.getName();

    public static UtilCache<String, Pattern> compiledPatterns = UtilCache.createUtilCache("regularExpression.compiledPatterns", false);

    private Perl5Compiler compiler = new Perl5Compiler();
    private Perl5Matcher matcher = new Perl5Matcher();
    private Perl5Util perl5Util = new Perl5Util();

    /**
     * This class is *not* thread-safe so it must be
     * accessed from a java.lang.ThreadLocal instance for multi-thread usage.
     * ThreadLocal causes slightly extra memory usage, but allows for faster
     * thread-safe processing than synchronization would afford.
     *
     * @return
     */
    public static ThreadLocal<CompilerMatcher> getThreadLocal() {
        return new ThreadLocal<CompilerMatcher>() {

            protected CompilerMatcher initialValue() {
                return new CompilerMatcher();
            }
        };
    }

    /**
     * Returns true if the compiled version of the patternString regular
     * expression argument matches the aString argument.
     * Case sensitive
     *
     * @param aString
     * @param patternString
     * @return
     * @throws MalformedPatternException
     */
    public boolean matches(String aString, String patternString) throws MalformedPatternException {
        return this.matches(aString, patternString, true);
    }

    /**
     * Returns true if the compiled version of the patternString regular
     * expression argument matches the aString argument.
     * @param aString
     * @param patternString
     * @param caseSensitive
     * @return
     * @throws MalformedPatternException
     */
    public boolean matches(String aString, String patternString, boolean caseSensitive) throws MalformedPatternException {
        return this.matcher.matches(aString, this.getTestPattern(patternString, caseSensitive));
    }


    /**
     * Returns true if the compiled version of the patternString regular
     * expression argument is contained in the aString argument.
     *
     * @param aString
     * @param patternString
     * @return
     * @throws MalformedPatternException
     */
    public boolean contains(String aString, String patternString) throws MalformedPatternException {
        return this.matcher.contains(aString, this.getTestPattern(patternString));
    }

    /**
     * Compiles and caches a case sensitive regexp pattern for the given string pattern.
     *
     * @param stringPattern
     * @return
     * @throws MalformedPatternException
     */
    private Pattern getTestPattern(String stringPattern) throws MalformedPatternException {
        return this.getTestPattern(stringPattern, true);
    }

    /**
     * Compiles and caches a regexp pattern for the given string pattern.
     *
     * @param stringPattern
     * @param caseSensitive
     * @return
     * @throws MalformedPatternException
     */
    private Pattern getTestPattern(String stringPattern, boolean caseSensitive) throws MalformedPatternException {
        Pattern pattern = compiledPatterns.get(stringPattern);
        if (pattern == null) {
            if (caseSensitive) {
                pattern = compiler.compile(stringPattern);
            } else {
                pattern = compiler.compile(stringPattern, Perl5Compiler.CASE_INSENSITIVE_MASK);
            }

            compiledPatterns.put(stringPattern, pattern);
            Debug.logVerbose("Compiled and cached a pattern: '" + stringPattern + "' - " + Thread.currentThread(), module);
        }
        return pattern;
    }

    /**
     * Perl5Util's substitute() function implements Perl's s/// operator.
     * It takes two arguments: a substitution expression, and an input.
     *
     * @param stringPattern
     * @param input
     * @return
     */
    public String substitute(String stringPattern, String input) {
        return this.perl5Util.substitute(stringPattern, input);
    }
}
