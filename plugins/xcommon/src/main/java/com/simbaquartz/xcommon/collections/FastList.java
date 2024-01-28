package com.simbaquartz.xcommon.collections;

import java.util.List;
import org.apache.commons.collections.FastArrayList;

public class FastList {
  private FastList(){}

  public static List newInstance(){
    return new FastArrayList();
  }
}
