package org.bzdev.epts;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.datatransfer.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.AbstractDocument;

import org.bzdev.ejws.*;
import org.bzdev.ejws.maps.*;
import org.bzdev.geom.FlatteningPathIterator2D;
import org.bzdev.geom.Path2DInfo;
import org.bzdev.geom.Paths2D;
import org.bzdev.geom.SplinePath2D;
import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.CPointType;
import org.bzdev.graphs.Graph;
import org.bzdev.graphs.RefPointName;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.imageio.BlockingImageObserver;
import org.bzdev.math.Functions;
import org.bzdev.net.WebEncoder;
import org.bzdev.util.CopyUtilities;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;
import org.bzdev.util.TemplateProcessor.KeyMapList;
import org.bzdev.swing.ErrorMessage;
import org.bzdev.swing.HtmlWithTocPane;
import org.bzdev.swing.PortTextField;
import org.bzdev.swing.VTextField;
import org.bzdev.swing.text.CharDocFilter;

class VectorPane extends JPanel {
    static int lindexSaved = 0;
    static int aindexSaved = 0;
    int lindex = lindexSaved;
    int aindex = aindexSaved;

    void saveIndices() {
	lindexSaved = lindex;
	aindexSaved = aindex;
    }

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    VTextField ltf;		// length text field
    VTextField atf;		// angle text field
    JComboBox<String> lunits = new JComboBox<>(ConfigGCSPane.units);
    static Vector<String> av = new Vector<>(2);
    static {
	av.add("Degrees");
	av.add("Radians");
    }
    JComboBox<String> aunits = new JComboBox<>(av);

    boolean firstTime = true;
    CharDocFilter cdf = new CharDocFilter();
    InputVerifier ltfiv = new InputVerifier() {
	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		if (string == null) string = "";
		string = string.trim();
		try {
		    if (string.length() == 0) {
			if (firstTime) {
			    firstTime = false;
			    return false;
			} else {
			    return true;
			}
		    }
		    double value = Double.parseDouble(string);
		    if (value >= 0.0) {
			return true;
		    } else {
			return false;
		    }
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    InputVerifier atfiv = new InputVerifier() {
	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		if (string == null) string = "";
		string = string.trim();
		try {
		    if (string.length() == 0) return true;
		    double value = Double.parseDouble(string);
		    return true;
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    double length = 0.0;
    double angle = 0.0;

    public double getLength() {
	return ConfigGCSPane.convert[lindex].valueAt(length);
    }

    public double getAngle() {
	return (aindex == 0)? Math.toRadians(angle): angle;
    }

    boolean noPrevErrors = true;

    public VectorPane() {
	super();
	cdf.setAllowedChars("09eeEE..,,++--");
	JLabel ll = new JLabel(localeString("Length"));
	JLabel al = new JLabel(localeString("Angle"));

	ltf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			length = 0.0;
		    } else {
			length = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    if (noPrevErrors) {
			// Hack to control focus.
			// When this panel first comes visible,
			// it requests the focus but the container
			// it is in does the same thing a bit later.
			// A VTextField will notice the focus loss and
			// check the validity of its value. We return
			// false so that the keyboard focus will be
			// requested again.
			noPrevErrors = false;
			String text = getText();
			if (text == null || text.trim().length() == 0) {
			    return false;
			}
		    }
		    JOptionPane.showMessageDialog
			(this, "Must enter a positive real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)ltf.getDocument()).setDocumentFilter(cdf);
	ltf.setInputVerifier(ltfiv);
	atf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			angle = 0.0;
		    } else {
			angle = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, "Must enter a real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)atf.getDocument()).setDocumentFilter(cdf);
	atf.setInputVerifier(atfiv);
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	gridbag.setConstraints(ll, c);
	add(ll);
	gridbag.setConstraints(ltf, c);
	add(ltf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(lunits, c);
	add(lunits);
	c.gridwidth = 1;
	gridbag.setConstraints(al, c);
	add(al);
	gridbag.setConstraints(atf, c);
	add(atf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(aunits, c);
	add(aunits);
	lunits.setSelectedIndex(lindex);
	aunits.setSelectedIndex(aindex);
	lunits.addActionListener((le) -> {
		lindex = lunits.getSelectedIndex();
	    });
	aunits.addActionListener((ae) -> {
		aindex = aunits.getSelectedIndex();
	    });
    }
}

class LocPane extends JPanel {
    static int xindexSaved = 0;
    static int yindexSaved = 0;
    int xindex = xindexSaved;
    int yindex = yindexSaved;

    void saveIndices() {
	xindexSaved = xindex;
	yindexSaved = yindex;
    }


    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    VTextField xtf;		// X text field
    VTextField ytf;		// Y text field
    JComboBox<String> xunits = new JComboBox<>(ConfigGCSPane.units);
    JComboBox<String> yunits = new JComboBox<>(ConfigGCSPane.units);

    boolean firstTime = true;
    CharDocFilter cdf = new CharDocFilter();
    InputVerifier tfiv = new InputVerifier() {
	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		if (string == null) string = "";
		string = string.trim();
		try {
		    if (string.length() == 0) {
			if (firstTime) {
			    firstTime = false;
			    return false;
			} else {
			    return true;
			}
		    }
		    double value = Double.parseDouble(string);
		    return true;
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    double xcoord = 0.0;
    double ycoord = 0.0;

    public double getXCoord() {
	return ConfigGCSPane.convert[xindex].valueAt(xcoord);
    }

    public double getYCoord() {
	return ConfigGCSPane.convert[yindex].valueAt(ycoord);
    }

    public void setXCoord(double x) {
	xcoord = ConfigGCSPane.inverseConvert[xindex].valueAt(x);
	Formatter f = new Formatter();
	xtf.setText(f.format("%10.6g",xcoord).toString().trim());
    }
    public void setYCoord(double y) {
	ycoord = ConfigGCSPane.inverseConvert[yindex].valueAt(y);
	Formatter f = new Formatter();
	ytf.setText(f.format("%10.6g",ycoord).toString().trim());
    }

    boolean noPrevErrors = true;

    public LocPane() {
	super();
	boolean firstTime = true;
	cdf.setAllowedChars("09eeEE..,,++--");
	JLabel xl = new JLabel(localeString("X"));
	JLabel yl = new JLabel(localeString("Y"));
	xtf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    text = text.trim();
		    if (text == null || text.length() == 0) {
			xcoord = 0.0;
		    } else {
			xcoord = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    if (noPrevErrors) {
			// Hack to control focus.
			// When this panel first comes visible,
			// it requests the focus but the container
			// it is in does the same thing a bit later.
			// A VTextField will notice the focus loss and
			// check the validity of its value. We return
			// false so that the keyboard focus will be
			// requested again.
			noPrevErrors = false;
			String text = getText();
			if (text == null || text.trim().length() == 0) {
			    return false;
			}
		    }
		    JOptionPane.showMessageDialog
			(this, "Must enter a real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)xtf.getDocument()).setDocumentFilter(cdf);
	xtf.setInputVerifier(tfiv);
	ytf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			ycoord = 0.0;
		    } else {
			ycoord = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, "Must enter a real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)ytf.getDocument()).setDocumentFilter(cdf);
	ytf.setInputVerifier(tfiv);
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	gridbag.setConstraints(xl, c);
	add(xl);
	gridbag.setConstraints(xtf, c);
	add(xtf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(xunits, c);
	add(xunits);
	c.gridwidth = 1;
	gridbag.setConstraints(yl, c);
	add(yl);
	gridbag.setConstraints(ytf, c);
	add(ytf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(yunits, c);
	add(yunits);
	xunits.setSelectedIndex(xindex);
	yunits.setSelectedIndex(yindex);
	xunits.addActionListener((ae) -> {
		xindex = xunits.getSelectedIndex();
	    });
	yunits.addActionListener((ae) -> {
		yindex = yunits.getSelectedIndex();
	    });
    }
}

class ArcPane extends JPanel {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    static int lindexSaved = 0;
    static int aindexSaved = 0;
    int lindex = lindexSaved;
    int aindex = aindexSaved;

    void saveIndices() {
	lindexSaved = lindex;
	aindexSaved = aindex;
    }


    static Vector<String> nv = new Vector<>(10);
    static {
	nv.add("1");
	nv.add("2");
	nv.add("3");
	nv.add("4");
    }

    JCheckBox ccwCheckBox = new JCheckBox(localeString("Counterclockwise"));
    JComboBox<String> nsegComboBox = new JComboBox<>(nv);

    VTextField rtf;		// radius text field
    VTextField atf;		// angle text field
    JComboBox<String> lunits = new JComboBox<>(ConfigGCSPane.units);
    static Vector<String> av = new Vector<>(2);
    static {
	av.add("Degrees");
	av.add("Radians");
    }
    JComboBox<String> aunits = new JComboBox<>(av);

    boolean firstTime = true;
    CharDocFilter cdf = new CharDocFilter();
    InputVerifier rtfiv = new InputVerifier() {
	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		if (string == null) string="";
		string = string.trim();
		try {
		    if (string.length() == 0) {
			if (firstTime) {
			    firstTime = false;
			    return false;
			} else {
			    return true;
			}
		    }
		    double value = Double.parseDouble(string);
		    if (value >= 0.0) {
			return true;
		    } else {
			return false;
		    }
		} catch (Exception e) {
		    return false;
		}
	    }
	};
    InputVerifier atfiv = new InputVerifier() {
	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		try {
		    if (string.length() == 0) return true;
		    double value = Double.parseDouble(string);
		    return true;
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    double radius = 0.0;
    double angle = 0.0;

    public double getRadius() {
	return ConfigGCSPane.convert[lindex].valueAt(radius);
    }

    public double getAngle() {
	return (aindex == 0)? Math.toRadians(angle): angle;
    }

    public boolean isCounterClockwise() {
	return ccwCheckBox.isSelected();
    }

    public double getMaxDelta() {
	int divisor = 1 << nsegComboBox.getSelectedIndex();
	return (Math.PI/2.0)/(divisor);
    }

    boolean noPrevErrors = true;

    public ArcPane() {
	super();
	cdf.setAllowedChars("09eeEE..,,++--");
	JLabel ll = new JLabel(localeString("Radius"));
	JLabel al = new JLabel(localeString("Angle"));
	JLabel nsl = new JLabel(localeString("NSegs"));
	rtf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			radius = 0.0;
		    } else {
			radius = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    if (noPrevErrors) {
			// Hack to control focus.
			// When this panel first comes visible,
			// it requests the focus but the container
			// it is in does the same thing a bit later.
			// A VTextField will notice the focus loss and
			// check the validity of its value. We return
			// false so that the keyboard focus will be
			// requested again.
			noPrevErrors = false;
			String text = getText();
			if (text == null || text.trim().length() == 0) {
			    return false;
			}
		    }
		    JOptionPane.showMessageDialog
			(this, "Must enter a positive real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)rtf.getDocument()).setDocumentFilter(cdf);
	rtf.setInputVerifier(rtfiv);
	atf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			angle = 0.0;
		    } else {
			angle = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, "Must enter a real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)atf.getDocument()).setDocumentFilter(cdf);
	atf.setInputVerifier(atfiv);
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	gridbag.setConstraints(ll, c);
	add(ll);
	gridbag.setConstraints(rtf, c);
	add(rtf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(lunits, c);
	add(lunits);
	c.gridwidth = 1;
	gridbag.setConstraints(al, c);
	add(al);
	gridbag.setConstraints(atf, c);
	add(atf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(aunits, c);
	add(aunits);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(ccwCheckBox, c);
	add(ccwCheckBox);
	ccwCheckBox.setSelected(true);
	c.gridwidth = 1;
	gridbag.setConstraints(nsl, c);
	add(nsl);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(nsegComboBox, c);
	add(nsegComboBox);

	nsegComboBox.setSelectedIndex(0);
	lunits.setSelectedIndex(lindex);
	aunits.setSelectedIndex(aindex);
	lunits.addActionListener((le) -> {
		lindex = lunits.getSelectedIndex();
	    });
	aunits.addActionListener((ae) -> {
		aindex = aunits.getSelectedIndex();
	    });
    }
}



public class EPTSWindow {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    JFrame frame;
    JLabel modeline;
    JScrollPane scrollPane;
    JPanel panel;
    BufferedImage bi;
    Graphics2D g2d;
    Graph graph;
    String languageName = null;
    String animationName = null;

    ConfigGCSPane configGCSPane;
    enum LocationFormat {
	COORD, EXPRESSION, OBJECT, STATEMENT
    }
    LocationFormat locationFormat = LocationFormat.STATEMENT;
    String varname = null;
    List<String> targetList = null;
    // true if targetList contains a file name of an image we loaded
    boolean imageFileNameSeen = false;
    URI imageURI = null;
    List<EPTS.NameValuePair> bindings = null;
    boolean scriptMode = false;

    PointTableModel ptmodel;
    JTable ptable;
    JFrame tableFrame;

    static int port;

    boolean shouldSaveScripts = false;

    List<String> savedStateCodebase = null;
    List<String> savedStateClasspath = null;
    List<String> savedStateModules = null;

    private static final TemplateProcessor.KeyMap emptyMap =
	new TemplateProcessor.KeyMap();

    public void save(File f) throws IOException {
	TemplateProcessor.KeyMap keymap = new TemplateProcessor.KeyMap();
	configGCSPane.saveState();
	// keymap.put("hasImageFile", imageFileNameSeen? "true": "false");
	TemplateProcessor.KeyMapList tlist =
	    new TemplateProcessor.KeyMapList();
	File fparent = f.getCanonicalFile().getParentFile();
	if (imageFileNameSeen) {
	    // TemplateProcessor.KeyMap map = new TemplateProcessor.KeyMap();
	    keymap.put("hasImageURI", new TemplateProcessor.KeyMap());
	    // System.out.println("hasImageURI set");
	    if (imageURI != null) {
		// we use relative URLs if the image file is in a
		// subdirectory of the directory containing the saved
		// state.
		String arg = fparent.toURI().relativize(imageURI).toString();
		keymap.put("imageURI", arg);
	    } else {
		keymap.put("imageURI", "-");
	    }
	    // map.put("arg", arg);
	    // tlist.add(map);
	}
	// System.out.println("scriptMode = "  + scriptMode);
	if (/*scriptMode && */ shouldSaveScripts) {
	    TemplateProcessor.KeyMapList hasScriptList =
		new TemplateProcessor.KeyMapList();
	    TemplateProcessor.KeyMap smap = new TemplateProcessor.KeyMap();
	    smap.put("language", languageName);
	    smap.put("animation", animationName);
	    // hasScriptList.add(smap);
	    // keymap.put("hasScript", hasScriptList);
	    keymap.put("hasScript", smap);
	    if (bindings != null) {
		TemplateProcessor.KeyMapList blist =
		    new TemplateProcessor.KeyMapList();
		for (EPTS.NameValuePair binding: bindings) {
		    String name = binding.getName();
		    Object value = binding.getValue();
		    String svalue;
		    String type;
		    if (value.getClass().equals(Integer.class)) {
			type = "int";
			svalue = value.toString();
		    } else if (value.getClass().equals(Double.class)) {
			type = "double";
			svalue = value.toString();
		    } else if (value.getClass().equals(String.class)) {
			type = "String";
			svalue = WebEncoder.htmlEncode((String)value);
		    } else if (value.getClass().equals(Boolean.class)) {
			type = "boolean";
			svalue = value.toString();
		    } else {
			throw new UnexpectedExceptionError();
		    }
		    TemplateProcessor.KeyMap map =
			new TemplateProcessor.KeyMap();
		    map.put("bindingName", name);
		    map.put("bindingType", type);
		    map.put("bindingValue", svalue);
		    blist.add(map);
		}
		smap.put("hasBindings", blist);
	    }
	}

	List<String> codebase = savedStateCodebase != null?
	    savedStateCodebase: EPTS.getCodebase();
	if (codebase.size() > 0) {
	    TemplateProcessor.KeyMap cbmap = new TemplateProcessor.KeyMap();
	    TemplateProcessor.KeyMapList pmaplist =
		new TemplateProcessor.KeyMapList();
	    keymap.put("hasCodebase", cbmap);
	    cbmap.put("pathlist", pmaplist);
	    int offset = EPTS.ourCodebaseDir.length();
	    for (String path: codebase) {
		if (path.startsWith(EPTS.ourCodebaseDir)) {
		    path = "..." + path.substring(offset);
		}
		TemplateProcessor.KeyMap pmap =  new TemplateProcessor.KeyMap();
		pmap.put("path", path);
		pmaplist.add(pmap);
	    }
	}
	List<String>classpath = savedStateClasspath != null?
	    savedStateClasspath: EPTS.getClasspath();
	if (classpath.size() > 0) {
	    TemplateProcessor.KeyMap cbmap = new TemplateProcessor.KeyMap();
	    TemplateProcessor.KeyMapList pmaplist =
		new TemplateProcessor.KeyMapList();
	    keymap.put("hasClasspath", cbmap);
	    cbmap.put("classpathlist", pmaplist);
	    int offset = EPTS.ourCodebaseDir.length();
	    for (String path: codebase) {
		if (path.startsWith(EPTS.ourCodebaseDir)) {
		    path = "..." + path.substring(offset);
		}
		TemplateProcessor.KeyMap pmap =  new TemplateProcessor.KeyMap();
		pmap.put("path", path);
		pmaplist.add(pmap);
	    }
	}
	List<String>modules = savedStateModules != null?
	    savedStateModules: EPTS.getAddedModules();
	// System.out.println("modules.size() = " + modules.size());
	if (modules.size() > 0) {
	    TemplateProcessor.KeyMap cbmap = new TemplateProcessor.KeyMap();
	    TemplateProcessor.KeyMapList pmaplist =
		new TemplateProcessor.KeyMapList();
	    keymap.put("hasAddedModules", cbmap);
	    cbmap.put("moduleslist", pmaplist);
	    int offset = EPTS.ourCodebaseDir.length();
	    for (String module: modules) {
		TemplateProcessor.KeyMap pmap =  new TemplateProcessor.KeyMap();
		pmap.put("module", module);
		pmaplist.add(pmap);
	    }
	}


	if (shouldSaveScripts && targetList != null) {
	    for (String arg: targetList) {
		TemplateProcessor.KeyMap map = new TemplateProcessor.KeyMap();
		URI uri;
		if (EPTS.maybeURL(arg)) {
		    URL url = new URL(fparent.toURI().toURL(), arg);
		    try {
			uri = url.toURI();
		    } catch(Exception e) {
			String msg = errorMsg("badURLtoURI", url.toString());
			System.err.println(msg);
			continue;
		    }
		} else {
		    File af = new File(arg);
		    uri = af.getCanonicalFile().toURI();
		}
		arg = fparent.toURI().relativize(uri).toString();
		/*
		File af = new File(arg);
		// File parent = f.getCanonicalFile().getParentFile();
		File afparent = af.getCanonicalFile().getParentFile();
		while (afparent != null) {
		    if (afparent.equals(fparent)) break;
		    afparent = afparent.if();
		}
		getParentFile (afparent == null) {
		    arg = af.getCanonicalFile().toURI().toString();
		} else {
		    // we use relative URLs if the image file is in a
		    // subdirectory of the directory containing the saved
		    // state.
		    arg = fparent.toURI()
			.relativize(af.getCanonicalFile().toURI()).toString();
		}
		*/
		map.put("arg", arg);
		tlist.add(map);
	    }
	}
	keymap.put("arglist", tlist);
	keymap.put("unitIndex",
		   String.format("%d", configGCSPane.savedUnitIndex));
	keymap.put("refPointIndex",
		   String.format("%d", configGCSPane.savedRefPointIndex));
	keymap.put("userSpaceDistance", configGCSPane.savedUsDistString);
	keymap.put("gcsDistance", configGCSPane.savedGcsDistString);
	keymap.put("xrefpoint", configGCSPane.savedXString);
	keymap.put("yrefpoint", configGCSPane.savedYString);

	keymap.put("width", String.format("%d", width));
	keymap.put("height", String.format("%d", height));
	TemplateProcessor.KeyMap km = ptmodel.getKeyMap((double)height);
	if (km != null && km.size() > 0) {
	    keymap.put("table", km);
	}
	if (ptfilters != null && ptfilters.size() > 0) {
	    keymap.put("hasFilterItems", emptyMap);
	    keymap.put("filterItems", ptfilters.getKeyMapList());
	}

	TemplateProcessor tp = new TemplateProcessor(keymap);
	File tf = File.createTempFile("epts-eptc", null, f.getParentFile());
	OutputStream os = new FileOutputStream(tf);
	Writer writer = new OutputStreamWriter(os, "UTF-8");
	tp.processSystemResource("org/bzdev/epts/save", "UTF-8", writer);
	String tfp = tf.getCanonicalPath();
	String fp = f.getCanonicalPath();
	if (!tf.renameTo(f)) {
	    throw new IOException(errorMsg("rename", tfp, fp));
	};
	needSave = false;
    }

    boolean needSave = false;
    TableModelListener tmlistener = new TableModelListener() {
	    public void tableChanged(TableModelEvent e) {
		needSave = true;
	    }
	};

    void setupTable(JComponent pane) {
	ptmodel = new PointTableModel(pane);
	ptmodel.addTableModelListener(tmlistener);
	ptable = new JTable(ptmodel);
	ptable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	ptable.setRowSelectionAllowed(true);
	ptable.setCellSelectionEnabled(false);
	ptable.setColumnSelectionAllowed(false);
	TableCellRenderer tcr = ptable.getDefaultRenderer(String.class);
	int w0 = 200;
	int w1 = 100;
	int w2 = 175;
	int w3 = 175;
	if (tcr instanceof DefaultTableCellRenderer) {
	    DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer) tcr;
	    FontMetrics fm = dtcr.getFontMetrics(dtcr.getFont());
	    w0 = 10 + fm.stringWidth("mmmmmmmmmmmmmmmmmmmm");
	    w1 = 10 + fm.stringWidth("CONTROL_POINT");
	    w2 = 10 + fm.stringWidth("mmmmmmmmmm");
	    w3 = w2;
	}
	TableColumnModel cmodel = ptable.getColumnModel();
	TableColumn column = cmodel.getColumn(0);
	column.setPreferredWidth(w0);
	column = cmodel.getColumn(1);
	column.setPreferredWidth(w1);
	column = cmodel.getColumn(2);
	column.setPreferredWidth(w2);
	column = cmodel.getColumn(3);
	column.setPreferredWidth(w3);

	tableFrame = new JFrame();
	tableFrame.setIconImages(EPTS.getIconList());
	tableFrame.setPreferredSize(new Dimension(800,600));
	JScrollPane tableScrollPane = new JScrollPane(ptable);
	tableScrollPane.setOpaque(true);
	ptable.setFillsViewportHeight(true);
	tableFrame.setContentPane(tableScrollPane);
  	tableFrame.pack();
    }

    JFrame manualFrame = null;
    HtmlWithTocPane manualPane = null;

    private void showManual() {
	if (manualFrame == null) {
	    manualFrame = new JFrame("Manual");
	    manualFrame.setIconImages(EPTS.getIconList());

	    Container pane = manualFrame.getContentPane();
	    manualPane = new HtmlWithTocPane();
	    manualFrame.setSize(920, 700);
	    manualFrame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			manualFrame.setVisible(false);
		    }
		});
	    JMenuBar menubar = new JMenuBar();
	    JMenu fileMenu = new JMenu(localeString("File"));
	    fileMenu.setMnemonic(KeyEvent.VK_F);
	    menubar.add(fileMenu);
	    JMenuItem menuItem = new JMenuItem(localeString("Close"),
					       KeyEvent.VK_C);
	    menuItem.setAccelerator(KeyStroke.getKeyStroke
				    (KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
	    menuItem.addActionListener(new ActionListener() {
		    public void  actionPerformed(ActionEvent e) {
			manualFrame.setVisible(false);
		    }
		});
	    fileMenu.add(menuItem);
	    manualFrame.setJMenuBar(menubar);
	    URL url = ClassLoader.getSystemClassLoader()
		.getResource("org/bzdev/epts/manual/manual.xml");
	    if (url != null) {
		try {
		    manualPane.setToc(url, true, false);
		    manualPane.setSelectionWithAction(0);
		} catch (IOException e) {
		    ErrorMessage.display(e);
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
		} catch (org.xml.sax.SAXException e) {
		    ErrorMessage.display(e);
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
		} catch(javax.xml.parsers.ParserConfigurationException e) {
		    ErrorMessage.display(e);
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
		}
	    } else {
		ErrorMessage.display("cannot load manual/manual.xml");
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
	    }
	    pane.setLayout(new BorderLayout());
	    pane.add(manualPane, "Center");
	}
	manualFrame.setVisible(true);
    }

    private void printManual() {
	try {
	    URL url = ClassLoader.getSystemClassLoader()
		.getResource("org/bzdev/epts/manual/manual.html");
	    if (url != null) {
		JEditorPane pane = new JEditorPane();
		pane.setPage(url);
		EditorKit ekit = pane.getEditorKit();
		if (ekit instanceof HTMLEditorKit) {
		    HTMLEditorKit hkit = (HTMLEditorKit)ekit;
		    StyleSheet stylesheet = hkit.getStyleSheet();
		    StyleSheet oursheet = new StyleSheet();
		    StringBuilder sb = new StringBuilder(512);
		    CopyUtilities.copyResource
			("org/bzdev/epts/manual/manual.css", sb,
			 Charset.forName("UTF-8"));
		    oursheet.addRule(sb.toString());
		    stylesheet.addStyleSheet(oursheet);
		}
		pane.print(null, new MessageFormat("- {0} -"));
	    }
	} catch  (PrinterException e) {
	} catch (IOException e) {
	}
    }

    static EmbeddedWebServer ews = null;
    static URI manualURI;
    public static synchronized void startManualWebServerIfNeeded()
	throws Exception
    {
	if (ews == null) {
	    ews = new EmbeddedWebServer(port, 48, 2, false);
	    if (port == 0) port = ews.getPort();
	    ews.add("/", ResourceWebMap.class, "org/bzdev/epts/manual/",
		    null, true, false, true);
	    WebMap wmap = ews.getWebMap("/");
	    if (wmap != null) {
		wmap.addWelcome("index.html");
		wmap.addMapping("html", "text/html; charset=utf-8");
	    }
	    manualURI = new URL("http://localhost:"
				+ port +"/manual.html").toURI();
	    ews.start();
	}
    }

    private void showManualInBrowser() {
	try {
	    startManualWebServerIfNeeded();
	    if (portTextField != null) {
		portTextField.setValue(port);
		portTextField.setEditable(false);
	    }
	    Desktop.getDesktop().browse(manualURI);
	    
	} catch (Exception e) {
	    setModeline(localeString("cannotOpenBrowser"));
	    e.printStackTrace();
	}
    }

    double xrefpoint = 0.0;
    double yrefpoint = 0.0;
    double scaleFactor = 1.0;

    double zoom = 1.0;
    int width;
    int height;
    double zoomTable[];
    int zoomIndex = -1;
    int initialZoomIndex = -1;


    public void setModeline(String line) {
	modeline.setText(" " + line);
    }

    ActionListener createLMIActionListener(final LocationFormat fmt) {
	return new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		resetState();
		setModeline("");
		JMenuItem item = (JMenuItem)(e.getSource());
		if (item.isSelected()) {
		    locationFormat = fmt;
		}
	    }
	};
    }

    // modeline for a given next state
    private void setModeline(Enum ns) {
	if (ns instanceof SplinePathBuilder.CPointType) {
	    SplinePathBuilder.CPointType pstate =
		(SplinePathBuilder.CPointType) ns;
	    switch (pstate) {
	    case MOVE_TO:
		setModeline(localeString("leftClickStart"));
		break;
	    case SPLINE:
		setModeline(localeString("leftClickSpline"));
		break;
	    case CONTROL:
		setModeline(localeString("leftClickControl"));
		break;
	    case SEG_END:
		setModeline(localeString("leftClickEnd"));
		break;
	    case CLOSE:
		setModeline(localeString("pathComplete"));
		break;
	    }
	} else if (ns instanceof EPTS.Mode) {
	    EPTS.Mode emode = (EPTS.Mode) ns;
	    switch (emode) {
	    case LOCATION:
		setModeline(localeString("locationCreated"));
		break;
	    case PATH_START:
		setModeline(localeString("pathStarting"));
		break;
	    case PATH_END:
		setModeline(localeString("pathComplete"));
		break;
	    }
	}
    }

    private void terminatePartialPath() {
	PointTMR lastrow = ptmodel.getLastRow();
	if (lastrow != null) {
	    Enum  lrmode = lastrow.getMode();
	    if (lrmode  instanceof SplinePathBuilder.CPointType) {
		// End the current partial path but do not remove it.
		if (lrmode == SplinePathBuilder.CPointType.CONTROL
		    || lrmode == SplinePathBuilder.CPointType.SPLINE) {
		    ptmodel.setLastRowMode
			(SplinePathBuilder.CPointType.SEG_END);
		    ttable.nextState(EPTS.Mode.PATH_END);
		    ptmodel.addRow("", EPTS.Mode.PATH_END,
				   0.0, 0.0, 0.0, 0.0);
		    setModeline("Path Complete");
		} else if (lrmode == SplinePathBuilder.CPointType.MOVE_TO) {
		    ptmodel.deleteRow(ptmodel.getRowCount()-1);
		    lastrow = ptmodel.getLastRow();
		    if (lastrow != null) {
			lrmode = lastrow.getMode();
			if (lrmode == EPTS.Mode.PATH_START) {
			    ptmodel.deleteRow(ptmodel.getRowCount()-1);
			}
		    }
		    setModeline("Partial Path Deleted");
		} else 	if (lrmode == SplinePathBuilder.CPointType.SEG_END) {
		    ttable.nextState(EPTS.Mode.PATH_END);
		    ptmodel.addRow("", EPTS.Mode.PATH_END,
				   0.0, 0.0, 0.0, 0.0);
		    setModeline("Path Complete");
		}
		resetState();
		panel.repaint();
	    } else if (lrmode == EPTS.Mode.PATH_START) {
		ptmodel.deleteRow(ptmodel.getRowCount()-1);
		setModeline("Partial Path Deleted");
		resetState();
		panel.repaint();
	    }
	}
	addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	deletePathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
    }

    private boolean cleanupPartialPath(boolean force) {
	PointTMR lastrow = ptmodel.getLastRow();
	if (lastrow != null
	    && lastrow.getMode() instanceof
	    SplinePathBuilder.CPointType) {
	    // check and remove the last partial path.
	    int lastrowInd = ptmodel.getRowCount()-1;
	    int result = (force)? JOptionPane.OK_OPTION:
		JOptionPane.showConfirmDialog(frame,
					      String.format
					      (localeString("confirmUndoCP"),
					       ptmodel.getRow(ptmodel.findStart
							      (lastrowInd))
					       .getVariableName()),
					      localeString("confirmCancelCP"),
					      JOptionPane
					      .OK_CANCEL_OPTION,
					      JOptionPane.
					      QUESTION_MESSAGE);
	    switch (result) {
	    case JOptionPane.OK_OPTION:
		{
		    int n = ptmodel.getRowCount();
		    if (n > 0) {
			do {
			    ptmodel.deleteRow(--n);
			    lastrow = ptmodel.getLastRow();
			} while (lastrow != null
				 && lastrow.getMode() instanceof
				 SplinePathBuilder.CPointType);
		    }
		    saveMenuItem.setEnabled(true);
		}
		break;
	    default:
		return false;
	    }
	}
	// expect only one iteration, but allow more in case of
	// an unanticipated error.
	while (lastrow != null
	    && lastrow.getMode() == EPTS.Mode.PATH_START) {
	    int n = ptmodel.getRowCount();
	    if (n > 0) {
		ptmodel.deleteRow(--n);
	    }
	    lastrow = ptmodel.getLastRow();
	}
	addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	deletePathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	return true;
    }

    JMenuItem saveMenuItem;
    JMenuItem saveAsMenuItem;
    JMenuItem addToPathMenuItem; // for Edit menu.
    JMenuItem deletePathMenuItem; // for Edit menu.
    JMenuItem makeCurrentMenuItem; // for Edit menu
    File savedFile = null;

    private void doSave(boolean mode) {
	try {
	    if (mode || savedFile == null) {
		File cdir = new File(System.getProperty("user.dir"))
		    .getCanonicalFile();
		JFileChooser chooser = new JFileChooser(cdir);
		FileNameExtensionFilter filter =
		    new FileNameExtensionFilter
		    (localeString("EptsState"), "epts");
		chooser.setFileFilter(filter);
		chooser.setSelectedFile(savedFile);
		int status = chooser.showSaveDialog(frame);
		if (status == JFileChooser.APPROVE_OPTION) {
		    savedFile = chooser.getSelectedFile();
		    String name = savedFile.getName();
		    if (!(name.endsWith(".epts")
			  || name.endsWith(".EPTS"))) {
			savedFile =
			    new File(savedFile.getParentFile(),
				     name + ".epts");
		    }
		} else {
		    return;
		}
	    }
	    if (!savedFile.exists()) {
		save(savedFile);
	    } else {
		File parent = savedFile.getCanonicalFile().getParentFile();
		File tmp = File.createTempFile("eptstmp", ".epts",
					       parent);
		// tmp.deleteOnExit(); // make sure it goes away even if errors.
		save(tmp);
		File backup = new File(savedFile.getCanonicalPath() + "~");
		if (backup.exists()) backup.delete();
		savedFile.renameTo(backup);
		tmp.renameTo(savedFile);
	    }
	    createTemplateMenuItem.setEnabled(true);
	} catch (Exception ee) {
	    JOptionPane.showMessageDialog(frame,
					  errorMsg("saveFailed",
						   savedFile,
						   ee.getMessage()),
					  localeString("errorTitle"),
					  JOptionPane.ERROR_MESSAGE);
	}
    }

    private ActionListener quitListener = new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (needSave) {
		    int result = JOptionPane.showConfirmDialog
			(frame, localeString ("confirmSave"),
			 localeString("confirmCancelSave"),
			 JOptionPane.YES_NO_CANCEL_OPTION,
			 JOptionPane.QUESTION_MESSAGE);
		    switch (result) {
		    case JOptionPane.OK_OPTION:
			doSave(false);
			break;
		    case JOptionPane.CANCEL_OPTION:
			return;
		    default:
			break;
		    }
		}
		System.exit(0);
	    }
	};

    private void acceptConfigParms() {
	// must be called after width and height are set.
	scaleFactor = configGCSPane.getScaleFactor();
	xrefpoint = configGCSPane.getXRefpoint();
	yrefpoint = configGCSPane.getYRefpoint();
	switch(configGCSPane.getRefPointName()) {
	case CENTER:
	    xrefpoint -= width*scaleFactor / 2;
	    yrefpoint -= height*scaleFactor / 2;
	    break;
	case CENTER_LEFT:
	    yrefpoint -= height*scaleFactor / 2;
	    break;
	case CENTER_RIGHT:
	    xrefpoint -= width*scaleFactor;
	    yrefpoint -= height*scaleFactor / 2;
	    break;
	case LOWER_CENTER:
	    xrefpoint -= width*scaleFactor / 2;
	    break;
	case LOWER_LEFT:
	    break;
	case LOWER_RIGHT:
	    xrefpoint -= width*scaleFactor;
	    break;
	case UPPER_CENTER:
	    xrefpoint -= width*scaleFactor / 2;
	    yrefpoint -= height*scaleFactor;
	    break;
	case UPPER_LEFT:
	    yrefpoint -= height*scaleFactor;
	    break;
	case UPPER_RIGHT:
	    xrefpoint -= width*scaleFactor;
	    yrefpoint -= height*scaleFactor;
	    break;
	}

    }

    boolean restartOptionShown = false;

    JMenuItem quitMenuItem;
    JMenuItem configMenuItem;
    JMenuItem portMenuItem = null;
    PortTextField portTextField = null;
    JMenuItem webMenuItem = null;
    JMenuItem createTemplateMenuItem = null;

    static final String OK_CANCEL[] = {
	localeString("OK"),
	localeString("Cancel")
    };

    PTFilters ptfilters = null;


    private void setMenus(JFrame frame, double w, double h) {
	JMenuBar menubar = new JMenuBar();
	JMenuItem menuItem;
	JMenu fileMenu = new JMenu(localeString("File"));
	fileMenu.setMnemonic(KeyEvent.VK_F);
	menubar.add(fileMenu);
	quitMenuItem = new JMenuItem(localeString("Quit"), KeyEvent.VK_Q);
	quitMenuItem.setAccelerator(KeyStroke.getKeyStroke
				    (KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
	quitMenuItem.addActionListener(quitListener);
	fileMenu.add(quitMenuItem);
	saveMenuItem = new JMenuItem(localeString("Save"), KeyEvent.VK_S);
	saveMenuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
	saveMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doSave(false);
		}
	    });
	fileMenu.add(saveMenuItem);

	saveAsMenuItem = new JMenuItem(localeString("SaveAs"), KeyEvent.VK_A);
	saveAsMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doSave(true);
		}
	    });
	fileMenu.add(saveAsMenuItem);


	menuItem = new JMenuItem(localeString("ConfigureGCS"), KeyEvent.VK_C);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    configGCSPane.saveState();
		     int status = JOptionPane.showConfirmDialog
			(frame, configGCSPane,
			 localeString("ConfigureGCSDialog"),
			 JOptionPane.OK_CANCEL_OPTION);
		     if (status == JOptionPane.OK_OPTION) {
			 double oldScaleFactor = scaleFactor;
			 double oldXrefpoint = xrefpoint;
			 double oldYrefpoint = yrefpoint;
			 acceptConfigParms();
			 if (ptmodel.getRowCount() > 0) {
			     for (PointTMR row: ptmodel.getRows()) {
				 Enum rmode = row.getMode();
				 if (rmode == EPTS.Mode.LOCATION
				     || (rmode instanceof
					 SplinePathBuilder.CPointType
					 && rmode !=
					 SplinePathBuilder.CPointType.CLOSE)) {
				     double xp = row.getXP();
				     double yp = row.getYP();
				    
				     double x = xp * scaleFactor;
				     double y = yp * scaleFactor;
				     x += xrefpoint;
				     y += yrefpoint;
				     row.setX(x, xp);
				     row.setY(y, yp);
				 }
			     }
			     ptmodel.fireXYChanged();
			 }
			 if (se != null) {
			     if (oldScaleFactor != scaleFactor
				 || oldXrefpoint != xrefpoint
				 || oldYrefpoint != yrefpoint) {
				 // issue warning that parameters changed
				 // and the program may have to be restarted.
				 String options[] = {
				     localeString("saveAndQuit"),
				     localeString("cancelChanges"),
				     localeString("continueRunning"),
				 };
				 int option = restartOptionShown? 2:
				     JOptionPane.showOptionDialog
				     (frame, localeString("restart"),
				      localeString("restartTitle"),
				      JOptionPane.DEFAULT_OPTION,
				      JOptionPane.WARNING_MESSAGE,
				      null, options, options[0]);
				 switch (option) {
				 case 0:
				     doSave(false);
				     System.exit(1);
				     break;
				 case 1:
				     configGCSPane.restoreState();
				     scaleFactor = oldScaleFactor;
				     xrefpoint = oldXrefpoint;
				     yrefpoint = oldYrefpoint;
				     if (ptmodel.getRowCount() > 0) {
					 for (PointTMR row: ptmodel.getRows()) {
					     Enum rmode = row.getMode();
					     if (rmode == EPTS.Mode.LOCATION
						 || (rmode instanceof
						     SplinePathBuilder
						     .CPointType
						     && rmode !=
						     SplinePathBuilder
						     .CPointType.CLOSE)) {
						 double xp = row.getXP();
						 double yp = row.getYP();
						 double x = xp * scaleFactor;
						 double y = yp * scaleFactor;
						 x += xrefpoint;
						 y += yrefpoint;
						 row.setX(x, xp);
						 row.setY(y, yp);
					     }
					 }
					 ptmodel.fireXYChanged();
				     }
				     break;
				 case JOptionPane.CLOSED_OPTION:
				 case 2:
				     try {
					 double xo =
					     configGCSPane.getXRefpoint();
					 double yo =
					     configGCSPane.getYRefpoint();
					 RefPointName rpn =
					     configGCSPane.getRefPointName();
					 se.rescale(scaleFactor, rpn, xo, yo);
					 restartOptionShown = true;
				     } catch (IllegalStateException ise) {
				     }
				     panel.repaint();
				     break;
				 }
			     }
			 }
		     } else {
			 // User canceled the changes, so we restore
			 // the state to what it was before the
			 // dialog box was opened.
			 configGCSPane.restoreState();
		     }
		}
	    });
	configMenuItem = menuItem;
	fileMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("PrintTable"), KeyEvent.VK_P);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			ptable.print(JTable.PrintMode.FIT_WIDTH,
				     null,
				     new MessageFormat("- {0} -"));
		    } catch (PrinterException ee) {
		    }
		}
	    });
	fileMenu.add(menuItem);


	menuItem = new JMenuItem(localeString("createTemplate"), KeyEvent.VK_T);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			if (savedFile != null) {
			    new ProcessBuilder("java", "-p",
					       EPTS.initialModulePath,
					       "-m",
					       EPTS.EPTSmodule,
					       "--templateConfig",
					       savedFile.getCanonicalPath())
				.start();
			}
		    } catch (Exception ex) {
			JOptionPane.showMessageDialog
			    (frame, errorMsg("TPfailed", ex.getMessage()),
			 "Error", JOptionPane.ERROR_MESSAGE);
		    }
		}
	    });
	menuItem.setEnabled(false);
	fileMenu.add(menuItem);
	createTemplateMenuItem = menuItem;

	JMenu editMenu = new JMenu(localeString("Edit"));
	editMenu.setMnemonic(KeyEvent.VK_E);
	menubar.add(editMenu);

	menuItem = new JMenuItem(localeString("UndoPointInsertion"),
				 KeyEvent.VK_U);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int n = ptmodel.getRowCount();
		    if (n > 0) {
			ptmodel.deleteRow(--n);
			if (n == 0) {
			    resetState();
			    setModeline("");
			} else {
			    PointTMR row = ptmodel.getRow(--n);
			    Enum mode = row.getMode();
			    if (mode instanceof EPTS.Mode) {
				EPTS.Mode emode = (EPTS.Mode) mode;
				switch (emode) {
				case LOCATION:
				    resetState();
				    setModeline("");
				    break;
				case PATH_END:
				    resetState();
				    setModeline("");
				    break;
				case PATH_START:
				    createPathFinalAction();
				    break;
				}
			    } else if (mode instanceof
				       SplinePathBuilder.CPointType) {
				SplinePathBuilder.CPointType smode =
				    (SplinePathBuilder.CPointType) mode;
				if (ttable == null) {
				    // reconstruct the last path up to
				    // the point where the path was about
				    // to be terminated.
				    savedCursorPath = panel.getCursor();
				    panel.setCursor
					(Cursor.getPredefinedCursor
					 (Cursor.CROSSHAIR_CURSOR));
				    ttable = new TransitionTable();

				}
				// This calls ttable.nextState,
				// which calls a menu item's action
				// listener's actionPeformed
				// method to set the nextState variable
				// for the radio-button menu item cases.
				ttable.setState(ptmodel, n);
			    }
			}
		    }
		    if (nextState != null) {
			saveMenuItem.setEnabled(false);
			saveAsMenuItem.setEnabled(false);
		    }
		    panel.repaint();
		}
	    });
	editMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("AppendBezier"), KeyEvent.VK_A);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
	addToPathMenuItem = menuItem;
	menuItem.setEnabled(false);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // select a path.
		    int index = -1;
		    if (selectedRow == -1) {
			Set<String> vnameSet = ptmodel.getPathVariableNames();
			if (vnameSet.isEmpty()) return;
			String[] vnames =
			    vnameSet.toArray(new String[vnameSet.size()]);
			String vname;
			if (vnames.length == 1) {
			    vname = vnames[0];
			} else {
			    vname = (String)JOptionPane.showInputDialog
				(frame, localeString("SelectPathExtend"),
				 localeString("SelectPath"),
				 JOptionPane.PLAIN_MESSAGE, null,
				 vnames, vnames[0]);
			}
			if (vname == null || vname.length() == 0) return;
			index = ptmodel.findStart(vname);
		    }
		    // complete the current path.
		    if (nextState != null) {
			PointTMR lastrow = ptmodel.getLastRow();
			Enum lrmode = lastrow.getMode();
			if (lrmode == SplinePathBuilder.CPointType
			    .CONTROL || lrmode ==
			    SplinePathBuilder.CPointType.SPLINE) {
			    ptmodel.setLastRowMode
				(SplinePathBuilder.CPointType.SEG_END);
			}
			// This can call a radio-button menu item's
			// action listener's actionPeformed
			// method, and that sets the
			// nextState variable.
			ttable.nextState(EPTS.Mode.PATH_END);
			ptmodel.addRow("", EPTS.Mode.PATH_END,
				       0.0, 0.0, 0.0, 0.0);
		    }
		    resetState();
		    if (selectedRow == -1) {
			if (index != -1) index++;
			selectedRow = index;
		    }
		    onExtendPath();
		}
	    });
	editMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("DeleteBezier"), KeyEvent.VK_D);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK));
	deletePathMenuItem = menuItem;
	menuItem.setEnabled(false);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // select a path.
		    int index = -1;
		    if (selectedRow == -1) {
			Set<String> vnameSet = ptmodel.getVariableNames();
			if (vnameSet.isEmpty()) return;
			String[] vnames =
			    vnameSet.toArray(new String[vnameSet.size()]);
			String vname;
			if (vnames.length == 1) {
			    vname = vnames[0];
			} else {
			    vname = (String)JOptionPane.showInputDialog
				(frame, localeString("SelectPathExtend"),
				 localeString("SelectPath"),
				 JOptionPane.PLAIN_MESSAGE, null,
				 vnames, vnames[0]);
			}
			if (vname == null || vname.length() == 0) return;
			index = ptmodel.findStart(vname);
		    } else {
			index = ptmodel.findStart(selectedRow);
			if (JOptionPane.OK_OPTION !=
			    JOptionPane.showConfirmDialog
			    (frame, String.format(localeString("confirmDel"),
						  ptmodel.getRow(index)
						  .getVariableName()),
			     localeString("confirmDelCPL"),
			     JOptionPane.OK_CANCEL_OPTION,
			     JOptionPane.QUESTION_MESSAGE)) {
			    return;
			}
		    }
		    int istart = ptmodel.findStart(index);
		    int iend = ptmodel.findEnd(index);
		    if (iend == -1) {
			// we reached the end of a path that had not been
			// completed.
			iend = ptmodel.getRowCount() - 1;
		    }
		    iend += 1;
		    ptmodel.deleteRows(istart, iend);
		    panel.repaint();
		}
	    });
	editMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("MakeCurrent"),
				 KeyEvent.VK_S);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int index = -1;
		    if (selectedRow == -1) {
			Set<String> vnameSet = ptmodel.getVariableNames();
			if (vnameSet.isEmpty()) return;
			String vname = null;
			String[] vnames =
			    vnameSet.toArray(new String[vnameSet.size()]);
			if (vnames.length == 1) {
			    vname = vnames[0];
			} else {
			    vname = (String)JOptionPane.showInputDialog
				(frame, localeString("SelectPathLoc"),
				 localeString("SelectPathLocTitle"),
				 JOptionPane.PLAIN_MESSAGE, null, vnames,
				 vnames[0]);
			}
			if (vname == null || vname.length() == 0) return;
			index = ptmodel.findStart(vname);
		    } else {
			index = ptmodel.findStart(selectedRow);
		    }
		    if (index != -1) {
			int offset = ptmodel.moveToEnd(index);
			if (selectedRow > -1) {
			    selectedRow += offset;
			}
		    }
		}
	    });
	makeCurrentMenuItem = menuItem;
	menuItem.setEnabled(false);
	editMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("CopyTableECMAScript"),
				 KeyEvent.VK_E);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			StringWriter writer = new
			    StringWriter(128*ptmodel.getRowCount());
			TemplateProcessor tp =
			    new TemplateProcessor(ptmodel.getKeyMap((double)
								    height));
			tp.processSystemResource("org/bzdev/epts/ECMAScript",
						 "UTF-8", writer);
			Clipboard cb = panel.getToolkit().getSystemClipboard();
			StringSelection selection =
			    new StringSelection(writer.toString());
			cb.setContents(selection, selection);
		    } catch (IOException ee) {
			System.err.println(errorMsg("ECMAExport"));
			ee.printStackTrace(System.err);
			System.exit(1);
		    }
		}
	    });
	editMenu.add(menuItem);
	
	editMenu.addSeparator();
	editMenu.add(new JLabel(localeString("LocationFormat")));
	ButtonGroup bg = new ButtonGroup();
	menuItem = new JRadioButtonMenuItem(localeString("LocationFormat1"));
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.COORD));
	bg.add(menuItem);
	editMenu.add(menuItem);
	menuItem = new JRadioButtonMenuItem(localeString("LocationFormat2"));
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.EXPRESSION));
	bg.add(menuItem);
	editMenu.add(menuItem);
	menuItem = new JRadioButtonMenuItem(localeString("LocationFormat3"));
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.OBJECT));
	bg.add(menuItem);
	editMenu.add(menuItem);
	menuItem = new JRadioButtonMenuItem(localeString("LocationFormat4"));
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.STATEMENT));
	menuItem.setSelected(true);
	bg.add(menuItem);
	editMenu.add(menuItem);
	editMenu.addSeparator();
	menuItem = new JCheckBoxMenuItem(localeString("dragImageMode"), true);
	menuItem.setMnemonic(KeyEvent.VK_D);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    toggledAltD = ((JCheckBoxMenuItem)e.getSource())
			.isSelected();
		}
	    });
	editMenu.add(menuItem);

	JMenu zoomMenu = new JMenu(localeString("Zoom"));
	zoomMenu.setMnemonic(KeyEvent.VK_Z);
	menubar.add(zoomMenu);

	menuItem = new JMenuItem(localeString("Reset"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int lastIndex = zoomIndex;
		    zoomIndex = initialZoomIndex;
		    if (lastIndex != zoomIndex) {
			rezoom();
		    }
		}
	    });
	zoomMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("ZoomIn"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_EQUALS,
				 InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int lastIndex = zoomIndex;
		    zoomIndex++;
		    if (zoomIndex >= zoomTable.length) {
			zoomIndex = zoomTable.length -1;
		    }
		    if (lastIndex != zoomIndex) {
			rezoom();
		    }
		}
	    });
	zoomMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("ZoomOut"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int lastIndex = zoomIndex;
		    zoomIndex--;
		    if (zoomIndex < 0) {
			zoomIndex = 0;
		    }
		    if (lastIndex != zoomIndex) {
			rezoom();
		    }
		}
	    });
	zoomMenu.add(menuItem);

	zoomMenu.addSeparator();

	LinkedList<Integer> exlist = new LinkedList<>();
	exlist.add(0);
	exlist.add(1);
	exlist.add(2);
	exlist.add(3);
	exlist.add(4);
	int max = Math.max((int)Math.round(width/w), (int)Math.round(height/h));
	int i = 0;
	while (max > 0) {
	    exlist.addFirst(--i);
	    max = max >>> 1;
	}
	zoomTable = new double[exlist.size()];
	i = 0;
	for (int exponent: exlist) {
	    zoomTable[i] = Functions.pow(2.0, exponent);
	    if (exponent < 0) {
		menuItem = new JMenuItem
		    (String.format("1/%d", Functions.lPow(2,-exponent)));

	    } else if (exponent == 0) {
		zoomIndex = i;
		initialZoomIndex = i;
		menuItem = new JMenuItem("1.0");
	    } else {
		menuItem = new JMenuItem
		    (String.format("%d", Functions.lPow(2,exponent)));
	    }
	    final int index = i;
	    menuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			int lastIndex = zoomIndex;
			zoomIndex = index;
			if (zoomIndex != lastIndex) {
			    rezoom();
			}
		    }
		});
	    zoomMenu.add(menuItem);
	    i++;
	}

	JMenu measureMenu = new JMenu(localeString("MeasureDistance"));
	measureMenu.setMnemonic(KeyEvent.VK_M);
	menubar.add(measureMenu);
	menuItem = new JMenuItem(localeString("ImageSpaceDistance"),
				 KeyEvent.VK_I);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    measureDistance(false);
		}
	    });
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
	measureMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("GCSDistance"),
				 KeyEvent.VK_G);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_G, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    measureDistance(true);
		}
	    });
	measureMenu.add(menuItem);

	JMenu toolMenu = new JMenu(localeString("Tools"));
	toolMenu.setMnemonic(KeyEvent.VK_T);
	menubar.add(toolMenu);
 
	menuItem = new JMenuItem(localeString("ShowPointTable"), KeyEvent.VK_S);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    tableFrame.setVisible(true);
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("CreatePoint"), KeyEvent.VK_P);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		long pointIndex = 1;
		public void actionPerformed(ActionEvent e) {
		    terminatePartialPath();
		    // if (!cleanupPartialPath(false)) return;
		    if (locationFormat == LocationFormat.STATEMENT) {
			varname = JOptionPane.showInputDialog
			    (frame, localeString("PleaseEnterVariableName"),
			     localeString("ScriptingLanguageVariableName"),
			     JOptionPane.PLAIN_MESSAGE);
			if (varname == null) return;
			varname = varname.trim();
			if (varname.length() == 0) {
			    varname = "pt" + (pointIndex++);
			    while (ptmodel.getVariableNames()
				   .contains(varname)) {
				varname = "pt" + (pointIndex++);
			    }
			}
			if (ptmodel.getVariableNames().contains(varname)) {
			    JOptionPane.showMessageDialog
				(frame,
				 errorMsg("nameInUse", varname),
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			    return;
			}
		    }
		    createLocation();
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("CreateBezierPath"),
				 KeyEvent.VK_B);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    terminatePartialPath();
		    // if (!cleanupPartialPath(false)) return;
		    createPath();
		}
	    });
	toolMenu.add(menuItem);


	toolMenu.addSeparator();
	toolMenu.add(new JLabel(localeString("BezierPathOptions")));
	JMenuItem lastMI = null;
	for (TransitionTable.Pair pair:
		 TransitionTable.getMenuItemsWithState()) {
	    JMenuItem mi = pair.getMenuItem();
	    final Enum state = pair.getState();
	    if (lastMI != null) {
		if ((lastMI instanceof JRadioButtonMenuItem)
		    != (mi instanceof JRadioButtonMenuItem)) {
		    toolMenu.addSeparator();
		    if (!(mi instanceof JRadioButtonMenuItem)) {
		    toolMenu.add
			(new JLabel(localeString("OptionsToEndBezierPath")));
		    }
		}
	    }
	    lastMI = mi;
	    toolMenu.add(mi);
	    mi.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			nextState = state;
			if (state instanceof SplinePathBuilder.CPointType) {
			    SplinePathBuilder.CPointType pstate =
				(SplinePathBuilder.CPointType) state;
			    setModeline(pstate);
			    if (pstate == SplinePathBuilder.CPointType.CLOSE) {
				ptmodel.addRow("", SplinePathBuilder.CPointType
					       .CLOSE, 0.0, 0.0, 0.0, 0.0);
				ptmodel.addRow("", EPTS.Mode.PATH_END,
					       0.0, 0.0, 0.0, 0.0);
				// setModeline("Path Complete");
				resetState();
				setModeline(localeString("LoopComplete"));

			    } else {
				JRadioButtonMenuItem rbmi =
				    (JRadioButtonMenuItem)
				    TransitionTable.getMenuItem(pstate);
				rbmi.setSelected(true);
			    }
			} else if (state instanceof EPTS.Mode) {
			    EPTS.Mode mstate = (EPTS.Mode) state;
			    switch (mstate) {
			    case PATH_END:
				PointTMR lastrow = ptmodel.getLastRow();
				Enum lrmode = lastrow.getMode();
				if (lrmode == SplinePathBuilder.CPointType
				    .CONTROL || lrmode ==
				    SplinePathBuilder.CPointType.SPLINE) {
				    ptmodel.setLastRowMode
					(SplinePathBuilder.CPointType.SEG_END);
				}
				// This can call a radio-button menu item's
				// action listener's actionPeformed
				// actionPerformed method, and that sets the
				// nextState variable.
				ttable.nextState(EPTS.Mode.PATH_END);
				ptmodel.addRow("", EPTS.Mode.PATH_END,
					       0.0, 0.0, 0.0, 0.0);
			    }
			    setModeline("Path Complete");
			    resetState();
			    panel.repaint();
			}
		    }
		});
	}
	toolMenu.addSeparator();
	toolMenu.add(new JLabel(localeString("bezierOps")));
	menuItem = TransitionTable.getLocMenuItem();
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    final LocPane lpane = new LocPane();
		    if (selectedRow != -1) {
			PointTMR row = ptmodel.getRow(selectedRow);
			if (row != null) {
			    lpane.setXCoord(row.getX());
			    lpane.setYCoord(row.getY());
			}
		    }
		    lpane.addAncestorListener(new AncestorListener() {
			    @Override
			    public void ancestorAdded(AncestorEvent e) {
				lpane.xtf.requestFocusInWindow();
			    }
			    @Override
			    public void ancestorMoved(AncestorEvent e) {}
			    @Override
			    public void ancestorRemoved(AncestorEvent e) {}
			});
		    int status = JOptionPane.showConfirmDialog
			(frame, lpane, "Add Vector-Specified Line Segment",
			 JOptionPane.OK_CANCEL_OPTION,
			 JOptionPane.QUESTION_MESSAGE);
		    if (status == 0) {
			lpane.saveIndices();
			double x = lpane.getXCoord();
			double y = lpane.getYCoord();
			double xp = (x - xrefpoint) / scaleFactor;
			double yp = (y - yrefpoint) / scaleFactor;
			yp = height - yp;
			if (xp < 0.0 || xp > width || yp < 0.0 || yp > height) {
			    JOptionPane.showMessageDialog
				(frame, "New point out of range",
				 "Error", JOptionPane.ERROR_MESSAGE);
			    return;
			}
			if (locState) {
			    if (selectedRow != -1) {
				ptmodel.changeCoords(selectedRow,
						     x, y, xp, yp, true);
			    } else {
				ptmodel.addRow(varname, EPTS.Mode.LOCATION,
					       x, y, xp, yp);
			    }
			    locState = false;
			    setModeline("");
			    TransitionTable.getLocMenuItem().setEnabled(false);
			    resetState();
			} else {
			    if (selectedRow != -1) {
				ptmodel.changeCoords(selectedRow,
						     x, y, xp, yp, true);
				selectedRow = -1;
			    } else {
				ptmodel.addRow("", nextState, x, y, xp, yp);
			    }
			    if (nextState != null) {
				ttable.nextState(nextState);
				JRadioButtonMenuItem rbmi =
				    (JRadioButtonMenuItem)
				    TransitionTable.getMenuItem(nextState);
				rbmi.setSelected(true);
			    } else {
				resetState();
				TransitionTable.getLocMenuItem()
				    .setEnabled(false);
			    }
			}
			JViewport vp = scrollPane.getViewport();
			int vpw = vp.getWidth();
			int vph = vp.getHeight();
			int ipx = (int)(Math.round(xp - vpw/2));
			int ipy = (int)(Math.round(yp - vph/2));
			if (ipx < 0) ipx = 0;
			if (ipy < 0) ipy = 0;
			vp.setViewPosition(new Point(ipx, ipy));
			scrollPane.repaint();
		    }
		}
	    });
	toolMenu.add(menuItem);
	menuItem = TransitionTable.getVectorMenuItem();
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    final VectorPane vpane = new VectorPane();
		    vpane.addAncestorListener(new AncestorListener() {
			    @Override
			    public void ancestorAdded(AncestorEvent e) {
				vpane.ltf.requestFocusInWindow();
			    }
			    @Override
			    public void ancestorMoved(AncestorEvent e) {}
			    @Override
			    public void ancestorRemoved(AncestorEvent e) {}
			});
		    int status = JOptionPane.showConfirmDialog
			(frame, vpane, "Add Vector-Specified Line Segment",
			 JOptionPane.OK_CANCEL_OPTION,
			 JOptionPane.QUESTION_MESSAGE);
		    if (status == 0) {
			vpane.saveIndices();
			double length = vpane.getLength();
			double angle = vpane.getAngle();
			PointTMR row = ptmodel.getLastRow();
			double x = row.getX() + length * Math.cos(angle);
			double y = row.getY() + length * Math.sin(angle);
			/*
			if (x < graph.getXLower() || x > graph.getXUpper()
			    || y < graph.getYLower() || y > graph.getYUpper()) {
			    // point out of range.
			    JOptionPane.showMessageDialog
				(frame, "New point out of range",
				 "Error", JOptionPane.ERROR_MESSAGE);
			    return;
			}
			*/
			double xp = (x - xrefpoint) / scaleFactor;
			double yp = (y - yrefpoint) / scaleFactor;
			yp = height - yp;
			if (xp < 0.0 || xp > width || yp < 0.0 || yp > height) {
			    JOptionPane.showMessageDialog
				(frame, "New point out of range",
				 "Error", JOptionPane.ERROR_MESSAGE);
			    return;
			}
			ptmodel.addRow("", SplinePathBuilder.CPointType.SEG_END,
				       x, y, xp, yp);
			ttable.nextState(SplinePathBuilder.CPointType.SEG_END);
			JRadioButtonMenuItem rbmi = (JRadioButtonMenuItem)
			    TransitionTable.getMenuItem
			    (SplinePathBuilder.CPointType.SEG_END);
			rbmi.setSelected(true);
			JViewport vp = scrollPane.getViewport();
			int vpw = vp.getWidth();
			int vph = vp.getHeight();
			int ipx = (int)(Math.round(xp - vpw/2));
			int ipy = (int)(Math.round(yp - vph/2));
			if (ipx < 0) ipx = 0;
			if (ipy < 0) ipy = 0;
			vp.setViewPosition(new Point(ipx, ipy));
			scrollPane.repaint();
		    }
		}
	    });
	toolMenu.add(menuItem);
	menuItem = TransitionTable.getArcMenuItem();
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    final ArcPane apane = new ArcPane();
		    apane.addAncestorListener(new AncestorListener() {
			    @Override
			    public void ancestorAdded(AncestorEvent e) {
				apane.rtf.requestFocusInWindow();
			    }
			    @Override
			    public void ancestorMoved(AncestorEvent e) {}
			    @Override
			    public void ancestorRemoved(AncestorEvent e) {}
			});
		    int status = JOptionPane.showConfirmDialog
			(frame, apane, "Add an Arc",
			 JOptionPane.OK_CANCEL_OPTION,
			 JOptionPane.QUESTION_MESSAGE);
		    if (status == 0) {
			apane.saveIndices();
			double radius = apane.getRadius();
			if (radius == 0.0) return;
			double angle = apane.getAngle();
			double maxdelta = apane.getMaxDelta();
			boolean ccw = apane.isCounterClockwise();
			int lastind = ptmodel.getRowCount()-1;
			int prevind = lastind - 1;
			PointTMR row2 = ptmodel.getRow(lastind);
			PointTMR row1 = ptmodel.getRow(prevind);
			Enum<?> mode = row1.getMode();
			if (mode instanceof SplinePathBuilder.CPointType) {
			    SplinePathBuilder.CPointType type =
				(SplinePathBuilder.CPointType) mode;
			    SplinePath2D segment = new SplinePath2D();
			    switch(type) {
			    case MOVE_TO:
			    case SEG_END:
			    case CONTROL:
				segment.moveTo(row1.getX(), row1.getY());
				segment.lineTo(row2.getX(), row2.getY());
				break;
			    case SPLINE:
				SplinePathBuilder sb = new SplinePathBuilder();
				LinkedList<SplinePathBuilder.CPoint> cpoints =
				    new LinkedList<>();
				cpoints.add(new SplinePathBuilder.CPoint
					    ((SplinePathBuilder.CPointType)
					     row2.getMode(),
					     row2.getX(), row2.getY()));
				prevind = lastind;
				do {
				    prevind--;
				    row1 = ptmodel.getRow(prevind);
				    mode = row1.getMode();
				    cpoints.add(new SplinePathBuilder.CPoint
						((SplinePathBuilder.CPointType)
						 mode,
						 row1.getX(), row1.getY()));
				} while (mode ==
					 SplinePathBuilder.CPointType.SPLINE);
				Collections.reverse(cpoints);
				sb.initPath();
				sb.append(cpoints);
				segment = sb.getPath();
				break;
			    default:
				throw new Error("bad case: " + type);
			    }
			    Path2D arc = Paths2D.createArc(segment, radius,
							   ccw, angle,
							   maxdelta);
			    PathIterator pi = arc.getPathIterator(null);
			    double[] coords = new double[6];
			    int ptype = pi.currentSegment(coords);
			    if (ptype != PathIterator.SEG_MOVETO) {
				throw new Error();
			    }
			    pi.next(); // first is a MOVE_TO segment
			    double x, y, xp, yp;
			    do {
				ptype = pi.currentSegment(coords);
				if (ptype != PathIterator.SEG_CUBICTO) {
				    throw new Error();
				}
				x = coords[0];
				y = coords[1];
				xp = (x - xrefpoint) / scaleFactor;
				yp = (y - yrefpoint) / scaleFactor;
				yp = height - yp;
				if (xp < 0.0 || xp > width
				    || yp < 0.0 || yp > height) {
				    JOptionPane.showMessageDialog
					(frame, "New point out of range",
					 "Error", JOptionPane.ERROR_MESSAGE);
				    return;
				}
				x = coords[2];
				y = coords[3];
				xp = (x - xrefpoint) / scaleFactor;
				yp = (y - yrefpoint) / scaleFactor;
				yp = height - yp;
				if (xp < 0.0 || xp > width
				    || yp < 0.0 || yp > height) {
				    JOptionPane.showMessageDialog
					(frame, "New point out of range",
					 "Error", JOptionPane.ERROR_MESSAGE);
				    return;
				}
				x = coords[4];
				y = coords[5];
				xp = (x - xrefpoint) / scaleFactor;
				yp = (y - yrefpoint) / scaleFactor;
				yp = height - yp;
				if (xp < 0.0 || xp > width
				    || yp < 0.0 || yp > height) {
				    JOptionPane.showMessageDialog
					(frame, "New point out of range",
					 "Error", JOptionPane.ERROR_MESSAGE);
				    return;
				}
				// Now repeat but set the row instead of
				// testing the values.
				x = coords[0];
				y = coords[1];
				xp = (x - xrefpoint) / scaleFactor;
				yp = (y - yrefpoint) / scaleFactor;
				yp = height - yp;
				ptmodel.addRow("",
					       SplinePathBuilder.CPointType
					       .CONTROL,
					       x, y, xp, yp);
				x = coords[2];
				y = coords[3];
				xp = (x - xrefpoint) / scaleFactor;
				yp = (y - yrefpoint) / scaleFactor;
				yp = height - yp;
				ptmodel.addRow("",
					       SplinePathBuilder.CPointType
					       .CONTROL,
					       x, y, xp, yp);
				x = coords[4];
				y = coords[5];
				xp = (x - xrefpoint) / scaleFactor;
				yp = (y - yrefpoint) / scaleFactor;
				yp = height - yp;
				ptmodel.addRow("",
					       SplinePathBuilder.CPointType
					       .SEG_END,
					       x, y, xp, yp);
				pi.next();
			    } while (!pi.isDone());
			    ttable.nextState
				(SplinePathBuilder.CPointType.SEG_END);
			    JRadioButtonMenuItem rbmi = (JRadioButtonMenuItem)
				TransitionTable.getMenuItem
				(SplinePathBuilder.CPointType.SEG_END);
			    rbmi.setSelected(true);
			    JViewport vp = scrollPane.getViewport();
			    int vpw = vp.getWidth();
			    int vph = vp.getHeight();
			    int ipx = (int)(Math.round(xp - vpw/2));
			    int ipy = (int)(Math.round(yp - vph/2));
			    if (ipx < 0) ipx = 0;
			    if (ipy < 0) ipy = 0;
			    vp.setViewPosition(new Point(ipx, ipy));
			    scrollPane.repaint();
			}
		    }
		}
	    });
	toolMenu.add(menuItem);

	final JMenu filterMenu = new JMenu(localeString("Filters"));
	filterMenu.setMnemonic(KeyEvent.VK_L);
	menubar.add(filterMenu);
	ptfilters = new PTFilters(frame, filterMenu, ptmodel);
	menuItem = new JMenuItem(localeString("newFilter"), KeyEvent.VK_N);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // create a new menu item.
		    // get the name
		    String name = null;
		    boolean loop = true;
		    do {
			name = (String)JOptionPane.showInputDialog
			    (frame, localeString("newFilterName"),
			     localeString("newFilterNameTitle"),
			     JOptionPane.PLAIN_MESSAGE);
			if (name == null) return;
			name = name.trim();
			if (name.length() == 0) return;
			if (ptfilters.getFilter(name) == null) {
			    loop = false;
			} else {
			    JOptionPane.showMessageDialog
				(frame, "Filter name in use",
				 "Error", JOptionPane.ERROR_MESSAGE);
			}
		    } while (loop);
		    ptfilters.addFilter(name, e, panel);
		    panel.repaint();
		}
	    });
	filterMenu.add(menuItem);
	menuItem = new JMenuItem(localeString("clearFilter"), KeyEvent.VK_C);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ptfilters.clear();
		    filterMenu.setText(localeString("Filters"));
		    panel.repaint();
		}
	    });
	filterMenu.add(menuItem);
	filterMenu.addSeparator();

	JMenu helpMenu = new JMenu(localeString("Help"));
	helpMenu.setMnemonic(KeyEvent.VK_H);
	menubar.add(helpMenu);
	menuItem = new JMenuItem(localeString("Manual"), KeyEvent.VK_M);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    showManual();
		}
	    });
	helpMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("PrintManual"), KeyEvent.VK_P);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    printManual();
		}
	    });
	helpMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("Browser"), KeyEvent.VK_B);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    showManualInBrowser();
		}
	    });
	helpMenu.add(menuItem);
	if (port == 0) {
	    helpMenu.addSeparator();
	    portMenuItem =
		new JMenuItem(localeString("TCPPort"), KeyEvent.VK_T);
	    portTextField = new PortTextField(5);
	    portTextField.setTCP(true);
	    portTextField.setPortName(localeString("mwebserver"));
	    portTextField.setDefaultValue(0);
	    portMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			if (JOptionPane.OK_OPTION ==
			    JOptionPane.showConfirmDialog(frame,
							  portTextField,
							  localeString
							  ("TCPTitle"),
							  JOptionPane
							  .OK_CANCEL_OPTION,
							  JOptionPane
							  .PLAIN_MESSAGE)) {
			    port = portTextField.getValue();
			} else {
			    if (port != 0) {
				portTextField.setValue(port);
			    } else {
				portTextField.setText("");
			    }
			}
		    }
		});
	    portMenuItem.setEnabled(true);
	    helpMenu.add(portMenuItem);
	    webMenuItem =
		new JMenuItem(localeString("StartWebserver"), KeyEvent.VK_W);
	    webMenuItem.setEnabled(true);
	    webMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			try {
			    startManualWebServerIfNeeded();
			    portTextField.setValue(port);
			    portTextField.setEditable(false);
			    webMenuItem.setEnabled(false);
			} catch (Exception ee) {
			    JOptionPane.showMessageDialog
				(frame,errorMsg("startingWS", ee.getMessage()),
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			}
		    }
		});
	    helpMenu.add(webMenuItem);
	}

	frame.setJMenuBar(menubar);
	setPopupMenus();
    }

    JPopupMenu editingMenu;
    JMenuItem toSegEndMenuItem;
    JMenuItem toSplineMenuItem;
    JMenuItem toControlMenuItem;

    JPopupMenu popupMenu;



    private int insertRowIndex = -1;
    private int insertionSelectedRow = -1;
    private Enum insertMode = null;
    private Enum replacementMode = null;

    private void onInsertBefore() {
	insertionSelectedRow = selectedRow;
	resetState();
	Enum mode = ptmodel.getRowMode(selectedRow);
	if (mode instanceof SplinePathBuilder.CPointType) {
	    int prevRowInd = selectedRow - 1;
	    int nextRowInd = selectedRow + 1;
	    switch((SplinePathBuilder.CPointType) mode) {
	    case MOVE_TO:
		{
		    insertRowIndex = selectedRow;
		    insertMode = mode;
		    replacementMode = SplinePathBuilder.CPointType.SEG_END;
		}
		break;
	    case SPLINE:
		insertRowIndex = selectedRow;
		insertMode = mode;
		replacementMode = mode;
		break;
	    case CONTROL:
		{
		    if(ptmodel.getRowMode(prevRowInd)
			== SplinePathBuilder.CPointType.SEG_END
			&&
			ptmodel.getRowMode(nextRowInd)
			== SplinePathBuilder.CPointType.SEG_END) {
			    insertRowIndex = selectedRow;
			    insertMode = mode;
			    replacementMode = mode;
		    } else {
			insertRowIndex = selectedRow;
			insertMode = SplinePathBuilder.CPointType.SEG_END;
			replacementMode = mode;
		    }       
		}
		break;
	    case SEG_END:
		insertRowIndex = selectedRow;
		insertMode = mode;
		replacementMode = mode;
		break;
	    default:
		throw new UnexpectedExceptionError();
	    }
	    resetState();
	    setModeline(String.format(localeString("leftClickInsertBefore"),
				      insertMode));
	    savedCursorDist = panel.getCursor();
	    panel.setCursor
		(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}
    }

    private void onInsertAfter() {
	insertionSelectedRow = selectedRow;
	resetState();
	Enum mode = ptmodel.getRowMode(selectedRow);
	if (mode instanceof SplinePathBuilder.CPointType) {
	    int nextRowInd = selectedRow + 1;
	    Enum nextRowMode = ptmodel.getRowMode(nextRowInd);
	    int nextRowInd2 = selectedRow + 2;
	    Enum nextRowMode2 = ptmodel.getRowMode(nextRowInd2);
	    switch((SplinePathBuilder.CPointType) mode) {
	    case MOVE_TO:
	    case SEG_END:
		{
		    insertRowIndex = nextRowInd;
		    if (nextRowMode == EPTS.Mode.PATH_END
			|| nextRowMode == SplinePathBuilder.CPointType.CLOSE) {
			insertMode = SplinePathBuilder.CPointType.SEG_END;
			replacementMode = null;
		    } else if (nextRowMode
			       == SplinePathBuilder.CPointType.SPLINE) {
			insertMode = SplinePathBuilder.CPointType.SPLINE;
			replacementMode = null;
		    } else if (nextRowMode
			       == SplinePathBuilder.CPointType.CONTROL) {
			if (nextRowMode2
			    == SplinePathBuilder.CPointType.SEG_END) {
			    insertMode = SplinePathBuilder.CPointType.CONTROL;
			    replacementMode = null;
			} else {
			    insertMode = SplinePathBuilder.CPointType.SEG_END;
			    replacementMode = null;
			}
		    } else {
			insertMode = SplinePathBuilder.CPointType.SEG_END;
			replacementMode = null;
		    }
		}
		break;
	    case SPLINE:
		insertRowIndex = nextRowInd;
		insertMode = mode;
		replacementMode = null;
		break;
	    case CONTROL:
		{
		    int prevRowInd = selectedRow - 1;
		    Enum prevRowMode = ptmodel.getRowMode(prevRowInd);
		    if(prevRowMode == SplinePathBuilder.CPointType.SEG_END
		       && nextRowMode == SplinePathBuilder.CPointType.SEG_END) {
			insertRowIndex = nextRowInd;
			insertMode = mode;
			replacementMode = null;
		    } else {
			insertRowIndex = selectedRow;
			insertMode = SplinePathBuilder.CPointType.SEG_END;
			replacementMode = null;
		    }  
		}
		break;
	    default:
		throw new UnexpectedExceptionError();
	    }
	    setModeline(String.format(localeString("leftClickInsertAfter"),
				      insertMode));
	    savedCursorDist = panel.getCursor();
	    panel.setCursor
		(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}
    }

    private void onDelete() {
	Enum mode = ptmodel.getRow(selectedRow).getMode();
	if (mode == EPTS.Mode.LOCATION) {
	    ptmodel.deleteRow(selectedRow);
	    selectedRow = -1;
	} else 	if (mode instanceof SplinePathBuilder.CPointType) {
	    int prevRowInd = selectedRow - 1;
	    int nextRowInd = selectedRow + 1;
	    switch ((SplinePathBuilder.CPointType) mode) {
	    case MOVE_TO:
		{
		    if (nextRowInd < ptmodel.getRowCount() &&
			ptmodel.getRowMode(nextRowInd)
			instanceof SplinePathBuilder.CPointType) {
			ptmodel.changeMode(nextRowInd,
					   SplinePathBuilder
					   .CPointType.MOVE_TO);
		    }
		}
		break;
	    case SPLINE:
		{
		    Enum prevmode = ptmodel.getRowMode(prevRowInd);
		    Enum nextmode = ptmodel.getRowMode(nextRowInd);
		    if (nextmode == EPTS.Mode.PATH_END &&
			prevmode == SplinePathBuilder.CPointType.SPLINE) {
			ptmodel.changeMode
			    (prevRowInd, SplinePathBuilder.CPointType.SEG_END);
		    }
		}
		break;
	    case CONTROL:
		break;
	    case SEG_END:
		{
		    Enum prevmode = ptmodel.getRowMode(prevRowInd);
		    Enum nextmode = ptmodel.getRowMode(nextRowInd);
		    if (prevmode == SplinePathBuilder.CPointType.SEG_END
			|| nextmode == SplinePathBuilder.CPointType.SEG_END) {
			break;
		    } else if (prevmode == SplinePathBuilder.CPointType.SPLINE
			       &&
			       nextmode == SplinePathBuilder.CPointType.SPLINE){
			break;
		    } else if (prevmode == SplinePathBuilder.CPointType.SPLINE){
			ptmodel.changeMode
			    (prevRowInd, SplinePathBuilder.CPointType.SEG_END);
			break;
		    } else if (nextmode == SplinePathBuilder.CPointType.SPLINE){
			ptmodel.changeMode
			    (nextRowInd, SplinePathBuilder.CPointType.SEG_END);
			break;
		    } else if (prevmode
			       == SplinePathBuilder.CPointType.MOVE_TO) {
			break;
		    } else if (prevmode == SplinePathBuilder.CPointType.CONTROL
			       &&
			       nextmode==SplinePathBuilder.CPointType.CONTROL){
			Enum pprevMode = ptmodel.getRowMode(prevRowInd-1);
			Enum nnextMode = ptmodel.getRowMode(nextRowInd+1);
			if (pprevMode == SplinePathBuilder.CPointType.CONTROL
			    || nnextMode==SplinePathBuilder.CPointType.CONTROL){
			    throw new IllegalStateException
				("delete SEG_END: bad case");
			}			    
		    } else {
			throw new IllegalStateException
			    ("delete SEG_END: bad case");
		    }
		}
		break;
	    default:
		throw new UnexpectedExceptionError();
	    }
	    ptmodel.deleteRow(selectedRow);
	    mode = ptmodel.getRow(selectedRow).getMode();
	    if (mode == SplinePathBuilder.CPointType.CLOSE
		&& ptmodel.getRowMode(prevRowInd)
		== EPTS.Mode.PATH_START) {
		ptmodel.deleteRow(selectedRow);
		mode = ptmodel.getRowMode(selectedRow);
		if (mode == EPTS.Mode.PATH_END) {
		    ptmodel.deleteRow(selectedRow);
		    ptmodel.deleteRow(prevRowInd);
		}
	    } else if (mode == EPTS.Mode.PATH_END
		       && ptmodel.getRowMode(prevRowInd)
		       == EPTS.Mode.PATH_START) {
		ptmodel.deleteRow(selectedRow);
		ptmodel.deleteRow(prevRowInd);
	    }
	    addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	    deletePathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	    selectedRow = -1;
	    setModeline("");
	}
    }

    private boolean onlySplinesAfterwards() {
	int n = ptmodel.getRowCount();
	for (int index = selectedRow+1; index < n; index++) {
	    Enum mode = ptmodel.getRowMode(index);
	    if (mode == EPTS.Mode.PATH_END
		|| mode == SplinePathBuilder.CPointType.CLOSE) {
		return true;
	    } else if (mode != SplinePathBuilder.CPointType.SPLINE) {
		return false;
	    }
	}
	return true;
    }

    private boolean onlySplinesPreviously() {
	for (int index = selectedRow-1; index > -1; index--) {
	    Enum mode = ptmodel.getRowMode(index);
	    if (mode == null || mode == EPTS.Mode.PATH_START
		|| mode == SplinePathBuilder.CPointType.MOVE_TO) {
		break;
	    } else if (mode != SplinePathBuilder.CPointType.SPLINE) {
		return false;
	    }
	}
	return true;
    }

    static final Object splineOptions[] = {
	SplinePathBuilder.CPointType.SEG_END,
	SplinePathBuilder.CPointType.SPLINE,
    };

    static final Object splineOptionsReversed[] = {
	SplinePathBuilder.CPointType.SPLINE,
	SplinePathBuilder.CPointType.SEG_END,
    };

    static final Object controlOptions[] = {
	SplinePathBuilder.CPointType.SEG_END,
	SplinePathBuilder.CPointType.CONTROL,
    };

    static final Object controlOptionsReversed[] = {
	SplinePathBuilder.CPointType.CONTROL,
	SplinePathBuilder.CPointType.SEG_END,
    };


    static final Object splineControlOptions[] = {
	SplinePathBuilder.CPointType.SPLINE,
	SplinePathBuilder.CPointType.CONTROL,
	SplinePathBuilder.CPointType.SEG_END,

    };

    private void onChangeType() {
	Enum mode = ptmodel.getRow(selectedRow).getMode();
	Object[] options = null;
	Object preferred = null;
	if (mode instanceof SplinePathBuilder.CPointType) {
	    switch((SplinePathBuilder.CPointType) mode) {
	    case MOVE_TO: // cannot change - we always need a starting point.
	    case CLOSE: // cannot change because there is no point for it.
		return;
	    case SEG_END: // can change depending on neighbors
		{
		    Enum prevMode = ptmodel.getRowMode(selectedRow-1);
		    if (prevMode == SplinePathBuilder.CPointType.MOVE_TO) {
			// Treat a previous MOVE_TO the same as a SEG_END
			//  for this case.
			prevMode = SplinePathBuilder.CPointType.SEG_END;
		    }
		    PointTMR nr = ptmodel.getRow(selectedRow+1);
		    Enum nextMode = (nr == null)? null: nr.getMode();
		    if (nextMode == EPTS.Mode.PATH_END
			&& prevMode == SplinePathBuilder.CPointType.SPLINE
			&& onlySplinesPreviously()) {
			options = splineOptionsReversed;
			preferred = SplinePathBuilder.CPointType.SPLINE;

		    } else if (prevMode==SplinePathBuilder.CPointType.SEG_END) {
			nr = ptmodel.getRow(selectedRow+2);
			Enum nextNextMode = (nr == null)? null: nr.getMode();
			if (nextMode == SplinePathBuilder.CPointType.SPLINE) {
			    options = splineOptionsReversed;
			    preferred = SplinePathBuilder.CPointType.SPLINE;
			} else if (nextMode
				   ==SplinePathBuilder.CPointType.SEG_END) {
			    options = splineControlOptions;
			    preferred = SplinePathBuilder.CPointType.SPLINE;
			} else if (nextNextMode
				   == SplinePathBuilder.CPointType.SEG_END) {
			    if (nextMode
				== SplinePathBuilder.CPointType.CONTROL) {
				options = controlOptionsReversed;
				preferred =
				    SplinePathBuilder.CPointType.CONTROL;
			    }
			}
		    } else if (prevMode==SplinePathBuilder.CPointType.SPLINE
			       && nextMode
			       == SplinePathBuilder.CPointType.SPLINE) {
			options = splineOptions;
			preferred = SplinePathBuilder.CPointType.SEG_END;
		    } else if (nextMode
			       == SplinePathBuilder.CPointType.SEG_END) {
			nr = ptmodel.getRow(selectedRow-2);
			Enum prevPrevMode = (nr == null)? null: nr.getMode();
			if (prevMode == SplinePathBuilder.CPointType.SPLINE) {
			    options = splineOptionsReversed;
			    preferred = SplinePathBuilder.CPointType.SPLINE;
			} else if (prevPrevMode
				   == SplinePathBuilder.CPointType.SEG_END
				   && prevMode
				   == SplinePathBuilder.CPointType.CONTROL) {
			    options = controlOptionsReversed;
			    preferred = SplinePathBuilder.CPointType.CONTROL;
			}
		    }
		}
		break;
	    case SPLINE: // can change to SEG_END
		{
		    if (onlySplinesPreviously() && onlySplinesAfterwards()
			&& hasLoop(selectedRow)) {
			throw new IllegalStateException
			    ("should not be able to change this point's type");
		    }
		    options = splineOptions;
		    preferred = SplinePathBuilder.CPointType.SEG_END;
		}
		break;
	    case CONTROL: // can change to SEG_END.
		{
		    Enum prevMode = ptmodel.getRowMode(selectedRow-1);
		    Enum nextMode = ptmodel.getRowMode(selectedRow+1);
		    if ((prevMode == SplinePathBuilder.CPointType.MOVE_TO
			 || prevMode == SplinePathBuilder.CPointType.SEG_END)
			&&
			nextMode == SplinePathBuilder.CPointType.SEG_END) {
			options = splineControlOptions;
		    } else {
			options = controlOptions;
		    }
		    preferred = SplinePathBuilder.CPointType.SEG_END;
		}
		break;
	    default:
		throw new UnexpectedExceptionError();
	    }
	    if (options != null) {
		Enum nm = (Enum)JOptionPane.showInputDialog
		    (frame, localeString("ChooseNewPointType"),
		     localeString("ChangeTypeOptions"),
		     JOptionPane.PLAIN_MESSAGE, null,
		     options, preferred);
		if (nm != null) {
		    ptmodel.changeMode(selectedRow, nm);
		    panel.repaint();
		}
	    }
	}
    }
    
    private void onBreakLoop() {
	if (selectedRow != -1) {
	    for (int index = selectedRow;
		 index < ptmodel.getRowCount();
		 index++) {
		Enum pmode = ptmodel.getRowMode(index);
		if (pmode == null || pmode == EPTS.Mode.PATH_END) {
		    break;
		} else if (pmode == SplinePathBuilder.CPointType.CLOSE) {
		    ptmodel.deleteRow(index);
		    index--;
		    pmode = ptmodel.getRowMode(index);
		    if (pmode == SplinePathBuilder.CPointType.SPLINE) {
			ptmodel.changeMode
			    (index, SplinePathBuilder.CPointType.SEG_END);
		    }
		    break;
		} 
	    }
	}
	breakLoopMenuItem.setEnabled(false);
    }

    private void onExtendPath() {
	if (selectedRow == -1) {
	    return;
	}
	int endIndex = selectedRow;
	int n = ptmodel.getRowCount();
	while (endIndex < n
	       && ptmodel.getRowMode(endIndex) != EPTS.Mode.PATH_END) {
	    endIndex++;
	}
	if (endIndex == n) {
	    selectedRow = -1;
	    return;
	}
	if (endIndex != n-1) {
	    int startIndex = selectedRow;
	    while (startIndex > 0 &&
		   ptmodel.getRowMode(startIndex) != EPTS.Mode.PATH_START) {
		startIndex--;
	    }
	    PointTMR row;
	    do {
		row = ptmodel.getRow(startIndex);
		ptmodel.deleteRow(startIndex, false);
		ptmodel.addRow(row);
	    } while (row.getMode() != EPTS.Mode.PATH_END);
	    endIndex = ptmodel.getRowCount()-1;
	    ptmodel.deleteRow(endIndex--);
	    selectedRow = -1;
	    // This calls ttable.nextState,
	    // which calls a menu item's
	    // action listener's actionPeformed
	    // method to set the nextState variable
	    // for the radio-button menu item cases.
	    if (ttable == null) {
		// reconstruct the last path up to
		// the point where the path was about
		// to be terminated.
		savedCursorPath = panel.getCursor();
		panel.setCursor
		    (Cursor.getPredefinedCursor
		     (Cursor.CROSSHAIR_CURSOR));
		ttable = new TransitionTable();
	    }
	    ttable.setState(ptmodel, endIndex);
	    TransitionTable.getLocMenuItem().setEnabled(true);
	} else {
	    endIndex = ptmodel.getRowCount()-1;
	    ptmodel.deleteRow(endIndex--);
	    selectedRow = -1;
	    if (ttable == null) {
		// reconstruct the last path up to
		// the point where the path was about
		// to be terminated.
		savedCursorPath = panel.getCursor();
		panel.setCursor
		    (Cursor.getPredefinedCursor
		     (Cursor.CROSSHAIR_CURSOR));
		ttable = new TransitionTable();
	    }
	    ttable.setState(ptmodel, endIndex);
	    TransitionTable.getLocMenuItem().setEnabled(true);
	}
	if (nextState != null) {
	    saveMenuItem.setEnabled(false);
	    saveAsMenuItem.setEnabled(false);
	}
    }

    private boolean hasLoop(int rowIndex) {
	if (rowIndex != -1) {
	    for (int index = rowIndex; index < ptmodel.getRowCount(); index++) {
		Enum pmode = ptmodel.getRowMode(index);
		if (pmode == null || pmode == EPTS.Mode.PATH_END) {
		    break;
		} else if (pmode == SplinePathBuilder.CPointType.CLOSE) {
		    return true;
		} 
	    }
	}
	return false;
    }

    JMenuItem deleteMenuItem;
    JMenuItem changeMenuItem; 
    JMenuItem breakLoopMenuItem;
    JMenuItem extendPathMenuItem;
    JMenuItem insertBeforeMenuItem;
    JMenuItem insertAfterMenuItem;

    private void setPopupMenus() {
	popupMenu = new JPopupMenu();
	deleteMenuItem = new JMenuItem(localeString("DeletePoint"));
	deleteMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onDelete();
		}
	    });
	popupMenu.add(deleteMenuItem);

	changeMenuItem = new JMenuItem(localeString("ChangePointType"));
	changeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onChangeType();
		}
	    });
	popupMenu.add(changeMenuItem);

	insertBeforeMenuItem = new JMenuItem(localeString("InsertBeforePoint"));
	insertBeforeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onInsertBefore();
		}
	    });
	popupMenu.add(insertBeforeMenuItem);

	insertAfterMenuItem = new JMenuItem(localeString("InsertAfterPoint"));
	insertAfterMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onInsertAfter();
		}
	    });
	popupMenu.add(insertAfterMenuItem);

	breakLoopMenuItem = new JMenuItem(localeString("BreakLoop"));
	breakLoopMenuItem.setEnabled(false);
	breakLoopMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onBreakLoop();
		}
	    });
	popupMenu.add(breakLoopMenuItem);

	extendPathMenuItem = new JMenuItem(localeString("ExtendPath"));
	extendPathMenuItem.setEnabled(true);
	extendPathMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onExtendPath();
		}
	    });
	popupMenu.add(extendPathMenuItem);

	editingMenu = new JPopupMenu();
	toSegEndMenuItem = new JMenuItem(localeString("ModeToSEGEND"));
	toSegEndMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ptmodel.setLastRowMode
			(SplinePathBuilder.CPointType.SEG_END);
		    int lastPointIndex = ptmodel.getRowCount()-1;
		    nextState = SplinePathBuilder.CPointType.SEG_END;
		    setModeline(nextState);
		    // This calls ttable.nextState,
		    // which calls a menu item's
		    // action listener's actionPeformed
		    // method to set the nextState variable
		    // for the radio-button menu item cases.
		    ttable.setState(ptmodel, lastPointIndex);
		}
	    });
	editingMenu.add(toSegEndMenuItem);
	toSplineMenuItem = new JMenuItem(localeString("ModeToSPLINE"));
	toSplineMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ptmodel.setLastRowMode(SplinePathBuilder.CPointType.SPLINE);
		    int lastPointIndex = ptmodel.getRowCount()-1;
		    nextState = SplinePathBuilder.CPointType.SPLINE;
		    setModeline(nextState);
		    // This calls ttable.nextState,
		    // which calls a menu item's
		    // action listener's actionPeformed
		    // method to set the nextState variable
		    // for the radio-button menu item cases.
		    ttable.setState(ptmodel, lastPointIndex);
		}
	    });
	editingMenu.add(toSplineMenuItem);
	toControlMenuItem = new JMenuItem(localeString("ModeToCONTROL"));
	toControlMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ptmodel.setLastRowMode
			(SplinePathBuilder.CPointType.CONTROL);
		    int lastPointIndex = ptmodel.getRowCount()-1;
		    if (ptmodel.getRowMode(lastPointIndex-1)
			== SplinePathBuilder.CPointType.CONTROL) {
			nextState = SplinePathBuilder.CPointType.SEG_END;
			setModeline(nextState);
		    } else {
			nextState = SplinePathBuilder.CPointType.CONTROL;
			setModeline(nextState);
		    }
		    // This calls ttable.nextState,
		    // which calls a menu item's
		    // action listener's actionPeformed
		    // method to set the nextState variable
		    // for the radio-button menu item cases.
		    ttable.setState(ptmodel, lastPointIndex);
		}
	    });
	editingMenu.add(toControlMenuItem);

    }

    private void checkPopupMenu(MouseEvent e) {
	if (nextState != null && e.isPopupTrigger()) {
	    Enum mode = ptmodel.getLastRow().getMode();
	    if (mode != null && mode instanceof SplinePathBuilder.CPointType) {
		int rind = ptmodel.getRowCount() - 1;
		switch ((SplinePathBuilder.CPointType) mode) {
		case SEG_END:
		    {
			int sz = ptmodel.getRowCount();
			Enum prevMode = ptmodel.getRowMode(sz-2);
			Enum pprevMode =ptmodel.getRowMode(sz-3);
			if (prevMode instanceof SplinePathBuilder.CPointType) {
			    switch ((SplinePathBuilder.CPointType)prevMode) {
			    case MOVE_TO:
			    case SEG_END:
				toSegEndMenuItem.setEnabled(true);
				toSplineMenuItem.setEnabled(true);
				toControlMenuItem.setEnabled(true);
				break;
			    case SPLINE:
				toSegEndMenuItem.setEnabled(true);
				toSplineMenuItem.setEnabled(true);
				toControlMenuItem.setEnabled(false);
				break;
			    case CONTROL:
				if (pprevMode
				    == SplinePathBuilder.CPointType.CONTROL) {
				    toControlMenuItem.setEnabled(false);
				    toSegEndMenuItem.setEnabled(true);
				    toSplineMenuItem.setEnabled(false);
				} else {
				    toControlMenuItem.setEnabled(true);
				    toSegEndMenuItem.setEnabled(true);
				    toSplineMenuItem.setEnabled(false);
				}
				break;
			    }
			}
		    }
		    break;
		case SPLINE:
		    toSegEndMenuItem.setEnabled(true);
		    toSplineMenuItem.setEnabled(true);
		    toControlMenuItem.setEnabled(false);
		    break;
		case CONTROL:
		    toSegEndMenuItem.setEnabled(true);
		    toSplineMenuItem.setEnabled(false);
		    toControlMenuItem.setEnabled(false);
		    break;
		default:
		    toSegEndMenuItem.setEnabled(false);
		    toSplineMenuItem.setEnabled(false);
		    toControlMenuItem.setEnabled(false);
		}
	    }
	    editingMenu.show(e.getComponent(), e.getX(), e.getY());
	} else if (selectedRow != -1 && e.isPopupTrigger()) {
	    breakLoopMenuItem.setEnabled(hasLoop(selectedRow));
	    Enum mode = ptmodel.getRow(selectedRow).getMode();
	    if (mode == EPTS.Mode.LOCATION) {
		changeMenuItem.setEnabled(false);
		deleteMenuItem.setEnabled(true);
		extendPathMenuItem.setEnabled(false);
		insertBeforeMenuItem.setEnabled(false);
		insertAfterMenuItem.setEnabled(false);
	    } else {
		insertBeforeMenuItem.setEnabled(true);
		insertAfterMenuItem.setEnabled(true);
		extendPathMenuItem.setEnabled(true);
		switch((SplinePathBuilder.CPointType) mode) {
		case MOVE_TO:
		    changeMenuItem.setEnabled(false);
		    deleteMenuItem.setEnabled(true);
		    break;
		case SEG_END:
		    Enum prevMode = ptmodel.getRowMode(selectedRow-1);
		    if (prevMode == SplinePathBuilder.CPointType.MOVE_TO) {
			prevMode = SplinePathBuilder.CPointType.SEG_END;
		    }
		    Enum nextMode = ptmodel.getRowMode(selectedRow+1);
		    if (prevMode == SplinePathBuilder.CPointType.CONTROL
			&& nextMode == SplinePathBuilder.CPointType.CONTROL) {
			changeMenuItem.setEnabled(false);
			Enum prevprevMode = ptmodel.getRowMode(selectedRow-2);
			Enum nextnextMode = ptmodel.getRowMode(selectedRow+2);
			if ((prevprevMode==SplinePathBuilder.CPointType.MOVE_TO
			     ||
			     prevprevMode==SplinePathBuilder.CPointType.SEG_END)
			    && nextnextMode
			    ==SplinePathBuilder.CPointType.SEG_END) {
			    deleteMenuItem.setEnabled(true);
			}	
		    } else if ((prevMode
				== SplinePathBuilder.CPointType.CONTROL
				&& nextMode
				!=SplinePathBuilder.CPointType.SEG_END)
			       || (nextMode
				   == SplinePathBuilder.CPointType.CONTROL
				   && prevMode
				   != SplinePathBuilder.CPointType.SEG_END)) {
			deleteMenuItem.setEnabled(false);
			changeMenuItem.setEnabled(false);
		    } else {
			changeMenuItem.setEnabled(true);
			deleteMenuItem.setEnabled(true);
		    }
		    break;
		case SPLINE:
		    if (hasLoop(selectedRow) && onlySplinesPreviously()
			&& onlySplinesAfterwards()) {
			changeMenuItem.setEnabled(false);
			deleteMenuItem.setEnabled(true);
		    } else {
			changeMenuItem.setEnabled(true);
			deleteMenuItem.setEnabled(true);
		    }
		    break;
		default:
		    changeMenuItem.setEnabled(true);
		    deleteMenuItem.setEnabled(true);
		}
	    }
	    popupMenu.show(e.getComponent(), e.getX(), e.getY());
	}
    }

    private void rezoom() {
	JViewport vp = scrollPane.getViewport();
	int vpw = vp.getWidth();
	int vph = vp.getHeight();
	Point p = vp.getViewPosition();
	int x = (int)Math.round((p.x  + vpw/2)/zoom);
	int y = (int)Math.round((p.y + vph/2)/zoom);
	zoom = zoomTable[zoomIndex];
	x = (int)Math.round(zoom*x) - vpw/2;
	y = (int)Math.round(zoom*y) - vph/2;
	Dimension d = new Dimension((int)Math.round(zoom*width),
				    (int)Math.round(zoom*height));
	panel.setPreferredSize(d);
	panel.setSize(d);
	int pw = (int)Math.round(d.getWidth());
	int ph = (int)Math.round(d.getHeight());
	if (x > (pw - vpw)) {
	    x = pw - vpw;
	}
	if (y > (ph - vph)) {
	    y = ph - vph;
	}
	if (x < 0) x = 0;
	if (y < 0) y = 0;
	vp.setViewPosition(new Point(x, y));
	scrollPane.repaint();
    }

    private static int CTRL_SHIFT =
	InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

    private static int KEY_MASK = CTRL_SHIFT | InputEvent.META_DOWN_MASK
	| InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK;

    double lastx, lasty;
    double startx = -1;
    double starty = -1;
    boolean mouseTracked = false;

    KeyAdapter kba = new KeyAdapter() {
	    public void keyTyped(KeyEvent e) {
		int lastZoomInd = zoomIndex;
		switch(e.getKeyChar()) {
		case KeyEvent.VK_ENTER:
		    if (locState) {
			Point p = panel.getMousePosition();
			if (p == null) break;
			double x = p.x / zoom;
			double y = height - (p.y / zoom);
			x *= scaleFactor;
			y *= scaleFactor;
			x += xrefpoint;
			y += yrefpoint;
			String location;
			switch(locationFormat) {
			case COORD:
			    location = String.format("(%g,%g)", x, y);
			    break;
			case EXPRESSION:
			    location = String.format("x: %g, y: %g", x, y);
			    break;
			case OBJECT:
			    location = String.format("{x: %g, y: %g}", x, y);
			    break;
			case STATEMENT:
			    {
				double xp = (p.x/zoom);
				double yp = (p.y/zoom);
				ptmodel.addRow(varname, EPTS.Mode.LOCATION,
					       x, y, xp, yp);
				location = String.format("%s = {x: %g, y: %g};",
							 varname, x, y);
			    }
			    break;
			default:
			    throw new UnexpectedExceptionError();
			}
			setModeline(String.format
				    (localeString("LocationCopied"), location));
			Clipboard cb = panel.getToolkit().getSystemClipboard();
			StringSelection selection =
			    new StringSelection(location);
			cb.setContents(selection, selection);
			if (savedCursorDist != null) {
			    panel.setCursor(savedCursorDist);
			    savedCursorDist = null;
			}
			locState = false;
			if (ptmodel.getRowCount() > 0) {
			    makeCurrentMenuItem.setEnabled(true);
			}
		    } else if (distState != 0) {
			Point p = panel.getMousePosition();
			if (p != null) {
			    if (distState == 2) {
				// g2d.setXORMode(Color.BLACK);
				// g2d.setStroke(new BasicStroke(1.0F));
				mouseTracked = true;
				x1 = p.x / zoom;
				y1 = p.y / zoom;
				setModeline("Left click the second point; "
					    + "ESC to abort");
				distState = 1;
				lastx = x1;
				lasty = y1;
				startx = lastx;
				starty = lasty;
			    } else if (distState == 1) {
				double x2 = p.x / zoom;
				double y2 = p.y / zoom;
				double x12 = x1 - x2;
				double y12 = y1 - y2;
				double dist = Math.sqrt(x12*x12 + y12*y12);
				String space;
				if (distInGCS) {
				    dist *= scaleFactor;
				    space = "GCS units";
				} else {
				    space = "Pts";
				}
				x1 = 0; y1 = 0;
				String distString = String.format("%g", dist);
				setModeline(String.format
					    (localeString("DistanceIs"),
					     distString, space));
				Clipboard cb =
				    panel.getToolkit().getSystemClipboard();
				StringSelection selection =
				    new StringSelection(distString);
				cb.setContents(selection, selection);
				resetState();
			    }
			}
		    } else if (selectedRow != -1) {
			selectedRow = -1;
			setModeline("");
		    } else if (nextState != null
			       && nextState instanceof
			       SplinePathBuilder.CPointType) {
			SplinePathBuilder.CPointType ns =
			    (SplinePathBuilder.CPointType) nextState;
			Point p = panel.getMousePosition();
			if (p != null) {
			    double xp = (p.x/zoom);
			    double yp = (p.y/zoom);
			    double x = xp;
			    double y = height - yp;
			    x *= scaleFactor;
			    y *= scaleFactor;
			    x += xrefpoint;
			    y += yrefpoint;
			    ptmodel.addRow("",ns, x, y, xp, yp);
			    // This can call a radio-button menu item's
			    // action listener's actionPeformed
			    // method, and that sets the
			    // nextState variable.
			    ttable.nextState(ns);
			}
		    }
		    return;
		default:
		    // System.out.format("code = %d\n",  (int)(e.getKeyChar()));
		    return;
		}
	    }

	    public void keyPressed(KeyEvent e) {
		int modifiers = e.getModifiersEx();
		if ((modifiers & KEY_MASK) == CTRL_SHIFT) {
		    // This handles the case were the shift key was pressed
		    // in addition to the control key, so the menu-item
		    // accelerator won't notice it.
		    int lastZoomInd = zoomIndex;
		    switch (e.getKeyCode()) {
		    case KeyEvent.VK_EQUALS:
			zoomIndex++;
			if (zoomIndex >= zoomTable.length) {
			    zoomIndex = zoomTable.length-1;
			}
			break;
		    case KeyEvent.VK_MINUS:
			if (e.isControlDown()) {
			    zoomIndex--;
			    if (zoomIndex < 0) zoomIndex = 0;
			}
			break;
		    case KeyEvent.VK_0:
			if (e.isControlDown()) {
			    zoomIndex = initialZoomIndex;
			}
			break;
		    }
		    if (lastZoomInd != zoomIndex) {
			rezoom();
		    }
		} else if ((modifiers&KEY_MASK) == InputEvent.CTRL_DOWN_MASK) {
		    JViewport vp = scrollPane.getViewport();
		    Point p = vp.getViewPosition();
		    int vpw = vp.getWidth();
		    int vph = vp.getHeight();

		    switch(e.getKeyCode()) {
		    case KeyEvent.VK_RIGHT:
			p.x++;
			if (p.x + vpw > panel.getWidth()) {
			    p.x = panel.getWidth() - vpw;
			}
 			break;
		    case KeyEvent.VK_LEFT:
			p.x--;
			if (p.x < 0) p.x = 0;
			break;
		    case KeyEvent.VK_DOWN:
			p.y++;
			if (p.y + vph > panel.getHeight()) {
			    p.y = panel.getHeight() - vph;
			}
			break;
		    case KeyEvent.VK_UP:
			p.y--;
			if (p.y < 0) p.y = 0;
			break;
		    }
		    vp.setViewPosition(p);
		    scrollPane.repaint();
		} else {
		    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			String frameText = localeString("CancelCurrentOps");
			String frameTitle = localeString("Cancel");
			if (insertRowIndex != -1) {
			    insertRowIndex = -1;
			    selectedRow = -1;
			    resetState();
			    setModeline("");
			    return;
			} else if (distState != 0) {
			    resetState();
			    setModeline("");
			    return;
			} else if (locState) {
			    resetState();
			    setModeline("");
			    return;
			} else if (nextState != null) {
			    int lastrowInd = ptmodel.getRowCount()-1;
			    int lastStart = ptmodel.findStart(lastrowInd);
			    String vname = ptmodel.getRow(lastStart)
				.getVariableName();
			    frameText = String.format
				(localeString("UndoCurrentPathSegs"), vname);
			    frameTitle =
				localeString("UndoCurrentPathSegsTitle");
			} else if (selectedRow != -1) {
			    ptmodel.changeCoords(selectedRow,
						 lastXSelected, lastYSelected,
						 lastXPSelected, lastYPSelected,
						 true);
			    selectedRow = -1;
			    setModeline("");
			    panel.repaint();
			    return;
			} else {
			    return;
			}
			int result =
			    JOptionPane.showConfirmDialog
			    (frame, frameText, frameTitle,
			     JOptionPane.OK_CANCEL_OPTION,
			     JOptionPane.QUESTION_MESSAGE);
			if (result == JOptionPane.OK_OPTION) {
			    cleanupPartialPath(true);
			    resetState();
			    setModeline("");
			}
		    } else if (selectedRow != -1) {
			PointTMR row = ptmodel.getRow(selectedRow);
			if (row == null) {
			    selectedRow = -1;
			    resetState();
			    setModeline("");
			    panel.repaint();
			    return;
			}
			Enum rmode = row.getMode();
			if ((rmode instanceof SplinePathBuilder.CPointType
			     && rmode != SplinePathBuilder.CPointType.CLOSE)
			    || rmode == EPTS.Mode.LOCATION) {
			    double xp = row.getXP();
			    double yp = row.getYP();
			    switch (e.getKeyCode()) {
			    case KeyEvent.VK_RIGHT:
				xp += 1.0/zoom;
				break;
			    case KeyEvent.VK_LEFT:
				xp -= 1.0/zoom;
				break;
			    case KeyEvent.VK_UP:
				yp -= 1.0/zoom;
				break;
			    case KeyEvent.VK_DOWN:
				yp += 1.0/zoom;
				break;
			    default:
			    }
			    double x = xp;
			    double y = height - yp;
			    x *= scaleFactor;
			    y *= scaleFactor;
			    x += xrefpoint;
			    y += yrefpoint;
			    ptmodel.changeCoords(selectedRow, x, y,
						 xp, yp, true);
			    panel.repaint();
			}
		    } else if (locState || distState != 0 ||
			       (nextState != null
				&& nextState instanceof
				SplinePathBuilder.CPointType)) {

			try {
			    PointerInfo pi = MouseInfo.getPointerInfo();
			    Point p = pi.getLocation();
			    GraphicsDevice gd = pi.getDevice();
			    GraphicsConfiguration gc =
				gd.getDefaultConfiguration();
			    Rectangle bounds = gc.getBounds();
			    Robot robot = new Robot(gd);
			    switch (e.getKeyCode()) {
			    case KeyEvent.VK_LEFT:
				p.x--;
				break;
			    case KeyEvent.VK_RIGHT:
				p.x++;
				break;
			    case KeyEvent.VK_UP:
				p.y--;
				break;
			    case KeyEvent.VK_DOWN:
				p.y++;
				break;
			    }
			    if (bounds.contains(p)) {
				robot.mouseMove(p.x, p.y);
			    }
			} catch (AWTException ee) {
			    System.err.println(errorMsg("mouseMove"));
			} catch (Exception ee) {
			    ee.printStackTrace(System.err);
			}
		    }
		}
	    }
	};

    int distState = 0;


    private void resetState() {
	if (savedCursorDist != null) {
	    panel.setCursor(savedCursorDist);
	    savedCursorDist = null;
	}
	if (locState || distState != 0) {
	    if (distState != 0) {
		mouseTracked = false;
		panel.repaint();
	    }
	}
	if (nextState != null) {
	    if (savedCursorPath != null) {
		panel.setCursor(savedCursorPath);
		savedCursorPath = null;
	    }
	    nextState = null;
	    TransitionTable.getLocMenuItem().setEnabled(false);
	    saveMenuItem.setEnabled(true);
	    saveAsMenuItem.setEnabled(true);
	    ttable = null;
	}

	locState = false;
	distState = 0;
	if (ptmodel.getRowCount() > 0) {
	    makeCurrentMenuItem.setEnabled(true);
	}
    }


    Cursor savedCursorDist = null;
    boolean distInGCS = false;
    double x1 = 0.0, y1 = 0.0;
    public void measureDistance(boolean gcs) {
	resetState();
	distInGCS = gcs;
	makeCurrentMenuItem.setEnabled(false);
	setModeline(localeString("LeftClickFirstPoint"));
	distState = 2;
	makeCurrentMenuItem.setEnabled(false);
	locState = false;
	savedCursorDist = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    boolean locState = false;

    public void createLocation() {
	resetState();
	makeCurrentMenuItem.setEnabled(false);
	setModeline(localeString("LeftClickCreate"));
	TransitionTable.getLocMenuItem().setEnabled(true);
	distState = 0; 
	locState = true;
	savedCursorDist = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    TransitionTable ttable = null;
    Enum nextState = null;
    Cursor savedCursorPath = null;
    private long pathIndex = 1;

    private void createPath() {
	resetState();
	varname = JOptionPane.showInputDialog
	    (frame, localeString("PleaseEnterVariableName"),
	     localeString("ScriptingLanguageVariableName"),
	     JOptionPane.PLAIN_MESSAGE);
	if (varname == null) return;
	varname = varname.trim();
	if (varname.length() == 0) {
	    varname = "path" + (pathIndex++);
	    while (ptmodel.getVariableNames().contains(varname)) {
		varname = "path" + (pathIndex++);
	    }
	}
	if (ptmodel.getVariableNames().contains(varname)) {
	    JOptionPane.showMessageDialog(frame, errorMsg("nameInUse", varname),
					  localeString("errorTitle"),
					  JOptionPane.ERROR_MESSAGE);
	    return;
	}
	// nextState = SplinePathBuilder.CPointType.MOVE_TO;
	makeCurrentMenuItem.setEnabled(false);
	ptmodel.addRow(varname, EPTS.Mode.PATH_START, 0.0, 0.0, 0.0, 0.0);
	savedCursorPath = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	createPathFinalAction();
    }

    private void createPathFinalAction() {
	ttable = new TransitionTable();
	setModeline(SplinePathBuilder.CPointType.MOVE_TO);
	nextState = SplinePathBuilder.CPointType.MOVE_TO;
	TransitionTable.getLocMenuItem().setEnabled(true);
	addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	deletePathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	saveMenuItem.setEnabled(false);
	saveAsMenuItem.setEnabled(false);
    }

    boolean mouseMoved = false;
    int selectedRow = -1;
    // boolean selectedRowClick = false;
    double lastXSelected = 0.0;
    double lastYSelected = 0.0;
    double lastXPSelected = 0.0;
    double lastYPSelected = 0.0;
    double xpoff = 0.0;
    double ypoff = 0.0;
    // pressed the alt key or in drag-image mode with the mouse
    // pressed while a point was not selected or a curve or point
    // was not being created.
    boolean altPressed = false;
    // actually pressed the ALT key instead of just being in
    // drag image mode with other constraints satisfied
    boolean altReallyPressed = false;
    boolean toggledAltD = true;

    MouseInputAdapter mia = new MouseInputAdapter() {
	    public void mouseClicked(MouseEvent e) {
		Point p = panel.getMousePosition();
		if (p != null && e.getButton() == MouseEvent.BUTTON1) {
		    int modifiers = e.getModifiersEx();
		    if ((modifiers & KEY_MASK) == InputEvent.CTRL_DOWN_MASK) {
			return;
		    }
		    if (insertRowIndex != -1) {
			double xp =(p.x/zoom);
			double yp = (p.y/zoom);
			double x = xp;
			double y = height - yp;
			x *= scaleFactor;
			y *= scaleFactor;
			x += xrefpoint;
			y += yrefpoint;
			if (replacementMode != null) {
			    ptmodel.changeMode(insertionSelectedRow,
					       replacementMode);
			}
			ptmodel.insertRow(insertRowIndex, "", insertMode,
					  x, y, xp, yp);
			insertRowIndex = -1;
			insertionSelectedRow = -1;
			selectedRow = -1;
			resetState();
		    } else if (locState) {
			double x = p.x / zoom;
			double y = height - p.y / zoom;
			x *= scaleFactor;
			y *= scaleFactor;
			x += xrefpoint;
			y += yrefpoint;
			String location;
			switch(locationFormat) {
			case COORD:
			    location = String.format("(%g,%g)", x, y);
			    break;
			case EXPRESSION:
			    location = String.format("x: %g, y: %g", x, y);
			    break;
			case OBJECT:
			    location = String.format("{x: %g, y: %g}", x, y);
			    break;
			case STATEMENT:
			    {
				double xp =(p.x/zoom);
				double yp = (p.y/zoom);
				ptmodel.addRow(varname, EPTS.Mode.LOCATION,
					       x, y, xp, yp);
				location = String.format("%s = {x: %g, y: %g};",
							 varname, x, y);
			    }
			    break;
			default:
			    throw new UnexpectedExceptionError();
			}
			setModeline(String.format("location: %s "
						  + "(copied to clipboard)",
						  location));
			Clipboard cb = panel.getToolkit().getSystemClipboard();
			StringSelection selection =
			    new StringSelection(location);
			cb.setContents(selection, selection);
			if (savedCursorDist != null) {
			    panel.setCursor(savedCursorDist);
			    savedCursorDist = null;
			}
			locState = false;
			if (ptmodel.getRowCount() > 0) {
			    makeCurrentMenuItem.setEnabled(true);
			}
			panel.repaint();
		    } else if (distState == 2) {
			// g2d.setXORMode(Color.BLACK);
			// g2d.setStroke(new BasicStroke(1.0F));
			mouseTracked = true;
			x1 = p.x / zoom;
			y1 = p.y / zoom;
			setModeline(localeString("LeftClickSecondPoint"));
			lastx = x1;
			lasty = y1;
			startx = lastx;
			starty = lasty;
			distState = 1;
		    } else if (distState == 1) {
			double x2 = p.x / zoom;
			double y2 = p.y / zoom;
			double x12 = x1 - x2;
			double y12 = y1 - y2;
			double dist = Math.sqrt(x12*x12 + y12*y12);
			String space;
			if (distInGCS) {
			    dist *= scaleFactor;
			    space = "GCS units";
			} else {
			    space = "Pts";
			}
			x1 = 0; y1 = 0;
			String distString = String.format("%g", dist);
			setModeline(String.format
				    (localeString("DistanceIs"),
				     distString, space));
			Clipboard cb = panel.getToolkit().getSystemClipboard();
			StringSelection selection =
			    new StringSelection(distString);
			cb.setContents(selection, selection);
			resetState();
		    } else if (nextState != null
			       && nextState instanceof
			       SplinePathBuilder.CPointType) {
			SplinePathBuilder.CPointType ns =
			    (SplinePathBuilder.CPointType) nextState;
			double xp =(p.x/zoom);
			double yp = (p.y/zoom);
			double x = xp;
			double y = height - yp;
			x *= scaleFactor;
			y *= scaleFactor;
			x += xrefpoint;
			y += yrefpoint;
			ptmodel.addRow("",ns, x, y, xp, yp);
			// This can call a radio-button menu item's
			// action listener's actionPeformed
			// method, and that sets the
			// nextState variable.
			ttable.nextState(ns);
		    } else {
			double xp =(p.x/zoom);
			double yp = (p.y/zoom);
			selectedRow = ptmodel.findRowXPYP(xp, yp, zoom);
			if (selectedRow != -1) {
			    PointTMR row = ptmodel.getRow(selectedRow);
			    lastXSelected = row.getX();
			    lastYSelected = row.getY();
			    lastXPSelected = row.getXP();
			    lastYPSelected = row.getYP();
			    xpoff = lastXPSelected - xp;
			    ypoff = lastYPSelected - yp;
			    String vn = ptmodel.getVariableName
				(ptmodel.findStart(selectedRow));
			    setModeline(String.format
					(localeString("SelectedPoint"),
					 selectedRow, vn, row.getMode()));
			    TransitionTable.getLocMenuItem().setEnabled(true);
			    
			} else {
			    setModeline("");
			}
			// selectedRowClick = true;
			panel.repaint();
		    }
		}
	    }
	    int mouseButton = -1;
	    public void mousePressed(MouseEvent e) {
		mouseButton = e.getButton();
		if (mouseButton == MouseEvent.BUTTON1) {
		    int modifiers = e.getModifiersEx();
		    altReallyPressed =
			(modifiers & KEY_MASK) == InputEvent.ALT_DOWN_MASK;
		    if ((modifiers & KEY_MASK) == InputEvent.CTRL_DOWN_MASK) {
			Point p = panel.getMousePosition();
			double xp =(p.x/zoom);
			double yp = (p.y/zoom);
			selectedRow = ptmodel.findRowXPYP(xp, yp, zoom);
			pointDragged = false;
			if (selectedRow != -1) {
			    PointTMR row = ptmodel.getRow(selectedRow);
			    xpoff = row.getXP() - xp;
			    ypoff = row.getYP() - yp;
			    String vn = ptmodel.getVariableName
				(ptmodel.findStart(selectedRow));
			    setModeline(String.format
					(localeString("SelectedPoint"),
					 selectedRow, vn, row.getMode()));
			}
			panel.repaint();
		    } else if (toggledAltD || altReallyPressed) {
			altPressed = true;
			JViewport vp = scrollPane.getViewport();
			Point p = vp.getViewPosition();
			e.translatePoint(-p.x, -p.y);
			vp.dispatchEvent(e);
		    } else {
			Point p = panel.getMousePosition();
			double xp =(p.x/zoom);
			double yp = (p.y/zoom);
			if (selectedRow != ptmodel.findRowXPYP(xp, yp, zoom)) {
			    // this is done in case we select a point,
			    // intend to click a location between points to
			    // deselected, but inadvertently drag the mouse.
			    if (selectedRow != -1 && nextState == null) {
				TransitionTable
				    .getLocMenuItem().setEnabled(false);
			    }
			    selectedRow = -1;
			    setModeline("");
			}
		    }
		}
		checkPopupMenu(e);
	    }

	    boolean pointDragged = false;
	    public void mouseReleased(MouseEvent e) {
		if (mouseButton == MouseEvent.BUTTON1) {
		    altReallyPressed = false;
		    if (altPressed) {
			JViewport vp = scrollPane.getViewport();
			Point p = vp.getViewPosition();
			e.translatePoint(-p.x, -p.y);
			vp.dispatchEvent(e);
		    }
		    if (pointDragged && selectedRow != -1) {
			ptmodel.fireRowChanged(selectedRow);
		    }
		    if (selectedRow != -1 && nextState == null) {
			TransitionTable.getLocMenuItem().setEnabled(false);
		    }
		    selectedRow = -1;
		    // selectedRowClick = false;
		    if (nextState == null) {
			setModeline("");
		    } else if (nextState instanceof
			       SplinePathBuilder.CPointType) {
			SplinePathBuilder.CPointType pstate =
			    (SplinePathBuilder.CPointType) nextState;
			setModeline(pstate);
		    }
		    panel.repaint();
		}
		mouseButton = -1;
		pointDragged = false;
		checkPopupMenu(e);
	    }

	    public void mouseMoved(MouseEvent e) {
		if (mouseTracked) {
		    lastx = (e.getX()/zoom);
		    lasty = (e.getY()/zoom);
		    panel.repaint();
		}
	    }

	    public void mouseDragged(MouseEvent e) {
		if (mouseButton == MouseEvent.BUTTON1) {
		    // only for button 1 because the mouse might be
		    // dragged with button 3 pushed if one accidentally
		    // moves the mouse outside of a popup menu.
		    // If the alt key is pressed, we preferentially scroll
		    // the viewport by appearing to move the underlying
		    // image.
		    if (selectedRow != -1 && altReallyPressed == false) {
			Point p = panel.getMousePosition();
			if (p == null) return;
			double xp =(p.x/zoom) + xpoff;
			double yp = (p.y/zoom) + ypoff;
			double x = xp;
			double y = height - yp;
			x *= scaleFactor;
			y *= scaleFactor;
			x += xrefpoint;
			y += yrefpoint;
			pointDragged = true;
			ptmodel.changeCoords(selectedRow, x, y, xp, yp, false);
			panel.repaint();
		    } else {
			JViewport vp = scrollPane.getViewport();
			Point p = vp.getViewPosition();
			e.translatePoint(-p.x, -p.y);
			vp.dispatchEvent(e);
		    }
		}
	    }
	};

    MouseInputAdapter vpmia = new MouseInputAdapter() {
	    int x1;
	    int y1;
	    int vx1;
	    int vy1;
	    int xmax;
	    int ymax;
	    boolean dragImage = false;
	    Cursor savedCursor = null;

	    public void mouseClicked(MouseEvent e) {
	    }

	    public void mousePressed(MouseEvent e) {
		if ((altPressed && selectedRow == -1 && nextState == null)
		    || altReallyPressed) {
		    x1 = (int)Math.round(e.getX());
		    y1 = (int)Math.round(e.getY());
		    JViewport vport = scrollPane.getViewport();
		    Point p = vport.getViewPosition();
		    vx1 = (int)Math.round(p.getX());
		    vy1 = (int)Math.round(p.getY());
		    xmax = panel.getWidth() - vport.getWidth();
		    ymax = panel.getHeight() - vport.getHeight();
		    savedCursor = panel.getCursor();
		    panel.setCursor(Cursor.getPredefinedCursor
				    (Cursor.MOVE_CURSOR));
		   dragImage = true;
		}
	    }
	    public void mouseReleased(MouseEvent e) {
		// if altReallyPressed was true when the mouse
		// was pressed, altPressed is also true
		if (altPressed) {
		    if (savedCursor != null) {
			panel.setCursor(savedCursor);
			savedCursor = null;
		    }
		    dragImage = false;
		    altPressed = false;
		}
	    }
	    public void mouseDragged(MouseEvent e) {
		if (dragImage) {
		    int offx = (int)Math.round(e.getX()) - x1;
		    int offy = (int)Math.round(e.getY()) - y1;
		    int vx2 = vx1 - offx;
		    int vy2 = vy1 - offy;
		    if (vx2 < 0) vx2 = 0;
		    if (vy2 < 0) vy2 = 0;
		    if (vx2 > xmax) vx2 = xmax;
		    if (vy2 > ymax) vy2 = ymax;
		    JViewport vport = scrollPane.getViewport();
		    vport.setViewPosition(new Point(vx2, vy2));
		    vport.repaint();
		}
	    }
	};

    double radius = 7.0;
    double radius2 = 2*radius;
    double lradius = radius*1.3;
    double lradius2 = radius2*1.3;
    double llradius = radius*1.4;
    double llradius2 = radius2*1.4;
    double w = radius2/Math.sqrt(2.0);
    double hw = w/2;
    double lw = w*1.3;
    double lhw = hw*1.3;

    public static TemplateProcessor.KeyMap
	getPathKeyMap(PointTableModel ptmodel, String name,
		      String[] vnames,
		      double flatness, int limit, boolean straight,
		      boolean elevate, boolean gcs)
    {
	Path2D path = getPath(ptmodel, vnames, gcs);
	if (path == null) return null;
	PathIterator pi = path.getPathIterator(null);
	if (limit > 0) {
	    if (straight) {
		pi = new FlatteningPathIterator(pi, flatness, limit);
	    } else {
		pi = new FlatteningPathIterator2D(pi, flatness, limit);
	    }
	}
	double[] coords = new double[6];
	double[] ncoords = elevate? new double[6]: null;
	TemplateProcessor.KeyMap kmap = new TemplateProcessor.KeyMap();
	kmap.put("varname", name);
	int irule = pi.getWindingRule();
	String rule = null;
	switch(irule) {
	case PathIterator.WIND_EVEN_ODD:
	    rule = "WIND_EVEN_ODD";
	    break;
	case PathIterator.WIND_NON_ZERO:
	    rule = "WIND_NON_ZERO";
	    break;
	}
	if (rule != null) {
	    kmap.put("windingRule", rule);
	}
	TemplateProcessor.KeyMapList list = new TemplateProcessor.KeyMapList();
	kmap.put("segments", list);
	double lastx = 0.0;
	double lasty = 0.0;
	double movex = 0.0;
	double movey = 0.0;
	// Key map indicating that a single iteration exists.
	// It doens't have to include any directives as all the needed
	// ones are defined at a higher level.
	TemplateProcessor.KeyMap emap = new TemplateProcessor.KeyMap();
	boolean allClosed = true;
	boolean closeSeen = false;
	boolean firstMoveTo = true;
	while (!pi.isDone()) {
	    int current = pi.currentSegment(coords);
	    TemplateProcessor.KeyMap imap = new TemplateProcessor.KeyMap();
	    String mode;
	    switch (current) {
	    case PathIterator.SEG_CLOSE:
		imap.put("type", "SEG_CLOSE");
		imap.put("method", "closePath");
		imap.put("hasClose", emap);
		lastx = movex;
		lasty = movey;
		closeSeen = true;
		break;
	    case PathIterator.SEG_CUBICTO:
		imap.put("type", "SEG_CUBICTO");
		imap.put("method", "curveTo");
		imap.put("hasCubicTo", emap);
		imap.put("has0", emap);
		imap.put("has1", emap);
		imap.put("has2", emap);
		imap.put("x0", String.format("%s", coords[0]));
		imap.put("y0", String.format("%s", coords[1]));
		imap.put("x1", String.format("%s", coords[2]));
		imap.put("y1", String.format("%s", coords[3]));
		imap.put("x2", String.format("%s", coords[4]));
		imap.put("y2", String.format("%s", coords[5]));
		lastx = coords[4];
		lasty = coords[5];
		break;
	    case PathIterator.SEG_LINETO:
		if (elevate) {
		    Path2DInfo.elevateDegree(1, ncoords, lastx, lasty, coords);
		    Path2DInfo.elevateDegree(2, coords, lastx, lasty, ncoords);
		    imap.put("type", "SEG_CUBICTO");
		    imap.put("method", "curveTo");
		    imap.put("hasCubicTo", emap);
		    imap.put("has0", emap);
		    imap.put("has1", emap);
		    imap.put("has2", emap);
		    imap.put("x0", String.format("%s", ncoords[0]));
		    imap.put("y0", String.format("%s", ncoords[1]));
		    imap.put("x1", String.format("%s", ncoords[2]));
		    imap.put("y1", String.format("%s", ncoords[3]));
		    imap.put("x2", String.format("%s", ncoords[4]));
		    imap.put("y2", String.format("%s", ncoords[5]));
		    lastx = ncoords[4];
		    lasty = ncoords[5];
		} else {
		    imap.put("type", "SEG_LINETO");
		    imap.put("method", "lineTo");
		    imap.put("hasLineTo", emap);
		    imap.put("has0", emap);
		    imap.put("x0", String.format("%s", coords[0]));
		    imap.put("y0", String.format("%s", coords[1]));
		    lastx = coords[0];
		    lasty = coords[1];
		}
		break;
	    case PathIterator.SEG_MOVETO:
		imap.put("type", "SEG_MOVETO");
		imap.put("method", "moveTo");
		imap.put("hasMoveTo", emap);
		imap.put("has0", emap);
		imap.put("x0", String.format("%s", coords[0]));
		imap.put("y0", String.format("%s", coords[1]));
		lastx = coords[0];
		lasty = coords[0];
		movex = lastx;
		movey = lasty;
		if (firstMoveTo) {
		    firstMoveTo = false;
		} else {
		    if (!closeSeen) allClosed = false;
		}
		closeSeen = false;
		break;
	    case PathIterator.SEG_QUADTO:
		if (elevate) {
		    Path2DInfo.elevateDegree(2, ncoords, lastx, lasty, coords);
		    imap.put("type", "SEG_CUBIC");
		    imap.put("method", "curveTo");
		    imap.put("hasCubicTo", emap);
		    imap.put("has0", emap);
		    imap.put("has1", emap);
		    imap.put("has2", emap);
		    imap.put("x0", String.format("%s", ncoords[0]));
		    imap.put("y0", String.format("%s", ncoords[1]));
		    imap.put("x1", String.format("%s", ncoords[2]));
		    imap.put("y1", String.format("%s", ncoords[3]));
		    imap.put("x2", String.format("%s", ncoords[4]));
		    imap.put("x2", String.format("%s", ncoords[5]));
		} else {
		    imap.put("type", "SEG_QUADTO");
		    imap.put("hasQuadTo", emap);
		    imap.put("method", "quadTo");
		    imap.put("has0", emap);
		    imap.put("has1", emap);
		    imap.put("x0", String.format("%s", coords[0]));
		    imap.put("y0", String.format("%s", coords[1]));
		    imap.put("x1", String.format("%s", coords[2]));
		    imap.put("y1", String.format("%s", coords[3]));
		}
		lastx = coords[2];
		lasty = coords[3];
		break;
	    }
	    list.add(imap);
	    pi.next();
	}
	if (closeSeen == false) allClosed = false;
	if (allClosed) {
	    kmap.put("area", "" + Path2DInfo.areaOf(path));
	    kmap.put("circumference", "" + Path2DInfo.circumferenceOf(path));
	} else {
	    kmap.put("area", "NaN");
	    kmap.put("circumference", "NaN");
	}
	kmap.put("pathLength", "" + Path2DInfo.pathLength(path));
	return kmap;
    }

    private static boolean isOurPath(String[] names, String name) {
	for (String s: names) {
	    if (name.equals(s)) {
		return true;
	    }
	}
	return false;
    }

    private static Path2D getPath(PointTableModel ptmodel, String[] names,
				  boolean gcs)
    {
	SplinePathBuilder pb = new SplinePathBuilder();
	pb.initPath();
	boolean ignore = true;
	boolean ok = false;
	int nrows = ptmodel.getRowCount();
	int index = 0;
	for (PointTMR row: ptmodel.getRows()) {
	    Enum mode = row.getMode();
	    if (ignore) {
		if (mode == EPTS.Mode.PATH_START
		    && isOurPath(names, row.getVariableName())) {
		    ignore = false;
		}
	    } else {
		if (mode instanceof SplinePathBuilder.CPointType) {
		    SplinePathBuilder.CPointType smode =
			(SplinePathBuilder.CPointType) mode;
		    double x = gcs? row.getX(): row.getXP();
		    double y = gcs? row.getY(): row.getYP();
		    ok = true;
		    switch (smode) {
		    case CLOSE:
			pb.append(new SplinePathBuilder.CPoint(smode));
			break;
		    default:
			if (index == nrows-1) {
			    pb.append(new SplinePathBuilder.CPoint
				      (SplinePathBuilder.CPointType.SEG_END,
				       x,  y));
			} else {
			    pb.append(new SplinePathBuilder.CPoint(smode,
								   x, y));
			}
			break;
		    }
		} else if (mode == EPTS.Mode.PATH_END) {
		    ignore = true;
		}
	    }
	    index++;
	}
	return ok? pb.getPath(): null;
    }

    private void drawRows(PointTableModel ptmodel, Graphics2D g2d,
			  Graphics2D g2d2)
    {
	double prevx = -1;
	double prevy = -1;
	double w = radius2/Math.sqrt(2.0);
	double hw = w/2;
	int nrows = ptmodel.getRowCount();
	int index = 0;
	SplinePathBuilder.CPointType lastMode = null;
	SplinePathBuilder pb = new SplinePathBuilder();
	pb.initPath();
	for (PointTMR row: ptmodel.getRows()) {
	    if (!row.isDrawable()) {
		index++;
		continue;
	    }
	    double x = zoom*row.getXP();
	    double y = zoom*row.getYP();
	    Enum mode = row.getMode();
	    if (mode instanceof EPTS.Mode) {
		EPTS.Mode emode = (EPTS.Mode)mode;
		switch(emode) {
		case LOCATION:
		    if (row.isSelectable()) {
			g2d2.draw(new Ellipse2D.Double
				  (x-radius, y-radius, radius2, radius2));
			g2d.fill(new Ellipse2D.Double
				 (x-radius, y-radius, radius2, radius2));
			if (index == selectedRow) {
			    g2d2.draw(new Ellipse2D.Double
				      (x-llradius, y-llradius,
				       llradius2, llradius2));
			    g2d.draw(new Ellipse2D.Double
				     (x-lradius, y-lradius,
				      lradius2, lradius2));
			}
		    }
		    break;
		case PATH_START:
		    break;
		case PATH_END:
		    break;
		}
		lastMode = null;
	    } else if (mode instanceof SplinePathBuilder.CPointType) {
		SplinePathBuilder.CPointType smode =
		    (SplinePathBuilder.CPointType) mode;
		switch(smode) {
		case MOVE_TO:
		    if (row.isSelectable()) {
			g2d2.draw(new Ellipse2D.Double
				  (x-radius, y-radius, radius2, radius2));
			g2d.fill(new Ellipse2D.Double
				 (x-radius, y-radius, radius2, radius2));
			if (index == selectedRow) {
			    g2d2.draw(new Ellipse2D.Double
				      (x-llradius, y-llradius,
				       llradius2, llradius2));
			    g2d.draw(new Ellipse2D.Double
				     (x-lradius, y-lradius,
				      lradius2, lradius2));
			}
		    }
		    pb.append(new SplinePathBuilder.CPoint(smode, x, y));
		    prevx = x;
		    prevy = y;
		    break;
		case SPLINE:
		    if (row.isSelectable()) {
			g2d2.draw(new Ellipse2D.Double
				  (x-radius, y-radius, radius2, radius2));
			g2d.fill(new Ellipse2D.Double
				 (x-radius, y-radius, radius2, radius2));
			if (index == selectedRow) {
			    g2d2.draw(new Ellipse2D.Double
				      (x-llradius, y-llradius,
				       llradius2, llradius2));
			    g2d.draw(new Ellipse2D.Double
				     (x-lradius, y-lradius,
				      lradius2, lradius2));
			}
		    }
		    if (index == nrows-1) {
			pb.append(new SplinePathBuilder.CPoint
				  (SplinePathBuilder.CPointType.SEG_END,
				   x,  y));
		    } else {
			pb.append(new SplinePathBuilder.CPoint(smode, x, y));
		    }
		    prevx = x;
		    prevy = y;
		    break;
		case SEG_END:
		    if (row.isSelectable()) {
			g2d2.draw(new Ellipse2D.Double
				  (x-radius, y-radius, radius2, radius2));
			g2d.fill(new Ellipse2D.Double
				 (x-radius, y-radius, radius2, radius2));
			if (index == selectedRow) {
			    g2d2.draw(new Ellipse2D.Double
				      (x-llradius, y-llradius,
				       llradius2, llradius2));
			    g2d.draw(new Ellipse2D.Double
				     (x-lradius, y-lradius,
				      lradius2, lradius2));
			}
		    }
		    if (lastMode == SplinePathBuilder.CPointType.CONTROL) {
			g2d2.draw(new Line2D.Double(prevx, prevy, x, y));
			g2d.draw(new Line2D.Double(prevx, prevy, x, y));
		    }
		    pb.append(new SplinePathBuilder.CPoint(smode, x, y));
		    prevx = x;
		    prevy = y;
		    break;
		case CONTROL:
		    if (row.isSelectable()) {
			g2d2.draw(new Rectangle.Double
				  (x-radius, y-radius, radius2, radius2));
			g2d.fill(new Rectangle2D.Double(x-hw, y-hw, w, w));
			if (index == selectedRow) {
			    g2d2.draw(new Rectangle2D.Double
				      (x-llradius, y-llradius,
				       llradius2, llradius2));
			    g2d.draw(new Rectangle2D.Double
				     (x-lhw, y-lhw, lw, lw));
			}
		    }
		    if (lastMode == SplinePathBuilder.CPointType.SEG_END
			|| lastMode == SplinePathBuilder.CPointType.MOVE_TO) {
			g2d2.draw(new Line2D.Double(prevx, prevy, x, y));
			g2d.draw(new Line2D.Double(prevx, prevy, x, y));
		    }
		    if (index == nrows-1) {
			pb.append(new SplinePathBuilder.CPoint
				  (SplinePathBuilder.CPointType.SEG_END,
				   x,  y));
		    } else {
			pb.append(new SplinePathBuilder.CPoint(smode, x, y));
		    }
		    prevx = x;
		    prevy = y;
		    break;
		case CLOSE:
		    pb.append(new SplinePathBuilder.CPoint(smode));
		    break;
		}
		lastMode = smode;
	    } 
	    index++;
	}
	g2d2.draw(pb.getPath());
	g2d.draw(pb.getPath());
    }


    public static void setPort(int port) {
	EPTSWindow.port = port;
	if (port != 0) {
	    try {
		startManualWebServerIfNeeded();
	    } catch (Exception e) {
	    }
	}
    }

    ScriptingEnv se = null;

    void init(Image image, boolean ifns, ScriptingEnv se)
	throws IOException, InterruptedException
    {
	// this.targetList = targetList;
	this.se = se;
	imageFileNameSeen = ifns;
	if (image != null) {
	    width = image.getWidth(null);
	    height = image.getHeight(null);
	    Graph graph = new Graph(width, height,
				    Graph.ImageType.INT_ARGB_PRE);
	    graph.setRanges(0.0, 0.0, 0.0, 0.0, 1.0, 1.0);
	    bi = graph.getImage();
	    g2d = bi.createGraphics();
	    BlockingImageObserver bio =
		new BlockingImageObserver(true, true, true, true);
	    if (!g2d.drawImage(image, new AffineTransform(), bio)) {
		bio.waitUntilDone();
		g2d.drawImage(image, new AffineTransform(), bio);
	    }
	} else {
	    try {
		Graph g = se.getGraph();
		width = g.getWidthAsInt();
		height = g.getHeightAsInt();
	    } catch (ScriptException e) {
		throw new IOException(e.getMessage());
	    }
	}
	this.port = port;

	SwingUtilities.invokeLater(new  Runnable () {
		public void run() {
		    AffineTransform identityAF = new AffineTransform();
		    panel = new JPanel() {
			    public void paintComponent(Graphics g) {
				super.paintComponent(g);
				AffineTransform af =
				    (zoom == 1.0 || bi == null)? identityAF:
				    AffineTransform.getScaleInstance(zoom,
								     zoom);
				if (g instanceof Graphics2D) {
				    Graphics2D g2d = (Graphics2D)g;
				    g2d.setPaintMode();
				    if (bi != null && se == null) {
					g2d.drawImage(bi, af, null);
				    }
				    if (se != null) {
					g2d.drawImage(se.zoomImage(zoom, bi),
						      identityAF, null);
				    }
				    Graphics2D g2d2 = (Graphics2D)g2d.create();
				    Stroke savedStroke = g2d.getStroke();
				    Color savedColor = g2d.getColor();
				    try {
					g2d.setStroke(new BasicStroke(2.0F));
					g2d.setColor(Color.BLACK);
					g2d2.setStroke(new BasicStroke(4.0F));
					g2d2.setColor(Color.WHITE);
					if (ptmodel.getRowCount() > 0) {
					    drawRows(ptmodel, g2d, g2d2);
					}
					// g2d.setColor(Color.WHITE);
					// g2d.setXORMode(Color.BLACK);
					if (mouseTracked) {
					    g2d2.draw(new Line2D.Double
						      (startx*zoom, starty*zoom,
						       lastx*zoom, lasty*zoom));
					    g2d.draw(new Line2D.Double
						     (startx*zoom, starty*zoom,
						      lastx*zoom, lasty*zoom));
					}
				    } finally {
					g2d2.dispose();
					g2d.setStroke(savedStroke);
					g2d.setColor(savedColor);
					g2d.setPaintMode();
				    }
				}
			    }
			};
		    JPanel container = new JPanel(new BorderLayout());
		    modeline = new JLabel(" ");
		    container.add(modeline, BorderLayout.NORTH);
		    Dimension d = new Dimension(width, height);
		    panel.setPreferredSize(d);
		    scrollPane = new JScrollPane(panel);
		    setupTable(scrollPane);
		    JViewport vport = scrollPane.getViewport();
		    frame = new JFrame();
		    Toolkit tk = frame.getToolkit();
		    if (tk != null) {
			Dimension screensize = tk.getScreenSize();
			double sw = screensize.getWidth();
			double sh = screensize.getHeight();
			double w = sw / 2;
			double h = sh / 2;
			double ar = ((double)width)/((double)height);
			// keep it closer to square
			ar = Math.sqrt(ar);
			if (w > h) {
			    w = h;
			} else if (w < h) {
			    h = w;
			}
			if (ar > 1.0) {
			    h =  (w/ar);
			} else if (ar < 1.0) {
			    w = (h*ar);
			}
			if (h < sh/4) {
			    h = sh/4;
			}
			if (w < sw/4) {
			    w = sw/4;
			}
			// leave enough for menu bar as an absolute min.
			if (w < 300) w = 300;
			frame.setPreferredSize(new Dimension
					       ((int)Math.round(Math.ceil(w)),
						(int)Math.round(Math.ceil(h))));
			setMenus(frame, w, h);
		    } else {
			// fallback in case we somehow can't get the
			// graphics context
			frame.setPreferredSize(new Dimension(700, 700));
			setMenus(frame, 700.0, 700.0);
		    }
		    if (savedFile != null) {
			createTemplateMenuItem.setEnabled(true);
		    }
		    frame.addKeyListener(kba);
		    panel.addMouseListener(mia);
		    panel.addMouseMotionListener(mia);
		    vport.addMouseListener(vpmia);
		    vport.addMouseMotionListener(vpmia);
		    scrollPane.setOpaque(true);
		    container.add(scrollPane, BorderLayout.CENTER);
		    frame.setContentPane(container);
		    frame.pack();
		    // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    frame.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent e) {
				// quitListener does not check its
				// action event.
				if (quitMenuItem.isEnabled()) {
				    quitListener.actionPerformed(null);
				}
				// If we get here, we didn't exit because
				// the exit operation was canceled.
				// In that case, we make the window
				// visible again.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
					    frame.setVisible(true);
					}
				    });
			    }
			});
		    frame.setIconImages(EPTS.getIconList());
		    configGCSPane = new ConfigGCSPane();
		    frame.setVisible(true);
		}
	    });
    }

    private void setupGCSConfigPane(EPTSParser parser) {
	configGCSPane.savedUnitIndex
	    = parser.getUnitIndex();
	configGCSPane.savedRefPointIndex
	    = parser.getRefPointIndex();
	configGCSPane.savedUsDistString =
	    parser.getUserSpaceDistance();
	configGCSPane.savedGcsDistString =
	    parser.getGcsDistance();
	configGCSPane.savedXString = parser.getXRefpoint();
	configGCSPane.savedYString = parser.getYRefpoint();
	configGCSPane.restoreState();
	acceptConfigParms();
    }

    private void setupFilters(EPTSParser parser) {
	List<PTFilters.TopEntry> flist = parser.getFilterList();
	if (flist == null) return;
	for (PTFilters.TopEntry tentry: flist) {
	    ptfilters.addFilter(tentry.name, panel, tentry.mode,
				tentry.entries);
	}
    }


    public EPTSWindow(final EPTSParser parser, File inputFile)
	throws IllegalStateException, IOException, InterruptedException
    {
	savedFile = inputFile;
	if (parser != null) {
	    savedStateCodebase = parser.getCodebase();
	    savedStateModules = parser.getModules();
	    savedStateClasspath = parser.getClasspath();
	    if (parser.imageURIExists()) {
		URI uri = parser.getImageURI();
		Image image = parser.getImage();
		imageURI = uri;
		init(image, (uri != null), null);
		// now restore state.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    /*
			      configGCSPane.savedUnitIndex
			      = parser.getUnitIndex();
			      configGCSPane.savedRefPointIndex
			      = parser.getRefPointIndex();
			      configGCSPane.savedUsDistString =
			      parser.getUserSpaceDistance();
			      configGCSPane.savedGcsDistString =
			      parser.getGcsDistance();
			      configGCSPane.savedXString = parser.getXRefpoint();
			      configGCSPane.savedYString = parser.getYRefpoint();
			      configGCSPane.restoreState();
			      acceptConfigParms();
			    */
			    setupGCSConfigPane(parser);
			    for (PointTMR row: parser.getRows()) {
				ptmodel.addRow(row);
			    }
			    addToPathMenuItem.setEnabled
				(ptmodel.pathVariableNameCount() > 0);
			    deletePathMenuItem.setEnabled
				(ptmodel.pathVariableNameCount() > 0);
			    setupFilters(parser);
			}
		    });
	    }
	}
    }

    public EPTSWindow(Image image, URI imageURI)
	throws IOException, InterruptedException
    {
	this.imageURI = imageURI;
	init(image, true, null);
    }

    public EPTSWindow(final Graph graph,
		      List<EPTS.NameValuePair> bindings,
		      List<String> targetList,
		      final boolean custom,
		      final ScriptingEnv se,
		      // String languageName, String a2dName,
		      final PointTMR[] rows,
		      Image image, URI imageURI,
		      final EPTSParser parser,
		      File inputFile)
    {
	this.imageURI = imageURI;
	this.savedFile = inputFile;
	try {
	    init(image /*graph.getImage() */, (image != null), se);
	} catch (Exception e) {
	    throw new UnexpectedExceptionError(e);
	}

	this.bindings = bindings;
	this.targetList = targetList;
	this.se = se;
	if (se != null) {
	    this.languageName = se.getLanguageName();
	    this.animationName = se.getAnimationName();
	    scriptMode = true;
	    shouldSaveScripts = (parser == null) || (image == null);
	}
	if (parser != null) {
	    savedStateCodebase = parser.getCodebase();
	    savedStateModules = parser.getModules();
	    savedStateClasspath = parser.getClasspath();
	}
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    if (parser != null && image != null) {
			setupGCSConfigPane(parser);
		    } else if (image == null) {
			configGCSPane.savedUnitIndex = custom? 0: 5;
			configGCSPane.savedRefPointIndex = 6;
			configGCSPane.savedUsDistString = ""
			    + graph.getXScale();
			configGCSPane.savedGcsDistString = "1.0";
			configGCSPane.savedXString = "" + graph.getXLower();
			configGCSPane.savedYString = "" + graph.getYLower() ;
			configGCSPane.restoreState();
			acceptConfigParms();
			configGCSPane.setEditable(false);
		    }
		    if (rows != null) {
			for (PointTMR row: rows) {
			    ptmodel.addRow(row);
			}
			addToPathMenuItem.setEnabled
			    (ptmodel.pathVariableNameCount() > 0);
			deletePathMenuItem.setEnabled
			    (ptmodel.pathVariableNameCount() > 0);
		    }
		    if (parser != null) {
			setupFilters(parser);
		    }
		}
	    });
    }
}
