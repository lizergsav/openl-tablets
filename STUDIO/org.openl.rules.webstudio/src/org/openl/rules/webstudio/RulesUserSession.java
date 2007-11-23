package org.openl.rules.webstudio;

import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerManager;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.impl.ProductionDeployerManagerImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.Log;

public class RulesUserSession {
    private WorkspaceUser user;
    private UserWorkspace userWorkspace;
    ProductionDeployer deployer;
    private MultiUserWorkspaceManager workspaceManager;
    private ProductionDeployerManager deployerManager;

    public RulesUserSession(WorkspaceUser user, MultiUserWorkspaceManager workspaceManager) {
        this.user = user;
        this.workspaceManager = workspaceManager;
        this.deployerManager = new ProductionDeployerManagerImpl();
    }

    public String getUserId() {
        return user.getUserId();
    }

    public synchronized UserWorkspace getUserWorkspace() throws WorkspaceException, ProjectException {
        if (userWorkspace == null) {
            userWorkspace = workspaceManager.getUserWorkspace(user);
            userWorkspace.activate();
        }
        
        return userWorkspace;
    }

    public synchronized ProductionDeployer getDeployer() throws DeploymentException {
        if (deployer == null) {
            deployer = deployerManager.getDeployer(user);
        }
        return deployer;
    }


    public void sessionWillPassivate() {
        userWorkspace.passivate();
    }

    public void sessionDidActivate() {
        try {
            userWorkspace.activate();
        } catch (ProjectException e) {
            Log.error("Error at activation", e);
        }
    }

    public void sessionDestroyed() {
        userWorkspace.release();
    }
}
