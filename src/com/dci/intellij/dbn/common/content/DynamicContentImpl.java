package com.dci.intellij.dbn.common.content;

import com.dci.intellij.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dci.intellij.dbn.common.content.dependency.VoidContentDependencyAdapter;
import com.dci.intellij.dbn.common.content.loader.DynamicContentLoadException;
import com.dci.intellij.dbn.common.content.loader.DynamicContentLoader;
import com.dci.intellij.dbn.common.dispose.DisposerUtil;
import com.dci.intellij.dbn.common.filter.Filter;
import com.dci.intellij.dbn.common.thread.BackgroundTask;
import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.GenericDatabaseElement;
import com.intellij.openapi.progress.ProgressIndicator;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class DynamicContentImpl<T extends DynamicContentElement> implements DynamicContent<T> {
    public static final List EMPTY_LIST = new ArrayList(0);

    private final Object LOAD_LOCK = new Object();
    private final Object BACKGROUND_LOAD_LOCK = new Object();

    private long changeTimestamp = 0;
    private volatile boolean isLoading = false;
    private volatile boolean isLoadingInBackground = false;
    private volatile boolean isLoaded = false;
    private volatile boolean isDirty = false;
    private volatile boolean isDisposed = false;

    private GenericDatabaseElement parent;
    protected DynamicContentLoader<T> loader;
    protected ContentDependencyAdapter dependencyAdapter;
    private boolean indexed;
    private Map<String, T> index;
    private int filterHashCode = 0;

    protected List<T> elements = EMPTY_LIST;

    protected DynamicContentImpl(GenericDatabaseElement parent, DynamicContentLoader<T> loader, ContentDependencyAdapter dependencyAdapter, boolean indexed) {
        this.parent = parent;
        this.loader = loader;
        this.dependencyAdapter = dependencyAdapter;
        this.indexed = indexed;
    }

    public boolean accepts(T element) {
        Filter filter = getFilter();
        return filter == null || filter.accepts(element);
    }

    public abstract Filter getFilter();

    public GenericDatabaseElement getParent() {
        return parent;
    }

    public ConnectionHandler getConnectionHandler() {
        return parent == null ? null : parent.getConnectionHandler();
    }

    public DynamicContentLoader<T> getLoader() {
        return loader;
    }

    public ContentDependencyAdapter getDependencyAdapter() {
        return dependencyAdapter;
    }

    public long getChangeTimestamp() {
        return changeTimestamp;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * The content can load
     */
    public boolean canLoadFast() {
        return dependencyAdapter.canLoadFast();
    }

    @Override
    public boolean isSubContent() {
        return getDependencyAdapter().isSubContent();
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isDirty() {
        if (isDirty || dependencyAdapter.isDirty()) {
            return true;
        }

        Filter filter = getFilter();
        return filter == null ?
                filterHashCode != 0 :
                filterHashCode != filter.hashCode();

        //return isDirty /*|| (elements.size() > 0 && elements.get(0).isDisposed())*/;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public final void load(boolean force) {
        synchronized (LOAD_LOCK) {
            if (shouldLoad(force)) {
                isLoading = true;
                try {
                    performLoad();
                    isLoaded = true;
                } catch (InterruptedException e) {
                    setElements(EMPTY_LIST);
                    isDirty = true;
                } finally {
                    isLoading = false;
                    updateChangeTimestamp();
                }
            }
        }
    }

    public final void reload() {
        synchronized (LOAD_LOCK) {
            if (!isDisposed && !isLoading) {
                isLoading = true;
                try {
                    performReload();
                    isLoaded = true;
                } catch (InterruptedException e) {
                    setElements(EMPTY_LIST);
                    isDirty = true;
                } finally {
                    isLoading = false;
                    updateChangeTimestamp();
                }
            }
        }
    }

    @Override
    public final void loadInBackground(final boolean force) {
        synchronized (BACKGROUND_LOAD_LOCK) {
            if (!isLoadingInBackground && shouldLoad(force)) {
                isLoadingInBackground = true;
                new BackgroundTask(getProject(), "Loading data dictionary", true) {
                    public void execute(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            DatabaseLoadMonitor.startBackgroundLoad();
                            load(force);
                        } finally {
                            DatabaseLoadMonitor.endBackgroundLoad();
                            isLoadingInBackground = false;
                        }
                    }
                }.start();
            }
        }
    }

    private void performLoad() throws InterruptedException {
        checkDisposed();
        dependencyAdapter.beforeLoad();
        checkDisposed();
        try {
            // mark first the dirty status since dirty dependencies may
            // become valid due to parallel background load
            isDirty = false;
            getLoader().loadContent(this, false);
        } catch (DynamicContentLoadException e) {
            isDirty = !e.isModelException();
        }
        checkDisposed();
        dependencyAdapter.afterLoad();
    }

    private void performReload() throws InterruptedException {
        checkDisposed();
        dependencyAdapter.beforeReload(this);
        checkDisposed();
        try {
            checkDisposed();
            getLoader().reloadContent(this);
        } catch (DynamicContentLoadException e) {
            isDirty = !e.isModelException();
        }
        checkDisposed();
        dependencyAdapter.afterReload(this);
    }

    public void updateChangeTimestamp() {
        changeTimestamp = System.currentTimeMillis();
    }


    /**
     * do whatever is needed after the content is loaded (e.g. refresh browser tree..)
     */
    public abstract void notifyChangeListeners();

    public void setElements(List<T> elements) {
        Filter filter = getFilter();
        filterHashCode = filter == null ? 0 : filter.hashCode();

        if (isDisposed || elements == null || elements.size() == 0) {
            elements = EMPTY_LIST;
            index = null;
        } else {
            Collections.sort(elements);

        }
        List<T> oldElements = this.elements;
        this.elements = elements;
        updateIndex();
        if (oldElements.size() != 0 || elements.size() != 0 ){
            notifyChangeListeners();
        }
        if (!dependencyAdapter.isSubContent() && oldElements.size() > 0 ) {
            DisposerUtil.dispose(oldElements);
        }
    }

    @NotNull
    public List<T> getElements() {
        if (isSubContent() || DatabaseLoadMonitor.isEnsureDataLoaded() || DatabaseLoadMonitor.isLoadingInBackground()) {
            load(false);
        } else{
            loadInBackground(false);
        }
        return elements;
    }

    protected void updateIndex() {
        if (indexed) {
            if (elements.size() > 0) {
                if (index == null)
                    index = new THashMap<String, T>(); else
                    index.clear();

                for (T element : elements) {
                    String name = element.getName().toUpperCase();
                    index.put(name, element);
                }
            } else {
                index = null;
            }
        }
    }

    public T getElement(String name) {
        if (name != null) {
            if (indexed && index != null) {
                return index.get(name.toUpperCase());
            } else {
                List<T> elements = getElements();
                for (T element : elements) {
                    if (element.getName().equalsIgnoreCase(name)) {
                        return element;
                    }
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    public List<T> getElements(String name) {
        List<T> elements = null;
        for (T element : getElements()) {
            if (element.getName().equalsIgnoreCase(name)) {
                if (elements == null) {
                    elements = new ArrayList<T>();
                }
                elements.add(element);
            }
        }
        return elements;
    }

    public int size() {
        return getElements().size();
    }

    public boolean shouldLoad(boolean force) {
        return !(isLoading || isDisposed) && (force || !isLoaded || (isDirty() && dependencyAdapter.shouldLoadIfDirty()));
    }

    public void checkDisposed() throws InterruptedException {
        if (isDisposed) throw new InterruptedException();
    }

    public void dispose() {
        if (!isDisposed) {
            isDisposed = true;
            if (dependencyAdapter.isSubContent())
                elements.clear(); else
                DisposerUtil.dispose(elements);
            CollectionUtil.clearMap(index);
            dependencyAdapter.dispose();
            dependencyAdapter = VoidContentDependencyAdapter.INSTANCE;
            parent = null;
        }
    }
}
