package org.bzdev.epts;

import java.awt.*;
import java.awt.event.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import javax.swing.*;
import javax.swing.colorchooser.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.xml.parsers.*;

import org.bzdev.graphs.Colors;
import org.bzdev.io.*;
import org.bzdev.swing.*;
import org.bzdev.swing.text.CharDocFilter;

import org.xml.sax.helpers.*;
import org.xml.sax.*;

public class TemplateSetup {

    // true if the first pane is being edited
    static boolean initialEditing = true;

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    private static CharDocFilter cfilter = new CharDocFilter();
    static {
	cfilter.setAllowedChars("--..09eeEE++");
    }

    private static CharDocFilter icfilter = new CharDocFilter();
    static {
	icfilter.setAllowedChars("--09");
    }
    private static DocumentFilter dpfilter = new DocumentFilter() {
	    @Override
	    public void insertString(DocumentFilter.FilterBypass fb,
				     int offs,
				     String str,
				     AttributeSet a)
		throws BadLocationException 
	    {
		if (offs == 0 && !str.startsWith("-")) {
		    Toolkit.getDefaultToolkit().beep();
		} else if (!str.matches("[- \u2423]*")) {
		    Toolkit.getDefaultToolkit().beep();
		} else {
		    str = str.replace(' ', '\u2423');
		    super.insertString(fb, offs, str, a);
		}
	    }
	    @Override
	    public void replace(DocumentFilter.FilterBypass fb,
				int offs, int length,
				String str,
				AttributeSet a)
		throws BadLocationException 
	    {
		if (offs == 0 && !str.startsWith("-")) {
		    Toolkit.getDefaultToolkit().beep();
		} else if (!str.matches("[- \u2423]*")) {
		    Toolkit.getDefaultToolkit().beep();
		} else {
		    str = str.replace(' ', '\u2423');
		    super.replace(fb, offs, length, str, a);
		}
	    }
	    @Override
	    public void remove(DocumentFilter.FilterBypass fb,
			       int offs, int len)
		throws BadLocationException 
	    {
		if (offs == 0) {
		    Document doc = fb.getDocument();
		    int doclen = doc.getLength();
		    if (len == 0) {
			return;	// nothing to do
		    } else if (doclen <= len) {
			super.remove(fb, offs, len);
		    } else {
			if (doc.getText(len,1).equals("-")) {
			    super.remove(fb, offs, len);
			} else {
			    Toolkit.getDefaultToolkit().beep();
			}
		    }
		} else {
		    super.remove(fb, offs, len);
		}
	    }
	};

        

    private static InputVerifier iv = new InputVerifier() {
	    public boolean verify(JComponent c) {
		if (c instanceof VTextField) {
		    VTextField tf = (VTextField)c;
		    String text = tf.getText();
		    try {
			double val = Double.parseDouble(text);
			if (val < 0.0) return false;
			return true;
		    } catch (Exception e) {
			return false;
		    }
		}
		return false;
	    }
	};

    private static InputVerifier iiv = new InputVerifier() {
	    public boolean verify(JComponent c) {
		if (c instanceof VTextField) {
		    VTextField tf = (VTextField)c;
		    String text = tf.getText();
		    try {
			int val = Integer.parseInt(text);
			return true;
		    } catch (Exception e) {
			return false;
		    }
		}
		return false;
	    }
	};

    private static InputVerifier liv = new InputVerifier() {
	    public boolean verify(JComponent c) {
		if (c instanceof VTextField) {
		    VTextField tf = (VTextField)c;
		    String text = tf.getText();
		    try {
			long val = Long.parseLong(text);
			return true;
		    } catch (Exception e) {
			return false;
		    }
		}
		return false;
	    }
	};

    private static String[] templateTypes = {
	localeString("SVG"),
	localeString("TableTemplate"),
	localeString("PathIteratorTemplate"),
	localeString("SVGmm")
    };

    public static enum TType {
	SVG, TT, PIT, SVG_MM
    }

    static TType templateType = null;
    

    public static class BasicData {
	public int templateTypeInd = -1;
	public boolean useBuiltins = false;
	public String template = null;
	public String savedState = null;
	public String mapName = null;
    }

    private static BasicData basicData = new BasicData();


    private static String strokeCaps[] = {
 	"butt",
	"round",
	"square"
    };

    private static  String strokeCaps2[] = new String[strokeCaps.length];

    private static String strokeJoins[] = {
	"bevel",
	"miter",
	"round"
    };

    private static String strokeJoins2[] = new String[strokeJoins.length];

    private static String windingRules[] = {
	"evenodd", "nonzero"
    };

    private static String[] windingRules2 = new String[windingRules.length];

    static {
	for (int i = 0; i < strokeCaps.length; i++) {
	    strokeCaps2[i] = localeString(strokeCaps[i]);
	}
	for (int i = 0; i < strokeJoins.length; i++) {
	    strokeJoins2[i] = localeString(strokeJoins[i]);
	}

	for (int i = 0; i < windingRules2.length; i++) {
	    windingRules2[i] = localeString(windingRules[i]);
	}

    }


    public static class SBPair {
	public String key;
	public boolean value;
    }

    static Object tdefCols[] = {
	localeString("TestDirective"),
	localeString("Directive"),
	localeString("DirectiveValue")
    };


    static Object defCols[] = {
	localeString("iterationName"),
	localeString("varName"),
	localeString("valueName")
    };

    private static String templatesWithClassOrPackage[] = {
	"JavaLayers", "JavaLocations",
	"JavaPathBuilders", "JavaPathFactories", "HTMLImageMap"
    };
    
    private static final String RESOURCE_PROTO = "resource:";
    private static final int RESOURCE_PROTO_LEN = RESOURCE_PROTO.length();

    private static boolean mayUseClassOrPackage(String template) {
	if (template.length() < RESOURCE_PROTO_LEN) return true;
	if  (!template.startsWith(RESOURCE_PROTO)) {
	    return true;
	}
	template = template.substring(RESOURCE_PROTO_LEN);
	for (String s: templatesWithClassOrPackage) {
	    if (s.equals(template)) {
		return true;
	    }
	}
	return false;
    }

    private static int status = 0;
    private static String[] results = null;
    private static boolean dialogButtonPushed = false;

    private static void addComponent(JPanel panel, JComponent component,
				     GridBagLayout gridbag,
				     GridBagConstraints c)
    {
	gridbag.setConstraints(component, c);
	panel.add(component);
    }

    static File cdir = new File(System.getProperty("user.dir"));
    // base directory for relative file names.

    static Path base = null;

    private static String chooseTemplateOptions1[] = {
	"ECMAScriptLayers",
	"ECMAScriptLocations",
	"ECMAScriptPaths",
	"ECMAScript",
	"JavaLayers",
	"JavaLocations",
	"JavaPathBuilders",
	"JavaPathFactories",
	"YAMLLayers",
	"YAMLLocations",
	"YAMLPaths",
	"YAML",
	"HTMLImageMap"
    };

    private static String chooseTemplateOptions2[] = {
	"area", "circumference", "pathlength", "SegmentsCSV"
    };

    // Following so the GUI will be localized
    private static String[] chooseTemplateOptions1L =
	new String[chooseTemplateOptions1.length];

    private static String[] chooseTemplateOptions2L =
	new String[chooseTemplateOptions2.length];

    private static Map<String,String> chooseTemplateOptions1Map =
	new HashMap<>();
    private static Map<String,String> chooseTemplateOptions2Map =
	new HashMap<>();

    static {
	for (int i = 0; i < chooseTemplateOptions1.length; i++) {
	    chooseTemplateOptions1L[i] =
		localeString(chooseTemplateOptions1[i]);
	    chooseTemplateOptions1Map.put(chooseTemplateOptions1L[i],
					  chooseTemplateOptions1[i]);
	}
	for (int i = 0; i < chooseTemplateOptions2.length; i++) {
	    chooseTemplateOptions2L[i] =
		localeString(chooseTemplateOptions2[i]);
	    chooseTemplateOptions2Map.put(chooseTemplateOptions2L[i],
					  chooseTemplateOptions2[i]);
	}
    }

    private static boolean templateMatch(String savedState, int index) {
	switch (index) {
	case 0:
	    return savedState.trim().length() == 0;
	case 1:
	    for (String s: chooseTemplateOptions1) {
		if (s.equals(savedState)) {
		    return true;
		}
	    }
	    return false;
	case 2:
	    for (String s: chooseTemplateOptions2) {
		if (s.equals(savedState)) {
		    return true;
		}
	    }
	    return false;
	default:
	    return false;
	}
    }

    private static String chooseTemplate(JPanel pane, boolean builtin) {
	if (builtin) {
	    if (templateType == null) return null;
	    switch (templateType) {
	    case TT:
		String result1 = (String)JOptionPane.showInputDialog
		    (pane, localeString("chooseTemplateMsg"),
		     localeString("chooseTemplateTitle"),
		     JOptionPane.PLAIN_MESSAGE,
		     null, chooseTemplateOptions1L, chooseTemplateOptions1L[3]);
		String tail1 = chooseTemplateOptions1Map.get(result1);
		return "resource:" + tail1;
	    case PIT:
		String result2 = (String)JOptionPane.showInputDialog
		    (pane, localeString("chooseTemplateMsg"),
		     localeString("chooseTemplateTitle"),
		     JOptionPane.PLAIN_MESSAGE,
		     null, chooseTemplateOptions2L, chooseTemplateOptions2L[2]);
		String tail2 = chooseTemplateOptions2Map.get(result2);
		return "resource:" + tail2;
	    default:
		return null;
	    }
	} else {
	    // no restriction on how users might name template files
	    JFileChooser fc = new JFileChooser(cdir);
	    int status = fc.showOpenDialog(pane);
	    if (status == JFileChooser.APPROVE_OPTION) {
		File f = fc.getSelectedFile().getAbsoluteFile();
		File parent = f.getParentFile();
		if (parent != null) {
		    cdir = parent;
		    try {
			System.setProperty("user.dir",
					   parent.getCanonicalPath());
		    } catch (Exception se) {
			// just in case a security manager is
			// installed - if we can't change the working
			// directory, the program will still function.
		    }
		}
	       return f.getAbsolutePath();
	    } else {
		return null;
	    }
	}
    }
    
    private static String chooseSavedStateFile(JPanel pane) {
	String extensions[] = {"epts"};
	String fname = localeString("BasicFTypes");
	JFileChooser fc = new JFileChooser(cdir);
	fc.setAcceptAllFileFilterUsed(false);
	FileNameExtensionFilter filter =
	    new FileNameExtensionFilter(fname, extensions);
	fc.addChoosableFileFilter(filter);
	int status = fc.showOpenDialog(pane);
	if (status == JFileChooser.APPROVE_OPTION) {
	    File f = fc.getSelectedFile().getAbsoluteFile();
	    File parent = f.getParentFile();
	    if (parent != null) {
		cdir = parent;
		try {
		    System.setProperty("user.dir",
				       parent.getCanonicalPath());
		} catch (Exception se) {
		    // just in case a security manager is
		    // installed - if we can't change the working
		    // directory, the program will still function.
		}
	    }
	    return f.getAbsolutePath();
	} else {
	    return null;
	}
    }

    private static String chooseSavedConfigFile(Component pane) {
	String extensions[] = {"eptt"};
	String fname = localeString("BasicTTypes");
	JFileChooser fc = new JFileChooser(cdir);
	fc.setAcceptAllFileFilterUsed(false);
	FileNameExtensionFilter filter =
	    new FileNameExtensionFilter(fname, extensions);
	fc.addChoosableFileFilter(filter);
	if (savedConfigFileName != null) {
	    fc.setSelectedFile(new File(savedConfigFileName));
	}
	int status = fc.showOpenDialog(pane);
	if (status == JFileChooser.APPROVE_OPTION) {
	    File f = fc.getSelectedFile().getAbsoluteFile();
	    File parent = f.getParentFile();
	    if (parent != null) {
		cdir = parent;
		try {
		    System.setProperty("user.dir",
				       parent.getCanonicalPath());
		} catch (Exception se) {
		    // just in case a security manager is
		    // installed - if we can't change the working
		    // directory, the program will still function.
		}
	    }
	    File fparent = f.getParentFile();
	    if (fparent != null) {
		try {
		    Path nbase =fparent.getCanonicalFile().toPath();
		    if (!nbase.equals(base)) {
			base = nbase;
		    }
		} catch (Exception exception) {}
	    }
	    return f.getAbsolutePath();
	} else {
	    return null;
	}
    }

    private static String chooseFile(JPanel pane) {
	JFileChooser fc = new JFileChooser(cdir);
	int status = fc.showOpenDialog(pane);
	if (status == JFileChooser.APPROVE_OPTION) {
	    File f = fc.getSelectedFile().getAbsoluteFile();
	    File parent = f.getParentFile();
	    if (parent != null) {
		cdir = parent;
		try {
		    System.setProperty("user.dir",
				       parent.getCanonicalPath());
		} catch (Exception se) {
		    // just in case a security manager is
		    // installed - if we can't change the working
		    // directory, the program will still function.
		}
	    }
	    return f.getAbsolutePath();
	} else {
	    return null;
	}
    }


    static JLabel templateLabel = null;
    static JTextField templateTF = null;
    static JButton templateButton = null;

    static JCheckBox useBuiltinCheckBox = null;
    static JLabel savedStateLabel = null;
    static JTextField savedStateTF = null;
    static JButton savedStateButton = null;
    static JLabel mapLabel = null;
    static JTextField mapTF = null;
    static JButton mapButton = null;

    static JLabel outFileLabel = null;
    static JTextField outFileTF = null;
    static JButton outFileButton = null;
    static JButton saveButton = null;
    static JButton saveAsButton = null;
    static JButton writeButton = null;
    static JButton exitButton = null;

    private static
	javax.swing.Timer templateTimer = new javax.swing.Timer(2000, (ae) -> {
		mapLabel.setEnabled(true);
	    mapTF.setEnabled(true);
	    mapButton.setEnabled(true);
	});

    static JButton addPathButton = null;
    static JButton delPathButton = null;
    static JTable compoundPathTable = null;
    static JScrollPane compoundPathSP = null;
    static JLabel subpathLabel = null;
    static JTable subpathTable = null;
    static JScrollPane subpathSP = null;
    static JTable pathLocTable = null;
    static Object compoundPathCols[] = {localeString("ExtraPaths")};
    static Object pathlocCols[] = {"\u2714", localeString("LocPaths")};
    static Object subpathCols[] = {localeString("Subpaths")};
    private static void configTable(JTable table, boolean selectFirst) {
	table.setColumnSelectionAllowed(false);
	table.setRowSelectionAllowed(true);
	if (selectFirst) {
	    table.setRowSelectionInterval(0,0);
	}
    }

    private static void setupCompoundPaths() {
	Set<String> kset = pathmap.keySet();
	String[] keys = new  String[kset.size()];
	int ind = 0;
	for (String key: kset) {
	    keys[ind++] = key;
	}
	compoundPathTable.clearSelection();
	DefaultTableModel tm = (DefaultTableModel)compoundPathTable.getModel();
	tm.setRowCount(0);
	ind = 0;
	for (String key: keys) {
	    Object[] data = {key};
	    tm.addRow(data);
	    ind++;
	}
	if (ind < 16) {
	    tm.setRowCount(16);	    
	}
    }

    static JCheckBox drawColorCheckBox = null;
    static JLabel drawColorExample = null;
    static JButton drawColorButton = null;
    static JCheckBox fillColorCheckBox = null;
    static JLabel fillColorExample = null;
    static JButton fillColorButton = null;
    static JLabel capLabel = null;
    static JComboBox<String> capCB = null;
    static JLabel dashIncrLabel = null;
    static VTextField dashIncrTF = null;
    static JLabel dashPatternLabel = null;
    static VTextField dashPatternTF = null;
    static JLabel dashPatternPhaseLabel = null;
    static VTextField dashPatternPhaseTF = null;
    static JLabel strokeJoinLabel = null;
    static JComboBox<String> strokeJoinCB = null;
    static JLabel miterLimitLabel = null;
    static VTextField miterLimitTF = null;
    static JCheckBox strokeGCSCheckBox = null;
    static JLabel strokeWidthLabel = null;
    static VTextField strokeWidthTF = null;
    static JLabel windingRuleLabel = null;
    static JComboBox<String> windingRuleCB = null;
    static JLabel zorderLabel = null;
    static VTextField zorderTF = null;

    private static class ColorChooser {
        JColorChooser cc;
        JDialog dialog;
        Color orig;
        Color result;
        public ColorChooser(Component parent, String title) {
	    String swatchName =
		UIManager.getString("ColorChooser.swatchesNameText",
				    java.util.Locale.getDefault());
	    AbstractColorChooserPanel swatchPanel = null;
	    for (AbstractColorChooserPanel ccp:
		     ColorChooserComponentFactory.getDefaultChooserPanels()) {
		if(ccp.getDisplayName().equals(swatchName)) {
		    swatchPanel = ccp;
		    break;
		}
	    }

            cc = new JColorChooser();
	    boolean hasSwatch = false;
	    for (AbstractColorChooserPanel ccp: cc.getChooserPanels()) {
		if(ccp.getDisplayName().equals(swatchName)) {
		    hasSwatch = true;
		    break;
		}
	    }
	    if (hasSwatch == false && swatchPanel != null) {
		cc.addChooserPanel(swatchPanel);
	    }
            cc.addChooserPanel(new CSSColorChooserPanel());
            dialog = JColorChooser.createDialog
                (parent, title, true, cc,
                 (e) -> {
                    result = cc.getColor();
                 },
                 (e) -> {
                     result = orig;
                 });
        }
        public Color showDialog(Color c) {
            orig = c;
            cc.setColor(orig);
            dialog.setVisible(true);
            return result;
        }
    }
    /*
    private static class ColorChooser {
	static JColorChooser cc;
	static JDialog dialog;
	static Color orig;
	static Color result;
	static {
	    cc = new JColorChooser();
	    cc.addChooserPanel(new CSSColorChooserPanel());
	    dialog = JColorChooser.createDialog
		(dataPanel, localeString("ColorChooser"), true, cc,
		 (e) -> {
		    result = cc.getColor(); 
		 },
		 (e) -> {
		     result = orig;
		 });
	}
	public static Color showDialog(Color c) {
	    orig = c;
	    cc.setColor(orig);
	    dialog.setVisible(true);
	    return result;
	}
    }
    */

    public enum FlatnessMode  {
	NORMAL, STRAIGHT,  ELEVATE,
    }

    public static class GlobalData {
	public boolean usesTableTemplate;
	public String packageName;
	public String className;
	public boolean isPublic;
	public FlatnessMode fmode = FlatnessMode.NORMAL;
	public double flatness;
	public int limit = 10;
	public boolean gcs;
    };
    static GlobalData globalData = new GlobalData();

    static void setGlobalData() {
	globalData.packageName = packageNameTF.getText().trim();
	globalData.className = classNameTF.getText().trim();
	globalData.isPublic = publicClassCheckBox.isSelected();
	/*
	  // handled by action listeners on radio buttons
	globalData.fmode = normalRB.isSelected()? FlatnessMode.NORMAL:
	    (straightRB.isSelected()? FlatnessMode.STRAIGHT:
	     (elevateRB.isSelected()? FlatnessMode.ELEVATE: null));
	*/
	String fs = flatnessTF.getText().trim();
	globalData.flatness = (fs.length() == 0)? 0.0: Double.parseDouble(fs);
	globalData.limit = limitTF.getValue();
	globalData.gcs = gcsCheckBox.isSelected();
    }

    static void restoreFromGlobalData() {
	if (packageNameTF == null) {
	    createGlobalPanel();
	}
	packageNameTF.setText((globalData.packageName == null)? "":
			      globalData.packageName);
	classNameTF.setText((globalData.className == null)? "":
			    globalData.className);
	publicClassCheckBox.setSelected(globalData.isPublic);
	switch(globalData.fmode) {
	case NORMAL:
	    normalRB.setSelected(true);
	    break;
	case STRAIGHT:
	    straightRB.setSelected(true);
	    break;
	case ELEVATE:
	    elevateRB.setSelected(true);
	    break;
	}
	limitTF.setValue(globalData.limit);
	gcsCheckBox.setSelected(globalData.gcs);
    }

    public static class PathLocInfo {
	public boolean isPath;
	public boolean active = false;
	public double strokeWidth = 1.0;
	public boolean strokeGCS = false;
	public boolean draw = false;
	public Color drawColor = Color.BLACK;
	public boolean fill = false;
	public Color fillColor = Color.BLACK;
	public int capIndex = 0;
	public double dashIncr = 10.0;
	public String dashPattern = "-";
	public double dashPhase = 0.0;
	public int joinIndex = 0;
	public double miterLimit = 10.0;
	public int windingRuleInd = 0;
	public long zorder = 0;

	public PathLocInfo() {isPath = false;}


	public PathLocInfo(boolean isPath) {
	    this.isPath = isPath;
	}
    }

    public static class PathLocMap extends TreeMap<String,PathLocInfo> {
	public PathLocMap() {super();}
    }
    static PathLocMap pathLocMap = new PathLocMap();
    static PathLocInfo currentPLI = null;

    static void populateDataPanel() {
	if (currentPLI == null) return;
	if (currentPLI.isPath == false) {
	    return;
	}
	strokeWidthTF.setText("" + currentPLI.strokeWidth);
	strokeGCSCheckBox.setSelected(currentPLI.strokeGCS);
	drawColorCheckBox.setSelected(currentPLI.draw);
	drawColorExample.setBackground(currentPLI.drawColor);
	fillColorCheckBox.setSelected(currentPLI.fill);
	fillColorExample.setBackground(currentPLI.fillColor);
	capCB.setSelectedIndex(currentPLI.capIndex);
	dashIncrTF.setText("" + currentPLI.dashIncr);
	dashPatternTF.setText(currentPLI.dashPattern);
	dashPatternPhaseTF.setText("" + currentPLI.dashPhase);
	strokeJoinCB.setSelectedIndex(currentPLI.joinIndex);
	miterLimitTF.setText("" + currentPLI.miterLimit);
	windingRuleCB.setSelectedIndex(currentPLI.windingRuleInd);
	zorderTF.setText("" + currentPLI.zorder);
    }


    private static CardLayout dataPanelCL = null;
    private static JPanel dataPanel = null; 
    private static JPanel emptyDataPanel = null;
    private static JPanel pathDataPanel = null;

    // use when the text is already validated.
    private static double parseDouble(String text) {
	try {
	    return Double.parseDouble(text);
	} catch (Exception e) {}
	return 0.0;
    }
    // use when the text is already validated.
    private static int parseInteger(String text) {
	try {
	    return Integer.parseInt(text);
	} catch (Exception e) {}
	return 0;
    }

    // use when the text is already validated.
    private static long parseLong(String text) {
	try {
	    return Long.parseLong(text);
	} catch (Exception e) {}
	return 0L;
    }

    static Vector<? extends Vector> tdefVector = null;

    static JTable tdefTable = null;

    /*
    private static final Object rdataColNames[] = {
	localeString("reservedDirective")
    };
    */
    private static final String rootName = localeString("reservedDirective");


    private static void createTreeNodes(DefaultMutableTreeNode node,
					Set<String> names,
					Map<String,Set<String>> map)
    {
	for (String name: names) {
	    DefaultMutableTreeNode next =
		new DefaultMutableTreeNode(name);
	    node.add(next);
	    if (map.containsKey(name)) {
		createTreeNodes(next, map.get(name), map);
	    }
	}
    }

    static void createTDefTable(final JTabbedPane tabpane, JPanel panel) {
	panel.setLayout(new BorderLayout());
	DefaultTableModel tdtm;
	if (tdefVector == null) {
	    tdtm = new DefaultTableModel(tdefCols, 16) {
		    public Class<?> getColumnClass(int col) {
			return String.class;
		    }
		};
	    tdefVector  = tdtm.getDataVector();
	} else {
	    tdtm =
		new DefaultTableModel(tdefVector, new Vector<Object>
				      (Arrays.asList(tdefCols)));
	}
	tdtm.addTableModelListener((tme) -> {
		switch (tme.getType()) {
		case TableModelEvent.DELETE:
		    break;
		case TableModelEvent.INSERT:
		case TableModelEvent.UPDATE:
		    {
			int col = tme.getColumn();
			if (col == 2) break;
			int startRow = tme.getFirstRow();
			int lastRow = tme.getLastRow();
			if (startRow == -1 || lastRow == -1 || col == -1) {
			    return;
			}
			for (int i = startRow; i <= lastRow; i++) {
			    String name = (String)tdtm.getValueAt(i, col);
			    if (name == null) continue;
			    name = name.trim();
			    if (name.length() == 0) continue;
			    if (EPTS.isReservedTdef(name)) {
				tabpane.setSelectedIndex(1);
			    }
			    while (EPTS.isReservedTdef(name)) {
				String msg = errorMsg("tdefNameQ", name, i+1);
				name = JOptionPane
				    .showInputDialog(tabpane,
						     msg,
						     localeString("tdefTitle"),
						     JOptionPane.ERROR_MESSAGE);
				if (name == null) name = "";
				if (!EPTS.isReservedTdef(name)) {
				    tdtm.setValueAt(name, i, col);
				}
			    }
			}
		    }
		}
	    });
	tdefTable = new JTable();
	tdefTable.setModel(tdtm);
	tdefTable.getTableHeader().setReorderingAllowed(false);
	JScrollPane tdefSP = new JScrollPane(tdefTable);
	tdefTable.setFillsViewportHeight(true);
	int twidth = Setup.configColumn(tdefTable, 0, "mmmmmmmmmmm");
	twidth += Setup.configColumn(tdefTable, 1, "mmmmmmmm");
	twidth += Setup.configColumn(tdefTable, 2,
				     "mmmmmmmmmmmmmmmmmmmm");
	tdefSP.setPreferredSize(new Dimension(twidth+10, 275));
	panel.add(tdefSP, BorderLayout.CENTER);

	JPanel top = new JPanel(new FlowLayout());
	JButton addRowButton = new JButton(localeString("addTDefRow"));
	JButton clearRowsButton = new JButton(localeString("clearTDefSel"));
	JButton compactRows = new JButton(localeString("compactRows"));
	addRowButton.addActionListener((ae) -> {
		DefaultTableModel tm = (DefaultTableModel) tdefTable.getModel();
		Vector v = new Vector<Object>();
		tm.addRow(v);
	    });
	clearRowsButton.addActionListener((ae) -> {
		for(int i: tdefTable.getSelectedRows()) {
		    for (int j = 0; j < 3; j++) {
			tdefTable.setValueAt("", i, j);
		    }
		}
	    });
	compactRows.addActionListener((ae) -> {
		DefaultTableModel tm = (DefaultTableModel) tdefTable.getModel();
		int rcount = tm.getRowCount();
		int i = rcount-1;
		while (i >= 0) {
		    Object o0 = tm.getValueAt(i, 0);
		    Object o1 = tm.getValueAt(i, 1);
		    Object o2 = tm.getValueAt(i, 2);
		    String s0 = (o0 == null)? "": (String)o0;
		    String s1 = (o1 == null)? "": (String)o1;
		    String s2 = (o2 == null)? "": (String)o2;
		    s0 = s0.trim();
		    s1 = s1.trim();
		    if (s0.length() == 0 && s1.length() == 0
			&& s2.length() == 0) {
			rcount--;
		    } else {
			break;
		    }
		    i--;
		}
		int target = rcount-1;
		i = 0;
		for (int j = 0; j < rcount; j++) {
		    Object o0 = tm.getValueAt(i, 0);
		    Object o1 = tm.getValueAt(i, 1);
		    Object o2 = tm.getValueAt(i, 2);
		    String s0 = (o0 == null)? "": (String)o0;
		    String s1 = (o1 == null)? "": (String)o1;
		    String s2 = (o2 == null)? "": (String)o2;
		    s0 = s0.trim();
		    s1 = s1.trim();
		    if (s0.length() == 0 && s1.length() == 0
			&& s2.length() == 0) {
			tm.moveRow(i,i,target);
		    } else {
			i++;
		    }
		}
		// if we reset the size too soon, Swing gets
		// confused.
		final int ii = i;
		SwingUtilities.invokeLater(() -> {
			tm.setRowCount((ii < 16)? 16: ii);
		    });
	    });

	top.add(addRowButton);
	top.add(clearRowsButton);

	top.add(compactRows);
	panel.add(top, BorderLayout.PAGE_START);


	Set<String> rdataSet = EPTS.getReservedTDefNames();
	Map<String,Set<String>> map = EPTS.getReservedTDefMap();
	DefaultMutableTreeNode topnode =
	    new DefaultMutableTreeNode(localeString("reservedDirective"));
	createTreeNodes(topnode, rdataSet, map);

	JTree reservedNameTree = new JTree(topnode);

	TreeCellRenderer tcr = reservedNameTree.getCellRenderer();
	DefaultTreeCellRenderer  dtcr =
	    (tcr instanceof DefaultTreeCellRenderer)?
	    (DefaultTreeCellRenderer) tcr: null;
	String example = localeString("longestRTStr");
	if (dtcr != null) {
	    FontMetrics fm = dtcr.getFontMetrics(dtcr.getFont());
	    twidth = fm.stringWidth(example);
	} else {
	    twidth = 12 * example.length();
	}
	twidth += twidth/10;
	// reservedNameTree.setPreferredSize(new Dimension(twidth+10, 275));
	/*
	Object[][] rdata = new Object[rdataSet.size()][1];
	int i = 0;
	for (String name: rdataSet) {
	    rdata[i++][0] = name;
	}
	JTable reservedTable = new JTable(rdata, rdataColNames) {
		public boolean isCellEditable(int row, int col) {
		    return false;
		}
		public Class<?> getColumnClass(int row) {
		    return String.class;
		}
	    };
	reservedTable.setRowSelectionAllowed(false);
	reservedTable.setColumnSelectionAllowed(false);
	*/

	// JScrollPane rtSP = new JScrollPane(reservedTable);
	JScrollPane rtSP = new JScrollPane(reservedNameTree);
	/*
	reservedTable.setFillsViewportHeight(true);
	twidth = Setup.configColumn(reservedTable, 0,
				    localeString("longestRTStr"));
	*/
	rtSP.setPreferredSize(new Dimension(twidth+50,275));
	panel.add(rtSP, BorderLayout.LINE_END);
	
	JPanel bottom = new JPanel(new FlowLayout());
	JLabel tdefInstructions = new JLabel(localeString("tdefInstructions"));
	bottom.add(tdefInstructions);
	panel.add(bottom, BorderLayout.PAGE_END);
	// reservedTable.setBackground(tdefInstructions.getBackground());
	if (dtcr != null) {
	    Color bcolor = tdefInstructions.getBackground();
	    dtcr.setBackground(bcolor);
	    dtcr.setBackgroundNonSelectionColor(bcolor);
	    dtcr.setBackgroundSelectionColor(bcolor);
	    dtcr.setBorderSelectionColor(bcolor);
	    reservedNameTree.setBackground(bcolor);
	}
    }

    // Want compound paths shown in bold to distinguish them from
    // simple paths.
    static class PathLocRenderer extends DefaultTableCellRenderer {
	PathMap pm;
	Font orig;
	Font cached;
	public PathLocRenderer(PathMap pm) {
	    super();
	    this.pm = pm;
	}
	@Override
	public Component getTableCellRendererComponent(JTable table,
						       Object object,
						       boolean isSelected,
						       boolean hasFocus,
						       int row, int column)
	{
	    Set<String>boldSet = pm.keySet();
	    Component c = super.getTableCellRendererComponent(table, object,
							      isSelected,
							      hasFocus,
							      row, column);
	    if (c instanceof JLabel) {
		JLabel label = (JLabel) c;
		if (orig == null) {
		    Font f = label.getFont();
		    orig = f;
		    cached = f.deriveFont(Font.BOLD);
		}
		if(object instanceof String) {
		    String s = (String) object;
		    if (boldSet.contains(s)) {
			label.setFont(cached);
		    } else {
			label.setFont(orig);
		    }
		} else {
		    label.setFont(orig);
		}
	    }
	    return c;
	}
    }

    static final char openspace = '\u2423';

    static void createConfigTable(JPanel panel) {
	panel.setLayout(new BorderLayout());

	dataPanelCL = new CardLayout();
	dataPanel = new JPanel();
	emptyDataPanel = new JPanel();
	pathDataPanel = new JPanel();

	dataPanel.setLayout(dataPanelCL);
	dataPanel.add(emptyDataPanel, "empty");
	dataPanel.add(pathDataPanel, "data");
	dataPanelCL.show(dataPanel, "empty");
	

	TableModel cftm = new DefaultTableModel(pathlocCols, 16) {
		public Class<?> getColumnClass(int col) {
		    return (col == 0)? Boolean.class: String.class;
		}
	    };
	cftm.addTableModelListener((tme) -> {
		String type = "NONE";
		switch(tme.getType()) {
		case TableModelEvent.INSERT:
		    type = "INSERT";
		    break;
		case TableModelEvent.UPDATE:
		    type = "UPDATE";
		    int col = tme.getColumn();
		    if (col == 0) {
			currentPLI.active = (Boolean)
			    cftm.getValueAt(tme.getFirstRow(), col);
		    }
		    break;
		case TableModelEvent.DELETE:
		    type = "DELETE";
		    break;
		}
	    });
	pathLocTable = new JTable() {
		public boolean isCellEditable(int row, int column) {
		    return column == 0;
		}
	    };
	
	pathLocTable.setModel(cftm);
	pathLocTable.getTableHeader().setReorderingAllowed(false);
	JScrollPane pathlocSP = new JScrollPane(pathLocTable);
	pathLocTable.setFillsViewportHeight(true);
	configTable(pathLocTable, false);
	pathLocTable.getSelectionModel().setSelectionMode
	    (ListSelectionModel.SINGLE_SELECTION);
	pathLocTable.getColumnModel().getColumn(0).setPreferredWidth(15);
	pathLocTable.setDefaultRenderer(String.class,
					new PathLocRenderer(pathmap));

	int twidth = 15;
	// twidth = Setup.configColumn(subpathTable, 0," ");
	twidth += Setup.configColumn(pathLocTable, 1,
				     "mmmmmmmmmmmmmmmmmmmm");
	pathlocSP.setPreferredSize (new Dimension(twidth+10, 275));
	panel.add(pathlocSP, BorderLayout.LINE_START);
	panel.add(dataPanel, BorderLayout.LINE_END);

	pathLocTable.getSelectionModel().addListSelectionListener((lse) -> {
		int ind = pathLocTable.getSelectedRow();
		if (ind == -1) {
		    dataPanelCL.show(dataPanel, "empty");
		} else  {
		    String s = (String)pathLocTable.getValueAt(ind, 1);
		    currentPLI = pathLocMap.get(s);
		    pathLocTable.setValueAt(currentPLI.active, ind, 0);
		    if (currentPLI.isPath) {
			populateDataPanel();
			dataPanelCL.show(dataPanel, "data");
		    } else {
			dataPanelCL.show(dataPanel, "empty");
		    }
		}
	    });


	drawColorCheckBox = new JCheckBox("DrawColor");
	drawColorCheckBox.addActionListener((ae) -> {
		currentPLI.draw = drawColorCheckBox.isSelected();
	    });
	drawColorExample = new JLabel("    ");
	drawColorExample.setOpaque(true);
	drawColorButton = new JButton(localeString("ChooseColor"));
	final ColorChooser drawColorChooser = new
	    ColorChooser(dataPanel, localeString("drawColorChooser"));
	drawColorButton.addActionListener((ae) -> {
		currentPLI.drawColor
		    = drawColorChooser.showDialog(currentPLI.drawColor);
		drawColorExample.setBackground(currentPLI.drawColor);
	    });
	fillColorCheckBox = new JCheckBox(localeString("FillColor"));
	fillColorCheckBox.addActionListener((ae) -> {
		currentPLI.fill = fillColorCheckBox.isSelected();
	    });
	fillColorExample = new JLabel("    ");
	fillColorExample.setOpaque(true);
	fillColorButton = new JButton(localeString("ChooseColor"));
	final ColorChooser fillColorChooser = new
	    ColorChooser(dataPanel, localeString("fillColorChooser"));
	fillColorButton.addActionListener((ae) -> {
		currentPLI.fillColor
		    = fillColorChooser.showDialog(currentPLI.fillColor);
		fillColorExample.setBackground(currentPLI.fillColor);
	    });
	capLabel = new JLabel(localeString("PathCap"));
	capCB = new JComboBox<String>();
	for (String s: strokeCaps2) {
	    capCB.addItem(s);
	}
	capCB.addActionListener((ae) -> {
		currentPLI.capIndex = capCB.getSelectedIndex();
	    });
	dashIncrLabel = new JLabel(localeString("DashIncr"));
	dashIncrTF = new VTextField(16) {
		protected void onAccepted() {
		    currentPLI.dashIncr = parseDouble(getText());
		}
	    };
	dashIncrTF.setAllowEmptyTextField(true);
	AbstractDocument adoc = (AbstractDocument)dashIncrTF.getDocument();
	adoc.setDocumentFilter(cfilter);
	dashIncrTF.setInputVerifier(iv);
	dashPatternLabel = new JLabel(localeString("DashPattern"));
	PlainDocument dpdoc = new PlainDocument();
	dpdoc.setDocumentFilter(dpfilter);
	dashPatternTF = new VTextField(16) {
		@Override
		protected void onAccepted() {
		    currentPLI.dashPattern =
			dashPatternTF.getText().replace('\u2423', ' ');
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("badDashPattern"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	dashPatternTF.setAllowEmptyTextField(true);
	dashPatternTF.setDocument(dpdoc);
	dashPatternTF.setInputVerifier(new InputVerifier() {
		public boolean verify(JComponent input) {
		    String s = dashPatternTF.getText();
		    if (s == null || s.length() == 0) return true;
		    if (s.charAt(0) != '-') return false;
		    int ind = s.lastIndexOf('\u2423');
		    return (ind == -1 || ind == s.length() - 1);
		}
	    });
	dashPatternPhaseLabel = new JLabel(localeString("DashPatternPhase"));
	dashPatternPhaseTF = new VTextField(16) {
		protected void onAccepted() {
		    currentPLI.dashPhase = parseDouble(getText());
		}
	    };
	dashPatternPhaseTF.setAllowEmptyTextField(true);
	adoc = (AbstractDocument)dashPatternPhaseTF.getDocument();
	adoc.setDocumentFilter(cfilter);
	dashPatternPhaseTF.setInputVerifier(iv);
	strokeGCSCheckBox = new JCheckBox(localeString("StrokeGCS"));
	strokeGCSCheckBox.setSelected(false);
	strokeGCSCheckBox.addActionListener((ae) -> {
		currentPLI.strokeGCS = strokeGCSCheckBox.isSelected();
	    });
	strokeJoinLabel = new JLabel("StrokeJoin");
	strokeJoinCB = new JComboBox<String>();
	for (String s: strokeJoins2) {
	    strokeJoinCB.addItem(s);
	}
	strokeJoinCB.setSelectedIndex(0);
	strokeJoinCB.addActionListener((ae) -> {
		currentPLI.joinIndex = strokeJoinCB.getSelectedIndex();
	    });
	miterLimitLabel = new JLabel(localeString("MiterLimit"));
	miterLimitTF = new VTextField(16) {
		protected void onAccepted() {
		    currentPLI.miterLimit = parseDouble(getText());
		}
	    };
	miterLimitTF.setAllowEmptyTextField(true);
	adoc = (AbstractDocument)miterLimitTF.getDocument();
	adoc.setDocumentFilter(cfilter);
	miterLimitTF.setInputVerifier(iv);
	strokeWidthLabel = new JLabel(localeString("StrokeWidth"));
	strokeWidthTF = new VTextField(16) {
		protected void onAccepted() {
		    currentPLI.strokeWidth = parseDouble(getText());
		}
	    };
	strokeWidthTF.setAllowEmptyTextField(true);
	adoc = (AbstractDocument)strokeWidthTF.getDocument();
	adoc.setDocumentFilter(cfilter);
	strokeWidthTF.setInputVerifier(iv);
	windingRuleLabel = new JLabel(localeString("WindingRule"));
	windingRuleCB = new JComboBox<String>();
	for (String s: windingRules2) {
	    windingRuleCB.addItem(s);
	}
	windingRuleCB.addActionListener((ae) -> {
		currentPLI.windingRuleInd = windingRuleCB.getSelectedIndex();
	    });
	zorderLabel = new JLabel(localeString("zorder"));
	zorderTF = new VTextField(10) {
		protected void onAccepted() {
		    currentPLI.zorder = parseLong(getText());
		}
	    };
	zorderTF.setAllowEmptyTextField(true);
	adoc = (AbstractDocument)zorderTF.getDocument();
	adoc.setDocumentFilter(icfilter);
	zorderTF.setInputVerifier(liv);
	

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	c.insets = new Insets(4, 8, 4, 8);
	pathDataPanel.setLayout(gridbag);
	
    	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, strokeWidthLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, strokeWidthTF, gridbag, c);

    	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, new JLabel(" "), gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, strokeGCSCheckBox, gridbag, c);

	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, drawColorCheckBox, gridbag, c);
	JPanel colorPanel1 = new JPanel(new FlowLayout());
	colorPanel1.add(drawColorExample);
	colorPanel1.add(drawColorButton);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, colorPanel1, gridbag, c);

	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, fillColorCheckBox, gridbag, c);
	JPanel colorPanel2 = new JPanel(new FlowLayout());
	colorPanel2.add(fillColorExample);
	colorPanel2.add(fillColorButton);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, colorPanel2, gridbag, c);
	
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, capLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, capCB , gridbag, c);

	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, dashIncrLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, dashIncrTF, gridbag, c);

	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, dashPatternLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, dashPatternTF, gridbag, c);

	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, dashPatternPhaseLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, dashPatternPhaseTF , gridbag, c);


    	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, strokeJoinLabel , gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, strokeJoinCB, gridbag, c);


    	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, miterLimitLabel , gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, miterLimitTF, gridbag, c);

    	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, windingRuleLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, windingRuleCB, gridbag, c);

    	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(pathDataPanel, zorderLabel , gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(pathDataPanel, zorderTF, gridbag, c);

}

    static JComboBox<String> subpathComboBox = null;
    static String[] initialPaths = null;
    static HashSet<String> initialSet = new HashSet<>();
    static String[] locations = null;


    static void initPathData(Parser parser) {
	// subpathComboBox.addItem(""); // (Done in getSetupArgs)
	initialPaths = parser.getPaths();
	locations = parser.getLocations();
	for (String s: locations) {
	    initialSet.add(s);
	}
	if (subpathComboBox != null) {
	    subpathComboBox.removeAllItems();
	    subpathComboBox.addItem("");
	}
	for (String s: initialPaths) {
	    if (subpathComboBox != null) {
		subpathComboBox.addItem(s);
	    }
	    initialSet.add(s);
	}
    }

    public static class PathMap extends LinkedHashMap<String,Vector<String>> {
	public PathMap() {super();}
    }

    static PathMap pathmap = new PathMap();

    static boolean subpathEditingAllowed = false;

    static void setupAllowedSubpaths() {
	if (initialPaths == null) initialPaths = new String[0];
	int n = initialPaths.length;
	subpathLabel.setText(localeString("subpathTbl1"));
	subpathTable.setRowSelectionAllowed(false);
	CellEditor ce = subpathTable.getCellEditor();
	if (ce != null) {
	    ce.stopCellEditing();
	}
	subpathEditingAllowed = false;
	DefaultTableModel tm = (DefaultTableModel) subpathTable.getModel();
	tm.setRowCount(0);
	tm.setRowCount(n > 16? n: 16);
	for (int i = 0; i < n; i++) {
	    subpathTable.setValueAt(initialPaths[i], i, 0);
	}
    }

    static JPanel globalPanel = null;
    static CardLayout globalCL = null;

    static JLabel moduleNameLabel = null;
    static JTextField moduleNameTF = null;
    static JLabel packageNameLabel = null;
    static JTextField packageNameTF = null;
    static JLabel classNameLabel = null;
    static JTextField classNameTF = null;
    static JCheckBox publicClassCheckBox = null;


    static JLabel flatnessLabel = null;
    static VTextField flatnessTF = null;
    static JLabel limitLabel = null;
    static WholeNumbTextField limitTF = null;
    static JRadioButton normalRB = null;
    static JRadioButton straightRB = null;
    static JRadioButton elevateRB = null;
    static JCheckBox gcsCheckBox= null;

    static String classname1 = localeString("ClassName");
    static String classname2 = localeString("MapName");
    static String classname3 = localeString("ClassName2");
    static String currentCN = classname1;

    static void createGlobalPanel() {
	globalPanel = new JPanel();
	globalCL = new CardLayout();
	JPanel svgPanel = new JPanel();
	JPanel ttPanel = new JPanel();
	JPanel piPanel = new JPanel();
	moduleNameLabel = new JLabel(localeString("moduleName"));
	moduleNameTF = new JTextField(32);
	packageNameLabel = new JLabel(localeString("PackageName"));
	packageNameTF = new JTextField(32);
	packageNameTF.addActionListener((ae) -> {
		globalData.packageName = packageNameTF.getText().trim();
	    });
	classNameLabel = new JLabel(currentCN);
	classNameTF = new JTextField(32);
	classNameTF.addActionListener((ae) -> {
		globalData.className = classNameTF.getText().trim();
	    });
	publicClassCheckBox = new JCheckBox(localeString("PublicClass"));
	publicClassCheckBox.addActionListener((ae) -> {
		globalData.isPublic = publicClassCheckBox.isSelected();
	    });
	publicClassCheckBox.setSelected(false);
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	ttPanel.setLayout(gridbag);
	c.insets = new Insets(4, 8, 4, 8);
	ttPanel.setLayout(gridbag);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(ttPanel, moduleNameLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(ttPanel, moduleNameTF, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(ttPanel, packageNameLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(ttPanel, packageNameTF, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(ttPanel, classNameLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(ttPanel, classNameTF, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(ttPanel, new JLabel(" "), gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(ttPanel, publicClassCheckBox, gridbag, c);
	
	flatnessLabel = new JLabel(localeString("Flatness"));
	flatnessTF = new VTextField(16) {
		public void onAccepted() {
		    globalData.flatness = parseDouble(getText());
		}
	    };
	flatnessTF.setAllowEmptyTextField(true);
	AbstractDocument adoc = (AbstractDocument)flatnessTF.getDocument();
	adoc.setDocumentFilter(cfilter);
	flatnessTF.setInputVerifier(iv);
	limitLabel = new JLabel(localeString("Limit"));
	limitTF = new WholeNumbTextField("", 5) {
		public void onAccepted() {
		    globalData.limit = getValue();
		}
	    };
	limitTF.setDefaultValue(-1);
	JLabel segmentOps = new JLabel(localeString("SegmentOptions"));
	normalRB = new JRadioButton(localeString("Normal"));
	straightRB = new JRadioButton(localeString("Straight"));
	elevateRB = new JRadioButton(localeString("Elevate"));
	normalRB.setSelected(true);
	normalRB.addActionListener((ae) -> {
		if (normalRB.isSelected()) {
		    globalData.fmode = FlatnessMode.NORMAL;
		}
	    });
	straightRB.addActionListener((ae) -> {
		if (straightRB.isSelected()) {
		    globalData.fmode = FlatnessMode.STRAIGHT;
		}
	    });
	elevateRB.addActionListener((ae) -> {
		if (elevateRB.isSelected()) {
		    globalData.fmode = FlatnessMode.ELEVATE;
		}
	    });
	ButtonGroup group = new ButtonGroup();
	group.add(normalRB);
	group.add(straightRB);
	group.add(elevateRB);
	gcsCheckBox = new JCheckBox(localeString("GCSMode"));
	gcsCheckBox.setSelected(false);

	gridbag = new GridBagLayout();
	c = new GridBagConstraints();
	piPanel.setLayout(gridbag);
	c.insets = new Insets(4, 8, 4, 8);
	piPanel.setLayout(gridbag);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(piPanel, flatnessLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(piPanel, flatnessTF, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(piPanel, limitLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(piPanel, limitTF, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(piPanel, new JLabel(" "), gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(piPanel, normalRB, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(piPanel, new JLabel(" "), gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(piPanel, straightRB, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(piPanel, new JLabel(" "), gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(piPanel, elevateRB, gridbag, c);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_END;
	addComponent(piPanel, new JLabel(" "), gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(piPanel, gcsCheckBox, gridbag, c);

	globalPanel.setLayout(globalCL);
	globalPanel.add(svgPanel, "svg");
	globalPanel.add(ttPanel, "tt");
	globalPanel.add(piPanel, "pi");
    }

    private static String savedConfigFileName = null;
    private static boolean mayNeedSave = false;
    
    private static void save(Component panel, boolean saveAs) {
	String fname = (saveAs || savedConfigFileName == null)?
	    chooseSavedConfigFile(panel): savedConfigFileName;
	if (fname != null) {
	    if (!fname.endsWith(".eptt") && !fname.endsWith(".EPTT")) {
		fname = fname + ".eptt";
	    }
	    if (createConfigFile(panel, fname)) {
		savedConfigFileName = fname;
	    }
	}
	mayNeedSave = false;
    }

    private static boolean createConfigFile(Component panel, String fname) {
	try {
	    File f = new File(fname);
	    File tf = File.createTempFile("epts-eptc", null, f.getParentFile());
	    String fp = f.getCanonicalPath();
	    String tfp = tf.getCanonicalPath();
	    OutputStream os = new FileOutputStream(tf);
	    ZipDocWriter zos = new ZipDocWriter
		(os, "application/vnd.bzdev.epts-template-config+zip");
	    // fix up basicData.savedState so we use relative path.
	    Path path = new File(basicData.savedState.trim()).toPath();
	    if (path.isAbsolute()) {
		path = base.relativize(path);
	    }
	    String saveString1 = basicData.savedState;
	    basicData.savedState = path.toString();
	    path = new File(basicData.mapName.trim()).toPath();
	    if (path.isAbsolute()) {
		path = base.relativize(path);
	    }
	    String saveString2 = basicData.mapName;
	    basicData.mapName = path.toString();
	    os = zos.nextOutputStream("basicData", true, 9);
	    XMLEncoder enc = new XMLEncoder(os);
	    enc.writeObject(basicData);
	    basicData.savedState = saveString1;
	    basicData.mapName = saveString2;
	    enc.close();
	    os.close();
	    os = zos.nextOutputStream("pathmap", true, 9);
	    enc = new XMLEncoder(os);
	    enc.writeObject(pathmap);
	    enc.close();
	    os.close();
	    os = zos.nextOutputStream("tdefTable", true, 9);
	    CellEditor ce = tdefTable.getCellEditor();
	    if (ce != null) {
		ce.stopCellEditing();
	    }
	    enc = new XMLEncoder(os);
	    enc.writeObject(tdefVector);
	    enc.close();
	    os.close();
	    os = zos.nextOutputStream("globalData", true, 9);
	    enc = new XMLEncoder(os);
	    setGlobalData();
	    enc.writeObject(globalData);
	    enc.close();
	    os.close();

	    String moduleName = moduleNameTF.getText().trim();
	    if (moduleName != null && moduleName.length() > 0) {
		os = zos.nextOutputStream("moduleName", false, 0);
		enc = new XMLEncoder(os);
		enc.writeObject(moduleName);
		enc.close();
		os.close();
	    }

	    os = zos.nextOutputStream("pathLocMap", true, 9);
	    enc = new XMLEncoder(os);
	    enc.writeObject(pathLocMap);
	    enc.close();
	    os.close();

	    os = zos.nextOutputStream("outfile", false, 0);
	    enc = new XMLEncoder(os);
	    enc.writeObject(outFileTF.getText());
	    enc.close();
	    os.close();
	    zos.close();
	    if (!tf.renameTo(f)) {
		throw new IOException(errorMsg("rename", tfp, fp));
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog
		(panel, errorMsg("saveFailed", fname, e.getMessage()),
		 localeString("errorTitle"),
		 JOptionPane.ERROR_MESSAGE);
	    return false;
	}
    }

    private static String colorString(Color c) {
	String name = Colors.getCSSName(c);
	if (name == null) {
	    int alpha = c.getAlpha();
	    int red = c.getRed();
	    int green = c.getGreen();
	    int blue = c.getBlue();
	    if (alpha == 255) {
		return String.format("rgb(%d,%d,%d)", red, green, blue);
	    } else {
		double dalpha = alpha / 255.0;
		return String.format("rgba(%d,%d,%d,%g)",
				     red, green, blue, dalpha);
	    }
	} else {
	    int alpha = c.getAlpha();
	    if (alpha == 255) {
		return name;
	    } else {
		int red = c.getRed();
		int green = c.getGreen();
		int blue = c.getBlue();
		double dalpha = alpha / 255.0;
		return String.format("rgba(%d,%d,%d,%g)",
				     red, green, blue, dalpha);
	    }
	}
    }

    static String outFile = null;

    public static String[] generate() {
	if (tdefTable != null) {
	    CellEditor ce = tdefTable.getCellEditor();
	    if (ce != null) {
		ce.stopCellEditing();
	    }
	}
	ArrayList<String> list = new ArrayList<>();
	if (EPTS.stackTrace) {
	    list.add("--stackTrace");
	}
	list.add("-o");
	String ofn = outFile;
	try {
	    Path path1 = new File(outFile).toPath();
	    if (!path1.isAbsolute()) {
		path1 = base.resolve(path1);
	    }
	    ofn = path1.toString();
	} catch (Exception exception) {
	}
	list.add(ofn);
	if (basicData.mapName != null) {
	    String name = basicData.mapName.trim();
	    if (name.length() > 0) {
		list.add("--map");
		try {
		    Path path2 = new File(name).toPath();
		    if (!path2.isAbsolute()) {
			path2 = base.resolve(path2);
		    }
		    name = path2.toString();
		} catch (Exception exception) {
		}
		list.add(name);
	    }
	}
	for (Object obj: tdefVector) {
	    if (obj instanceof Vector) {
		Vector v = (Vector) obj;
		String test = (String)v.get(0);
		String var = (String) v.get(1);
		String val = (String) v.get(2);
		if (test == null) test= "";
		if (var == null) var = "";
		if (val == null) val = "";
		test = test.trim();
		var = var.trim();
		if (test.length() > 0) {
		    if (var.length() > 0) {
			if (val.length() > 0) {
			    list.add("--tdef");
			    list.add(test + ":" + var + "=" + val);
			}
		    } else {
			list.add("--tdef");
			list.add(test);
		    }
		} else {
		    if (var.length() > 0 && val.length() > 0) {
			list.add("--tdef");
			list.add(var + "=" + val);
		    }
		}
	    }
	}

	String tname = (basicData.template == null)? "":
	    basicData.template.trim();
	switch(templateType) {
	case SVG_MM:
	    list.add("--svg-mm");
	    break;
	case SVG:
	    list.add("--svg");
	    break;
	case TT:
	case PIT:
	    if (tname.length() > 0) {
		list.add("--template");
		list.add(tname);
	    } else {
		System.err.println(errorMsg("noTemplateName"));
		return null;
	    }
	}
	setGlobalData();
	if (globalData.usesTableTemplate) {
	    String pname = (globalData.packageName == null)? "":
		globalData.packageName.trim();
	    String cname = (globalData.className == null)? "":
		globalData.className.trim();
	    if (pname.length() > 0) {
		list.add("--package");
		list.add(pname);
	    }
	    String moduleName = moduleNameTF.getText().trim();
	    if (moduleName.length() > 0) {
		list.add("--module");
		list.add(moduleName);
	    }
	    if (cname.length() > 0) {
		list.add("--class");
		list.add(cname);
	    }
	    if (globalData.isPublic) {
		list.add("--public");
	    }
	} else {
	    switch(globalData.fmode) {
	    case STRAIGHT:
		list.add("--straight");
		if (globalData.limit != 10) {
		    list.add("--limit");
		    list.add("" + globalData.limit);
		}
		if (globalData.flatness > 0.0) {
		    list.add("--flatness");
		    list.add("" + globalData.flatness);
		}
		break;
	    case ELEVATE:
		list.add("--elevate");
		break;
	    }
	}
	for (Map.Entry<String,PathLocInfo> entry: pathLocMap.entrySet())  {
	    String name = entry.getKey();
	    PathLocInfo pli = entry.getValue();
	    if (pli.active == false) continue;
	    switch(templateType) {
	    case SVG_MM:
	    case SVG:
	    case TT:
		if (pli.isPath) {
		    if (pli.draw) {
			list.add("--stroke-width");
			list.add("" + pli.strokeWidth);
			list.add("--stroke-dash-incr");
			list.add("" + pli.dashIncr);
			list.add("--stroke-dash-pattern");
			list.add(pli.dashPattern);
			list.add("--stroke-dash-phase");
			list.add("" + pli.dashPhase);
			list.add("--stroke-cap");
			list.add(strokeCaps[pli.capIndex]);
			list.add("--stroke-join");
			list.add(strokeJoins[pli.joinIndex]);
			list.add("--stroke-miter-limit");
			list.add("" + pli.miterLimit);
			list.add("--stroke-color");
			list.add(colorString(pli.drawColor));
			list.add("--stroke-gcs-mode");
			list.add("" + pli.strokeGCS);
		    }
		    if (pli.fill) {
			list.add("--fill-color");
			list.add(colorString(pli.fillColor));
		    }
		    if (pli.draw || pli.fill) {
			list.add("--zorder");
			list.add("" + pli.zorder);
		    }
		    list.add("--winding-rule");
		    list.add(windingRules[pli.windingRuleInd]);
		}
		list.add("--tname");
		break;
	    case PIT:
		list.add("--pname");
	    }
	    Vector<String> vector = pathmap.get(name);
	    int n = (vector == null)? 0: vector.size();
	    if (n == 0) {
		list.add(name);
	    } else {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(':');
		int cnt = 0;
		for (String s: vector) {
		    sb.append(s);
		    if ((++cnt) < n) {
			sb.append(',');
		    }
		}
		list.add(sb.toString());
	    }
	}

	list.add("--");
	if (basicData.savedState != null) {
	    String path = basicData.savedState.trim();
	    try {
		basicData.savedState = path;
		path = base.resolve(path).toFile()
		    .getCanonicalPath();
	    } catch (Exception e) {}
	    list.add(path);
	}
	String[] results = new String[list.size()];

	return list.toArray(results);
    }

    static boolean restore(ZipDocFile zf, boolean setComponents, JPanel panel) {
	try {
	    ZipEntry ze = zf.getEntry("basicData");
	    InputStream is = zf.getInputStream(ze);
	    XMLDecoder dec = new XMLDecoder(is);
	    Object result = dec.readObject();
	    if (result instanceof BasicData) {
		basicData = (BasicData) result;
		if (setComponents) {
		    templateTypeCB.setSelectedIndex(basicData.templateTypeInd);
		}
		switch(basicData.templateTypeInd) {
		case -1:
		    templateType = null;
		    break;
		case 0:
		    templateType = TType.SVG;
		    break;
		case 1:
		    templateType = TType.TT;
		    break;
		case 2:
		    templateType = TType.PIT;
		    break;
		case 3:
		    templateType = TType.SVG_MM;
		default:
		    break;
		}
		if (setComponents) {
		    useBuiltinCheckBox.setSelected(basicData.useBuiltins);
		    templateTF.setText(basicData.template);
		    if (basicData.template.equals("resource:HTMLImageMap")) {
			currentCN = classname2;
			if (classNameLabel != null) {
			    classNameLabel.setText(currentCN);
			}
		    } else if (templateType == TType.TT) {
			if (basicData.template.length() > RESOURCE_PROTO_LEN
			    && basicData.template.startsWith(RESOURCE_PROTO)) {
			    currentCN = classname1;
			} else {
			    currentCN = classname3;
			}
			if (classNameLabel != null) {
			    classNameLabel.setText(currentCN);
			}
		    }
		    savedStateTF.setText(basicData.savedState);
		    mapTF.setText(basicData.mapName);
		}
		processSavedState(basicData.savedState, panel);
	    }
	    dec.close();
	    is.close();
	    ze = zf.getEntry("tdefTable");
	    if (ze != null) {
		is = zf.getInputStream(ze);
		dec = new XMLDecoder(is);
		result = dec.readObject();
		if (result instanceof Vector) {
		    @SuppressWarnings("unchecked")
		    Vector<? extends Vector> tmp =
			(Vector<? extends Vector>)result;
		    tdefVector = tmp;
		    Vector v1 = (Vector)tdefVector.get(0);
		}
		dec.close();
		is.close();
	    }
	    ze = zf.getEntry("pathmap");
	    is = zf.getInputStream(ze);
	    dec = new XMLDecoder(is);
	    result = dec.readObject();
	    if (result instanceof PathMap) {
		pathmap = (PathMap) result;
		if (pathLocTable != null) {
		    pathLocTable.setDefaultRenderer(String.class,
						    new PathLocRenderer
						    (pathmap));
		}
	    }
	    dec.close();
	    is.close();
	    ze = zf.getEntry("globalData");
	    is = zf.getInputStream(ze);
	    dec = new XMLDecoder(is);
	    result = dec.readObject();
	    if (result instanceof GlobalData) {
		globalData = (GlobalData) result;
		restoreFromGlobalData();
	    }
	    dec.close();
	    is.close();

	    ze = zf.getEntry("moduleName");
	    if (ze != null) {
		is = zf.getInputStream(ze);
		dec = new XMLDecoder(is);
		result = dec.readObject();
		if (result instanceof String) {
		    moduleNameTF.setText((String)result);
		}
		dec.close();
		is.close();
	    }

	    ze = zf.getEntry("pathLocMap");
	    is = zf.getInputStream(ze);
	    dec = new XMLDecoder(is);
	    result = dec.readObject();
	    if (result instanceof PathLocMap) {
		pathLocMap = (PathLocMap) result;
	    }
	    dec.close();
	    is.close();

	    ze = zf.getEntry("outfile");
	    is = zf.getInputStream(ze);
	    dec = new XMLDecoder(is);
	    result = dec.readObject();
	    if (result instanceof String) {
		String outfile = (String) result;
		if (outfile != null && outfile.length() > 0) {
		    if (setComponents) {
			outFileTF.setText(outfile.trim());
		    }
		}
		if (outFile == null) {
		    outFile = outfile;
		}
	    }
	    dec.close();
	    is.close();
	    return true;
	} catch  (Exception e) {
	    return false;
	}
    }


    static void createFinishPane(final JDialog dialog, final JPanel panel) {
	outFileLabel = new JLabel(localeString("outFile"));
	outFileTF = new JTextField(32);
	outFileButton = new JButton(localeString("outFileButton"));
	outFileButton.addActionListener((ae) -> {
		String name = chooseFile(panel);
		if (name == null) return;
		if (name.endsWith(".epts") || name.endsWith(".EPTS")
		    || name.endsWith(".eptt") || name.endsWith(".EPTT")
		    || name.endsWith(".eptc") || name.endsWith(".EPTC")) {
		    // an output file name should not match an EPTS file
		    JOptionPane.showMessageDialog
			(panel,errorMsg("noEPTSFileExt", name),
			 localeString("errorTitle"),
			 JOptionPane.ERROR_MESSAGE);
		    return;
		}
		if (name != null) {
		    outFileTF.setText(name);
		    mayNeedSave = true;
		}
	    });
	/*
	outFileTF.addKeyListener(new KeyAdapter() {
		@Override
		public void keyReleased(KeyEvent ke) {
		    mayNeedSave = true;
		}
	    });
	*/
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	c.insets = new Insets(4, 8, 4, 8);
	panel.setLayout(gridbag);
	
	c.anchor = GridBagConstraints.LINE_END;
	c.gridwidth = 1;
	addComponent(panel, outFileLabel, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.RELATIVE;
	addComponent(panel, outFileTF, gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, outFileButton, gridbag, c);

	saveButton = new JButton(localeString("Save"));
	saveButton.addActionListener((ae) -> {
		save(panel, false);
	    });

	saveAsButton = new JButton(localeString("SaveAs"));
	saveAsButton.addActionListener((ae) -> {
		save(panel, true);
	    });
	writeButton = new JButton(localeString("GenerateOutput"));
	writeButton.addActionListener((ae) -> {
		if (mayNeedSave) {
		    switch(JOptionPane
			   .showConfirmDialog(dialog,
					      localeString("needSave"),
					      localeString("saveFirst"),
					      JOptionPane.YES_NO_OPTION)) {
		    case JOptionPane.YES_OPTION:
			save(panel, false);
			break;
		    default:
			break;
		    }
		}
		outFile = outFileTF.getText().trim();
		if (outFile.endsWith(".epts") || outFile.endsWith(".EPTS")
		    || outFile.endsWith(".eptt") || outFile.endsWith(".EPTT")
		    || outFile.endsWith(".eptc") || outFile.endsWith(".EPTC")) {
		    // an output file name should not match an EPTS file
		    JOptionPane.showMessageDialog
			(dialog,errorMsg("noEPTSFileExt", outFile),
			 localeString("errorTitle"),
			 JOptionPane.ERROR_MESSAGE);
		    outFile = null;
		    return;
		}
		results = generate();
		dialog.setVisible(false);
	    });
	writeButton.setEnabled(false);
	outFileTF.getDocument().addDocumentListener
	    (new DocumentListener() {
		    String lastText = null;
		    private void doit() {
			String text = outFileTF.getText();
			if (text != null) {
			    text = text.trim();
			} else {
			    text ="";
			}
			if (lastText != null && !lastText.equals(text)) {
			    mayNeedSave = true;
			}
			lastText = text;
			writeButton.setEnabled(text.length() > 0);
		    }
		    public void changedUpdate(DocumentEvent e) {
			doit();
		    }
		    public void insertUpdate(DocumentEvent e) {
			doit();
		    }
		    public void removeUpdate(DocumentEvent e) {
			doit();
		    }
		});

	exitButton = new JButton(localeString("Exit"));
	exitButton.addActionListener((ae) -> {
		if (mayNeedSave) {
		    switch(JOptionPane
			   .showConfirmDialog(dialog,
					      localeString("needSave"),
					      localeString("saveFirst"),
					      JOptionPane.YES_NO_OPTION)) {
		    case JOptionPane.YES_OPTION:
			save(panel, false);
			break;
		    default:
			break;
		    }
		}
		System.exit(0);
	    });

	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, new JLabel(localeString("WriteSaveOptions")),
		     gridbag, c);

	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, new JLabel(" "), gridbag, c);

	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, saveButton, gridbag, c);
	c.gridwidth = 1;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, saveAsButton, gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.gridwidth = 1;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, writeButton, gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.gridwidth = 1;
	addComponent(panel, new JLabel(" "), gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, exitButton, gridbag, c);
    }


    static void createPathTables(JPanel panel)
    {
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	c.insets = new Insets(4, 8, 4, 8);
	panel.setLayout(gridbag);

	addPathButton = new JButton(localeString("newCompound"));
	addPathButton.addActionListener((ae) -> {
		String name = (String)JOptionPane.showInputDialog
		    (panel, localeString("addAdditionalName"),
		     localeString("addNameTitle"), JOptionPane.PLAIN_MESSAGE);
		if (name == null) return;
		name = name.trim();
		if (name.length() == 0) return;
		if (pathmap.containsKey(name)) {
		    JOptionPane.showMessageDialog
			(panel,
			 errorMsg("nameExists1"),
			 localeString("errorTitle"),
			 JOptionPane.ERROR_MESSAGE);
		    return;
		}
		if (initialSet.contains(name)) {
		    JOptionPane.showMessageDialog
			(panel,
			 errorMsg("nameExists2"),
			 localeString("errorTitle"),
			 JOptionPane.ERROR_MESSAGE);
		    return;
		}
		compoundPathTable.clearSelection();
		DefaultTableModel tm = (DefaultTableModel)
		    compoundPathTable.getModel();
		pathmap.put(name, new Vector<String>());
		Object[] data = {name};
		int n =  compoundPathTable.getRowCount();
		if (n > 16) {
		    tm.addRow(data);
		} else {
		    int i;
		    for (i = 0 ; i < 16; i++) {
			Object obj = tm.getValueAt(i, 0);
			if (obj == null) {
			    break;
			} else if (((String)obj).trim().length() == 0) {
			    break;
			}
		    }
		    if (i < 16) { 
			n = i;
			tm.setValueAt(name, i, 0);
		    } else {
			tm.addRow(data);
		    }
		}
		compoundPathTable.setRowSelectionInterval(n,n);

	    });
	delPathButton = new JButton(localeString("deleteSelected"));
	delPathButton.addActionListener((ae) -> {
		int[] selected = compoundPathTable.getSelectedRows();
		if (selected  == null || selected.length == 0) return;
		Arrays.sort(selected);
		DefaultTableModel tm = (DefaultTableModel)
		    compoundPathTable.getModel();
		for (int i = selected.length-1; i > -1; i--) {
		    int index  = selected[i];
		    String name = (String)
			compoundPathTable.getValueAt(index,0);
		    pathmap.remove(name);
		    tm.removeRow(index);
		}
	    });
	TableModel cptm = new DefaultTableModel(compoundPathCols, 16) {
		public Class<?> getColumnClass(int col) {
		    return String.class;
		}
	    };
	compoundPathTable = new JTable() {
		public boolean isCellEditable(int row, int column) {
		    return false;
		}
	    };
	compoundPathTable.setModel(cptm);
	int twidth = Setup.configColumn(compoundPathTable, 0,
					"mmmmmmmmmmmmmmmmmmmm");

	compoundPathSP = new JScrollPane(compoundPathTable);
	compoundPathTable.setFillsViewportHeight(true);
	configTable(compoundPathTable, true);

	compoundPathSP.setPreferredSize(new Dimension(twidth+10, 275));
	TableModel sptm = new DefaultTableModel(subpathCols, 16) {
		public Class<?> getColumnClass(int col) {
		    return String.class;
		}
	    };

	compoundPathTable.getSelectionModel()
	    .addListSelectionListener((se) -> {
		    int selcount = compoundPathTable.getSelectedRowCount();
		    if (selcount == 0) {
			return;
		    } else if (selcount == 1) {
			String name = (String)compoundPathTable.getValueAt
			    (compoundPathTable.getSelectedRow(), 0);
			if (pathmap.containsKey(name)) {
			    Vector<String> values =
				new Vector<String>(pathmap.get(name));
			    DefaultTableModel tm = (DefaultTableModel)
				subpathTable.getModel();
			    int n = values.size();
			    tm.setRowCount(n);
			    int i = 0;
			    for (String s: values) {
				tm.setValueAt(s, i++, 0);
			    }
			    if (n < 15) {
				tm.setRowCount(16);
			    } else {
				int nn = n + 4;
				if (nn > initialPaths.length) {
				    nn = initialPaths.length;
				}
				tm.setRowCount(nn);
			    }
			    subpathLabel.setText(localeString("subpathTbl2"));
			    subpathTable.setRowSelectionAllowed(true);
			    subpathEditingAllowed = true;
			} else {
			    setupAllowedSubpaths();
			}
		    } else {
			setupAllowedSubpaths();
		    }
	    });


	subpathLabel = new JLabel(localeString("subpathTbl1"));
	subpathTable = new JTable() {
		public boolean isCellEditable(int row, int column) {
		    return (column == 0) && subpathEditingAllowed;
		}
	    };
	subpathTable.setModel(sptm);
	subpathTable.getTableHeader().setReorderingAllowed(false);
	subpathSP = new JScrollPane(subpathTable);
	subpathTable.setFillsViewportHeight(true);
	configTable(subpathTable, false);
	twidth = Setup.configColumn(subpathTable, 0,
				     "mmmmmmmmmmmmmmmmmmmm");
	subpathSP.setPreferredSize (new Dimension(twidth+10, 275));
	subpathTable.getColumnModel().getColumn(0).setCellEditor
	    (new DefaultCellEditor(subpathComboBox));

	subpathTable.getModel().addTableModelListener((tme) -> {
		if (compoundPathTable.getSelectedRowCount() == 1) {
		    String name = (String)compoundPathTable.getValueAt
			(compoundPathTable.getSelectedRow(), 0);
		    if (name == null) return;
		    name = name.trim();
		    if (name.length() == 0) return;
		    Vector<String>v = pathmap.get(name);
		    if (v == null) return;
		    int n = subpathTable.getRowCount();
		    Vector<String> values =new Vector<String>(n);
		    for (int i = 0; i < n; i++) {
			String val = (String)subpathTable.getValueAt(i, 0);
			if (val == null) continue;
			val = val.trim();
			if (val.length() != 0) {
			    values.add((String)subpathTable.getValueAt(i, 0));
			}
		    }
		    if (name != null && name.length() > 0
			&& values.size() == n) {
			// we filled up the table, so add some rows
			int nn = n + 4;
			if (nn > initialPaths.length) {
			    nn = initialPaths.length;
			}
			if (n != nn) {
			    ((DefaultTableModel)(subpathTable.getModel()))
				.setRowCount(nn);
			}
		    }
		    pathmap.put(name, values);
		}
	    });

	c.gridwidth = 1;
	c.anchor = GridBagConstraints.CENTER;
	JPanel buttonPanel = new JPanel(new BorderLayout());
	buttonPanel.add(addPathButton, "North");
	buttonPanel.add(delPathButton, "South");
	addComponent(panel, buttonPanel, gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.anchor = GridBagConstraints.CENTER;
	addComponent(panel, subpathLabel, gridbag, c);
	
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.LINE_START;
	addComponent(panel, compoundPathSP, gridbag, c);
	c.gridwidth = GridBagConstraints.REMAINDER;
	addComponent(panel, subpathSP, gridbag, c);
    }


    private static void doEnables(JTabbedPane tabpane) {
	Component piComponents[] = {
	    /*addPathButton, delPathButton,
	      compoundPathTable, subpathLabel, subpathTable */
	};

	Component svgComponents[] = {
	    addPathButton, delPathButton,
	    compoundPathTable, subpathLabel, subpathTable
	};

	Component ttComponents[] = {
	    /*templateLabel, templateTF, templateButton, savedStateLabel,
	    savedStateTF, savedStateButton, outFileLabel,
	    outFileTF, outFileButton, mapLabel, mapButton, mapTF,*/
	    /*addPathButton, delPathButton,
	      compoundPathTable, subpathLabel, subpathTable, */
	    /* publicClassCheckBox, packageNameLabel,
	       packageNameTF, classNameLabel, classNameTF */
	};

	Component allComponents[] = {
	    templateLabel, templateTF, templateButton,
	    useBuiltinCheckBox, mapLabel, mapTF, mapButton,
	    /* addPathButton, delPathButton,
	       compoundPathTable, subpathLabel, subpathTable,
	    publicClassCheckBox, packageNameLabel,
	    packageNameTF, classNameLabel, classNameTF,*/
	};
	if (templateType == null) {
	    // disable almost everything.
	    if (tabpane != null) {
		tabpane.setEnabledAt(1, false);
		tabpane.setEnabledAt(2, false);
		tabpane.setEnabledAt(3, false);
		tabpane.setEnabledAt(4, false);
		tabpane.setEnabledAt(5, false);
	    }
	    globalData.usesTableTemplate = false;
	    for (Component c: allComponents) {
		c.setEnabled(false);
	    }
	} else {
	    if (tabpane != null) {
		tabpane.setEnabledAt(2, true);
		tabpane.setEnabledAt(4, true);
		tabpane.setEnabledAt(5, true);
	    }
	    switch(templateType) {
	    case SVG_MM:
	    case SVG:
		if (tabpane != null) {
		    tabpane.setEnabledAt(1, false);
		}
		globalCL.show(globalPanel, "svg");
		for (Component c: svgComponents) {
		    c.setEnabled(true);
		}
		globalData.usesTableTemplate = false;
		break;
	    case TT:
		if (tabpane != null) {
		    tabpane.setEnabledAt(1, !templateTF.getText()
					 .startsWith(RESOURCE_PROTO));
		    if (mayUseClassOrPackage(templateTF.getText())) {
			tabpane.setEnabledAt(3, true);
			globalData.usesTableTemplate = true;
		    } else {
			tabpane.setEnabledAt(3, false);
			globalData.usesTableTemplate = false;
		    }
		}
		globalCL.show(globalPanel, "tt");
		for (Component c: ttComponents) { 
		    c.setEnabled(true);
		}
		break;
	    case PIT:
		if (tabpane != null) {
		    tabpane.setEnabledAt(1, !templateTF.getText()
					 .startsWith(RESOURCE_PROTO));
		    tabpane.setEnabledAt(3, true);
		}
		globalCL.show(globalPanel, "pi");
		for (Component c: piComponents) {
		    c.setEnabled(true);
		}
	    default:
		globalData.usesTableTemplate = false;
		break;
	    }
	}
    }

    static void processSavedState(String fname, JPanel panel) {
	if (!fname.endsWith(".epts")) {
	    if (panel == null) {
		System.err.println(errorMsg("eptsFileExpected"));
		System.exit(1);
	    } else {
		JOptionPane.showMessageDialog(panel,
					      errorMsg("eptsFileExpected"),
					      localeString("errorTitle"),
					      JOptionPane.ERROR_MESSAGE);
		return;
	    }
	}
	try {
	    if (fname.startsWith("file:")) {
		fname = (new File(new URI(fname)))
		    .getCanonicalPath();
	    } else if (EPTS.maybeURL(fname)) {
		if (panel == null) {
		    System.err.println(errorMsg("eptsFileExpected"));
		    System.exit(1);
		} else {
		    JOptionPane.showMessageDialog(panel,
						  errorMsg("eptsFileExpected"),
						  localeString("errorTitle"),
						  JOptionPane.ERROR_MESSAGE);
		    return;
		}
	    }
	    Path fp = (new File(fname)).toPath();
	    InputStream is = (base == null)? new FileInputStream(fp.toString()):
		new FileInputStream(base.resolve(fp).toString());
	    Parser parser = new Parser();
	    parser.parse(is);
	    initPathData(parser);
	    is.close();
	} catch (Exception e) {
	    if (!(e instanceof IOException)) {
		e.printStackTrace();
	    }
	    if (panel == null) {
		System.err.println(errorMsg("eptsFileExpected"));
		System.exit(1);
	    } else {
		e.printStackTrace();
		JOptionPane.showMessageDialog(panel,
					      errorMsg("eptsFileExpected"),
					      localeString("errorTitle"),
					      JOptionPane.ERROR_MESSAGE);
	    }
	}
    }


    static JComboBox<String> templateTypeCB = null;
    private static boolean freezeTemplateType = false;
    private static void freezeTemplateType(boolean mode) {
	freezeTemplateType = mode;
	if (mode) {
	    templateTypeCB.setFocusable(false);
	} else {
	    templateTypeCB.setFocusable(true);
	}
    }
    private static String lastSavedState = "";

    private static int CTRL = InputEvent.CTRL_DOWN_MASK;
    private static int CTRL_SHIFT =
	InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

    private static int KEY_MASK = CTRL_SHIFT | InputEvent.META_DOWN_MASK
	| InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK;


    public static String[] getSetupArgs(ZipDocFile zf, File zfile,
					String eptsFile)
    {
	try {
	    base = cdir.getCanonicalFile().toPath();
	    results = null;
	    SwingUtilities.invokeAndWait(() -> {
		    JDialog dialog = new JDialog();
		    subpathComboBox = new JComboBox<String>();
		    subpathComboBox.addItem(" ");
		    JPanel topPanel = new JPanel();
		    topPanel.setLayout(new BorderLayout());
		    JTabbedPane tabpane = new JTabbedPane();
		    topPanel.add(tabpane, "Center");

		    JPanel basicPanel = new JPanel();
		    JPanel tdefPanel = new JPanel();
		    JPanel addPathPanel = new JPanel();
		    JPanel configPanel = new JPanel();
		    JPanel finishPanel = new JPanel();
		    GridBagLayout gridbag = new GridBagLayout();
		    basicPanel.setLayout(gridbag);
		    JLabel templateTypeLabel =
			new JLabel(localeString("templateType"));
		    templateTypeCB =
			new JComboBox<String>(templateTypes) {
			    public void processMouseEvent(MouseEvent e) {
				if (freezeTemplateType == false) {
				    super.processMouseEvent(e);
				}
			    }
			    };
		    templateTypeCB.addPopupMenuListener
			(new PopupMenuListener() {
				@Override
				public void
				    popupMenuCanceled(PopupMenuEvent e) {
				}
				@Override
				public void popupMenuWillBecomeVisible
				    (PopupMenuEvent e)
				{
				    if (freezeTemplateType) {
					/*
					 * Need to delay hiding the popup:
					 * otherwise the popup will be made
					 * visible after we asked to hide it.
					 */
					SwingUtilities.invokeLater(() -> {
						templateTypeCB
						    .setPopupVisible(false);
					    });
				    }
				}
				@Override
				public void popupMenuWillBecomeInvisible
				    (PopupMenuEvent e) {
				}
			    });
		    templateTypeCB.setSelectedIndex(-1);
		    useBuiltinCheckBox =
			new JCheckBox(localeString("useBuiltin"));
		    useBuiltinCheckBox.setSelected(true);
 		    JButton acceptTypeButton =
			new JButton(localeString("AcceptType"));
		    templateTypeCB.addActionListener((ae) -> {
			    int index = templateTypeCB.getSelectedIndex();
			    switch (index) {
			    case -1: templateType = null; break;
			    case 0: templateType = TType.SVG; break;
			    case 1: templateType = TType.TT; break;
			    case 2: templateType = TType.PIT; break;
			    }
			    boolean enable = (index > -1);
			    String tplate = templateTF.getText().trim();
			    if (tplate.startsWith("resource:")) {
				if (!templateMatch(tplate.substring(9),
						   index)) {
				    tplate = "";
				    templateTF.setText(tplate);
				}
			    }
			    String savedState = savedStateTF.getText().trim();
			    if (savedState.length() > 0) {
				acceptTypeButton.setEnabled(enable);
				if (!savedState.equals(lastSavedState)) {
				    DefaultTableModel tm = (DefaultTableModel)
					compoundPathTable.getModel();
				    compoundPathTable.clearSelection();
				    subpathTable.clearSelection();
				    pathmap.clear();
				    tm.setRowCount(0);
				    tm.setRowCount(16);
				    tm = (DefaultTableModel)
					subpathTable.getModel();
				    tm.setRowCount(0);
				    tm.setRowCount(16);
				}
			    } else {
				// acceptTypeButton.setEnabled(false);
			    }
			    enable = (index > 0);
			    templateLabel.setEnabled(enable);
			    templateTF.setEnabled(enable);
			    templateButton.setEnabled(enable);
			    useBuiltinCheckBox.setEnabled(enable);
			    enable = (index == 1);
			    if (enable) {
				String ttext = templateTF.getText().trim();
				enable = (ttext.length() != 0 &&
					  !ttext.startsWith("resource:"));
			    }
			    mapLabel.setEnabled(enable);
			    mapTF.setEnabled(enable);
			    mapButton.setEnabled(enable);
			});

		    JButton resetTypeButton = 
			new JButton(localeString("ResetType"));
		    resetTypeButton.setEnabled(false);
		    acceptTypeButton.setEnabled(false);
		    acceptTypeButton.addActionListener((ae) -> {
			    String savedState1 = savedStateTF.getText().trim();
			    if (savedState1.length() > 0) {
				if (!savedState1.equals(lastSavedState)) {
				    DefaultTableModel tm = (DefaultTableModel)
					compoundPathTable.getModel();
				    compoundPathTable.clearSelection();
				    subpathTable.clearSelection();
				    pathmap.clear();
				    tm.setRowCount(0);
				    tm.setRowCount(16);
				    tm = (DefaultTableModel)
					subpathTable.getModel();
				    tm.setRowCount(0);
				    tm.setRowCount(16);
				}
			    }
			    int index = templateTypeCB.getSelectedIndex();
			    basicData.templateTypeInd = index;
			    if (index > -1) {
				String fname = savedStateTF.getText().trim();
				processSavedState(fname, topPanel);
				if (fname.length() > 0) {
				    if (!fname.equals(lastSavedState)) {
					DefaultTableModel tm =
					    (DefaultTableModel)
					    compoundPathTable.getModel();
					compoundPathTable.clearSelection();
					subpathTable.clearSelection();
					pathmap.clear();
					tm.setRowCount(0);
					tm.setRowCount(16);
					tm = (DefaultTableModel)
					    subpathTable.getModel();
					tm.setRowCount(0);
					tm.setRowCount(16);
				    }
				}
			    }
			    switch (index) {
			    case -1:
				JOptionPane.showMessageDialog
				    (topPanel,
				     errorMsg("mustSelectTemplateType"),
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
				return;
			    case 0:
				templateType = TType.SVG;
				break;
			    case 1:
				templateType = TType.TT;
				break;
			    case 2:
				templateType = TType.PIT;
				break;
			    case 3:
				templateType = TType.SVG_MM;
			    }
			    mayNeedSave = true;
			    doEnables(tabpane);
			    setupAllowedSubpaths();
			    templateTypeLabel.setEnabled(false);
			    // templateTypeCB.setEnabled(false);
			    freezeTemplateType(true);
			    templateTypeCB.setEnabled(false);
			    useBuiltinCheckBox.setEnabled(false);
			    basicData.useBuiltins =
				useBuiltinCheckBox.isSelected();
			    templateLabel.setEnabled(false);
			    // templateTF.setEnabled(false);
			    basicData.template = templateTF.getText().trim();
			    if (basicData.template
				.equals("resource:HTMLImageMap")) {
				currentCN = classname2;
			    } else if (templateType == TType.TT) {
				if (basicData.template.length()
				    > RESOURCE_PROTO_LEN
				    && basicData.template.startsWith
					(RESOURCE_PROTO)) {
				    currentCN = classname1;
				} else {
				    currentCN = classname3;
				}
				if (classNameLabel != null) {
				    classNameLabel.setText(currentCN);
				}
			    } else {
				    currentCN = classname1;
				    if (classNameLabel != null) {
					classNameLabel.setText(currentCN);
				    }
			    }
			    classNameLabel.setText(currentCN);
			    templateButton.setEnabled(false);
			    resetTypeButton.setEnabled(true);
			    acceptTypeButton.setEnabled(false);
			    savedStateLabel.setEnabled(false);
			    String savedState = savedStateTF.getText().trim();
			    basicData.savedState = savedState;
			    lastSavedState = savedState;
			    savedStateTF.setEnabled(false);
			    savedStateButton.setEnabled(false);
			    // mapLabel.setEnabled(false);
			    mapTF.setEnabled(false);
			    mapButton.setEnabled(false);
			    basicData.mapName  = mapTF.getText().trim();
			    initialEditing = false;
			});
		    resetTypeButton.addActionListener((ae) -> {
			    resetTypeButton.setEnabled(false);
			    acceptTypeButton.setEnabled(false);
			    templateTypeLabel.setEnabled(true);
			    templateTypeCB.setEnabled(true);
			    freezeTemplateType(false);
			    savedStateLabel.setEnabled(true);
			    savedStateTF.setEnabled(true);
			    savedStateButton.setEnabled(true);
			    doEnables(tabpane);
			    compoundPathTable.clearSelection();
			    subpathTable.clearSelection();
			    int index = templateTypeCB.getSelectedIndex();
			    boolean enable = (index > -1);
			    if (savedStateTF.getText().trim().length() > 0) {
				acceptTypeButton.setEnabled(enable);
			    } else {
				acceptTypeButton.setEnabled(false);
			    }
			    enable = (index > 0);
			    templateLabel.setEnabled(enable);
			    templateTF.setEnabled(enable);
			    templateButton.setEnabled(enable);
			    useBuiltinCheckBox.setEnabled(enable);
			    enable = (index == 1);
			    if (enable) {
				String ttext = templateTF.getText().trim();
				enable = (ttext.length() != 0 &&
					  !ttext.startsWith("resource:"));
			    }
			    mapLabel.setEnabled(enable);
			    mapTF.setEnabled(enable);
			    mapButton.setEnabled(enable);
			    tabpane.setEnabledAt(1, false);
			    tabpane.setEnabledAt(2, false);
			    tabpane.setEnabledAt(3, false);
			    tabpane.setEnabledAt(4, false);
			    mayNeedSave = true;
			    initialEditing = true;
			});

		    templateLabel =
			new JLabel(localeString("templateTF"));
		    templateTF = new JTextField(32);
		    templateTF.setDisabledTextColor(Color.BLACK);
		    templateTF.addKeyListener(new KeyAdapter() {
			    @Override
			    public void keyReleased(KeyEvent ke) {
				String text = templateTF.getText().trim();
				if (text.length() == 0) {
				    if (templateTimer.isRunning()) {
					templateTimer.stop();
				    }
				    mapLabel.setEnabled(false);
				    mapTF.setEnabled(false);
				    mapButton.setEnabled(false);
				} else if (text.startsWith(RESOURCE_PROTO)) {
				    if (templateTimer.isRunning()) {
					templateTimer.stop();
				    }
				    mapLabel.setEnabled(false);
				    mapTF.setEnabled(false);
				    mapButton.setEnabled(false);
				} else if (RESOURCE_PROTO.startsWith(text)) {
				    // wait 2 seconds and if there are
				    // no more characters typed, then
				    // enable the map components.
				    if (templateTimer.isRunning()) {
					templateTimer.restart();
				    } else {
					templateTimer.start();
				    }
				} else {
				    if (templateTimer.isRunning()) {
					templateTimer.stop();
				    }
				    mapLabel.setEnabled(true);
				    mapTF.setEnabled(true);
				    mapButton.setEnabled(true);
				}
				if (savedStateTF.getText().trim().length() > 0
				    && templateTypeCB.getSelectedIndex()!=-1) {
				    acceptTypeButton.setEnabled(true);
				} else {
				    acceptTypeButton.setEnabled(false);
				}
			    }
			});


		    templateButton = new
			JButton(localeString("templateButton"));
		    templateButton.addActionListener((ae) -> {
			    String name = chooseTemplate
				(topPanel, useBuiltinCheckBox.isSelected());
			    if (name != null) {
				templateTF.setText(name);
				if (templateType == TType.TT &&
				    !name.startsWith("resource:")) {
				    mapLabel.setEnabled(true);
				    mapTF.setEnabled(true);
				    mapButton.setEnabled(true);
				} else {
				    mapLabel.setEnabled(false);
				    mapTF.setEnabled(false);
				    mapButton.setEnabled(false);
				    
				}
			    }
				
			});
		    savedStateLabel =
			new JLabel(localeString("savedState"));
		    savedStateTF = new JTextField(32);
		    savedStateTF.setDisabledTextColor(Color.BLACK);
		    savedStateTF.addKeyListener(new KeyAdapter() {
			    @Override
			    public void keyReleased(KeyEvent ke) {
				String text = savedStateTF.getText().trim();
				if (text.length() > 0
				    && templateTypeCB.getSelectedIndex()!=-1) {
				    acceptTypeButton.setEnabled(true);
				} else {
				    acceptTypeButton.setEnabled(false);
				}
			    }
			});
		    savedStateButton =
			new JButton(localeString("savedStateButton"));
		    savedStateButton.addActionListener((ae) -> {
			    String name = chooseSavedStateFile(topPanel);
			    if (name != null) {
				savedStateTF.setText(name);
				if (templateTypeCB.getSelectedIndex() != -1) {
				    acceptTypeButton.setEnabled(true);
				}

			    }
			});
		    mapLabel = new JLabel(localeString("mapName"));
		    mapTF = new JTextField(32);
		    mapTF.setDisabledTextColor(Color.BLACK);
		    mapButton = new JButton(localeString("mapNameButton"));
		    mapButton.addActionListener((ae) -> {
			    String name = chooseFile(topPanel);
			    if (name != null) {
				mapTF.setText(name);
			    }
			});
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.insets = new Insets(4, 8, 4, 8);
		    c.anchor = GridBagConstraints.LINE_END;
		    c.gridwidth = 1;
		    addComponent(basicPanel, templateTypeLabel, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    addComponent(basicPanel, templateTypeCB, gridbag, c);
		    c.gridwidth = GridBagConstraints.RELATIVE;
		    c.anchor = GridBagConstraints.LINE_START;
		    addComponent(basicPanel, useBuiltinCheckBox, gridbag, c);
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(basicPanel, new JLabel(" "), gridbag, c);

		    c.anchor = GridBagConstraints.LINE_END;
		    c.gridwidth = 1;
		    addComponent(basicPanel, templateLabel, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.RELATIVE;
		    addComponent(basicPanel, templateTF, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(basicPanel, templateButton, gridbag, c);

		    c.anchor = GridBagConstraints.LINE_END;
		    c.gridwidth = 1;
		    addComponent(basicPanel, savedStateLabel, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.RELATIVE;
		    addComponent(basicPanel, savedStateTF, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(basicPanel, savedStateButton, gridbag, c);

		    c.anchor = GridBagConstraints.LINE_END;
		    c.gridwidth = 1;
		    addComponent(basicPanel, mapLabel, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.RELATIVE;
		    addComponent(basicPanel, mapTF, gridbag, c);
		    c.anchor = GridBagConstraints.LINE_START;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(basicPanel, mapButton, gridbag, c);

		    for (int i = 0; i < 3; i++) {
			c.anchor = GridBagConstraints.CENTER;
			c.gridwidth = GridBagConstraints.REMAINDER;
			addComponent(basicPanel, new JLabel(" "), gridbag, c);
		    }

		    c.anchor = GridBagConstraints.CENTER;
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    JPanel separator = new JPanel();
		    separator.setPreferredSize(new Dimension(600,2));
		    separator.setBackground(Color.BLUE);
		    addComponent(basicPanel, separator, gridbag, c);

		    for (int i = 0; i < 2; i++) {
			c.anchor = GridBagConstraints.CENTER;
			c.gridwidth = GridBagConstraints.REMAINDER;
			addComponent(basicPanel, new JLabel(" "), gridbag, c);
		    }

		    c.gridwidth = 1;
		    c.anchor = GridBagConstraints.LINE_START;
		    addComponent(basicPanel, new JLabel(" "), gridbag, c);
		    addComponent(basicPanel, acceptTypeButton, gridbag, c);
		    c.gridwidth = GridBagConstraints.RELATIVE;
		    addComponent(basicPanel, resetTypeButton, gridbag, c);
		    c.gridwidth = GridBagConstraints.REMAINDER;
		    addComponent(basicPanel, new JLabel(" "), gridbag, c);

		    if (zf == null) createTDefTable(tabpane, tdefPanel);
		    createPathTables(addPathPanel);
		    createGlobalPanel();
		    createConfigTable(configPanel);
		    createFinishPane(dialog, finishPanel);

		    if (zf != null) {
			// templateType is null and templateTypeCB has its
			// selected index set to null as well
			if (restore(zf, true, topPanel)) {
			    // delayed because we need tdefVector
			    createTDefTable(tabpane, tdefPanel);
			    doEnables(null);
			    acceptTypeButton.setEnabled(false);
			    resetTypeButton.setEnabled(true);
			    initialEditing = false;
			    templateTypeLabel.setEnabled(false);
			    useBuiltinCheckBox.setEnabled(false);
			    freezeTemplateType(true);
			    templateTypeCB.setEnabled(false);
			    setupAllowedSubpaths();
			    setupCompoundPaths();
			    // templateLabel.setEnabled(false);
			    templateTF.setEnabled(false);
			    templateButton.setEnabled(false);
			    savedStateButton.setEnabled(false);
			    savedStateLabel.setEnabled(false);
			    savedStateTF.setEnabled(false);
			    mapTF.setEnabled(false);
			    mapButton.setEnabled(false);
			} else {
			    // could not restore state
			    System.exit(1);
			}
		    } else if (eptsFile != null) {
			File eptsf = new File(eptsFile);
			File parent = eptsf.getParentFile();
			if (parent != null) {
			    cdir = parent;
			    try {
				System.setProperty("user.dir",
						   parent.getCanonicalPath());
			    } catch (Exception se) {
				// just in case a security manager is
				// installed - if we can't change the working
				// directory, the program will still function.
			    }
			}
			savedStateTF.setText(eptsFile);
		    }

		    tabpane.add(localeString("Basic"), basicPanel);
		    tabpane.add(localeString("TDefs"), tdefPanel);
		    tabpane.add(localeString("AddPaths"), addPathPanel);
		    tabpane.add(localeString("Global"), globalPanel);
		    tabpane.add(localeString("Configure"), configPanel);
		    tabpane.add(localeString("Finish"), finishPanel);

		    doEnables(tabpane);

		    if (zf != null) {
			resetTypeButton.setEnabled(true);
		    }
		    
		    tabpane.addChangeListener((ce) -> {
			    CellEditor tdefce = tdefTable.getCellEditor();
			    if (tdefce != null) {
				tdefce.stopCellEditing();
			    }
			    int index = tabpane.getSelectedIndex();
			    if (index > 0 && index < 5) {
				mayNeedSave = true;
			    }
			    if (index == 4) {
				// populate table
				HashSet<String> hset =
				    new HashSet<> (2*(locations.length
						      + initialPaths.length
						      + pathmap.size()));
				for (String s: locations) {
				    hset.add(s);
				    if (!pathLocMap.containsKey(s)) {
					pathLocMap.put(s,
						       new PathLocInfo(false));
				    }
				}
				for (String s: initialPaths) {
				    hset.add(s);
				    if (!pathLocMap.containsKey(s)) {
					pathLocMap.put(s,
						       new PathLocInfo(true));
				    }
				}
				for (String s: pathmap.keySet()) {
				    hset.add(s);
				    if (!pathLocMap.containsKey(s)) {
					pathLocMap.put(s,
						       new PathLocInfo(true));
				    }
				}
				DefaultTableModel tm1 = (DefaultTableModel)
				    pathLocTable.getModel();
				tm1.setRowCount(0);
				// to prevent concurrent modification exception
				// copy the set to an array
				Set<String> kset = pathLocMap.keySet();
				String[] keys = new String[kset.size()];
				int kind = 0;
				for (String s: kset) {
				    keys[kind++] = s;
				}
				for (String s: keys) {
				    if (!hset.contains(s)) {
					pathLocMap.remove(s);
				    } else {
					PathLocInfo pathLocInfo
					    = pathLocMap.get(s);
					Vector<Object> row = new Vector<>(2);
					row.add(Boolean.valueOf
						(pathLocInfo.active));
					row.add(s);
					DefaultTableModel tm =
					    (DefaultTableModel)
					    pathLocTable.getModel();
					tm.addRow(row);
				    }
				}
				index = pathLocTable.getSelectedRow();
				if (index == -1) {
				    dataPanelCL.show(dataPanel, "empty");
				} else {
				    String s = (String)
					pathLocTable.getValueAt(index,1);
				    currentPLI = pathLocMap.get(s);
				    if (currentPLI.isPath) {
					populateDataPanel();
					dataPanelCL.show(dataPanel, "data");
				    } else {
					dataPanelCL.show(dataPanel, "empty");
				    }
				}
			    }
			});


		    dialog.setModalityType
			(Dialog.ModalityType.APPLICATION_MODAL);
		    dialog.setTitle(localeString("templateSetupTitle"));
		    dialog.setContentPane(topPanel);
		    dialog.pack();
		    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		    dialog.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent we) {
				if (dialogButtonPushed == false) {
				    if (mayNeedSave && initialEditing==false) {
					switch(JOptionPane
					       .showConfirmDialog
					       (dialog,
						localeString("needSave"),
						localeString("saveFirst"),
						JOptionPane.YES_NO_OPTION)) {
					case JOptionPane.YES_OPTION:
					    save(dialog, false);
					    break;
					default:
					    break;
					}
				    }
				    System.exit(0);
				}
			    }
			});
		    if (zfile != null) {
			savedConfigFileName = zfile.getAbsolutePath();
		    }
		    Action quitAction = new AbstractAction() {
			    public void actionPerformed(ActionEvent e) {
				System.exit(0);
			    }
			};
		    tabpane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke("control Q"),"quit");
		    tabpane.getActionMap().put("quit", quitAction);

		    Action saveAction = new AbstractAction() {
			    public void actionPerformed(ActionEvent e) {
				if (!resetTypeButton.isEnabled()) {
				    JOptionPane.showMessageDialog
					(topPanel,
					 errorMsg("mustAcceptBeforeSave"),
					 localeString("errorTitle"),
					 JOptionPane.ERROR_MESSAGE);
				    return;
				} else if (savedConfigFileName == null) {
				    save(dialog, false);
				} else {
				    createConfigFile(dialog,
						     savedConfigFileName);
				}
			    }
			};
		    tabpane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke("control S"), "save");
		    tabpane.getActionMap().put("save", saveAction);
		    dialog.setVisible(true);

		});
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
	return results;
    }


    static class PathInfo {
	PathInfo pathInfo = null;
	Vector subpaths = null;
	Boolean gcsMode = null;

	Color fillColor = null;
	Color strokeColor = null;
	String strokeCap = null;
	Boolean strokeGCSMode = null;

	Double dashIncr = null;
	String dashPattern = null;
	String strokeJoin = null;
	Double miterLimit = null;
	Double strokeWidth = null;

	private static Long zorder = null;
    }

    // XML parser that only recovers path and location names.
    // This is a minimal implementation with no error handling.
    // It just looks for 'row' elements and processes those. These
    // elements have attributes but do not have contents.
    static class Parser {
	SAXParser parser;

	Parser() throws ParserConfigurationException, SAXException {
	    SAXParserFactory factory = SAXParserFactory.newInstance();
	    factory.setValidating(false);
	    parser = factory.newSAXParser();
	}

	ArrayList<String> paths;
	ArrayList<String> locations;

	public String[] getPaths() {
	    if (paths == null) return null;
	    String[] results = new String[paths.size()];
	    return paths.toArray(results);
	}

	public String[] getLocations() {
	    if (locations == null) return null;
	    String[] results = new String[locations.size()];
	    return locations.toArray(results);
	}

	public void parse(InputStream is) throws SAXException, IOException {
	    OurDefaultHandler handler = new OurDefaultHandler();
	    parser.parse(is, handler);
	}
	
	class OurDefaultHandler extends DefaultHandler {
	    public void startDocument() {
		paths = new ArrayList<String>();
		locations = new ArrayList<String>();
	    }
	    public void startElement(String uri, String localName,
				     String qName, Attributes attr)
		throws SAXException 
	    {
		if (qName.equals("row")) {
		    String varname = attr.getValue("varname");
		    if (varname != null) {
			varname = varname.trim();
			if (varname.length() > 0) {
			    String type = attr.getValue("type");
			    if (type != null) {
				type = type.trim();
				if (type.equals("LOCATION")) {
				    locations.add(varname);
				} else if (type.equals("PATH_START")) {
				    paths.add(varname);
				}
			    }

			}
		    }
		}
	    }
	    public void endDocument() {
		locations.sort(null);
		paths.sort(null);
	    }
	}
    }
}
