productId = parameters.productId;
if (productId) {
    productContents  = delegator.findByAnd("ProductContent", ["productId" : productId]);
    productContents.each{ productContent->
        if (productContent.productContentTypeId == "PAGE_TITLE") {
            contentTitle  = delegator.findOne("Content", ["contentId" : productContent.contentId], false);
            dataTextTitle  = delegator.findOne("ElectronicText", ["dataResourceId" : contentTitle.dataResourceId], false);
            context.title = dataTextTitle.textData;
        }
        if (productContent.productContentTypeId == "META_KEYWORD") {
            contentMetaKeyword  = delegator.findOne("Content", ["contentId" : productContent.contentId], false);
            dataTextMetaKeyword  = delegator.findOne("ElectronicText", ["dataResourceId" : contentMetaKeyword.dataResourceId], false);
            context.metaKeyword = dataTextMetaKeyword.textData;
        }
        if (productContent.productContentTypeId == "META_DESCRIPTION") {
            contentMetaDescription  = delegator.findOne("Content", ["contentId" : productContent.contentId], false);
            dataTextMetaDescription  = delegator.findOne("ElectronicText", ["dataResourceId" : contentMetaDescription.dataResourceId], false);
            context.metaDescription = dataTextMetaDescription.textData;
        }
    }
}