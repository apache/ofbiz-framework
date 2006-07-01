
// - BSF JavaScript test script

x = bsf.lookupBean("message");
m = bsf.lookupBean("response");
m.put("result", x);

bsf.registerBean("response", m);
java.lang.System.out.println("BSF - JS: " + x);
