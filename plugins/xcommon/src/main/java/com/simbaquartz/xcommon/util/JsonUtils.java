package com.simbaquartz.xcommon.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtils {

  /**
   * Converts an object into JSON formatted string.
   * @param objectToConvert
   * @return
   */
  public static String toJson(Object objectToConvert){
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(objectToConvert);
  }

}
