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

import org.bzdev.util.TemplateProcessor;

public class PTFilters {
    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    Component frame;
    JMenu menu;
    PointTableModel ptmodel;

    public PTFilters(Component frame, JMenu menu, PointTableModel ptmodel) {
	this.frame = frame;
	this.menu = menu;
	this.ptmodel = ptmodel;
    }

    TreeMap<String,PTFilter> map = new TreeMap<>();

    public int size() {
	return map.size();
    }

    public PTFilter getFilter(String name) {return map.get(name);}

    public void clear() {
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
	    System.out.println("... filterName = "+filterMap.get("filterName"));
	    System.out.println("... filterMode = "+filterMap.get("filterMode"));
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


    // to restore a filter from a saved-state file
    public boolean addFilter(String name, final JPanel panel,
			     PointTMR.FilterMode defaultMode,
			     List<Entry> entries) {
	if (map.containsKey(name)) return false;
	final PTFilter filter = new PTFilter(name, ptmodel, false);
	map.put(name, filter);
	filter.setMode(defaultMode);
	PTFilterModel model = filter.getModel();
	for (Entry entry: entries) {
	    PointTFMRow row = new PointTFMRow(entry.name);
	    row.setMode(entry.mode);
	    model.add(row);
	}

	final JMenuItem item = new JMenuItem(name);
	final ActionListener listener = (e) -> {
	    filter.editFilter(frame, (eApply) -> {
		    filter.apply();
		    panel.repaint();
		    menu.setText(localeString("FilterInUse"));
		    return;
		},
		(eSave) -> {
		    // nothing to do as dialog will automatically close
		    return;
		},
		(eDelete) -> {
		    map.remove(filter.getName());
		    menu.remove(item);
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
	final PTFilter filter = new PTFilter(name, ptmodel);
	map.put(name, filter);
	final JMenuItem item = new JMenuItem(name);
	final ActionListener listener = (e) -> {
	    filter.editFilter(frame, (eApply) -> {
		    filter.apply();
		    panel.repaint();
		    menu.setText(localeString("FilterInUse"));
		    return;
		},
		(eSave) -> {
		    // nothing to do as dialog will automatically close
		    return;
		},
		(eDelete) -> {
		    map.remove(filter.getName());
		    menu.remove(item);
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
