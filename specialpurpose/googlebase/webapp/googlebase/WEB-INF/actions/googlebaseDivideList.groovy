import java.util.List;
import javolution.util.FastList;

//productList.unique() like distinct sql command, return string type.
def productUniqueStr = productList.unique();
def productUniqueStrList = productUniqueStr.toList();
def googleBaseList = delegator.findByAnd("GoodIdentification",["goodIdentificationTypeId":"GOOGLE_ID_" + productStore.defaultLocaleString], null, false);
//find product is existed in google base.
def notNeededList = productUniqueStrList - googleBaseList.productId;
def resultList = productUniqueStrList - notNeededList;
def list = [];
def productExportList = [];
//if feed more than 1000 always found an IO error, so should divide to any sections.
def amountPerSection = 1000;
def section = (int)(resultList.size()/amountPerSection);
if(resultList.size() % amountPerSection != 0){
	section = section+1;
}

for(int i=0; i<section; i++){
	if(!(i == (section-1))){
		productExportList.add(resultList.subList((i*amountPerSection), ((i+1)*amountPerSection)));
	} else {
		productExportList.add(resultList.subList((i*amountPerSection), resultList.size()));
	}
}
context.productExportLists = productExportList
