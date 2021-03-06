package com.dci.intellij.dbn.code.common.style;

import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.util.DocumentUtil;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.language.common.psi.LeafPsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.language.common.psi.TokenPsiElement;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DBLCodeStyleManager extends AbstractProjectComponent implements JDOMExternalizable {
    private DBLCodeStyleManager(Project project) {
        super(project);
    }

    public static DBLCodeStyleManager getInstance(Project project) {
        return project.getComponent(DBLCodeStyleManager.class);
    }

    public void formatCase(PsiFile file) {
        Document document = DocumentUtil.getDocument(file);
        if (document != null && document.isWritable()) {
            Editor[] editors = EditorFactory.getInstance().getEditors(document);
            if (editors.length == 1) {
                Editor editor = editors[0];
                SelectionModel selectionModel = editor.getSelectionModel();
                int selectionStart = selectionModel.getSelectionStart();
                int selectionEnd = selectionModel.getSelectionEnd();
                format(document, file, selectionStart, selectionEnd);
            }
        }
    }

    public CodeStyleCaseSettings getCodeStyleCaseSettings(DBLanguage language) {
        return language.getCodeStyleSettings(getProject()).getCaseSettings();
    }

    private void format(Document document, PsiElement psiElement, int startOffset, int endOffset){
        Language language = PsiUtil.getLanguage(psiElement);
        if (language instanceof DBLanguage) {
            CodeStyleCaseSettings styleCaseSettings = getCodeStyleCaseSettings((DBLanguage) language);
            PsiElement child = psiElement.getFirstChild();
            while (child != null) {
                if (child instanceof LeafPsiElement) {
                    TextRange textRange = child.getTextRange();
                    boolean isInRange =
                            startOffset == endOffset || (
                                    textRange.getStartOffset() >= startOffset &&
                                            textRange.getEndOffset() <= endOffset);
                    if (isInRange) {
                        CodeStyleCaseOption caseOption = null;
                        if (child instanceof IdentifierPsiElement) {
                            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) child;
                            if (identifierPsiElement.isObject()) {
                                caseOption = styleCaseSettings.getObjectCaseOption();
                            }
                        }
                        else if (child instanceof TokenPsiElement) {
                            TokenPsiElement tokenPsiElement = (TokenPsiElement) child;
                            TokenType tokenType = tokenPsiElement.getElementType().getTokenType();
                            caseOption =
                                    tokenType.isKeyword() ? styleCaseSettings.getKeywordCaseOption() :
                                            tokenType.isFunction() ? styleCaseSettings.getFunctionCaseOption() :
                                                    tokenType.isParameter() ? styleCaseSettings.getParameterCaseOption() :
                                                            tokenType.isDataType() ? styleCaseSettings.getDatatypeCaseOption() : null;
                        }

                        if (caseOption != null) {
                            String text = child.getText();
                            String newText = caseOption.changeCase(text);

                            if (newText != null && !newText.equals(text))
                                document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), newText);

                        }
                    }
                } else {
                    format(document, child, startOffset, endOffset);
                }
                child = child.getNextSibling();
            }
        }
    }

    /****************************************
    *            ProjectComponent           *
    *****************************************/
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.Project.CodeStyleManager";
    }
    /****************************************
    *            JDOMExternalizable         *
    *****************************************/
    public void readExternal(Element element) throws InvalidDataException {

    }

    public void writeExternal(Element element) throws WriteExternalException {

    }
}