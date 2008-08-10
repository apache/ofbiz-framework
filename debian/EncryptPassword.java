import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.common.login.LoginServices;

public class EncryptPassword {
    public static void main(String[] args) {
        String hashType = LoginServices.getHashType();
        for (String arg: args) {
            System.out.println(HashCrypt.getDigestHash(arg, hashType));
        }
    }
}

