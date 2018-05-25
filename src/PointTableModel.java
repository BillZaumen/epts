import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.CPointType;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;
import org.bzdev.util.TemplateProcessor.KeyMapList;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.event.*;
import javax.swing.table.*;

public class PointTableModel implements TableModel {

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    Vector<TableModelListener> listeners = new Vector<>();
    ArrayList<PointTMR> rows = new ArrayList<>();
    TreeSet<String> names =new TreeSet<>();
    TreeSet<String> pnames =new TreeSet<>();
    
    public int pathVariableNameCount() {return pnames.size();}


    public List<PointTMR> getRows() {
	return Collections.unmodifiableList(rows);
    }

    public Set<String> getVariableNames() {
	return Collections.unmodifiableSet(names);
    }

    public Set<String> getPathVariableNames() {
	return Collections.unmodifiableSet(pnames);
    }

    public int findStart(String varname) {
	varname = varname.trim();
	if (names.contains(varname)) {
	    int n = rows.size();
	    for (int i = 0; i < n; i++) {
		PointTMR row = rows.get(i);
		Enum rmode = row.getMode();
		if ((rmode == EPTS.Mode.PATH_START
		     || rmode == EPTS.Mode.LOCATION)
		    && row.getVariableName().equals(varname)) {
		    return i;
		}
	    }
	}
	return -1;
    }

    public int findStart(int index) {
	while (index > 0) {
	    Enum mode = getRowMode(index);
	    if (mode == EPTS.Mode.LOCATION || mode == EPTS.Mode.PATH_START) {
		break;
	    }
	    index--;
	}
	return index;
    }

    public int findEnd(int start) {
	int n = rows.size();
	if (start == -1 ||  start >= n) return -1;
	for (int i = start; i < n; i++) {
	    if (getRowMode(i) == EPTS.Mode.PATH_END) {
		return i;
	    }
	}
	return -1;
    }


    public PointTMR getLastRow()  {
	int n = rows.size();
	n--;
	if (n < 0) return null;
	return rows.get(n);
    }

    public void setLastRowMode(Enum mode) {
	int n = rows.size();
	n--;
	if (n < 0) return;
	PointTMR row = rows.get(n);
	Enum rmode = row.getMode();
	String varname = row.getVariableName().trim();
	if ((rmode == EPTS.Mode.PATH_START
	     || rmode == EPTS.Mode.LOCATION)
	    && (varname != null || varname.length() != 0)) {
	    if (names.contains(varname)) {
		throw new IllegalStateException
		    (String.format(localeString("NameInUse"), varname));
	    }
	    names.add(varname);
	    if (rmode == EPTS.Mode.PATH_START) {
		pnames.add(varname);
	    }
		
	}
	row.setMode(mode);
	fireTableChanged(n, n, Mode.MODIFIED);
    }


    public PointTMR getRow(int i) throws IndexOutOfBoundsException {
	return rows.get(i);
    }

    public Enum getRowMode(int i) {
	if (i < 0 || i >= rows.size()) return null;
	return rows.get(i).getMode();
    }

    public String getVariableName(int index) {
	if (index < 0 || index >= rows.size()) return null;
	return rows.get(index).getVariableName();
    }

    JComponent pane;		// for repaint();

    public PointTableModel(JComponent pane) {
	this.pane = pane;
    }


    @Override
    public void addTableModelListener(TableModelListener l) {
	listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
	listeners.remove(l);
    }

    public void addRow(PointTMR row) {
	Enum mode = row.getMode();
	String varname = row.getVariableName();
	if ((mode == EPTS.Mode.PATH_START || mode == EPTS.Mode.LOCATION)
	     && varname != null && varname.length() != 0) {
	    if (names.contains(varname)) {
		throw new IllegalStateException
		    (String.format(localeString("NameInUse"), varname));
	    }
	    names.add(varname);
	    if (mode == EPTS.Mode.PATH_START) {
		pnames.add(varname);
	    }
	}
	rows.add(row);
    }

    public void insertRow(int index, String varname,
			  Enum mode, double x, double y, double xp, double yp)
	throws IllegalStateException
    {
	if ((mode == EPTS.Mode.PATH_START || mode == EPTS.Mode.LOCATION)
	    && varname != null) {
	    varname = varname.trim();
	    if (names.contains(varname)) {
		throw new IllegalStateException
		    (String.format(localeString("NameInUse"), varname));
	    }
	    names.add(varname);
	    if (mode == EPTS.Mode.PATH_START) {
		pnames.add(varname);
	    }
	}
	rows.add(index, new PointTMR(varname, mode, x, y, xp, yp));
	if (!(mode instanceof EPTS.Mode)) {
	    pane.repaint();
	}
	fireTableChanged(index, index, Mode.ADDED);
    }

    public void addRow(String varname, Enum mode, double x, double y,
		       double xp, double yp)
	throws IllegalStateException
    {
	if  (mode == null
	     || !((mode instanceof EPTS.Mode)
		  || (mode instanceof SplinePathBuilder.CPointType))) {
	    throw new IllegalArgumentException("illegal mode");
	}
	int rowIndex = rows.size();
	if ((mode == EPTS.Mode.PATH_START || mode == EPTS.Mode.LOCATION)
	     && varname != null) {
	    varname = varname.trim();
	    if (names.contains(varname)) {
		throw new IllegalStateException
		    (String.format(localeString("NameInUse"), varname));
	    }
	    names.add(varname);
	    if (mode == EPTS.Mode.PATH_START) {
		pnames.add(varname);
	    }
	}
	rows.add(new PointTMR(varname, mode, x, y, xp, yp));
	if (!(mode instanceof EPTS.Mode)) {
	    pane.repaint();
	}
	fireTableChanged(rowIndex, rowIndex, Mode.ADDED);
    }

    public void changeMode(int index, Enum mode) {
	PointTMR row = rows.get(index);
	if (row == null) return;
	Enum rmode = row.getMode();
	if (rmode == mode) return; // nothing to do
	String varname = row.getVariableName();
	if ((rmode == EPTS.Mode.PATH_START || rmode == EPTS.Mode.LOCATION )
	    && varname != null && varname.length() != 0) {
	    names.remove(varname);
	    if (rmode == EPTS.Mode.PATH_START) {
		pnames.remove(varname);
	    }
	}
	row.setMode(mode);
	fireTableChanged(index, index, Mode.MODIFIED);
    }
	

    public void changeCoords(int index,
			     double x, double y, double xp, double yp,
			     boolean notify)
    {
	PointTMR row = rows.get(index);
	if (row == null) return;
	row.x = x;
	row.y = y;
	row.xp = xp;
	row.yp = yp;
	if (notify) {
	    fireTableChanged(index, index, Mode.MODIFIED);
	}
    }

    public void deleteRow(int index) throws IndexOutOfBoundsException {
	deleteRow(index, true);
    }

    public void deleteRow(int index, boolean repaint)
	throws IndexOutOfBoundsException
    {
	PointTMR row = rows.remove(index);
	Enum mode = (row == null)? null: row.getMode();
	if (mode == EPTS.Mode.PATH_START || mode == EPTS.Mode.LOCATION) {
	    String varname = row.getVariableName();
	    names.remove(varname);
	    if (mode == EPTS.Mode.PATH_START) {
		pnames.remove(varname);
	    }
	}
	if (repaint && row != null ) {
	    pane.repaint();
	}
	fireTableChanged(index, index, Mode.DELETED);
    }

    public void deleteRows(int fromIndex, int toIndex)
	throws IndexOutOfBoundsException
    {
	for (int i = fromIndex; i < toIndex; i++) {
	    PointTMR row = rows.remove(fromIndex);
	    if (row == null) continue;
	    Enum mode = row.getMode();
	    if (mode == EPTS.Mode.PATH_START || mode == EPTS.Mode.LOCATION) {
		String varname = row.getVariableName();
		names.remove(varname);
		if (mode == EPTS.Mode.PATH_START) {
		    pnames.remove(varname);
		}
	    }
	}
	fireTableChanged(fromIndex, toIndex-1, Mode.DELETED);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
	switch (columnIndex) {
	case 0:
	    return String.class;
	case 1:
	    return Enum.class;
	case 2:
	    return String.class;
	case 3:
	    return String.class;
	default:
	    return Object.class;
	}
    }

    @Override
    public int getColumnCount() {
	return 4;
    }

    private static final String BAD_COL = "Illegal column: ";

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }

    enum Mode {
	ADDED, DELETED, MODIFIED
    };

    /**
     * Notify listeners that columns 2 and 3, containing XY coordinates,
     * may have changed.
     */
    public void fireXYChanged() {
	int last = rows.size()-1;
	TableModelEvent event1 = new TableModelEvent(this, 0,last, 2,
						     TableModelEvent.UPDATE);
	TableModelEvent event2 = new TableModelEvent(this, 0, last, 3,
						     TableModelEvent.UPDATE);
	for (TableModelListener listener: listeners) {
	    listener.tableChanged(event1);
	    listener.tableChanged(event2);
	}
    }

    /**
     * Indicate that the row has changed so the table can be updated.
     * @param index the row index
     */
    public void fireRowChanged(int index) {
	if (index == -1) return;
	fireTableChanged(index, index, Mode.MODIFIED);
    }

    /**
     * Notify listeners.
     * @param start the starting index (inclusive)
     * @param end the ending index (inclusive)
     * @param mode the mode (ADDED, DELETED, or MODIFIED).
     */
    protected void fireTableChanged(int start, int end, Mode mode) {
	TableModelEvent event;
	switch(mode) {
	case ADDED:
	    event = new TableModelEvent(this, start, end,
					TableModelEvent.ALL_COLUMNS,
					TableModelEvent.INSERT);
	    break;
	case DELETED:
	    event = new TableModelEvent(this, start, end,
					TableModelEvent.ALL_COLUMNS,
					TableModelEvent.DELETE);
	    break;
	case MODIFIED:
	    event = new TableModelEvent(this, start, end);
	    break;
	default:
	    throw new UnexpectedExceptionError();
	}
	for (TableModelListener listener: listeners) {
	    listener.tableChanged(event);
	}
    }

    @Override
    public String getColumnName(int columnIndex) {
	switch(columnIndex) {
	case 0:
	    return localeString("Variable");
	case 1:
	    return localeString("Mode");
	case 2:
	    return localeString("XGCS");
	case 3:
	    return localeString("YGCS");
	default:
	    throw new IndexOutOfBoundsException(BAD_COL + columnIndex);
	}
    }

    @Override
    public int getRowCount() {
	return rows.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
	throws IndexOutOfBoundsException
    {
	PointTMR row = rows.get(rowIndex);
	Enum mode = row.getMode();
	switch (columnIndex) {
	case 0:
	    return row.getVariableName();
	case 1:
	    return row.getMode();
	case 2:
	    if (mode == EPTS.Mode.PATH_START || mode == EPTS.Mode.PATH_END
		|| mode == SplinePathBuilder.CPointType.CLOSE) {
		return "";
	    }
	    return String.format("%s", row.getX());
	case 3:
	    if (mode == EPTS.Mode.PATH_START || mode == EPTS.Mode.PATH_END
		|| mode == SplinePathBuilder.CPointType.CLOSE) {
		return "";
	    }
	    return String.format("%s", row.getY());
	default:
	    throw new IndexOutOfBoundsException(BAD_COL + columnIndex);
	}
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
	throw new IllegalStateException("not editable");
    }

    public int findRowXPYP(double xp, double yp, double zoom) {
	int i = 0;
	double limit = 7.0/zoom;
	limit *= limit;
	for (PointTMR row: rows) {
	    double d = Point2D.distanceSq(xp, yp, row.getXP(), row.getYP());
	    if (d < limit) {
		return i;
	    }
	    i++;
	}
	return -1;
    }

    /**
     * Get the keymap for this table-row-model.
     * This keymap is used by templates ({@link TemplateProcessor})
     * that are set up as follows:
     * The top level of the template contains a single key,
     * "items", whose value is a list. Each element of this list
     * contains the keys
     * <UL>
     *   <LI> "varname". This provides a variable name.
     *   <LI> "location" or "pathStatement" but not both.
     * </UL>
     * The value for "location" is a keymap
     * contain the following keys:
     * <UL>
     *    <LI> "x". This provides an X coordinate.
     *    <LI> "y". This provides a Y coordinate.
     * </UL>
     * The value for "pathStatement" is a keymap containing the key
     * "pathItem".  The value for "pathitem" is a list of keymaps,
     * each of which contain the following keys:
     * <UL>
     *   <LI> "type". This provides the type of a point along the path.
     *   <LI> "ltype". This provides an alternative name for the type of a
     *         point along the path. The alternate uses the names appropriate
     *         for configuring an animation-layer factory.
     *   <LI> "atype". This provides a user-supplied alternative name for
     *         the type of a point along the path. The alternate is
     *         specified by a user-provided map.
     *   <LI> "xy" The value is either empty or a keymap containing the
     *        keys
     *        <UL>
     *           <LI> $(x). This provides an GCS X coordinate.
     *           <LI> $(y). This provides a GCS Y coordinate.
     *           <LI> $(xp). This provides an image-space X coordinate.
     *           <LI> $(yp). This provides an image-space  Y coordinate
     *                measured from the lower-left corner of the image.
     *           <LI> $(ypj). This provides an image-space Y coordinate
     *                measured from the upper-left corner of the image
     *                (the normal Java convention).
     *        </UL>
     *   <LI> "optcomma". This is either empty of the string "," and
     *        is used as list-element separator. In a template, $(optcomma)
     *        should appear as the last token in a pathItem block
     * </UL>
     * <P>
     * <blockquote><code><pre>
     * $(items:endItems)
     *    [can use $(varname)]
     *     $(location:endLocation)
     *         [can use $(varname) $(x), $(y), $(xp), $(yp), $(ypj]
     *     $(endLocation)
     *     $(pathStatement)
     *          [can use $(varname)]
     *          $(pathItem:endPathItem)
     *              [can use $(varname), $(type), $(ltype) $(atype), and
     *               $(optcomma)]
     *              $(xy:endXY)
     *                [can use $varname, $(type), $(ltype), $(atype),
     *                 $(optcomma), $(x), and $(y), $(xp), $(yp) and $(ypj)]
     *              $(endXY)
     *          $(endPathItem)
     *     $(endPathStatement)
     * $(endItems)
     * </pre></code></blockquote>
     * <P>
     * For example,
     * $(items:endItems)
     *     $(location:endLocation)
     *         $(varname) = {x: $(x), y: $(y)};
     *     $(endLocation)
     *     $(pathStatement)
     *          $(varname) = [
     *              $(pathItem:endPathItem)
     *                  {type: $(type)$(xy:endXY),
     *                   x: $(x), y: $(y)$(endXY)}$(optcomma)
     *          $(endPathItem)
     *     $(endPathStatement)
     * $(endItems)
     */
    public TemplateProcessor.KeyMap getKeyMap(double height) {
	return getKeyMap(null, height);
    }

    public TemplateProcessor.KeyMap getKeyMap(Map<String,String> tmap,
					      double height) {

	TemplateProcessor.KeyMapList list = new TemplateProcessor.KeyMapList();
	TemplateProcessor.KeyMap kmap1 = null;
	TemplateProcessor.KeyMap kmap2 = null;
	TemplateProcessor.KeyMap kmap3 = null;
	TemplateProcessor.KeyMapList plist = null;
	int n = rows.size();
	int index = 0;
	int vindex = 0;
	int pindex = 0;
	for (PointTMR row: rows) {
	    index++;
	    Enum mode = row.getMode();
	    if (mode == EPTS.Mode.LOCATION) {
		vindex++;
		kmap1 = new TemplateProcessor.KeyMap();
		kmap2 = new TemplateProcessor.KeyMap();
		kmap1.put("varname", row.getVariableName());
		kmap1.put("vindex", ("" + vindex));
		kmap1.put("index", ("" + index));
		kmap1.put("location", kmap2);
		kmap2.put("x", String.format((Locale)null, "%s", row.getX()));
		kmap2.put("y", String.format((Locale)null, "%s", row.getY()));
		kmap2.put("xp", String.format((Locale)null, "%s", row.getXP()));
		kmap2.put("yp", String.format((Locale) null, "%s", row.getYP()));
		kmap2.put("ypr", String.format((Locale)null, "%s",
						height - row.getYP()));
		list.add(kmap1);
	    } else if (mode == EPTS.Mode.PATH_START) {
		vindex++;
		kmap1 = new TemplateProcessor.KeyMap();
		kmap2 = new TemplateProcessor.KeyMap();
		kmap1.put("varname", row.getVariableName());
		kmap1.put("index", ("" + index));
		kmap1.put("vindex", ("" + vindex));
		kmap1.put("pathStatement", kmap2);
		list.add(kmap1);
		plist = new TemplateProcessor.KeyMapList();
		pindex = 0;
		kmap2.put("pathItem", plist);
		kmap3 = null;
	    } else if (mode instanceof SplinePathBuilder.CPointType) {
		if (index == n) {
		    if (mode == SplinePathBuilder.CPointType.CONTROL
			|| mode == SplinePathBuilder.CPointType.SPLINE) {
			// terminate a partial path
			mode = SplinePathBuilder.CPointType.SEG_END;
		    }
		}
		if (kmap3 != null) kmap3.put("optcomma", ",");
		kmap3 = new TemplateProcessor.KeyMap();
		kmap3.put("index", ("" + index));
		pindex++;
		kmap3.put("pindex", ("" + pindex));
		kmap3.put("type", mode.toString());
		if (mode == SplinePathBuilder.CPointType.CONTROL) {
		    kmap3.put("ltype", "CONTROL_POINT");
		} else if (mode == SplinePathBuilder.CPointType.SPLINE) {
		    kmap3.put("ltype", "SPLINE_POINT");
		} else if (mode == SplinePathBuilder.CPointType.CLOSE) {
		    kmap3.put("ltype", "SEG_CLOSE");
		} else {
		    kmap3.put("ltype", mode.toString());
		}
		if (tmap != null) {
		    String val = tmap.get(mode.toString());
		    kmap3.put("atype", (val == null)? mode.toString(): val);
		}
		if (mode != SplinePathBuilder.CPointType.CLOSE) {
		    TemplateProcessor.KeyMap
			kmap4 = new TemplateProcessor.KeyMap();
		    kmap4.put("x",
			      String.format((Locale)null, "%s", row.getX()));
		    kmap4.put("y",
			      String.format((Locale)null, "%s", row.getY()));
		    kmap4.put("xp",
			      String.format((Locale)null, "%s", row.getXP()));
		    kmap4.put("yp",
			      String.format((Locale)null, "%s", row.getYP()));
		    kmap4.put("ypr", String.format((Locale)null, "%s",
						    height - row.getYP()));
		    kmap3.put("xy", kmap4);
		}
		plist.add(kmap3);
	    }
	}
	TemplateProcessor.KeyMap map = new TemplateProcessor.KeyMap();
	map.put("items", list);
	return map;
    }
}
