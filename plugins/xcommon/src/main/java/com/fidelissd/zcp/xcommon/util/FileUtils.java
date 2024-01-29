package com.fidelissd.zcp.xcommon.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.ofbiz.base.util.Debug;

public class FileUtils {
  private static final String module = FileUtils.class.getName();

  /**
   * Copies a file over to a new path.
   *
   * @param newFilePath
   * @param oldFilePath
   * @return
   */
  public static Boolean copyFile(String newFilePath, String oldFilePath) {
    try {
      FileInputStream Fread = new FileInputStream(oldFilePath);
      FileOutputStream Fwrite = new FileOutputStream(newFilePath);
      Debug.logInfo("File copied successfully...", module);
      int c;
      while ((c = Fread.read()) != -1) Fwrite.write((char) c);
      Fread.close();
      Fwrite.close();
      return true;
    } catch (IOException e) {
      Debug.logError(e, module);
      return false;
    }
  }
}
