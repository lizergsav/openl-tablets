package org.openl.rules.runtime;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.runtime.EngineFactory;
import org.openl.runtime.EngineFactoryDefinition;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

/**
 * 
 * Class RuleEngineFactory creates wrappers around OpenL classes with openl name
 * "org.openl.xls".
 * 
 */
public class RuleEngineFactory<T> extends EngineFactory<T> {

    public static final String RULE_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>() {
        @Override
        protected org.openl.vm.IRuntimeEnv initialValue() {
            return new org.openl.vm.SimpleVM().getRuntimeEnv();
        }
    };

    /**
     * 
     * @param factoryDef Engine factory definition
     *            {@link EngineFactoryDefinition}.
     * @param engineInterface User interface of rule.
     */
    public RuleEngineFactory(EngineFactoryDefinition factoryDef, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, factoryDef, engineInterface);
    }

    /**
     * Extended constructor to accommodated alternative OpenL implementations,
     * such as org.openl.xls.ce
     */
    public RuleEngineFactory(EngineFactoryDefinition factoryDef, Class<T> engineInterface, String openlName) {
        super(openlName, factoryDef, engineInterface);
    }

    /**
     * 
     * @param file Rule file
     * @param engineInterface User interface of rule
     */
    public RuleEngineFactory(File file, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, file, engineInterface);
    }

    /**
     * Extended constructor to accommodated alternative OpenL implementations,
     * such as org.openl.xls.ce
     */
    public RuleEngineFactory(File file, Class<T> engineInterface, String openlName) {
        super(openlName, file, engineInterface);
    }

    /**
     * 
     * @param sourceFile A path name of rule file string
     * @param engineInterface User interface of a rule
     */
    public RuleEngineFactory(String sourceFile, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, sourceFile, engineInterface);
    }

    /**
     * Extended constructor to accommodated alternative OpenL implementations,
     * such as org.openl.xls.ce
     */
    public RuleEngineFactory(String sourceFile, Class<T> engineInterface, String openlName) {
        super(openlName, sourceFile, engineInterface);
    }

    /**
     * 
     * @param userHome Current path of Openl userHome
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     */
    public RuleEngineFactory(String userHome, String sourceFile, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, userHome, sourceFile, engineInterface);
    }

    /**
     * Extended constructor to accommodated alternative OpenL implementations,
     * such as org.openl.xls.ce
     */
    public RuleEngineFactory(String userHome, String sourceFile, Class<T> engineInterface, String openlName) {
        super(openlName, userHome, sourceFile, engineInterface);
    }

    /**
     * 
     * @param url Url to rule file
     * @param engineInterface User interface of a rule
     */
    public RuleEngineFactory(URL url, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, url, engineInterface);
    }

    /**
     * Extended constructor to accommodated alternative OpenL implementations,
     * such as org.openl.xls.ce
     */
    public RuleEngineFactory(URL url, Class<T> engineInterface, String openlName) {
        super(openlName, url, engineInterface);
    }

    public RuleEngineFactory(IOpenSourceCodeModule source, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, source, engineInterface);
    }

    /**
     * Extended constructor to accommodated alternative OpenL implementations,
     * such as org.openl.xls.ce
     */
    public RuleEngineFactory(IOpenSourceCodeModule source, Class<T> engineInterface, String openlName) {
        super(openlName, source, engineInterface);
    }

    public RuleEngineFactory(String userHome, IOpenSourceCodeModule source, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, userHome, source, engineInterface);
    }

    /**
     * Extended constructor to accommodated alternative OpenL implementations,
     * such as org.openl.xls.ce
     */
    public RuleEngineFactory(String userHome, IOpenSourceCodeModule source, Class<T> engineInterface, String openlName) {
        super(openlName, userHome, source, engineInterface);
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        List<Class<?>> interfaces = new ArrayList<Class<?>>();
        interfaces.addAll(Arrays.asList(super.getInstanceInterfaces()));
        interfaces.add(IRulesRuntimeContextProvider.class);

        return interfaces.toArray(new Class<?>[interfaces.size()]);
    }

    @Override
    protected IRuntimeEnv getRuntimeEnv() {
        return __env.get();
    }

    @Override
    protected InvocationHandler makeInvocationHandler(Object openClassInstance, Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {
        return new RulesInvocationHandler(openClassInstance, this, runtimeEnv, methodMap);
    }

}
