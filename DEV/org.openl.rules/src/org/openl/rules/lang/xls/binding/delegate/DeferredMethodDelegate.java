package org.openl.rules.lang.xls.binding.delegate;

import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.module.DeferredMethod;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class DeferredMethodDelegate extends DeferredMethod implements DispatchDelegateOpenMethod{
    DeferredMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public DeferredMethodDelegate(XlsModuleOpenClass xlsModuleOpenClass, DeferredMethod delegate) {
        super(null, null, null, null, null);
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

    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    public ISyntaxNode getMethodBodyNode() {
        return delegate.getMethodBodyNode();
    }

    public String getName() {
        return delegate.getName();
    }

    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public void setMethodBodyBoundNode(IBoundMethodNode bnode) {
        delegate.setMethodBodyBoundNode(bnode);
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public String toString() {
        return delegate.toString();
    }
}
