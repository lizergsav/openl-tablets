package org.openl.rules.webstudio.web.tableeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectHelper;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RecentlyVisitedTables;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.test.InputArgsBean;
import org.openl.rules.webstudio.web.trace.TraceIntoFileBean;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.util.StringTool;

/**
 * Request scope managed bean for showTable page.
 */
@ManagedBean
@RequestScoped
public class ShowTableBean {

    private static final Log LOG = LogFactory.getLog(ShowTableBean.class);
    
    private static final String INFO_MESSAGE = "Can`t find requested table in current module";

    // Test in current table (only for test tables)
    private TestDescription[] runnableTestMethods = {}; //test units
    private Map<TestDescription, Boolean> selectedTests;
    // All checks and tests for current table (including tests with no cases, run methods).
    private IOpenMethod[] allTests = {};
    private IOpenMethod[] tests = {};

    private List<IOpenLTable> targetTables;

    private String uri;
    private IOpenLTable table;

    private List<OpenLMessage> errors;
    private List<OpenLMessage> warnings;
    // Errors + Warnings
    private List<OpenLMessage> problems;

    private String paramsWithoutUri;

    public ShowTableBean() {
        uri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);

        WebStudio studio = WebStudioUtils.getWebStudio();
        final ProjectModel model = studio.getModel();

        table = model.getTable(uri);

        if (table == null) {
            uri = studio.getTableUri();
            table = model.getTable(uri);
        } else {
            studio.setTableUri(uri);
        }

        if (table == null) {
            try {
                String infoLink = 
                    String.format("message.xhtml?summary=%s", 
                        INFO_MESSAGE);

                FacesUtils.redirect(infoLink);
            } catch (IOException e) {                
                LOG.error("Can`t redirect to info message page", e);
            }
        } else {
            String tableType = table.getType();
            if (tableType.equals(XlsNodeTypes.XLS_TEST_METHOD.toString())
                    || tableType.equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {
                targetTables = model.getTargetTables(uri);
            }
            
            initProblems();
            initTests(model);        
            initParams();

            storeTable();
        }
    }

    private void storeTable() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        RecentlyVisitedTables recentlyVisitedTables = model.getRecentlyVisitedTables();
        recentlyVisitedTables.add(table);
    }

    @SuppressWarnings("unchecked")
    private void initParams() {
        Map paramMap = new HashMap(FacesUtils.getRequestParameterMap());
        for (Map.Entry entry : (Set<Map.Entry>) paramMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                entry.setValue(new String[] { (String) entry.getValue() });
            }
        }

        paramsWithoutUri = WebTool.listRequestParams(paramMap, new String[] { "uri", "mode" });
    }

    private void initTests(final ProjectModel model) {
        initRunnableTestMethods(model);
        
        allTests = model.getTestAndRunMethods(uri);
        tests = model.getTestMethods(uri);
    }

    private void initRunnableTestMethods(final ProjectModel model) {
        if (model.getMethod(uri) instanceof TestSuiteMethod) {
            runnableTestMethods = ((TestSuiteMethod) model.getMethod(uri)).getTests();
            selectedTests = new HashMap<TestDescription, Boolean>();
            for (TestDescription test : runnableTestMethods) {
                selectedTests.put(test, true);
            }
        }
    }
    
    private void initProblems() {
        initErrors();
        initWarnings();

        problems = new ArrayList<OpenLMessage>();
        problems.addAll(errors);
        problems.addAll(warnings);
    }

    private void initErrors() {
        List<OpenLMessage> messages = table.getMessages();
        errors = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
    }

    private void initWarnings() {
        warnings = new ArrayList<OpenLMessage>();
        
        if (targetTables != null) {
            for (IOpenLTable targetTable : targetTables) {
                if (targetTable.getMessages().size() > 0) {
                    warnings.add(new OpenLMessage("Tested rules have errors", StringUtils.EMPTY, Severity.WARN));
                    // one warning is enough.
                    break;
                }
            }
        }

        ProjectModel model = WebStudioUtils.getProjectModel();

        List<OpenLMessage> messages = model.getModuleMessages();

        List<OpenLMessage> warningMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        for (OpenLMessage message : warningMessages) {
            if (message instanceof OpenLWarnMessage) {//there can be simple OpenLMessages with severity WARN
                OpenLWarnMessage warning = (OpenLWarnMessage) message;
                ISyntaxNode syntaxNode = warning.getSource();
                if (syntaxNode instanceof TableSyntaxNode && ((TableSyntaxNode) syntaxNode).getUri().equals(uri)) {
                    warnings.add(warning);
                }
            }
        }
    }

    public boolean isDispatcherValidationNode() {
        return table.getTechnicalName().startsWith(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME);
    }

    private boolean updateSystemValue(TableEditorModel editorModel, TablePropertyDefinition sysProperty) {
        boolean result = false;
        Object systemValue = null;

        if (sysProperty.getSystemValuePolicy().equals(SystemValuePolicy.ON_EACH_EDIT)) {
            systemValue = SystemValuesManager.getInstance().getSystemValue(
                    sysProperty.getSystemValueDescriptor());
            if (systemValue != null) {
                try {
                    editorModel.setProperty(sysProperty.getName(), systemValue);
                    result = true;
                } catch (Exception e) {
                    LOG.error(String.format("Can`t update system property '%s' with value '%s'", sysProperty.getName(),
                            systemValue), e);
                }
            }
        }
        return result;
    }

    public String getEncodedUri() {
        return StringTool.encodeURL(uri);
    }

    public String getMode() {
        return FacesUtils.getRequestParameter("mode");
    }

    public String getParamsWithoutUri() {
        return paramsWithoutUri;
    }

    public IOpenLTable getTable() {
        return table;
    }

    public List<OpenLMessage> getErrors() {
        return errors;
    }

    public List<OpenLMessage> getWarnings() {
        return warnings;
    }

    public List<OpenLMessage> getProblems() {
        return problems;
    }    
    
    
    /**
     * Return test cases for current table.
     * 
     * @return array of tests for current table. 
     */
    public TestDescription[] getTests() {
        return runnableTestMethods;
    }

    public Map<TestDescription, Boolean> getSelectedTests() {
        return selectedTests;
    }
    
    public String makeTestSuite() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        IOpenMethod method = studio.getModel().getMethod(uri);
        TestSuite testSuite;
        if (method instanceof TestSuiteMethod) {
            TestSuiteMethod testSuiteMethodSelected = (TestSuiteMethod) method;
            testSuite = new TestSuite(testSuiteMethodSelected, getSelectedIndices());
        } else {//method without parameters
            testSuite = new TestSuite(new TestDescription(method, new Object[] {}));
        }
        studio.getModel().addTestSuiteToRun(testSuite);
        return null;
    }
    
    public void runAllTestsForTable(){
        ProjectModel model = WebStudioUtils.getProjectModel();
        List<TestSuite> testSuites = new ArrayList<TestSuite>();
        for(IOpenMethod testSuiteMethod :  tests){
            testSuites.add(new TestSuite((TestSuiteMethod)testSuiteMethod));
        }
        model.addTestSuitesToRun(testSuites);
    }

    public String traceIntoFile(boolean withArgs) {
        if (withArgs) {
            ((InputArgsBean) FacesUtils.getBackingBean("inputArgsBean")).makeTestSuite();
        } else {
            makeTestSuite();
        }
        return ((TraceIntoFileBean) FacesUtils.getBackingBean("traceIntoFileBean")).traceIntoFile();
    }

    public String traceIntoFile() {
        return traceIntoFile(false);
    }

    public int[] getSelectedIndices() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < runnableTestMethods.length; i++) {
            if (selectedTests.get(runnableTestMethods[i])) {
                list.add(i);
            }
        }
        Integer[] indices = new Integer[list.size()];
        return ArrayUtils.toPrimitive(list.toArray(indices));
    }

    public String getUri() {
        return uri;
    }

    public List<IOpenLTable> getTargetTables() {
        return targetTables;
    }

    public boolean isCopyable() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.isEditable() && table.isCanContainProperties()
                && !XlsNodeTypes.XLS_DATATYPE.toString().equals(table.getType()); 
    }

    /**
     * 
     * @return true if it is possible to create tests for current table.
     */
    public boolean isCanCreateTest() {
        return table.isExecutable() && isEditable();
    }    

    public boolean isEditable() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.isEditable() && !isDispatcherValidationNode();
    }

    public boolean isHasErrors() {
        return CollectionUtils.isNotEmpty(errors);
    }

    public boolean isHasWarnings() {
        return CollectionUtils.isNotEmpty(warnings);
    }

    public boolean isHasProblems() {
        return isHasErrors() || isHasWarnings();
    }

    /**
     * Checks if there are runnable tests for current table.
     * 
     * @return true if there are runnable tests for current table.
     */
    public boolean isTestable() {
        return runnableTestMethods.length > 0;
    }
    
    /**
     * Checks if there are tests, including tests with test cases, runs with filled runs, tests without cases(empty),
     * runs without any parameters and tests without cases and runs.
     */
    public boolean isHasAnyTests() {
        return ArrayUtils.isNotEmpty(allTests);
    }

    public boolean isHasTests() {
        return ArrayUtils.isNotEmpty(tests);
    }

    /**
     * Gets all tests for current table.
     */
    public IOpenMethod[] getAllTests() {
        return allTests;
    }
    
    public String getTestName(Object testMethod){
        return ProjectHelper.createTestName((IOpenMethod) testMethod);
    }

    public String removeTable() throws Exception {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        IGridTable gridTable = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);

        new TableServiceImpl(true).removeTable(gridTable);
        studio.rebuildModel();
        RecentlyVisitedTables visitedTables = studio.getModel().getRecentlyVisitedTables();
        visitedTables.getTables().remove(table);

        return null;
    }

    public boolean beforeSaveAction() {
        return updateSystemProperties();
    }

    public void afterSaveAction(String newUri) {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTableUri(newUri);
        studio.rebuildModel();
    }

    @SuppressWarnings("unchecked")
    public boolean updateSystemProperties() {
        boolean result = true;
        if (table.isCanContainProperties()) {
            String editorId = FacesUtils.getRequestParameter(
                    org.openl.rules.tableeditor.util.Constants.REQUEST_PARAM_EDITOR_ID);

            Map editorModelMap = (Map) FacesUtils.getSessionParam(
                    org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);

            TableEditorModel editorModel = (TableEditorModel) editorModelMap.get(editorId);

            List<TablePropertyDefinition> sysProps = TablePropertyDefinitionUtils.getSystemProperties();

            for (TablePropertyDefinition sysProperty : sysProps) {
                result = updateSystemValue(editorModel, sysProperty);
            }
        } 
        return result;
    }    

    public String getTreeNodeId() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        String id = studio.getModel().getTreeNodeId(getUri());
        return id;
    }

    public void setShowFormulas() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setShowFormulas(!studio.isShowFormulas());
    }

    public void setCollapseProperties() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setCollapseProperties(!studio.isCollapseProperties());
    }

    public static class TestRunsResultBean {

        private TestSuite[] tests;

        public TestRunsResultBean(TestSuite[] tests) {
            this.tests = tests;
        }

        public TestSuite[] getTests() {
            return tests;
        }

        public boolean isNotEmpty() {
            if (ArrayUtils.isNotEmpty(tests)) {
                for (TestSuite test : getTests()) {
                    if (ArrayUtils.isNotEmpty(test.getTests())) {
                        return true;
                    }
                }
            }
            return false;
        }
        
    }
}