package org.openl.rules.workspace.lw.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.util.MsgHelper;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LocalWorkspaceImpl implements LocalWorkspace {
    private static final Log log = LogFactory.getLog(LocalWorkspaceImpl.class);

    private WorkspaceUser user;
    private File location;
    private Map<String, LocalProject> localProjects;
    private List<LocalWorkspaceListener> listeners = new ArrayList<LocalWorkspaceListener>();
    private FileFilter localWorkspaceFolderFilter;
    private FileFilter localWorkspaceFileFilter;

    public LocalWorkspaceImpl(WorkspaceUser user, File location, FileFilter localWorkspaceFolderFilter, FileFilter localWorkspaceFileFilter) {
        this.user = user;
        this.location = location;
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;

        localProjects = new HashMap<String, LocalProject>();

        loadProjects();
    }

    public Collection<LocalProject> getProjects() {
        return localProjects.values();
    }

    public LocalProject getProject(String name) throws ProjectException {
        LocalProject lp = localProjects.get(name);
        if (lp == null) {
            throw new ProjectException("Cannot find project ''{0}''!", null, name);
        }

        return lp;
    }

    public boolean hasProject(String name) {
        return (localProjects.get(name) != null);
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        LocalProject lp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return lp.getArtefactByPath(pathInProject);
    }

    public LocalProject addProject(Project project) throws ProjectException {
        String name = project.getName();

        if (hasProject(name)) {
            // remove it
            getProject(name).remove();
            // TODO smart update (if it is reasonable)
        }

        return downloadProject(project);
    }

    public void removeProject(String name) throws ProjectException {
        getProject(name).remove();
    }

    public void refresh() {
        // check existing
        Iterator<LocalProject> i = localProjects.values().iterator();
        while (i.hasNext()) {
            LocalProjectImpl lp = (LocalProjectImpl)i.next();

            File location = lp.getLocation();
            if (location.exists()) {
                // still here
                lp.refresh();
            } else {
                // deleted externally
                i.remove();
            }
        }

        // check new
        File[] folders = location.listFiles(localWorkspaceFolderFilter);
        if (folders == null) return;

        for (File folder : folders) {
            String name = folder.getName();
            if (!localProjects.containsKey(name)) {
                // new project detected
                ArtefactPath ap = new ArtefactPathImpl(new String[]{name});
                LocalProjectImpl newlyDetected = new LocalProjectImpl(name, ap, folder, this, localWorkspaceFileFilter);

                try {
                    newlyDetected.load();
                } catch (ProjectException e) {
                    String msg = MsgHelper.format("Error loading just detected local project ''{0}''!", name);
                    log.error(msg, e);
                }

                // add it
                localProjects.put(name, newlyDetected);
            }
        }
    }

    public void saveAll() {
        for (LocalProject lp : localProjects.values()) {
            try {
                lp.save();
            } catch (ProjectException e) {
                String msg = MsgHelper.format("Error saving local project ''{0}''!", lp.getName());
                log.error(msg, e);
            }
        }
    }

    public void release() {
        saveAll();
        localProjects.clear();

        for (LocalWorkspaceListener lwl : listeners)
            lwl.workspaceReleased(this);
    }

    public File getLocation() {
        return location;
    }

    public void addWorkspaceListener(LocalWorkspaceListener listener) {
        listeners.add(listener);
    }

    public boolean removeWorkspaceListener(LocalWorkspaceListener listener) {
        return listeners.remove(listener);
    }

// --- protected

    protected void notifyRemoved(LocalProject project) {
        localProjects.remove(project.getName());
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    protected LocalProjectImpl downloadProject(Project project) throws ProjectException {
        String name = project.getName();

        ArtefactPath ap = new ArtefactPathImpl(new String[]{name});
        File f = new File(location, name);

        LocalProjectImpl lpi = new LocalProjectImpl(name, ap, f, this, localWorkspaceFileFilter);
        lpi.downloadArtefact(project);

        lpi.save();

        // add project
        localProjects.put(name, lpi);
        return lpi;
    }

    protected void loadProjects() {
        File[] folders = location.listFiles(localWorkspaceFolderFilter);
        for (File f : folders) {
            String name = f.getName();
            ArtefactPath ap = new ArtefactPathImpl(new String[]{name});

            LocalProjectImpl lpi = new LocalProjectImpl(name, ap, f, this, localWorkspaceFileFilter);
            try {
                lpi.load();
            } catch (ProjectException e) {
                log.error(MsgHelper.format("Error loading local project ''{0}''!", lpi.getName()), e);
            }

            localProjects.put(name, lpi);
        }
    }
}
