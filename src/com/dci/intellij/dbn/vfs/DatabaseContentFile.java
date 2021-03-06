package com.dci.intellij.dbn.vfs;

import com.dci.intellij.dbn.common.DevNullStreams;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.mapping.FileConnectionMappingProvider;
import com.dci.intellij.dbn.ddl.DDLFileType;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.psql.PSQLLanguage;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.DBView;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.IOException;
import java.io.InputStream;

public abstract class DatabaseContentFile extends VirtualFile implements FileConnectionMappingProvider, DBVirtualFile {
    protected DatabaseEditableObjectFile databaseFile;
    protected DBContentType contentType;
    private FileType fileType;
    private boolean modified;
    private String name;
    private String path;
    private String url;

    public ConnectionHandler getActiveConnection() {
        return getObject().getConnectionHandler();
    }

    @Override
    public boolean isInLocalFileSystem() {
        return false;
    }

    public DBSchema getCurrentSchema() {
        return getObject().getSchema();
    }

    public DatabaseContentFile(DatabaseEditableObjectFile databaseFile, DBContentType contentType) {
        this.databaseFile = databaseFile;
        this.contentType = contentType;

        DBSchemaObject object = databaseFile.getObject();
        this.name = object.getName();
        this.path = DatabaseFileSystem.createPath(object, getContentType());
        this.url = DatabaseFileSystem.createUrl(object);

        DDLFileType ddlFileType = object.getDDLFileType(contentType);
        this.fileType = ddlFileType == null ? null : ddlFileType.getLanguageFileType();
    }

    public DatabaseEditableObjectFile getDatabaseFile() {
        return databaseFile;
    }

    public DBContentType getContentType() {
        return contentType;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Nullable
    public DBSchemaObject getObject() {
        return databaseFile == null ? null : databaseFile.getObject();
    }

    @Override
    public ConnectionHandler getConnectionHandler() {
        return databaseFile.getConnectionHandler();
    }

    public DBLanguageDialect getLanguageDialect() {
        DBSchemaObject object = getObject();
        DBLanguage language =
                object instanceof DBView ?
                        SQLLanguage.INSTANCE :
                        PSQLLanguage.INSTANCE;
        
        return object == null ?
                language.getMainLanguageDialect() :
                object.getLanguageDialect(language);
    }

    /*********************************************************
     *                     VirtualFile                       *
     *********************************************************/
    @NotNull
    @NonNls
    public String getName() {
        return name;
    }

    @NotNull
    public FileType getFileType() {
        return fileType;
    }

    @NotNull
    public VirtualFileSystem getFileSystem() {
        return DatabaseFileSystem.getInstance();
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    public Project getProject() {
        DBSchemaObject object = getObject();
        return object == null ? null : object.getProject();
    }

    public boolean isWritable() {
        return true;
    }

    public boolean isDirectory() {
        return false;
    }

    public boolean isValid() {
        return true;
    }

    @Nullable
    public VirtualFile getParent() {
        DBSchemaObject object = databaseFile.getObject();
        if (object != null) {
            DBObject parentObject = object.getParentObject();
            if (parentObject != null) {
                return parentObject.getVirtualFile();
            }
        }
        return null;
    }

    public Icon getIcon() {
        DBSchemaObject object = getObject();
        return object == null ? null : object.getOriginalIcon();
    }

    public VirtualFile[] getChildren() {
        return VirtualFile.EMPTY_ARRAY;
    }

    public long getTimeStamp() {
        return 0;
    }

    public void refresh(boolean b, boolean b1, Runnable runnable) {

    }

    public InputStream getInputStream() throws IOException {
        return DevNullStreams.INPUT_STREAM;
    }

    public long getModificationStamp() {
        return 1;
    }

    @Override
    public void dispose() {
        databaseFile = null;
    }
}
