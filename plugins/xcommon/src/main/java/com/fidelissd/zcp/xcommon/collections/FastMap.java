package com.fidelissd.zcp.xcommon.collections;

import java.util.Map;
import org.apache.commons.collections.FastHashMap;

public class FastMap {

  private FastMap(){}

  public static Map newInstance(){
    return new FastHashMap();
  }
}
