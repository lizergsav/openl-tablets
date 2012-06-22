package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.springframework.beans.factory.FactoryBean;

/**
 * File based data source. Thread safe implementation.
 * 
 * @author Marat Kamalov
 * 
 */
public class FileSystemDataSource implements DataSource {

    private final Log log = LogFactory.getLog(FileSystemDataSource.class);

    private String loadDeploymentsFromDirectory;

    private File loadDeploymentsFromFolder;

    private FileFilter localWorkspaceFolderFilter;

    private FileFilter localWorkspaceFileFilter;

    private Object flag = new Object();

    private List<DataSourceListener> listeners = new ArrayList<DataSourceListener>();

    /**
     * Sets localWorkspaceFileFilter @see LocalFolderAPI. Spring bean
     * configuration property.
     * 
     * @param localWorkspaceFileFilter
     */
    public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }

    /**
     * Gets localWorkspaceFileFilter.
     */
    public FileFilter getLocalWorkspaceFileFilter() {
        return localWorkspaceFileFilter;
    }

    /**
     * Sets localWorkspaceFolderFilter @see LocalFolderAPI. Spring bean
     * configuration property.
     * 
     * @param localWorkspaceFolderFilter
     */
    public void setLocalWorkspaceFolderFilter(FileFilter localWorkspaceFolderFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
    }

    /**
     * Gets localWorkspaceFolderFilter.
     */
    public FileFilter getLocalWorkspaceFolderFilter() {
        return localWorkspaceFolderFilter;
    }

    public FileSystemDataSource() {
    }

    public FileSystemDataSource(String loadDeploymentsFromDirectory) {
        if (loadDeploymentsFromDirectory == null) {
            throw new IllegalArgumentException("loadDeploymentsFromDirectory argument can't be null");
        }
        this.loadDeploymentsFromDirectory = loadDeploymentsFromDirectory;
    }

    public void setLoadDeploymentsFromDirectory(String loadDeploymentsFromDirectory) {
        this.loadDeploymentsFromDirectory = loadDeploymentsFromDirectory;
    }

    public String getLoadDeploymentsFromDirectory() {
        return loadDeploymentsFromDirectory;
    }

    private File getLoadDeploymentsFromFolder() {
        if (loadDeploymentsFromFolder == null) {
            synchronized (flag) {
                if (loadDeploymentsFromFolder == null) {
                    loadDeploymentsFromFolder = new File(getLoadDeploymentsFromDirectory());
                }
            }
        }
        return loadDeploymentsFromFolder;
    }

    private void validateFileSystemDataSourceFolder(File fileSystemDataSourceFolder) {
        if (!fileSystemDataSourceFolder.exists()) {
            throw new DataSourceException("Folder doesn't exist. Path: " + getLoadDeploymentsFromDirectory());
        }
        if (!fileSystemDataSourceFolder.isDirectory()) {
            throw new DataSourceException("Folder doesn't exist. Path: " + getLoadDeploymentsFromDirectory());
        }
    }

    /** {@inheritDoc} */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }

        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument can't be null");
        }

        File folder = getLoadDeploymentsFromFolder();
        validateFileSystemDataSourceFolder(folder);
        if (folder.getName().equals(deploymentName)) {
            LocalFolderAPI localFolderAPI = new LocalFolderAPI(folder, new ArtefactPathImpl(
                    folder.getName()), new LocalWorkspaceImpl(null,
                    folder.getParentFile(), getLocalWorkspaceFolderFilter(),
                    getLocalWorkspaceFileFilter()));
            Deployment deployment = new Deployment(localFolderAPI);
            return deployment;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public Collection<Deployment> getDeployments() {
        File folder = getLoadDeploymentsFromFolder();
        validateFileSystemDataSourceFolder(folder);
        Collection<Deployment> deployments = new ArrayList<Deployment>(1);
        LocalFolderAPI localFolderAPI = new LocalFolderAPI(folder, new ArtefactPathImpl(
                folder.getName()), new LocalWorkspaceImpl(null,
                folder.getParentFile(), getLocalWorkspaceFolderFilter(),
                getLocalWorkspaceFileFilter()));
        Deployment deployment = new Deployment(localFolderAPI);
        deployments.add(deployment);
        validateDeployment(deployment);
        return Collections.unmodifiableCollection(deployments);
    }

    private void validateDeployment(Deployment deployment) {
        if (deployment.getProjects().isEmpty() && log.isWarnEnabled()) {
            log.warn("Data source with path '" + getLoadDeploymentsFromDirectory() + "' does not contain projects. Make sure you have specified correct folder.");
        }
    }

    /** {@inheritDoc} */
    public List<DataSourceListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /** {@inheritDoc} */
    public void addListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            throw new IllegalArgumentException("dataSourceListener argument can't be null");
        }
        synchronized (listeners) {
            listeners.add(dataSourceListener);
            if (log.isInfoEnabled()) {
                log.info(dataSourceListener.getClass().toString()
                        + " class listener is registered in file system data source");
            }
        }

    }

    /** {@inheritDoc} */
    public void removeAllListeners() {
        synchronized (listeners) {
            Iterator<DataSourceListener> itr = listeners.iterator();
            while (itr.hasNext()) {
                itr.next();
                itr.remove();
            }
        }
    }

    /** {@inheritDoc} */
    public void removeListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            throw new IllegalArgumentException("dataSourceListener argument can't be null");
        }
        synchronized (listeners) {
            listeners.remove(dataSourceListener);
            if (log.isInfoEnabled()) {
                log.info(dataSourceListener.getClass().toString()
                        + " class listener is unregistered from file system data source");
            }
        }
    }

    public static class DirFilterWatcher implements FileFilter {
        private String filter;

        public DirFilterWatcher() {
            this.filter = "";
        }

        public DirFilterWatcher(String filter) {
            this.filter = filter;
        }

        public boolean accept(File file) {
            if ("".equals(filter)) {
                return true;
            }
            return (file.getName().endsWith(filter));
        }
    }

    public static abstract class DirWatcher extends TimerTask {
        private String path;
        private File filesArray[];
        private HashMap<File, Long> dir = new HashMap<File, Long>();
        private DirFilterWatcher dfw;

        public DirWatcher(String path) {
            this(path, "");
        }

        public DirWatcher(String path, String filter) {
            if (path == null) {
                throw new IllegalArgumentException("path argument can't be null");
            }
            if (filter == null) {
                throw new IllegalArgumentException("filter argument can't be null");
            }

            this.path = path;
            dfw = new DirFilterWatcher(filter);
            filesArray = new File(path).listFiles(dfw);

            // transfer to the hashmap be used a reference and keep the
            // lastModfied value
            for (int i = 0; i < filesArray.length; i++) {
                dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
            }
        }

        @SuppressWarnings("unchecked")
        public final void run() {
            boolean f = false;
            Set<File> checkedFiles = new HashSet<File>();
            filesArray = new File(path).listFiles(dfw);

            // scan the files and check for modification/addition
            for (int i = 0; i < filesArray.length; i++) {
                Long current = (Long) dir.get(filesArray[i]);
                checkedFiles.add(filesArray[i]);
                if (current == null) {
                    // new file
                    dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
                    if (!f) {
                        onChange();
                        f = true;
                    }
                    onChange(filesArray[i], "add");
                } else if (current.longValue() != filesArray[i].lastModified()) {
                    // modified file
                    dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
                    if (!f) {
                        onChange();
                        f = true;
                    }
                    onChange(filesArray[i], "modify");
                }
            }

            // now check for deleted files
            Set<File> ref = ((HashMap<File, Long>) dir.clone()).keySet();
            ref.removeAll(checkedFiles);
            Iterator<File> it = ref.iterator();
            while (it.hasNext()) {
                File deletedFile = (File) it.next();
                dir.remove(deletedFile);
                if (!f) {
                    onChange();
                    f = true;
                }
                onChange(deletedFile, "delete");
            }
        }

        /**
         * Executes once on change if change is detected
         */
        protected abstract void onChange();

        protected abstract void onChange(File file, String action);
    }

    /**
     * TimerTask for check file data source modifications.
     * 
     * @author
     * 
     */
    public final static class CheckFileSystemChanges extends DirWatcher {
        private FileSystemDataSource fileSystemDataSource;

        private CheckFileSystemChanges(FileSystemDataSource fileSystemDataSource) {
            super(fileSystemDataSource.getLoadDeploymentsFromDirectory());
            this.fileSystemDataSource = fileSystemDataSource;
        }

        @Override
        protected void onChange() {
            List<DataSourceListener> listeners = fileSystemDataSource.getListeners();
            for (DataSourceListener listener : listeners) {
                listener.onDeploymentAdded();
            }
        }

        @Override
        protected void onChange(File file, String action) {

        }
    }

    public static class CheckFileSystemChangesFactoryBean implements FactoryBean<CheckFileSystemChanges> {

        private FileSystemDataSource fileSystemDataSource;

        public Class<?> getObjectType() {
            return CheckFileSystemChanges.class;
        }

        public CheckFileSystemChanges getObject() throws Exception {
            if (getFileSystemDataSource() == null) {
                throw new IllegalStateException("File system data source can't be null");
            }
            return new CheckFileSystemChanges(getFileSystemDataSource());
        }

        public boolean isSingleton() {
            return true;
        }

        public void setFileSystemDataSource(FileSystemDataSource fileSystemDataSource) {
            if (fileSystemDataSource == null) {
                throw new IllegalArgumentException("fileSystemDataSource can't be null");
            }
            this.fileSystemDataSource = fileSystemDataSource;
        }

        public FileSystemDataSource getFileSystemDataSource() {
            return fileSystemDataSource;
        }

    }
}
