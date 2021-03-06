package com.dci.intellij.dbn.code.common.completion;

import com.dci.intellij.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dci.intellij.dbn.code.common.completion.options.filter.CodeCompletionFilterSettings;
import com.dci.intellij.dbn.code.common.style.options.ProjectCodeStyleSettings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.common.DBLanguageFile;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.dci.intellij.dbn.options.GlobalProjectSettings;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiElement;

public class CodeCompletionContext {
    private boolean extended;
    private DBLanguageFile file;
    private ProjectCodeStyleSettings codeStyleSettings;
    private CodeCompletionSettings codeCompletionSettings;
    private CompletionParameters parameters;
    private CompletionResultSet result;
    private PsiElement elementAtCaret;
    private ConnectionHandler connectionHandler;
    private String userInput;


    public CodeCompletionContext(DBLanguageFile file, CompletionParameters parameters, CompletionResultSet result) {
        this.file = file;
        this.parameters = parameters;
        this.result = result;
        this.extended = parameters.getCompletionType() == CompletionType.SMART;
        this.connectionHandler = file.getActiveConnection();

        PsiElement position = parameters.getPosition();
        if (parameters.getOffset() > position.getTextOffset()) {
            userInput = position.getText().substring(0, parameters.getOffset() - position.getTextOffset());
        }

        GlobalProjectSettings globalSettings = GlobalProjectSettings.getInstance(file.getProject());
        codeStyleSettings = globalSettings.getCodeStyleSettings();
        codeCompletionSettings = globalSettings.getCodeCompletionSettings();

        elementAtCaret = position instanceof BasePsiElement ? (BasePsiElement) position : PsiUtil.lookupLeafAtOffset(file, position.getTextOffset());
        elementAtCaret = elementAtCaret == null ? file : elementAtCaret;
    }

    public String getUserInput() {
        return userInput;
    }

    public CompletionParameters getParameters() {
        return parameters;
    }

    public CompletionResultSet getResult() {
        return result;
    }

    public PsiElement getElementAtCaret() {
        return elementAtCaret;
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public boolean isExtended() {
        return extended;
    }

    public ProjectCodeStyleSettings getCodeStyleSettings() {
        return codeStyleSettings;
    }

    public CodeCompletionSettings getCodeCompletionSettings() {
        return codeCompletionSettings;
    }

    public CodeCompletionFilterSettings getCodeCompletionFilterSettings() {
        return codeCompletionSettings.getFilterSettings().getFilterSettings(extended);
    }

    public DBLanguageFile getFile() {
        return file;
    }

    public DBLanguage getLanguage() {
        DBLanguageDialect languageDialect = file.getLanguageDialect();
        return languageDialect == null ? SQLLanguage.INSTANCE : languageDialect.getBaseLanguage();
    }
}
