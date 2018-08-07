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
package org.apache.ofbiz.common;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * A few utility methods related to Keyword Search.
 */
public final class KeywordSearchUtil {

    public static final String module = KeywordSearchUtil.class.getName();

    private static Set<String> thesaurusRelsToInclude = new HashSet<>();
    private static Set<String> thesaurusRelsForReplace = new HashSet<>();

    static {
        thesaurusRelsToInclude.add("KWTR_UF");
        thesaurusRelsToInclude.add("KWTR_USE");
        thesaurusRelsToInclude.add("KWTR_CS");
        thesaurusRelsToInclude.add("KWTR_NT");
        thesaurusRelsToInclude.add("KWTR_BT");
        thesaurusRelsToInclude.add("KWTR_RT");

        thesaurusRelsForReplace.add("KWTR_USE");
        thesaurusRelsForReplace.add("KWTR_CS");
    }

    private KeywordSearchUtil () {}

    public static String getSeparators() {
        String seps = UtilProperties.getPropertyValue("keywordsearch", "index.keyword.separators", ";: ,.!?\t\"\'\r\n\\/()[]{}*%<>-+_");
        return seps;
    }

    public static String getStopWordBagOr() {
        return UtilProperties.getPropertyValue("keywordsearch", "stop.word.bag.or");
    }
    public static String getStopWordBagAnd() {
        return UtilProperties.getPropertyValue("keywordsearch", "stop.word.bag.and");
    }

    public static boolean getRemoveStems() {
        String removeStemsStr = UtilProperties.getPropertyValue("keywordsearch", "remove.stems");
        return "true".equals(removeStemsStr);
    }
    public static Set<String> getStemSet() {
        String stemBag = UtilProperties.getPropertyValue("keywordsearch", "stem.bag");
        Set<String> stemSet = new TreeSet<>();
        if (UtilValidate.isNotEmpty(stemBag)) {
            String curToken;
            StringTokenizer tokenizer = new StringTokenizer(stemBag, ": ");
            while (tokenizer.hasMoreTokens()) {
                curToken = tokenizer.nextToken();
                stemSet.add(curToken);
            }
        }
        return stemSet;
    }

    public static void processForKeywords(String str, Map<String, Long> keywords, boolean forSearch, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        String separators = getSeparators();
        String stopWordBagOr = getStopWordBagOr();
        String stopWordBagAnd = getStopWordBagAnd();

        boolean removeStems = getRemoveStems();
        Set<String> stemSet = getStemSet();

        processForKeywords(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet, forSearch, anyPrefix, anySuffix, isAnd);
    }

    public static void processKeywordsForIndex(String str, Map<String, Long> keywords, String separators, String stopWordBagAnd, String stopWordBagOr, boolean removeStems, Set<String> stemSet) {
        processForKeywords(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet, false, false, false, false);
    }

    public static void processForKeywords(String str, Map<String, Long> keywords, String separators, String stopWordBagAnd, String stopWordBagOr, boolean removeStems, Set<String> stemSet, boolean forSearch, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        Set<String> keywordSet = makeKeywordSet(str, separators, forSearch);
        fixupKeywordSet(keywordSet, keywords, stopWordBagAnd, stopWordBagOr, removeStems, stemSet, forSearch, anyPrefix, anySuffix, isAnd);
    }

    public static void fixupKeywordSet(Set<String> keywordSet, Map<String, Long> keywords, String stopWordBagAnd, String stopWordBagOr, boolean removeStems, Set<String> stemSet, boolean forSearch, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        if (keywordSet == null) {
            return;
        }

        for (String token: keywordSet) {

            // when cleaning up the tokens the ordering is inportant: check stop words, remove stems, then get rid of 1 character tokens (1 digit okay)

            // check stop words
            String colonToken = ":" + token + ":";
            if (forSearch) {
                if ((isAnd && stopWordBagAnd.indexOf(colonToken) >= 0) || (!isAnd && stopWordBagOr.indexOf(colonToken) >= 0)) {
                    continue;
                }
            } else {
                if (stopWordBagOr.indexOf(colonToken) >= 0 && stopWordBagAnd.indexOf(colonToken) >= 0) {
                    continue;
                }
            }

            // remove stems
            if (removeStems) {
                for (String stem: stemSet) {
                    if (token.endsWith(stem)) {
                        token = token.substring(0, token.length() - stem.length());
                    }
                }
            }

            // get rid of all length 0 tokens now
            if (token.length() == 0) {
                continue;
            }

            // get rid of all length 1 character only tokens, pretty much useless
            if (token.length() == 1 && Character.isLetter(token.charAt(0))) {
                continue;
            }

            if (forSearch) {
                StringBuilder strSb = new StringBuilder();
                if (anyPrefix) {
                    strSb.append('%');
                }
                strSb.append(token);
                if (anySuffix) {
                    strSb.append('%');
                }
                // replace all %% with %
                int dblPercIdx = -1;
                while ((dblPercIdx = strSb.indexOf("%%")) >= 0) {
                    strSb.replace(dblPercIdx, dblPercIdx+2, "%");
                }
                token = strSb.toString();
            }

            // group by word, add up weight
            Long curWeight = keywords.get(token);
            if (curWeight == null) {
                keywords.put(token, 1L);
            } else {
                keywords.put(token, curWeight + 1);
            }
        }
    }

    public static Set<String> makeKeywordSet(String str, String separators, boolean forSearch) {
        if (separators == null) {
            separators = getSeparators();
        }

        Set<String> keywords = new TreeSet<>();
        if (str.length() > 0) {
            // strip off weird characters
            str = str.replaceAll("\\\302\\\240|\\\240", " ");

            if (forSearch) {
                // remove %_*? from separators if is for a search
                StringBuilder sb = new StringBuilder(separators);
                if (sb.indexOf("%") >= 0) {
                    sb.deleteCharAt(sb.indexOf("%"));
                }
                if (sb.indexOf("_") >= 0) {
                    sb.deleteCharAt(sb.indexOf("_"));
                }
                if (sb.indexOf("*") >= 0) {
                    sb.deleteCharAt(sb.indexOf("*"));
                }
                if (sb.indexOf("?") >= 0) {
                    sb.deleteCharAt(sb.indexOf("?"));
                }
                separators = sb.toString();
            }

            StringTokenizer tokener = new StringTokenizer(str, separators, false);
            while (tokener.hasMoreTokens()) {
                // make sure it is lower case before doing anything else
                String token = tokener.nextToken().toLowerCase(Locale.getDefault());

                if (forSearch) {
                    // these characters will only be present if it is for a search, ie not for indexing
                    token = token.replace('*', '%');
                    token = token.replace('?', '_');
                }

                keywords.add(token);
            }
        }
        return keywords;
}

    public static Set<String> fixKeywordsForSearch(Set<String> keywordSet, boolean anyPrefix, boolean anySuffix, boolean removeStems, boolean isAnd) {
        Map<String, Long> keywords = new LinkedHashMap<>();
        fixupKeywordSet(keywordSet, keywords, getStopWordBagAnd(), getStopWordBagOr(), removeStems, getStemSet(), true, anyPrefix, anySuffix, isAnd);
        return keywords.keySet();
    }

    public static boolean expandKeywordForSearch(String enteredKeyword, Set<String> addToSet, Delegator delegator) {
        boolean replaceEnteredKeyword = false;

        try {
            List<GenericValue> thesaurusList = EntityQuery.use(delegator).from("KeywordThesaurus").where("enteredKeyword", enteredKeyword).cache(true).queryList();
            for (GenericValue keywordThesaurus: thesaurusList) {
                String relationshipEnumId = (String) keywordThesaurus.get("relationshipEnumId");
                if (thesaurusRelsToInclude.contains(relationshipEnumId)) {
                    addToSet.addAll(makeKeywordSet(keywordThesaurus.getString("alternateKeyword"), null, true));
                    if (thesaurusRelsForReplace.contains(relationshipEnumId)) {
                        replaceEnteredKeyword = true;
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error expanding entered keyword", module);
        }

        Debug.logInfo("Expanded keyword [" + enteredKeyword + "], got set: " + addToSet, module);
        return replaceEnteredKeyword;
    }
}
