package org.openl.rules.lang.xls.binding.delegate;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class JavaOpenMethodDelegate extends JavaOpenMethod implements DispatchDelegateOpenMethod{
    JavaOpenMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public JavaOpenMethodDelegate(XlsModuleOpenClass xlsModuleOpenClass, JavaOpenMethod delegate) {
        super(null);
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }
    
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return DispatcherLogic.dispatch(xlsModuleOpenClass, delegate, target, params, env);
    }

    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public String getName() {
        return delegate.getName();
    }

    public int getNumberOfParameters() {
        return delegate.getNumberOfParameters();
    }

    public int getParameterDirection(int i) {
        return delegate.getParameterDirection(i);
    }

    public String getParameterName(int i) {
        return delegate.getParameterName(i);
    }

    public IOpenClass getParameterType(int i) {
        return delegate.getParameterType(i);
    }

    public IOpenClass[] getParameterTypes() {
        return delegate.getParameterTypes();
    }

    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public String toString() {
        return delegate.toString();
    }

    
}
