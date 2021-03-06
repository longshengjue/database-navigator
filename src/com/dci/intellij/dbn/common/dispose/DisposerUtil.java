package com.dci.intellij.dbn.common.dispose;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;

import java.util.Collection;
import java.util.Map;

public class DisposerUtil {
    public static void dispose(Disposable disposable) {
        if (disposable != null) {
            Disposer.dispose(disposable);
        }
    }

    public static void dispose(Collection<? extends Disposable> collection) {
        if (collection != null) {
            for(Disposable disposable : collection) {
                dispose(disposable);
            }
            collection.clear();
        }
    }
    
    public static void dispose(Map<?, ? extends Disposable> map) {
        if (map != null) {
            for (Disposable disposable : map.values()) {
                dispose(disposable);
            }
            map.clear();
        }
    }


    public static void register(Disposable parent, Collection<? extends Disposable> collection) {
        for (Disposable disposable : collection) {
            Disposer.register(parent, disposable);
        }
    }
}