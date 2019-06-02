package org.bzdev.epts;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import org.bzdev.geom.SplinePathBuilder;

public class PTFilter {

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    private String name;
    /**
     * Get the print name for this filter.
     */
    public String getName() {return name;}

    PointTableModel ptmodel;
    PTFilterModel fmodel;

    public PTFilterModel getModel() {return fmodel;}

    PointTMR.FilterMode defaultMode  = PointTMR.FilterMode.INVISIBLE;

    public PTFilter(String name, PointTableModel ptmodel) {
	this(name, ptmodel, true);
    }

    public PTFilter(String name, PointTableModel ptmodel, boolean fillRows) {
	this.name = name;
	this.ptmodel = ptmodel;
	fmodel = new PTFilterModel(ptmodel, fillRows);
    }

    public void merge() {
	// to update the model just before the component is
	// displayed.
	fmodel.merge();
    }

    public void setMode(PointTMR.FilterMode mode) {
	defaultMode = mode;
    }

    public PointTMR.FilterMode getMode() {return defaultMode;}

    private static void addComponent(JPanel panel, JComponent component,
				     GridBagLayout gridbag,
				     GridBagConstraints c)
    {
	gridbag.setConstraints(component, c);
	panel.add(component);
    }

    static String filterOptions[] = {
	localeString("FilterApply"),
	localeString("FilterSaveOnly"),
	localeString("FilterDelete")
    };
    
    static JComboBox<PointTMR.FilterMode> fmodeComboBox
	= null;

    public void editFilter(Component anchorComponent,
			   ActionListener applyAction,
			   ActionListener saveAction,
			   ActionListener deleteAction,
			   ActionEvent event)
    {
	try {
	    JLabel modeLabel = new JLabel(localeString("filterMode"));
	    JComboBox<PointTMR.FilterMode> modeCB
		= new JComboBox<>(PointTMR.FilterMode.values());
	    fmodeComboBox = new JComboBox<>(PointTMR.FilterMode.values());

	    modeCB.setSelectedIndex(getMode().ordinal());
	    modeCB.addActionListener((ev) -> {
		    Object obj = modeCB.getSelectedItem();
		    if (obj instanceof PointTMR.FilterMode) {
			PointTMR.FilterMode item = (PointTMR.FilterMode)obj;
			setMode(item);
		    }
		});
	    JLabel tableLabel =
		new JLabel(localeString("filterTableHdr"));
	    JTable table = new JTable() {
		    public boolean isCellEditable(int row, int column) {
			return (column == 1);
		    }
		};
	    table.setModel(fmodel);
	    table.setCellSelectionEnabled(true);
	    table.getColumnModel().getColumn(1).setCellEditor
		(new DefaultCellEditor(fmodeComboBox));
	    
	    table.getTableHeader().setReorderingAllowed(false);
	    table.getSelectionModel().setSelectionMode
		(ListSelectionModel.SINGLE_SELECTION);
	    JScrollPane scrollpane = new JScrollPane(table);
	    int twidth = Setup.configColumn(table, 0,
					    "mmmmmmmmmmmmmmmmmmmmmmmm");
	    twidth += Setup.configColumn(table, 1, "mmmmmmmmm");
	    scrollpane.setPreferredSize(new Dimension(twidth+10, 275));
	    JPanel panel = new JPanel();

	    GridBagLayout gridbag = new GridBagLayout();
	    GridBagConstraints c = new GridBagConstraints();
	    c.insets = new Insets(4, 8, 4, 8);
	    panel.setLayout(gridbag);

	    c.anchor = GridBagConstraints.LINE_END;
	    c.gridwidth = 1;
	    addComponent(panel, modeLabel, gridbag, c);
	    c.anchor = GridBagConstraints.LINE_START;
	    c.gridwidth = GridBagConstraints.REMAINDER;
	    addComponent(panel, modeCB, gridbag, c);

	    c.anchor = GridBagConstraints.BASELINE;
	    c.gridwidth = GridBagConstraints.REMAINDER;
	    addComponent(panel, tableLabel, gridbag, c);
	    c.anchor = GridBagConstraints.BASELINE;
	    c.gridwidth = GridBagConstraints.REMAINDER;
	    addComponent(panel, scrollpane, gridbag, c);

	    String title =String.format(localeString("filterTitle"), getName());
	    switch(JOptionPane.showOptionDialog(anchorComponent,
						panel, title,
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						filterOptions,
						filterOptions[0])) {
	    case 0:
		applyAction.actionPerformed(event);
		break;
	    case 1:
		saveAction.actionPerformed(event);
		break;
	    case 2:
		deleteAction.actionPerformed(event);
		break;
	    default:
		break;
	    }
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	}
    }


    public void apply() {
	PointTMR.FilterMode mode = null;
	for (PointTMR row: ptmodel.getRows()) {
	    row.setFilterMode(defaultMode);
	    Enum<?> rmode = row.getMode();
	    if (rmode == EPTS.Mode.LOCATION) {
		mode = fmodel.getMode(row.getVariableName());
		row.setFilterMode(mode);
		mode = null;
	    } else if (rmode == EPTS.Mode.PATH_START) {
		mode = fmodel.getMode(row.getVariableName());
		row.setFilterMode(mode);
	    } else if (rmode == EPTS.Mode.PATH_END) {
		row.setFilterMode(mode);
		mode = null;
	    } else if (rmode instanceof SplinePathBuilder.CPointType) {
		row.setFilterMode(mode);
	    }
	}
    }
}
