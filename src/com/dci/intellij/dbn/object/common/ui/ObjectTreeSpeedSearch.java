package com.dci.intellij.dbn.object.common.ui;

import com.dci.intellij.dbn.object.common.DBObject;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class ObjectTreeSpeedSearch extends SpeedSearchBase {

    public ObjectTreeSpeedSearch(ObjectTree objectTree) {
        super(objectTree);
    }

    @Override
    public ObjectTree getComponent() {
        return (ObjectTree) super.getComponent();
    }

    @Override
    protected int getSelectedIndex() {
        TreePath selectionPath = getComponent().getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            for (int i=0; i<getAllElements().length; i++) {
                if (getAllElements()[i] == node) {
                    return i;
                }
            }
        }
        return 0;
    }

    @Override
    protected Object[] getAllElements() {
        return getComponent().getModel().getAllElements();
    }

    @Override
    protected String getElementText(Object obj) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
        if (node.getUserObject() instanceof DBObject) {
            DBObject object = (DBObject) node.getUserObject();
            return object.getName();
        }
        return node.getUserObject().toString();
    }

    @Override
    protected void selectElement(Object obj, String s) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
        TreeUtil.selectPath(getComponent(), new TreePath(node.getPath()), true);
    }
}
