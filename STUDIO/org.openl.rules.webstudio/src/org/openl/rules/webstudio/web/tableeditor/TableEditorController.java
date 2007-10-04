package org.openl.rules.webstudio.web.tableeditor;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;
import org.openl.jsf.Util;
import static org.openl.jsf.Util.getRequestParameter;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.ui.EditorHelper;
import org.openl.rules.ui.TableEditorModel;
import org.openl.rules.ui.TableModel;
import org.openl.rules.ui.TableViewer;

import java.util.Map;
import java.io.IOException;

/**
 * Table editor controller. It should be a managed bean with <b>request</b> scope.
 *
 * @author Andrey Naumenko
 */
public class TableEditorController {
    private String response;
    private int row, col, elementID;

    public static final String OUTCOME_SUCCESS = "tableEditor_success";

     public String load() throws Exception {
         int elementId = Integer.parseInt(getRequestParameter("elementID"));
         TableModel tableModel = initializeTableModel(elementId);

         response = TableRenderer.render(tableModel);

         return OUTCOME_SUCCESS;
     }

    /**
     * Handles request saving new cell value.
     *
     * @return {@link #OUTCOME_SUCCESS} jsf navigation case outcome
     */
    public String save() {
        readRequestParams();
        String value = Util.getRequestParameter("value");

        EditorHelper editorHelper = getHelper(elementID);
        if (editorHelper != null) {
            editorHelper.getModel().setCellValue(col, row, value);
            response = pojo2json(new TableModificationResponse(null));
        }
        return OUTCOME_SUCCESS;
    }

    /**
     * Generates JSON response for cell type: editor type and editor specific setup javascript object.
     *
     * @return {@link #OUTCOME_SUCCESS} jsf navigation case outcome
     */
    public String getCellType() {
        readRequestParams();

        response = "";
        EditorHelper editorHelper = getHelper(elementID);
        if (editorHelper != null) {
            EditorTypeResponse typeResponse = new EditorTypeResponse("text");
            TableEditorModel editorModel = editorHelper.getModel();

            if (editorModel.updatedTableCellInsideTableRegion(row, col)) {

                TableEditorModel.CellType type = editorModel.getCellType(row, col);
                if (type == TableEditorModel.CellType.CA_ENUMERATION_CELL_TYPE) {
                    String[] metadata = (String[]) editorModel.getCellEditorMetadata(row, col);
                    typeResponse.setEditor("combo");
                    typeResponse.setParams(metadata);
                }

                if (col == 3 && row == 1) {
                    typeResponse = new EditorTypeResponse("multilineText");
                }
                if (col == 2 && row == 1) {
                    typeResponse = new EditorTypeResponse("date");
                }

                if (col == 1 && row == 1) {
                    typeResponse = new EditorTypeResponse("numeric");
                    typeResponse.setParams(new RangeParam(-1000L, 1000L));
                }

                if (col == 0 && row == 1) {
                    typeResponse = new EditorTypeResponse("price");
                }

                response = pojo2json(typeResponse);
            }
        }
        return OUTCOME_SUCCESS;
    }

    public String undo() throws Exception {
        readRequestParams();
        EditorHelper editorHelper = getHelper(elementID);
        if (editorHelper != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null);
            if (editorHelper.getModel().hasUndo()) {
                editorHelper.getModel().undo();
                load();
                tmResponse.setResponse(response);
            } else {
                tmResponse.setStatus("No actions to undo");
            }
            response = pojo2json(tmResponse);
        }
        return OUTCOME_SUCCESS; 
    }

    public String redo() throws Exception {
        readRequestParams();
        EditorHelper editorHelper = getHelper(elementID);
        if (editorHelper != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null);
            if (editorHelper.getModel().hasRedo()) {
                editorHelper.getModel().redo();
                load();
                tmResponse.setResponse(response);
            } else {
                tmResponse.setStatus("No actions to redo");
            }
            response = pojo2json(tmResponse);
        }
        return OUTCOME_SUCCESS;
    }

    public String getResponse() {
        return response;
    }

   public String addRowColBefore() throws Exception {
       readRequestParams();

       EditorHelper editorHelper = getHelper(elementID);
       if (editorHelper != null) {
           TableEditorModel editorModel = editorHelper.getModel();

           TableModificationResponse tmResponse = new TableModificationResponse(null);
           try {
               if (row >= 0)
                   if (editorModel.canAddRows(1)) editorModel.insertRows(1, row); else tmResponse.setStatus("Can not add row");
               else
                   if (editorModel.canAddCols(1)) editorModel.insertColumns(1, col); else tmResponse.setStatus("Can not add column");
           } catch (Exception e) {
               tmResponse.setStatus("Internal server error");
           }

           load();
           tmResponse.setResponse(response);
           response = pojo2json(tmResponse);
       }
       return OUTCOME_SUCCESS;
   }

    public String removeRowCol() throws Exception {
        readRequestParams();
        EditorHelper editorHelper = getHelper(elementID);
        if (editorHelper != null) {
            TableEditorModel editorModel = editorHelper.getModel();
            boolean move = Boolean.valueOf(Util.getRequestParameter("move"));

            if (row >= 0) {
                if (move) ;
                else editorModel.removeRows(1, row);
            } else {
                if (move) ;
                else editorModel.removeColumns(1, col);
            }
            load();
            response = pojo2json(new TableModificationResponse(response));
        }
        return OUTCOME_SUCCESS;
    }

    public String saveTable() throws IOException {
        readRequestParams();
        EditorHelper editorHelper = getHelper(elementID);
        if (editorHelper != null) {
            editorHelper.getModel().save();
            response = pojo2json(new TableModificationResponse(""));
        }
        return OUTCOME_SUCCESS;
    }

   private void readRequestParams() {
       Map<String, String> paramMap = Util.getRequestParameterMap();
       row = col = elementID = -1;

       try {row = Integer.parseInt(paramMap.get("row")) - 1;} catch (NumberFormatException e) {}
       try {col = Integer.parseInt(paramMap.get("col")) - 1;} catch (NumberFormatException e) {}
       try {elementID = Integer.parseInt(paramMap.get("elementID"));} catch (NumberFormatException e) {}
   }

   private TableModel initializeTableModel(int elementID) {
          IGridTable gt = getGridTable(elementID);
          if (gt == null) return null;

          IGrid htmlGrid = gt.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 1;
            IGridFilter[] f1 = new IGridFilter[N];
            f1[0] = new SimpleXlsFormatter();
//            f1[1] = new SimpleHtmlFilter();
            htmlGrid = new FilteredGrid(gt.getGrid(), f1);
        }

        TableViewer tv = new TableViewer(htmlGrid, gt.getRegion());
        return tv.buildModel();
    }

    private IGridTable getGridTable(int elementID) {
      return getHelper(elementID).getModel().getUpdatedTable();
   }

    /**
     * Returns <code>EditorHelper</code> instance from http session or creates new one if not present there. Checks
     * that <code>elementId</code> matches id in this helper. If it does not the method prepares response which notifies
     * a client of the mismatch and returns <code>null</code>. In the latter case calling method may just do nothing as
     * corresponding response is already prepared.
     * @param elementId table id
     * @return <code>EditorHelper</code> instance or <code>null</code> if <code>elementId</code> does not match element
     * id in an existing helper.
     */
    private EditorHelper getHelper(int elementId) {
       Map sessionMap = Util.getSessionMap();
       synchronized (sessionMap) {
           if (sessionMap.containsKey("editorHelper")) {
               EditorHelper editorHelper = (EditorHelper) sessionMap.get("editorHelper");
               if (editorHelper.getElementID() != elementId) {
                   response = pojo2json(new TableModificationResponse(null,
                           "You started editing another table, this table changes are lost"));
                   return null;
               }
               return editorHelper;
           }
           EditorHelper editorHelper = new EditorHelper();
           editorHelper.setTableID(elementId, Util.getWebStudio().getModel());
           sessionMap.put("editorHelper", editorHelper);
           return editorHelper;
       }
   }

   private static String pojo2json(Object pojo) {
       try {
           return new StringBuilder().append("(").append(JSONMapper.toJSON(pojo).render(true)).append(")")
                   .toString();
       } catch (MapperException e) {
           return null;
       }
   }

    public static class EditorTypeResponse {
        private String editor;
        private Object params;

        public EditorTypeResponse(String editor) {
            this.editor = editor;
        }

        public String getEditor() {
            return editor;
        }

        public void setEditor(String editor) {
            this.editor = editor;
        }

        public Object getParams() {
            return params;
        }

        public void setParams(Object params) {
            this.params = params;
        }
    }

    public static class TableModificationResponse {
        private String response;
        private String status;

        public TableModificationResponse(String response) {
            this.response = response;
        }

        public TableModificationResponse(String response, String status) {
            this.response = response;
            this.status = status;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class RangeParam {
        private Long min, max;

        public RangeParam(Long min, Long max) {
            this.min = min;
            this.max = max;
        }

        public Long getMax() {
            return max;
        }

        public void setMax(Long max) {
            this.max = max;
        }

        public Long getMin() {
            return min;
        }

        public void setMin(Long min) {
            this.min = min;
        }
    }

}
