package org.bzdev.epts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.bzdev.util.TemplateProcessor;


public class PTFilterModel implements TableModel {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    PointTMR.FilterMode defaultMode  = PointTMR.FilterMode.INVISIBLE;

    PointTableModel ptmodel;

    public PTFilterModel(PointTableModel ptmodel) {
	this(ptmodel, true);
    }

    public PTFilterModel(PointTableModel ptmodel, boolean fillRows) {
	this.ptmodel = ptmodel;
	if (fillRows) {
	    for (String name: ptmodel.getVariableNames()) {
		PointTFMRow row = new PointTFMRow(name);
		rows.add(row);
		map.put(name, row);
	    }
	}
    }

    Vector<TableModelListener> listeners = new Vector<>();
    ArrayList<PointTFMRow> rows = new ArrayList<>();
    HashMap<String,PointTFMRow> map = new HashMap<>();

    public void add(PointTFMRow row) {
	rows.add(row);
	map.put(row.getName(), row);
    }

    public List<PointTFMRow> getRows() {
	return Collections.unmodifiableList(rows);
    }

    public PointTMR.FilterMode getMode(String name) {
	PointTFMRow row = map.get(name);
	return (row == null)? null: row.getMode();
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
	listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
	listeners.remove(l);
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
	switch (columnIndex) {
	case 0:
	    return String.class;
	case 1:
	    return Enum.class;
	default:
	    return Object.class;
	}
    }

    @Override
    public int getColumnCount() {
	return 2;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	if (columnIndex == 1) return true;
	return false;
	/*
	if (columnIndex  == 1 && getColumnClass(columnIndex) != Enum.class) {
	    System.out.println("wrong cell editable?");
	}
	return getColumnClass(columnIndex) == Enum.class;
	*/
    }

    @Override
    public String getColumnName(int columnIndex) {
	switch(columnIndex) {
	case 0:
	    return localeString("Variable");
	case 1:
	    return localeString("filterMode");
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
	PointTFMRow row = rows.get(rowIndex);
	Enum mode = row.getMode();
	switch (columnIndex) {
	case 0:
	    return row.getVariableName();
	case 1:
	    return row.getMode();
	default:
	    return null;
	}
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
	if (columnIndex == 1) {
	    if (value instanceof PointTMR.FilterMode) {
		PointTMR.FilterMode mode = (PointTMR.FilterMode)value;
		PointTFMRow row = rows.get(rowIndex);
		if (row != null) {
		    row.setMode(mode);
		} else {
		    throw new
			IllegalArgumentException(errorMsg("notFilterMode"));
		}
	    }
	} else {
	    throw new IllegalStateException(errorMsg("notEditable"));
	}
    }

    public TemplateProcessor.KeyMapList getKeyMapList() {
	TemplateProcessor.KeyMapList list = new TemplateProcessor.KeyMapList();
	for (PointTFMRow row: rows) {
	    TemplateProcessor.KeyMap map = new TemplateProcessor.KeyMap();
	    map.put("filterVarname", row.getVariableName());
	    map.put("filterRowMode", row.getMode().name());
	    list.add(map);
	}
	return list;
    }
}
