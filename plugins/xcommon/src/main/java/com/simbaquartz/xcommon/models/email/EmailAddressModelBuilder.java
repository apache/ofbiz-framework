package com.simbaquartz.xcommon.models.email;

import com.simbaquartz.xcommon.collections.FastList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Created by mande on 1/3/2022.
 */
public class EmailAddressModelBuilder {

  public static List<EmailAddress> build(List<Map> emailAddressMaps){
      List<EmailAddress> emailList = FastList.newInstance();
      for (Map emailAddress : emailAddressMaps) {
        EmailAddress email = new EmailAddress();
        if (UtilValidate.isNotEmpty(emailAddress.get("email"))) {
          email.setEmailAddress((String) emailAddress.get("email"));
        }
        if (UtilValidate.isNotEmpty(emailAddress.get("contactMechId"))) {
          email.setId((String) emailAddress.get("contactMechId"));
        }
        emailList.add(email);
      }
      return emailList;

  }
}
