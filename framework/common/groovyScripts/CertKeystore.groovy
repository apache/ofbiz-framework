import java.io.IOException
import java.util.ArrayList
import java.util.Collection
import java.util.List

import org.apache.ofbiz.base.component.ComponentConfig
import org.apache.ofbiz.base.component.ComponentConfig.KeystoreInfo
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.base.util.KeyStoreUtil

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.util.*

cert = org.apache.ofbiz.base.util.KeyStoreUtil.pemToCert(certString)
if (cert){
    context.certType = cert.getType()
    context.certName = cert.getSubjectX500Principal().getName()
    context.certSerialNumber = cert.getSerialNumber().toString(16)
    context.certPublicKey = cert.getPublicKey()
}

stores = []
store = []
Collection<ComponentConfig> allComponentConfigs = ComponentConfig.getAllComponents()
for (ComponentConfig cc: allComponentConfigs) {
    if (cc.getKeystoreInfos()){
        componentName = cc.getComponentName()
        store = ["componentId" : componentName]
        store.componentName = componentName
        for (KeystoreInfo ks : cc.getKeystoreInfos()) {keystoreName = ks.getName()
            store.keystoreName = ks.getName()
            }
        stores.add(store)
    }
    
}
context.stores = stores
