package com.dci.intellij.dbn.editor.code.action;

import com.dci.intellij.dbn.vfs.SourceCodeFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSourceCodeEditorAction extends DumbAwareAction {
    public AbstractSourceCodeEditorAction(String text, String description, javax.swing.Icon icon) {
        super(text, description, icon);
    }

    @Nullable
    protected Editor getEditor(AnActionEvent e) {
        return e.getData(PlatformDataKeys.EDITOR);
    }

    @Nullable
    protected SourceCodeFile getSourcecodeFile(AnActionEvent e) {
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        return virtualFile instanceof SourceCodeFile ? (SourceCodeFile) virtualFile : null;
    }
}
