<img src="http://ofbiz.apache.org/images/logo.png" alt="Apache OFBiz" />

# PriCat component
PriCat is the abbreviation of Price and Catalog/Category. The PriCat component is to support importing/parsing excel files with price and catalog/category data. The excel files can be checked by version, header column names, currencyId. Each row can be validated by facility(name, Id and ownership), required fields, string or number and etc.

PriCat component contains two webapps: /pricat/ and /pricatdemo/. In production environment, you SHOULD remove or disable the /pricatdemo/.

## more information
---------------------------------------
PriCat Demos
---------------------------------------
/pricatdemo/control/SamplePricat/: you can use this demo to implement your own excel templates.

/pricatdemo/control/countdownreport and /pricatdemo/control/countupreport: these 2 demos are on html report, you can try this way to display the processing report of rebuilding of lucene index or marchine learning data.