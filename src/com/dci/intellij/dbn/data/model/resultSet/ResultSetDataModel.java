package com.dci.intellij.dbn.data.model.resultSet;

import com.dci.intellij.dbn.common.content.loader.DynamicContentLoader;
import com.dci.intellij.dbn.common.thread.SimpleBackgroundTask;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionUtil;
import com.dci.intellij.dbn.data.model.sortable.SortableDataModel;
import com.dci.intellij.dbn.data.model.sortable.SortableDataModelState;
import com.intellij.openapi.util.Disposer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResultSetDataModel<T extends ResultSetDataModelRow> extends SortableDataModel<T> {
    protected ResultSet resultSet;
    protected ConnectionHandler connectionHandler;
    protected boolean resultSetExhausted = false;
    private final Object DISPOSE_LOCK = new Object();

    public ResultSetDataModel(ConnectionHandler connectionHandler) throws SQLException {
        super(connectionHandler.getProject());
        this.connectionHandler = connectionHandler;
    }

    public ResultSetDataModel(ResultSet resultSet, ConnectionHandler connectionHandler, int maxRecords) throws SQLException {
        super(connectionHandler.getProject());
        this.connectionHandler = connectionHandler;
        this.resultSet = resultSet;
        setHeader(new ResultSetDataModelHeader(connectionHandler.getObjectBundle(), resultSet));
        fetchNextRecords(maxRecords, false);
    }

    protected T createRow(int resultSetRowIndex) throws SQLException {
        return (T) new ResultSetDataModelRow(this, resultSet);
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public int fetchNextRecords(int records, boolean reset) throws SQLException {
        int originalRowCount = getRowCount();
        if (resultSetExhausted) return originalRowCount;

        int initialIndex = reset ? 0 : originalRowCount;
        int count = 0;

        final List<T> oldRows = getRows();
        List<T> newRows = reset ? new ArrayList<T>(oldRows.size()) : new ArrayList<T>(oldRows);
        if (resultSet == null) {
            resultSetExhausted = true;
        } else {
            while (count < records) {
                synchronized (DISPOSE_LOCK) {
                    checkDisposed();
                    if (resultSet.next()) {
                        count++;
                        T row = createRow(initialIndex + count);
                        newRows.add(row);
                    } else {
                        resultSetExhausted = true;
                        break;
                    }
                }
            }
        }

        checkDisposed();
        sort(newRows);
        setRows(newRows);

        if (reset) {
            new SimpleBackgroundTask() {
                @Override
                public void run() {
                    // dispose old content
                    for (T row : oldRows) {
                        disposeRow(row);
                    }
                }
            }.start();
        }

        int newRowCount = getRowCount();

        if (newRowCount > originalRowCount) notifyRowsInserted(originalRowCount, newRowCount);
        if (newRowCount < originalRowCount) notifyRowsDeleted(newRowCount, originalRowCount);
        int updateIndex = Math.min(originalRowCount, newRowCount);
        if (updateIndex > 0) notifyRowsUpdated(0, updateIndex);

        return newRowCount;
    }

    protected void checkDisposed() throws SQLException {
        if (isDisposed()) throw DynamicContentLoader.DBN_INTERRUPTED_EXCEPTION;
    }

    protected void disposeRow(T row) {
        Disposer.dispose(row);
    }

    public boolean isResultSetExhausted() {
        return resultSetExhausted;
    }

    public void closeResultSet() throws SQLException {
        ConnectionUtil.closeResultSet(resultSet);
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    @Override
    protected SortableDataModelState createState() {
        return new SortableDataModelState();
    }

    public boolean isReadonly() {
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return getColumnInfo(columnIndex).getDataType().getTypeClass();
    }

    @Override
    public void dispose() {
        if (!isDisposed()) {
            synchronized (DISPOSE_LOCK) {
                super.dispose();
                ConnectionUtil.closeResultSet(resultSet);
                resultSet = null;
                connectionHandler = null;
            }
        }
    }
}
