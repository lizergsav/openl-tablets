package org.openl.rules.tableeditor.taglib;

import javax.faces.component.UIComponent;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorTag extends TableViewerTag {

    private String mode = null;
    private String editable = null;

    @Override
    public String getComponentType() {
        return Constants.TABLE_EDITOR_TYPE;
    }

    public String getEditable() {
        return editable;
    }

    public void setEditable(String editable) {
        this.editable = editable;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_EDITOR_TYPE;
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        mode = null;
        editable = null;
    }

    @Override
    public void setProperties(UIComponent component) {
        // always call the superclass method
        super.setProperties(component);
        setObject(component, Constants.ATTRIBUTE_MODE, mode);
        setBoolean(component, Constants.ATTRIBUTE_EDITABLE, editable);
    }
}