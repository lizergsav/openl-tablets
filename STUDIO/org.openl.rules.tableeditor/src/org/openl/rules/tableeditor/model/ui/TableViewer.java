package org.openl.rules.tableeditor.model.ui;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.binding.impl.MethodUsagesSearcher.MethodUsage;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.Log;

public class TableViewer {

    private IGrid grid;

    private IGridRegion reg;

    private String linkBase;

    private String linkTarget;

    private void setStyle(ICell cell, CellModel cm) {
        ICellStyle style = cell.getStyle();

        if (style == null) {
            return;
        }

        switch (style.getHorizontalAlignment()) {
            case ICellStyle.ALIGN_LEFT:
                // Left by default    
                break;
            case ICellStyle.ALIGN_RIGHT:
                cm.setHalign("right");
                break;
            case ICellStyle.ALIGN_CENTER:
                cm.setHalign("center");
                break;
            case ICellStyle.ALIGN_JUSTIFY:
                cm.setHalign("justify");
                break;
            default:
                // Align right numeric and date
                if (cell.getNativeType() == IGrid.CELL_TYPE_NUMERIC) {
                    cm.setHalign("right");
                }
                break;
        }

        switch (style.getVerticalAlignment()) {
            case ICellStyle.VERTICAL_BOTTOM:
                // Left by default
                break;
            case ICellStyle.VERTICAL_CENTER:
                cm.setValign("center");
                break;
            case ICellStyle.VERTICAL_TOP:
                cm.setValign("top");
                break;
        }

        if (style.getIdent() > 0) {
            cm.setIdent(style.getIdent());
        }

        short[] rgb = style.getFillForegroundColor();
        cm.setRgbBackground(rgb);

        cm.setFont(cell.getFont());
    }

    /**
     * Default constructor
     */
    public TableViewer() {

    }

    /**
     * Two argument constructor
     */
    public TableViewer(IGrid grid, IGridRegion reg, String linkBase, String linkTarget) {
        super();
        this.grid = grid;
        this.reg = reg;
        this.linkBase = linkBase;
        this.linkTarget = linkTarget;
    }

    CellModel buildCell(ICell cell, CellModel cm) {
        cm.setColspan(getColSpan(cell));
        cm.setRowspan(getRowSpan(cell));

        if (cm.getRow() == 0) {
            cm.setWidth(getWidth(cell));
        }

        String formattedValue = cell.getFormattedValue();
        if (StringUtils.isNotBlank(formattedValue)) {
            String content;
            // has Explanation link
            //
            if (link(formattedValue)) {
                content = formattedValue;
                // has method call
                //
            } else if (CellMetaInfo.isCellContainsMethodUsages(cell)) {
                content = createFormulaCellWithLinks(cell, formattedValue);
                // has image
            } else if (image(formattedValue)) {
                content = formattedValue;
            } else {            
                content = StringEscapeUtils.escapeHtml4(formattedValue);
            }
            cm.setContent(content);
            if (cell.getFormula() != null) {
                cm.setFormula(cell.getFormula());
            }
        }

        ICellComment cellComment = cell.getComment();
        cm.setComment(cellComment != null ? cellComment.getText() : null);

        setStyle(cell, cm);
        return cm;
    }

    private boolean image(String formattedValue) {
        return formattedValue.replaceAll("\n", "").matches(".*<i .*>.*</i>.*");
    }

    private boolean link(String formattedValue) {
        return formattedValue.matches(".*<a href.*</a>.*");
    }

    private String createFormulaCellWithLinks(ICell cell, String formattedValue) {
        int nextSymbolIndex = 0;
        StringBuilder buff = new StringBuilder();
        if (isShowLinks()) {
            for (MethodUsage methodUsage : cell.getMetaInfo().getUsedMethods()) {
                int pstart = methodUsage.getStart();
                int pend = methodUsage.getEnd();
                String tableUri = methodUsage.getTableUri();
                // add link to used table with signature in tooltip
                buff.append(formattedValue.substring(nextSymbolIndex, pstart)).append("<span class=\"title\">");
                if (tableUri != null) {
                    String tableId = TableUtils.makeTableId(tableUri);
                    buff.append("<a href=\"").append(linkBase).append("?id=")
                        .append(tableId).append("\"");
                    if (StringUtils.isNotBlank(linkTarget)) {
                        buff.append(" target=\"").append(linkTarget).append("\"");
                    }
                    buff.append(">")
                        .append(formattedValue.substring(pstart, pend + 1))
                        .append("</a>");
                } else {
                    buff.append(formattedValue.substring(pstart, pend + 1));
                }
                buff.append("<em>").append(methodUsage.getMethodSignature()).append("</em></span>");
                nextSymbolIndex = pend + 1;
            }
        }
        buff.append(formattedValue.substring(nextSymbolIndex));
        return buff.toString();
    }

    private boolean isShowLinks() {
        return linkBase != null;
    }

    public TableModel buildModel(IGridTable gt) {
        return buildModel(gt, -1);
    }

    public TableModel buildModel(IGridTable gt, int numRows) {

        // IGridTable table = new GridTable(g.getTop(), g.getLeft(),
        // g.getBottom(),
        // g.getRight(), t.getGrid());

        int h = IGridRegion.Tool.height(reg);
        int w = IGridRegion.Tool.width(reg);

        TableModel tm = new TableModel(w, h, gt);
        tm.setNumRowsToDisplay(numRows);

        for (int row = reg.getTop(); row <= reg.getBottom(); row++) {
            for (int column = reg.getLeft(); column <= reg.getRight(); column++) {
                int c = column - reg.getLeft();
                int r = row - reg.getTop();
                if (tm.hasCell(r, c)) {
                    continue;
                }
                ICell cell = grid.getCell(column, row);

                CellModel cm = buildCell(cell, new CellModel(r, c));

                tm.addCell(cm, r, c);
                if (cm.getColspan() > 1 || cm.getRowspan() > 1) {
                    CellModelDelegator cmd = new CellModelDelegator(cm);
                    for (int i = 0; i < cm.getRowspan(); i++) {
                        for (int j = 0; j < cm.getColspan(); j++) {
                            if (i == 0 && j == 0) {
                                continue;
                            }
                            tm.addCell(cmd, r + i, c + j);
                        }
                    }
                }

            }

        }

        setGrid(tm);
        return tm;
    }

    BorderStyle getBorderStyle(ICellStyle cs, int side) {

        int xlsStyle;
        short[] rgb;

        short[] bss = cs.getBorderStyle();
        xlsStyle = bss == null ? ICellStyle.BORDER_NONE : bss[side];

        short[][] rgbb = cs.getBorderRGB();
        rgb = rgbb == null ? new short[] { 0, 0, 0 } : rgbb[side];

        BorderStyle bs = new BorderStyle();
        bs.setRgb(rgb);
        switch (xlsStyle) {
            case ICellStyle.BORDER_NONE:
                return BorderStyle.NONE;
            case ICellStyle.BORDER_DASH_DOT_DOT:
            case ICellStyle.BORDER_DASH_DOT:
            case ICellStyle.BORDER_DASHED:
                bs.setWidth(1);
                bs.setStyle("dashed");
                break;

            case ICellStyle.BORDER_DOTTED:
                bs.setWidth(1);
                bs.setStyle("dotted");
                break;
            case ICellStyle.BORDER_DOUBLE:
                bs.setWidth(1);
                bs.setStyle("double");
                break;
            case ICellStyle.BORDER_THIN:
                bs.setWidth(1);
                bs.setStyle("solid");
                break;
            case ICellStyle.BORDER_THICK:
                bs.setWidth(2);
                bs.setStyle("solid");
                break;
            case ICellStyle.BORDER_HAIR:
                bs.setWidth(1);
                bs.setStyle("dotted");
                break;
            case ICellStyle.BORDER_MEDIUM:
                bs.setWidth(2);
                bs.setStyle("solid");
                break;
            case ICellStyle.BORDER_MEDIUM_DASH_DOT:
            case ICellStyle.BORDER_MEDIUM_DASH_DOT_DOT:
            case ICellStyle.BORDER_MEDIUM_DASHED:
                bs.setWidth(2);
                bs.setStyle("dashed");
                break;
            default:
                Log.warn("Unknown border style: " + xlsStyle);
                bs.setWidth(1);
                bs.setStyle("solid");
        }
        return bs;
    }

    int getColSpan(ICell cell) {
        IGridRegion gr = cell.getRegion();
        if (gr == null) {
            return 1;
        }
        return IGridRegion.Tool.width(IGridRegion.Tool.intersect(reg, gr));
    }

    int getRowSpan(ICell cell) {
        IGridRegion gr = cell.getRegion();
        if (gr == null) {
            return 1;
        }
        return IGridRegion.Tool.height(IGridRegion.Tool.intersect(reg, gr));
    }

    public int getWidth(ICell cell) {
        IGridRegion gr;
        if ((gr = cell.getRegion()) == null) {
            return grid.getColumnWidth(cell.getColumn());
        }
        int w = 0;

        gr = IGridRegion.Tool.intersect(gr, reg);
        for (int c = gr.getLeft(); c <= gr.getRight(); c++) {
            w += grid.getColumnWidth(c);
        }

        return w;
    }

    short[] rgb(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null && bs2 == null) {
            return new short[] { 0, 0, 0 };
        }

        return bs1 == null ? bs2.getRgb()
                : (bs2 == null ? bs1.getRgb()
                        : (bs1 == BorderStyle.NONE ? bs2.getRgb() : bs1.getRgb()));
    }

    void setGrid(TableModel tm) {
        int width = IGridRegion.Tool.width(reg);

        for (int i = 0; i <= width; i++) {
            setVerticalBorder(i, tm);
        }

        int height = IGridRegion.Tool.height(reg);

        for (int i = 0; i <= height; i++) {
            setHorizontalBorder(i, tm);
        }

    }

    void setHorizontalBorder(int row, TableModel tm) {
        int width = IGridRegion.Tool.width(reg);
        int left = reg.getLeft();
        int top = reg.getTop();

        for (int i = 0; i < width; i++) {
            ICellStyle ts = row + top - 1 < 0 ? null : grid.getCell(i + left, row + top - 1).getStyle();
            ICellStyle bs = grid.getCell(i + left, row + top).getStyle();

            CellModel cmTop = ts == null ? null : tm.findCellModel(i, row - 1, ICellStyle.BOTTOM);
            CellModel cmBottom = bs == null ? null : tm.findCellModel(i, row, ICellStyle.TOP);

            if (cmTop == null && cmBottom == null) {
                continue;
            }

            BorderStyle tStyle = ts != null ? getBorderStyle(ts, ICellStyle.BOTTOM) : null;
            BorderStyle bStyle = bs != null ? getBorderStyle(bs, ICellStyle.TOP) : null;

            int W = width(tStyle, bStyle);
            String style = style(tStyle, bStyle);
            short[] rgb = rgb(tStyle, bStyle);

            BorderStyle bstyle = new BorderStyle(W, style, rgb);

            switch (W) {
                case 0:
                    break;
                case 1:
                    if (cmTop == null) {
                        cmBottom.setBorderStyle(bstyle, ICellStyle.TOP);

                    } else {
                        cmTop.setBorderStyle(bstyle, ICellStyle.BOTTOM);
                    }
                    break;
                case 2:
                    if (cmTop == null) {
                        cmBottom.setBorderStyle(bstyle, ICellStyle.TOP);
                    } else if (cmBottom == null) {
                        cmTop.setBorderStyle(bstyle, ICellStyle.BOTTOM);
                    } else {
                        bstyle.setWidth(1);
                        cmBottom.setBorderStyle(bstyle, ICellStyle.TOP);
                        cmTop.setBorderStyle(bstyle, ICellStyle.BOTTOM);
                    }

            }
        }

    }

    void setVerticalBorder(int column, TableModel tm) {
        int height = IGridRegion.Tool.height(reg);
        int left = reg.getLeft();
        int top = reg.getTop();

        for (int i = 0; i < height; i++) {
            ICellStyle ls = column + left - 1 < 0 ? null : grid.getCell(column + left - 1, i + top).getStyle();
            ICellStyle rs = grid.getCell(column + left, i + top).getStyle();

            CellModel cmLeft = ls == null ? null : tm.findCellModel(column - 1, i, ICellStyle.RIGHT);
            CellModel cmRight = rs == null ? null : tm.findCellModel(column, i, ICellStyle.LEFT);

            if (cmLeft == null && cmRight == null) {
                continue;
            }

            BorderStyle lStyle = ls != null ? getBorderStyle(ls, ICellStyle.RIGHT) : null;
            BorderStyle rStyle = rs != null ? getBorderStyle(rs, ICellStyle.LEFT) : null;

            int W = width(lStyle, rStyle);
            String style = style(lStyle, rStyle);
            short[] rgb = rgb(lStyle, rStyle);

            BorderStyle bstyle = new BorderStyle(W, style, rgb);

            switch (W) {
                case 0:
                    break;
                case 1:
                    if (cmLeft == null) {
                        cmRight.setBorderStyle(bstyle, ICellStyle.LEFT);

                    } else {
                        cmLeft.setBorderStyle(bstyle, ICellStyle.RIGHT);
                    }
                    break;
                case 2:
                    if (cmLeft == null) {
                        cmRight.setBorderStyle(bstyle, ICellStyle.LEFT);
                    } else if (cmRight == null) {
                        cmLeft.setBorderStyle(bstyle, ICellStyle.RIGHT);
                    } else {
                        bstyle.setWidth(1);
                        cmRight.setBorderStyle(bstyle, ICellStyle.LEFT);
                        cmLeft.setBorderStyle(bstyle, ICellStyle.RIGHT);
                    }

            }
        }

    }

    String style(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null && bs2 == null) {
            return "none";
        }

        return bs1 == null ? bs2.getStyle()
                : (bs2 == null ? bs1.getStyle()
                        : (bs1 == BorderStyle.NONE ? bs2.getStyle() : bs1.getStyle()));
    }

    int width(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null && bs2 == null) {
            return 0;
        }

        return bs1 == null ? bs2.getWidth() : (bs2 == null ? bs1.getWidth() : Math.max(bs1.getWidth(), bs2.getWidth()));
    }

}
