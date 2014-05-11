package com.dci.intellij.dbn.common.content.loader;

import com.dci.intellij.dbn.common.content.DynamicContent;
import com.dci.intellij.dbn.common.content.DynamicContentElement;

public interface DynamicContentLoader<T extends DynamicContentElement> {
    void loadContent(DynamicContent<T> dynamicContent, boolean forceReload) throws DynamicContentLoadException, InterruptedException;
    void reloadContent(DynamicContent<T> dynamicContent) throws DynamicContentLoadException, InterruptedException;
}
