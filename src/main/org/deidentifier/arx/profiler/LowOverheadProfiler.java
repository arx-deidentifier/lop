/*
 * LOP: Low Overhead Profiler
 * Copyright (C) 2014 Florian Kohlmayer, Fabian Prasser
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.deidentifier.arx.profiler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;

/**
 * This class implements a simplistic (low overhead) profiler
 *
 * @author Prasser, Kohlmayer
 */
class LowOverheadProfiler implements ClassFileTransformer {
    
    /** Returns the singleton instance*/
    static LowOverheadProfiler get(){
        if (profiler == null) {
            throw new IllegalStateException("Profiler is not initialized");
        }
        return profiler;
    }
    
    /** As required for instrumentation*/
    static Instrumentation getInstrumentation() {
        if (instrumentation == null) {
            throw new IllegalStateException("Use -javaagent:lop-"+VERSION+".jar to initialize agent");
        }
        return instrumentation;
    }
    
    /** As required for instrumentation*/
    static void premain(final String args, final Instrumentation instr) throws IOException {
        if (instr == null) {
            throw new IllegalStateException("Use -javaagent:lop-"+VERSION+".jar to initialize agent");
        }
        instrumentation = instr;
        instrumentation.addTransformer(get());
    }
    
    private static LowOverheadProfiler profiler = null;
    
    /* ************************************
     * INITIALIZE INSTRUMENTATION
      *************************************/
    
    static {
        try {
            profiler = new LowOverheadProfiler();
        } catch (IOException e) {
            System.out.println("Error initializing the profiler: "+e.getMessage());
        }
    }

    /** Version*/
    private static final String VERSION = "1.0.0";

    /** As required for instrumentation*/
    private static volatile Instrumentation instrumentation;

    /* ************************************
     * ACTUAL PROFILER
      *************************************/

    /** As defined in the config file*/
    private final Properties                             classToMethod;
    /** As created during instrumentation*/
    private final Map<String, Map<String, List<String>>> classToMethodToParameter;

    /**
     * Creates a new instance
     */
    LowOverheadProfiler() throws IOException {
        
        classToMethod            = initialize();
        classToMethodToParameter = new HashMap<String, Map<String, List<String>>>();
        
        Thread thread = new Thread() {
            public void run() {
                try {
                    finish();
                } catch (Exception e) {
                    if (e instanceof IllegalStateException) {
                        throw (IllegalStateException)e;
                    } else {
                        throw new IllegalStateException(e);
                    }
                }
            }
        };
        thread.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    @Override
    public byte[] transform(  final ClassLoader loader,
                              final String className,
                              final Class<?> classBeingRedefined,
                              final ProtectionDomain protectionDomain,
                              byte[] classfileBuffer) throws IllegalClassFormatException {
        
        // Prepare
        String classNameWithDots = className.replaceAll("/", "\\.");

        // Instrument classes
        if (classToMethod.containsKey(classNameWithDots)) {
            final String[] methods = ((String) classToMethod.get(classNameWithDots)).split(",");
            classToMethod.remove(classNameWithDots);
            for (int i = 0; i < methods.length; i++) {
                try {
                    classfileBuffer = instrument(classNameWithDots, classBeingRedefined, classfileBuffer, methods[i]);
                } catch ( CannotCompileException | IOException | NotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        
        // Return
        return classfileBuffer;
    }

    /**
     * Finish: print results
     * @throws Exception
     */
    private void finish() throws Exception {

        // Check
        if (!classToMethod.isEmpty()) {
            String classNameNotFound = String.valueOf(classToMethod.keySet().iterator().next());
            System.out.println("Profiler: Class "+classNameNotFound+" not found!");
            Runtime.getRuntime().halt(1);
        }

        System.out.println("Profile:");
        System.out.printf( "%20s | %20s | %s %n", "Total time [ms]", "Invocation count", "Method");
        System.out.println("----------------------------------------------------");
        for (String className : classToMethodToParameter.keySet()) {
            Class<?> clazz = Class.forName(className);
            for (String methodName : classToMethodToParameter.get(className).keySet()) {
                for (String parameterName : classToMethodToParameter.get(className).get(methodName)) {
                    Field counter = clazz.getField(methodName+"_"+parameterName+"_COUNTER");
                    Field timer = clazz.getField(methodName+"_"+parameterName+"_TIMER");
                    int time = (int)Math.round((double)timer.getLong(null) / 1000000d);
                    long count = counter.getLong(null);
                    parameterName = parameterName.replace("_InstrumentedArray_", "[]").replace('_', ',');
                    System.out.printf( "%20s | %20s | %s %n", time, count, className+"."+methodName+"("+parameterName+")");
                }
            }
        }
    }
    
    /**
     * Read configuration from file
     * @return
     * @throws IOException
     */
    private Properties initialize() throws IOException {
        final Properties properties = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream("profiler.properties");
            // load a properties file
            properties.load(input);
        } catch (final IOException e) {
            throw (e);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        return properties;
    }

    /**
     * Instrument class and method
     * @param className
     * @param classBeingRedefined
     * @param classfileBuffer
     * @param methodName
     * @return
     * @throws CannotCompileException
     * @throws IOException
     * @throws NotFoundException
     */
    private byte[] instrument(final String className,
                                   final Class<?> classBeingRedefined,
                                   final byte[] classfileBuffer,
                                   final String methodName) throws CannotCompileException, IOException, NotFoundException {
        
        ClassPool pool = ClassPool.getDefault();
        CtClass cl = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));

        // Foreach method
        boolean found = false;
        for (CtMethod method : cl.getDeclaredMethods()){
            
            if (method.getName().equals(methodName)) {
                // Get parameters
                CtClass[] params = method.getParameterTypes();
                found = true;
                
                // Build parameter string
                StringBuilder b = new StringBuilder();
                for (int i=0; i<params.length; i++){
                    
                    Class<?> parameterClazz = params[i].getClass();
                    if (parameterClazz == CtPrimitiveType.class) {
                        b.append(((CtPrimitiveType)params[i]).getName());
                    } else {
                        b.append(params[i].getSimpleName());
                    }
                    if (i!=params.length-1) {
                        b.append("_");
                    }
                }
                
                // Store parameters
                String parameters = b.toString().replace("[]", "_InstrumentedArray_");
                if (!classToMethodToParameter.containsKey(className)) {
                    classToMethodToParameter.put(className, new HashMap<String, List<String>>());
                }
                if (!classToMethodToParameter.get(className).containsKey(methodName)) {
                    classToMethodToParameter.get(className).put(methodName, new ArrayList<String>());
                }
                classToMethodToParameter.get(className).get(methodName).add(parameters);

                // Variable names
                String counterName = methodName + "_"+parameters+ "_COUNTER";
                String timerName = methodName + "_"+parameters+ "_TIMER";
                
                // Instrument class
                cl.addField(CtField.make("public static long " + timerName+";", cl));
                cl.addField(CtField.make("public static long " +counterName+";", cl));

                // Instrument method
                method.addLocalVariable("byteCodeInjectedStart", CtClass.longType);
                method.insertBefore(counterName+"++;");
                method.insertBefore("byteCodeInjectedStart = System.nanoTime();");
                method.insertAfter(timerName+" += (System.nanoTime() - byteCodeInjectedStart);");
            }
        }
        // Check
        if (!found) {
            System.out.println("Profiler: Method "+className+"."+methodName+" not found!");
            Runtime.getRuntime().halt(1);
        }
        cl.detach();
        return cl.toBytecode();
    }

    /**
     * Resets all counters and timers
     */
    void reset() throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException{
        for (String className : classToMethodToParameter.keySet()) {
            Class<?> clazz = Class.forName(className);
            for (String methodName : classToMethodToParameter.get(className).keySet()) {
                for (String parameterName : classToMethodToParameter.get(className).get(methodName)) {
                    Field counter = clazz.getField(methodName+"_"+parameterName+"_COUNTER");
                    Field timer = clazz.getField(methodName+"_"+parameterName+"_TIMER");
                    counter.set(null, 0);
                    timer.set(null, 0);
                }
            }
        }
    }
}
