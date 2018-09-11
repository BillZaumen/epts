import java.awt.*;
import java.awt.event.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import javax.swing.text.EditorKit;
import org.bzdev.scripting.Scripting;
import org.bzdev.io.ZipDocFile;
import org.bzdev.io.ZipDocWriter;

public class Setup {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

 static enum ControlType {
	JARFILE, SCRIPTFILE, VALUE,
    }
    static final String blankRow1[] = {""};
    static final String blankRow4[] = {"", "", "", ""};


    private static void addComponent(JPanel panel, JComponent component,
				     GridBagLayout gridbag,
				     GridBagConstraints c)
    {
	gridbag.setConstraints(component, c);
	panel.add(component);
    }

    private static final String typeStrings[] = {
	localeString("typeNotUsed"),  // index 0		
	localeString("typeBoolean"),  // index 1
	localeString("typeInteger"),  // index 2
	localeString("typeReal"),     // index 3
	localeString("typeString")    //index 4
    };

    private static Integer getTypeIndex(Object val) {
	int n = typeStrings.length;
	for (int i = 0; i < n; i++) {
	    if (typeStrings[i].equals(val)) {
		return new Integer(i);
	    }
	}
	return new Integer(-1);
    }

    private static final String unitStrings[] = {
	localeString("unitsNotUsed"),     //index 0
	localeString("unitsCustomUnits"), // index 1
	localeString("unitsnm"),           // index 2
	localeString("unitsum"),           // index 3
	localeString("unitsmm"),           // index 4
	localeString("unitscm"),           // index 5
	localeString("unitsm"),            // index 6
	localeString("unitskm"),           // index 7
	localeString("unitsinches"),       // index 8
	localeString("unitsfeet"),         // index 9
	localeString("unitsyards"),        // index 10
	localeString("unitsmiles")         // index 11
    };

    private static Integer getUnitIndex(Object val) {
	int n = unitStrings.length;
	for (int i = 0; i < n; i++) {
	    if (unitStrings[i].equals(val)) {
		return new Integer(i);
	    }
	}
	return new Integer(-1);
    }


    private static int longestUnitIndex = 0;
    static {
	int length = 0;
	int i  = 0;
	for (String s: unitStrings) {
	    int len = s.length();
	    if (s.length() > length) {
		length = len;
		longestUnitIndex = i;
	    }
	    i++;
	}
    }

    private static int findIndex(String[] strings, String value) {
	for (int  i = 0; i < strings.length; i++) {
	    if (value.equals(strings[i])) {
		return i;
	    }
	}
	return 0;
    }


    private static JTextField varTextField;			    
    private static JComboBox<String> typeComboBox;
    private static JTextField valueTextField;
    private static JPanel varEditor;
    private static JComboBox<String> unitComboBox;

    private static JPanel createVarEditor(final JTable table) {
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	JLabel varLabel = new JLabel(localeString("variableName"));
	varTextField = new JTextField(32);
	JLabel typeLabel = new JLabel(localeString("variableType"));
	typeComboBox = new JComboBox<String>(typeStrings);
	typeComboBox.setEditable(false);
	JLabel valueLabel = new JLabel("variableValue");
	valueTextField = new JTextField(40);
	JLabel unitLabel = new JLabel(localeString("variableUnits"));
	unitComboBox = new JComboBox<String>(unitStrings);
	unitComboBox.setEditable(false);

	GridBagConstraints c = new GridBagConstraints();
	c.insets = new Insets(4, 8, 4, 8);

	c.anchor  = GridBagConstraints.LINE_END;
	c.gridwidth = 1;
	addComponent(panel, varLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, varTextField, gridbag, c);

	c.anchor  = GridBagConstraints.LINE_END;
	c.gridwidth = 1;

	addComponent(panel, typeLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, typeComboBox, gridbag, c);

	
	c.anchor  = GridBagConstraints.LINE_END;
	c.gridwidth = 1;
	addComponent(panel, valueLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, valueTextField, gridbag, c);
	
	c.anchor  = GridBagConstraints.LINE_END;
	c.gridwidth = 1;
	addComponent(panel, unitLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, unitComboBox, gridbag, c);

	return panel;
    }
    
    private static void addControls(final JPanel pane, final JTextField tf,
				    final JTextField atf)
    {
	JPanel ourPanel = new JPanel();
	ourPanel.setLayout(new FlowLayout());
	JButton fileButton = new JButton(localeString("inputFileButton"));
	final String cd = System.getProperty("user.dir");

	fileButton.addActionListener((e) -> {
		String[] extensions = new String[1];
		String fname = localeString("BasicFTypes");
		JFileChooser fc = new JFileChooser(cd);
		fc.setAcceptAllFileFilterUsed(false);
		extensions[0] = "epts";
		FileNameExtensionFilter filter =
		    new FileNameExtensionFilter(fname, extensions);
		String[] iextensions =
		    ImageIO.getReaderFileSuffixes();
		FileNameExtensionFilter ifilter =
		    new FileNameExtensionFilter
		    (localeString("Images"), iextensions);
		String[] allExt =
		    new String[extensions.length + iextensions.length];
		allExt[0] = extensions[0];
		System.arraycopy(iextensions, 0, allExt, 1, iextensions.length);
		FileNameExtensionFilter afilter =
		    new FileNameExtensionFilter
		    (localeString("AllBasicFTypes"), allExt);
		fc.addChoosableFileFilter(afilter);
		fc.addChoosableFileFilter(filter);
		fc.addChoosableFileFilter(ifilter);
		int status = fc.showOpenDialog(pane);
		if (status == JFileChooser.APPROVE_OPTION) {
		    try {
			String path = fc.getSelectedFile().getAbsolutePath();
			tf.setText(path);
			usesImageFile = EPTS.isImagePath(path);
		    } catch (Exception eio) {
			eio.printStackTrace(System.err);
		    }
		}
	    });
	ourPanel.add(fileButton);
	JButton clearButton = new
	    JButton(localeString("clearFileButton"));
	clearButton.addActionListener((e) -> {
		tf.setText("");
	    });
	ourPanel.add(clearButton);

	JButton defaultAnimButton = new
	    JButton(localeString("defaultAnimButton"));
	defaultAnimButton.addActionListener((e) -> {
		atf.setText("a2d");
	    });
	ourPanel.add(defaultAnimButton);
	pane.add(ourPanel, "North");
    }


    private static Object builtinScripts[] = {
	localeString("gridBuiltin"),
	localeString("polarBuiltin")
    };
    private static String builtinScriptLocations[] = {
	"sresource:grid",
	"sresource:polar"
    };
    
    private static void addControls(final JPanel pane, final JTable table,
				    final ControlType type)
    {
	JPanel ourPanel = new JPanel();
	ourPanel.setLayout(new FlowLayout());
	if (type == ControlType.JARFILE || type == ControlType.SCRIPTFILE) {
	    JButton fileButton = new JButton(localeString("insertFileButton"));

	    fileButton.addActionListener((e) -> {
		    CellEditor ce = table.getCellEditor();
		    if (ce != null) {
			ce.stopCellEditing();
		    }
		    int[] rowIndices = table.getSelectedRows();
		    if (rowIndices.length == 0) {
			return;
		    } else if (rowIndices.length > 1) {
		    } else {
			int index = rowIndices[0];
			String fname;
			String cd;
			String[] extensions;
			switch(type) {
			case JARFILE:
			    cd = EPTS.ourCodebaseDir;
			    fname = "JAR files";
			    extensions = new String[2];
			    extensions[0] = "jar";
			    extensions[1] = "JAR";
			    break;
			case SCRIPTFILE:
			    cd = System.getProperty("user.dir");
			    fname = "Script files";
			    Set<String> eset = Scripting.getExtensionSet();
			    extensions = eset.toArray(new String[eset.size()]);
  			    break;
  			default:
			    return;
			}
			JFileChooser fc = new JFileChooser(cd);
			fc.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter =
			    new FileNameExtensionFilter(fname, extensions);
			fc.addChoosableFileFilter(filter);
			int status = fc.showOpenDialog(pane);
			if (status == JFileChooser.APPROVE_OPTION) {
			    try {
				table.setValueAt(fc.getSelectedFile()
						 .getAbsolutePath(), index, 0);
				index++;
				if (index >= table.getColumnCount()) {
				    DefaultTableModel tm =
					(DefaultTableModel)table.getModel();
				    tm.setRowCount(index+8);
				}
				table.setRowSelectionInterval(index,index);
			    } catch (Exception eio) {
				eio.printStackTrace(System.err);
			    }
			}
		    }
		});
	    ourPanel.add(fileButton);
	    if (type == ControlType.SCRIPTFILE) {
		JButton builtinButton =
		    new JButton(localeString("insertBuiltin"));
		builtinButton.addActionListener((e) -> {
			int[] rowIndices = table.getSelectedRows();
			if (rowIndices.length == 0) {
			    return;
			} else if (rowIndices.length > 1) {
			    return;
			} else {
			    int index = rowIndices[0];
			    String s = (String)JOptionPane.showInputDialog
				(pane, localeString("builtinMsg"),
				 localeString("builtinTitle"),
				 JOptionPane.PLAIN_MESSAGE,
				 null, builtinScripts,
				 builtinScripts[0]);
			    if (s != null) {
				int i  = -1;
				while ((++i) < builtinScripts.length) {
				    if (s.equals(builtinScripts[i])) {
					break;
				    }
				}
				table.setValueAt(builtinScriptLocations[i],
						 index, 0);
			    }
			}			
		    });
		ourPanel.add(builtinButton);
	    }
	} else {
	    JButton varButton =
		new JButton(localeString("modSelectedRowButton"));
	    varButton.addActionListener((e) -> {
		    CellEditor ce = table.getCellEditor();
		    if (ce != null) {
			ce.stopCellEditing();
		    }
		    int[] rowIndices = table.getSelectedRows();
		    if (rowIndices.length == 0) {
			return;
		    } else if (rowIndices.length > 1) {
			return;
		    } else {
			int index = rowIndices[0];

			String val = (String)table.getValueAt(index,0);
			if (val == null) val="";
			varTextField.setText(val);

			val = (String)table.getValueAt(index,1);
			if (val == null) val="";
			int ind = findIndex(typeStrings, val);
			typeComboBox.setSelectedIndex(ind);

			val = (String)table.getValueAt(index,2);
			if (val == null) val="";
			valueTextField.setText(val);

			val = (String)table.getValueAt(index,3);
			if (val == null) val="";
			ind = findIndex(unitStrings, val);
			unitComboBox.setSelectedIndex(ind);

			int status = JOptionPane.showConfirmDialog
			    (pane, varEditor, "EPTS Variable Editor",
			     JOptionPane.OK_CANCEL_OPTION,
			     JOptionPane.PLAIN_MESSAGE);
			if (status == JOptionPane.OK_OPTION) {
			    // modify row
			    table.setValueAt(varTextField.getText(), index, 0);
			    ind = typeComboBox.getSelectedIndex();
			    table.setValueAt(typeStrings[ind], index, 1);
			    table.setValueAt(valueTextField.getText(),
					     index, 2);
			    ind = unitComboBox.getSelectedIndex();
			    table.setValueAt(unitStrings[ind], index, 3);
			    index++;
			    if (index >= table.getRowCount()) {
				DefaultTableModel tm =
				    (DefaultTableModel)table.getModel();
				tm.setRowCount(index+8);
			    }
			    table.setRowSelectionInterval(index, index);
			}
		    }
		});
	    
	    ourPanel.add(varButton);
	}

	JButton clearButton = new
	    JButton(localeString("clearSelectionButton"));
	clearButton.addActionListener((e) -> {
		CellEditor ce = table.getCellEditor();
		if (ce != null) {
		    ce.stopCellEditing();
		}
		int ncols = table.getColumnCount();
		for (int i: table.getSelectedRows()) {
		    for (int j = 0; j < ncols; j++) {
			table.setValueAt(null, i, j);
		    }
		}
	    });
	ourPanel.add(clearButton);
	JButton compactButton = new JButton(localeString("compactTableButton"));
	compactButton.addActionListener((e) -> {
		CellEditor ce = table.getCellEditor();
		if (ce != null) {
		    ce.stopCellEditing();
		}

		int ncols = table.getColumnCount();
		int nrows = table.getRowCount();
		int offset =  0;
		int index = 0;
		while(offset < nrows) {
		    String v = (String)table.getValueAt(offset, 0);
		    if (offset > index) {
			if (v != null && v.trim().length() > 0) {
			    // copy to index.
			    for (int j = 0; j < ncols; j++) {
				table.setValueAt(table.getValueAt(offset, j),
						 index, j);
				table.setValueAt(null, offset, j);
			    }
			    index++;
			}
			offset++;
		    } else if (v != null && v.trim().length() > 0) {
			index++;
			offset++;
		    } else {
			offset++;
		    }
		}
	    });
	ourPanel.add(compactButton);


	pane.add(ourPanel, "North");
    }

    // returns the colum width
    private static int configColumn(JTable table, int col, String example) {
	TableCellRenderer tcr = table.getDefaultRenderer(String.class);
	int w;
	if (tcr instanceof DefaultTableCellRenderer) {
	    DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer)tcr;
	    FontMetrics fm = dtcr.getFontMetrics(dtcr.getFont());
	    w = 10 + fm.stringWidth(example);
	} else {
	    w = 10 + 12 * example.length();
	}
	TableColumnModel cmodel = table.getColumnModel();
	TableColumn column = cmodel.getColumn(col);
	int ipw = column.getPreferredWidth();
	if (ipw > w) {
	    w = ipw; 
	}
	column.setPreferredWidth(w);
	if (col == 1) {
	    column.setMinWidth(w);
	} else 	if (col == 3) {
	    column.setMinWidth(15+w);
	}
	return w;
    }

    private static void configTable(JTable table) {
	table.setColumnSelectionAllowed(false);
	table.setRowSelectionAllowed(true);
	table.setColumnSelectionAllowed(false);
	table.setRowSelectionInterval(0,0);
    }

    static String tableErrorMsg = null;
    private static boolean hasTableError(JTable vtable) {
	int nrows = vtable.getRowCount();
	for (int i = 0; i < nrows; i++) {
	    String name = (String)vtable.getValueAt(i, 0);
	    if (name == null || name.trim().length() == 0) continue;
	    String type = (String)vtable.getValueAt(i, 1);
	    if (type == null) {
		type = typeStrings[0];
		vtable.setValueAt(type, i, 1);
	    }
	    String value =  (String)vtable.getValueAt(i, 2);
	    if (value == null) value = "";
	    String units = (String)vtable.getValueAt(i, 3);
	    if (units == null) {
		units = unitStrings[0];
		vtable.setValueAt(units, i, 3);
	    }
	    if ((type.equals(typeStrings[1]) || type.equals(typeStrings[4]))
		&& !units.equals(unitStrings[0])) {
		vtable.setValueAt(unitStrings[0], i, 3);
	    }
	    if ((type.equals(typeStrings[2]) || type.equals(typeStrings[3]))
		&& units.equals(unitStrings[0])) {
		vtable.setValueAt(unitStrings[1], i, 3);
	    }
	    
	    if (type.equals(typeStrings[0])) {
		// Not Used
	    } else if (type.equals(typeStrings[1])) {
		// Boolean
		if (value.trim().equalsIgnoreCase("true")) {
		    vtable.setValueAt("true", i, 2);
		} else if (value.trim().equalsIgnoreCase("false")) {
		    vtable.setValueAt("false", i, 2);
		} else {
		    tableErrorMsg = "notBooleanVariable";
		    return true;
		}
	    } else if (type.equals(typeStrings[2])) {
		// Integer
		try {
		    Long.parseLong(value);
		} catch (NumberFormatException e) {
		    tableErrorMsg = "notIntegerVariable";
		    return true;
		}
	    } else if (type.equals(typeStrings[3])) {
		// Real
		try {
		    tableErrorMsg = "notRealVariable";
		    Double.parseDouble(value);
		} catch (NumberFormatException e) {
		    return true;
		}
	    } else if (type.equals(typeStrings[4])) {
		// String
	    } else {
		tableErrorMsg = "unknownVarType";
		return true;
	    }
	}
	return false;
    }

    private static boolean atLeastOneFile(JTextField tf, JTable stable) {
	String text = tf.getText();
	if (text != null && text.trim().length() != 0) {
	    return true;
	}
	int n = stable.getRowCount();
	for (int i = 0; i < n; i++) {
	    Object s = stable.getValueAt(i, 0);
	    if (s != null) {
		String string = (String)s;
		if (string.trim().length() > 0) return true;
	    }
	}
	tableErrorMsg = "AtLeastOneFileNeeded";
	return false;
    }


    private static String getZDFString(ZipDocFile f, String name) {
	try {
	    InputStream is = f.getInputStream(f.getEntry(name));
	    XMLDecoder d = new XMLDecoder(is);
	    String result  = (String) d.readObject();
	    d.close();
	    is.close();
	    return result;
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    System.exit(1);
	}
	return null;
    }

    private static int getZDFInteger(ZipDocFile f, String name) {
	try {
	    InputStream is = f.getInputStream(f.getEntry(name));
	    XMLDecoder d = new XMLDecoder(is);
	    Integer result  = (Integer) d.readObject();
	    d.close();
	    is.close();
	    return result;
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    System.exit(1);
	    return (-1);
	}
    }

    // private static class OVect extends Vector{}

    @SuppressWarnings("unchecked")
    private static DefaultTableModel getZDFTM(ZipDocFile f, String name,
					      Object[] columnNames) 
    {
	try {
	    InputStream is = f.getInputStream(f.getEntry(name));
	    XMLDecoder d = new XMLDecoder(is);
	    Vector<Object> cnv = new Vector<Object>(columnNames.length);
	    for (Object s: columnNames) {
		cnv.add(s);
	    }
	    Vector v = (Vector)(d.readObject());
	    if (name.equals("variables")) {
		int vsize = v.size();
		for (int i = 0; i < vsize; i++) {
		    Vector vv = (Vector)(v.elementAt(i));
		    Integer ind = (Integer)(vv.elementAt(1));
		    vv.setElementAt(((ind == -1)? null: typeStrings[ind]), 1);
		    ind = (Integer)( vv.elementAt(3));
		    vv.setElementAt(((ind == -1)? null: unitStrings[ind]), 3);
		}
	    }
	    DefaultTableModel result
		= new DefaultTableModel(v, cnv) {
			public Class<?> getColumnClass(int col) {
			    return String.class;
			}
		    };
	    d.close();
	    is.close();
	    return result;
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    System.exit(1);
	}
	return null;
    }

    static Object[] options = {
	localeString("SaveAccept"),
	localeString("SaveOnly"),
	localeString("Accept"),
	localeString("Cancel")
    };
    private static JButton[] buttons = new JButton[options.length];

    private static boolean dialogButtonPushed = false;

    private static JPanel createButtons(final JTable javaOptionTable,
					final JTable codebaseTable,
					final JTable scriptTable,
					final JTable varTable)
    {
	final JPanel panel = new JPanel();
	panel.setLayout(new FlowLayout());
	for (int i = 0; i < options.length; i++) {
	    final int index = i;
	    JButton b = new JButton((String)options[i]);
	    b.addActionListener((e) -> {
		    CellEditor ce = javaOptionTable.getCellEditor();
		    if (ce != null) {
			ce.stopCellEditing();
		    }
		    ce = codebaseTable.getCellEditor();
		    if (ce != null) {
			ce.stopCellEditing();
		    }
		    ce = scriptTable.getCellEditor();
		    if (ce != null) {
			ce.stopCellEditing();
		    }		    
		    ce = varTable.getCellEditor();
		    if (ce != null) {
			ce.stopCellEditing();
		    }		    
		    status = index;
		    Window w = SwingUtilities.windowForComponent(panel);
		    if (w instanceof JDialog) {
			JDialog d = (JDialog)w;
			dialogButtonPushed = true;
			d.setVisible(false);
			d.dispose();
		    }
		});
	    panel.add(b);
	    buttons[i] = b;
	    if (i < 2) b.setEnabled(false);
	}
	return panel;
    }
    
    private static boolean disabled = true;
    private static void enableButtons() {
	if (disabled) {
	    buttons[0].setEnabled(true);
	    buttons[1].setEnabled(true);
	}
	disabled = false;
    }

    private static TableModelListener tml = (e) -> {
	enableButtons();
    };

    private static DocumentListener dl = new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) {
		enableButtons();
	    }
	    public void insertUpdate(DocumentEvent e) {
		enableButtons();
	    }
	    public void removeUpdate(DocumentEvent e) {
		enableButtons();
	    }
	};

    private static String[] scriptingLanguages;
    private static String DEFAULT_LANG = "(DEFAULT)";
    private static String defaultSL = "(" + localeString("defaultSL") + ")";
    static {
	Set<String> langSet = Scripting.getLanguageNameSet();
	scriptingLanguages = new String[langSet.size()+1];
	langSet = new TreeSet<String>(langSet);
	langSet.add(defaultSL);
	scriptingLanguages = langSet.toArray(scriptingLanguages);
    }

    /*
    private static boolean isImageFile(String path) {
	if (path == null) return false;
	path = path.trim();
	if (path.length() == 0) return false;
	int i = path.lastIndexOf('.');
	if (i == -1) return false;
	path = path.substring(i+1).toLowerCase();
	for (String suffix: ImageIO.getReaderFileSuffixes()) {
	    suffix = suffix.toLowerCase();
	    System.out.println("... trying " + suffix + ", ext = " + path);
	    if (path.equals(suffix)) return true;
	}
	return false;
    }
    */
    private static int status = 0;
    private static boolean usesImageFile = false;
    private static String[] results = null;

    @SuppressWarnings("unchecked")
    private static void fixupVector(Vector v3) {
	int vsize = v3.size();
	for (int i = 0; i < vsize; i++) {
	    Vector v = (Vector)(((Vector)(v3.elementAt(i))).clone());
	    v.setElementAt(getTypeIndex(v.elementAt(1)), 1);
	    v.setElementAt(getUnitIndex(v.elementAt(3)), 3);
	    v3.setElementAt(v, i);
	}
    }

    public static String[] getSetupArgs(ZipDocFile zf, File zfile) {
	try {
	    results = null;
	    SwingUtilities.invokeAndWait(() -> {
		    JTabbedPane tabpane = new JTabbedPane();
		    JPanel basicPanel = new JPanel();
		    basicPanel.setLayout(new BorderLayout());
		    JLabel inputFileLabel =
			new JLabel(localeString("inputFileLabel"));
		    JTextField inputFileTF = new JTextField(32);
		    inputFileTF.addFocusListener(new FocusAdapter() {
			    public void focusLost(FocusEvent e) {
				usesImageFile =
				    EPTS.isImagePath(inputFileTF.getText());
			    }
			});
		    JLabel animLabel =
			new JLabel(localeString("animLabel"));
		    JTextField animTF = new JTextField("a2d", 32);
		    JLabel slLabel = new JLabel(localeString("slLabel"));
		    JComboBox<String> langCB =
			new JComboBox<String>(scriptingLanguages);
		    langCB.setEditable(false);

		    String jcolumnNames[] = {
			localeString("JavaOptions")
		    };
		    TableModel javaOptionsTM;
		    if (zf != null) {
			inputFileTF.setText(getZDFString(zf, "inputfile"));
			usesImageFile = EPTS.isImagePath(inputFileTF.getText());
			animTF.setText(getZDFString(zf, "animation"));
			String lang = getZDFString(zf, "scriptingLang");
			if (lang.equals(DEFAULT_LANG)) {
			    lang = defaultSL;
			}
			langCB.setSelectedItem(lang);
			javaOptionsTM = getZDFTM(zf, "joptions", jcolumnNames);
		    } else {
			langCB.setSelectedIndex(0);
			javaOptionsTM =
			    new DefaultTableModel(jcolumnNames, 32) {
				public Class<?> getColumnClass(int col) {
				    return String.class;
				}
			    };
		    }
		    JTable javaOptionTable = new JTable();
		    javaOptionTable.setModel(javaOptionsTM);
		    JScrollPane javaOptionsScrollPane =
			new JScrollPane(javaOptionTable);
		    javaOptionTable.setFillsViewportHeight(true);
		    configTable(javaOptionTable);

		    JPanel subPanel = new JPanel();
		    GridBagLayout gridbag = new GridBagLayout();
		    subPanel.setLayout(gridbag);
		    GridBagConstraints c = new GridBagConstraints();
		    c.insets = new Insets(4, 8, 4, 8);
		    c.anchor  = GridBagConstraints.LINE_END;
		    c.gridwidth = 1;
		    addComponent(subPanel, inputFileLabel, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(subPanel, inputFileTF, gridbag, c);
		    c.anchor  = GridBagConstraints.LINE_END;
		    c.gridwidth = 1;
		    addComponent(subPanel, animLabel, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(subPanel, animTF, gridbag, c);
		    c.anchor  = GridBagConstraints.LINE_END;
		    c.gridwidth = 1;
		    addComponent(subPanel, slLabel, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(subPanel, langCB, gridbag, c);
		    c.anchor = GridBagConstraints.CENTER;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(subPanel, javaOptionsScrollPane,
				 gridbag, c);

		    addControls(basicPanel, inputFileTF, animTF);
		    basicPanel.add(subPanel, "Center");
		    tabpane.addTab(localeString("Basic"), basicPanel);

		    JPanel codebasePanel = new JPanel();
		    codebasePanel.setLayout(new BorderLayout());

		    Object colnames[] = {
			localeString("codebaseHeader")
		    };
		    TableModel codebaseModel = (zf != null)?
			getZDFTM(zf, "codebase", colnames):
			new DefaultTableModel(colnames, 32) {
			    public Class<?> getColumnClass(int col) {
				return String.class;
			    }
			};
		    JTable codebaseTable = new JTable();
		    codebaseTable.setModel(codebaseModel);
		    JScrollPane codebaseScrollPane =
			new JScrollPane(codebaseTable);
		    codebaseTable.setFillsViewportHeight(true);
		    configTable(codebaseTable);
		    codebasePanel.add(codebaseScrollPane);
		    addControls(codebasePanel, codebaseTable,
				ControlType.JARFILE);
		    tabpane.addTab(localeString("Codebase"), codebasePanel);

		    JPanel scriptPanel = new JPanel();
		    scriptPanel.setLayout(new BorderLayout());
		    Object colnames2[] = {
			localeString("scriptHeader")
		    };
		    TableModel scriptModel = (zf != null)?
			getZDFTM(zf, "scripts", colnames2):
			new DefaultTableModel(colnames2, 32) {
			    public Class<?> getColumnClass(int col) {
				return String.class;
			    }
			};
		    JTable scriptTable = new JTable();
		    scriptTable.setModel(scriptModel);

		    JScrollPane scriptScrollPane =
			new JScrollPane(scriptTable);
		    scriptTable.setFillsViewportHeight(true);
		    configTable(scriptTable);
		    scriptPanel.add(scriptScrollPane, "Center");
		    addControls(scriptPanel, scriptTable,
				ControlType.SCRIPTFILE);

		    tabpane.addTab(localeString("Scripts"), scriptPanel);

		    JPanel varPanel = new JPanel();
		    varPanel.setLayout(new BorderLayout());
		    Object colnames3[] =  {
			localeString("Variable"),
			localeString("Type"),
			localeString("Value"),
			localeString("Units")
		    };
		    TableModel varModel = (zf != null)?
			getZDFTM(zf, "variables", colnames3):
			new DefaultTableModel(colnames3, 32) {
			    public Class<?> getColumnClass(int col) {
				return String.class;
			    }
			};
		    JTable varTable = new JTable();
		    varTable.setModel(varModel);
		    int vtWidth = configColumn(varTable, 0,
					       "mmmmmmmmmmmmm");
		    vtWidth += configColumn(varTable, 1, 
					    typeStrings[4]);
		    vtWidth += configColumn(varTable, 2,
					    "mmmmmmmmmmmmmmmmmmmmmmm");
		    vtWidth += configColumn(varTable, 3,
					    unitStrings[longestUnitIndex]);
		    varTable.getColumnModel().getColumn(1)
			.setCellEditor(new DefaultCellEditor
				       (new JComboBox<String>(typeStrings)));
		    varTable.getColumnModel().getColumn(3)
			.setCellEditor(new DefaultCellEditor
				       (new JComboBox<String>(unitStrings)));
		    
		    JScrollPane varScrollPane =
			new JScrollPane(varTable);
		    varScrollPane.setPreferredSize(new Dimension(20+vtWidth,
								 400));
		    varTable.setFillsViewportHeight(true);
		    configTable(varTable);
		    varPanel.add(varScrollPane, "Center");
		    addControls(varPanel, varTable,
				ControlType.VALUE);
		    varEditor = createVarEditor(varTable);
		    tabpane.addTab(localeString("Variables"), varPanel);
		    // tabpane.setPreferredSize(new Dimension(700, 500));
		    inputFileTF.getDocument().addDocumentListener(dl);
		    animTF.getDocument().addDocumentListener(dl);
		    codebaseModel.addTableModelListener(tml);
		    scriptModel.addTableModelListener(tml);
		    varModel.addTableModelListener(tml);
		    
		    JPanel topPanel = new JPanel();
		    topPanel.setLayout(new BorderLayout());
		    topPanel.add(tabpane, "Center");
		    topPanel.add(createButtons(javaOptionTable,
					       codebaseTable,
					       scriptTable,
					       varTable),
				 "South");
		    JDialog dialog = new JDialog();
		    dialog.setModalityType
			(Dialog.ModalityType.APPLICATION_MODAL);
		    dialog.setTitle("EPTS Setup");
		    dialog.setContentPane(topPanel);
		    dialog.pack();
		    dialog.setVisible(false);
		    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		    dialog.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent we) {
				if (dialogButtonPushed == false) {
				    System.exit(0);
				}
			    }
			});

		    int option = 0;
		    for(;;) {
			/*
			  status = JOptionPane.showOptionDialog
			  (null, tabpane, "EPTS Setup",
			  JOptionPane.DEFAULT_OPTION,
			  JOptionPane.PLAIN_MESSAGE, null,
			  options, option);
			*/
			dialogButtonPushed = false;
			dialog.setVisible(true);
			if (status == 0 || status == 2) {
			    CellEditor ce = javaOptionTable.getCellEditor();
			    if (ce != null) {
				ce.stopCellEditing();
			    }
			    ce = codebaseTable.getCellEditor();
			    if (ce != null) {
				ce.stopCellEditing();
			    }
			    ce = scriptTable.getCellEditor();
			    if (ce != null) {
				ce.stopCellEditing();
			    }
			    ce = varTable.getCellEditor();
			    if (ce != null) {
				ce.stopCellEditing();
			    }
			    if (hasTableError(varTable) ||
				!atLeastOneFile(inputFileTF, scriptTable)) {
				JOptionPane.showMessageDialog
				    (tabpane, localeString(tableErrorMsg),
				     localeString("SetupError"),
				     JOptionPane.ERROR_MESSAGE);
				tableErrorMsg = null;
			    } else {
				ArrayList<String> arglist =
				    new ArrayList<>(64);
				arglist.add("--gui");
				int len = javaOptionTable.getRowCount();
				for (int i = 0; i < len; i++) {
				    String s = (String)
					javaOptionTable.getValueAt(i, 0);
				    if (s != null) {
					s = s.trim();
					if (s.length() > 0) {
					    arglist.add("-J" + s);
					}
				    }
				}
				len = codebaseTable.getRowCount();
				for (int i = 0; i < len; i++) {
				    String s = (String)
					codebaseTable.getValueAt(i, 0);
				    if (s != null) {
					s = s.trim();
					if (s.length() > 0) {
					    arglist.add("--codebase");
					    arglist.add(s);
					}
				    }
				}
				if (usesImageFile == false) {
				    String text = animTF.getText();
				    if (text == null) text = "a2d";
				    text = text.trim();
				    if (text.length() == 0) {
					text = "a2d";
				    }
				    arglist.add("--animation");
				    arglist.add(text);
				    len = varTable.getRowCount();
				    for (int i = 0; i < len; i++) {
					String v = (String)
					    varTable.getValueAt(i, 0);
					if (v != null) {
					    v = v.trim();
					    if (v.length() == 0) {
						continue;
					    }
					} else {
					    continue;
					}
					String t = (String)
					    varTable.getValueAt(i, 1);
					if (t.equals(typeStrings[0])) {
					    continue;
					} else if (t.equals(typeStrings[1])) {
					    t = "--boolean";
					} else if (t.equals(typeStrings[2])) {
					    t = "--int";
					} else if (t.equals(typeStrings[3])) {
					    t = "--double";
					} else if (t.equals(typeStrings[4])) {
					    t = "--string";
					}

					String val = (String)
					    varTable.getValueAt(i, 2);
					String u = (String)
					    varTable .getValueAt(i, 3);
					if (u.equals(unitStrings[0])) {
					    u = "";
					} else if (u.equals(unitStrings[1])) {
					    u = "";
					} else if (u.equals(unitStrings[2])) {
					    u = "nm";
					} else if (u.equals(unitStrings[3])) {
					    u = "um";
					} else if (u.equals(unitStrings[4])) {
					    u = "mm";
					} else if (u.equals(unitStrings[5])) {
					    u = "cm";
					} else if (u.equals(unitStrings[6])) {
					    u = "m";
					} else if (u.equals(unitStrings[7])) {
					    u = "km";
					} else if (u.equals(unitStrings[8])) {
					    u = "in";
					} else if (u.equals(unitStrings[9])) {
					    u = "ft";
					} else if (u.equals(unitStrings[10])) {
					    u = "ud";
					} else if (u.equals(unitStrings[11])) {
					    u = "mi";
					}
					arglist.add(t);
					arglist.add(v+"="+val+u);
				    }
				}
				arglist.add("--");
				String text = inputFileTF.getText();
				if (text != null) {
				    text = text.trim();
				    if (text.length() > 0) {
					arglist.add(text);
				    }
				}
				if (!usesImageFile) {
				    len = scriptTable.getRowCount();
				    for (int i = 0; i < len; i++) {
					String s = (String)
					    scriptTable.getValueAt(i, 0);
					if (s == null) continue;
					s = s.trim();
					if (s.length() == 0) continue;
					arglist.add(s);
				    }
				}
				results = new String[arglist.size()];
				results = arglist.toArray(results);
				break;
			    }
			} else {
			    break;
			}
		    }
		    if (status == 0 || status == 1) {
			String cd = System.getProperty("user.dir");
			JFileChooser saveChooser = new JFileChooser(cd);
			FileNameExtensionFilter fne =
			    new FileNameExtensionFilter
			    ("EPTS Saved Setup", "eptc");
			for(;;) {
			    int retval = (zfile != null)?
				JFileChooser.APPROVE_OPTION:
				saveChooser.showDialog(null, "Save EPTS Setup");
			    if (retval == JFileChooser.APPROVE_OPTION) {
				try {
				    File f = (zfile != null)? zfile:
					saveChooser.getSelectedFile();
				    if (!f.getName().endsWith(".eptc")) {
					JOptionPane.showMessageDialog
					    (tabpane,
					     errorMsg("eptc", f.getName()),
					     "Setup Error",
					     JOptionPane.ERROR_MESSAGE);
					continue;
				    }
				    OutputStream fos = new FileOutputStream(f);
				    ZipDocWriter zdw = new
					ZipDocWriter(fos,
						     "application/vnd.bzdev"
						     +".epts-config+zip");
				    OutputStream os =
					zdw.nextOutputStream("inputfile",
							     false, 0);
				    XMLEncoder enc = new XMLEncoder(os);
				    enc.writeObject(inputFileTF.getText());
				    enc.close(); os.close();
				    os =
					zdw.nextOutputStream("animation",
							     false, 0);
				    enc = new XMLEncoder(os);
				    enc.writeObject(animTF.getText());
				    enc.close(); os.close();

				    os =
					zdw.nextOutputStream("scriptingLang",
							     false, 0);
				    enc = new XMLEncoder(os);
				    int ilang = langCB.getSelectedIndex();
				    String lang = (ilang == 0)?
					DEFAULT_LANG:
					langCB.getItemAt(ilang);
				    enc.writeObject(lang);
				    enc.close(); os.close();

				    os = zdw.nextOutputStream("joptions",
							      true, 9);
				    enc = new XMLEncoder(os);
				    Vector v0 = ((DefaultTableModel)
						 javaOptionTable.getModel())
					.getDataVector();
				    enc.writeObject(v0);
				    enc.close();
				    os.close();
				    os = zdw.nextOutputStream("codebase",
							      true, 9);
				    enc = new XMLEncoder(os);
				    Vector v1 = ((DefaultTableModel)
						 codebaseTable.getModel())
					.getDataVector();
				    enc.writeObject(v1);
				    enc.close();
				    os.close();

				    Vector v2 = ((DefaultTableModel)
						 scriptTable.getModel())
					.getDataVector();
				    os = zdw.nextOutputStream("scripts",
							      true, 9);
				    enc = new XMLEncoder(os);
				    enc.writeObject(v2);
				    enc.close();
				    os.close();
				    Vector v3 = (Vector)
					(((Vector)(((DefaultTableModel)
						    varTable.getModel())
						   .getDataVector()))
					 .clone());
				    fixupVector(v3);
				    /*
				    int vsize = v3.size();
				    for (int i = 0; i < vsize; i++) {
					Vector v = (Vector)
					    (((Vector)(v3.elementAt(i)))
					     .clone());
					v.setElementAt
					    (getTypeIndex(v.elementAt(1)), 1);
					v.setElementAt
					    (getUnitIndex(v.elementAt(3)), 3);
					v3.setElementAt(v, i);
				    }
				    */
				    os = zdw.nextOutputStream("variables",
							      true, 9);
				    enc = new XMLEncoder(os);
				    enc.writeObject(v3);
				    enc.close();
				    os.close();
				    zdw.close();
				} catch (Exception ee) {
				    JOptionPane.showMessageDialog
					(tabpane,ee.getMessage(),
					 "Setup Error",
					 JOptionPane.ERROR_MESSAGE);
				    continue;
				}
			    }
			    break;
			}
		    }
		    if (status == 1|| status == 3) {
			System.exit(0);
		    }
		});
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
	return results;
    }
    }
