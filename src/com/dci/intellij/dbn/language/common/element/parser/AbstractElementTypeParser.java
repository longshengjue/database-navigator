package com.dci.intellij.dbn.language.common.element.parser;

import com.dci.intellij.dbn.code.common.completion.CodeCompletionContributor;
import com.dci.intellij.dbn.common.options.setting.SettingsUtil;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.ElementTypeBundle;
import com.dci.intellij.dbn.language.common.element.QualifiedIdentifierElementType;
import com.dci.intellij.dbn.language.common.element.SequenceElementType;
import com.dci.intellij.dbn.language.common.element.path.ParsePathNode;
import com.dci.intellij.dbn.language.common.element.path.PathNode;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeLogger;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeUtil;
import com.dci.intellij.dbn.language.common.element.util.ParseBuilderErrorHandler;
import com.intellij.lang.PsiBuilder;

public abstract class AbstractElementTypeParser<T extends ElementType> implements ElementTypeParser<T>{
    private T elementType;
    private ParseBuilderErrorHandler errorHandler;
    private ElementTypeLogger logger;

    public AbstractElementTypeParser(T elementType) {
        this.elementType = elementType;
        errorHandler = new ParseBuilderErrorHandler(elementType);
    }

    public ParsePathNode createParseNode(ParsePathNode parentParseNode, int builderOffset) {
        return new ParsePathNode(elementType, parentParseNode, builderOffset, 0);
    }

    protected boolean isDummyToken(String tokenText){
        return tokenText != null && tokenText.contains(CodeCompletionContributor.DUMMY_TOKEN);
    }

    public T getElementType() {
        return elementType;
    }

    private ElementTypeLogger getLogger() {
        if (logger == null) logger = new ElementTypeLogger(getElementType());
        return logger;
    }

    public void logBegin(ParserBuilder builder, boolean optional, int depth) {
        if (SettingsUtil.isDebugEnabled) {
            getLogger().logBegin(builder, optional, depth);
        }
    }

    public void logEnd(ParseResultType resultType, int depth) {
        if (SettingsUtil.isDebugEnabled) {
            getLogger().logEnd(resultType, depth);
        }
    }
    public ParseBuilderErrorHandler getErrorHandler() {
        return errorHandler;
    }

    protected ParseResult stepOut(PsiBuilder.Marker marker, int depth, ParseResultType resultType, int matchedTokens, ParsePathNode node, ParserContext context) {
        try {
            if (resultType == ParseResultType.PARTIAL_MATCH) {
                ParseBuilderErrorHandler.updateBuilderError(elementType.getLookupCache().getNextPossibleTokens(), context);
            }
            ParserBuilder builder = context.getBuilder();
            if (resultType == ParseResultType.NO_MATCH) {
                builder.markerRollbackTo(marker, node);
            } else
                builder.markerDone(marker, elementType, node);


            logEnd(resultType, depth);
            return resultType ==
                    ParseResultType.NO_MATCH ?
                    ParseResult.createNoMatchResult() :
                    ParseResult.createFullMatchResult(matchedTokens);
        } finally {
            if (node != null) {
                node.detach();
            }

        }
    }

    /**
     * Returns true if the token is a reserved word, but can act as an identifier in this context.
     */
    protected boolean isSuppressibleReservedWord(TokenType tokenType, PathNode node) {
        if (tokenType != null) {
            if (tokenType.isSuppressibleReservedWord()) {
                ElementType elementType = node.getElementType();
                if (elementType instanceof QualifiedIdentifierElementType) {
                    if (node.getCurrentSiblingIndex() > 0) return true;
                }

                ElementType namedElementType = ElementTypeUtil.getEnclosingNamedElementType(node);
                if (namedElementType != null && namedElementType.getLookupCache().containsToken(tokenType)) {
                    return false;
                }


                return true;//!isFollowedByToken(tokenType, node);
            }
        }
        return false;
    }

    private boolean isFollowedByToken(TokenType tokenType, PathNode node) {
        PathNode parent = node;
        while (parent != null) {
            if (parent.getElementType() instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) parent.getElementType();
                if (sequenceElementType.isPossibleTokenFromIndex(tokenType, parent.getCurrentSiblingIndex() + 1)) {
                    return true;
                }
            }
            // break when statement boundary found
            /*if (parent.getElementType().is(ElementTypeAttribute.STATEMENT)) {
                return false;
            }*/
            parent = parent.getParent();
        }
        return false;
    }

    public ElementTypeBundle getElementBundle() {
        return elementType.getElementBundle();
    }
}
