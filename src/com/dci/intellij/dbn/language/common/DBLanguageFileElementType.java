package com.dci.intellij.dbn.language.common;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IFileElementType;

public class DBLanguageFileElementType extends IFileElementType {
    public DBLanguageFileElementType(Language language) {
        super(language);                                                         
    }

    public ASTNode parseContents(ASTNode chameleon) {
        DBLanguageFile file = (DBLanguageFile) chameleon.getPsi();
        Project project = file.getProject();
        DBLanguageDialect languageDialect = file.getLanguageDialect();
        if (languageDialect == null) {
            return super.parseContents(chameleon);
        }

        /*DBLanguageFile originalFile = (DBLanguageFile) file.getViewProvider().getAllFiles().get(0).getOriginalFile();
        if (originalFile != null)  file = originalFile;*/

        String text = chameleon.getText();
        ParserDefinition parserDefinition = languageDialect.getParserDefinition();
        Lexer lexer = parserDefinition.createLexer(project);

        DBLanguageParser parser = (DBLanguageParser) parserDefinition.createParser(project);

        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, lexer, languageDialect, text);
        ASTNode node = parser.parse(this, builder, file.getParseRootId());
        return node.getFirstChildNode();
    }
}
