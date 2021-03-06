package com.dci.intellij.dbn.debugger.execution;

import com.dci.intellij.dbn.common.Constants;
import com.dci.intellij.dbn.common.thread.BackgroundTask;
import com.dci.intellij.dbn.common.thread.SimpleLaterInvocator;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.debugger.DBProgramDebugProcessStarter;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.debugger.execution.ui.CompileDebugDependenciesDialog;
import com.dci.intellij.dbn.execution.compiler.CompileType;
import com.dci.intellij.dbn.execution.compiler.DatabaseCompilerManager;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.execution.method.MethodExecutionManager;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.history.LocalHistory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DBProgramRunner extends GenericProgramRunner {
    public static final String RUNNER_ID = "DBNavigatorProgramRunner";

    @NotNull
    public String getRunnerId() {
        return RUNNER_ID;
    }

    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (profile instanceof DBProgramRunConfiguration) {
            DBProgramRunConfiguration runConfiguration = (DBProgramRunConfiguration) profile;
            return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && runConfiguration.getMethod() != null;
        }
        return false;
    }

    @Nullable
    protected RunContentDescriptor doExecute(Project project, RunProfileState state, RunContentDescriptor contentToReuse, ExecutionEnvironment env) throws ExecutionException {
        return doExecute(project, env.getExecutor(), state, contentToReuse, env);
    }

    protected RunContentDescriptor doExecute(
            Project project,
            final Executor executor,
            RunProfileState state,
            RunContentDescriptor contentToReuse,
            final ExecutionEnvironment environment) throws ExecutionException {

        final DBProgramRunConfiguration runProfile = (DBProgramRunConfiguration) environment.getRunProfile();

        new BackgroundTask(runProfile.getProject(), "Checking debug privileges", false, true) {
            public void execute(@NotNull ProgressIndicator progressIndicator) {
                initProgressIndicator(progressIndicator, true);
                performPrivilegeCheck(
                        runProfile.getExecutionInput(),
                        executor,
                        environment,
                        null);

            }
        }.start();

        return null;
    }

    private void performPrivilegeCheck(
            final MethodExecutionInput executionInput,
            final Executor executor,
            final ExecutionEnvironment environment,
            final Callback callback) {
        final DBProgramRunConfiguration runProfile = (DBProgramRunConfiguration) environment.getRunProfile();
        final ConnectionHandler connectionHandler = runProfile.getMethod().getConnectionHandler();
        Project project = connectionHandler.getProject();

        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
        final List<String> missingPrivileges = debuggerManager.getMissingDebugPrivileges(connectionHandler);
        if (missingPrivileges.size() > 0) {
            new SimpleLaterInvocator() {
                public void execute() {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("The current user (").append(connectionHandler.getUserName()).append(") does not have sufficient privileges to perform debug operations on this database.\n");
                    buffer.append("Please contact your administrator to grant the required privileges. ");
                    buffer.append("Missing privileges:\n");
                    for (String missingPrivilege : missingPrivileges) {
                        buffer.append(" - ").append(missingPrivilege).append("\n");
                    }

                    int response = Messages.showDialog(
                                    buffer.toString(),
                                    Constants.DBN_TITLE_PREFIX + "Insufficient privileges",
                                    new String[]{"Continue anyway", "Cancel"}, 0,
                                    Messages.getWarningIcon());
                    if (response == 0) {
                        performInitialize(
                                executionInput,
                                executor,
                                environment,
                                callback);
                    }
                }
            }.start();
        } else {
            performInitialize(
                    executionInput,
                    executor,
                    environment,
                    callback);
        }
    }

    private void performInitialize(
            final MethodExecutionInput executionInput,
            final Executor executor,
            final ExecutionEnvironment environment,
            final Callback callback) {
        final DBProgramRunConfiguration runProfile = (DBProgramRunConfiguration) environment.getRunProfile();
        if (runProfile.isCompileDependencies()) {
            final ConnectionHandler connectionHandler = runProfile.getMethod().getConnectionHandler();
            final Project project = connectionHandler.getProject();


            new BackgroundTask(project, "Initializing debug environment", false, true) {
                public void execute(@NotNull ProgressIndicator progressIndicator) {
                    DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                    initProgressIndicator(progressIndicator, true, "Loading dependencies of " + runProfile.getMethod().getQualifiedNameWithType());
                    if (!project.isDisposed() && !progressIndicator.isCanceled()) {

                        DBMethod method = executionInput.getMethod();
                        List<DBSchemaObject> dependencies = debuggerManager.loadCompileDependencies(method, progressIndicator);
                        if (!progressIndicator.isCanceled()) {
                            if (dependencies.size() > 0) {
                                performCompile(
                                        executionInput,
                                        executor,
                                        environment,
                                        callback,
                                        dependencies);
                            } else {
                                performExecution(
                                        executionInput,
                                        executor,
                                        environment,
                                        callback);
                            }
                        }
                    }
                }
            }.start();
        } else {
            performExecution(
                    executionInput,
                    executor,
                    environment,
                    callback);
        }

    }

    private void performCompile(
            final MethodExecutionInput executionInput,
            final Executor executor,
            final ExecutionEnvironment environment,
            final Callback callback,
            final List<DBSchemaObject> dependencies) {

        new SimpleLaterInvocator() {
            public void execute() {
                final Project project = executionInput.getProject();
                DBProgramRunConfiguration runConfiguration = (DBProgramRunConfiguration) environment.getRunProfile();
                CompileDebugDependenciesDialog dependenciesDialog = new CompileDebugDependenciesDialog(runConfiguration, dependencies);
                dependenciesDialog.show();
                final List<DBSchemaObject> selectedDependencies =  dependenciesDialog.getSelection();

                if (dependenciesDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE){
                    if (selectedDependencies.size() > 0) {
                        new BackgroundTask(project, "Compiling dependencies", false, true){
                            @Override
                            public void execute(@NotNull ProgressIndicator progressIndicator) {
                                initProgressIndicator(progressIndicator, true);
                                DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
                                for (DBSchemaObject schemaObject : selectedDependencies) {
                                    if (!progressIndicator.isCanceled()) {
                                        progressIndicator.setText("Compiling " + schemaObject.getQualifiedNameWithType());
                                        compilerManager.compileObject(schemaObject, CompileType.DEBUG, true);
                                    }
                                }
                                executionInput.getConnectionHandler().getObjectBundle().refreshObjectsStatus();
                                if (!progressIndicator.isCanceled()) {
                                    performExecution(
                                            executionInput,
                                            executor,
                                            environment,
                                            callback);
                                }
                            }
                        }.start();
                    } else {
                        performExecution(
                                executionInput,
                                executor,
                                environment,
                                callback);
                    }
                }
            }
        }.start();
    }

    private void performExecution(
            final MethodExecutionInput executionInput,
            final Executor executor,
            final ExecutionEnvironment environment,
            final Callback callback) {
        new SimpleLaterInvocator() {
            public void execute() {
                final ConnectionHandler connectionHandler = executionInput.getConnectionHandler();
                final Project project = connectionHandler.getProject();

                MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
                boolean continueExecution = executionManager.promptExecutionDialog(executionInput, true);

                if (continueExecution) {
                    RunContentDescriptor reuseContent = environment.getContentToReuse();
                    DBProgramDebugProcessStarter debugProcessStarter = new DBProgramDebugProcessStarter(connectionHandler);
                    XDebugSession session = null;
                    try {
                        session = XDebuggerManager.getInstance(project).startSession(
                                DBProgramRunner.this,
                                environment,
                                reuseContent,
                                debugProcessStarter);

                        RunContentDescriptor descriptor = session.getRunContentDescriptor();

                        if (callback != null) callback.processStarted(descriptor);

                        if (true /*LocalHistoryConfiguration.getInstance().ADD_LABEL_ON_RUNNING*/) {
                            RunProfile runProfile = environment.getRunProfile();
                            LocalHistory.getInstance().putSystemLabel(project, executor.getId() + " " + runProfile.getName());
                        }

                        ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, descriptor);
                        ProcessHandler processHandler = descriptor.getProcessHandler();
                        if (processHandler != null) processHandler.startNotify();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

}

