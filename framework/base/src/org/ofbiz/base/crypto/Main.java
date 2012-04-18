package org.ofbiz.base.crypto;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("-crypt")) {
            System.out.println(HashCrypt.cryptPassword(args[1], args[2]));
        }
    }
}
