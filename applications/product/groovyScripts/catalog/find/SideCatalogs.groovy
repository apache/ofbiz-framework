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

state = request.getParameter("BrowseCatalogsState")
isOpen = true
if (state) {
    session.setAttribute("BrowseCatalogsState", state)
    isOpen = "open".equals(state)
} else {
    state = (String) session.getAttribute("BrowseCatalogsState")
    if (state) {
        isOpen = "open".equals(state)
    }
}
context.isOpen = isOpen

//prodCatalogs
if (isOpen) {
    prodCatalogs = from("ProdCatalog").queryList()
    context.prodCatalogs = prodCatalogs
}

//get the current prodCatalogId
curProdCatalogId = request.getParameter("prodCatalogId")
if (curProdCatalogId) {
    session.setAttribute("curProdCatalogId", curProdCatalogId)
} else {
    curProdCatalogId = (String) session.getAttribute("curProdCatalogId")
}
context.curProdCatalogId = curProdCatalogId
