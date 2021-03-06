package com.dci.intellij.dbn.data.model.sortable;

import com.dci.intellij.dbn.data.model.DataModelCell;
import com.dci.intellij.dbn.data.model.basic.BasicDataModelCell;
import org.jetbrains.annotations.NotNull;

public class SortableDataModelCell extends BasicDataModelCell implements Comparable {

    public SortableDataModelCell(SortableDataModelRow row, Object userValue, int index) {
        super(userValue, row, index);
    }

    public int compareTo(@NotNull Object o) {
        DataModelCell cell = (DataModelCell) o;
        Comparable local = (Comparable) getUserValue();
        Comparable remote = (Comparable) cell.getUserValue();

        if (local == null && remote == null) return 0;
        if (local == null) return -1;
        if (remote == null) return 1;
        // local class may differ from remote class for
        // columns with data conversion error
        if (local.getClass().equals(remote.getClass())) {
            return local.compareTo(remote);
        } else {
            Class typeClass = cell.getColumnInfo().getDataType().getTypeClass();
            return local.getClass().equals(typeClass) ? 1 :
                   remote.getClass().equals(typeClass) ? -1 : 0;
        }
    }

}
