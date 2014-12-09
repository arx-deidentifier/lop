LOP - Low Overhead Profiler for Java
====
 
LOP is a very simplistic profiler for Java that uses bytecode instrumentation via 
[Javassist](http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/). 

Usage
------

1. Create a file profiler.properties in the root directory of your Java program.
2. In this file, add a set of lines following the scheme `<class-name>=<method-name-1>,<method-name-2>...`, for
example `org.deidentifier.arx.example.Example=test1,test2`, telling LOP to profile the according methods of the
given class. LOP will automatically handle aspects such as class hierarchies, method overloading etc for you.
3. Add the following parameter when launching the JVM: `-javaagent:lop-1.0.0.jar`.
4. For JDK 7 and newer, also add: `-XX:-UseSplitVerifier`.

This will result in output, such as (see `org.deidentifier.arx.example.Example`):

```Java
     Total time [ms] |     Invocation count | Method 
----------------------------------------------------
                   0 |                    0 | org.deidentifier.arx.example.Example.test1() 
                   0 |                    0 | org.deidentifier.arx.example.Example.test1(int) 
                   0 |                    0 | org.deidentifier.arx.example.Example.test1(int,String) 
                  42 |                 2000 | org.deidentifier.arx.example.Example.test2() 
```
 
Download
------
A binary version (JAR file) is available for download [here](https://rawgithub.com/arx-deidentifier/lop/master/jars/lop-1.0.0.jar).
