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
package org.apache.ofbiz.product

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

import java.sql.Timestamp

class ProductTest extends OFBizTestCase {
    public ProductTest(String name) {
        super(name)
    }

    void testCreateProduct() {
        String internalName = 'Test_product'
        String productTypeId = 'Test_type'

        Map serviceCtx = [
                internalName: internalName,
                productTypeId: productTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createProduct', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productId = serviceResult.productId

        GenericValue product = from('Product')
                .where('productId', productId)
                .queryOne()
        assert product
        assert internalName.equals(product.internalName)
        assert productTypeId.equals(product.productTypeId)
    }

    void testUpdateProduct() {
        String productId = 'Test_product_A'
        String productName = 'Test_name_B'
        String description = 'Updated description'

        Map serviceCtx = [
                productId: productId,
                productName: productName,
                description: description,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateProduct', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue product = from('Product')
                .where('productId', productId)
                .queryOne()
        assert product
        assert productName.equals(product.productName)
        assert description.equals(product.description)
    }

    void testDuplicateProduct() {
        String productId = 'Duplicate_Id'
        String oldProductId = 'Test_product_B'

        Map serviceCtx = [
                productId: productId,
                oldProductId: oldProductId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('duplicateProduct', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue product = from('Product')
                .where('productId', productId)
                .queryOne()
        assert product
        assert 'Test_type'.equals(product.productTypeId)
        assert 'Test_name_C'.equals(product.productName)
        assert 'This is product description'.equals(product.description)
    }

    void testQuickAddVariant() {
        String productId = 'Test_product_B'
        String productFeatureIds = 'Test_feature'
        String productVariantId = 'Test_variant'

        Map serviceCtx = [
                productId: productId,
                productFeatureIds: productFeatureIds,
                productVariantId: productVariantId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('quickAddVariant', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue product = from('Product')
                .where('productId', productVariantId)
                .queryOne()
        assert product
        assert 'N'.equals(product.isVirtual)
        assert 'Y'.equals(product.isVariant)
        assert !product.primaryProductCategoryId

        GenericValue productAssoc = from('ProductAssoc')
                .where('productId', productId, 'productIdTo', productVariantId, 'productAssocTypeId', 'PRODUCT_VARIANT')
                .filterByDate().queryFirst()
        assert productAssoc

        GenericValue productFeature = from('ProductFeature')
                .where('productFeatureId', productFeatureIds)
                .queryOne()
        assert productFeature
    }

    void testDeleteProductKeywords() {
        String productId = 'Test_product_C'

        List keywords = from('ProductKeyword')
                .where('productId', productId)
                .queryList()
        assert keywords

        Map serviceCtx = [
                productId: productId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteProductKeywords', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        keywords.clear()
        keywords = from('ProductKeyword')
                .where('productId', productId)
                .queryList()
        //assert keywords == null
        assert !keywords
    }

    void testDiscontinueProductSales() {
        String productId = 'Test_product_C'

        Map serviceCtx = [
                productId: productId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('discontinueProductSales', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue product = from('Product')
                .where('productId', productId)
                .queryOne()
        assert product
        assert product.salesDiscontinuationDate
    }

    void testCreateProductReview() {
        String productId = 'Test_product_C'
        String productStoreId = 'Test_store'
        BigDecimal productRating = new BigDecimal('5')
        String productReview = 'Test review'

        Map serviceCtx = [
                productId: productId,
                productStoreId: productStoreId,
                productRating: productRating,
                productReview: productReview,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createProductReview', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue review = from('ProductReview')
                .where('productReviewId', serviceResult.productReviewId)
                .queryOne()
        assert productReview
        assert productId.equals(review.productId)
        assert productStoreId.equals(review.productStoreId)
        assert productReview.equals(review.productReview)
        assert productRating.compareTo(review.productRating) == 0
    }

    void testUpdateProductReview() {
        String productReviewId = 'Test_review'
        BigDecimal productRating = new BigDecimal('3')
        String productReview = 'Updated review'

        Map serviceCtx = [
                productReviewId: productReviewId,
                productRating: productRating,
                productReview: productReview,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateProductReview', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue review = from('ProductReview')
                .where('productReviewId', productReviewId)
                .queryOne()
        assert productReview
        assert productReview.equals(review.productReview)
        assert productRating.compareTo(review.productRating) == 0
    }

    void testFindProductById() {
        Map serviceCtx = [
                idToFind: 'Test_product_C',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findProductById', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.product
    }

    void testCreateProductPrice() {
        String productId = 'Test_product_A'
        String productPriceTypeId = 'AVERAGE_COST'
        String productPricePurposeId = 'COMPONENT_PRICE'
        String productStoreGroupId = 'Test_group'
        String currencyUomId = 'USD'
        BigDecimal price = new BigDecimal('30')
        Timestamp fromDate = UtilDateTime.toTimestamp("04/07/2013 00:00:00")

        Map serviceCtx = [
                productId: productId,
                productPriceTypeId: productPriceTypeId,
                productPricePurposeId: productPricePurposeId,
                productStoreGroupId: productStoreGroupId,
                currencyUomId: currencyUomId,
                price: price,
                fromDate: fromDate,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createProductPrice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue productPrice = from('ProductPrice')
                .where('productId', productId,
                              'productPriceTypeId', productPriceTypeId,
                              'productPricePurposeId', productPricePurposeId,
                              'productStoreGroupId', productStoreGroupId,
                              'currencyUomId', currencyUomId,
                              'fromDate', fromDate)
                .queryOne()
        assert productPrice
        assert price.compareTo(productPrice.price) == 0
    }

    void testUpdateProductPrice() {
        String productId = 'Test_prod_price_up'
        String productPriceTypeId = 'AVERAGE_COST'
        String productPricePurposeId = 'COMPONENT_PRICE'
        String productStoreGroupId = 'Test_group'
        String currencyUomId = 'USD'
        BigDecimal price = new BigDecimal('50')
        Timestamp fromDate = UtilDateTime.toTimestamp("07/04/2013 00:00:00")

        Map serviceCtx = [
                productId: productId,
                internalName: 'Test update product price',
                productTypeId: 'Test_type',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createProduct', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                productId: productId,
                productPriceTypeId: productPriceTypeId,
                productPricePurposeId: productPricePurposeId,
                productStoreGroupId: productStoreGroupId,
                currencyUomId: currencyUomId,
                price: new BigDecimal('30'),
                fromDate: fromDate,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('createProductPrice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceResult.clear()
        serviceCtx.price = price
        serviceResult = dispatcher.runSync('updateProductPrice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert price.compareTo(serviceResult.oldPrice) != 0

        GenericValue productPrice = from('ProductPrice')
                .where('productId', productId,
                'productPriceTypeId', productPriceTypeId,
                'productPricePurposeId', productPricePurposeId,
                'productStoreGroupId', productStoreGroupId,
                'currencyUomId', currencyUomId,
                'fromDate', fromDate)
                .queryOne()
        assert productPrice
        assert productPrice.price
        assert price.compareTo(productPrice.price) == 0
    }

    void testDeleteProductPrice() {
        String productId = 'Test_product_C'
        String productPriceTypeId = 'AVERAGE_COST'
        String productPricePurposeId = 'COMPONENT_PRICE'
        String productStoreGroupId = 'Test_group'
        String currencyUomId = 'USD'
        Timestamp fromDate = UtilDateTime.toTimestamp("07/04/2013 00:00:00")

        Map serviceCtx = [
                productId: productId,
                productPriceTypeId: productPriceTypeId,
                productPricePurposeId: productPricePurposeId,
                productStoreGroupId: productStoreGroupId,
                currencyUomId: currencyUomId,
                fromDate: fromDate,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteProductPrice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue productPrice = from('ProductPrice')
                .where('productId', productId,
                'productPriceTypeId', productPriceTypeId,
                'productPricePurposeId', productPricePurposeId,
                'productStoreGroupId', productStoreGroupId,
                'currencyUomId', currencyUomId,
                'fromDate', fromDate)
                .queryOne()
        assert !productPrice
    }

    void testCreateProductCategory() {
        String productCategoryId = 'TEST_CATEGORY'
        String productCategoryTypeId = 'USAGE_CATEGORY'

        Map serviceCtx = [
                productCategoryId: productCategoryId,
                productCategoryTypeId: productCategoryTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createProductCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue productCategory = from('ProductCategory')
                .where('productCategoryId', productCategoryId)
                .queryOne()
        assert productCategory
        assert 'USAGE_CATEGORY'.equals(productCategory.productCategoryTypeId)
    }
}