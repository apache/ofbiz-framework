productCategoryId = parameters.productCategoryId;
if (productCategoryId) {
    productCategoryContents  = delegator.findByAnd("ProductCategoryContent", ["productCategoryId" : productCategoryId]);
    productCategoryContents.each{ productCategoryContent->
        if (productCategoryContent.prodCatContentTypeId == "PAGE_TITLE") {
            contentTitle  = delegator.findOne("Content", ["contentId" : productCategoryContent.contentId], false);
            dataTextTitle  = delegator.findOne("ElectronicText", ["dataResourceId" : contentTitle.dataResourceId], false);
            context.title = dataTextTitle.textData;
        }
        if (productCategoryContent.prodCatContentTypeId == "META_KEYWORD") {
            contentMetaKeyword  = delegator.findOne("Content", ["contentId" : productCategoryContent.contentId], false);
            dataTextMetaKeyword  = delegator.findOne("ElectronicText", ["dataResourceId" : contentMetaKeyword.dataResourceId], false);
            context.metaKeyword = dataTextMetaKeyword.textData;
        }
        if (productCategoryContent.prodCatContentTypeId == "META_DESCRIPTION") {
            contentMetaDescription  = delegator.findOne("Content", ["contentId" : productCategoryContent.contentId], false);
            dataTextMetaDescription  = delegator.findOne("ElectronicText", ["dataResourceId" : contentMetaDescription.dataResourceId], false);
            context.metaDescription = dataTextMetaDescription.textData;
        }
    }
}