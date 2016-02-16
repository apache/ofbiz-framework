NotSoSerial
================

[![Build Status](https://travis-ci.org/kantega/notsoserial.svg)](https://travis-ci.org/kantega/notsoserial)

NotSoSerial is a Java Agent designed as a mitigation effort against deserialization attacks.

Think of it as a "deserialization firewall". It gives you complete control over which classes your application should be allowed to deserialize.

See http://foxglovesecurity.com/2015/11/06/what-do-weblogic-websphere-jboss-jenkins-opennms-and-your-application-have-in-common-this-vulnerability/ for details on this attack.

## How does it work?
 
NotSoSerial makes some well known vulnerable classes effectively non-deserializable by preventing them from loading.

It does so by adding a check just before the call to ObjectInputStream.resolveClass. If the class is not allowed, an UnsupportedOperationException is called instead of calling resolveClass.

This means the class never even gets loaded.

## Usage

Build NotSoSerial:

    mvn clean install

This builds an NotSoSerial jar file in target/notsoserial-1.0-SNAPSHOT.jar

Copy this as notsoserial.jar to your application, and add the following parameters to your Java startup script:

    -javaagent:notsoserial.jar

> PLEASE NOTE: In this mode, NotSoSerial only blocks a few known vulnerabilities. It does not fix the problem with deserialization attacks. It only knows about some well known classes for which it rejects deserialization. See below how you can whitelist or completely reject any objects to be deserialized.


## Which classes are rejected?

By default, NotSoSerial rejects deserialization of the following classes:

* org.apache.commons.collections.functors.InvokerTransformer
* org.apache.commons.collections.functors.InstantiateTransformer
* org.apache.commons.collections4.functors.InvokerTransformer
* org.apache.commons.collections4.functors.InstantiateTransformer
* org.codehaus.groovy.runtime.ConvertedClosure
* org.codehaus.groovy.runtime.MethodClosure
* org.springframework.beans.factory.ObjectFactory
* com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl

You can add your own classes blacklist by configuring a blacklist file like this:

    -javaagent:notsoserial.jar -Dnotsoserial.blacklist=my-blacklist.txt

Where my-blacklist.txt is a file with one class or package per line.

## Whitelisting mode

As always, it would be better if we could accept only classes we explicitly want to allow for deserialization. Rejecting based on a whitelist is better security than rejecting based on a blacklist.

To help build a whitelist file with legitimately serializable classes, a 'dryrun' option has been added. Together with an empty white list, this will create a list of classes which your application actually deserializes.

This can be produced by configuring the agent as follows:

    -javaagent:notsoserial.jar -Dnotsoserial.whitelist=empty.txt -Dnotsoserial.dryrun=is-deserialized.txt

Where 'empty.txt' is an empty file and 'is-deserialized.txt' is a file where the names of your actually deserialized classes will be written to. 

After you are confident that all deserializable classes in your application have been recorded, you may restart your app, now reusing the recorded-as-serialized file as the whitelist:

    -javaagent:notsoserial.jar -Dnotsoserial.whitelist=is-deserialized.txt

## What happens when NotSoSerial rejects a deserialization attempt?

An Exception will be thrown, looking something like this:

    java.lang.UnsupportedOperationException: Deserialization not allowed for class java.util.concurrent.locks.AbstractOwnableSynchronizer
    	at org.kantega.notsoserial.NotSoSerialClassFileTransformer.preventDeserialization(NotSoSerialClassFileTransformer.java:119)

## Rejecting deserialization entirely

Just use an empty whitelist. Preliminary testing with a non-trivial Java application (which does not intentionally use RMI or other serialization) seems to indicate that this might work just fine. Looks like the JDK might not need serialization for any of its internal operations.


## Tracing deserialization

You might be interested not just in which classes your application deserializes, but also where in your code deserialization happens.

This can be enabled by using the 'trace' option, like the following:

     -javaagent:notsoserial.jar -Dnotsoserial.whitelist=empty.txt -Dnotsoserial.dryrun=is-deserialized.txt -Dnotsoserial.trace=deserialize-trace.txt

This will produce a file deserialize-trace.txt that looks something like this:

    Deserialization of class java.util.PriorityQueue (on Mon Nov 09 19:34:26 CET 2015)
             at org.kantega.notsoserial.WithDryRunWhitelistAndTraceIT.deserialize
