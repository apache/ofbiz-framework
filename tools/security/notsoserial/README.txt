The notsoserial Java agent was introduced to protect your OFBiz instance from the infamous Java serialize vulnerability if you use RMI, JMX or Spring and maybe other Java classes we don't use OOTB in OFBiz.
We (PMC) decided to comment out RMI OOTB but we also decided to provide a simple way to protect yourself from all possible Java serialize vulnerabilities.

While working on the serialize vulnerability, I (Jacques Le Roux) stumbled upon this article https://tersesystems.com/2015/11/08/closing-the-open-door-of-java-object-serialization/ and found notsoserial was a Java agent better than the Contrast one I introduced at r1717058. Because notsoserial easily protects you from all possible serialize vulnerabilities as explained at https://github.com/kantega/notsoserial#rejecting-deserialization-entirely
So I replaced contrast-rO0.jar by notsoserial-1.0-SNAPSHOT at r1730735 + r1730736. To be safe in case you use RMI for instance, use one of the start*-secure ant targets or use the JVM arguments those targets use.

You might find more information at https://cwiki.apache.org/confluence/display/OFBIZ/The+infamous+Java+serialize+vulnerability