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

import org.ofbiz.product.catalog.*;

catalogId = CatalogWorker.getCurrentCatalogId(request);
promoCat = CatalogWorker.getCatalogPromotionsCategoryId(request, catalogId);
request.setAttribute("productCategoryId", promoCat);

/* NOTE DEJ20070220 woah, this is doing weird stuff like always showing the last viewed category when going to the main page;
 * It appears this was done for to make it go back to the desired category after logging in, but this is NOT the place to do that,
 * and IMO this is an unacceptable side-effect.
 *
 * The whole thing should be re-thought, and should preferably NOT use a custom session variable or try to go through the main page.
 *
 * NOTE: see section commented out in Category.groovy for the other part of this.
 *
 * NOTE JLR 20070221 this should be done using the same method than in add to cart. I will do it like that and remove all this after.
 *
productCategoryId = session.getAttribute("productCategoryId");
if (!productCategoryId) {
    request.setAttribute("productCategoryId", promoCat);
} else {
    request.setAttribute("productCategoryId", productCategoryId);
}
*/
