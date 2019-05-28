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
package org.apache.ofbiz.base.html;

import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Based on the
 * <a href="http://www.owasp.org/index.php/Category:OWASP_AntiSamy_Project#Stage_2_-_Choosing_a_base_policy_file">AntiSamy Slashdot example</a>.
 * Slashdot (http://www.slashdot.org/) is a techie news site that allows users
 * to respond anonymously to news posts with very limited HTML markup. Now
 * Slashdot is not only one of the coolest sites around, it's also one that's
 * been subject to many different successful attacks. Even more unfortunate is
 * the fact that most of the attacks led users to the infamous goatse.cx picture
 * (please don't go look it up). The rules for Slashdot are fairly strict: users
 * can only submit the following HTML tags and no CSS: {@code <b>}, {@code <u>},
 * {@code <i>}, {@code <a>}, {@code <blockquote>}.
 * 
 * Accordingly, we've built a policy file that allows fairly similar
 * functionality. All text-formatting tags that operate directly on the font,
 * color or emphasis have been allowed.
 */
public class CustomSafePolicy implements SanitizerCustomPolicy {

    /**
     * A policy that can be used to produce policies that sanitize to HTML sinks via
     * {@link PolicyFactory#apply}.
     */
    public static final PolicyFactory POLICY_DEFINITION = new HtmlPolicyBuilder()
            .allowStandardUrlProtocols()
            // Allow title="..." on any element.
            .allowAttributes("title").globally()
            // Allow href="..." on <a> elements.
            .allowAttributes("href").onElements("a")
            // Defeat link spammers.
            .requireRelNofollowOnLinks()
            // Allow lang= with an alphabetic value on any element.
            .allowAttributes("lang").matching(Pattern.compile("[a-zA-Z]{2,20}"))
                .globally()
            // The align attribute on <p> elements can have any value below.
            .allowAttributes("align")
                .matching(true, "center", "left", "right", "justify", "char")
                .onElements("p")
            // These elements are allowed.
            .allowElements("a", "p", "div", "i", "b", "em", "blockquote", "tt", "strong","br", "ul", "ol", "li")
            .toFactory();
    
    @Override
    public PolicyFactory getSanitizerPolicy() {
        return POLICY_DEFINITION;
    }
}
