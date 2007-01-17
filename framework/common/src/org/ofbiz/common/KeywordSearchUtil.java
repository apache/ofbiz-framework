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
package org.ofbiz.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * A few utility methods related to Keyword Search.
 */
public class KeywordSearchUtil {

    public static final String module = KeywordSearchUtil.class.getName();

    public static Set thesaurusRelsToInclude = new HashSet();
    public static Set thesaurusRelsForReplace = new HashSet();

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

    public static String getSeparators() {
        // String separators = ";: ,.!?\t\"\'\r\n\\/()[]{}*%<>-+_";
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
    public static Set getStemSet() {
        String stemBag = UtilProperties.getPropertyValue("keywordsearch", "stem.bag");
        Set stemSet = new TreeSet();
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
    
    public static void processForKeywords(String str, Map keywords, boolean forSearch, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        String separators = getSeparators();
        String stopWordBagOr = getStopWordBagOr();
        String stopWordBagAnd = getStopWordBagAnd();

        boolean removeStems = getRemoveStems();
        Set stemSet = getStemSet();
        
        processForKeywords(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet, forSearch, anyPrefix, anySuffix, isAnd);
    }
    
    public static void processKeywordsForIndex(String str, Map keywords, String separators, String stopWordBagAnd, String stopWordBagOr, boolean removeStems, Set stemSet) {
        processForKeywords(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet, false, false, false, false);
    }

    public static void processForKeywords(String str, Map keywords, String separators, String stopWordBagAnd, String stopWordBagOr, boolean removeStems, Set stemSet, boolean forSearch, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        Set keywordSet = makeKeywordSet(str, separators, forSearch);
        fixupKeywordSet(keywordSet, keywords, stopWordBagAnd, stopWordBagOr, removeStems, stemSet, forSearch, anyPrefix, anySuffix, isAnd);
    }
    
    public static void fixupKeywordSet(Set keywordSet, Map keywords, String stopWordBagAnd, String stopWordBagOr, boolean removeStems, Set stemSet, boolean forSearch, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        if (keywordSet == null) {
            return;
        }
        
        Iterator keywordIter = keywordSet.iterator();
        while (keywordIter.hasNext()) {
            String token = (String) keywordIter.next();
            
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
                Iterator stemIter = stemSet.iterator();
                while (stemIter.hasNext()) {
                    String stem = (String) stemIter.next();
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
                StringBuffer strSb = new StringBuffer();
                if (anyPrefix) strSb.append('%');
                strSb.append(token);
                if (anySuffix) strSb.append('%');
                // replace all %% with %
                int dblPercIdx = -1;
                while ((dblPercIdx = strSb.indexOf("%%")) >= 0) {
                    //Debug.logInfo("before strSb: " + strSb, module);
                    strSb.replace(dblPercIdx, dblPercIdx+2, "%");
                    //Debug.logInfo("after strSb: " + strSb, module);
                }
                token = strSb.toString();
            }
            
            // group by word, add up weight
            Long curWeight = (Long) keywords.get(token);
            if (curWeight == null) {
                keywords.put(token, new Long(1));
            } else {
                keywords.put(token, new Long(curWeight.longValue() + 1));
            }
        }
    }

    public static Set makeKeywordSet(String str, String separators, boolean forSearch) {
        if (separators == null) separators = getSeparators();
        
        Set keywords = new TreeSet();
        if (str.length() > 0) {
            if (forSearch) {
                // remove %_*? from separators if is for a search
                StringBuffer sb = new StringBuffer(separators);
                if (sb.indexOf("%") >= 0) sb.deleteCharAt(sb.indexOf("%"));
                if (sb.indexOf("_") >= 0) sb.deleteCharAt(sb.indexOf("_"));
                if (sb.indexOf("*") >= 0) sb.deleteCharAt(sb.indexOf("*"));
                if (sb.indexOf("?") >= 0) sb.deleteCharAt(sb.indexOf("?"));
                separators = sb.toString();
            }
            
            StringTokenizer tokener = new StringTokenizer(str, separators, false);
            while (tokener.hasMoreTokens()) {
                // make sure it is lower case before doing anything else
                String token = tokener.nextToken().toLowerCase();

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
    
    public static Set fixKeywordsForSearch(Set keywordSet, boolean anyPrefix, boolean anySuffix, boolean removeStems, boolean isAnd) {
        Map keywords = new HashMap();
        fixupKeywordSet(keywordSet, keywords, getStopWordBagAnd(), getStopWordBagOr(), removeStems, getStemSet(), true, anyPrefix, anySuffix, isAnd);
        return keywords.keySet();
    }

    public static boolean expandKeywordForSearch(String enteredKeyword, Set addToSet, GenericDelegator delegator) {
        boolean replaceEnteredKeyword = false;

        try {
            List thesaurusList = delegator.findByAndCache("KeywordThesaurus", UtilMisc.toMap("enteredKeyword", enteredKeyword));
            Iterator thesaurusIter = thesaurusList.iterator();
            while (thesaurusIter.hasNext()) {
                GenericValue keywordThesaurus = (GenericValue) thesaurusIter.next();
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
