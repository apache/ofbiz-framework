package org.apache.ofbiz.base.util.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.ofbiz.base.util.UtilXml;

public class ServerTableEntryTest {

    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        FileInputStream fis=new FileInputStream("C:\\payload_ofbiz.txt");
        Reader reader=new InputStreamReader(fis);
        int temp=-1;
        StringBuilder sb=new StringBuilder();
        while((temp=reader.read())!=-1) {
            sb.append((char)temp);
            temp=-1;
        }
        String str=sb.toString();
        //System.out.println(str);
        fis.close();
        UtilXml.fromXml(str);
    }
}
