package com.dci.intellij.dbn.execution.statement.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ExecutionResultViewRecordAction extends AbstractExecutionResultAction {
    public ExecutionResultViewRecordAction() {
        super("View record", Icons.EXEC_RESULT_VIEW_RECORD);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        StatementExecutionCursorResult executionResult = getExecutionResult(e);
        if (executionResult != null) {
            executionResult.getResultTable().showRecordViewDialog();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        StatementExecutionCursorResult executionResult = getExecutionResult(e);
        boolean enabled = executionResult != null && executionResult.getResultTable().getSelectedColumn() > -1;
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setText("View record");
    }
}
