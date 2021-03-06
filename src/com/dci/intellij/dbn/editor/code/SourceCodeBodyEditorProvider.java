package com.dci.intellij.dbn.editor.code;

import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.vfs.DatabaseEditableObjectFile;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SourceCodeBodyEditorProvider extends BasicSourceCodeEditorProvider{

    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        DatabaseEditableObjectFile databaseFile = null;
        if (virtualFile instanceof DatabaseEditableObjectFile) {
            databaseFile = (DatabaseEditableObjectFile) virtualFile;
        }

/*
        else if (virtualFile instanceof SourceCodeFile) {
            SourceCodeFile sourceCodeFile = (SourceCodeFile) virtualFile;
            databaseFile = sourceCodeFile.getDatabaseFile();
        }
*/

        return databaseFile != null && databaseFile.getObject().getContentType() == DBContentType.CODE_SPEC_AND_BODY;
    }

    @Override
    public DBContentType getContentType() {
        return DBContentType.CODE_BODY;
    }

    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;

    }

    @NotNull
    @NonNls
    public String getEditorTypeId() {
        return "2";
    }

    public String getName() {
        return "Body";
    }

    @Override
    public Icon getIcon() {
        return null;//Icons.CODE_EDITOR_BODY;
    }

    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/

    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.DBSourceBodyEditorProvider";
    }
}
