package org.bzdev.epts;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.datatransfer.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
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
import org.bzdev.geom.PathSplitter;
import org.bzdev.geom.SplinePath2D;
import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.CPointType;
import org.bzdev.graphs.Graph;
import org.bzdev.graphs.RefPointName;
import org.bzdev.lang.MathOps;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.imageio.BlockingImageObserver;
import org.bzdev.io.CSVReader;
import org.bzdev.io.LineReader;
import org.bzdev.io.LineReader.Delimiter;
import org.bzdev.math.Functions;
import org.bzdev.math.VectorOps;
import org.bzdev.net.WebEncoder;
import org.bzdev.util.CopyUtilities;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;
import org.bzdev.util.TemplateProcessor.KeyMapList;
import org.bzdev.swing.DarkmodeMonitor;
import org.bzdev.swing.SwingErrorMessage;
import org.bzdev.swing.HtmlWithTocPane;
import org.bzdev.swing.PortTextField;
import org.bzdev.swing.SwingOps;
import org.bzdev.swing.InputTablePane;
import org.bzdev.swing.InputTablePane.ColSpec;
import org.bzdev.swing.VTextField;
import org.bzdev.swing.VTextFieldMenuItem;
import org.bzdev.swing.text.CharDocFilter;

/**
 * Principal moments and axes.
 * For EPTS, we want the first prinical axis to have the
 * largest principal moment, and the second the smallest.
 * We also want the first pricipal axes to preferentially
 * point towards the positive X axis, with the second principal
 * axis pointing 90 degrees from the first in the counterclockwise
 * direction. This class computes the principal moments and
 * principal axes given these constraints.
 */
class Moments {
    /**
     * Find the principle moments of a shape given its moments.
     * The array returned contains the moments sorted so that the
     * largest moment appears first.  The argument array can be
     * computed from a shape by calling {@link #momentsOf(Shape)} or
     * {@link #momentsOf(Shape, AffineTransform)}.
     * <P>
     * The principal moments are the eigenvalues of the moments
     * matrix.
     * @param moments the shape's moments.
     * @return the principal moments
     */
    public static double[] principalMoments(double[][] moments) {
	if (moments[0][1] != moments[1][0]) {
	    throw new IllegalArgumentException();
	}
	if (moments[0][1] == 0.0) {
	    double val1 = moments[0][0];
	    double val2 = moments[1][1];
	    double results[] = {Math.max(val1, val2), Math.min(val1, val2)};
	    return results;
	}
	double negB = moments[0][0] + moments[1][1];
	double radical = (moments[0][0] - moments[1][1]);
	radical *= radical;
	double term = moments[0][1];
	term *= term;
	radical += 4*term;
	if (radical == 0.0) {
	    double results[] = {moments[0][0], moments[1][1]};
	    return results;
	} else {
	    radical = Math.sqrt(radical);
	    double results[] = {0.5*(negB + radical),
				0.5*(negB - radical)};
	    return results;
	}
    }

    /**
     * Compute the axes corresponding to the principal moments of a
     * moments matrix.
     * The moments matrix can be computed from a shape by calling the
     * method {@link #momentsOf(Shape)} or by calling the method
     * {@link #momentsOf(Shape, AffineTransform)}. The method
     * {@link #principalMoments(double[][])} can be used to compute
     * the principal moments given a moments array.  The return value
     * for principalAxes is a two dimensional array.  If the array
     * returned is stored as pmatrix, then pmatrix[i] is an array
     * containing the principal axis corresponding to the
     * i<sup>th</sup> principal moment. Each principal axis vector
     * contains its X component followed by its Y component and
     * is normalized so its length is 1.0.
     * <P>
     * The principal axes are actually the eigenvectors of the moments
     * matrix provided as this method's first argument.  In addition,
     * the eigenvector at index 1 points counterclockwise from the one
     * at index 0, assuming the convention in which the positive Y
     * axis is counterclockwise from the positive X axis.
     * @param moments the moments
     * @param principalMoments the principle moments
     * @areturn an array of vectors, each providing an axis.
     */
    public static double[][] principalAxes(double[][] moments,
					   double[] principalMoments)
    {
	if (moments[0][1] != moments[1][0]) {
	    throw new IllegalArgumentException();
	}

	if (principalMoments[0] == principalMoments[1]) {
	    double results[][] = {{1.0, 0.0}, {0.0, 1.0}};
	    return results;
	}
	double[][] results = new double[2][2];
	if (moments[0][1] == 0.0) {
	    if (principalMoments[0] == moments[0][0]) {
		    results[0][0] = 1.0;
		    results[0][1] = 0.0;
		    results[1][0] = 0.0;
		    results[1][1] = 1.0;
	    } else {
		results[0][0] = 0.0;
		results[0][1] = 1.0;
		results[1][0] = -1.0;
		results[1][1] = 0.0;
	    }
	    return results;
	}
	double v1 = 1.0;
	// double v2 = -(moments[0][0] - principalMoments[0]) / moments[0][1];
	double v2 = (principalMoments[0] - moments[0][0]) / moments[0][1];
	results[0][0] = v1;
	results[0][1] = v2;
	VectorOps.normalize(results[0]);
	results[1][1] = results[0][0];
	results[1][0] = -results[0][1];
	return results;
    }
}

class AnglePane extends JPanel {
    static int aindexSaved = 0;
    int aindex = aindexSaved;
    void saveIndices() {
	aindexSaved = aindex;
    }
    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    VTextField atf;		// angle text field
    static Vector<String> av = new Vector<>(2);
    static {
	av.add("Degrees");
	av.add("Radians");
    }
    JComboBox<String> aunits = new JComboBox<>(av);
    boolean firstTime = true;
    CharDocFilter cdf = new CharDocFilter();
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
    double angle = 0.0;
    public double getAngle() {
	return (aindex == 0)? Math.toRadians(angle): angle;
    }

    boolean noPrevErrors = true;

    public AnglePane() {
	super();
	cdf.setAllowedChars("09eeEE..,,++--");
	JLabel al = new JLabel(localeString("TangentAngle"));
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
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)atf.getDocument()).setDocumentFilter(cdf);
	atf.setInputVerifier(atfiv);
	JButton okButton = new JButton(localeString("OK"));
	okButton.addActionListener((e) -> {
		Window w = SwingUtilities.getWindowAncestor(AnglePane.this);
		w.setVisible(false);
	    });

	/*
	okButton.addFocusListener(new FocusListener() {
		public void focusGained(FocusEvent e) {
		    System.out.println("okButton has Focus");
		}
		public void focusLost(FocusEvent e) {
		    System.out.println("okButton lost Focus");
		}
	    });
	*/

	// Normally the OK button will only respond to a 'space'.
	// we want it to respond to a return so it works like a
	// normal dialog box.
	KeyAdapter ka = new KeyAdapter() {
		boolean notPressed = true;
		public void keyPressed(KeyEvent e) {
		    switch(e.getKeyChar()) {
		    case KeyEvent.VK_ENTER:
			if (notPressed) {
			    Window w =
				SwingUtilities.getWindowAncestor
				(AnglePane.this);
			    w.setVisible(false);
			    notPressed = false;
			}
			break;
		    default:
			break;
		    }
		}
	    };
	okButton.addKeyListener(ka);

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	gridbag.setConstraints(al, c);
	add(al);
	gridbag.setConstraints(atf, c);
	add(atf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(aunits, c);
	add(aunits);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(okButton, c);
	add(okButton);

	aunits.addActionListener((ae) -> {
		aindex = aunits.getSelectedIndex();
	    });
    }
}

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

    public void setAngle(double angle) {
	this.angle = (aindex == 0)? Math.toDegrees(angle): angle;
	String sangle = String.format("%.7g", this.angle);
	// System.out.println("angle = " + this.angle);
	// System.out.println("sangle = " + sangle);
	atf.setText(sangle);
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
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
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
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)atf.getDocument()).setDocumentFilter(cdf);
	atf.setInputVerifier(atfiv);
	atf.addFocusListener(new FocusAdapter() {
		boolean firstTime = true;
		@Override
		public void focusGained(FocusEvent e) {
		    String text = atf.getText();
		    if (firstTime) {
			atf.setCaretPosition(0);
			atf.moveCaretPosition(text.length());
			firstTime = false;
		    }
		}
	    });


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

class OffsetPane extends JPanel {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    static double dist1Saved = 0.0;
    static double dist2Saved = 0.0;
    static double dist3Saved = 0.0;

    static int uindex1Saved = 0;
    static int uindex2Saved = 0;
    static int uindex3Saved = 0;
    // static int nsegIndexSaved = 1;

    static String lastBaseName = null;

    int mindex = 0;
    int uindex1 = uindex1Saved;
    int uindex2 = uindex2Saved;
    int uindex3 = uindex3Saved;
    // int nsegIndex = nsegIndexSaved;

    void saveDistances() {
	dist1Saved = dist1;
	dist2Saved = dist2;
    }

    void saveIndices() {
	if (newBasename) {
	    uindex1Saved = uindex1;
	    uindex2Saved = uindex2;
	    uindex3Saved = uindex3;
	}
	// nsegIndexSaved = nsegIndex;
    }
    JComboBox<String> pcb;

    public String getBasePathName() {
	if (pcb.getSelectedIndex() == 0) return null;
	// System.out.println("basename = " + (String)pcb.getSelectedItem());
	return ((String)pcb.getSelectedItem()).trim();
    }

    JComponent targetTF = null;
    public JComponent getTargetTF() {return targetTF;}

    private static String[] modes = {
	localeString("closedPathMode"), // 0
	localeString("CCWPathMode"),  // 1
	localeString("CCWReversedPathMode"), // 2
	localeString("CWPathMode"),  // 3
	localeString("CWReversedPathMode"), // 4
    };

    JComboBox<String> modesCB = new JComboBox<>(modes);

    /*
    private static String[] dirs = {
	localeString("CCWSide"),
	localeString("CWSide")
    };

    JComboBox<String> dirCB = new JComboBox<>(dirs);
    */

    public boolean requestsClosedPath() {
	return modesCB.getSelectedIndex() == 0;
    }
    public boolean requestsReversedPath() {
	int index = modesCB.getSelectedIndex();
	return (index > 0 && index%2 == 0);
    }

    public boolean requestsCCWPath() {
	int index = modesCB.getSelectedIndex();
	// System.out.println("requestsCCWPath: index = " + index);
	return (index > 0 && index < 3);
    }

    public boolean requestsCWPath() {
	int index = modesCB.getSelectedIndex();
	// System.out.println("requestsCWPath: index = " + index);
	return (index > 2 && index < 5);
    }

    JComboBox<String> lunits1 = new JComboBox<>(ConfigGCSPane.units);
    JComboBox<String> lunits2 = new JComboBox<>(ConfigGCSPane.units);
    JComboBox<String> lunits3 = new JComboBox<>(ConfigGCSPane.units);
    CharDocFilter cdf = new CharDocFilter();
    InputVerifier utfiv1 = new InputVerifier() {
	    boolean firstTime = true;

	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		if (string == null) string = "";
		string = string.trim();
		try {
		    if (string.length() == 0) {
			if (firstTime) {
			    return true;
			} else {
			    return false;
			}
		    }
		    double value = Double.parseDouble(string);
		    if (value >= 0.0) {
			firstTime = false;
			return true;
		    } else {
			return false;
		    }
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    InputVerifier utfiv2 = new InputVerifier() {
	    boolean firstTime = true;

	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		if (string == null) string = "";
		string = string.trim();
		try {
		    if (string.length() == 0) {
			if (firstTime) {
			    return true;
			} else {
			    return false;
			}
		    }
		    double value = Double.parseDouble(string);
		    if (value >= 0.0) {
			firstTime = false;
			return true;
		    } else {
			return false;
		    }
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    InputVerifier utfiv3 = new InputVerifier() {
	    boolean firstTime = true;

	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		if (string == null) string = "";
		string = string.trim();
		try {
		    if (string.length() == 0) {
			if (firstTime) {
			    return true;
			} else {
			    return false;
			}
		    }
		    double value = Double.parseDouble(string);
		    if (value >= 0.0) {
			firstTime = false;
			return true;
		    } else {
			return false;
		    }
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    /*
    static Vector<String> nv = new Vector<>(10);
    static {
	nv.add("1");
	nv.add("2");
	nv.add("3");
	nv.add("4");
    }
    */

    /*
    JComboBox<String> nsegComboBox = new JComboBox<>(nv);

    public double getMaxDelta() {
	int divisor = 1 << nsegComboBox.getSelectedIndex();
	return (Math.PI/2.0)/(divisor);
    }
    */

    VTextField d1tf;
    VTextField d2tf;
    VTextField d3tf;

    double dist1 = dist1Saved;
    double dist2 = dist2Saved;
    double dist3 = 0.0;

    public double getDist1() {
	return ConfigGCSPane.convert[uindex1].valueAt(dist1);
    }
    public double getDist2() {
	return ConfigGCSPane.convert[uindex2].valueAt(dist2);
    }
    public double getDist3() {
	return ConfigGCSPane.convert[uindex3].valueAt(dist3);
    }

    public static class BME {
	int mindex;
	double dist1;
	double dist2;
	double dist3;
	int uindex1;
	int uindex2;
	int uindex3;
	int count = 0;
    }

    static Map<String,BME> baseMap = new TreeMap<String,BME>();

    public void saveBaseMap() {
	String name = getBasePathName();
	if (name != null) {
	    BME entry = baseMap.get(name);
	    if (entry == null) entry = new BME();
	    entry.mindex = mindex;
	    entry.dist1 = dist1;
	    entry.dist2 = dist2;
	    entry.dist3 = dist3;
	    entry.uindex1 = uindex1;
	    entry.uindex2 = uindex2;
	    entry.uindex3 = uindex3;
	    baseMap.put(name, entry);
	}
    }

    public static void removeBasename(String name) {
	BME entry = baseMap.remove(name);
	if (entry != null) {
	    // fix up ourpaths
	    Iterator<Map.Entry<String,String>> iterator
		= ourpaths.entrySet().iterator();
	    while (iterator.hasNext()) {
		Map.Entry mentry = iterator.next();
		if (mentry.getValue().equals(name)) {
		    iterator.remove();
		}
	    }
	}
    }

    static Map<String,String> ourpaths = new HashMap<String,String>();

    public static void addOurPathName(String name, String bname) {
	if (!ourpaths.containsKey(name)) {
	    ourpaths.put(name, bname);
	    BME entry = baseMap.get(bname);
	    if (entry != null) entry.count++;
	}
    }

    public static void removeOurPathName(String name) {
	String bname = ourpaths.remove(name);
	if (bname != null) {
	    BME entry = baseMap.get(bname);
	    if (entry != null) {
		entry.count--;
		if (entry.count == 0) {
		    baseMap.remove(bname);
		}
	    }
	}
    }

    boolean baseMode = false;
    public boolean getBaseMode() {return baseMode;}

    boolean newBasename = false;


    public OffsetPane( PointTableModel ptmodel) {
	super();
	cdf.setAllowedChars("09eeEE..,,++--");
	JLabel nsl = new JLabel(localeString("NSegs"));
	JLabel pl = new JLabel(localeString("pathLabel"));
	JLabel d1l = new JLabel(localeString("dist1"));
	JLabel d2l = new JLabel(localeString("dist2"));
	JLabel d3l = new JLabel(localeString("dist3"));
	JLabel ml = new JLabel(localeString("offsetMode"));

	Vector<String> pathnames =
	    new Vector<>(ptmodel.pathVariableNameCount() + 2);
	pathnames.add(localeString("PCBHDR"));
	for (String name: ptmodel.getPathVariableNames()) {
	    pathnames.add(name);
	}

	// in order of appearance in the table
	List<String> allpathnames =
	    ptmodel.getPathVariableNamesInTableOrder();
	int  allpathnamesSize = allpathnames.size();

	boolean usingLastBaseName = false;
	int baseInd = ptmodel.pathVariableNameCount();
	String baseName;
	if (baseInd > 0) {
	    baseName = allpathnames.get(baseInd-1);
	    // if what we get is actually one of the paths
	    // we created, skip back further until we find a
	    // good candiate for a base name: it would be very
	    // unusual to use an offset path as a base name.
	    while (ourpaths.containsKey(baseName)) {
		baseInd--;
		if (baseInd == 0) {
		    baseName = null;
		    break;
		}
		baseName = allpathnames.get(baseInd-1);
	    }
	} else {
	    baseName = null;
	}
	// System.out.println("initial base name = " + baseName);
	baseInd = 0;
	if (baseName != null) {
	    for (String name: pathnames) {
		if (baseInd > 0) {
		    if (name.equals(baseName)) {
			break;
		    }
		}
		baseInd++;
	    }
	}
	// System.out.println("initial base index = " +  baseInd);

	pcb = new JComboBox<String>(pathnames);

	d1tf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			dist1 = 0.0;
		    } else {
			dist1 = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)d1tf.getDocument()).setDocumentFilter(cdf);
	d1tf.setInputVerifier(utfiv1);

	d2tf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			dist2 = 0.0;
		    } else {
			dist2 = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)d2tf.getDocument()).setDocumentFilter(cdf);
	d2tf.setInputVerifier(utfiv2);

	d3tf = new VTextField("", 10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			dist3 = 0.0;
		    } else {
			dist3 = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)d3tf.getDocument()).setDocumentFilter(cdf);
	d3tf.setInputVerifier(utfiv3);

	// initialize fields
	pcb.setSelectedIndex(baseInd);
	/*
	System.out.println("pcb.getSelectedItem() = "
			   + (String)(pcb.getSelectedItem()));
	*/
	if (baseMap.containsKey(baseName)) {
	    BME entry = baseMap.get(baseName);
	    mindex = entry.mindex;
	    uindex1 = entry.uindex1;
	    uindex2 = entry.uindex2;
	    uindex3 = entry.uindex3;
	    dist1 = entry.dist1;
	    dist2 = entry.dist2;
	    dist3 = entry.dist3;
	    if (mindex == 0) {
		mindex = 1;
	    }
	    // System.out.println("mindex = " + mindex);
	    modesCB.setSelectedIndex(mindex);
	    lunits1.setSelectedIndex(uindex1);
	    lunits2.setSelectedIndex(uindex2);
	    lunits3.setSelectedIndex(uindex3);
	    if (dist1 != 0.0) d1tf.setText("" + dist1);
	    if (dist2 != 0.0) d2tf.setText("" + dist2);
	    if (dist3 != 0.0 && entry.count > 0) {
		d3tf.setText("" + dist3);
	    }
	    // System.out.println("targetTF set for dist3");
	    targetTF = d3tf;
	} else {
	    newBasename = true;
	    modesCB.setSelectedIndex(0);
	    // System.out.println("targetTF set for dist1");
	    targetTF = d1tf;
	    dist1 = 0.0;
	    dist2 = 0.0;
	    dist3 = 0.0;
	    lunits1.setSelectedIndex(uindex1);
	    lunits2.setSelectedIndex(uindex2);
	    lunits3.setSelectedIndex(uindex3);
	}

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor = GridBagConstraints.LINE_START;

	/*
	c.gridwidth = 1;
	gridbag.setConstraints(nsl, c);
	add(nsl);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(nsegComboBox, c);
	add(nsegComboBox);
	*/

	c.gridwidth = 1;
	gridbag.setConstraints(pl, c);
	add(pl);
	gridbag.setConstraints(pcb, c);
	add(pcb);
	c.gridwidth = GridBagConstraints.REMAINDER;
	JLabel blank = new JLabel(" ");
	gridbag.setConstraints(blank, c);
	add(blank);

	c.gridwidth = 1;
	gridbag.setConstraints(d1l, c);
	add(d1l);
	gridbag.setConstraints(d1tf, c);
	add(d1tf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(lunits1, c);
	add(lunits1);

	c.gridwidth = 1;
	gridbag.setConstraints(d2l, c);
	add(d2l);
	gridbag.setConstraints(d2tf, c);
	add(d2tf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(lunits2, c);
	add(lunits2);

	c.gridwidth = 1;
	gridbag.setConstraints(ml, c);
	add(ml);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(modesCB, c);
	add(modesCB);

	c.gridwidth = 1;
	gridbag.setConstraints(d3l, c);
	add(d3l);
	gridbag.setConstraints(d3tf, c);
	add(d3tf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(lunits3, c);
	add(lunits3);


	if (dist1 != 0.0) d1tf.setText("" + dist1);
	if (dist2 != 0.0) d2tf.setText("" + dist2);
	if (dist3 != 0.0) d3tf.setText("" + dist3);

	// nsegComboBox.setSelectedIndex(nsegIndex);
	lunits1.setSelectedIndex(uindex1);
	lunits2.setSelectedIndex(uindex2);
	lunits3.setSelectedIndex(uindex3);

	/*
	nsegComboBox.addActionListener((le) -> {
		nsegIndex = nsegComboBox.getSelectedIndex();
	    });
	*/
	lunits1.addActionListener((le) -> {
		uindex1 = lunits1.getSelectedIndex();
	    });
	lunits2.addActionListener((le) -> {
		uindex2 = lunits2.getSelectedIndex();
	    });
	lunits3.addActionListener((le) -> {
		uindex3 = lunits3.getSelectedIndex();
	    });
	d3l.setEnabled(mindex != 0);
	d3tf.setEnabled(mindex != 0);
	lunits3.setEnabled(mindex != 0);
	modesCB.addActionListener((le) -> {
		mindex = modesCB.getSelectedIndex();
		if (mindex != 0) {
		    d3tf.setEnabled(true);
		    lunits3.setEnabled(true);
		    d3l.setEnabled(true);
		} else {
		    d3l.setEnabled(false);
		    d3tf.setEnabled(false);
		    lunits3.setEnabled(false);
		}
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
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
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
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
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

class ShiftPane extends JPanel {
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

    public ShiftPane() {
	super();
	boolean firstTime = true;
	cdf.setAllowedChars("09eeEE..,,++--");
	JLabel xl = new JLabel(localeString("DeltaX"));
	JLabel yl = new JLabel(localeString("DeltaY"));
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
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
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
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
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

class PipePane extends JPanel {
    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    private static int savedUnitsIndex = 0;
    private int unitsIndex = savedUnitsIndex;

    private static Vector<String> modes = new Vector<>(2);
    static {
	modes.add(localeString("pipeCmd"));
	modes.add(localeString("pipeFile"));
    }
    private static int savedModeIndex = 0;
    private int modeIndex = savedModeIndex;

    private JComboBox<String> modeComboBox = new JComboBox<>(modes);

    private int caretPosition = 0;
    private JTextField cmdTextField = new JTextField(50);
    private JButton fileButton = new JButton(localeString("pipeFileButton"));

    private JComboBox<String> unitsComboBox = new
	JComboBox<>(ConfigGCSPane.units);

    private boolean savedHashdr = false;
    boolean hashdr = savedHashdr;

    private JCheckBox hashdrCheckBox =
	new JCheckBox(localeString("pipeHasHdr"),  hashdr);

    private static Vector<String> delims = new Vector<>(3);
    static {
	delims.add(localeString("SystemEOL"));
	delims.add(localeString("CRLF"));
	delims.add(localeString("LF"));
	delims.add(localeString("CR"));
    }

    LineReader.Delimiter getDelimiter(int ind) {
	switch (ind) {
	case 0: return null;
	case 1: return LineReader.Delimiter.CRLF;
	case 2: return LineReader.Delimiter.LF;
	case 3: return LineReader.Delimiter.CR;
	}
	return null;
    }

    private static int savedDelimIndex = 0;
    private int delimIndex = savedDelimIndex;
    private JComboBox<String> delimComboBox = new JComboBox<>(delims);

    public int getUnitsIndex() {
	return unitsIndex;
    }

    private static File savedDir = new File(System.getProperty("user.dir"));
    private File dir = savedDir;

    void saveState() {
	savedModeIndex = modeIndex;
	savedHashdr = hashdr;
	savedDelimIndex = delimIndex;
	savedDir = dir;
    }

    Process p = null;
    Thread monitor = null;
    public boolean isAlive() {
	if (p == null) return false;
	try {
	    p.waitFor(5, TimeUnit.SECONDS);
	} catch (Exception e){}
	return (p != null) && p.isAlive();
    }

    public void kill() {
	if (p != null) {
	    try {
		p.destroyForcibly().waitFor();
	    } catch (Exception e){}
	}
    }

    StringBuilder errmsg = new StringBuilder();
    boolean msgDone = false;
    boolean  msgReady() {
	try {
	    monitor.join();
	} catch (Exception e) {}
	return msgDone;
    }
    public String errmsg() {
	return  errmsg.toString();
    }

    public int exitValue() {
	return (p == null)? 0: p.exitValue();
    }

    public CSVReader getReader() throws Exception {
	InputStream is;
	if (modeIndex == 0) {
	    p = Runtime.getRuntime().exec(cmdTextField.getText());
	    is = p.getInputStream();
	    monitor = new Thread(() -> {
		    try {
			InputStream eis = p.getErrorStream();
			Reader er = new
			    InputStreamReader(eis, Charset.forName("UTF-8"));
			int ch;
			while ((ch = er.read()) != -1) {
			    errmsg.append((char)ch);
			}
		    } catch (Exception e) {
			errmsg.append("\n" + e.getMessage());
		    } finally {
			msgDone = true;
		    }
	    });
	    monitor.start();
	} else if (modeIndex == 1) {
	    is = new FileInputStream(cmdTextField.getText());
	} else {
	    return null;
	}
	Reader r = new InputStreamReader(is, Charset.forName("UTF-8"));
	return new CSVReader(r, hashdr, getDelimiter(delimIndex));
    }

    boolean status = false;
    public boolean getStatus() {return status;}

    public PipePane(JFrame frame) {
	modeComboBox.setSelectedIndex(modeIndex);
	fileButton.setEnabled(true);
	unitsComboBox.setSelectedIndex(unitsIndex);
	hashdrCheckBox.setSelected(hashdr);
	delimComboBox.setSelectedIndex(delimIndex);

	modeComboBox.addActionListener((e) -> {
		modeIndex = modeComboBox.getSelectedIndex();
	    });
	unitsComboBox.addActionListener((e) -> {
		unitsIndex = unitsComboBox.getSelectedIndex();
	    });
	hashdrCheckBox.addActionListener((e) -> {
		hashdr = hashdrCheckBox.isSelected();
	    });
	delimComboBox.addActionListener((e) -> {
		delimIndex = delimComboBox.getSelectedIndex();
	    });

	cmdTextField.addCaretListener((ce) -> {
		caretPosition = ce.getDot();
	    });

	fileButton.addActionListener((e) -> {
		JFileChooser fc = new JFileChooser(dir);
		int status = fc.showDialog(frame,
					   localeString("pipeFileAction"));
		if (status == JFileChooser.APPROVE_OPTION) {
		    File f = fc.getSelectedFile();
		    dir = f.getParentFile();
		    String path = f.getPath();
		    try {
			cmdTextField.getDocument()
			    .insertString(caretPosition, path, null);
		    } catch (Exception ee) {
			System.err.println("insertion failed");
		    }
		}
	    });


	JButton okButton = new JButton(localeString("okButton"));
	JButton cancelButton = new JButton(localeString("cancelButton"));

	cancelButton.addActionListener((e) -> {
		status = false;
		Window w = SwingUtilities.getWindowAncestor(PipePane.this);
		w.setVisible(false);
	    });
	okButton.addActionListener((e) -> {
		status = true;
		saveState();
		Window w = SwingUtilities.getWindowAncestor(PipePane.this);
		w.setVisible(false);
	    });


	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(modeComboBox, c);
	add(modeComboBox);
	c.gridwidth = 1;
	gridbag.setConstraints(cmdTextField, c);
	add(cmdTextField);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(fileButton, c);
	add(fileButton);
	gridbag.setConstraints(hashdrCheckBox, c);
	add(hashdrCheckBox);
	c.gridwidth = 1;
	JPanel leftPane = new JPanel();
	leftPane.setLayout(new FlowLayout(FlowLayout.LEADING));
	JLabel label = new JLabel(localeString("inputUnits"));
	leftPane.add(label);
	leftPane.add(unitsComboBox);
	gridbag.setConstraints(label, c);
	c.gridwidth = 1;
	gridbag.setConstraints(leftPane, c);
	add(leftPane);
	c.gridwidth = GridBagConstraints.REMAINDER;
	label = new JLabel("");
	gridbag.setConstraints(label, c);
	add(label);
	c.gridwidth = 1;
	leftPane = new JPanel();
	leftPane.setLayout(new FlowLayout(FlowLayout.LEADING));
	label = new JLabel(localeString("pipeCSVDelimiter"));
	leftPane.add(label);
	leftPane.add(delimComboBox);
	c.gridwidth = 1;
	gridbag.setConstraints(leftPane, c);
	add(leftPane);
	c.gridwidth = GridBagConstraints.REMAINDER;
	label = new JLabel("");
	gridbag.setConstraints(label, c);
	add(label);

	JPanel buttonPane = new JPanel();
	GridBagLayout gridbag2 = new GridBagLayout();
	buttonPane.setLayout(gridbag2);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.CENTER;
	gridbag2.setConstraints(okButton, c);
	buttonPane.add(okButton);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag2.setConstraints(cancelButton, c);
	buttonPane.add(cancelButton);
	c.anchor = GridBagConstraints.CENTER;
	gridbag.setConstraints(buttonPane, c);
	add(buttonPane);
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
    // static int nsegIndexSaved = 1;
    int lindex = lindexSaved;
    int aindex = aindexSaved;
    // int nsegIndex = nsegIndexSaved;

    void saveIndices() {
	lindexSaved = lindex;
	aindexSaved = aindex;
	// nsegIndexSaved = nsegIndex;
    }


    static Vector<String> nv = new Vector<>(10);
    static {
	nv.add("1");
	nv.add("2");
	nv.add("3");
	nv.add("4");
    }

    JCheckBox ccwCheckBox = new JCheckBox(localeString("Counterclockwise"));
    // JComboBox<String> nsegComboBox = new JComboBox<>(nv);

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

    /*
    public double getMaxDelta() {
	int divisor = 1 << nsegComboBox.getSelectedIndex();
	return (Math.PI/2.0)/(divisor);
    }
    */

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
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
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
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
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
	/*
	c.gridwidth = 1;
	gridbag.setConstraints(nsl, c);
	add(nsl);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(nsegComboBox, c);
	add(nsegComboBox);

	nsegComboBox.setSelectedIndex(nsegIndex);
	nsegComboBox.addActionListener((le) -> {
		nsegIndex = nsegComboBox.getSelectedIndex();
	    });
	*/
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


abstract class InsertArcPane extends JPanel {
    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }
    static String localeString(String key) {
	return EPTS.localeString(key);
    }
    PointTMR prev;
    PointTMR current;
    PointTMR next;
    static boolean savedCCW = false;
    boolean counterclockwise = savedCCW;
    static double savedRadius = 0.0;
    double radius = savedRadius;

    public boolean isCounterClockwise() {
	return counterclockwise;
    }

    public static boolean savedKeepCenter = false;
    public boolean  keepCenter = savedKeepCenter;
    private JCheckBox keepCenterCB = new
	JCheckBox(localeString("keepCenter"), savedKeepCenter);

    public boolean keepCenter() {
	return keepCenter;
    }

    private JCheckBox ccwCB = new JCheckBox(localeString("arcCCW"),
					    savedCCW);
    private CharDocFilter cdf;
    private VTextField radiusTF;

    static int rindexSaved = 0;
    int rindex = rindexSaved;
    JLabel runitsLabel = new JLabel(localeString("radiusUnits"));
    JComboBox<String> runits = new JComboBox<>(ConfigGCSPane.units);

    boolean status =  false;
    public boolean getStatus() {return status;}

    Path2D arc = null;
    public Path2D getArc(AffineTransform af) {
	if (arc == null) computeArc();
	if (af == null) {
	    return arc;
	} else {
	    return new Path2D.Double(arc, af);
	}
    }

    double maxRadius;
    double dist1;
    double dist2;
    private void computeArc() {
	double r = ConfigGCSPane.convert[rindex].valueAt(radius);
	if (r >= maxRadius) {
	    arc = null;
	    JOptionPane
		.showMessageDialog(this,
				   errorMsg("maxRadiusErr", r, maxRadius),
				   localeString("errorTitle"),
				   JOptionPane.ERROR_MESSAGE);
	    return;
	}
	double t = r/dist1;
	double x1 = prev.getX()*t + current.getX()*(1-t);
	double y1 = prev.getY()*t + current.getY()*(1-t);
	t = r/dist2;
	double uv1x = (current.getX() - prev.getX())/dist1;
	double uv1y = (current.getY() - prev.getY())/dist1;
	double un1y = uv1x;
	double un1x = -uv1y;
	double uv2x = (next.getX() - current.getX())/dist2;
	double uv2y = (next.getY() - current.getY())/dist2;

	double dx = uv1x*uv2x + uv1y*uv2y;
	double dy = un1x*uv2x + un1y*uv2y;
	double theta = Math.atan2(dy, dx);
	if (counterclockwise) {
	    if (theta == -Math.PI) theta = Math.PI;
	} else {
	    if (theta == Math.PI) theta = -Math.PI;
	}
	if (Math.abs(theta) != Math.PI) {
	    if (counterclockwise) {
		theta = Math.PI + theta;
	    } else {
		theta -= Math.PI;
	    }
	}
	arc = Paths2D.createArc(current.getX(), current.getY(),
				x1, y1, theta);
    }

    void saveState() {
	rindexSaved = rindex;
	savedRadius = radius;
	savedCCW = counterclockwise;
	savedKeepCenter = keepCenter;
    }


    public InsertArcPane(PointTMR prev, PointTMR current, PointTMR next) {
	super();
	this.prev = prev;
	this.current = current;
	this.next = next;

	double prevX = prev.getX();
	double currentX = current.getX();
	double nextX = next.getX();
	double prevY = prev.getY();
	double currentY = current.getY();
	double nextY = next.getY();

	dist1 = Point2D.distance(prevX, prevY, currentX, currentY);
	dist2 = Point2D.distance(nextX, nextY, currentX, currentY);
	maxRadius = Math.min(dist1, dist2);

	cdf  = new CharDocFilter();
	cdf.setAllowedChars("09..,,++--eE");
	InputVerifier iv = new InputVerifier() {
		public boolean verify(JComponent input) {
		    VTextField tf = (VTextField)input;
		    String string = tf.getText();
		    try {
			if (string.length() == 0) string = "0.0";
			double value = Double.valueOf(string);
			value = ConfigGCSPane.convert[rindex].valueAt(value);
			if (value >= 0.0 && value < maxRadius) {
			    return true;
			} else {
			    return false;
			}
		    } catch (Exception e) {
			return false;
		    }
		}
	    };

	radiusTF = new VTextField(""+radius, 20) {
		@Override
		protected void onAccepted() {
		    try {
			String txt = getText();
			if (txt.length()  == 0.0) txt = "0.0";
			radius = Double.valueOf(txt);
		    } catch (Exception e) {
			return;
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("notNonNegativeLimit"),
			 localeString("Error"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)radiusTF.getDocument()).setDocumentFilter(cdf);
	radiusTF.setInputVerifier(iv);
	radiusTF.setAllowEmptyTextField(false);
	runits.setSelectedIndex(rindex);
	runits.addActionListener((e) -> {
		rindex = runits.getSelectedIndex();
	    });
	ccwCB.addActionListener((e) -> {
		counterclockwise = ccwCB.isSelected();
	    });

	keepCenterCB.addActionListener((e) -> {
		keepCenter = keepCenterCB.isSelected();
	    });

	JButton okButton = new JButton(localeString("okButton"));
	JButton acceptButton = new JButton(localeString("acceptButton"));
	JButton cancelButton = new JButton(localeString("cancelButton"));

	acceptButton.addActionListener((e) -> {
		computeArc();
		if (arc == null) {
		    return;
		}
		status = true;
		accept();
	    });
	cancelButton.addActionListener((e) -> {
		status = false;
		Window w = SwingUtilities.getWindowAncestor(InsertArcPane.this);
		w.setVisible(false);
		cancel();
	    });
	okButton.addActionListener((e) -> {
		computeArc();
		if (arc == null) {
		    return;
		}
		status = true;
		saveState();
		Window w = SwingUtilities.getWindowAncestor(InsertArcPane.this);
		w.setVisible(false);
		ok();
	    });
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor  = GridBagConstraints.LINE_START;
	c.gridwidth = 1;

	JLabel label = new JLabel(localeString("radiusUnits"));
	gridbag.setConstraints(label, c);
	add(label);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(runits, c);
	add(runits);
	c.gridwidth = 1;
	label = new JLabel(localeString("Radius"));
	gridbag.setConstraints(label, c);
	add(label);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(radiusTF, c);
	add(radiusTF);
	gridbag.setConstraints(ccwCB, c);
	add(ccwCB);
	gridbag.setConstraints(keepCenterCB, c);
	add(keepCenterCB);
	JPanel buttonPane = new JPanel();
	GridBagLayout gridbag2 = new GridBagLayout();
	buttonPane.setLayout(gridbag2);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.CENTER;
	gridbag2.setConstraints(okButton, c);
	buttonPane.add(okButton);
	// c.anchor = GridBagConstraints.CENTER;
	gridbag2.setConstraints(acceptButton, c);
	buttonPane.add(acceptButton);
	// c.anchor  = GridBagConstraints.LINE_END;
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag2.setConstraints(cancelButton, c);
	buttonPane.add(cancelButton);
	c.anchor = GridBagConstraints.CENTER;
	gridbag.setConstraints(buttonPane, c);
	add(buttonPane);
    }

    public abstract void accept();
    public abstract void cancel();
    public abstract void ok();

}


abstract class TransformPane extends JPanel {
    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    AffineTransform af = null;
    Path2D oldpath = null;
    // Path2D newpath = null;

    Point2D ref = null;
    Point2D getRef() {return ref;}

    double scale1 = 1.0;
    double scale2 = 1.0;

    // double shear12 = 0.0;
    // double shear21 = 0.0;

    static int lindexSaved = 0;
    static int aindexSaved = 0;

    double rotAngle = 0.0;
    static Vector<String> av = new Vector<>(2);
    static {
	av.add("Degrees");
	av.add("Radians");
    }
    JComboBox<String> aunits = new JComboBox<>(av);
    int aindex = aindexSaved;

    JComboBox<String> lunits = new JComboBox<>(ConfigGCSPane.units);
    int lindex = lindexSaved;

    double principalAngle = 0.0;
    double tx = 0.0;
    double ty = 0.0;

    void saveIndices() {
	lindexSaved = lindex;
	aindexSaved = aindex;
    }

    public AffineTransform getTransform() {
	double x = ConfigGCSPane.convert[lindex].valueAt(tx);
	double y = ConfigGCSPane.convert[lindex].valueAt(ty);
	AffineTransform af =
	    AffineTransform.getTranslateInstance(x+ref.getX(), y+ref.getY());
	int quadrants = 0;
	double rangle;
	if (aindex == 0) {
	    if (rotAngle == 0.0) {
		rangle = 0.0;
	    } else if (rotAngle == 90.0) {
		rangle = 0.0;
		quadrants = 1;
	    } else if (rotAngle == 180.0) {
		rangle = 0.0;
		quadrants = 2;
	    } else if (rotAngle == 270.0) {
		rangle = 0.0;
		quadrants = 3;
	    } else if (rotAngle == -90.0) {
		rangle = 0.0;
		quadrants = 3;
	    } else if (rotAngle == -180.0) {
		rangle = 0.0;
		quadrants = -2;
	    } else if (rotAngle == -270.0) {
		rangle = 0.0;
		quadrants = 1;
	    } else {
		quadrants = 0;
		rangle = Math.toRadians(rotAngle);
	    }
	} else {
	    rangle = rotAngle;
	}
	if (quadrants > 0) {
	    af.quadrantRotate(quadrants);
	}
	af.rotate(rangle+principalAngle);
	// af.shear(shear12, shear21);
	af.scale(scale1, scale2);
	af.rotate(-principalAngle);
	af.translate(-ref.getX(), -ref.getY());
	return af;
    }

    String oldName;
    PointTableModel ptmodel;

    public Path2D getOldPath() {
	return oldpath;
    }

    public Path2D getNewPath() {
	Path2D newpath = ptmodel.getPath(oldName, getTransform());
	return newpath;
    }

    private JCheckBox reversePathCB;

    public boolean requestsReversed() {
	return reversePathCB.isSelected();
    }

    CharDocFilter cdf = new CharDocFilter();

    boolean status =  false;
    public boolean getStatus() {return status;}


    Line2D paline1;
    Line2D paline2;

    public Line2D principalAxis1() {
	return paline1;
    }
    public Line2D principalAxis2() {
	return paline2;
    }

    JButton zoomIn = new JButton(localeString("ZoomIn"));
    JButton zoomOut = new JButton(localeString("ZoomOut"));

    protected void enableIn(boolean enabled) {
	zoomIn.setEnabled(enabled);
    }
    protected void enableOut(boolean enabled) {
	zoomOut.setEnabled(enabled);
    }

    public TransformPane(PointTableModel ptmodel, String name,
			 double scaleFactor)
    {
	super();
	oldName = name;
	this.ptmodel = ptmodel;
	oldpath = ptmodel.getPath(name);
	ref = Path2DInfo.centerOfMassOf(oldpath);
	double[][] moments = (ref == null)? null:
	    Path2DInfo.momentsOf(ref, oldpath);
	double[] principalMoments = (moments == null)? null:
	    Moments.principalMoments(moments);
	double mean = (moments == null)? 0.0:
	    (principalMoments[0] + principalMoments[1])/2;
	if (mean == 0.0) {
	    // punt - the area must be zero.
	    if (principalMoments == null) {
		if (ref == null) {
		    double[] ar = EPTSWindow
			.getDefaultPrincipalAngleAndRef(oldpath);
		    principalAngle = ar[0];
		    ref = new Point2D.Double(ar[1], ar[2]);
		} else {
		    principalAngle = 0.0;
		}
	    } else {
		principalAngle = 0.0;
	    }
	    double dx =  20*scaleFactor*Math.cos(principalAngle);
	    double dy =  20*scaleFactor*Math.sin(principalAngle);
	    paline1 = new Line2D.Double(ref.getX(), ref.getY(),
					ref.getX() + dx, ref.getY() + dy);
	    paline2 = new Line2D.Double(ref.getX(), ref.getY(),
					ref.getX() - .3*dy, ref.getY() + .3*dx);
	} else if (Math.abs((principalMoments[0]
			     - principalMoments[1])/mean) < 0.01) {
	    // we can't see the difference so use X and Y axes
	    principalAngle = 0.0;
	    paline1 = new Line2D.Double(ref.getX(),  ref.getY(),
					ref.getX() + Math.sqrt(mean),
					ref.getY());
	    paline2 = new Line2D.Double(ref.getX(),  ref.getY(),
					ref.getX(),
					ref.getY() + Math.sqrt(mean));
	} else {
	    double[][]principalAxes =
		Moments.principalAxes(moments, principalMoments);
	    principalAngle = Math.atan2(principalAxes[0][1],
					principalAxes[0][0]);
	    double len = Math.sqrt(principalMoments[0]);
	    paline1 = new Line2D.Double(ref.getX(), ref.getY(),
					ref.getX() + len*principalAxes[0][0],
					ref.getY() + len*principalAxes[0][1]);
	    len = Math.sqrt(principalMoments[1]);
	    paline2 = new Line2D.Double(ref.getX(), ref.getY(),
					ref.getX() + len*principalAxes[1][0],
					ref.getY() + len*principalAxes[1][1]);
	}

	reversePathCB = new JCheckBox(localeString("reversePath"));
	reversePathCB.setEnabled(true);
	reversePathCB.setSelected(false);

	/*
	JRadioButton cmButton = new JRadioButton(localeString("cm"), true);
	JRadioButton bbButton = new JRadioButton(localeString("bbCenter"),
						 false);
	ButtonGroup group = new ButtonGroup();
	group.add(cmButton);
	group.add(bbButton);
	cmButton.addActionListener((e) -> {
		ref = Path2DInfo.centerOfMassOf(oldpath);
	    });
	bbButton.addActionListener((e) -> {
		Rectangle2D rect = oldpath.getBounds2D();
		ref = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
	    });
	*/
	cdf.setAllowedChars("09eeEE..,,++--");
	InputVerifier tfiv = new InputVerifier() {
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
	JLabel scale1Label = new JLabel(localeString("scale1Label"));
	VTextField scale1TF = new VTextField(10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			scale1 = 1.0;
		    } else {
			scale1 = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	JLabel scale2Label = new JLabel(localeString("scale2Label"));
	VTextField scale2TF = new VTextField(10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			scale2 = 1.0;
		    } else {
			scale2 = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	/*
	JLabel shear12Label = new JLabel(localeString("shear12Label"));
	VTextField shear12TF = new VTextField(10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			shear12 = 0.0;
		    } else {
			shear12 = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	JLabel shear21Label = new JLabel(localeString("shear21Label"));
	VTextField shear21TF = new VTextField(10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			shear21 = 0.0;
		    } else {
			shear21 = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	*/
	JLabel rotLabel = new JLabel(localeString("rotation"));
	JLabel rotUnitsLabel = new JLabel(localeString("rotUnits"));
	VTextField rotTF = new VTextField(10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			rotAngle = 0.0;
		    } else {
			rotAngle = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	aunits.setSelectedIndex(aindex);
	aunits.addActionListener((e) -> {
		aindex = aunits.getSelectedIndex();
	    });
	lunits.setSelectedIndex(lindex);
	lunits.addActionListener((e) -> {
		lindex = lunits.getSelectedIndex();
	    });

	JLabel transXLabel = new JLabel(localeString("translateX"));
	VTextField transXTF = new VTextField(10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			tx = 0.0;
		    } else {
			tx = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	JLabel transYLabel = new JLabel(localeString("translateY"));
	VTextField transYTF = new VTextField(10) {
		@Override
		protected void onAccepted() {
		    String text = getText();
		    if (text == null || text.length() == 0) {
			ty = 0.0;
		    } else {
			ty = Double.valueOf(text);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("needRealNumber"),
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	JLabel translateUnitsLabel = new JLabel(localeString("translateUnits"));

	JLabel zoomLabel = new JLabel(localeString("ZoomWindow"));
	zoomIn.addActionListener((e) -> {
		doZoomIn();
		setZoomEnabled();
	    });
	zoomOut.addActionListener((e) -> {
		doZoomOut();
		setZoomEnabled();
	    });

	JButton okButton = new JButton(localeString("okButton"));
	JButton acceptButton = new JButton(localeString("acceptButton"));
	JButton cancelButton = new JButton(localeString("cancelButton"));

	((AbstractDocument)scale1TF.getDocument()).setDocumentFilter(cdf);
	scale1TF.setInputVerifier(tfiv);
	((AbstractDocument)scale2TF.getDocument()).setDocumentFilter(cdf);
	scale2TF.setInputVerifier(tfiv);
	/*
	((AbstractDocument)shear12TF.getDocument()).setDocumentFilter(cdf);
	shear12TF.setInputVerifier(tfiv);
	((AbstractDocument)shear21TF.getDocument()).setDocumentFilter(cdf);
	shear21TF.setInputVerifier(tfiv);
	*/
	((AbstractDocument)rotTF.getDocument()).setDocumentFilter(cdf);
	rotTF.setInputVerifier(tfiv);
	((AbstractDocument)transXTF.getDocument()).setDocumentFilter(cdf);
	transXTF.setInputVerifier(tfiv);
	((AbstractDocument)transYTF.getDocument()).setDocumentFilter(cdf);
	transYTF.setInputVerifier(tfiv);


	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor  = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	/*
	JPanel rbPanel = new JPanel();
	GridBagLayout gridbag1 = new GridBagLayout();
	rbPanel.setLayout(gridbag1);
	gridbag1.setConstraints(cmButton, c);
	rbPanel.add(cmButton);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag1.setConstraints(bbButton, c);
	rbPanel.add(bbButton);

	c.gridwidth = GridBagConstraints.CENTER;
	gridbag.setConstraints(rbPanel, c);
	add(rbPanel);

	c.gridwidth = GridBagConstraints.REMAINDER;
	JLabel spacer = new JLabel(" ");
	gridbag.setConstraints(spacer, c);
	add(spacer);
	*/
	gridbag.setConstraints(scale1Label, c);
	add(scale1Label);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(scale1TF, c);
	add(scale1TF);
	c.gridwidth = 1;
	gridbag.setConstraints(scale2Label, c);
	add(scale2Label);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(scale2TF, c);
	add(scale2TF);
	c.gridwidth = 1;
	gridbag.setConstraints(reversePathCB, c);
	add(reversePathCB);
	JLabel spacer = new JLabel(" ");
	gridbag.setConstraints(spacer, c);
	add(spacer);
	spacer = new JLabel(" ");
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(spacer, c);
	add(spacer);

	/*
	c.gridwidth = 1;
	gridbag.setConstraints(shear12Label, c);
	add(shear12Label);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(shear12TF, c);
	add(shear12TF);
	c.gridwidth = 1;
	gridbag.setConstraints(shear21Label, c);
	add(shear21Label);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(shear21TF, c);
	add(shear21TF);

	spacer = new JLabel(" ");
	gridbag.setConstraints(spacer, c);
	add(spacer);
	*/
	c.gridwidth = 1;
	gridbag.setConstraints(rotLabel, c);
	add(rotLabel);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(rotTF, c);
	add(rotTF);
	c.gridwidth = 1;
	gridbag.setConstraints(rotUnitsLabel, c);
	add(rotUnitsLabel);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(aunits, c);
	add(aunits);

	spacer = new JLabel(" ");
	gridbag.setConstraints(spacer, c);
	add(spacer);

	c.gridwidth = 1;
	gridbag.setConstraints(transXLabel, c);
	add(transXLabel);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(transXTF, c);
	add(transXTF);
	c.gridwidth = 1;
	gridbag.setConstraints(transYLabel, c);
	add(transYLabel);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(transYTF, c);
	add(transYTF);
	c.gridwidth = 1;
	gridbag.setConstraints(translateUnitsLabel, c);
	add(translateUnitsLabel);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(lunits, c);
	add(lunits);

	acceptButton.addActionListener((e) -> {
		status = true;
		accept();
	    });
	cancelButton.addActionListener((e) -> {
		status = false;
		Window w = SwingUtilities.getWindowAncestor(TransformPane.this);
		w.setVisible(false);
		cancel();
	    });
	okButton.addActionListener((e) -> {
		status = true;
		Window w = SwingUtilities.getWindowAncestor(TransformPane.this);
		w.setVisible(false);
		saveIndices();
		ok();
	    });

	spacer = new JLabel(" ");
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(spacer, c);
	add(spacer);

	JPanel zoomPane = new JPanel();
	GridBagLayout gridbagZ = new GridBagLayout();
	zoomPane.setLayout(gridbagZ);
	c.gridwidth = 1;
	gridbagZ.setConstraints(zoomLabel, c);
	zoomPane.add(zoomLabel);
	gridbagZ.setConstraints(zoomIn, c);
	zoomPane.add(zoomIn);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbagZ.setConstraints(zoomOut, c);
	zoomPane.add(zoomOut);
	c.anchor = GridBagConstraints.CENTER;
	gridbag.setConstraints(zoomPane, c);
	add(zoomPane);
	JPanel buttonPane = new JPanel();
	GridBagLayout gridbag2 = new GridBagLayout();
	buttonPane.setLayout(gridbag2);
	c.gridwidth = 1;
	c.anchor = GridBagConstraints.CENTER;
	gridbag2.setConstraints(okButton, c);
	buttonPane.add(okButton);
	// c.anchor = GridBagConstraints.CENTER;
	gridbag2.setConstraints(acceptButton, c);
	buttonPane.add(acceptButton);
	// c.anchor  = GridBagConstraints.LINE_END;
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag2.setConstraints(cancelButton, c);
	buttonPane.add(cancelButton);

	c.anchor = GridBagConstraints.CENTER;
	gridbag.setConstraints(buttonPane, c);
	add(buttonPane);
	setZoomEnabled();
    }

    public abstract void accept();
    public abstract void cancel();
    public abstract void ok();

    public abstract void doZoomIn();
    public abstract void doZoomOut();
    protected abstract void setZoomEnabled();
}

public class EPTSWindow {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    static int vk(String key) {
	return org.bzdev.swing.keys.VirtualKeys.lookup(localeString(key));
    }

    // called when Path2DInfo.centerOfMassOf returned null,
    // which probably means all the points are colinear.
    static double[] getDefaultPrincipalAngleAndRef(Path2D path) {
	double xmean = 0.0;
	double ymean = 0.0;

	double[] coords = new double[6];
	double x, y;
	double n = 0;
	PathIterator pi = path.getPathIterator(null);
	while (!pi.isDone()) {
	    switch(pi.currentSegment(coords)) {
	    case PathIterator.SEG_MOVETO:
	    case PathIterator.SEG_LINETO:
		x = coords[0];
		y = coords[1];
		break;
	    case PathIterator.SEG_QUADTO:
		x = coords[2];
		y = coords[3];
		break;
	    case PathIterator.SEG_CUBICTO:
		x = coords[4];
		y = coords[5];
		break;
	    case PathIterator.SEG_CLOSE:
		pi.next();
		continue;
	    default:
		throw new UnexpectedExceptionError();
	    }
	    xmean += x;
	    ymean += y;
	    n++;
	    pi.next();
	}
	xmean /= n;
	ymean /= n;
	pi = path.getPathIterator(null);
	double distsq = 0.0;
	double vx = 0.0; double vy = 0.0;
	while (!pi.isDone()) {
	    switch(pi.currentSegment(coords)) {
	    case PathIterator.SEG_MOVETO:
	    case PathIterator.SEG_LINETO:
		x = coords[0] - xmean;
		y = coords[1] - ymean;
		break;
	    case PathIterator.SEG_QUADTO:
		x = coords[2] - xmean;
		y = coords[3] - ymean;
		break;
	    case PathIterator.SEG_CUBICTO:
		x = coords[4] - xmean;
		y = coords[5] - ymean;
		break;
	    case PathIterator.SEG_CLOSE:
		pi.next();
		continue;
	    default:
		throw new UnexpectedExceptionError();
	    }
	    double dsq = x*x + y*y;
	    if (dsq > distsq) {
		distsq = dsq;
		vx = x;
		vy = y;
	    } else if (dsq > 0.999 *dsq) {
		// If close to a tie, prefer the one closest to the
		// end of the path.  This happens regularly with
		// a single straight-line segment.
		vx = x;
		vy = y;
	    }
	    pi.next();
	}
	double angle =  (vx == 0.0 && vy == 0.0)? 0.0: Math.atan2(vy,vx);
	double[] results = {
	    angle, xmean, ymean
	};
	return results;
    }


    static Map<String,String> keys = new HashMap<String,String>();
    static Map<String,String> links = new HashMap<String,String>();
    static Map<String,String> descriptions = new HashMap<String,String>();

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
	if (EPTS.sbrp.length() > 0) {
	    keymap.put("resourcePath", EPTS.sbrp.toString());
	}
	if (shouldSaveScripts) {
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
	keymap.put("unitIndexRP",
		   String.format("%d", configGCSPane.savedUnitIndexRP));
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

	boolean hasBaseMap = OffsetPane.baseMap.size() != 0;
	boolean hasPathMap = OffsetPane.ourpaths.size() != 0;

	if (hasBaseMap || hasPathMap) {
	    keymap.put("hasOffsets", emptyMap);
	    if (hasBaseMap) {
		keymap.put("hasBasemap", emptyMap);
		TemplateProcessor.KeyMapList basemapList =
		    new TemplateProcessor.KeyMapList();
		for(Map.Entry<String,OffsetPane.BME> entry:
			OffsetPane.baseMap.entrySet()) {
		    String base = entry.getKey();
		    OffsetPane.BME bme = entry.getValue();
		    TemplateProcessor.KeyMap kmp =
			new TemplateProcessor.KeyMap();
		    kmp.put("base", WebEncoder.htmlEncode(base));
		    kmp.put("mindex", "" + bme.mindex);
		    kmp.put("dist1", "" + bme.dist1);
		    kmp.put("dist2", "" + bme.dist2);
		    kmp.put("dist3", "" + bme.dist3);
		    kmp.put("uindex1", "" + bme.uindex1);
		    kmp.put("uindex2", "" + bme.uindex2);
		    kmp.put("uindex3", "" + bme.uindex3);
		    basemapList.add(kmp);
		}
		keymap.put("basemapEntries", basemapList);
	    }
	    if (hasPathMap) {
		keymap.put("hasPathmap", emptyMap);
		TemplateProcessor.KeyMapList pathmapList =
		    new TemplateProcessor.KeyMapList();
		for (Map.Entry<String,String> entry:
			 OffsetPane.ourpaths.entrySet()) {
		    TemplateProcessor.KeyMap kmp =
			new TemplateProcessor.KeyMap();
		    kmp.put("pathForEntry",
			    WebEncoder.htmlEncode(entry.getKey()));
		    kmp.put("base", WebEncoder.htmlEncode(entry.getValue()));
		    pathmapList.add(kmp);
		}
		keymap.put("pathmapEntries", pathmapList);
	    }
	}

	// printKeymap(keymap);

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
		toCircleMenuItem.setEnabled
		    (!ptmodel.getToCircleVariableNames().isEmpty());
	    }
	};

    void setupTableAux(JTable tbl, boolean mode) {
	tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	tbl.setRowSelectionAllowed(true);
	tbl.setCellSelectionEnabled(false);
	tbl.setColumnSelectionAllowed(false);
	TableCellRenderer tcr = tbl.getDefaultRenderer(String.class);
	int w0 = 200;
	int w1 = 100;
	int w2 = 175;
	int w3 = 175;
	if (tcr instanceof DefaultTableCellRenderer) {
	    DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer) tcr;
	    FontMetrics fm = dtcr.getFontMetrics(dtcr.getFont());
	    if (mode) {
		w0 = 10 + fm.stringWidth("mmmmmmmmmmmmmmmmmmmm");
		w1 = 40 + fm.stringWidth("mmmmmmmmmmmm");
		w2 = 40 + fm.stringWidth("mmmmmmmmmmmmmmmmmmmmm");
	    } else {
		w0 = 10 + fm.stringWidth("MMMMMMMMMMMM");
		w1 = 10 + fm.stringWidth("MMMMMMMMMMMM");
		w2 = 10 + fm.stringWidth("777777777777777777777");
	    }
	    w3 = w2;
	}
	TableColumnModel cmodel = tbl.getColumnModel();
	TableColumn column = cmodel.getColumn(0);
	column.setPreferredWidth(w0);
	column = cmodel.getColumn(1);
	column.setPreferredWidth(w1);
	column = cmodel.getColumn(2);
	column.setPreferredWidth(w2);
	column = cmodel.getColumn(3);
	column.setPreferredWidth(w3);
    }

    void setupTable(JComponent pane) {
	ptmodel = new PointTableModel(pane);
	ptmodel.addTableModelListener(tmlistener);
	ptable = new JTable(ptmodel);
	setupTableAux(ptable, true);
	tableFrame = new JFrame();
	tableFrame.setIconImages(EPTS.getIconList());
	tableFrame.setPreferredSize(new Dimension(800,600));
	JScrollPane tableScrollPane = new JScrollPane(ptable);
	tableScrollPane.setOpaque(true);
	ptable.setFillsViewportHeight(true);
	tableFrame.setContentPane(tableScrollPane);
  	tableFrame.pack();
    }

    static JFrame printTableFrame = null;
    JTable createPrintTable() {
	JPanel ourPane = new JPanel();
	PointTableModel ourModel = new PointTableModel(ourPane);
	for (PointTMR row: ptmodel.getRows()) {
	    ourModel.addRow(row);
	}
	JFrame ourFrame = new JFrame();
	JTable table = new JTable(ourModel);
	setupTableAux(table, false);
	table.setGridColor(Color.BLUE);
	table.setBackground(Color.WHITE);
	table.setForeground(Color.BLACK);
	JScrollPane tableScrollPane = new JScrollPane(table);
	ourFrame.setPreferredSize(new Dimension(800,600));
	tableScrollPane.setOpaque(true);
	table.setFillsViewportHeight(true);
	ourFrame.setContentPane(tableScrollPane);
	ourFrame.pack();
	if (printTableFrame != null) {
	    printTableFrame.dispose();
	    printTableFrame = null;
	}
	
	printTableFrame = ourFrame;
	return table;
    }

    JFrame manualFrame = null;
    HtmlWithTocPane manualPane = null;

    // Color tocBackgroundColor = new Color(20,20,50);
    // Color tocForegroundColor = Color.WHITE;
    // Color tocFrameBackgroundColor = new Color(196,196,196);

    // for manual
    /*
    private boolean darkmode = false;

    boolean darkmodeChanged() {
	Color c = manualFrame.getContentPane().getBackground();
	boolean dm =
	    ((c.getRed() < 128 && c.getGreen() < 128 && c.getBlue() < 128));
	try {
	    return (dm != darkmode);
	} finally {
	    darkmode = dm;
	}
    }
    */

    static private Color contentBackground  = Color.WHITE;
    static private Color contentBackgroundDM  = new Color(10, 10, 25);

    private void showManual() {
	if (manualFrame == null) {
	    manualFrame = new JFrame("Manual");
	    manualFrame.setIconImages(EPTS.getIconList());


	    Container pane = manualFrame.getContentPane();
	    manualPane = new HtmlWithTocPane();
	    manualPane.setContentPaneBorder
		(BorderFactory.createMatteBorder(0, 10, 0, 0,
						DarkmodeMonitor.getDarkmode()?
						contentBackgroundDM:
						contentBackground));

	    // darkmodeChanged();
	    /*
	    manualPane.setBackground(tocBackgroundColor);
	    manualPane.setSplitterBackground(tocBackgroundColor
					     .brighter().brighter());
	    manualPane.setTocBackground(tocBackgroundColor);
	    manualPane.setTocForeground(tocForegroundColor);
	    manualPane.setHtmlButtonBackground(tocBackgroundColor
					       .brighter(), true);
	    manualPane.setHtmlPaneBackground(tocBackgroundColor);
	    */
	    manualFrame.setSize(920, 700);
	    // manualFrame.setBackground(tocFrameBackgroundColor);
	    // pane.setBackground(tocFrameBackgroundColor);
	    manualFrame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			manualFrame.setVisible(false);
		    }
		});
	    JMenuBar menubar = new JMenuBar();
	    JMenu fileMenu = new JMenu(localeString("File"));
	    fileMenu.setMnemonic(vk("VK_FILE"));
	    menubar.add(fileMenu);
	    JMenuItem menuItem = new JMenuItem(localeString("Close"),
					       vk("VK_CLOSE"));
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
		.getResource(DarkmodeMonitor.getDarkmode()?
			     "org/bzdev/epts/manual/manual.dm.xml":
			     "org/bzdev/epts/manual/manual.xml");
	    if (url != null) {
		try {
		    manualPane.setToc(url, true, false);
		    manualPane.setSelectionWithAction(0);
		} catch (IOException e) {
		    SwingErrorMessage.display(e);
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
		} catch (org.xml.sax.SAXException e) {
		    SwingErrorMessage.display(e);
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
		} catch(javax.xml.parsers.ParserConfigurationException e) {
		    SwingErrorMessage.display(e);
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
		}
	    } else {
		SwingErrorMessage.display("cannot load manual");
		    manualFrame.dispose();
		    manualFrame = null;
		    return;
	    }
	    pane.setLayout(new BorderLayout());
	    pane.add(manualPane, "Center");
	    /*
	    manualPane.addPropertyChangeListener(evt -> {
		    // The property name we get on a PopOS system is "name"
		    // but we don't know if all pluggable look and feels
		    // will generate that, so we'll check regardless
		    if (darkmodeChanged()) {
			System.out.println("darkMode = " + darkmode);
		    }
		});
	    */
	    DarkmodeMonitor.addPropertyChangeListener(evt -> {
		    try {
			boolean darkmode = DarkmodeMonitor.getDarkmode();
			manualPane.setContentPaneBorder
			    (BorderFactory
			     .createMatteBorder(0, 10, 0, 0, darkmode?
						contentBackgroundDM:
						contentBackground));
			URL u = ClassLoader.getSystemClassLoader()
			    .getResource(darkmode?
					 "org/bzdev/epts/manual/manual.dm.xml":
					 "org/bzdev/epts/manual/manual.xml");
			if (url != null) {
			    manualPane.setToc(u, true, false);
			    manualPane.setSelectionWithAction(0);
			}
		    } catch(Exception epchl) {
		    }
		});
	}
	manualFrame.setVisible(true);
    }

    private void printManual() {
	try {
	    URL url = ClassLoader.getSystemClassLoader()
		.getResource("org/bzdev/epts/manual/print.html");
	    if (url != null) {
		JEditorPane pane = new JEditorPane();
		pane.setBackground(Color.WHITE);
		pane.setForeground(Color.BLACK);
		pane.setPage(url);
		EditorKit ekit = pane.getEditorKit();
		if (ekit instanceof HTMLEditorKit) {
		    HTMLEditorKit hkit = (HTMLEditorKit)ekit;
		    StyleSheet stylesheet = hkit.getStyleSheet();
		    StyleSheet oursheet = new StyleSheet();
		    StringBuilder sb = new StringBuilder(512);
		    CopyUtilities.copyResource
			("org/bzdev/epts/manual/print.css", sb,
			 Charset.forName("UTF-8"));
		    oursheet.addRule(sb.toString());
		    oursheet.addRule("A {color: rbg(0,0,0);}");
		    oursheet.addRule("A:link {color: rbg(0,0,0);}");
		    oursheet.addRule("A:visited {color: rbg(0,0,0);}");
		    oursheet.addRule("BLOCKQUOTE {background-color: "
				     + "rgb(200,200,200);}");
		    oursheet.addRule("DIV.bodybg {background-color: "
				     + "rbg(255,255,255);}");
		    stylesheet.addStyleSheet(oursheet);
		}
		pane.print(null, new MessageFormat("- {0} -"));
	    }
	} catch (Exception e) {
	    JOptionPane.showMessageDialog(frame,
					  errorMsg("printFailed",
						   e.getMessage()),
					  localeString("errorTitle"),
					  JOptionPane.ERROR_MESSAGE);
	}
    }

    static EmbeddedWebServer ews = null;
    static URI manualURI;
    public static synchronized void startManualWebServerIfNeeded()
	throws Exception
    {
	if (ews == null) {
	    ews = new EmbeddedWebServer(port, 48, 2, null);
	    if (port == 0) port = ews.getPort();
	    ews.add("/", ResourceWebMap.class, "org/bzdev/epts/manual/",
		    null, true, false, true);
	    WebMap wmap = ews.getWebMap("/");
	    if (wmap != null) {
		wmap.addWelcome("index.html");
		wmap.addMapping("html", "text/html; charset=utf-8");
	    }
	    manualURI = new URL("http://localhost:"
				+ port +"/index.html").toURI();
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


    private String prevModeline = "";
    public void setModeline(String line) {
	if (!line.equals(prevModeline)) {
	    if (locState == false) {
		// when locState is true, the prevModeline test
		// doesn't work
		if (selectedRow == -1 ||
		    ptmodel.getRow(selectedRow).getMode()
		    != EPTS.Mode.LOCATION) {
		    endGotoMode();
		}
	    }
	}
	modeline.setText(" " + line);
	prevModeline = line;
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
	cancelPathOps();
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
	// addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	addToPathMenuItem.setEnabled(canAddToBezier());
	deletePathMenuItem.setEnabled(ptmodel.variableNameCount() > 0);
	offsetPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	tfMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	newTFMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
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
		    addToPathMenuItem.setEnabled(canAddToBezier());
		    TransitionTable.getPipeMenuItem().setEnabled(false);
		    TransitionTable.getArcMenuItem().setEnabled(false);
		    TransitionTable.getVectorMenuItem().setEnabled(false);
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
	// addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	addToPathMenuItem.setEnabled(canAddToBezier());
	deletePathMenuItem.setEnabled(ptmodel.variableNameCount() > 0);
	offsetPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	tfMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	newTFMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	return true;
    }

    JMenuItem saveMenuItem;
    JMenuItem saveAsMenuItem;
    JMenuItem addToPathMenuItem; // for Edit menu.
    JMenuItem deletePathMenuItem; // for Edit menu.
    JMenuItem makeCurrentMenuItem; // for Edit menu
    JMenuItem newTFMenuItem;	  // for Edit menu (copy a path)
    JMenuItem tfMenuItem;	  // for Edit menu (modify a path)
    JMenuItem mpMenuItem;	   // for Edit Menu (move a path)
    JMenuItem rotMenuItem;	   // for Edit Menu (rotate a path)
    JMenuItem scaleMenuItem;	   // for Edit Menu (scale a path)
    JMenuItem toCircleMenuItem;	   // for Edit Menu (turn a line into a circle)
    JMenuItem insertArcMenuItem;   // insert an arc into an existing path

    JMenuItem offsetPathMenuItem; // for Tools menu

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

    private int maxdeltaLevel = 2;
    private double getMaxDelta() {
	int divisor = 1 << (maxdeltaLevel - 1);
	return (Math.PI/2.0)/(divisor);
    }

    private Integer maxdeltaLevels[] = {1, 2, 3, 4};

    boolean moveLocOrPath = false;
    boolean rotatePath = false;
    int pathStart = -1;
    double rotateAngle = 0.0;
    boolean scalePath = false;
    Line2D scaleLine = null;
    Point2D scaleCMP = null;
    double paX1, paY1, paX2, paY2;
    boolean scaleInitializedWhenClicked = false;
    double scaleX = 1.0;
    double scaleY = 1.0;
    double initialX = 0.0;
    double initialY = 0.0;

    void cancelPathOps() {
	if (rotatePath || scalePath) {
	    pathStart = -1;
	    centerOfMass = null;
	    rotStart = null;
	    rotRows = null;
	    paxis1 = null;
	    paxis2 = null;
	}
	if (rotatePath) {
	    rotateAngle = 0.0;
	}
	if (scalePath) {
	    scaleLine = null;
	    paX1 = 0.0;
	    paY1 = 0.0;
	    paX2 = 0.0;
	    paY2 = 0.0;
	    scaleX = 1.0;
	    scaleY = 1.0;
	    scaleCMP = null;
	    initialX = 0.0;
	    initialY = 0.0;
	    scaleInitializedWhenClicked = false;
	}
	if (moveLocOrPath || rotatePath || scalePath) {
	    setModeline("");
	    moveLocOrPath = false;
	    rotatePath = false;
	    scalePath = false;
	    if (selectedRow != -1) {
		ptmodel.fireTableChanged(ptmodel.findStart(selectedRow),
					 ptmodel.findEnd(selectedRow),
					 PointTableModel.Mode.MODIFIED);
	    }
	}
    }
    boolean hasPathOps() {
	return moveLocOrPath || rotatePath || scalePath;
    }

    String pathOpsModelineString() {
	if (moveLocOrPath) {
	    return "Move: drag selected point";
	} else if (rotatePath) {
	    return "Rotate: drag selected point around CM";
	} else if (scalePath) {
	    return "Scale: drag selected point";
	} else {
	    return "";
	}
    }

    Point2D centerOfMass = null;
    Point2D rotStart = null;
    PointTMR[] rotRows = null;
    PointTMR[] scaleRows = null;

    private boolean rotPathSetup() {
	int start = ptmodel.findStart(selectedRow);
	if (start == -1) return false;
	if (ptmodel.getRowMode(start) == EPTS.Mode.LOCATION) return false;
	pathStart = start;
	PointTMR row = ptmodel.getRow(selectedRow);
	rotStart = new Point2D.Double(row.getX(), row.getY());
	String name = ptmodel.getVariableName(start);
	Path2D path = ptmodel.getPath(name);
	boolean linear = false;
	Rectangle2D bounds = path.getBounds2D();
	if (bounds.getWidth() == 0.0 && bounds.getHeight() == 0.0) {
	    // reject if this is a degenerate case in which all
	    // control points on the curve are at the same location.
	    pathStart = -1;
	    rotStart = null;
	    return false;
	}
	double area = Path2DInfo.areaOf(path);
	if (area/(bounds.getWidth()*bounds.getHeight()) < 1.e-10) {
	    centerOfMass = new Point2D.Double(bounds.getCenterX(),
						  bounds.getCenterY());
	} else {
	    centerOfMass = Path2DInfo.centerOfMassOf(path);
	    if (centerOfMass == null) {
		// should have been caught above, but just in case we'll
		// set it.
		centerOfMass = new Point2D.Double(bounds.getCenterX(),
						  bounds.getCenterY());
	    } else {
		bounds = null;
	    }
	}
	if (centerOfMass.distanceSq(rotStart) == 0.0) {
	    // reject if the selected point is also the center of mass
	    // as a line we use to determine the rotation will have zero
	    // length.
	    // System.out.println("rejecting - try again");
	    pathStart = -1;
	    rotStart = null;
	    centerOfMass = null;
	    return false;
	}
	rotRows = ptmodel.getRows(selectedRow);
	double[][] moments = (bounds == null)?
	    Path2DInfo.momentsOf(centerOfMass, path):
	    Path2DInfo.momentsOf(centerOfMass, bounds);
	if (moments == null) {
	    /*
	    if (bounds == null) System.out.println("bounding box expected");
	    else {
		System.out.println("bounds = " + bounds);
	    }
	    */
	    if (bounds.getHeight() == 0.0) {
		moments = new double[2][2];
		double tmp = bounds.getWidth();
		moments[0][0] = (tmp*tmp)/3;
	    } else if (bounds.getWidth() == 0.0) {
		moments = new double[2][2];
		double tmp = bounds.getHeight();
		moments[1][1] = (tmp*tmp)/3;
	    }
	}
	/*
	System.out.println("cm = " + centerOfMass);
	System.out.format("| %8.3g %8.3g |\n",
			  moments[0][0], moments[0][1]);
	System.out.format("| %8.3g %8.3g |\n",
			  moments[1][0], moments[1][1]);
	*/
	double[] principalMoments = Moments.principalMoments(moments);
	double mean = (principalMoments[0] + principalMoments[1])/2;
	// double principalAngle;
	// mean == 0.0 if all points are identical, in which case the
	// bounding box is a point, a case handled above.
	if (Math.abs((principalMoments[0]
			     - principalMoments[1])/mean) < 0.01) {
	    // we can't see the difference so use X and Y axes
	    // principalAngle = 0.0;
	    paxis1 = new Line2D.Double(centerOfMass.getX(),
				       centerOfMass.getY(),
				       centerOfMass.getX() + Math.sqrt(mean),
				       centerOfMass.getY());
	    paxis2 = new Line2D.Double(centerOfMass.getX(),
				       centerOfMass.getY(),
				       centerOfMass.getX(),
				       centerOfMass.getY() + Math.sqrt(mean));
	} else {
	    double[][]principalAxes =
		Moments.principalAxes(moments, principalMoments);
	    /*
	    principalAngle = Math.atan2(principalAxes[0][1],
					principalAxes[0][0]);
	    */
	    double len = Math.sqrt(principalMoments[0]);
	    paxis1 = new Line2D.Double(centerOfMass.getX(), centerOfMass.getY(),
				       centerOfMass.getX()
				       + len*principalAxes[0][0],
				       centerOfMass.getY()
				       + len*principalAxes[0][1]);
	    len = Math.sqrt(principalMoments[1]);
	    paxis2 = new Line2D.Double(centerOfMass.getX(), centerOfMass.getY(),
				       centerOfMass.getX()
				       + len*principalAxes[1][0],
				       centerOfMass.getY()
				       + len*principalAxes[1][1]);
	}
	return true;
    }

    private boolean scalePathSetup() {
	int start = ptmodel.findStart(selectedRow);
	if (start == -1) return false;
	if (ptmodel.getRowMode(start) == EPTS.Mode.LOCATION) return false;
	pathStart = start;
	String name = ptmodel.getVariableName(start);
	Path2D path = ptmodel.getPath(name);
	boolean linear = false;
	Rectangle2D bounds = path.getBounds2D();
	if (bounds.getWidth() == 0.0 && bounds.getHeight() == 0.0) {
	    // reject if this is a degenerate case in which all
	    // control points on the curve are at the same location.
	    pathStart = -1;
	    return false;
	}
	double area = Path2DInfo.areaOf(path);
	if (area/(bounds.getWidth()*bounds.getHeight()) < 1.e-10) {
	    centerOfMass = new Point2D.Double(bounds.getCenterX(),
						  bounds.getCenterY());
	} else {
	    centerOfMass = Path2DInfo.centerOfMassOf(path);
	    if (centerOfMass == null) {
		// should have been caught above, but just in case we'll
		// set it.
		centerOfMass = new Point2D.Double(bounds.getCenterX(),
						  bounds.getCenterY());
	    } else {
		bounds = null;
	    }
	}
	scaleRows = ptmodel.getRows(selectedRow);
	double[][] moments = (bounds == null)?
	    Path2DInfo.momentsOf(centerOfMass, path):
	    Path2DInfo.momentsOf(centerOfMass, bounds);
	if (moments == null) {
	    /*
	    if (bounds == null) System.out.println("bounding box expected");
	    else {
		System.out.println("bounds = " + bounds);
	    }
	    */
	    if (bounds.getHeight() == 0.0) {
		moments = new double[2][2];
		double tmp = bounds.getWidth();
		moments[0][0] = (tmp*tmp)/3;
	    } else if (bounds.getWidth() == 0.0) {
		moments = new double[2][2];
		double tmp = bounds.getHeight();
		moments[1][1] = (tmp*tmp)/3;
	    }
	}
	/*
	System.out.println("cm = " + centerOfMass);
	System.out.format("| %8.3g %8.3g |\n",
			  moments[0][0], moments[0][1]);
	System.out.format("| %8.3g %8.3g |\n",
			  moments[1][0], moments[1][1]);
	*/
	double[] principalMoments = Moments.principalMoments(moments);
	double mean = (principalMoments[0] + principalMoments[1])/2;
	// double principalAngle;
	// mean == 0.0 if all points are identical, in which case the
	// bounding box is a point, a case handled above.
	if (Math.abs((principalMoments[0]
			     - principalMoments[1])/mean) < 0.01) {
	    // we can't see the difference so use X and Y axes
	    // principalAngle = 0.0;
	    paxis1 = new Line2D.Double(centerOfMass.getX(),
				       centerOfMass.getY(),
				       centerOfMass.getX() + Math.sqrt(mean),
				       centerOfMass.getY());
	    paX1 = 1.0;
	    paY1 = 0.0;
	    paX2 = 0.0;
	    paY2 = 1.0;
	    paxis2 = new Line2D.Double(centerOfMass.getX(),
				       centerOfMass.getY(),
				       centerOfMass.getX(),
				       centerOfMass.getY() + Math.sqrt(mean));
	} else {
	    double[][]principalAxes =
		Moments.principalAxes(moments, principalMoments);
	    double len1 = Math.sqrt(principalMoments[0]);
	    paxis1 = new Line2D.Double(centerOfMass.getX(), centerOfMass.getY(),
				       centerOfMass.getX()
				       + len1*principalAxes[0][0],
				       centerOfMass.getY()
				       + len1*principalAxes[0][1]);
	    paX1 = principalAxes[0][0];
	    paY1 = principalAxes[0][1];
	    paX2 = principalAxes[1][0];
	    paY2 = principalAxes[1][1];
	    double len2 = Math.sqrt(principalMoments[1]);
	    if (len2/len1 < 1.e-10) {
		// we don't show paxis2 if it is so short that
		// the points effectively lie along a straight line
		paxis2 = null;
	    } else {
		paxis2 =
		    new Line2D.Double(centerOfMass.getX(), centerOfMass.getY(),
				      centerOfMass.getX()
				      + len2*principalAxes[1][0],
				      centerOfMass.getY()
				      + len2*principalAxes[1][1]);
	    }
	}
	double cmxp = (centerOfMass.getX() - xrefpoint) /scaleFactor;
	double cmyp = (centerOfMass.getY() - yrefpoint) / scaleFactor;
	cmyp = height - cmyp;
	scaleCMP = new Point2D.Double(cmxp, cmyp);
	scaleX = 1.0;
	scaleY = 1.0;
	return true;
    }

    /*
    private String itName = null;
    private void itransformPathAction() {
	int index = ptmodel.findStart(selectedRow);
	if (ptmodel.getRowMode(index) == EPTS.Mode.LOCATION) {
	    return;
	} else {
	    itName = ptmodel.getVariableName(index);
	}
	// complete the current path.
	if (nextState != null) {
	    PointTMR lastrow = ptmodel.getLastRow();
	    Enum lrmode = lastrow.getMode();
	    if (lrmode == SplinePathBuilder.CPointType
		.CONTROL || lrmode == SplinePathBuilder.CPointType.SPLINE) {
		ptmodel.setLastRowMode(SplinePathBuilder.CPointType.SEG_END);
	    }
	    // This can call a radio-button menu item's
	    // action listener's actionPeformed
	    // method, and that sets the
	    // nextState variable.
	    ttable.nextState(EPTS.Mode.PATH_END);
	    ptmodel.addRow("", EPTS.Mode.PATH_END, 0.0, 0.0, 0.0, 0.0);
	}
	resetState();
	Path2D itpath = ptmodel.getPath(itName);
	Point2D itref = Path2DInfo.centerOfMassOf(itpath);
	double[][] moments = Path2DInfo.momentsOf(itref, itpath);
	double[] principalMoments = Moments.principalMoments(moments);
	double mean = (principalMoments[0] + principalMoments[1])/2;
	double itprincipalAngle;
	if (mean == 0.0) {
	    // punt - the area must be zero.
	    itprincipalAngle = 0.0;
	} else if (Math.abs((principalMoments[0]
			     - principalMoments[1])/mean) < 0.01) {
	    // we can't see the difference so use X and Y axes
	    itprincipalAngle = 0.0;
	    paxis1 = new Line2D.Double(itref.getX(),  itref.getY(),
				       itref.getX() + Math.sqrt(mean),
				       itref.getY());
	    paxis2 = new Line2D.Double(itref.getX(),  itref.getY(),
				      itref.getX(),
				       itref.getY() + Math.sqrt(mean));
	} else {
	    double[][]principalAxes =
		Moments.principalAxes(moments, principalMoments);
	    itprincipalAngle = Math.atan2(principalAxes[0][1],
					  principalAxes[0][0]);
	    double len = Math.sqrt(principalMoments[0]);
	    paxis1 = new Line2D.Double(itref.getX(), itref.getY(),
				       itref.getX() + len*principalAxes[0][0],
				       itref.getY() + len*principalAxes[0][1]);
	    len = Math.sqrt(principalMoments[1]);
	    paxis2 = new Line2D.Double(itref.getX(), itref.getY(),
				       itref.getX() + len*principalAxes[1][0],
				       itref.getY() + len*principalAxes[1][1]);
	}
    }
    */

    private int circleMaxDeltaDivisor = 2;

    private void toCircleAction() {
	String oldPathName = null;
	if (selectedRow == -1) {
	    Set<String> vnameSet = ptmodel.getToCircleVariableNames();
	    if (vnameSet.isEmpty()) return;
	    String[] vnames =
		vnameSet.toArray(new String[vnameSet.size()]);
	    /*
	    for (String nm: vnames) {
		System.out.println("nm = " + nm);
	    }
	    */
	    String vname;
	    if (vnames.length == 1) {
		vname = vnames[0];
		if (JOptionPane.OK_OPTION !=
		    JOptionPane.showConfirmDialog
		    (frame, String.format(localeString("toCircQ"), vname),
		     localeString("toCircQTitle"),
		     JOptionPane.OK_CANCEL_OPTION,
		     JOptionPane.QUESTION_MESSAGE)) {
		    return;
		}
	    } else {
		vname = (String)JOptionPane.showInputDialog
		    (frame, localeString("toCircle1"),
		     localeString("toCircle2"),
		     JOptionPane.PLAIN_MESSAGE, null,
		     vnames, vnames[0]);
	    }
	    if (vname == null || vname.length() == 0) return;
	    oldPathName = vname;
	} else {
	    int index = ptmodel.findStart(selectedRow);
	    if (ptmodel.getRowMode(index) == EPTS.Mode.LOCATION) {
		return;
	    } else {
		if (ptmodel.getRowMode(index+3) != EPTS.Mode.PATH_END) {
		    return;
		}
		if (ptmodel.getRowMode(index+2) != SplinePathBuilder
		    .CPointType.SEG_END) {
		    return;
		}
		if (ptmodel.getRowMode(index+1) != SplinePathBuilder
		    .CPointType.MOVE_TO) {
		    return;
		}
		oldPathName = ptmodel.getVariableName(index);
	    }
	}
	// complete the current path.
	if (nextState != null) {
	    PointTMR lastrow = ptmodel.getLastRow();
	    Enum lrmode = lastrow.getMode();
	    if (lrmode == SplinePathBuilder.CPointType
		.CONTROL || lrmode == SplinePathBuilder.CPointType.SPLINE) {
		ptmodel.setLastRowMode(SplinePathBuilder.CPointType.SEG_END);
	    }
	    // This can call a radio-button menu item's
	    // action listener's actionPeformed
	    // method, and that sets the
	    // nextState variable.
	    ttable.nextState(EPTS.Mode.PATH_END);
	    ptmodel.addRow("", EPTS.Mode.PATH_END, 0.0, 0.0, 0.0, 0.0);
	}
	resetState();
	// We have found a line segment.
	int start = ptmodel.findStart(oldPathName);
	ptmodel.moveToEnd(start);
	int end = ptmodel.getRowCount() - 1;
	start = end-2;
	PointTMR row1 = ptmodel.getRow(end-2);
	PointTMR row2 = ptmodel.getRow(end-1);
	double x1 = row1.getX();
	double y1 = row1.getY();
	double x2 = row2.getX();
	double y2 = row2.getY();
	double maxDelta = Math.PI/4;
	int n = 2;
	maxDelta /= circleMaxDeltaDivisor;
	Path2D circle = Paths2D.createArc(x1, y1, x2, y2, 2*Math.PI,
					  maxDelta);
	circle.closePath();
	PathIterator pi = circle.getPathIterator(null);
	double[] coords = new double[6];
	int i = 1;
	while (!pi.isDone()) {
	    switch(pi.currentSegment(coords)) {
	    case PathIterator.SEG_MOVETO:
		row1.setX(coords[0], (coords[0] - xrefpoint) / scaleFactor);
		row1.setY(coords[1], height
			  - (coords[1] - yrefpoint)/scaleFactor);
		break;
	    case PathIterator.SEG_CUBICTO:
		double nx, ny, nxp, nyp;
		for (int j = 0; j < 4; j += 2) {
		    nx = coords[j];
		    ny = coords[j+1];
		    nxp = (nx - xrefpoint) / scaleFactor;
		    nyp = (ny - yrefpoint) / scaleFactor;
		    nyp = height - nyp;
		    if (i < 3) {
			PointTMR row = ptmodel.getRow(start + i);
			row.setX(nx, nxp);
			row.setY(ny, nyp);
			row.setMode(SplinePathBuilder.CPointType.CONTROL);
		    } else {
			ptmodel.addRow("",
				       SplinePathBuilder.CPointType.CONTROL,
				       nx, ny, nxp, nyp);
		    }
		    i++;
		}
		nx = coords[4];
		ny = coords[5];
		nxp = (nx - xrefpoint) / scaleFactor;
		nyp = (ny - yrefpoint) / scaleFactor;
		nyp = height - nyp;
		ptmodel.addRow("", SplinePathBuilder.CPointType.SEG_END,
			       nx, ny, nxp, nyp);
		i++;
		break;
	    case PathIterator.SEG_CLOSE:
		nxp = (x2 - xrefpoint) / scaleFactor;
		nyp = (y2 - yrefpoint) / scaleFactor;
		nyp = height - nyp;
		ptmodel.addRow("", SplinePathBuilder.CPointType.CLOSE,
			       x2, y2, nxp, nyp);
		break;
	    default:
		throw new UnexpectedExceptionError();
	    }
	    pi.next();
	    i++;
	}
	ptmodel.addRow("", EPTS.Mode.PATH_END, 0.0, 0.0, 0.0, 0.0);
	end = ptmodel.getRowCount() - 1;
	ptmodel.fireTableChanged(start, end, PointTableModel.Mode.MODIFIED);
	selectedRow = -1;
	setModeline(errorMsg("lineChangedToCircle", oldPathName));
	toCircleMenuItem.setEnabled
	    (!ptmodel.getToCircleVariableNames().isEmpty());
    }

    private boolean insertingArc = false;
    Path2D tmpArcPath = null;
    private void insertArcAction() {
	if (selectedRow == -1) {
	    insertingArc = true;
	    setModeline(localeString("insertingArc"));
	    panel.setCursor(Cursor.getPredefinedCursor
			    (Cursor.CROSSHAIR_CURSOR));
	} else {
	    PointTMR row = ptmodel.getRow(selectedRow);
	    int prevIndex = selectedRow-1;
	    PointTMR prev = ptmodel.getRow(prevIndex);
	    PointTMR next = ptmodel.getRow(selectedRow+1);
	    if (row.getMode() == SplinePathBuilder.CPointType.MOVE_TO) {
		int ind = ptmodel.findEnd(selectedRow) - 2;
		prevIndex = ind;
		prev = ptmodel.getRow(prevIndex);
		double delta = 1.e-10;
		if (Math.abs(prev.getXP() - row.getXP()) < delta
		    && Math.abs(prev.getYP() - row.getYP()) < delta) {
		    prevIndex = ind - 1;
		    prev = ptmodel.getRow(prevIndex);
		}
	    } else if (next.getMode() == SplinePathBuilder.CPointType.CLOSE) {
		next = ptmodel.getRow(ptmodel.findStart(selectedRow) + 1);
	    }

	    InsertArcPane iapane = new InsertArcPane(prev, row, next) {
		    public void accept() {
			AffineTransform af =
			    (zoom == 1.0)? new AffineTransform():
			    AffineTransform.getScaleInstance(zoom, zoom);
			af.translate(-xrefpoint/scaleFactor,
				     height+yrefpoint/scaleFactor);
			af.scale(1.0/scaleFactor, -1.0/scaleFactor);
			tmpArcPath = getArc(af);
			panel.repaint();
		    }
		    public void cancel() {
			tmpArcPath = null;
		    }
		    public void ok() {
			tmpArcPath = null;
		    }
		};
	    JDialog dialog = new JDialog(frame,
					 localeString("insertArcTitle"),
					 true);
	    dialog.setLocationRelativeTo(frame);
	    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	    dialog.add(iapane);
	    dialog.pack();
	    tmpArcPath = null;
	    dialog.setVisible(true);
	    if (iapane.getStatus()) {
		if (iapane.keepCenter()) {
		    String lname = null;
		    while (lname == null) {
			lname = JOptionPane.showInputDialog
			    (frame, localeString("PleaseEnterVariableName"),
			     localeString("ScriptingLanguageVariableName"),
			     JOptionPane.PLAIN_MESSAGE);
			if (lname == null) break;
			PointTMR lrow = new
			    PointTMR(lname, EPTS.Mode.LOCATION,
				     row.getX(), row.getY(),
				     row.getXP(), row.getYP());
			try {
			    ptmodel.addRow(lrow);
			} catch (Exception e) {
				JOptionPane.showMessageDialog
				    (frame, e.getMessage(),
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
			    lname = null;
			}
		    }
		}

		Path2D arcPath = iapane.getArc(null);
		int start = ptmodel.findStart(selectedRow);
		int penultimateIndex = ptmodel.findEnd(selectedRow) - 2;
		PointTMR penultimate = ptmodel.getRow(penultimateIndex);
		// adjust is true if the end of a closed curve is the
		// same point as the start (we don't allow a MOVE_TO as
		// the start of an arc for the case where the path is open).
		boolean adjust =
		    row.getMode() == SplinePathBuilder.CPointType.MOVE_TO
		    && Math.abs(row.getXP() - penultimate.getXP()) < 1.e-10
		    && Math.abs(row.getYP() - penultimate.getYP()) < 1.e-10;
		PathIterator pi = arcPath.getPathIterator(null);
		double[] coords = new double[6];
		int index = selectedRow;
		while (!pi.isDone()) {
		    switch(pi.currentSegment(coords)) {
		    case PathIterator.SEG_MOVETO:
			row.setX(coords[0],
				 (coords[0] - xrefpoint) / scaleFactor);
			row.setY(coords[1],
				 height
				 - (coords[1] - yrefpoint) / scaleFactor);
			break;
		    case PathIterator.SEG_CUBICTO:
			ptmodel
			    .insertRow(index, "",
				       SplinePathBuilder.CPointType.CONTROL,
				       coords[0],
				       coords[1],
				       (coords[0] - xrefpoint) / scaleFactor,
				       height
				       - (coords[1] - yrefpoint) / scaleFactor);
			index++;
			ptmodel
			    .insertRow(index, "",
				       SplinePathBuilder.CPointType.CONTROL,
				       coords[2],
				       coords[3],
				       (coords[2] - xrefpoint) / scaleFactor,
				       height
				       - (coords[3] - yrefpoint) / scaleFactor);
			index++;
			ptmodel
			    .insertRow(index, "",
				       SplinePathBuilder.CPointType.SEG_END,
				       coords[4],
				       coords[5],
				       (coords[4] - xrefpoint) / scaleFactor,
				       height
				       - (coords[5] - yrefpoint) / scaleFactor);
			break;
		    default:
			throw new UnexpectedExceptionError();
		    }
		    index++;
		    pi.next();
		}
		if (adjust) {
		    penultimate.setX(row.getX(), row.getXP());
		    penultimate.setY(row.getY(), row.getYP());
		}
	    }

	    insertingArc = false;
	    tmpArcPath = null;
	    panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    selectedRow = -1;
	    setModeline("");
	    resetState();
	    panel.repaint();
	}
    }

    private void transformPathAction(boolean copyMode) {
	// select a path.
	String oldPathName = null;
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
		    (frame, localeString("newTransformedPath1"),
		     localeString("SelectPath"),
		     JOptionPane.PLAIN_MESSAGE, null,
		     vnames, vnames[0]);
	    }
	    if (vname == null || vname.length() == 0) return;
	    oldPathName = vname;
	} else {
	    int index = ptmodel.findStart(selectedRow);
	    if (ptmodel.getRowMode(index) == EPTS.Mode.LOCATION) {
		return;
	    } else {
		oldPathName = ptmodel.getVariableName(index);
	    }
	}
	// complete the current path.
	if (nextState != null) {
	    PointTMR lastrow = ptmodel.getLastRow();
	    Enum lrmode = lastrow.getMode();
	    if (lrmode == SplinePathBuilder.CPointType
		.CONTROL || lrmode == SplinePathBuilder.CPointType.SPLINE) {
		ptmodel.setLastRowMode(SplinePathBuilder.CPointType.SEG_END);
	    }
	    // This can call a radio-button menu item's
	    // action listener's actionPeformed
	    // method, and that sets the
	    // nextState variable.
	    ttable.nextState(EPTS.Mode.PATH_END);
	    ptmodel.addRow("", EPTS.Mode.PATH_END, 0.0, 0.0, 0.0, 0.0);
	}
	resetState();
	onNewTransformedPath(oldPathName, copyMode);
    }


    JMenuItem zoomOutMI;
    JMenuItem zoomInMI;


    private void setMenus(JFrame frame, double w, double h) {
	JMenuBar menubar = new JMenuBar();
	JMenuItem menuItem;
	JMenu fileMenu = new JMenu(localeString("File"));
	fileMenu.setMnemonic(vk("VK_FILE"));
	menubar.add(fileMenu);
	quitMenuItem = new JMenuItem(localeString("Quit"), vk("VK_QUIT"));
	quitMenuItem.setAccelerator(KeyStroke.getKeyStroke
				    (KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
	quitMenuItem.addActionListener(quitListener);
	fileMenu.add(quitMenuItem);
	saveMenuItem = new JMenuItem(localeString("Save"), vk("VK_SAVE"));
	saveMenuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
	saveMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doSave(false);
		}
	    });
	fileMenu.add(saveMenuItem);

	saveAsMenuItem = new JMenuItem(localeString("SaveAs"),
				       vk("VK_SAVE_AS"));
	saveAsMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doSave(true);
		}
	    });
	fileMenu.add(saveAsMenuItem);


	menuItem = new JMenuItem(localeString("ConfigureGCS"),
				 vk("VK_ConfigureGCS"));
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

	menuItem = new JMenuItem(localeString("PrintTable"),
				 vk("VK_PrintTable"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			/*
			ptable.print(JTable.PrintMode.FIT_WIDTH,
				     null,
				     new MessageFormat("- {0} -"));
			*/
			createPrintTable().print(JTable.PrintMode.FIT_WIDTH,
				     null,
				     new MessageFormat("- {0} -"));
		    } catch (PrinterException ee) {
			ee.printStackTrace();
		    } finally {
			printTableFrame.dispose();
			printTableFrame = null;
		    }
		}
	    });
	fileMenu.add(menuItem);


	menuItem = new JMenuItem(localeString("createTemplate"),
				 vk("VK_CreateTemplate"));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			// System.out.println(EPTS.initialModulePath);
			// System.out.println(EPTS.EPTSmodule);
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
			 localeString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		    }
		}
	    });
	menuItem.setEnabled(false);
	fileMenu.add(menuItem);
	createTemplateMenuItem = menuItem;

	JMenu editMenu = new JMenu(localeString("Edit"));
	editMenu.setMnemonic(vk("VK_EDIT"));
	menubar.add(editMenu);

	menuItem = new JMenuItem(localeString("UndoPointInsertion"),
				 vk("VK_UndoPointInsertion"));
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
			    TransitionTable.getPipeMenuItem()
				.setEnabled(false);
			} else {
			    PointTMR row = ptmodel.getRow(--n);
			    Enum mode = row.getMode();
			    if (mode instanceof EPTS.Mode) {
				EPTS.Mode emode = (EPTS.Mode) mode;
				switch (emode) {
				case LOCATION:
				    resetState();
				    setModeline("");
				    TransitionTable.getPipeMenuItem()
					.setEnabled(false);
				    break;
				case PATH_END:
				    resetState();
				    setModeline("");
				    TransitionTable.getPipeMenuItem()
					.setEnabled(false);
				    break;
				case PATH_START:
				    cancelPathOps();
				    mpMenuItem.setEnabled(false);
				    rotMenuItem.setEnabled(false);
				    scaleMenuItem.setEnabled(false);
				    createPathFinalAction();
				    break;
				}
			    } else if (mode instanceof
				       SplinePathBuilder.CPointType) {
				cancelPathOps();
				mpMenuItem.setEnabled(false);
				rotMenuItem.setEnabled(false);
				scaleMenuItem.setEnabled(false);
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
		    addToPathMenuItem.setEnabled(canAddToBezier());
		    panel.repaint();
		}
	    });
	editMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("AppendBezier"),
				 vk("VK_AppendBezier"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
	addToPathMenuItem = menuItem;
	menuItem.setEnabled(false);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // select a path.
		    int index = -1;
		    if (selectedRow == -1 || selectedClosedPath) {
			Set<String> vnameSet =
			    ptmodel.getOpenPathVariableNames();
			int n = ptmodel.getPathVariableNames().size();
			if (vnameSet.isEmpty()) return;
			String[] vnames =
			    vnameSet.toArray(new String[vnameSet.size()]);
			String vname;
			if (vnames.length == 1 && n == 1) {
			    vname = vnames[0];
			} else {
			    vname = (String)JOptionPane.showInputDialog
				(frame, localeString("SelectPathExtend"),
				 localeString("SelectPath"),
				 JOptionPane.PLAIN_MESSAGE, null,
				 vnames, vnames[0]);
			}
			if (vname == null || vname.length() == 0) return;
			selectedRow = -1;
			selectedClosedPath = false;
			addToPathMenuItem.setEnabled(canAddToBezier());
			insertArcMenuItem.setEnabled(true);
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
			} else if (lrmode == EPTS.Mode.PATH_START) {
			    // we never got started on a path so
			    // we should just continue where we left
			    // off.  The code is copied from createPath()
			    // but assumes the variable was created so we
			    // set the varname from the current row.
			    varname = lastrow.varname;
			    makeCurrentMenuItem.setEnabled(false);
			    mpMenuItem.setEnabled(false);
			    rotMenuItem.setEnabled(false);
			    scaleMenuItem.setEnabled(false);
			    savedCursorPath = panel.getCursor();
			    panel.setCursor(Cursor.getPredefinedCursor
					    (Cursor.CROSSHAIR_CURSOR));
			    createPathFinalAction();
			    return;
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

	editMenu.addSeparator();
	editMenu.add(new JLabel(localeString("DirectManip")));

	mpMenuItem = new JMenuItem(localeString("moveLocOrPath"));
	mpMenuItem.setAccelerator(KeyStroke.getKeyStroke
				      (KeyEvent.VK_M,
				       (InputEvent.ALT_DOWN_MASK
					| InputEvent.CTRL_DOWN_MASK)));
	mpMenuItem.setEnabled(false);
	mpMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (nextState != null) return;
		    if (locState) return;
		    cancelPathOps();
		    moveLocOrPath = true;
		    if (selectedRow == -1) {
			setModeline("Move: select a location or a point "
				    + "on a path; then drag");
		    } else {
			setModeline(pathOpsModelineString());
		    }
		}
	    });
	editMenu.add(mpMenuItem);

	rotMenuItem = new JMenuItem(localeString("rotatePath"));
	rotMenuItem.setAccelerator(KeyStroke.getKeyStroke
				   (KeyEvent.VK_R,
				    (InputEvent.ALT_DOWN_MASK
				     | InputEvent.CTRL_DOWN_MASK)));
	rotMenuItem.setEnabled(false);
	rotMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cancelPathOps();
		    if (nextState != null) return;
		    if (locState) return;
		    rotatePath = true;
		    if (selectedRow == -1) {
			setModeline("Rotate: select a point on a path; "
				    + "then drag around center of mass");
		    } else {
			// will not do anything if the object
			// is a location instead of a path
			if (rotPathSetup()) {
			    setModeline(pathOpsModelineString());
			    panel.repaint();
			} else {
			    selectedRow = -1;
			    selectedClosedPath = false;
			    insertArcMenuItem.setEnabled(true);
			    addToPathMenuItem.setEnabled(canAddToBezier());
			    setModeline("Rotate: select a point (not the "
					+ "center of mass) on a path; "
					+ "then drag to rotate");
			}
		    }
		}
	    });
	editMenu.add(rotMenuItem);

	scaleMenuItem = new JMenuItem(localeString("scalePath"));
	scaleMenuItem.setAccelerator(KeyStroke.getKeyStroke
				   (KeyEvent.VK_S,
				    (InputEvent.ALT_DOWN_MASK
				     | InputEvent.CTRL_DOWN_MASK)));
	scaleMenuItem.setEnabled(false);
	scaleMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cancelPathOps();
		    if (nextState != null) return;
		    if (locState) return;
		    scalePath = true;
		    if (selectedRow == -1) {
			setModeline("Scale: select a point on a path; "
				    + "then drag to scale");
		    } else {
			// will not do anything if the object
			// is a location instead of a path
			if (scalePathSetup()) {
			    setModeline(pathOpsModelineString());
			    PointTMR row = ptmodel.getRow(selectedRow);
			    initialX = row.getXP()*zoom;
			    initialY = row.getYP()*zoom;
			    panel.repaint();
			} else {
			    selectedRow = -1;
			    selectedClosedPath = false;
			    insertArcMenuItem.setEnabled(true);
			    addToPathMenuItem.setEnabled(canAddToBezier());
			    setModeline("Scale: select a point on a path; "
					+ "then drag to scale");
			}
		    }
		}
	    });
	editMenu.add(scaleMenuItem);

	menuItem = new JMenuItem(localeString("circleMaxDeltaDivisor"));
	menuItem.setEnabled(true);
	menuItem.addActionListener(cmdevt -> {
		Object[] options = {
		    Integer.valueOf(1),
		    Integer.valueOf(2),
		    Integer.valueOf(4),
		    Integer.valueOf(8),
		    Integer.valueOf(16),
		    Integer.valueOf(32),
		    Integer.valueOf(64)
		};
		Object obj = JOptionPane
		    .showInputDialog(frame,
				      localeString("circMaxDeltaDiv2"),
				      localeString("circMaxDeltaDiv"),
				      JOptionPane.QUESTION_MESSAGE,
				      null, options, options[1]);
		if  (obj != null) {
		    circleMaxDeltaDivisor = ((Integer)obj).intValue();
		}
	    });
	editMenu.add(menuItem);


	toCircleMenuItem = new JMenuItem(localeString("toCircle"),
					 vk("VK_toCircle"));
	toCircleMenuItem.setAccelerator(KeyStroke.getKeyStroke
					(KeyEvent.VK_C,
					 (InputEvent.ALT_DOWN_MASK
					  | InputEvent.CTRL_DOWN_MASK)));
	toCircleMenuItem.setEnabled(false);

	toCircleMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    toCircleAction();
		}
	    });
	editMenu.add(toCircleMenuItem);

	insertArcMenuItem = new JMenuItem(localeString("insertArc"),
					 vk("VK_insertArc"));
	insertArcMenuItem.setAccelerator(KeyStroke.getKeyStroke
					 (KeyEvent.VK_I,
					  (InputEvent.ALT_DOWN_MASK
					   | InputEvent.CTRL_DOWN_MASK)));

	insertArcMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    insertArcAction();
		}
	    });

	editMenu.add(insertArcMenuItem);

	editMenu.addSeparator();
	editMenu.add(new JLabel(localeString("DialogBased")));

	tfMenuItem = new JMenuItem(localeString("TransformedPath"),
				   vk("VK_TransformedPath"));
	tfMenuItem.setAccelerator (KeyStroke.getKeyStroke
				      (KeyEvent.VK_T,
				       (InputEvent.ALT_DOWN_MASK
					| InputEvent.CTRL_DOWN_MASK)));
	tfMenuItem.setEnabled(false);
	tfMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    transformPathAction(false);
		}
	    });
	editMenu.add(tfMenuItem);


	newTFMenuItem = new JMenuItem(localeString("newTransformedPath"),
				      vk("VK_newTransformedPath")
				      );
	newTFMenuItem.setAccelerator (KeyStroke.getKeyStroke
				      (KeyEvent.VK_N,
				       (InputEvent.ALT_DOWN_MASK
					| InputEvent.CTRL_DOWN_MASK)));
	newTFMenuItem.setEnabled(false);
	newTFMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    transformPathAction(true);
		}
	    });
	editMenu.add(newTFMenuItem);


	editMenu.addSeparator();

	menuItem = new JMenuItem(localeString("DeleteBezier"),
				 vk("VK_DeleteBezier"));
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
				(frame, localeString("SelectPathDelete"),
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
		    addToPathMenuItem.setEnabled(canAddToBezier());
		}
	    });
	editMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("MakeCurrent"),
				 vk("VK_MakeCurrent"));
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
				 vk("VK_CopyTableECMAScript"));
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
	menuItem.setMnemonic(vk("VK_dragImageMode"));
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
	zoomMenu.setMnemonic(vk("VK_Zoom"));
	menubar.add(zoomMenu);

	menuItem = new JMenuItem(localeString("Reset"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int lastIndex = zoomIndex;
		    double lastZoom = zoom;
		    zoomIndex = initialZoomIndex;
		    zoom = zoomTable[initialZoomIndex];
		    if (zoom != lastZoom) {
			rezoom(zoom, lastZoom);
		    }
		    /*
		    if (lastIndex != zoomIndex) {
			rezoom();
		    }
		    */
		}
	    });
	zoomMenu.add(menuItem);

	zoomInMI = new JMenuItem(localeString("ZoomIn"));
	zoomInMI.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_EQUALS,
				 InputEvent.CTRL_DOWN_MASK));
	zoomInMI.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int lastIndex = zoomIndex;
		    double lastZoom = zoom;
		    if (zoom < zoomTable[zoomTable.length - 1]) {
			zoomIndex++;
			if (zoomIndex >= zoomTable.length) {
			    zoomIndex = zoomTable.length -1;
			}
			zoom = zoomTable[zoomIndex];
		    }
		    if (zoom != lastZoom) {
			rezoom(zoom, lastZoom);
		    }
		    /*
		    if (lastIndex != zoomIndex) {
			rezoom();
		    }
		    */
		}
	    });
	zoomMenu.add(zoomInMI);

	zoomOutMI = new JMenuItem(localeString("ZoomOut"));
	zoomOutMI.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
	zoomOutMI.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int lastIndex = zoomIndex;
		    double lastZoom = zoom;
		    if (zoom <= zoomTable[0]) {
			// no change
		    } else if (zoom == zoomTable[zoomIndex]) {
			zoomIndex--;
			if (zoomIndex < 0) {
			    zoomIndex = 0;
			}
			zoom = zoomTable[zoomIndex];
		    } else {
			zoom = zoomTable[zoomIndex];
		    }
		    if (zoom != lastZoom) {
			rezoom(zoom, lastZoom);
		    }
		    /*
		    if (lastIndex != zoomIndex) {
			rezoom();
		    }
		    */
		}
	    });
	zoomMenu.add(zoomOutMI);

	CharDocFilter cdf = new CharDocFilter();
	cdf.setAllowedChars("09..,,");
	InputVerifier iv = new InputVerifier() {
		public boolean verify(JComponent input) {
		    VTextField tf = (VTextField)input;
		    String string = tf.getText();
		    try {
			double value = Double.valueOf(string);
			if (value > 0.0) return true;
			else return false;
		    } catch (Exception e) {
			return false;
		    }
		}
	    };
	VTextField vtf = new VTextField("1.0", 6) {
		@Override
		protected void onAccepted() {
		    try {
			double z = Double.valueOf(getText());
			int maxzi = zoomTable.length - 1;
			if (z <= zoomTable[0]) {
			    zoomIndex = 0;
			} else {
			    for (int i = 0; i <= maxzi; i++) {
				if (z >= zoomTable[i]) {
				    zoomIndex = i;
				}
			    }
			}
			if (zoom != z) {
			    double oldzoom =zoom;
			    zoom = z;
			    rezoom(zoom, oldzoom);
			}
		    } catch (Exception e) {
			return;
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, localeString("notPositive"),
			 localeString("Error"), JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
        ((AbstractDocument)vtf.getDocument()).setDocumentFilter(cdf);
	vtf.setInputVerifier(iv);
	vtf.setAllowEmptyTextField(false);
	menuItem = new VTextFieldMenuItem(vtf,
					  localeString("ZoomTo"),
					  frame,
					  localeString("ZoomTo"),
					  localeString("ZoomFactor"),
					  true);
	menuItem.setMnemonic(vk("VK_ZoomTo"));
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
	    zoomTable[i] = MathOps.pow(2.0, exponent);
	    if (exponent < 0) {
		menuItem = new JMenuItem
		    (String.format("1/%d", MathOps.lPow(2,-exponent)));

	    } else if (exponent == 0) {
		zoomIndex = i;
		initialZoomIndex = i;
		menuItem = new JMenuItem("1.0");
	    } else {
		menuItem = new JMenuItem
		    (String.format("%d", MathOps.lPow(2,exponent)));
	    }
	    final int index = i;
	    menuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			int lastIndex = zoomIndex;
			double lastZoom = zoom;
			zoomIndex = index;
			zoom = zoomTable[zoomIndex];
			if (zoom != lastZoom) {
			    rezoom(zoom, lastZoom);
			}
			/*
			if (zoomIndex != lastIndex) {
			    rezoom();
			}
			*/
		    }
		});
	    zoomMenu.add(menuItem);
	    i++;
	}

	JMenu measureMenu = new JMenu(localeString("MeasureDistance"));
	measureMenu.setMnemonic(vk("VK_MeasureDistance"));
	menubar.add(measureMenu);
	menuItem = new JMenuItem(localeString("ImageSpaceDistance"),
				 vk("VK_ImageSpaceDistance"));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    measureDistance(false);
		}
	    });
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
	measureMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("GCSDistance"),
				 vk("VK_GCSDistance"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_G, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    measureDistance(true);
		}
	    });
	measureMenu.add(menuItem);

	JMenu toolMenu = new JMenu(localeString("Tools"));
	toolMenu.setMnemonic(vk("VK_Tools"));
	menubar.add(toolMenu);
 
	menuItem = new JMenuItem(localeString("ShowPointTable"),
				 vk("VK_ShowPointTable"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    tableFrame.setVisible(true);
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("EditVarParameters"),
				 vk("VK_EditVarParameters"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    InputTablePane.ColSpec colspec[] = {
			new InputTablePane.ColSpec
			(localeString("VariableOrIndex"),
			 "MMMMMMMMMMMMMMMM",
			 String.class, null, null),
			new InputTablePane.ColSpec
			(localeString("Key"),
			 "MMMMMMM",
			 String.class, null,
			 InputTablePane.DEFAULT_CELL_EDITOR),
			new InputTablePane.ColSpec
			(localeString("Link"),
			 "MMMMMMMMMMMMMMMMMMMMMMMMMMM",
			 String.class, null,
			 InputTablePane.DEFAULT_CELL_EDITOR),
			new InputTablePane.ColSpec
			(localeString("Descr"),
			 "MMMMMMMMMMMMMMMMMMMMMMMMMMM",
			 String.class, null, InputTablePane.DEFAULT_CELL_EDITOR)
		    };
		    Vector<Vector<Object>> initialRows =
			new Vector<>(ptmodel.names.size());
		    for (String vn: ptmodel.names) {
			Vector<Object>row = new Vector<>(4);
			row.add(vn);
			String s = keys.get(vn);
			row.add((s == null)? "": s);
			s = links.get(vn);
			row.add((s == null)? "": s);
			s = descriptions.get(vn);
			row.add((s == null)? "": s);
			initialRows.add(row);
		    }
		    InputTablePane parmPane = InputTablePane
			.showDialog(frame, localeString("EditVarParameters"),
				    colspec,  initialRows.size(), initialRows,
				    false, false, false);
		    if (parmPane != null) {
			int n = parmPane.getRowCount();
			for (int i = 0; i < n; i++) {
			    String vn = ((String)parmPane.getValueAt(i, 0))
				.trim();
			    String key = ((String)parmPane.getValueAt(i, 1))
				.trim();
			    String link = ((String)parmPane.getValueAt(i, 2))
				.trim();
			    String descr =((String) parmPane.getValueAt(i, 3))
				.trim();
			    if (vn != null) {
				if (key.length() > 0) {
				    keys.put(vn, key);
				} else {
				    keys.remove(vn);
				}
				if (link.length() > 0) {
				    links.put(vn, link);
				} else {
				    links.remove(vn);
				}
				if (descr.length() > 0) {
				    descriptions.put(vn, descr);
				} else {
				    descriptions.remove(vn);
				}
			    }
			}
		    }
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("setMaxDelta"),
				 vk("VK_SetMaxDelta"));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Integer newlevel = (Integer) JOptionPane.showInputDialog
			(frame, localeString("selectMaxdelta"),
			 localeString("setMaxDelta"),
			 JOptionPane.PLAIN_MESSAGE,
			 null,
			 maxdeltaLevels,
			 maxdeltaLevel);
		    if (newlevel != null) {
			maxdeltaLevel = (int) newlevel;
		    }
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("CreatePoint"),
				 vk("VK_CreatePoint"));
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
			String[] components  = varname.split(";");
			String key = null;
			String link = null;
			String descr = null;
			if (components.length > 1) {
			    varname = components[0];
			}
			for (int i = 1; i < components.length; i++) {
			    String s = components[i];
			    String[] term = s.split("\\s*=\\s*");
			    if (term.length != 2) {
				JOptionPane.showMessageDialog
				    (frame, errorMsg("badVarnameFormat", s),
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
				return;
			    }
			    String k = term[0].trim();
			    String v = term[1].trim();
			    if (k.equals("key")) {
				key = v;
			    } else if (k.equals("link")) {
				link = v;
			    } else if (k.equals("descr")) {
				descr = v;
			    } else {
				JOptionPane.showMessageDialog
				    (frame, errorMsg("badVarnameFormat", s),
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
				return;
			    }
			}
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
			if (key != null) keys.put(varname, key);
			if (link != null) links.put(varname, link);
			if (descr != null) descriptions.put(varname, descr);
		    }
		    createLocation();
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("CreateBezierPath"),
				 vk("VK_CreateBezierPath"));
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

	menuItem = TransitionTable.getOffsetMenuItem();
	offsetPathMenuItem = menuItem;

	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    terminatePartialPath();
		    // System.out.println("creating an offset ...");
		    OffsetPane offsetPane = new OffsetPane(ptmodel);
		    //setup(offsetPane);
		    final JComponent target = offsetPane.getTargetTF();
		    target.addAncestorListener(new AncestorListener() {
			    @Override
			    public void ancestorAdded(AncestorEvent e) {
				// SwingUtilities.invokeLater(repeat);
				SwingOps.tryRequestFocusInWindow(target, 0);
			    }
			    @Override
			    public void ancestorMoved(AncestorEvent e) {}
			    @Override
			    public void ancestorRemoved(AncestorEvent e) {}
			});
		    int status = JOptionPane.showConfirmDialog
			(frame, offsetPane, localeString("CreateOffset"),
			 JOptionPane.OK_CANCEL_OPTION,
			 JOptionPane.QUESTION_MESSAGE);
		    if (status == 0) {
			double dist1 = offsetPane.getDist1();
			double dist2 = offsetPane.getDist2();
			String bpn = offsetPane.getBasePathName();
			if (bpn == null) return;
			OffsetPane.lastBaseName = bpn;
			// System.out.println("geting path for " + bpn);
			Path2D basePath = ptmodel.getPath(bpn);
			if (basePath == null) return;
			Set<String> pvnames = ptmodel.getPathVariableNames();
			String opathName = null;
			String opathName2 = null;
			boolean closedPath = offsetPane.requestsClosedPath();
			double mdelta = getMaxDelta();
			Path2D opath = null;
			Path2D opath2 = null;
			if (closedPath == false) {
			    do {
				opathName = JOptionPane
				    .showInputDialog(frame,
						     localeString("opathName"),
						     localeString("opathTitle"),
						     JOptionPane.PLAIN_MESSAGE);
				if (opathName == null) return;
				opathName = opathName.trim();
				// System.out.println(opathName);
			    } while (pvnames.contains(opathName));
			    double dist3 = offsetPane.getDist3();
			    // System.out.println("dist3 = " + dist3);
			    if ((offsetPane.requestsCCWPath()
				 && dist1 == dist3)
				|| (offsetPane.requestsCWPath()
				    && dist2 == dist3)) {
				opath = Paths2D.offsetBy(basePath,
							 dist1, dist2,
							 closedPath, mdelta);
				Path2D[] opaths = PathSplitter.split(opath);
				if (opaths.length == 2) {
				    if (offsetPane.requestsCCWPath()) {
					opath = opaths[1];
				    } else if (offsetPane.requestsCWPath()){
					opath = opaths[0];
				    }
				    if (offsetPane.requestsReversedPath()) {
					opath = Paths2D.reverse(opath);
				    }
				} else {
				    return;
				}
			    } else {
				boolean ccw = offsetPane.requestsCCWPath();
				if (ccw && dist1 < dist3) {
				    dist1 = dist3;
				} else if (dist2 < dist3) {
				    dist2 = dist3;
				}
				opath = Paths2D.offsetBy(basePath,
							 dist1, dist2, dist3,
							 ccw, mdelta);
				if (offsetPane.requestsReversedPath()) {
				    opath = Paths2D.reverse(opath);
				}
				/*
				System.out.println("opath end: "
						   + opath.getCurrentPoint());
				*/
			    }
			} else {
			    opath = Paths2D.offsetBy(basePath,
						     dist1, dist2, closedPath,
						     mdelta);
			    if (opath != null) {
				OffsetPane.lastBaseName = bpn;
				Path2D[] opaths = PathSplitter.split(opath);
				if (opaths.length == 2) {
				    opath = opaths[0];
				    opath2 = opaths[1];
				}
			    }
			    if (opath2 == null) {
				do {
				    opathName = JOptionPane.showInputDialog
					(frame,
					 localeString("opathName"),
					 localeString("opathTitle"),
					 JOptionPane.PLAIN_MESSAGE);
				    if (opathName == null) return;
				    opathName = opathName.trim();
				    // System.out.println(opathName);
				} while (pvnames.contains(opathName));
			    } else {
				do {
				    opathName = JOptionPane.showInputDialog
					(frame,
					 localeString("opathName1"),
					 localeString("opathTitle1"),
					 JOptionPane.PLAIN_MESSAGE);
				    if (opathName == null) return;
				    opathName = opathName.trim();
				    // System.out.println(opathName);
				} while (pvnames.contains(opathName));
				do {
				    opathName2 = JOptionPane.showInputDialog
					(frame,
					 localeString("opathName2"),
					 localeString("opathTitle2"),
					 JOptionPane.PLAIN_MESSAGE);
				    if (opathName2 == null) return;
				    opathName2 = opathName2.trim();
				    // System.out.println(opathName);
				} while (pvnames.contains(opathName2));
			    }
			}
			if (opath == null) return;
			offsetPane.saveIndices();
			offsetPane.saveDistances();
			offsetPane.saveBaseMap();
			OffsetPane.addOurPathName(opathName, bpn);
			ptmodel.addRow(new PointTMR(opathName,
						    EPTS.Mode.PATH_START,
						    0.0, 0.0, 0.0, 0.0));
			double xstart = 0.0;
			double ystart = 0.0;
			double nx, ny, nxp, nyp;
			for (SplinePathBuilder.CPoint cp:
				 Path2DInfo.getCPoints(opath)) {
			    SplinePathBuilder.CPointType cptype
				= (SplinePathBuilder.CPointType) cp.type;
			    switch(cptype) {
			    case MOVE_TO:
				xstart = cp.x;
				ystart = cp.y;
			    case SEG_END:
			    case CONTROL:
				nx = cp.x;
				ny = cp.y;
				nxp = (nx - xrefpoint) / scaleFactor;
				nyp = (ny - yrefpoint) / scaleFactor;
				nyp = height - nyp;
				ptmodel.addRow("", cp.type, nx, ny,
					       nxp, nyp);
				break;
			    case CLOSE:
				nxp = (xstart - xrefpoint) / scaleFactor;
				nyp = (ystart - yrefpoint) / scaleFactor;
				nyp = height - nyp;
				ptmodel.addRow("", cp.type, xstart, ystart,
					       nxp, nyp);
				break;
			    default:
				break;
			    }
			}
			ptmodel.addRow(new PointTMR("",
						    EPTS.Mode.PATH_END,
						    0.0, 0.0, 0.0, 0.0));
			if (opath2 != null) {
			    OffsetPane.addOurPathName(opathName2, bpn);
			    ptmodel.addRow(new PointTMR
					   (opathName2,
					    EPTS.Mode.PATH_START,
					    0.0, 0.0, 0.0, 0.0));
			    xstart = 0.0;
			    ystart = 0.0;
			    for (SplinePathBuilder.CPoint cp:
				     Path2DInfo.getCPoints(opath2)) {
				SplinePathBuilder.CPointType cptype
				    = (SplinePathBuilder.CPointType) cp.type;
				switch(cptype) {
				case MOVE_TO:
				    xstart = cp.x;
				    ystart = cp.y;
				case SEG_END:
				case CONTROL:
				    nx = cp.x;
				    ny = cp.y;
				    nxp = (nx - xrefpoint) / scaleFactor;
				    nyp = (ny - yrefpoint) / scaleFactor;
				    nyp = height - nyp;
				    ptmodel.addRow("", cp.type, nx, ny,
						   nxp, nyp);
				    break;
				case CLOSE:
				    nxp = (xstart - xrefpoint) / scaleFactor;
				    nyp = (ystart - yrefpoint) / scaleFactor;
				    nyp = height - nyp;
				    ptmodel.addRow("", cp.type,
						   xstart, ystart,
						   nxp, nyp);
				    break;
				default:
				    break;
				}
			    }
			    ptmodel.addRow(new PointTMR("",
							EPTS.Mode.PATH_END,
							0.0, 0.0,
							0.0, 0.0));
			}
		    }
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
				if (ttable != null) {
				    ttable.nextState(EPTS.Mode.PATH_END);
				    ptmodel.addRow("", EPTS.Mode.PATH_END,
						   0.0, 0.0, 0.0, 0.0);
				} else {
				    if (lrmode == EPTS.Mode.PATH_START) {
					ptmodel.deleteRow
					    (ptmodel.getRowCount() - 1);
					addToPathMenuItem.setEnabled
					    (canAddToBezier());

				    }
				}
			    }
			    setModeline("Path Complete");
			    resetState();
			    addToPathMenuItem.setEnabled(canAddToBezier());
			    panel.repaint();
			}
		    }
		});
	}
	toolMenu.addSeparator();
	toolMenu.add(new JLabel(localeString("bezierOps")));
	menuItem = TransitionTable.getGotoMenuItem();
	menuItem.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    startGotoMode();
		}
	    });
	toolMenu.add(menuItem);
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
				// lpane.xtf.requestFocusInWindow();
				SwingOps.tryRequestFocusInWindow(lpane.xtf, 0);
			    }
			    @Override
			    public void ancestorMoved(AncestorEvent e) {}
			    @Override
			    public void ancestorRemoved(AncestorEvent e) {}
			});
		    int status = JOptionPane.showConfirmDialog
			(frame, lpane, localeString("moveToLocation"),
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
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			    return;
			}
			if (locState) {
			    if (selectedRow != -1) {
				if (moveLocOrPath) {
				    ptmodel.moveObject(selectedRow,
						       x, y, xp, yp, true);
				    cancelPathOps();
				} else {
				    ptmodel.changeCoords(selectedRow,
							 x, y, xp, yp, true);
				}
			    } else {
				ptmodel.addRow(varname, EPTS.Mode.LOCATION,
					       x, y, xp, yp);
				deletePathMenuItem.setEnabled(true);
			    }
			    locState = false;
			    setModeline("");
			    TransitionTable.getGotoMenuItem().setEnabled(false);
			    TransitionTable.getPipeMenuItem().setEnabled(false);
			    TransitionTable.getLocMenuItem().setEnabled(false);
			    TransitionTable.getShiftMenuItem()
				.setEnabled(false);
			    resetState ();
			} else {
			    if (selectedRow != -1) {
				if (moveLocOrPath) {
				    ptmodel.moveObject(selectedRow,
						       x, y, xp, yp, true);
				    cancelPathOps();
				} else {
				    ptmodel.changeCoords(selectedRow,
							 x, y, xp, yp, true);
				}
				selectedRow = -1;
				selectedClosedPath = false;
				insertArcMenuItem.setEnabled(true);
				addToPathMenuItem.setEnabled(canAddToBezier());
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
				TransitionTable.getGotoMenuItem()
				    .setEnabled(false);
				TransitionTable.getPipeMenuItem()
				    .setEnabled(false);
				TransitionTable.getLocMenuItem()
				    .setEnabled(false);
				TransitionTable.getShiftMenuItem()
				    .setEnabled(false);
			    }
			}
			JViewport vp = scrollPane.getViewport();
			int vpw = vp.getWidth();
			int vph = vp.getHeight();
			int ipx = (int)(Math.round(xp*zoom - vpw/2));
			int ipy = (int)(Math.round(yp*zoom - vph/2));
			if (ipx < 0) ipx = 0;
			if (ipy < 0) ipy = 0;
			vp.setViewPosition(new Point(ipx, ipy));
			scrollPane.repaint();
		    }
		}
	    });
	toolMenu.add(menuItem);
	menuItem = TransitionTable.getShiftMenuItem();
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    final ShiftPane spane = new ShiftPane();
		    spane.addAncestorListener(new AncestorListener() {
			    @Override
			    public void ancestorAdded(AncestorEvent e) {
				// spane.xtf.requestFocusInWindow();
				SwingOps.tryRequestFocusInWindow(spane.xtf, 0);
			    }
			    @Override
			    public void ancestorMoved(AncestorEvent e) {}
			    @Override
			    public void ancestorRemoved(AncestorEvent e) {}
			});
		    int status = JOptionPane.showConfirmDialog
			(frame, spane, localeString("shiftLocation"),
			 JOptionPane.OK_CANCEL_OPTION,
			 JOptionPane.QUESTION_MESSAGE);
		    if (status == 0) {
			spane.saveIndices();
			double ix, iy;
			if (selectedRow != -1) {
			    PointTMR srow = ptmodel.getRow(selectedRow);
			    ix = srow.getX();
			    iy = srow.getY();
			} else {
			    PointTMR srow = ptmodel.getLastRow();
			    Enum<?> mode = srow.getMode();
			    if (mode instanceof EPTS.Mode
				|| mode == SplinePathBuilder.CPointType.CLOSE) {
				// should not happen if we disable the
				// menu item when not in the right state
				JOptionPane.showMessageDialog
				    (frame, "Cannot get starting location",
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
				return;
			    } else {
				ix = srow.getX();
				iy = srow.getY();
			    }
			}
			double x = ix + spane.getXCoord();
			double y = iy + spane.getYCoord();
			double xp = (x - xrefpoint) / scaleFactor;
			double yp = (y - yrefpoint) / scaleFactor;
			yp = height - yp;
			if (xp < 0.0 || xp > width || yp < 0.0 || yp > height) {
			    JOptionPane.showMessageDialog
				(frame, "New point out of range",
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			    return;
			}
			if (locState) {
			    if (selectedRow != -1) {
				if (moveLocOrPath) {
				    ptmodel.moveObject(selectedRow,
						       x, y, xp, yp, true);
				    cancelPathOps();
				} else {
				    ptmodel.changeCoords(selectedRow,
							 x, y, xp, yp, true);
				}
			    } else {
				ptmodel.addRow(varname, EPTS.Mode.LOCATION,
					       x, y, xp, yp);
				deletePathMenuItem.setEnabled(true);
			    }
			    locState = false;
			    setModeline("");
			    TransitionTable.getGotoMenuItem().setEnabled(false);
			    TransitionTable.getPipeMenuItem().setEnabled(false);
			    TransitionTable.getLocMenuItem().setEnabled(false);
			    TransitionTable.getShiftMenuItem()
				.setEnabled(false);
			    resetState ();
			} else {
			    if (selectedRow != -1) {
				if (moveLocOrPath) {
				    ptmodel.moveObject(selectedRow,
						       x, y, xp, yp, true);
				    cancelPathOps();
				} else {
				    ptmodel.changeCoords(selectedRow,
							 x, y, xp, yp, true);
				}
				selectedRow = -1;
				selectedClosedPath = false;
				insertArcMenuItem.setEnabled(true);
				addToPathMenuItem.setEnabled(canAddToBezier());
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
				TransitionTable.getGotoMenuItem()
				    .setEnabled(false);
				TransitionTable.getPipeMenuItem()
				    .setEnabled(false);
				TransitionTable.getLocMenuItem()
				    .setEnabled(false);
				TransitionTable.getShiftMenuItem()
				    .setEnabled(false);
			    }
			}
			JViewport vp = scrollPane.getViewport();
			int vpw = vp.getWidth();
			int vph = vp.getHeight();
			int ipx = (int)(Math.round(xp*zoom - vpw/2));
			int ipy = (int)(Math.round(yp*zoom - vph/2));
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
		    PointTMR lastRow = ptmodel.getLastRow();
		    PointTMR prevRow = ptmodel.getNextToLastRow();
		    if (lastRow != null && prevRow != null) {
			Enum lm = lastRow.getMode();
			Enum pm = prevRow.getMode();
			// System.out.println("modes = " + lm + ", " + pm);
			if (lm instanceof SplinePathBuilder.CPointType
			    && pm instanceof SplinePathBuilder.CPointType) {
			    SplinePathBuilder.CPointType lastMode =
				(SplinePathBuilder.CPointType) lm;
			    SplinePathBuilder.CPointType prevMode =
				(SplinePathBuilder.CPointType) pm;
			    double theta = 0.0;
			    switch(lastMode) {
			    case CLOSE:
				break;
			    case SEG_END:
				switch(prevMode) {
				case MOVE_TO:
				case CONTROL:
				case SEG_END:
				    double delX =
					lastRow.getX() - prevRow.getX();
				    double delY =
					lastRow.getY() - prevRow.getY();
				    theta = Math.atan2(delY, delX);
				    vpane.setAngle(theta);
				    break;
				case SPLINE:
				    // System.out.println("found a spline");
				    // create the spline and recover the
				    // control points.
				    {
					SplinePath2D segment =
					    new SplinePath2D();
					int lastind = ptmodel.getRowCount()-1;
					int prevind = lastind;
					SplinePathBuilder
					    sb = new SplinePathBuilder();
					LinkedList<SplinePathBuilder.CPoint>
					    cpoints = new LinkedList<>();
					cpoints.add(new SplinePathBuilder
						    .CPoint
						    (lastMode,
						     lastRow.getX(),
						     lastRow.getY()));
					Enum m = null;
					do {
					    prevind--;
					    PointTMR r =
						ptmodel.getRow(prevind);
					    m = r.getMode();
					    cpoints.add(new SplinePathBuilder.
							CPoint
							((SplinePathBuilder
							  .CPointType) m,
							 r.getX(),
							 r.getY()));
					} while (m ==
						 SplinePathBuilder.CPointType
						 .SPLINE);
					Collections.reverse(cpoints);
					sb.initPath();
					sb.append(cpoints);
					segment = sb.getPath();
					List<SplinePathBuilder.CPoint> list
					    = Path2DInfo.getCPoints(segment);
					if (list.size() > 1) {
					    SplinePathBuilder.CPoint prevCPoint
						= list.get(list.size() - 2);
					    delX = lastRow.getX()
						- prevCPoint.x;
					    delY = lastRow.getY()
						- prevCPoint.y;
					    theta = Math.atan2(delY, delX);
					    vpane.setAngle(theta);
					}
				    }
				}
			    }
			    /*
			    if (lastMode != SplinePathBuilder.CPointType.CLOSE){
				double delX = lastRow.getX() - prevRow.getX();
				double delY = lastRow.getY() - prevRow.getY();
				double theta = Math.atan2(delY, delX);
				vpane.setAngle(theta);
			    }
			    */
			}
		    }
		    vpane.addAncestorListener(new AncestorListener() {
			    @Override
			    public void ancestorAdded(AncestorEvent e) {
				// vpane.ltf.requestFocusInWindow();
				SwingOps.tryRequestFocusInWindow(vpane.ltf, 0);
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
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			    return;
			}
			*/
			double xp = (x - xrefpoint) / scaleFactor;
			double yp = (y - yrefpoint) / scaleFactor;
			yp = height - yp;
			if (xp < 0.0 || xp > width || yp < 0.0 || yp > height) {
			    JOptionPane.showMessageDialog
				(frame, "New point out of range",
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
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
			int ipx = (int)(Math.round(xp*zoom - vpw/2));
			int ipy = (int)(Math.round(yp*zoom - vph/2));
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
				// apane.rtf.requestFocusInWindow();
				SwingOps.tryRequestFocusInWindow(apane.rtf, 0);

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
			double maxdelta = getMaxDelta ();
			boolean ccw = apane.isCounterClockwise();
			int lastind = ptmodel.getRowCount()-1;
			int prevind = lastind - 1;
			PointTMR row2 = ptmodel.getRow(lastind);
			PointTMR row1 = ptmodel.getRow(prevind);
			Enum<?> mode = row1.getMode();
			if ((row2.getMode()
			     == SplinePathBuilder.CPointType.MOVE_TO)
			    && (mode == EPTS.Mode.PATH_START)) {
			    final AnglePane angPane = new AnglePane();
			    angPane.addAncestorListener(new AncestorListener() {
				    @Override
				    public void ancestorAdded(AncestorEvent e) {
					// angPane.atf.requestFocusInWindow();
					SwingOps.tryRequestFocusInWindow
					    (angPane.atf, 0);
				    }
				    @Override
				    public void ancestorMoved(AncestorEvent e) {
				    }
				    @Override
				    public void ancestorRemoved
					(AncestorEvent e) {
				    }
				});
			    final JDialog dialog =
				new JDialog(frame,
					    localeString("setInitAngle"),
					    true);
			    dialog.setLocationRelativeTo(frame);
			    dialog.setDefaultCloseOperation
				(JDialog.DISPOSE_ON_CLOSE);
			    dialog.add(angPane);
			    dialog.pack();
			    dialog.setVisible(true);
			    angPane.saveIndices();
			    // set row1 to a fake value. We only use
			    // x, and y, not xp, or yp.
			    // This will let us compute the tangent to the
			    // curve correctly.
			    double theta = angPane.getAngle();
			    double fakeX = row2.getX() - Math.cos(theta);
			    double fakeY = row2.getY() - Math.sin(theta);
			    mode = SplinePathBuilder.CPointType.MOVE_TO;
			    row1 = new PointTMR(null, mode,
						fakeX, fakeY, 0.0, 0.0);
			}
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
					 localeString("errorTitle"),
					 JOptionPane.ERROR_MESSAGE);
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
					 localeString("errorTitle"),
					 JOptionPane.ERROR_MESSAGE);
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
					 localeString("errorTitle"),
					 JOptionPane.ERROR_MESSAGE);
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
			    int ipx = (int)(Math.round(xp*zoom - vpw/2));
			    int ipy = (int)(Math.round(yp*zoom - vph/2));
			    if (ipx < 0) ipx = 0;
			    if (ipy < 0) ipy = 0;
			    vp.setViewPosition(new Point(ipx, ipy));
			    scrollPane.repaint();
			}
		    }
		}
	    });
	toolMenu.add(menuItem);
	menuItem = TransitionTable.getPipeMenuItem();
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    final PipePane pipePane = new PipePane(frame);
		    JDialog dialog = new JDialog(frame,
						 localeString("pipePaneTitle"),
						 true);
		    dialog.setLocationRelativeTo(frame);
		    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		    dialog.add(pipePane);
		    dialog.pack();
		    dialog.setVisible(true);
		    if (pipePane.getStatus()) {
			int unitsIndex = pipePane.getUnitsIndex();
			int lineno = 0;
			PointTMR lrow;
			double x, y;
			double xp, yp;
			Enum<?> lmode;
			try {
			    CSVReader r = pipePane.getReader();
			    String[] fields;
			    boolean firstTime = true;
			    double baseX = 0.0;
			    double baseY = 0.0;;
			    lrow = ptmodel.getLastRow();
			    lmode = lrow.getMode();
			    Enum<?> ns = null;
			    // System.out.println("reading rows");
			    while ((fields = r.nextRow()) != null) {
				lineno++;
				if (fields.length < 3) {
				    String msg = errorMsg("pipeFieldLen");
				    throw new Exception(msg);
				}
				if (firstTime &&
				    !fields[0].trim().equals("MOVE_TO")) {
				    throw new Exception(errorMsg("pipeMOVETO"));
				}
				x = Double.parseDouble(fields[1].trim())
				    + baseX;
				y = Double.parseDouble(fields[2].trim())
				    + baseY;
				x = ConfigGCSPane.convert[unitsIndex]
				    .valueAt(x);
				y = ConfigGCSPane.convert[unitsIndex]
				    .valueAt(y);
				xp = (x - xrefpoint) / scaleFactor;
				yp = (y - yrefpoint) / scaleFactor;
				yp = height - yp;
				if (fields[0].trim().equals("MOVE_TO")) {
				    if (lmode != EPTS.Mode.PATH_START) {
					baseX = lrow.getX() - x;
					baseY = lrow.getY() - y;
					firstTime = false;
					continue;
				    } else {
					ns = SplinePathBuilder.CPointType
					    .MOVE_TO;
					ptmodel.addRow("", ns, x, y, xp, yp);
				    }
				} else if (fields[0].trim().equals("SEG_END")) {
				    ns = SplinePathBuilder.CPointType.SEG_END;
				    ptmodel.addRow("", ns, x, y, xp, yp);
				} else if (fields[0].trim().equals("CONTROL")) {
				    ns = SplinePathBuilder.CPointType.CONTROL;
				    ptmodel.addRow("", ns, x, y, xp, yp);
				} else if (fields[0].trim().equals("SPLINE")) {
				    ns = SplinePathBuilder.CPointType.SPLINE;
				    ptmodel.addRow("", ns, x, y, xp, yp);
				} else if (fields[0].trim().equals("CLOSE")) {
				    throw new Exception("NoClose");
				} else {
				    String msg =
					errorMsg("unknownField", fields[0])
					.trim();
				    throw new Exception(msg);
				}
				ttable.nextState(ns);
				firstTime = false;
				fields = null;
			    }
			    r.close();
			    if (pipePane.isAlive() == false
				&& pipePane.exitValue() != 0) {
				if (pipePane.msgReady()) {
				    String msg = pipePane.errmsg();
				    JOptionPane.showMessageDialog
					(frame, msg, localeString("errorTitle"),
					 JOptionPane.ERROR_MESSAGE);
				}
			    }
			} catch (Exception ex) {
			    // First check if we can find a useful
			    // error message
			    if (pipePane.isAlive() == false
				&& pipePane.exitValue() != 0) {
				if (pipePane.msgReady()) {
				    String msg = pipePane.errmsg();
				    JOptionPane.showMessageDialog
					(frame, msg, localeString("errorTitle"),
					 JOptionPane.ERROR_MESSAGE);
				}
			    } else {
				String s = ex.getMessage();
				String msg = errorMsg("pipeReadErr", lineno, s);
				JOptionPane.showMessageDialog
				    (frame, msg, localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
			    }
			}
			lrow = ptmodel.getLastRow();
			lmode = lrow.getMode();
			if (lmode instanceof SplinePathBuilder.CPointType) {
			    if (lmode != SplinePathBuilder.CPointType.CLOSE) {
				xp = lrow.getXP();
				yp = lrow.getYP();
				JViewport vp = scrollPane.getViewport();
				int vpw = vp.getWidth();
				int vph = vp.getHeight();
				int ipx = (int)(Math.round(xp*zoom - vpw/2));
				int ipy = (int)(Math.round(yp*zoom - vph/2));
				if (ipx < 0) ipx = 0;
				if (ipy < 0) ipy = 0;
				vp.setViewPosition(new Point(ipx, ipy));
				scrollPane.repaint();
			    }
			}
		    }
		    panel.repaint();
		}
	    });
	toolMenu.add(menuItem);

	final JMenu filterMenu = new JMenu(localeString("Filters"));
	filterMenu.setMnemonic(vk("VK_Filters"));
	menubar.add(filterMenu);
	ptfilters = new PTFilters(frame, filterMenu, ptmodel);
	menuItem = new JMenuItem(localeString("newFilter"),
				 vk("VK_newFilter"));
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
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			}
		    } while (loop);
		    ptfilters.addFilter(name, e, panel);
		    panel.repaint();
		}
	    });
	filterMenu.add(menuItem);
	menuItem = new JMenuItem(localeString("clearFilter"),
				 vk("VK_clearFilter"));
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
	helpMenu.setMnemonic(vk("VK_Help"));
	menubar.add(helpMenu);
	menuItem = new JMenuItem(localeString("Manual"),
				 vk("VK_Manual"));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    showManual();
		}
	    });
	helpMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("PrintManual"),
				 vk("VK_PrintManual"));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    printManual();
		}
	    });
	helpMenu.add(menuItem);

	menuItem = new JMenuItem(localeString("Browser"),
				 vk("VK_Browser"));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    showManualInBrowser();
		}
	    });
	helpMenu.add(menuItem);
	if (port == 0) {
	    helpMenu.addSeparator();
	    portMenuItem =
		new JMenuItem(localeString("TCPPort"),
			      vk("VK_TCPPort"));
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
		new JMenuItem(localeString("StartWebserver"),
			      vk("VK_StartWebserver"));
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


    private Path2D tmpTransformedPath = null;
    private Line2D paxis1 = null;
    private Line2D paxis2 = null;

    private void adjustForNTP(Point vpp, Path2D oldpath, Point2D oldCM,
			      Path2D newpath)
    {
	if (newpath == null) {
	    // we canceled so go back to the previous state
	    scrollPane.getViewport().setViewPosition(vpp);
	} else {
	    // make sure the new object is visible by shifting the view
	    // so that either the new object's center of mass is at the
	    // center of the view or both the new and old object's
	    // CM is in the view.
	    JViewport vp = scrollPane.getViewport();
	    Point2D newCM = Path2DInfo.centerOfMassOf(newpath);
	    if (newCM == null) {
		double[] ar = getDefaultPrincipalAngleAndRef(newpath);
		newCM = new Point2D.Double(ar[1], ar[2]);
	    }
	    AffineTransform af =(zoom == 1.0)? new AffineTransform():
		AffineTransform.getScaleInstance(zoom, zoom);
	    af.translate(-xrefpoint/scaleFactor, height+yrefpoint/scaleFactor);
	    af.scale(1.0/scaleFactor, -1.0/scaleFactor);
	    Point2D oldCMXP = af.transform(oldCM, null);
	    Point2D newCMXP = af.transform(newCM, null);
	    double x1 = oldCMXP.getX();
	    double y1 = oldCMXP.getY();
	    double x2 = newCMXP.getX();
	    double y2 = newCMXP.getY();
	    int vpx = (int)Math.round(vpp.getX());
	    int vpy = (int)Math.round(vpp.getY());
	    int vpw = vp.getWidth();
	    int vph = vp.getHeight();
	    /*
	    System.out.format("oldCM=(%g, %g), newCM=(%g, %g)\n",
			      x1, y1, x2, y2);
	    System.out.format("vpp=(%d, %d), dim=(%d,%d)\n",
			      vpx, vpy, vpw, vph);
	    */
	    int ix, iy;
	    if ((int)Math.ceil(Math.abs(x2-x1)) < vpw
		&& (int)Math.ceil(Math.abs(y2-y1)) < vph) {
		ix = (int)(Math.round((x1+x2)/2)) - vpw/2;
		iy = (int)(Math.round((y1+y2)/2)) - vph/2;
	    } else {
		ix = (int)(Math.round(x2)) - vpw/2;
		iy = (int)(Math.round(y2)) - vph/2;
	    }
	    if (ix < 0) ix = 0;
	    if (iy < 0) iy = 0;
	    if (ix > (Math.ceil(width*zoom) - vpw)) {
		ix = (int)Math.round(width*zoom) - vpw;
	    }
	    if (iy > (Math.ceil(height*zoom) - vph)) {
		iy = (int)Math.round(height*zoom) - vph;
	    }
	    // System.out.format("(ix,iy) = (%d, %d)\n", ix, iy);
	    vp.setViewPosition(new Point(ix, iy));
	}
    }

    private void onNewTransformedPath(String oldPathName, boolean copyMode) {
	final TransformPane tfpane = new
	    TransformPane(ptmodel, oldPathName, scaleFactor) {
		Point vpp = scrollPane.getViewport().getViewPosition();
		public void accept(){
		    tmpTransformedPath = getNewPath();
		    adjustForNTP(vpp, getOldPath(), getRef(),
				 tmpTransformedPath);
		    panel.repaint();
		}
		public void cancel(){
		    adjustForNTP(vpp, getOldPath(), getRef(), null);
		    tmpTransformedPath = null;
		}
		public void ok() {
		    adjustForNTP(vpp, getOldPath(), getRef(), getNewPath());
		    tmpTransformedPath = null;
		}

		public void doZoomIn() {
		    int lastIndex = zoomIndex;
		    double lastZoom = zoom;
		    if (zoom > zoomTable[zoomTable.length - 1]) {
			// no change
		    } else if (zoom >= zoomTable[zoomIndex]) {
			zoomIndex++;
			if (zoomIndex >= zoomTable.length) {
			    zoomIndex = zoomTable.length - 1;
			}
			zoom = zoomTable[zoomIndex];
		    }
		    if (zoom != lastZoom /*lastIndex != zoomIndex*/) {
			rezoom(zoom, lastZoom);
			if (getStatus()) {
			    accept();
			} else {
			    adjustForNTP(vpp, getOldPath(), getRef(),
					 getOldPath());
			    panel.repaint();
			}
		    }
		}
		public void doZoomOut() {
		    int lastIndex = zoomIndex;
		    double lastZoom = zoom;
		    if (zoom < zoomTable[0]) {
			// no change
		    } else if (zoom == zoomTable[zoomIndex]) {
			zoomIndex--;
			if (zoomIndex < 0) {
			    zoomIndex = 0;
			}
			zoom = zoomTable[zoomIndex];
		    } else {
			// no change to the index.
			zoom = zoomTable[zoomIndex];
		    }
		    if (zoom != lastZoom /*lastIndex != zoomIndex*/) {
			rezoom(zoom, lastZoom);
			if (getStatus()) {
			    accept();
			} else {
			    adjustForNTP(vpp, getOldPath(), getRef(),
					 getOldPath());
			    panel.repaint();
			}
		    }
		}
		protected void setZoomEnabled() {
		    enableIn(zoom < zoomTable[zoomTable.length - 1]);
		    enableOut(zoom > zoomTable[0]);
		    // enableIn(zoomIndex < zoomTable.length - 1);
		    // enableOut(zoomIndex > 0);
		}
	    };
	paxis1 = tfpane.principalAxis1();
	paxis2 = tfpane.principalAxis2();
	// if (paxis1 == null) System.out.println("paxis1 = null");
	panel.repaint();
	// panel.getToolkit().sync();
	String title = copyMode? localeString("newTransformedPath"):
	    localeString("TransformedPath");
	final JDialog dialog = new JDialog(frame,
				    title,
				    true);
	dialog.setLocationRelativeTo(frame);
	dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	dialog.add(tfpane);
	dialog.pack();
	tmpTransformedPath = null;
	dialog.setVisible(true);
	paxis1 = null;
	paxis2 = null;
	tmpTransformedPath = null;
	if (tfpane.getStatus()) {
	    String newPathName = null;
	    String key = null;
	    String link = null;
	    String descr = null;
	    if (copyMode == false) {
		newPathName = oldPathName;
	    } else {
		do {
		    newPathName = (String)JOptionPane.showInputDialog
			(frame, localeString("PleaseEnterNewVariableName"),
			 localeString("ScriptingLanguageVariableName"),
			 JOptionPane.PLAIN_MESSAGE);
		    if (newPathName != null) {
			String[] components  = newPathName.split(";");
			if (components.length > 1) {
			    newPathName  = components[0];
			}
			for (int i = 1; i < components.length; i++) {
			    String s = components[i];
			    String[] term = s.split("\\s*=\\s*");
			    if (term.length != 2) {
				JOptionPane.showMessageDialog
				    (frame, errorMsg("badVarnameFormat", s),
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
				return;
			    }
			    String k = term[0].trim();
			    String v = term[1].trim();
			    if (k.equals("key")) {
				key = v;
			    } else if (k.equals("link")) {
				link = v;
			    } else if (k.equals("descr")) {
				descr = v;
			    } else {
				JOptionPane.showMessageDialog
				    (frame, errorMsg("badVarnameFormat", s),
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
				return;
			    }
			}
			newPathName = newPathName.trim();
		    }
		    if (newPathName == null) {
			panel.repaint();
			return;
		    } else if (newPathName.equals(oldPathName)) {
			copyMode = false;
			break;
		    }
		} while (ptmodel.getVariableNames().contains(newPathName));
	    }
	    int n = ptmodel.getRowCount();
	    AffineTransform af = tfpane.getTransform();
	    boolean reverse = tfpane.requestsReversed();
	    double[] tcoords = new double[4];
	    if (key != null) keys.put(varname, key);
	    if (link != null) links.put(varname, link);
	    if (descr != null) descriptions.put(varname, descr);
	    if (copyMode) {
		int start = ptmodel.findStart(oldPathName);
		if (start == -1) {
		    SwingErrorMessage.display(frame, localeString("errorTitle"),
					      errorMsg("noPath", oldPathName));
		    panel.repaint();
		    return;
		}
		if (reverse) {
		    ptmodel.addRow(new PointTMR(newPathName,
						EPTS.Mode.PATH_START,
						0.0, 0.0, 0.0, 0.0));
		    int ind1 = ptmodel.getRowCount();
		    for (SplinePathBuilder.CPoint cp:
			     ptmodel.getCPoints(start,true, null)) {
			Enum mode = cp.type;
			tcoords[0] = cp.x;
			tcoords[1] = cp.y;
			af.transform(tcoords, 0, tcoords, 2, 1);
			double x = tcoords[2];
			double y = tcoords[3];
			double xp = (x - xrefpoint) / scaleFactor;
			double yp = (y - yrefpoint) / scaleFactor;
			yp = height - yp;
			// add to table.
			ptmodel.addRow(new PointTMR("", mode, x, y, xp, yp));
		    }
		    ptmodel.addRow(new PointTMR("", EPTS.Mode.PATH_END,
						0.0, 0.0, 0.0, 0.0));
		    int ind2 = ptmodel.getRowCount()-1;
		    ptmodel.fireTableChanged(ind1, ind2,
					     PointTableModel.Mode.ADDED);
		} else {
		    for (int index = start; index < n; index++) {
			PointTMR row = ptmodel.getRow(index);
			Enum mode = row.getMode();
			if (mode == EPTS.Mode.PATH_START) {
			    ptmodel.addRow(new PointTMR(newPathName, mode,
							0.0, 0.0, 0.0, 0.0));
			} else if (mode == EPTS.Mode.PATH_END) {
			    ptmodel.addRow(new PointTMR("", mode,
							0.0, 0.0, 0.0, 0.0));
			    break;
			} else {
			    tcoords[0] = row.getX();
			    tcoords[1] = row.getY();
			    af.transform(tcoords, 0, tcoords, 2, 1);
			    double x = tcoords[2];
			    double y = tcoords[3];
			    double xp = (x - xrefpoint) / scaleFactor;
			    double yp = (y - yrefpoint) / scaleFactor;
			    yp = height - yp;
			    // add to table.
			    ptmodel.addRow(new PointTMR("", mode,
							x, y, xp, yp));
			}
		    }
		}
	    } else {
		int start = ptmodel.findStart(oldPathName);
		if (start == -1) {
		    SwingErrorMessage.display(frame, localeString("errorTitle"),
					      errorMsg("noPath", oldPathName));
		    panel.repaint();
		    return;
		}
		if (reverse) {
		    int index = start + 1;
		    int end = ptmodel.findEnd(start);
		    for (SplinePathBuilder.CPoint cp:
			     ptmodel.getCPoints(start,true, null)) {
			tcoords[0] = cp.x;
			tcoords[1] = cp.y;
			af.transform(tcoords, 0, tcoords, 2, 1);
			double x = tcoords[2];
			double y = tcoords[3];
			double xp = (x - xrefpoint) / scaleFactor;
			double yp = (y - yrefpoint) / scaleFactor;
			yp = height - yp;
			PointTMR row = ptmodel.getRow(index);
			row.setMode(cp.type);
			row.setX(x, xp);
			row.setY(y, yp);
			index++;
		    }
		    ptmodel.fireTableChanged(start, end,
					     PointTableModel.Mode.MODIFIED);
		} else {
		    for (int index = start; index < n; index++) {
			PointTMR row = ptmodel.getRow(index);
			Enum ourmode = row.getMode();
			if (ourmode instanceof SplinePathBuilder.CPointType) {
			    tcoords[0] = row.getX();
			    tcoords[1] = row.getY();
			    af.transform(tcoords, 0, tcoords, 2, 1);
			    double x = tcoords[2];
			    double y = tcoords[3];
			    double xp = (x - xrefpoint) / scaleFactor;
			    double yp = (y - yrefpoint) / scaleFactor;
			    yp = height - yp;
			    row.setX(x, xp);
			    row.setY(y, yp);
			} else if (ourmode == EPTS.Mode.PATH_END) {
			    break;
			}
		    }
		}
	    }
	    selectedRow = -1;
	    resetState();
	    if (copyMode) {
		String fmt = localeString("tformNew");
		setModeline(String.format(fmt, newPathName, oldPathName));
	    } else {
		String fmt = localeString("tformExisting");
		setModeline(String.format(fmt, oldPathName));
	    }
	} else {
	    selectedRow = -1;
	    resetState();
	    setModeline("");
	}
	panel.repaint();
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
	    insertArcMenuItem.setEnabled(true);
	    selectedClosedPath = false;
	    deletePathMenuItem.setEnabled(ptmodel.variableNameCount() > 0);
	    addToPathMenuItem.setEnabled(canAddToBezier());
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
	    // addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount()>0);
	    selectedRow = -1;
	    insertArcMenuItem.setEnabled(true);
	    selectedClosedPath = false;
	    addToPathMenuItem.setEnabled(canAddToBezier());
	    deletePathMenuItem.setEnabled(ptmodel.variableNameCount() > 0);
	    offsetPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	    tfMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	    newTFMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
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
		    addToPathMenuItem.setEnabled(canAddToBezier());
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
	    selectedClosedPath = false;
	    insertArcMenuItem.setEnabled(true);
	    addToPathMenuItem.setEnabled(canAddToBezier());
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
	    selectedClosedPath = false;
	    insertArcMenuItem.setEnabled(true);
	    addToPathMenuItem.setEnabled(canAddToBezier());
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
	    TransitionTable.getShiftMenuItem().setEnabled(true);
	    TransitionTable.getGotoMenuItem().setEnabled(true);
	} else {
	    endIndex = ptmodel.getRowCount()-1;
	    ptmodel.deleteRow(endIndex--);
	    selectedRow = -1;
	    selectedClosedPath = false;
	    insertArcMenuItem.setEnabled(true);
	    addToPathMenuItem.setEnabled(canAddToBezier());
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
	    TransitionTable.getShiftMenuItem().setEnabled(true);
	    TransitionTable.getGotoMenuItem().setEnabled(true);
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
		    cancelPathOps();
		    onDelete();
		}
	    });
	popupMenu.add(deleteMenuItem);

	changeMenuItem = new JMenuItem(localeString("ChangePointType"));
	changeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cancelPathOps();
		    onChangeType();
		}
	    });
	popupMenu.add(changeMenuItem);

	insertBeforeMenuItem = new JMenuItem(localeString("InsertBeforePoint"));
	insertBeforeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cancelPathOps();
		    onInsertBefore();
		}
	    });
	popupMenu.add(insertBeforeMenuItem);

	insertAfterMenuItem = new JMenuItem(localeString("InsertAfterPoint"));
	insertAfterMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cancelPathOps();
		    onInsertAfter();
		}
	    });
	popupMenu.add(insertAfterMenuItem);

	breakLoopMenuItem = new JMenuItem(localeString("BreakLoop"));
	breakLoopMenuItem.setEnabled(false);
	breakLoopMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cancelPathOps();
		    onBreakLoop();
		}
	    });
	popupMenu.add(breakLoopMenuItem);

	extendPathMenuItem = new JMenuItem(localeString("ExtendPath"));
	extendPathMenuItem.setEnabled(true);
	extendPathMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cancelPathOps();
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
			deleteMenuItem.setEnabled(false);
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

    /*
    private void rezoom() {
	double lastZoom = zoom;
	zoom = zoomTable[zoomIndex];
	rezoom(zoom, lastZoom);
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
    */

    private void rezoom(double zoom, double oldzoom) {
	JViewport vp = scrollPane.getViewport();
	int vpw = vp.getWidth();
	int vph = vp.getHeight();
	Point p = vp.getViewPosition();
	int x = (int)Math.round((p.x  + vpw/2)/oldzoom);
	int y = (int)Math.round((p.y + vph/2)/oldzoom);
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
	if (zoom <= zoomTable[0]) {
	    zoomInMI.setEnabled(true);
	    zoomOutMI.setEnabled(false);
	} else if (zoom >= zoomTable[zoomTable.length-1]) {
	    zoomInMI.setEnabled(false);
	    zoomOutMI.setEnabled(true);
	} else {
	    zoomInMI.setEnabled(true);
	    zoomOutMI.setEnabled(true);
	}
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
		double lastZoom = zoom;
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
				deletePathMenuItem.setEnabled(true);
				location = String.format("%s = {x: %g, y: %g};",
							 varname, x, y);
			    }
			    break;
			default:
			    throw new UnexpectedExceptionError();
			}
			// Need to fix this up - this is the only relevant
			// case where setModeline changes the text.
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
			    mpMenuItem.setEnabled(true);
			}
			if (ptmodel.pathVariableNameCount() > 0) {
			    rotMenuItem.setEnabled(true);
			    scaleMenuItem.setEnabled(true);
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
			if (hasPathOps()) {
			    cancelPathOps();
			} else {
			    setModeline("");
			}
			selectedRow = -1;
			selectedClosedPath = false;
			insertArcMenuItem.setEnabled(true);
			addToPathMenuItem.setEnabled(canAddToBezier());
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
		    double lastZoom = zoom;
		    switch (e.getKeyCode()) {
		    case KeyEvent.VK_EQUALS:
			zoomIndex++;
			if (zoomIndex >= zoomTable.length) {
			    zoomIndex = zoomTable.length-1;
			}
			break;
		    case KeyEvent.VK_MINUS:
			if (e.isControlDown()) {
			    if (zoom < zoomTable[0]) {
				// nothing to do
			    } else {
				zoomIndex--;
				if (zoomIndex < 0) zoomIndex = 0;
				zoom = zoomTable[zoomIndex];
			    }
			}
			break;
		    case KeyEvent.VK_0:
			if (e.isControlDown()) {
			    zoomIndex = initialZoomIndex;
			    zoom = zoomTable[zoomIndex];
			}
			break;
		    }
		    if (zoom != lastZoom/*lastZoomInd != zoomIndex*/) {
			rezoom(zoom, lastZoom);
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
			if (insertingArc) {
			    insertingArc = false;
			    resetState();
			    panel.setCursor
				(Cursor.getPredefinedCursor
				 (Cursor.DEFAULT_CURSOR));
			    setModeline("");
			    panel.repaint();
			    return;
			}
			if (gotoMode) {
			    endGotoMode();
			    return;
			}
			String frameText = localeString("CancelCurrentOps");
			String frameTitle = localeString("Cancel");
			if (insertRowIndex != -1) {
			    insertRowIndex = -1;
			    selectedRow = -1;
			    selectedClosedPath = false;
			    insertArcMenuItem.setEnabled(true);
			    addToPathMenuItem.setEnabled(canAddToBezier());
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
			    if (moveLocOrPath) {
				ptmodel.moveObject(selectedRow,
						   lastXSelected,
						   lastYSelected,
						   lastXPSelected,
						   lastYPSelected,
						   true);
				cancelPathOps();
			    } else if (rotatePath) {
				double cmxp = (centerOfMass.getX() - xrefpoint)
				    /scaleFactor;
				double cmyp = (centerOfMass.getY() - yrefpoint)
				    / scaleFactor;
				cmyp = height - cmyp;
				Point2D cmp = new Point2D.Double(cmxp, cmyp);
				double xx = rotStart.getX();
				double yy = rotStart.getY();
				double xxp = (xx - xrefpoint) / scaleFactor;
				double yyp = (yy - yrefpoint) / scaleFactor;
				yyp = height - yyp;
				ptmodel.rotateObject(rotRows, centerOfMass, cmp,
						     rotStart, pathStart,
						     xx, yy, xxp, yyp, true);
				cancelPathOps();
			    } else if (scalePath) {
				double cmxp = (centerOfMass.getX() - xrefpoint)
				    /scaleFactor;
				double cmyp = (centerOfMass.getY() - yrefpoint)
				    / scaleFactor;
				cmyp = height - cmyp;
				Point2D cmp = new Point2D.Double(cmxp, cmyp);
				ptmodel.scaleObject(scaleRows, centerOfMass,
						    cmp, paX1, paY1,
						    1.0, 1.0, pathStart, true);
				cancelPathOps();
			    } else {
				ptmodel.changeCoords(selectedRow,
						     lastXSelected,
						     lastYSelected,
						     lastXPSelected,
						     lastYPSelected,
						     true);
			    }
			    selectedRow = -1;
			    selectedClosedPath = false;
			    insertArcMenuItem.setEnabled(true);
			    addToPathMenuItem.setEnabled(canAddToBezier());
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
		    } else if (scalePath && selectedRow != -1) {
		       boolean lock = ((modifiers & KEY_MASK) ==
				       InputEvent.SHIFT_DOWN_MASK);
		       if (scaleLine == null) {
			   scaleInitializedWhenClicked = true;
			   initialX = scaleCMP.getX()*zoom;
			   initialY = scaleCMP.getY()*zoom;
			   scaleLine = new Line2D.Double(initialX, initialY,
							 initialX, initialY);
		       }
			double px = scaleLine.getX2();
			double py = scaleLine.getY2();
			switch (e.getKeyCode()) {
			case KeyEvent.VK_RIGHT:
			    // px += 1.0;
			    px += paX1;
			    py -= paY1;
			    scaleX += 0.01;
			    if (lock) {
				// py -= 1.0;
				px += paX2;
				py -= paY2;
				scaleY += 0.01;
			    }
			    // xp += 1.0/zoom;
			    break;
			case KeyEvent.VK_LEFT:
			    // px -= 1.0;
			    px -= paX1;
			    py += paY1;
			    scaleX -= 0.01;
			    if (lock) {
				// py += 1.0;
				px -= paX2;
				py += paY2;
				scaleY -= 0.01;
			    }
			    // xp -= 1.0/zoom;
			    break;
			case KeyEvent.VK_UP:
			    // py -= 1.0;
			    px += paX2;
			    py -= paY2;
			    scaleY += 0.01;
			    if (lock) {
				// px += 1.0;
				px += paX1;
				py -= paY1;
				scaleX += 0.01;
			    }
			    // yp -= 1.0/zoom;
			    break;
			case KeyEvent.VK_DOWN:
			    // py += 1.0;
			    px -= paX2;
			    py += paY2;
			    scaleY -= 0.01;
			    if (lock) {
				// px -= 1.0;
				px -= paX1;
				py += paY1;
				scaleX -= 0.01;
			    }
			    // yp += 1.0/zoom;
			    break;
			default:
			    return;
			}
			scaleInitializedWhenClicked = true;
			scaleLine =
			    new Line2D.Double(initialX, initialY, px, py);
			ptmodel.scaleObject(scaleRows, centerOfMass,
					    scaleCMP, paX1, paY1,
					    scaleX, scaleY,
					    pathStart, true);
			panel.repaint();
		    } else if (selectedRow != -1) {
			PointTMR row = ptmodel.getRow(selectedRow);
			if (row == null) {
			    selectedRow = -1;
			    selectedClosedPath = false;
			    insertArcMenuItem.setEnabled(true);
			    addToPathMenuItem.setEnabled(canAddToBezier());
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
			    if (moveLocOrPath) {
				ptmodel.moveObject(selectedRow, x, y,
						   xp, yp, true);
				// cancelPathOps() is called when either
				// the ESCAPE key is pressed or when the
				// ENTER key is typed, as the user may
				// press the arrow keys multiple times.
			    } else if (rotatePath) {
				double cmxp = (centerOfMass.getX() - xrefpoint)
				    /scaleFactor;
				double cmyp = (centerOfMass.getY() - yrefpoint)
				    / scaleFactor;
				cmyp = height - cmyp;
				Point2D cmp = new Point2D.Double(cmxp, cmyp);
				ptmodel.rotateObject(rotRows, centerOfMass, cmp,
						     rotStart, pathStart,
						     x, y, xp, yp, false);
			    } else {
				ptmodel.changeCoords(selectedRow, x, y,
						     xp, yp, true);
			    }
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
	insertingArc = false;
	endGotoMode();
	insertArcMenuItem.setEnabled(true);
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
	    TransitionTable.getGotoMenuItem().setEnabled(false);
	    TransitionTable.getLocMenuItem().setEnabled(false);
	    TransitionTable.getShiftMenuItem().setEnabled(false);
	    saveMenuItem.setEnabled(true);
	    saveAsMenuItem.setEnabled(true);
	    ttable = null;
	    panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	locState = false;
	distState = 0;
	if (ptmodel.getRowCount() > 0) {
	    makeCurrentMenuItem.setEnabled(true);
	    mpMenuItem.setEnabled(true);
	}
	if (ptmodel.pathVariableNameCount() > 0) {
	    rotMenuItem.setEnabled(true);
	    scaleMenuItem.setEnabled(true);
	}
	for (TransitionTable.Pair item:
		 TransitionTable.getMenuItemsWithState()) {
	    item.getMenuItem().setEnabled(false);
	}
    }


    Cursor savedCursorDist = null;
    boolean distInGCS = false;
    double x1 = 0.0, y1 = 0.0;
    public void measureDistance(boolean gcs) {
	resetState();
	distInGCS = gcs;
	makeCurrentMenuItem.setEnabled(false);
	mpMenuItem.setEnabled(false);
	rotMenuItem.setEnabled(false);
	scaleMenuItem.setEnabled(false);
	setModeline(localeString("LeftClickFirstPoint"));
	distState = 2;
	// makeCurrentMenuItem.setEnabled(false);
	locState = false;
	savedCursorDist = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    boolean locState = false;

    public void createLocation() {
	resetState();
	makeCurrentMenuItem.setEnabled(false);
	mpMenuItem.setEnabled(false);
	rotMenuItem.setEnabled(false);
	scaleMenuItem.setEnabled(false);
	setModeline(localeString("LeftClickCreate"));
	TransitionTable.getGotoMenuItem().setEnabled(true);
	TransitionTable.getLocMenuItem().setEnabled(true);
	TransitionTable.getShiftMenuItem().setEnabled(true);
	distState = 0; 
	locState = true;
	savedCursorDist = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	TransitionTable.getShiftMenuItem().setEnabled(false);

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
	String[] components  = varname.split(";");
	String key = null;
	String link = null;
	String descr = null;
	if (components.length > 1) {
	    varname  = components[0];
	}
	for (int i = 1; i < components.length; i++) {
	    String s = components[i];
	    String[] term = s.split("\\s*=\\s*");
	    if (term.length != 2) {
		JOptionPane.showMessageDialog
		    (frame, errorMsg("badVarnameFormat", s),
		     localeString("errorTitle"),
		     JOptionPane.ERROR_MESSAGE);
		return;
	    }
	    String k = term[0].trim();
	    String v = term[1].trim();
	    if (k.equals("key")) {
		key = v;
	    } else if (k.equals("link")) {
		link = v;
	    } else if (k.equals("descr")) {
		descr = v;
	    } else {
		JOptionPane.showMessageDialog
		    (frame, errorMsg("badVarnameFormat", s),
		     localeString("errorTitle"),
		     JOptionPane.ERROR_MESSAGE);
		return;
	    }
	}
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
	mpMenuItem.setEnabled(false);
	rotMenuItem.setEnabled(false);
	scaleMenuItem.setEnabled(false);
	if (key != null) keys.put(varname, key);
	if (link != null) links.put(varname, link);
	if (descr != null) descriptions.put(varname, descr);
	ptmodel.addRow(varname, EPTS.Mode.PATH_START, 0.0, 0.0, 0.0, 0.0);
	savedCursorPath = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	createPathFinalAction();
    }

    private void createPathFinalAction() {
	ttable = new TransitionTable();
	setModeline(SplinePathBuilder.CPointType.MOVE_TO);
	nextState = SplinePathBuilder.CPointType.MOVE_TO;
	TransitionTable.getGotoMenuItem().setEnabled(true);
	TransitionTable.getPipeMenuItem().setEnabled(true);
	TransitionTable.getLocMenuItem().setEnabled(true);
	TransitionTable.getShiftMenuItem().setEnabled(true);
	// addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	addToPathMenuItem.setEnabled(canAddToBezier());
	deletePathMenuItem.setEnabled(ptmodel.variableNameCount() > 0);
	offsetPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	tfMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	newTFMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	saveMenuItem.setEnabled(false);
	saveAsMenuItem.setEnabled(false);
	TransitionTable.getShiftMenuItem().setEnabled(false);
    }

    boolean mouseMoved = false;
    int selectedRow = -1;
    boolean gotoMode = false;	// true to choose an existing point

    private void startGotoMode() {
	if (!gotoMode) {
	    modeline.setText(" " + localeString("gotoModeline"));
	}
	gotoMode = true;
    }

    private void endGotoMode() {
	if (gotoMode) {
	    gotoMode = false;
	    modeline.setText(prevModeline);
	}
    }


    // boolean selectedRowClick = false;
    double lastXSelected = 0.0;
    double lastYSelected = 0.0;
    double lastXPSelected = 0.0;
    double lastYPSelected = 0.0;
    double xpoff = 0.0;
    double ypoff = 0.0;
    // If we click a control point on a path and the  path is closed,
    // and we are not editing a path, this should be set to true.
    boolean selectedClosedPath = false;
    // pressed the alt key or in drag-image mode with the mouse
    // pressed while a point was not selected or a curve or point
    // was not being created.
    boolean altPressed = false;
    // actually pressed the ALT key instead of just being in
    // drag image mode with other constraints satisfied
    boolean altReallyPressed = false;
    boolean toggledAltD = true;

    boolean canAddToBezier() {
	boolean hop = ptmodel.hasOpenPaths();
	/*
	System.out.println("selected Row = " + selectedRow
			   + ", selectedClosedPath = " + selectedClosedPath
			   + ", hop = " + hop);
	*/
	if (hop == false) return false;
	if (selectedRow == -1) {
	    return hop;
	} else {
	    return selectedClosedPath == false;
	}
    }


    MouseInputAdapter mia = new MouseInputAdapter() {
	    public void mouseClicked(MouseEvent e) {
		Point p = panel.getMousePosition();
		if (p != null && e.getButton() == MouseEvent.BUTTON1) {
		    int modifiers = e.getModifiersEx();
		    if ((modifiers & KEY_MASK) == InputEvent.CTRL_DOWN_MASK) {
			return;
		    }
		    if (insertingArc) {
			int ind = ptmodel.findRowIA(p.x/zoom, p.y/zoom, zoom);
			if (ind == -1) return;
			selectedRow = ind;
			insertArcAction();
			return;
		    }
		    if (gotoMode) {
			int ind = ptmodel.findRowXPYP(p.x/zoom, p.y/zoom, zoom);
			if (ind == -1) {
			    Toolkit.getDefaultToolkit().beep();
			    return;
			}
			PointTMR row = ptmodel.getRow(ind);
			long lx = Math.round(row.getXP()*zoom);
			long ly = Math.round(row.getYP()*zoom);
			if (lx < Integer.MIN_VALUE || lx > Integer.MAX_VALUE
			    || ly < Integer.MIN_VALUE
			    || ly > Integer.MAX_VALUE) {
			    JOptionPane.showMessageDialog
				(frame, localeString("PointRange"),
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			    return;
			}
			p = new Point((int)lx, (int)ly);
			if (selectedRow != -1) {
			    PointTMR srow = ptmodel.getRow(selectedRow);
			    Enum<?> smode = srow.getMode();
			    if (smode == EPTS.Mode.LOCATION
				|| smode instanceof
				SplinePathBuilder.CPointType) {
				// Special case - we are moving a location
				// or path point to the position of an
				// existing point.
				endGotoMode();
				double xp =(p.x/zoom);
				double yp = (p.y/zoom);
				double x = xp;
				double y = height - yp;
				x *= scaleFactor;
				y *= scaleFactor;
				x += xrefpoint;
				y += yrefpoint;
				if (moveLocOrPath) {
				    ptmodel.moveObject(selectedRow,
						       x, y, xp, yp, true);
				} else {
				    srow.setX(x, xp);
				    srow.setY(y, yp);
				}
				resetState();
				panel.repaint();
				return;
			    }
			}
			endGotoMode();
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
			selectedClosedPath = false;
			addToPathMenuItem.setEnabled(canAddToBezier());
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
				deletePathMenuItem.setEnabled(true);
				TransitionTable.getLocMenuItem()
				    .setEnabled(false);
				TransitionTable.getGotoMenuItem()
				    .setEnabled(false);
				TransitionTable.getPipeMenuItem()
				    .setEnabled(false);
				TransitionTable.getShiftMenuItem()
				    .setEnabled(false);
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
			    mpMenuItem.setEnabled(true);
			}
			if (ptmodel.pathVariableNameCount() > 0) {
			    rotMenuItem.setEnabled(true);
			    scaleMenuItem.setEnabled(true);
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
			try {
			    ttable.nextState(ns);
			} catch (Exception ee) {
			    JOptionPane.showMessageDialog
				(frame, ee.getMessage(),
				 localeString("errorTitle"),
				 JOptionPane.ERROR_MESSAGE);
			    ptmodel.deleteRow(ptmodel.getRowCount()-1);
			    addToPathMenuItem.setEnabled(canAddToBezier());
			    resetState();
			    return;
			}
		    } else {
			double xp =(p.x/zoom);
			double yp = (p.y/zoom);
			selectedRow = ptmodel.findRowXPYP(xp, yp, zoom);
			if (selectedRow != -1) {
			    if (ptmodel.findRowIA(xp, yp, zoom) == -1) {
				insertArcMenuItem.setEnabled(false);
			    }
			    PointTMR row = ptmodel.getRow(selectedRow);
			    if (rotatePath) {
				if (row.getMode() == EPTS.Mode.LOCATION) {
				    // ignore case where the selected row
				    // turns out to be a location, not part of
				    // a path: we can't meaninfully rotate a
				    // single point.
				    selectedRow = -1;
				    selectedClosedPath = false;
				    insertArcMenuItem.setEnabled(true);
				    addToPathMenuItem.setEnabled
					(canAddToBezier());
				    return;
				}
			    }
			    lastXSelected = row.getX();
			    lastYSelected = row.getY();
			    lastXPSelected = row.getXP();
			    lastYPSelected = row.getYP();
			    xpoff = lastXPSelected - xp;
			    ypoff = lastYPSelected - yp;
			    int startIndex = ptmodel.findStart(selectedRow);
			    int endIndex = ptmodel.findEnd(selectedRow);
			    int penultimateIndex = endIndex - 1;
			    String vn = ptmodel.getVariableName
				(ptmodel.findStart(selectedRow));
			    if (endIndex > startIndex &&
				ptmodel.getRowMode(endIndex) ==
				EPTS.Mode.PATH_END) {
				selectedClosedPath =
				    (ptmodel.getRowMode(penultimateIndex) ==
				     SplinePathBuilder.CPointType.CLOSE);
				addToPathMenuItem.setEnabled(canAddToBezier());
			    }
			    if (hasPathOps()) {
				setModeline(pathOpsModelineString());
				if (moveLocOrPath) {
				    TransitionTable.getGotoMenuItem()
					.setEnabled(true);
				    TransitionTable.getLocMenuItem()
					.setEnabled(true);
				    TransitionTable.getShiftMenuItem()
					.setEnabled(true);
				} else if (rotatePath) {
				    // will not be called if the point
				    // is a location instead of a point on
				    // a path
				    if (rotPathSetup() == false) {
					selectedRow = -1;
					selectedClosedPath = false;
					insertArcMenuItem.setEnabled(true);
					addToPathMenuItem.setEnabled
					    (canAddToBezier());
					setModeline("Rotate: select a point "
						    + "(not the center of "
						    + "mass) on a path; "
						    + "then drag to rotate");
				    }
				} else if (scalePath) {
				    if (scalePathSetup() == false) {
					selectedRow = -1;
					selectedClosedPath = false;
					insertArcMenuItem.setEnabled(true);
					cancelPathOps();
				    } else {
					/*
					double cmxp =
					    (centerOfMass.getX() - xrefpoint)
					    /scaleFactor;
					double cmyp =
					    (centerOfMass.getY() - yrefpoint)
					    / scaleFactor;
					cmyp = height - cmyp;
					scaleCMP =
					    new Point2D.Double(cmxp, cmyp);
					*/
					initialX = p.x;
					initialY = p.y;
					scaleInitializedWhenClicked = true;
					// scaleX = 1.0;
					// scaleY = 1.0;
				    }
				}
			    } else {
				setModeline(String.format
					    (localeString("SelectedPoint"),
					     selectedRow, vn, row.getMode()));
				TransitionTable.getGotoMenuItem()
				    .setEnabled(true);
				TransitionTable.getLocMenuItem()
				    .setEnabled(true);
				TransitionTable.getShiftMenuItem()
				    .setEnabled(true);
			    }
			} else {
			    selectedClosedPath = false;
			    insertArcMenuItem.setEnabled(true);
			    if (hasPathOps()) {
				cancelPathOps();
			    } else {
				setModeline("");
			    }
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
			} else {
			    selectedClosedPath = false;
			}
			panel.repaint();
		    } else if (toggledAltD || altReallyPressed) {
			altPressed = true;
			JViewport vp = scrollPane.getViewport();
			Point p = vp.getViewPosition();
			e.translatePoint(-p.x, -p.y);
			vp.dispatchEvent(e);
		    } else {
			if (insertingArc) return;
			Point p = panel.getMousePosition();
			double xp =(p.x/zoom);
			double yp = (p.y/zoom);
			if (selectedRow != ptmodel.findRowXPYP(xp, yp, zoom)
			    && scalePath == false) {
			    // this is done in case we select a point,
			    // intend to click a location between points to
			    // deselected, but inadvertently drag the mouse.
			    // If scalePath is true, that is an exception to
			    // this rule.
			    if (selectedRow != -1 && nextState == null) {
				TransitionTable
				    .getLocMenuItem().setEnabled(false);
				TransitionTable
				    .getShiftMenuItem().setEnabled(false);
			    }
			    selectedRow = -1;
			    selectedClosedPath = false;
			    insertArcMenuItem.setEnabled(true);
			    addToPathMenuItem.setEnabled(canAddToBezier());
			    setModeline("");
			}
		    }
		}
		checkPopupMenu(e);
	    }


	    boolean pointDragged = false;
	    public void mouseReleased(MouseEvent e) {
		if (mouseButton == MouseEvent.BUTTON1) {
		    if (insertingArc) return;
		    altReallyPressed = false;
		    if (altPressed) {
			JViewport vp = scrollPane.getViewport();
			Point p = vp.getViewPosition();
			e.translatePoint(-p.x, -p.y);
			vp.dispatchEvent(e);
		    }
		    if (pointDragged && selectedRow != -1) {
			cancelPathOps();
			ptmodel.fireRowsChanged(selectedRow);
		    }
		    if (selectedRow != -1 && nextState == null) {
			TransitionTable.getGotoMenuItem().setEnabled(false);
			TransitionTable.getPipeMenuItem().setEnabled(false);
			TransitionTable.getLocMenuItem().setEnabled(false);
			TransitionTable.getShiftMenuItem().setEnabled(false);
		    }
		    if (gotoMode && selectedRow != -1) {
			 if (nextState == null && locState == false) {
			     // Case were we are moving a location to overlay
			     // an existing point.
			     panel.repaint();
			     return;
			 }
		    }
		    selectedRow = -1;
		    selectedClosedPath = false;
		    insertArcMenuItem.setEnabled(true);
		    addToPathMenuItem.setEnabled(canAddToBezier());
		    // selectedRowClick = false;
		    if (nextState == null) {
			if (locState == false) {
			    setModeline("");
			}
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
			if (moveLocOrPath) {
			    ptmodel.moveObject(selectedRow, x, y,
					       xp, yp, false);
			    // don't call cancelPathOps here
			    // because this is called repeated when a
			    // point is dragged.  We do it instead when
			    // the mouse button is released.
			} else if (rotatePath) {
			    if (centerOfMass == null) {
				JOptionPane.showMessageDialog
				    (panel, localeString("noCenterOfMass"),
				     localeString("errorTitle"),
				     JOptionPane.ERROR_MESSAGE);
			    }
			    double cmxp = (centerOfMass.getX() - xrefpoint)
				/scaleFactor;
			    double cmyp = (centerOfMass.getY() - yrefpoint)
				/ scaleFactor;
			    cmyp = height - cmyp;
			    Point2D cmp = new Point2D.Double(cmxp, cmyp);
			    ptmodel.rotateObject(rotRows, centerOfMass, cmp,
						 rotStart, pathStart,
						 x, y, xp, yp, false);
			} else if (scalePath) {
			    if (pointDragged || scaleInitializedWhenClicked) {
				double deltaX = p.x - initialX;
				// user space has increasing Y coords go down.
				double deltaY = initialY - p.y;
				double delta1 = deltaX*paX1 + deltaY*paY1;
				double delta2 = deltaX*paX2 + deltaY*paY2;
				int modifiers = e.getModifiersEx();
				boolean lock =
				    (modifiers & KEY_MASK)
				    == InputEvent.SHIFT_DOWN_MASK;
				boolean one = (modifiers & KEY_MASK)
				    == (InputEvent.CTRL_DOWN_MASK
					| InputEvent.SHIFT_DOWN_MASK);
				if (lock) {
				    if (Math.abs(delta1) < Math.abs(delta2)) {
					scaleY = (delta2)/100 + 1.0;
					scaleX = scaleY;
				    } else {
					scaleX = (delta1)/100 + 1.0;
					scaleY = scaleX;
				    }

				} else if (one) {
				    double dx = p.x - initialX;
				    double dy = p.y - initialY;
				    if (Math.abs(delta1) < Math.abs(delta2)) {
					scaleY = (delta2)/100 + 1.0;
					scaleX = 1.0;
				    } else {
					scaleX = (delta1)/100 + 1.0;
					scaleY = 1.0;
				    }
				} else {
				    scaleX = (delta1)/100 + 1.0;
				    scaleY = (delta2)/100 + 1.0;
				}
				scaleLine =
				    new Line2D.Double(initialX, initialY,
						      p.x, p.y);
				ptmodel.scaleObject(scaleRows, centerOfMass,
						    scaleCMP, paX1, paY1,
						    scaleX, scaleY,
						    pathStart, false);
			    } else {
				if (centerOfMass == null) {
				    // just in case we didn't call this
				    // previously.
				    scalePathSetup();
				}
				/*
				double cmxp = (centerOfMass.getX() - xrefpoint)
				    /scaleFactor;
				double cmyp = (centerOfMass.getY() - yrefpoint)
				    / scaleFactor;
				cmyp = height - cmyp;
				scaleCMP = new Point2D.Double(cmxp, cmyp);
				*/
				initialX = p.x;
				initialY = p.y;
			    }
			} else {
			    ptmodel.changeCoords(selectedRow, x, y,
						 xp, yp, false);
			}
			pointDragged = true;
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
			if (row.isSelectable()) {
			    g2d2.draw(new Line2D.Double(prevx, prevy, x, y));
			    g2d.draw(new Line2D.Double(prevx, prevy, x, y));
			}
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
			if (row.isSelectable()) {
			    g2d2.draw(new Line2D.Double(prevx, prevy, x, y));
			    g2d.draw(new Line2D.Double(prevx, prevy, x, y));
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
					if (paxis1 != null) {
					    g2d.setColor(Color.ORANGE);
					    AffineTransform af2 =
						(zoom == 1.0)?
						new AffineTransform():
						AffineTransform.getScaleInstance
						(zoom, zoom);

					    af2.translate
						(-xrefpoint/scaleFactor,
						 height+yrefpoint/scaleFactor);
					    af2.scale(1.0/scaleFactor,
						      -1.0/scaleFactor);
					    Path2D paxis1Line = new
						Path2D.Double
						(paxis1, af2);
					    Path2D paxis2Line = new
						Path2D.Double
						(paxis2, af2);
					    g2d2.draw(paxis1Line);
					    g2d.draw(paxis1Line);
					    g2d2.draw(paxis2Line);
					    g2d.draw(paxis2Line);
					    if (tmpTransformedPath != null) {
						g2d.setColor(Color.RED);
						Path2D tmpPath = new
						    Path2D.Double
						    (tmpTransformedPath, af2);
						g2d2.draw(tmpPath);
						g2d.draw(tmpPath);
					    }
					    if (scaleLine != null) {
						g2d.setColor(Color.RED);
						g2d2.draw(scaleLine);
						g2d.draw(scaleLine);
					    }
					} else if (tmpTransformedPath != null) {
					    AffineTransform af2 =
						(zoom == 1.0)?
						new AffineTransform():
						AffineTransform
						.getScaleInstance
						(zoom, zoom);

					    af2.translate
						(-xrefpoint/scaleFactor,
						 height+yrefpoint/scaleFactor);
					    af2.scale(1.0/scaleFactor,
						      -1.0/scaleFactor);
					    g2d.setColor(Color.RED);
					    Path2D tmpPath = new
						Path2D.Double
						(tmpTransformedPath, af2);
					    g2d2.draw(tmpPath);
					    g2d.draw(tmpPath);
					}
					if (tmpArcPath != null) {
					    g2d.setColor(Color.RED);
					    g2d2.draw(tmpArcPath);
					    g2d.draw(tmpArcPath);
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

    void setupGCSConfigPane(EPTSParser parser) {
	configGCSPane.savedUnitIndex
	    = parser.getUnitIndex();
	configGCSPane.savedUnitIndexRP
	    = parser.getUnitIndexRP();
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
	    Image image = parser.getImage();
	    if (image != null /*parser.imageURIExists()*/) {
		boolean hasURI = parser.imageURIExists();
		URI uri = hasURI? parser.getImageURI(): null;
		// Image image = parser.getImage();
		imageURI = uri;
		init(image, (uri != null), null);
		// now restore state.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    setupGCSConfigPane(parser);
			    for (PointTMR row: parser.getRows()) {
				ptmodel.addRow(row);
			    }
			    // addToPathMenuItem.setEnabled
			    // (ptmodel.pathVariableNameCount() > 0);
			    addToPathMenuItem.setEnabled(canAddToBezier());
			    if (ptmodel.getRowCount() > 0) {
				makeCurrentMenuItem.setEnabled(true);
				mpMenuItem.setEnabled(true);
			    }
			    if (ptmodel.pathVariableNameCount() > 0) {
				rotMenuItem.setEnabled(true);
				scaleMenuItem.setEnabled(true);
			    }
			    deletePathMenuItem.setEnabled
				(ptmodel.variableNameCount() > 0);
			    offsetPathMenuItem.setEnabled
				(ptmodel.pathVariableNameCount() > 0);
			    tfMenuItem.setEnabled
				(ptmodel.pathVariableNameCount() > 0);
			    newTFMenuItem.setEnabled
				(ptmodel.pathVariableNameCount() > 0);
			    setupFilters(parser);
			    parser.configureOffsetPane();
			}
		    });
	    }
	}
	// this gets set to true whenever the table is modified, which
	// will happen when loading an existing table. We use invoke
	// later because when a table-change event is fired, that is
	// queued and we have to set needSave to false after all those
	// events are processed.
	SwingUtilities.invokeLater(() -> {
		needSave = false;
	    });
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
	if (se != null && targetList.size() > 0) {
	    this.languageName = se.getLanguageName();
	    this.animationName = se.getAnimationName();
	    scriptMode = true;
	    // shouldSaveScripts = (parser == null) || (image == null);
	    shouldSaveScripts = true;
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
			// addToPathMenuItem.setEnabled
			//    (ptmodel.pathVariableNameCount() > 0);
			addToPathMenuItem.setEnabled(canAddToBezier());
			if (ptmodel.getRowCount() > 0) {
			    makeCurrentMenuItem.setEnabled(true);
			    mpMenuItem.setEnabled(true);
			}
			if (ptmodel.pathVariableNameCount() > 0) {
			    rotMenuItem.setEnabled(true);
			    scaleMenuItem.setEnabled(true);
			}
			deletePathMenuItem.setEnabled
			    (ptmodel.variableNameCount() > 0);
			offsetPathMenuItem.setEnabled
			    (ptmodel.pathVariableNameCount() > 0);
			tfMenuItem.setEnabled
			    (ptmodel.pathVariableNameCount() > 0);
			newTFMenuItem.setEnabled
			    (ptmodel.pathVariableNameCount() > 0);
		    }
		    if (parser != null) {
			setupFilters(parser);
			parser.configureOffsetPane();
		    }
		    // this gets set to true whenever the table is
		    // modified, which will happen when loading an
		    // existing table. We use invoke later because
		    // when a table-change event is fired, that is
		    // queued and we have to set needSave to false
		    // after all those events are processed.
		    SwingUtilities.invokeLater( () -> {
			    needSave = false;
			});
		}
	    });
    }
}
