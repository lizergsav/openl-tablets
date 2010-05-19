/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.base.INamedThing;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.ArrayIndex;
import org.openl.types.impl.ArrayLengthOpenField;
import org.openl.util.AOpenIterator;
import org.openl.util.CollectionsUtil;
import org.openl.util.IConvertor;
import org.openl.util.IOpenIterator;
import org.openl.util.OpenIterator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class JavaOpenClass extends AOpenClass {
    @SuppressWarnings("unchecked")
    private static class Class2JavaOpenClassCollector implements IConvertor<Class, IOpenClass> {
        public IOpenClass convert(Class c) {
            return getOpenClass(c);
        }
    }

    private static class JavaArrayLengthField extends ArrayLengthOpenField {
        @Override
        public int getLength(Object target) {
            return Array.getLength(target);
        }

    }

    private static class JavaClassClassField implements IOpenField {
        private Class<?> instanceClass;

        public JavaClassClassField(Class<?> instanceClass) {
            this.instanceClass = instanceClass;
        }

        public Object get(Object target, IRuntimeEnv env) {
            return instanceClass;
        }

        public IOpenClass getDeclaringClass() {
            return null;
        }

        public String getDisplayName(int mode) {
            return "class";
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public String getName() {
            return "class";
        }

        public IOpenClass getType() {
            return JavaOpenClass.CLASS;
        }

        public boolean isConst() {
            return true;
        }

        public boolean isReadable() {
            return true;
        }

        public boolean isStatic() {
            return true;
        }

        public boolean isWritable() {
            return false;
        }

        public void set(Object target, Object value, IRuntimeEnv env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return getName();
        }

    }
    static class JavaOpenInterface extends JavaOpenClass {

        class InterfaceInvocationHandler implements InvocationHandler {

            IdentityHashMap<Object, HashMap<BeanOpenField, Object>> map = new IdentityHashMap<Object, HashMap<BeanOpenField, Object>>();

            public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                HashMap<BeanOpenField, Object> values = map.get(proxy);
                if (values == null) {
                    values = new HashMap<BeanOpenField, Object>();
                    map.put(proxy, values);
                }

                BeanOpenField bf = getters.get(method);

                if (bf != null) {
                    Object res = values.get(bf);
                    return res != null ? res : bf.getType().nullObject();
                }

                bf = setters.get(method);

                if (bf != null) {
                    values.put(bf, args[0]);
                    return null;
                }

                if (method.getName().equals(toString.getName())) {
                    return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                }

                if (method.getName().equals(hashCode.getName())) {
                    return System.identityHashCode(proxy);
                }

                if (method.getName().equals(equals.getName())) {
                    return proxy == args[0];
                }

                throw new RuntimeException("Default Interface Proxy Implementation does not support method "
                        + method.getDeclaringClass().getName() + "::" + method.getName()
                        + ". Only bean access is supported");
            }

        }

        static Method toString, equals, hashCode;

        HashMap<Method, BeanOpenField> getters;
        HashMap<Method, BeanOpenField> setters;

        Class<?> proxyClass;

        InvocationHandler handler;

        {
            try {
                toString = Object.class.getMethod("toString");
                equals = Object.class.getMethod("equals", Object.class);
                hashCode = Object.class.getMethod("hashCode");
            } catch (NoSuchMethodException nsme) {
                throw RuntimeExceptionWrapper.wrap(nsme);
            }
        }

        protected JavaOpenInterface(Class<?> instanceClass, IOpenSchema schema) {
            super(instanceClass, schema);
            proxyClass = Proxy.getProxyClass(instanceClass.getClassLoader(), new Class[] { instanceClass });

        }

        @Override
        protected void collectBeanFields() {
            getters = new HashMap<Method, BeanOpenField>();
            setters = new HashMap<Method, BeanOpenField>();
            BeanOpenField.collectFields(fields, instanceClass, getters, setters);
        }

        private synchronized InvocationHandler getInvocationHandler() {
            if (handler == null) {
                handler = new InterfaceInvocationHandler();
            }

            return handler;
        }

        @Override
        public Object newInstance(IRuntimeEnv env) {
            try {
                return Proxy.newProxyInstance(instanceClass.getClassLoader(), new Class[] { instanceClass },
                        getInvocationHandler());
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }

        }

    }

    static class JavaPrimitiveClass extends JavaOpenClass {
        Class<?> wrapperClass;

        Object nullObject;
        /**
         * @param instanceClass
         * @param schema
         * @param factory
         */
        public JavaPrimitiveClass(Class<?> instanceClass, Class<?> wrapperClass, Object nullObject) {
            super(instanceClass, null);
            this.wrapperClass = wrapperClass;
            this.nullObject = nullObject;
        }

        @Override
        public boolean isSimple() {
            return true;
        }

        /**
         *
         */

        @Override
        public Object newInstance(IRuntimeEnv env) {
            return nullObject;
        }

        @Override
        public Object nullObject() {
            return nullObject;
        }

    }

    @SuppressWarnings("unchecked")
    static public final IConvertor<Class, IOpenClass> Class2JavaOpenClass = new Class2JavaOpenClassCollector();

    static Map<Class<?>, IOpenClass> javaClassCache = null;

    @SuppressWarnings("hiding")
    public static final JavaOpenClass INT = new JavaPrimitiveClass(int.class, Integer.class, new Integer(0));
    public static final JavaOpenClass LONG = new JavaPrimitiveClass(long.class, Long.class, new Long(0));
    public static final JavaOpenClass DOUBLE = new JavaPrimitiveClass(double.class, Double.class, new Double(0));
    public static final JavaOpenClass FLOAT = new JavaPrimitiveClass(float.class, Float.class, new Float(0));
    public static final JavaOpenClass SHORT = new JavaPrimitiveClass(short.class, Short.class, new Short((short) 0));
    public static final JavaOpenClass CHAR = new JavaPrimitiveClass(char.class, Character.class, new Character('\0'));
    public static final JavaOpenClass BYTE = new JavaPrimitiveClass(byte.class, Byte.class, new Byte((byte) 0));
    public static final JavaOpenClass BOOLEAN = new JavaPrimitiveClass(boolean.class, Boolean.class, Boolean.FALSE);
    public static final JavaOpenClass VOID = new JavaPrimitiveClass(void.class, Void.class, null);
    public static final JavaOpenClass STRING = new JavaOpenClass(String.class, null, true);
    public static final JavaOpenClass OBJECT = new JavaOpenClass(Object.class, null, true);
    public static final JavaOpenClass CLASS = new JavaOpenClass(Class.class, null, true);

    protected Class<?> instanceClass;

    boolean simple = false;

    protected HashMap<String, IOpenField> fields = null;

    protected HashMap<MethodKey, IOpenMethod> methods = null;

    static synchronized Map<Class<?>, IOpenClass> getJavaClassCache() {
        if (javaClassCache == null) {
            javaClassCache = new HashMap<Class<?>, IOpenClass>();
            javaClassCache.put(int.class, INT);
            javaClassCache.put(long.class, LONG);
            javaClassCache.put(double.class, DOUBLE);
            javaClassCache.put(float.class, FLOAT);
            javaClassCache.put(short.class, SHORT);
            javaClassCache.put(char.class, CHAR);
            javaClassCache.put(byte.class, BYTE);
            javaClassCache.put(boolean.class, BOOLEAN);
            javaClassCache.put(void.class, VOID);
            javaClassCache.put(String.class, STRING);
            javaClassCache.put(Object.class, OBJECT);
            javaClassCache.put(Class.class, CLASS);
        }
        return javaClassCache;

    }

    /**
     * @param obj object or null, does not work on primitives
     * @return JavaOpenClass for objects and NullOpenClass for null
     */

    // static public IOpenClass getOpenClass(Object obj)
    // {
    // if (obj == null)
    // return NullOpenClass.the;
    // return getOpenClass((Class)obj);
    // }
    //
    static public synchronized JavaOpenClass getOpenClass(Class<?> c) {
        JavaOpenClass res = (JavaOpenClass) getJavaClassCache().get(c);
        if (res == null) {
            if (c.isInterface()) {
                res = new JavaOpenInterface(c, null);
            } else if (c.isEnum())
                res = new JavaOpenEnum(c, null);
            else {
                res = new JavaOpenClass(c, null);
            }

            getJavaClassCache().put(c, res);
        }

        return res;
    }

    static public IOpenClass[] getOpenClasses(Class<?>[] cc) {
        if (cc.length == 0) {
            return IOpenClass.EMPTY;
        }

        IOpenClass[] ary = new IOpenClass[cc.length];

        CollectionsUtil.collect(ary, cc, Class2JavaOpenClass);

        return ary;

    }

    public static Class<?> makeArrayClass(Class<?> c) {
        return Array.newInstance(c, 0).getClass();
    }

    public static ArrayIndex makeArrayIndex(IOpenClass arrayType) {
        return new ArrayIndex(getOpenClass(arrayType.getInstanceClass().getComponentType()));
    }

    public static synchronized void printCache() {
        int i = 0;
        for (Iterator<Class<?>> iter = getJavaClassCache().keySet().iterator(); iter.hasNext();) {
            Class<?> element = iter.next();
            System.out.println("" + (i++) + ":\t" + printClass(element));

        }
    }

    static String printClass(Class<?> c) {
        if (c.isArray()) {
            return "[]" + printClass(c.getComponentType());
        }

        return c.getName();
    }

    // ////////////////////// helpers ////////////////////////////

    public static synchronized void resetAllClassloaders(HashMap<?, ClassLoader> oldLoaders) {
        for (Iterator<ClassLoader> iter = oldLoaders.values().iterator(); iter.hasNext();) {
            ClassLoader cl = iter.next();
            resetClassloader(cl);
        }
    }

    public static synchronized void resetClassloader(ClassLoader cl) {
        List<Class<?>> toRemove = new ArrayList<Class<?>>();
        for (Iterator<Class<?>> iter = getJavaClassCache().keySet().iterator(); iter.hasNext();) {
            Class<?> c = iter.next();
            if (c.getClassLoader() == cl) {
                toRemove.add(c);
            }

        }

        for (Iterator<Class<?>> iter = toRemove.iterator(); iter.hasNext();) {
            Class<?> c = iter.next();
            javaClassCache.remove(c);

            // System.out.println("Removing " + printClass(c));
        }

    }

    protected JavaOpenClass(Class<?> instanceClass, IOpenSchema schema) {
        super(schema);
        this.instanceClass = instanceClass;
        this.schema = schema;
    }

    protected JavaOpenClass(Class<?> instanceClass, IOpenSchema schema, boolean simple) {
        super(schema);
        this.instanceClass = instanceClass;
        this.schema = schema;
        this.simple = simple;
    }

    protected void collectBeanFields() {
        BeanOpenField.collectFields(fields, instanceClass, null, null);

    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JavaOpenClass)) {
            return false;
        }

        return instanceClass == ((JavaOpenClass) obj).instanceClass;
    }

    @Override
    synchronized protected Map<String, IOpenField> fieldMap() {
        if (fields == null) {
            fields = new HashMap<String, IOpenField>();
            Field[] ff = instanceClass.getFields();

            for (int i = 0; i < ff.length; i++) {
                if (isPublic(ff[i].getDeclaringClass())) {
                    fields.put(ff[i].getName(), new JavaOpenField(ff[i]));
                }
            }
            if (instanceClass.isArray()) {
                fields.put("length", new JavaArrayLengthField());
            }

            fields.put("class", new JavaClassClassField(instanceClass));
            collectBeanFields();

        }
        return fields;
    }

    private IAggregateInfo aggregateInfo;
    
    public synchronized IAggregateInfo getAggregateInfo() {
        if (aggregateInfo != null)
            return aggregateInfo;
        
        if (List.class.isAssignableFrom(getInstanceClass())) {
            aggregateInfo = JavaListAggregateInfo.LIST_AGGREGATE;
        }
        else aggregateInfo = JavaArrayAggregateInfo.ARRAY_AGGREGATE;
        return aggregateInfo;
    }

    public String getDisplayName(int mode) {

        String name = getName();
        switch (mode) {
            case INamedThing.SHORT:
            case INamedThing.REGULAR:
            default:
                return StringTool.lastToken(name, ".");
            case INamedThing.LONG:
                return name;
        }
    }

    public Class<?> getInstanceClass() {
        return instanceClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String,
     *      org.openl.types.IOpenClass[])
     */
    @Override
    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params) throws AmbiguousMethodException {
        return getMethod(name, params);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName() {
        return instanceClass.getName();
    }

    public String getSimpleName() {
        return getDisplayName(INamedThing.SHORT);
    }

    @Override
    public int hashCode() {
        return instanceClass.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(instanceClass.getModifiers());
    }

    public boolean isAssignableFrom(Class<?> c) {
        return instanceClass.isAssignableFrom(c);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#isAssignableFrom(org.openl.types.IOpenClass)
     */
    public boolean isAssignableFrom(IOpenClass ioc) {
        return instanceClass.isAssignableFrom(ioc.getInstanceClass());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#instanceOf(java.lang.Object)
     */
    public boolean isInstance(Object instance) {
        return instanceClass.isInstance(instance);
    }

    boolean isPublic(Class<?> declaringClass) {
        return Modifier.isPublic(declaringClass.getModifiers());
    }

    @Override
    public boolean isSimple() {
        return simple;
    }

    @Override
    synchronized protected Map<MethodKey, IOpenMethod> methodMap() {
        if (methods == null) {
            methods = new HashMap<MethodKey, IOpenMethod>();
            Method[] mm = instanceClass.getMethods();
            for (int i = 0; i < mm.length; i++) {
                if (isPublic(mm[i].getDeclaringClass())) {
                    JavaOpenMethod om = new JavaOpenMethod(mm[i]);
                    methods.put(new MethodKey(om), om);
                }
            }

            Constructor<?>[] cc = instanceClass.getConstructors();
            for (int i = 0; i < cc.length; i++) {
                if (isPublic(cc[i].getDeclaringClass())) {
                    IOpenMethod om = new JavaOpenConstructor(cc[i]);
                    // Log.debug("Adding method " + mm[i].getName() + " code = "
                    // + new MethodKey(om).hashCode());
                    methods.put(new MethodKey(om), om);
                }
            }

        }
        return methods;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#newInstance()
     */
    public Object newInstance(IRuntimeEnv env) {

        try {
            return getInstanceClass().newInstance();
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    @Override
    public Object nullObject() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Iterator<IOpenClass> superClasses() {
        Class[] tmp = instanceClass.getInterfaces();
        IOpenIterator<Class> ic = OpenIterator.fromArray(tmp);

        IOpenIterator<IOpenClass> interfaces = ic.collect(new Class2JavaOpenClassCollector());

        Class superClass = instanceClass.getSuperclass();

        return AOpenIterator.merge(AOpenIterator.single((IOpenClass) JavaOpenClass.getOpenClass(superClass)), interfaces);
    }

}