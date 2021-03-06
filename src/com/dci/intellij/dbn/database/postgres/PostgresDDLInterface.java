package com.dci.intellij.dbn.database.postgres;

import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.code.psql.style.options.PSQLCodeStyleSettings;
import com.dci.intellij.dbn.database.DatabaseInterfaceProvider;
import com.dci.intellij.dbn.database.common.DatabaseDDLInterfaceImpl;
import com.dci.intellij.dbn.object.factory.ArgumentFactoryInput;
import com.dci.intellij.dbn.object.factory.MethodFactoryInput;
import com.intellij.openapi.util.text.StringUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class PostgresDDLInterface extends DatabaseDDLInterfaceImpl {
    public PostgresDDLInterface(DatabaseInterfaceProvider provider) {
        super("postgres_ddl_interface.xml", provider);
    }

    public String getSessionSqlMode(Connection connection) throws SQLException {
        return getSingleValue(connection, "get-session-sql-mode");
    }

    public void setSessionSqlMode(String sqlMode, Connection connection) throws SQLException {
        if (sqlMode != null) {
            executeQuery(connection, true, "set-session-sql-mode", sqlMode);
        }
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    public void updateView(String viewName, String oldCode, String newCode, Connection connection) throws SQLException {
        executeUpdate(connection, "change-view", viewName, newCode);
    }

    public void updateTrigger(String tableOwner, String tableName, String triggerName, String oldCode, String newCode, Connection connection) throws SQLException {
        executeUpdate(connection, "drop-trigger", tableOwner, tableName, triggerName);
        try {
            createObject(newCode, connection);
        } catch (SQLException e) {
            createObject(oldCode, connection);
            throw e;
        }
    }

    public void updateObject(String objectName, String objectType, String oldCode, String newCode, Connection connection) throws SQLException {
        executeUpdate(connection, "change-object", newCode);
    }

    /*********************************************************
     *                     DROP statements                   *
     *********************************************************/
    private void dropTriggerIfExists(String objectName, Connection connection) throws SQLException {

    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    public void createMethod(MethodFactoryInput method, Connection connection) throws SQLException {
        CodeStyleCaseSettings styleCaseSettings = PSQLCodeStyleSettings.getInstance(method.getSchema().getProject()).getCaseSettings();
        CodeStyleCaseOption keywordCaseOption = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption objectCaseOption = styleCaseSettings.getObjectCaseOption();
        CodeStyleCaseOption dataTypeCaseOption = styleCaseSettings.getDatatypeCaseOption();

        StringBuilder buffer = new StringBuilder();
        String methodType = method.isFunction() ? "function " : "procedure ";
        buffer.append(keywordCaseOption.changeCase(methodType));
        buffer.append(objectCaseOption.changeCase(method.getObjectName()));
        buffer.append("(");

        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        for (ArgumentFactoryInput argument : method.getArguments()) {
            maxArgNameLength = Math.max(maxArgNameLength, argument.getObjectName().length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength,
                    argument.isInput() && argument.isOutput() ? 5 :
                    argument.isInput() ? 2 :
                    argument.isOutput() ? 3 : 0);
        }


        for (ArgumentFactoryInput argument : method.getArguments()) {
            buffer.append("\n    ");
            
            if (!method.isFunction()) {
                String direction =
                        argument.isInput() && argument.isOutput() ? keywordCaseOption.changeCase("inout") :
                                argument.isInput() ? keywordCaseOption.changeCase("in") :
                                        argument.isOutput() ? keywordCaseOption.changeCase("out") : "";
                buffer.append(direction);
                buffer.append(StringUtil.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            }

            buffer.append(objectCaseOption.changeCase(argument.getObjectName()));
            buffer.append(StringUtil.repeatSymbol(' ', maxArgNameLength - argument.getObjectName().length() + 1));

            buffer.append(dataTypeCaseOption.changeCase(argument.getDataType()));
            if (argument != method.getArguments().get(method.getArguments().size() -1)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (method.isFunction()) {
            buffer.append(keywordCaseOption.changeCase("returns "));
            buffer.append(dataTypeCaseOption.changeCase(method.getReturnArgument().getDataType()));
            buffer.append("\n");
        }
        buffer.append(keywordCaseOption.changeCase("begin\n\n"));
        if (method.isFunction()) buffer.append(keywordCaseOption.changeCase("    return null;\n\n"));
        buffer.append("end");
        
        String sqlMode = getSessionSqlMode(connection);
        try {
            setSessionSqlMode("TRADITIONAL", connection);
            createObject(buffer.toString(), connection);
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }

    }

}
