package org.bzdev.epts;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bzdev.lang.Callable;
import org.bzdev.lang.CallableReturns;
import org.bzdev.util.TemplateProcessor;

public class PTFilters {
    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    Component frame;
    JMenu menu;
    PointTableModel ptmodel;

    CallableReturns<Boolean> tryClear = null;
    private boolean tryClear() {
	return tryClear.call();
    }

    Callable callClearLSRM = null;
    void clearLSRM() {callClearLSRM.call();}

    public PTFilters(Component frame, JMenu menu, PointTableModel ptmodel,
		     CallableReturns<Boolean> callClearLSRM) {
	this.frame = frame;
	this.menu = menu;
	this.ptmodel = ptmodel;
	// this.callClearLSRM = callClearLSRM;
	this.tryClear = callClearLSRM;
    }

    TreeMap<String,PTFilter> map = new TreeMap<>();

    public int size() {
	return map.size();
    }

    public PTFilter getFilter(String name) {return map.get(name);}

    public void clear() {
	if (currentFilter != null) {
	    currentFilter.setSelected(false);
	}
	for (PointTMR row: ptmodel.getRows()) {
	    row.setFilterMode(PointTMR.FilterMode.SELECTABLE);
	}
    }

    public TemplateProcessor.KeyMapList getKeyMapList() {
	TemplateProcessor.KeyMapList list = new TemplateProcessor.KeyMapList();
	for (Map.Entry<String,PTFilter> entry: map.entrySet()) {
	    TemplateProcessor.KeyMap filterMap = new TemplateProcessor.KeyMap();
	    String filterName = entry.getKey();
	    PTFilter filter = entry.getValue();
	    filterMap.put("filterName", filterName);
	    filterMap.put("filterMode", filter.getMode().name());
	    filterMap.put("filterRows", filter.getModel().getKeyMapList());
	    list.add(filterMap);
	}
	return list;
    }


    public static class Entry {
	public String name;
	public PointTMR.FilterMode mode;
    }

    public static class TopEntry {
	public String name;
	public PointTMR.FilterMode mode;
	ArrayList<Entry> entries;
    }

    PTFilter currentFilter = null;

    // to restore a filter from a saved-state file
    public boolean addFilter(String name, final JPanel panel,
			     PointTMR.FilterMode defaultMode,
			     List<Entry> entries) {
	if (map.containsKey(name)) return false;
	final JMenuItem item = new JMenuItem(name);
	final PTFilter filter = new PTFilter(name, ptmodel, item, false);
	map.put(name, filter);
	filter.setMode(defaultMode);
	PTFilterModel model = filter.getModel();
	for (Entry entry: entries) {
	    PointTFMRow row = new PointTFMRow(entry.name);
	    row.setMode(entry.mode);
	    model.add(row);
	}

	final ActionListener listener = (e) -> {
	    // clearLSRM();
	    if (!tryClear()) return;
	    filter.merge();	// in case paths/locations have changed
	    filter.editFilter(frame, (eApply) -> {
		    filter.apply();
		    panel.repaint();
		    menu.setText(localeString("FilterInUse"));
		    if (currentFilter != null) {
			currentFilter.setSelected(false);
		    }
		    filter.setSelected(true);
		    currentFilter = filter;
		    return;
		},
		(eSave) -> {
		    // nothing to do as dialog will automatically close
		    return;
		},
		(eDelete) -> {
		    map.remove(filter.getName());
		    menu.remove(item);
		    if (currentFilter == filter) {
			// clearLSRM();
			if (!tryClear()) return;
			clear();
			currentFilter = null;
			menu.setText(localeString("Filters"));
			panel.repaint();
		    }
		    return;
		}, e);
	    return;
	};
	item.addActionListener(listener);
	menu.add(item);
	return true;
    }


    public boolean addFilter(String name, ActionEvent event,
			     final JPanel panel)
    {
	if (map.containsKey(name)) return false;
	final JMenuItem item = new JMenuItem(name);
	final PTFilter filter = new PTFilter(name, ptmodel, item);
	map.put(name, filter);
	final ActionListener listener = (e) -> {
	    filter.merge();	// in case paths/locations have changed
	    filter.editFilter(frame, (eApply) -> {
		    // clearLSRM();
		    if (!tryClear()) return;
		    filter.apply();
		    if (currentFilter != null) {
			currentFilter.setSelected(false);
		    }
		    panel.repaint();
		    menu.setText(localeString("FilterInUse"));
		    filter.setSelected(true);
		    currentFilter = filter;
		    return;
		},
		(eSave) -> {
		    // nothing to do as dialog will automatically close
		    return;
		},
		(eDelete) -> {
		    if (currentFilter == filter) {
			if (!tryClear()) return;
		    }
		    map.remove(filter.getName());
		    menu.remove(item);
		    if (currentFilter == filter) {
			//clearLSRM();
			clear();
			currentFilter = null;
			menu.setText(localeString("Filters"));
			panel.repaint();
		    }
		    return;
		}, e);
	    return;
	};
	item.addActionListener(listener);
	menu.add(item);
	listener.actionPerformed(event);
	return true;
    }

}
