package com.dci.intellij.dbn.editor.data;

import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.options.setting.SettingsUtil;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.data.record.ColumnSortingType;
import com.dci.intellij.dbn.data.record.DatasetRecord;
import com.dci.intellij.dbn.data.record.navigation.RecordNavigationTarget;
import com.dci.intellij.dbn.data.record.navigation.action.RecordNavigationActionGroup;
import com.dci.intellij.dbn.data.record.ui.RecordViewerDialog;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterInput;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterManager;
import com.dci.intellij.dbn.editor.data.options.DataEditorSettings;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.vfs.DatabaseEditableObjectFile;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

public class DatasetEditorManager extends AbstractProjectComponent implements JDOMExternalizable {
    public static final DatasetLoadInstructions INITIAL_LOAD_INSTRUCTIONS = new DatasetLoadInstructions(true, true, true, true);
    public static final DatasetLoadInstructions RELOAD_LOAD_INSTRUCTIONS = new DatasetLoadInstructions(true, true, true, false);
    private ColumnSortingType recordViewColumnSortingType = ColumnSortingType.BY_INDEX;
    private boolean valuePreviewTextWrapping = true;
    private boolean valuePreviewPinned = false;

    private DatasetEditorManager(Project project) {
        super(project);
    }

    public static DatasetEditorManager getInstance(Project project) {
        return project.getComponent(DatasetEditorManager.class);
    }

    public void reloadEditorData(DBDataset dataset) {
        VirtualFile file = dataset.getVirtualFile();
        FileEditor[] fileEditors = FileEditorManager.getInstance(getProject()).getEditors(file);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof DatasetEditor) {
                DatasetEditor datasetEditor = (DatasetEditor) fileEditor;
                datasetEditor.loadData(RELOAD_LOAD_INSTRUCTIONS);
                break;
            }
        }
    }

    public void openDataEditor(DatasetFilterInput filterInput) {
        DBDataset dataset = filterInput.getDataset();
        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
        filterManager.createBasicFilter(filterInput);
        DatabaseFileSystem.getInstance().openEditor(dataset);
    }
    
    public void openRecordViewer(DatasetFilterInput filterInput) {
        try {
            DatasetRecord record = new DatasetRecord(filterInput);
            RecordViewerDialog dialog = new RecordViewerDialog(record);
            dialog.show();
        } catch (SQLException e) {
            MessageUtil.showErrorDialog("Could not load record details", e);
        }
    }

    public void navigateToRecord(DatasetFilterInput filterInput, InputEvent inputEvent) {
        DataEditorSettings settings = DataEditorSettings.getInstance(getProject());
        RecordNavigationTarget navigationTarget = settings.getRecordNavigationSettings().getNavigationTarget();
        if (navigationTarget == RecordNavigationTarget.EDITOR) {
            openDataEditor(filterInput);
        } else if (navigationTarget == RecordNavigationTarget.VIEWER) {
            openRecordViewer(filterInput);
        } else if (navigationTarget == RecordNavigationTarget.ASK) {
            ActionGroup actionGroup = new RecordNavigationActionGroup(filterInput);
            Component component = (Component) inputEvent.getSource();

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    "Select navigation target",
                    actionGroup,
                    DataManager.getInstance().getDataContext(component),
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    true, null, 10);

            if (inputEvent instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) inputEvent;
                popup.showInScreenCoordinates(component, mouseEvent.getLocationOnScreen());
                        
            } else {
                popup.show(component);
            }

            
        }
    }

    public void setRecordViewColumnSortingType(ColumnSortingType columnSorting) {
        recordViewColumnSortingType = columnSorting;
    }

    public ColumnSortingType getRecordViewColumnSortingType() {
        return recordViewColumnSortingType;
    }

    public boolean isValuePreviewTextWrapping() {
        return valuePreviewTextWrapping;
    }

    public void setValuePreviewTextWrapping(boolean valuePreviewTextWrapping) {
        this.valuePreviewTextWrapping = valuePreviewTextWrapping;
    }

    public boolean isValuePreviewPinned() {
        return valuePreviewPinned;
    }

    public void setValuePreviewPinned(boolean valuePreviewPinned) {
        this.valuePreviewPinned = valuePreviewPinned;
    }

    /****************************************
    *             ProjectComponent          *
    *****************************************/
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.Project.DataEditorManager";
    }

    FileEditorManagerAdapter fileEditorListener = new FileEditorManagerAdapter() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (file instanceof DatabaseEditableObjectFile) {
                DatabaseEditableObjectFile editableObjectFile = (DatabaseEditableObjectFile) file;
                DBSchemaObject object = editableObjectFile.getObject();
                if (object instanceof DBDataset) {
                    FileEditor[] fileEditors = source.getEditors(file);
                    for (FileEditor fileEditor : fileEditors) {
                        if (fileEditor instanceof DatasetEditor) {
                            DatasetEditor datasetEditor = (DatasetEditor) fileEditor;
                            datasetEditor.loadData(INITIAL_LOAD_INSTRUCTIONS);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void initComponent() {
        EventManager.subscribe(getProject(), FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorListener);
    }

    @Override
    public void disposeComponent() {
        EventManager.unsubscribe(fileEditorListener);
    }

    /****************************************
    *            JDOMExternalizable         *
    *****************************************/
    public void readExternal(Element element) throws InvalidDataException {
        recordViewColumnSortingType = SettingsUtil.getEnum(element, "record-view-column-sorting-type", recordViewColumnSortingType);
        valuePreviewTextWrapping = SettingsUtil.getBoolean(element, "value-preview-text-wrapping", valuePreviewTextWrapping);
        valuePreviewTextWrapping = SettingsUtil.getBoolean(element, "value-preview-pinned", valuePreviewPinned);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        SettingsUtil.setEnum(element, "record-view-column-sorting-type", recordViewColumnSortingType);
        SettingsUtil.setBoolean(element, "value-preview-text-wrapping", valuePreviewTextWrapping);
        SettingsUtil.setBoolean(element, "value-preview-pinned", valuePreviewPinned);
    }

}
