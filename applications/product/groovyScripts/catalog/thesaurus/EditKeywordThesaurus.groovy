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

import org.ofbiz.entity.condition.*

relationshipEnums = from("Enumeration").where("enumTypeId", "KW_THES_REL").orderBy("sequenceId").cache(true).queryList();

keywordThesauruses = from("KeywordThesaurus").orderBy("enteredKeyword").queryList();

//if no param sent in make firstLetter 'a' else use firstLetter passed in
firstLetterString = request.getParameter("firstLetter");
if (!firstLetterString) {
    firstLetter = 'a';
}
else {
    firstLetter = firstLetterString.charAt(0);
}

//add elememts to new list as long as it is smaller then 20,
//  but always get all of the first letter
keywordThesaurusIter = keywordThesauruses.iterator();
newKeywordThesaurus = [];
specialCharKeywordThesaurus = [];
currentLetter = firstLetter;
if (keywordThesaurusIter) {
    while (keywordThesaurusIter) {
        keywordThesaurus = keywordThesaurusIter.next();
        if (keywordThesaurus.get("enteredKeyword").charAt(0)<'a' ||
                keywordThesaurus.get("enteredKeyword").charAt(0)>'z') {
            specialCharKeywordThesaurus.add(keywordThesaurus);
        } else if (keywordThesaurus.get("enteredKeyword").charAt(0) >= firstLetter) {
            if (keywordThesaurus.get("enteredKeyword").charAt(0) == currentLetter ||
                    newKeywordThesaurus.size()<20) {
                newKeywordThesaurus.add(keywordThesaurus);
                currentLetter = keywordThesaurus.get("enteredKeyword").charAt(0);
            }
        }
    }
}
if ((specialCharKeywordThesaurus.size() > 0 && newKeywordThesaurus.size()<20) || firstLetter=='z') {
    specialCharKeywordThesaurusIter = specialCharKeywordThesaurus.iterator();
    while (specialCharKeywordThesaurusIter) {
        keywordThesaurus = specialCharKeywordThesaurusIter.next();
        newKeywordThesaurus.add(keywordThesaurus);
    }
}

//create list for a-z
letterList = [];
for (i='a'; i<='z'; i++) {
    letterList.add(i);
}

context.relationshipEnums = relationshipEnums;
context.keywordThesauruses = newKeywordThesaurus;
context.firstLetter = firstLetter;
context.letterList = letterList;
