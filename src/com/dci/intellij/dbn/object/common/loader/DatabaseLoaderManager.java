package com.dci.intellij.dbn.object.common.loader;

import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.editor.BasicTextEditor;
import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.thread.SimpleLaterInvocator;
import com.dci.intellij.dbn.common.util.DocumentUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionLoadListener;
import com.dci.intellij.dbn.connection.mapping.FileConnectionMappingManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DatabaseLoaderManager extends AbstractProjectComponent {
    private DatabaseLoaderQueue loaderQueue;

    private DatabaseLoaderManager(final Project project) {
        super(project);
        EventManager.subscribe(project, ConnectionLoadListener.TOPIC, new ConnectionLoadListener() {
            @Override
            public void contentsLoaded(final ConnectionHandler connectionHandler) {
                new SimpleLaterInvocator() {
                    @Override
                    public void execute() {
                        if (!project.isDisposed()) {
                            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                            FileConnectionMappingManager connectionMappingManager = FileConnectionMappingManager.getInstance(project);
                            VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
                            for (VirtualFile openFile : openFiles) {

                                ConnectionHandler activeConnection = connectionMappingManager.getActiveConnection(openFile);
                                if (activeConnection == connectionHandler) {
                                    FileEditor[] fileEditors = fileEditorManager.getEditors(openFile);
                                    for (FileEditor fileEditor : fileEditors) {
                                        Editor editor = null;
                                        if (fileEditor instanceof TextEditor) {
                                            TextEditor textEditor = (TextEditor) fileEditor;
                                            editor = textEditor.getEditor();
                                        }
                                        else if (fileEditor instanceof BasicTextEditor) {
                                            BasicTextEditor textEditor = (BasicTextEditor) fileEditor;
                                            editor = textEditor.getEditor();
                                        }

                                        if (editor != null) {
                                            DocumentUtil.refreshEditorAnnotations(editor);
                                        }

                                    }

                                }
                            }
                        }
                    }
                }.start();

            }
        });
    }

    public static DatabaseLoaderManager getInstance(Project project) {
        return project.getComponent(DatabaseLoaderManager.class);
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.Project.DatabaseLoaderManager";
    }

    public void disposeComponent() {
        super.disposeComponent();
        if (loaderQueue != null) {
            loaderQueue.dispose();
            loaderQueue = null;
        }
    }
}
