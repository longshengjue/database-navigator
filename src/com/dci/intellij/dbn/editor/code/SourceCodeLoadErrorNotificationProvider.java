package com.dci.intellij.dbn.editor.code;

import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.thread.ConditionalLaterInvocator;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.editor.code.ui.SourceCodeLoadErrorNotificationPanel;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.vfs.DatabaseEditableObjectFile;
import com.dci.intellij.dbn.vfs.SourceCodeFile;
import com.intellij.ide.FrameStateManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SourceCodeLoadErrorNotificationProvider extends EditorNotifications.Provider<SourceCodeLoadErrorNotificationPanel> {
    private static final Key<SourceCodeLoadErrorNotificationPanel> KEY = Key.create("DBNavigator.SourceCodeLoadErrorNotificationPanel");
    private Project project;

    public SourceCodeLoadErrorNotificationProvider(final Project project, @NotNull FrameStateManager frameStateManager) {
        this.project = project;

        EventManager.subscribe(project, SourceCodeLoadListener.TOPIC, sourceCodeLoadListener);

    }

    SourceCodeLoadListener sourceCodeLoadListener = new SourceCodeLoadListener() {
        @Override
        public void sourceCodeLoaded(final VirtualFile virtualFile) {
            new ConditionalLaterInvocator() {
                @Override
                public void execute() {
                    if (!project.isDisposed()) {
                        EditorNotifications notifications = EditorNotifications.getInstance(project);
                        notifications.updateNotifications(virtualFile);
                    }
                }
            }.start();
        }
    };

    @Override
    public Key<SourceCodeLoadErrorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public SourceCodeLoadErrorNotificationPanel createNotificationPanel(VirtualFile virtualFile, FileEditor fileEditor) {
        if (virtualFile instanceof DatabaseEditableObjectFile) {
            if (fileEditor instanceof SourceCodeEditor) {
                DatabaseEditableObjectFile editableObjectFile = (DatabaseEditableObjectFile) virtualFile;
                DBSchemaObject editableObject = editableObjectFile.getObject();
                SourceCodeEditor sourceCodeEditor = (SourceCodeEditor) fileEditor;
                SourceCodeFile sourceCodeFile = sourceCodeEditor.getVirtualFile();
                String sourceLoadError = sourceCodeFile.getSourceLoadError();
                if (StringUtil.isNotEmpty(sourceLoadError)) {
                    return createPanel(editableObject, sourceLoadError);
                }

            }
        }
        return null;
    }

    private SourceCodeLoadErrorNotificationPanel createPanel(final DBSchemaObject editableObject, String sourceLoadError) {
        SourceCodeLoadErrorNotificationPanel panel = new SourceCodeLoadErrorNotificationPanel();
        panel.setText("Could not load source for " + editableObject.getQualifiedNameWithType() + ". Error details: " + sourceLoadError.replace("\n", " "));
        return panel;
    }


}
