package org.apache.ofbiz.product.migrate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class MigrationServices {
    public static final String module = MigrationServices.class.getName();

    public static Map<String, Object> migrateProductPromoCodeEmail(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        List<Object> errors = new LinkedList<>();
        EntityQuery eq = EntityQuery.use(delegator).from("OldProductPromoCodeEmail");

        try (EntityListIterator eli = eq.queryIterator()) {
            GenericValue productPromoCodeEmail;
            while ((productPromoCodeEmail = eli.next()) != null) {
                String contactMechId;

                String emailAddress = productPromoCodeEmail.getString("emailAddress");
                if (!UtilValidate.isEmail(emailAddress)) {
                    Debug.logError(emailAddress + ": is not a valid email address", module);
                    errors.add(emailAddress + ": is not a valid email address ");
                    continue;
                }

                long contactMechs = EntityQuery.use(delegator)
                                        .from("ContactMech")
                                        .where("infoString", emailAddress)
                                        .queryCount();
                if (contactMechs > 1) {
                    errors.add(emailAddress + ": Too many contactMechIds found ");
                    continue;
                }

                GenericValue contactMech = EntityQuery.use(delegator)
                                    .from("ContactMech")
                                    .where("infoString", emailAddress)
                                    .queryOne();
                if (contactMech == null) {
                    //If no contactMech found create new
                    GenericValue newContactMech = delegator.makeValue("ContactMech");
                    contactMechId = delegator.getNextSeqId("ContactMech");
                    newContactMech.set("contactMechId", contactMechId);
                    newContactMech.set("contactMechTypeId", "EMAIL_ADDRESS");
                    newContactMech.set("infoString", emailAddress);
                    delegator.create(newContactMech);
                } else {
                    contactMechId = contactMech.getString("contactMechId");
                }

                GenericValue prodPromoCodeContMech = delegator.makeValue("ProdPromoCodeContactMech");
                prodPromoCodeContMech.set("productPromoCodeId", productPromoCodeEmail.getString("productPromoCodeId"));
                prodPromoCodeContMech.set("contactMechId", contactMechId);
                //createOrStore to avoid duplicate data for same email.
                delegator.createOrStore(prodPromoCodeContMech);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess("Data has been migrated with following errors: " + errors);
    }
}
