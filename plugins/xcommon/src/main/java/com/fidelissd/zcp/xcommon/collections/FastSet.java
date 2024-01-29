package com.fidelissd.zcp.xcommon.collections;

import java.util.HashSet;
import java.util.Set;

public class FastSet {
  private FastSet(){}

  public static Set newInstance(){
    return new HashSet();
  }
}
