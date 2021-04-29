package org.bzdev.epts;

import org.bzdev.geom.BasicSplinePath2D;
import org.bzdev.geom.BasicSplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.CPointType;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;
import org.bzdev.util.TemplateProcessor.KeyMapList;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.event.*;
import javax.swing.table.*;

public class
 PointTableModel implements TableModel {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

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

    // get the rows for the object whose table entries contain the row
    // specified by index
    public PointTMR[] getRows(int index) {
	int start = findStart(index);
	if (start == -1) return null;
	int end = findEnd(index);
	if (end == -1) return null;
	end++;
	List<PointTMR> sublist = rows.subList(start, end);
	PointTMR[] results = new PointTMR[sublist.size()];
	for (int i = 0; i < sublist.size(); i++) {
	    results[i] = new PointTMR(sublist.get(i));
	}
	return results;
    }

    public Set<String> getVariableNames() {
	return Collections.unmodifiableSet(names);
    }

    public Set<String> getPathVariableNames() {
	return Collections.unmodifiableSet(pnames);
    }

    public List<String> getPathVariableNamesInTableOrder() {
	// order in which they
	List<String> names = new LinkedList<String>();
	for (PointTMR row: rows) {
	    if (row.getMode() == EPTS.Mode.PATH_START) {
		names.add(row.getVariableName());
	    }
	}
	return names;
    }


    public void stopFiltering() {
	for (PointTMR row: rows) {
	    row.setFilterMode(PointTMR.FilterMode.SELECTABLE);
	}
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
	    Enum mode = getRowMode(i);
	    if (mode == EPTS.Mode.PATH_END || mode == EPTS.Mode.LOCATION) {
		return i;
	    }
	}
	return -1;
    }

    public SplinePathBuilder.CPoint[]
	getCPoints(int index, boolean reversed, AffineTransform af)
    {
	int start = findStart(index);
	if (start == -1)  return null;
	int end = findEnd(start);
	if (end == -1) return null;
	start++;
	if (start >= end) return null;
	if (start+1 >= end) return null;
	SplinePathBuilder pb = new SplinePathBuilder();
	while (start < end) {
	    PointTMR row = getRow(start++);
	    Enum rmode = row.getMode();
	    double x = row.getX();
	    double y = row.getY();
	    if (rmode instanceof SplinePathBuilder.CPointType) {
		SplinePathBuilder.CPointType
		    mode = (SplinePathBuilder.CPointType) rmode;
		switch (mode) {
		case MOVE_TO:
		case SPLINE:
		case SEG_END:
		case CONTROL:
		    pb.append(new SplinePathBuilder.CPoint(mode, x, y));
		    break;
		case CLOSE:
		    pb.append(new SplinePathBuilder.CPoint(mode));
		    break;
		}
	    }
	}
	return pb.getCPoints(reversed, af);
    }

    public Path2D getPath(String pname) {
	int start = findStart(pname);
	if (start == -1)  return null;
	int end = findEnd(start);
	if (end == -1) return null;
	start++;
	if (start >= end) return null;
	if (start+1 >= end) return null;
	// System.out.format("start = %d, end = %d\n", start, end);
	SplinePathBuilder pb = new SplinePathBuilder();
	while (start < end) {
	    PointTMR row = getRow(start++);
	    Enum rmode = row.getMode();
	    double x = row.getX();
	    double y = row.getY();
	    if (rmode instanceof SplinePathBuilder.CPointType) {
		SplinePathBuilder.CPointType
		    mode = (SplinePathBuilder.CPointType) rmode;
		switch (mode) {
		case MOVE_TO:
		case SPLINE:
		case SEG_END:
		case CONTROL:
		    pb.append(new SplinePathBuilder.CPoint(mode, x, y));
		    break;
		case CLOSE:
		    pb.append(new SplinePathBuilder.CPoint(mode));
		    break;
		}
	    }
	}
	return pb.getPath();
    }

    double[] originalxy = new double[2];
    double[] modifiedxy = new double[2];
    public Path2D getPath(String pname, AffineTransform af) {
	int start = findStart(pname);
	if (start == -1)  return null;
	int end = findEnd(start);
	if (end == -1) return null;
	start++;
	if (start >= end) return null;
	if (start+1 >= end) return null;
	// System.out.format("start = %d, end = %d\n", start, end);
	SplinePathBuilder pb = new SplinePathBuilder();
	while (start < end) {
	    PointTMR row = getRow(start++);
	    Enum rmode = row.getMode();
	    if (rmode instanceof SplinePathBuilder.CPointType) {
		SplinePathBuilder.CPointType
		    mode = (SplinePathBuilder.CPointType) rmode;
		switch (mode) {
		case MOVE_TO:
		case SPLINE:
		case SEG_END:
		case CONTROL:
		    originalxy[0] = row.getX();
		    originalxy[1] = row.getY();
		    af.transform(originalxy, 0, modifiedxy, 0, 1);
		    // double x = row.getX();
		    // double y = row.getY();
		    pb.append(new SplinePathBuilder.CPoint(mode, modifiedxy[0],
							   modifiedxy[1]));
		    break;
		case CLOSE:
		    pb.append(new SplinePathBuilder.CPoint(mode));
		    break;
		}
	    }
	}
	return pb.getPath();
    }

    public PointTMR getNextToLastRow() {
	int n = rows.size();
	n -= 2;
	if (n < 0) return null;
	return rows.get(n);
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
	if (i < 0 || i >= rows.size()) return null;
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
	    throw new IllegalArgumentException(errorMsg("illegalMode", mode));
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
	
    public void moveObject(int index,
			   double x, double y, double xp, double yp,
			   boolean notify)
    {
	PointTMR row = rows.get(index);
	if (row == null) return;
	Enum mode = row.getMode();
	if (mode == EPTS.Mode.LOCATION) {
	    changeCoords(index, x, y, xp, yp, notify);
	    return;
	}
	double refx = row.x;
	double refy = row.y;
	double refxp = row.xp;
	double refyp = row.yp;
	double deltax = x - refx;
	double deltay = y - refy;
	double deltaxp = xp - refxp;
	double deltayp = yp - refyp;
	int start = findStart(index);
	int ind = start;
	for(;;) {
	    row = rows.get(++ind);
	    if (row == null) {
		break;
	    } else if (row.getMode() == EPTS.Mode.PATH_END) {
		break;
	    } else if (ind == index) {
		changeCoords(index, x, y, xp, yp, false);
	    } else {
		changeCoords(ind, row.x+deltax, row.y+deltay,
			     row.xp + deltaxp, row.yp+deltayp,
			     false);
	    }
	}
	if (notify) {
	    fireTableChanged(start+1, ind-1, Mode.MODIFIED);
	}
    }

    double[] coords = new double[4]; // tmp  work area

    public void rotateObject(PointTMR[] origRows, Point2D cm, Point2D cmp,
			     Point2D start, int pathStart,
			     double x, double y, double xp, double yp,
			     boolean notify)
    {
	double x1 = start.getX() - cm.getX();
	double y1 = start.getY() - cm.getY();
	double norm = Math.sqrt(x1*x1 + y1*y1);
	x1 /= norm;
	y1 /= norm;
	double x2 = x - cm.getX();
	double y2 = y - cm.getY();
	norm = Math.sqrt(x2*x2 + y2*y2);
	x2 /= norm;
	y2 /= norm;
	double xx = x1*x2 + y1*y2;
	double yy = x1*y2 - x2*y1;
	double angle = Math.atan2(yy, xx);

	AffineTransform af1 =
	    AffineTransform.getRotateInstance(xx, yy, cm.getX(), cm.getY());
	AffineTransform af2 =
	    AffineTransform.getRotateInstance(xx, -yy, cmp.getX(), cmp.getY());

	for (int i = 1; i <  origRows.length-1; i++) {
	    PointTMR origRow = origRows[i];
	    if (origRow.getMode() != SplinePathBuilder.CPointType.CLOSE) {
		coords[0] = origRow.getX();
		coords[1] = origRow.getY();
		af1.transform(coords, 0, coords, 2, 1);
		xx = coords[2];
		yy = coords[3];
		coords[0] = origRow.getXP();
		coords[1] = origRow.getYP();
		af2.transform(coords, 0, coords, 2, 1);
		double xxp = coords[2];
		double yyp = coords[3];
		PointTMR row = rows.get(pathStart + i);
		row.setX(xx, xxp);
		row.setY(yy, yyp);
	    }
	}
	if (notify) {
	    fireTableChanged(pathStart+1, pathStart+origRows.length-2,
			     Mode.MODIFIED);
	}
    }

    // (paX, paY) has unit length
    public void scaleObject(PointTMR[] origRows, Point2D cm, Point2D cmp,
			    double paX, double paY,
			    double scaleX, double scaleY,
			    int pathStart, boolean notify)
    {

	AffineTransform af1 =
	    AffineTransform.getTranslateInstance(cm.getX(), cm.getY());
	af1.rotate(paX, paY);
	af1.scale(scaleX, scaleY);
	af1.rotate(paX, -paY);
	af1.translate(-cm.getX(), -cm.getY());

	AffineTransform af2 =
	    AffineTransform.getTranslateInstance(cmp.getX(), cmp.getY());
	af2.rotate(paX, -paY);
	af2.scale(scaleX, scaleY);
	af2.rotate(paX, paY);
	af2.translate(-cmp.getX(), -cmp.getY());

	for (int i = 1; i <  origRows.length-1; i++) {
	    PointTMR origRow = origRows[i];
	    if (origRow.getMode() != SplinePathBuilder.CPointType.CLOSE) {
		coords[0] = origRow.getX();
		coords[1] = origRow.getY();
		af1.transform(coords, 0, coords, 2, 1);
		double xx = coords[2];
		double yy = coords[3];
		coords[0] = origRow.getXP();
		coords[1] = origRow.getYP();
		af2.transform(coords, 0, coords, 2, 1);
		double xxp = coords[2];
		double yyp = coords[3];
		PointTMR row = rows.get(pathStart + i);
		row.setX(xx, xxp);
		row.setY(yy, yyp);
	    }
	}
	if (notify) {
	    fireTableChanged(pathStart+1, pathStart+origRows.length-2,
			     Mode.MODIFIED);
	}
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
		OffsetPane.removeOurPathName(varname);
		OffsetPane.removeBasename(varname);
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
		    OffsetPane.removeOurPathName(varname);
		    OffsetPane.removeBasename(varname);
		}
	    }
	}
	fireTableChanged(fromIndex, toIndex-1, Mode.DELETED);
    }

    // index must point to a location or the start of a path
    // That entry will be moved to the end of the table and the
    // other entries shifted.
    public int moveToEnd(int index) {
	int cnt = 0;
	int sz = rows.size();
	Enum<?> mode = rows.get(cnt+index).getMode();
	Enum<?> endMode = (sz == 0)? null: getLastRow().getMode();
	if (endMode == null || (endMode != EPTS.Mode.LOCATION
				&& endMode != EPTS.Mode.PATH_END)) {
	    // safety check - we should not do anything if a path is
	    // currently being edited. We should not be able to get
	    // here unless a menu item is being enabled at the wrong time.
	    System.err.println("called moveToEnd while editing a path?");
	    return 0;
	}
	if (mode == EPTS.Mode.LOCATION) {
	    cnt++;
	} else {
	    if (mode != EPTS.Mode.PATH_START) return 0;
	    cnt++;
	    while (cnt+index < sz) {
		mode = rows.get(cnt+index).getMode();
		boolean done = false;
		if (mode == EPTS.Mode.LOCATION) {
		    // should not occur.
		    System.err.println("PointTableModel.moveToEnd: "
				       + " unexpected EPTS.Mode.LOCATION");
		    return 0;
		} else if (mode == EPTS.Mode.PATH_END) {
		    cnt++;
		    break;
		} else {
		    cnt++;
		}
	    }
	}
	ArrayList<PointTMR> tmp = new ArrayList<>(cnt);
	for (int i = 0; i < cnt; i++) {
	    tmp.add(rows.get(index));
	    rows.remove(index);
	}
	rows.addAll(tmp);
	fireTableChanged(index, sz, Mode.MODIFIED);
	return cnt;
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

    public void fireRowsChanged(int index) {
	if (index == -1) return;
	int start = findStart(index);
	int end = findEnd(index);
	if (start != -1 && end != -1) {
	    if (end-start > 1) {
		fireTableChanged(start+1, end-1, Mode.MODIFIED);
	    } else {
		fireTableChanged(start, end, Mode.MODIFIED);
	    }
	}
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
	    throw new
		IndexOutOfBoundsException(errorMsg("badColumn", columnIndex));
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
	    throw new
		IndexOutOfBoundsException(errorMsg("badColumn", columnIndex));
	}
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
	throw new IllegalStateException(errorMsg("notEditable"));
    }

    public int findRowXPYP(double xp, double yp, double zoom) {
	int i = 0;
	double limit = 7.0/zoom;
	// compare square of distances to avoid having to
	// compute a square root
	limit *= limit;
	int result = -1;
	for (PointTMR row: rows) {
	    if (!row.isSelectable()) {
		i++;
		continue;
	    }
	    double d = Point2D.distanceSq(xp, yp, row.getXP(), row.getYP());
	    if (d < limit) {
		// We previously returned the first match. Returning
		// the last match is better because the last path or
		// point edited will be at the end of the list. Users
		// will frequently create a path and then try to edit
		// their newly created points in order to fix up any
		// errors.
		result = i;
		// return i;
	    }
	    i++;
	}
	return result;
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
	BasicSplinePathBuilder spb = null;
	int u = -1;
	for (PointTMR row: rows) {
	    index++;
	    Enum mode = row.getMode();
	    String varname = null;
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
		kmap2.put("yp", String.format((Locale) null,"%s", row.getYP()));
		kmap2.put("ypr", String.format((Locale)null, "%s",
						height - row.getYP()));
		list.add(kmap1);
	    } else if (mode == EPTS.Mode.PATH_START) {
		vindex++;
		kmap1 = new TemplateProcessor.KeyMap();
		kmap2 = new TemplateProcessor.KeyMap();
		kmap2.put("draw", "false");
		kmap2.put("fill", "false");
		varname = row.getVariableName();
		kmap1.put("varname", varname);
		kmap1.put("index", ("" + index));
		kmap1.put("vindex", ("" + vindex));
		kmap1.put("pathStatement", kmap2);
		list.add(kmap1);
		spb = new BasicSplinePathBuilder();
		u = -1;
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
		if (mode == SplinePathBuilder.CPointType.CLOSE) {
		    spb.append(new SplinePathBuilder.CPoint
			       (SplinePathBuilder.CPointType.CLOSE));
		} else {
		    spb.append(new SplinePathBuilder.CPoint
			       ((SplinePathBuilder.CPointType)mode,
				row.getX(), row.getY()));
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
		    TemplateProcessor.KeyMap kmap5 =
			new TemplateProcessor.KeyMap();
		    kmap3.put("hasParameterInfo", kmap5);
		    kmap5.put("u",  (++u) + ".0");
		    // kmap5.put("subvarname", varname);
		} else if (mode == SplinePathBuilder.CPointType.CLOSE) {
		    kmap3.put("ltype", "SEG_CLOSE");
		    TemplateProcessor.KeyMap kmap5 =
			new TemplateProcessor.KeyMap();
		    kmap3.put("hasParameterInfo", kmap5);
		    kmap5.put("u",  (++u) + ".0");
		    // kmap5.put("subvarname", varname);
		} else {
		    kmap3.put("ltype", mode.toString());
		    TemplateProcessor.KeyMap kmap5 =
			new TemplateProcessor.KeyMap();
		    kmap3.put("hasParameterInfo", kmap5);
		    if (mode == SplinePathBuilder.CPointType.MOVE_TO) {
			u = -1;
			kmap5.put("s", "0.0");
		    }
		    kmap5.put("u",  (++u) + ".0");
		    // kmap5.put("subvarname", varname);
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
	    } else if (mode == EPTS.Mode.PATH_END) {
		BasicSplinePath2D spath = spb.getPath();
		int ind = -1;
		int v = -1;
		for (TemplateProcessor.KeyMap km: plist) {
		    ind++;
		    if (km.get("hasParameterInfo") == null) continue;
		    v++;
		    if (km.get("s") != null) continue;
		    double uu = (double)v;
		    if (uu <= spath.getMaxParameter() + 0.1) {
			km.put("s",  "" + spath.s(uu));
		    } else if (km.get("type").equals("CLOSE")) {
			km.remove("hasParameterInfo");
		    } else {
			System.err.print("epts: at index " + ind + " (type "
					 + km.get("type") + "), ");
			System.err.println(errorMsg("maxpathparm"));
		    }
		}
	    }
	}
	TemplateProcessor.KeyMap map = new TemplateProcessor.KeyMap();
	map.put("items", list);
	return map;
    }

    private static boolean isOurPath(String[] names, String name) {
	for (String s: names) {
	    if (name.equals(s)) {
		return true;
	    }
	}
	return false;
    }

    private static final TemplateProcessor.KeyMap emptyMap =
	new TemplateProcessor.KeyMap();


    public TemplateProcessor.KeyMap getKeyMap(EPTS.FilterInfo[] filters,
					      Map<String,String> tmap,
					      double height)
	throws IllegalArgumentException
    {
	TemplateProcessor.KeyMapList list = new TemplateProcessor.KeyMapList();
	TemplateProcessor.KeyMap kmap1 = null;
	TemplateProcessor.KeyMap kmap2 = null;
	TemplateProcessor.KeyMap kmap3 = null;
	TemplateProcessor.KeyMapList plist = null;
	int n = rows.size();
	int index = 0;
	int vindex = 0;
	int pindex = 0;
	BasicSplinePathBuilder spb = null;
	int u = -1;
	for (EPTS.FilterInfo filter: filters) {
	    boolean ignore =  true;
	    boolean addname = true;
	    String name = filter.name;
	    String[] names  = filter.nameArray;
	    String windingRule = filter.windingRule;
	    String draw = filter.draw;
	    String fill = filter.fill;
	    String gcsMode = filter.gcsMode;
	    String subvarname = null;
	    for (PointTMR row: rows) {
		index++;
		Enum mode = row.getMode();
		if (mode == EPTS.Mode.LOCATION) {
		    if (isOurPath(names, row.getVariableName())) {
			if (names.length != 1) {
			    throw new IllegalArgumentException
				("Cannot concatenate locations");
			}
			vindex++;
			kmap1 = new TemplateProcessor.KeyMap();
			kmap2 = new TemplateProcessor.KeyMap();
			kmap1.put("varname", name);
			kmap1.put("vindex", ("" + vindex));
			kmap1.put("index", ("" + index));
			kmap1.put("location", kmap2);
			kmap2.put("x", String.format((Locale)null, "%s",
						     row.getX()));
			kmap2.put("y", String.format((Locale)null, "%s",
						     row.getY()));
			kmap2.put("xp", String.format((Locale)null, "%s",
						      row.getXP()));
			kmap2.put("yp", String.format((Locale) null, "%s",
						      row.getYP()));
			kmap2.put("ypr", String.format((Locale)null, "%s",
						       height - row.getYP()));
			list.add(kmap1);
			break;
		    }
		} else if (mode == EPTS.Mode.PATH_START) {
		    if (isOurPath(names, row.getVariableName())) {
			ignore = false;
			spb = new BasicSplinePathBuilder();
			u = -1;
			subvarname = row.getVariableName();
			if (subvarname.equals(name)) subvarname = null;
			if (addname) {
			    vindex++;
			    kmap1 = new TemplateProcessor.KeyMap();
			    kmap2 = new TemplateProcessor.KeyMap();
			    kmap1.put("varname", name);
			    kmap1.put("index", ("" + index));
			    kmap1.put("vindex", ("" + vindex));
			    kmap1.put("pathStatement", kmap2);
			    list.add(kmap1);
			    plist = new TemplateProcessor.KeyMapList();
			    pindex = 0;
			    if (windingRule != null) {
				kmap2.put("windingRule", windingRule);
				kmap2.put("hasWindingRule", emptyMap);
			    }
			    kmap2.put("draw", draw);
			    kmap2.put("fill", fill);
			    if (filter.draw.equals("true")
				|| filter.fill.equals("true")) {
				kmap2.put("hasAttributes", emptyMap);
			    }
			    if (filter.gcsMode != null) {
				kmap2.put("gcsMode", gcsMode);
				kmap2.put("hasGcsMode", emptyMap);
			    }
			    if (filter.drawColor != null) {
				kmap2.put("drawColor", filter.drawColor);
				kmap2.put("hasDrawColor", emptyMap);
			    }
			    if (filter.fillColor != null) {
				kmap2.put("fillColor", filter.fillColor);
				kmap2.put("hasFillColor", emptyMap);
			    }
			    if (filter.strokeCap != null) {
				kmap2.put("strokeCap", filter.strokeCap);
				kmap2.put("hasStrokeCap", emptyMap);
			    }
			    if (filter.dashIncrement != null) {
				kmap2.put("dashIncrement",
					  filter.dashIncrement);
				kmap2.put("hasDashIncrement", emptyMap);
			    }
			    if (filter.dashPhase != null) {
				kmap2.put("dashPhase", filter.dashPhase);
				kmap2.put("hasDashPhase", emptyMap);
			    }
			    if (filter.dashPattern != null) {
				kmap2.put("dashPattern", filter.dashPattern);
				kmap2.put("hasDashPattern", emptyMap);
			    }
			    if (filter.strokeJoin != null) {
				kmap2.put("strokeJoin", filter.strokeJoin);
				kmap2.put("hasStrokeJoin", emptyMap);
			    }
			    if (filter.miterLimit != null) {
				kmap2.put("miterLimit", filter.miterLimit);
				kmap2.put("hasMiterLimit", emptyMap);
			    }
			    if (filter.strokeWidth != null) {
				kmap2.put("strokeWidth", filter.strokeWidth);
				kmap2.put("hasStrokeWidth", emptyMap);
			    }
			    if (filter.zorder != null) {
				kmap2.put("zorder", filter.zorder);
				kmap2.put("hasZorder", emptyMap);
			    }

			    kmap2.put("pathItem", plist);
			    kmap3 = null;
			    addname = false;
			}
		    }
		} else if (mode instanceof SplinePathBuilder.CPointType) {
		    if (ignore) continue;
		    if (index == n) {
			if (mode == SplinePathBuilder.CPointType.CONTROL
			    || mode == SplinePathBuilder.CPointType.SPLINE) {
			    // terminate a partial path
			    mode = SplinePathBuilder.CPointType.SEG_END;
			}
		    }
		    if (spb != null) {
			if (mode == SplinePathBuilder.CPointType.CLOSE) {
			    spb.append(new SplinePathBuilder.CPoint
				       (SplinePathBuilder.CPointType.CLOSE));
			} else {
			    spb.append(new SplinePathBuilder.CPoint
				       ((SplinePathBuilder.CPointType)mode,
					row.getX(), row.getY()));
			}
		    }
		    if (kmap3 != null) kmap3.put("optcomma", ",");
		    kmap3 = new TemplateProcessor.KeyMap();
		    // kmap3.put("index", ("" + index));
		    pindex++;
		    kmap3.put("pindex", ("" + pindex));
		    kmap3.put("type", mode.toString());
		    if (mode == SplinePathBuilder.CPointType.CONTROL) {
			kmap3.put("ltype", "CONTROL_POINT");
		    } else if (mode == SplinePathBuilder.CPointType.SPLINE) {
			kmap3.put("ltype", "SPLINE_POINT");
			TemplateProcessor.KeyMap kmap5 =
			    new TemplateProcessor.KeyMap();
			kmap3.put("hasParameterInfo", kmap5);
			kmap5.put("u",  (++u) + ".0");
			if (subvarname != null) {
			    kmap5.put("subvarname", subvarname);
			    kmap5.put("hasSubvarname", emptyMap);
			}
		    } else if (mode == SplinePathBuilder.CPointType.CLOSE) {
			kmap3.put("ltype", "SEG_CLOSE");
			TemplateProcessor.KeyMap kmap5 =
			    new TemplateProcessor.KeyMap();
			kmap3.put("hasParameterInfo", kmap5);
			kmap5.put("u",  (++u) + ".0");
			if (subvarname != null) {
			    kmap5.put("subvarname", subvarname);
			    kmap5.put("hasSubvarname", emptyMap);
			}
		    } else {
			kmap3.put("ltype", mode.toString());
			TemplateProcessor.KeyMap kmap5 =
			    new TemplateProcessor.KeyMap();
			kmap3.put("hasParameterInfo", kmap5);
			if (mode == SplinePathBuilder.CPointType.MOVE_TO) {
			    u = -1;
			    kmap5.put("s", "0.0");
			}
			kmap5.put("u",  (++u) + ".0");
			if (subvarname != null) {
			    kmap5.put("subvarname", subvarname);
			    kmap5.put("hasSubvarname", emptyMap);
			}
		    }
		    if (tmap != null) {
			String val = tmap.get(mode.toString());
			kmap3.put("atype", (val == null)? mode.toString(): val);
		    }
		    if (mode != SplinePathBuilder.CPointType.CLOSE) {
			TemplateProcessor.KeyMap
			    kmap4 = new TemplateProcessor.KeyMap();
			kmap4.put("x",
				  String.format((Locale)null, "%s",
						row.getX()));
			kmap4.put("y",
				  String.format((Locale)null, "%s",
						row.getY()));
			kmap4.put("xp",
				  String.format((Locale)null, "%s",
						row.getXP()));
			kmap4.put("yp",
				  String.format((Locale)null, "%s",
						row.getYP()));
			kmap4.put("ypr", String.format((Locale)null, "%s",
						       height - row.getYP()));
			kmap3.put("xy", kmap4);
		    }
		    plist.add(kmap3);
		} else if (mode == EPTS.Mode.PATH_END) {
		    ignore = true;
		    if (spb != null) {
			BasicSplinePath2D spath = spb.getPath();
			int v = -1;
			int ind = -1;
			for (TemplateProcessor.KeyMap km: plist) {
			    ind++;
			    if (km.get("hasParameterInfo") == null) continue;
			    v++;
			    if (km.get("s") != null) continue;
			    double uu = (double)v;
			    if (uu <= spath.getMaxParameter() + 0.1) {
				km.put("s",  "" + spath.s(uu));
			    } else if (km.get("type").equals("CLOSE")) {
				km.remove("hasParamterInfo");
			    } else {
				System.err.print("epts: at index " + ind
						 + " (type "
						 + km.get("type") + "), ");
				System.err.println(errorMsg("maxpathparm"));
			    }
			}
			spb = null;
		    }
		}
	    }
	}
	TemplateProcessor.KeyMap map = new TemplateProcessor.KeyMap();
	map.put("items", list);
	return map;
    }

    // provided for generating SVG files.
    public Map<String,TemplateProcessor.KeyMap>
	getLocationMap(double height, boolean gcs)
    {
	int index = 0;
	int vindex = 0;
	int sz = 3 + ((names.size() - pnames.size())/2)*3;
	Map<String,TemplateProcessor.KeyMap> map = new
	    LinkedHashMap<String,TemplateProcessor.KeyMap>(sz);
	for (PointTMR row: rows) {
	    index++;
	    Enum mode = row.getMode();
	    if (mode == EPTS.Mode.LOCATION) {
		vindex++;
		TemplateProcessor.KeyMap kmap1 = new TemplateProcessor.KeyMap();
		TemplateProcessor.KeyMap kmap2 = new TemplateProcessor.KeyMap();
		String name = row.getVariableName();
		kmap1.put("varname", name);
		kmap1.put("vindex", ("" + vindex));
		kmap1.put("index", ("" + index));
		kmap1.put("location", kmap2);
		double xL, xR, yT, yB, lw;
		if (gcs) {
		    double r = height/100.0;
		    double x = row.getX();
		    double y = row.getY();
		    lw = r/10.0;
		    xL = x - r;
		    xR = x + r;
		    yT = y + r;
		    yB = y - r;
		    kmap2.put("x", String.format((Locale)null, "%s", x));
		    kmap2.put("y", String.format((Locale)null, "%s", y));
		    kmap2.put("r", String.format((Locale)null, "%s", r));
		} else {
		    double r = 10.0;
		    double x = row.getXP();
		    double y = row.getYP();
		    lw = r/10.0;
		    xL = x - r;
		    xR = x + r;
		    yT = y - r;
		    yB = y + r;
		    kmap2.put("x", String.format((Locale)null, "%s", x));
		    kmap2.put("y", String.format((Locale) null,"%s", y));
		    kmap2.put("r", String.format((Locale)null, "%s", r));
		}
		kmap2.put("xL", String.format((Locale)null, "%s", xL));
		kmap2.put("xR", String.format((Locale)null, "%s", xR));
		kmap2.put("yT", String.format((Locale)null, "%s", yT));
		kmap2.put("yB", String.format((Locale)null, "%s", yB));
		kmap2.put("lw", String.format((Locale)null, "%s", lw));
		map.put(name, kmap1);
	    } else if (mode == EPTS.Mode.PATH_START) {
		vindex++;
	    }
	}
	return map;
    }
}
