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
import java.util.LinkedList;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.bzdev.ejws.*;
import org.bzdev.ejws.maps.*;
import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.CPointType;
import org.bzdev.graphs.Graph;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.imageio.BlockingImageObserver;
import org.bzdev.math.Functions;
import org.bzdev.net.WebEncoder;
import org.bzdev.util.CopyUtilities;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;
import org.bzdev.swing.ErrorMessage;
import org.bzdev.swing.HtmlWithTocPane;

public class EPTSWindow {
    JFrame frame;
    JLabel modeline;
    JScrollPane scrollPane;
    JPanel panel;
    BufferedImage bi;
    Graphics2D g2d;
    Graph graph;
    ConfigGCSPane configGCSPane;
    enum LocationFormat {
	COORD, EXPRESSION, OBJECT, STATEMENT
    }
    LocationFormat locationFormat = LocationFormat.STATEMENT;
    String varname = null;
    ArrayList<String> targetList = null;
    // true if targetList contains a file name of an image we laoded
    boolean imageFileNameSeen = false;
    
    PointTableModel ptmodel;
    JTable ptable;
    JFrame tableFrame;

    int port;

    public void save(File f) throws IOException {
	TemplateProcessor.KeyMap keymap = new TemplateProcessor.KeyMap();
	configGCSPane.saveState();
	keymap.put("hasImageFile", imageFileNameSeen? "true": "false");
	TemplateProcessor.KeyMapList tlist =
	    new TemplateProcessor.KeyMapList();
	for (String arg: targetList) {
	    TemplateProcessor.KeyMap map = new TemplateProcessor.KeyMap();
	    File af = new File(arg);
	    File fparent = f.getCanonicalFile().getParentFile();
	    File afparent = af.getCanonicalFile().getParentFile();
	    while (afparent != null) {
		if (afparent.equals(fparent)) break;
		afparent = afparent.getParentFile();
	    }
	    if (afparent == null) {
		arg = af.getCanonicalFile().toURI().toString();
	    } else {
		// we use relative URLs if the image file is in a
		// subdirectory of the directory containing the saved
		// state.
		arg = fparent.toURI()
		    .relativize(af.getCanonicalFile().toURI()).toString();
	    }
	    map.put("arg", arg);
	    tlist.add(map);
	}
	keymap.put("arglist", tlist);
	keymap.put("unitIndex",
		   String.format("%d", configGCSPane.savedUnitIndex));
	keymap.put("refPointIndex",
		   String.format("%d", configGCSPane.savedRefPointIndex));
	keymap.put("userSpaceDistance", configGCSPane.savedUsDistString);
	keymap.put("gcsDistance", configGCSPane.savedGcsDistString);
	keymap.put("width", String.format("%d", width));
	keymap.put("height", String.format("%d", height));
	TemplateProcessor.KeyMap km = ptmodel.getKeyMap();
	if (km != null && km.size() > 0) {
	    keymap.put("table", km);
	}
	TemplateProcessor tp = new TemplateProcessor(keymap);
	OutputStream os = new FileOutputStream(f);
	Writer writer = new OutputStreamWriter(os, "UTF-8");
	tp.processSystemResource("save.tpl", "UTF-8", writer);
    }

    void setupTable(JComponent pane) {
	ptmodel = new PointTableModel(pane);
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
	    Container pane = manualFrame.getContentPane();
	    manualPane = new HtmlWithTocPane();
	    manualFrame.setSize(920, 700);
	    manualFrame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			manualFrame.setVisible(false);
		    }
		});
	    URL url = ClassLoader.getSystemClassLoader()
		.getResource("manual/manual.xml");
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
		.getResource("manual/manual.html");
	    if (url != null) {
		JEditorPane pane = new JEditorPane();
		pane.setPage(url);
		EditorKit ekit = pane.getEditorKit();
		if (ekit instanceof HTMLEditorKit) {
		    HTMLEditorKit hkit = (HTMLEditorKit)ekit;
		    StyleSheet stylesheet = hkit.getStyleSheet();
		    StyleSheet oursheet = new StyleSheet();
		    StringBuilder sb = new StringBuilder(512);
		    CopyUtilities.copyResource("manual/manual.css",
					       sb,
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
    private void showManualInBrowser() {
	try {
	    if (ews == null) {
		ews = new EmbeddedWebServer(port, 5, 2, false);
		
		ews.add("/", ResourceWebMap.class, "manual/",
			null, true, false, true);
		manualURI =
		    new URL("http://localhost:"
			    + port +"/manual.html").toURI();
		ews.start();
	    }		
	    Desktop.getDesktop().browse(manualURI);
	    
	} catch (Exception e) {
	    setModeline("Browser cannot be opened");
	    e.printStackTrace();
	}
    }

    double xorigin = 0.0;
    double yorigin = 0.0;
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
		setModeline("Left click to start path (GCS); ESC to abort");
		break;
	    case SPLINE:
		setModeline("Left click for next spline "
			    + "point; ESC to abort");
		break;
	    case CONTROL:
		setModeline("Left click for next control "
			    + "point; ESC to abort");
		break;
	    case SEG_END:
		setModeline("Left click for a segment's "
			    + "last point; ESC to abort");
		break;
	    case CLOSE:
		setModeline("Path Complete");
		break;
	    }
	} else if (ns instanceof EPTS.Mode) {
	    EPTS.Mode emode = (EPTS.Mode) ns;
	    switch (emode) {
	    case LOCATION:
		setModeline("Location Computed");
		break;
	    case PATH_START:
		setModeline("Starting a B\u00e9zier Path");
		break;
	    case PATH_END:
		setModeline("Path Complete");
		break;
	    }
	}
    }

    private boolean cleanupPartialPath(boolean force) {
	PointTMR lastrow = ptmodel.getLastRow();
	if (lastrow != null
	    && lastrow.getMode() instanceof
	    SplinePathBuilder.CPointType) {
	    // check and remove the last partial path.
	    int result = (force)? JOptionPane.OK_OPTION:
		JOptionPane.showConfirmDialog(frame,
					      "Undo current path segments?",
					      "Cancel Current Path",
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
	return true;
    }

    JMenuItem saveMenuItem;
    JMenuItem addToPathMenuItem; // for Tools menu.
    File savedFile = null;

    private void setMenus(JFrame frame, double w, double h) {
	JMenuBar menubar = new JMenuBar();
	JMenuItem menuItem;
	JMenu fileMenu = new JMenu("File");
	fileMenu.setMnemonic(KeyEvent.VK_F);
	menubar.add(fileMenu);
	menuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    System.exit(0);
		}
	    });
	fileMenu.add(menuItem);
	saveMenuItem = new JMenuItem("Save", KeyEvent.VK_S);
	saveMenuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
	saveMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			if (savedFile == null) {
			    File cdir = new File(System.getProperty("user.dir"))
				.getCanonicalFile();
			    JFileChooser chooser = new JFileChooser(cdir);
			    FileNameExtensionFilter filter =
				new FileNameExtensionFilter("EPTS State",
							    "epts");
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
			save(savedFile);
		    } catch (Exception ee) {
		    }
		}
	    });
	fileMenu.add(saveMenuItem);
	
	menuItem = new JMenuItem("Configure GCS", KeyEvent.VK_C);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    configGCSPane.saveState();
		     int status = JOptionPane.showConfirmDialog
			(frame, configGCSPane,
			 "Configure Graph Coordiate Space (GCS)",
			 JOptionPane.OK_CANCEL_OPTION);
		     if (status == JOptionPane.OK_OPTION) {
			 scaleFactor = configGCSPane.getScaleFactor();
			 xorigin = configGCSPane.getXOrigin();
			 yorigin = configGCSPane.getYOrigin();
			 switch(configGCSPane.getRefPointName()) {
			 case CENTER:
			     xorigin += width*scaleFactor / 2;
			     yorigin += height*scaleFactor / 2;
			     break;
			 case CENTER_LEFT:
			     yorigin += height*scaleFactor / 2;
			     break;
			 case CENTER_RIGHT:
			     xorigin += width*scaleFactor;
			     yorigin += height*scaleFactor / 2;
			     break;
			 case LOWER_CENTER:
			     xorigin += width*scaleFactor / 2;
			     break;
			 case LOWER_LEFT:
			     break;
			 case LOWER_RIGHT:
			     xorigin += width*scaleFactor;
			     break;
			 case UPPER_CENTER:
			     xorigin += width*scaleFactor / 2;
			     yorigin += height*scaleFactor;
			     break;
			 case UPPER_LEFT:
			     yorigin += height*scaleFactor;
			     break;
			 case UPPER_RIGHT:
			     xorigin += width*scaleFactor;
			     yorigin += height*scaleFactor;
			     break;
			 }
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
				     x -= xorigin;
				     y -= yorigin;
				     row.setX(x, xp);
				     row.setY(y, yp);
				 }
			     }
			     ptmodel.fireXYChanged();
			 }
		     } else {
			 // User canceled the changes, so we restore
			 // the state to what it was before the
			 // dialog box was opened.
			 configGCSPane.restoreState();
		     }
		}
	    });
	fileMenu.add(menuItem);

	menuItem = new JMenuItem("Print Table", KeyEvent.VK_P);
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

	JMenu editMenu = new JMenu("Edit");
	editMenu.setMnemonic(KeyEvent.VK_E);
	menubar.add(editMenu);

	menuItem = new JMenuItem("Undo Point Insertion", KeyEvent.VK_U);
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
				// which calls a menu item's doClick
				// method to set the nextState variable
				// for the radio-button menu item cases.
				ttable.setState(ptmodel, n);
			    }
			}
		    }
		    if (nextState != null) {
			saveMenuItem.setEnabled(false);
		    }
		}
	    });
	editMenu.add(menuItem);

	menuItem = new JMenuItem("Append to a B\u00e9zier Path", KeyEvent.VK_A);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
	addToPathMenuItem = menuItem;
	menuItem.setEnabled(false);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // select a path.
		    Set<String> vnameSet = ptmodel.getPathVariableNames();
		    if (vnameSet.isEmpty()) return;
		    String[] vnames =
			vnameSet.toArray(new String[vnameSet.size()]);
		    String vname = (String)JOptionPane.showInputDialog
			(frame, "Select a path to extend:",
			 "Select Path",
			 JOptionPane.PLAIN_MESSAGE, null,
			 vnames, vnames[0]);
		    if (vname == null || vname.length() == 0) return;
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
			// doClick method, and that sets the
			// nextState variable.
			ttable.nextState(EPTS.Mode.PATH_END);
			ptmodel.addRow("", EPTS.Mode.PATH_END,
				       0.0, 0.0, 0.0, 0.0);
		    }
		    resetState();
		    int index = ptmodel.findStart(vname);
		    if (index != -1) index++;
		    selectedRow = index;
		    onExtendPath();
		}
	    });
	editMenu.add(menuItem);

	menuItem = new JMenuItem("Copy Table as ECMAScript", KeyEvent.VK_E);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			StringWriter writer = new
			    StringWriter(48*ptmodel.getRowCount());
			TemplateProcessor tp =
			    new TemplateProcessor(ptmodel.getKeyMap());
			tp.processSystemResource("ECMAScript.tpl",
						 "UTF-8", writer);
			Clipboard cb = panel.getToolkit().getSystemClipboard();
			StringSelection selection =
			    new StringSelection(writer.toString());
			cb.setContents(selection, selection);
		    } catch (IOException ee) {
			System.err.println("ECMA export to clipboard failed");
			ee.printStackTrace(System.err);
			System.exit(1);
		    }
		}
	    });
	editMenu.add(menuItem);
	
	editMenu.addSeparator();
	editMenu.add(new JLabel("Location Format"));
	ButtonGroup bg = new ButtonGroup();
	menuItem = new JRadioButtonMenuItem("location as (X,Y)");
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.COORD));
	bg.add(menuItem);
	editMenu.add(menuItem);
	menuItem = new JRadioButtonMenuItem("location as x: X, y: Y");
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.EXPRESSION));
	bg.add(menuItem);
	editMenu.add(menuItem);
	menuItem = new JRadioButtonMenuItem("location as {x: X, y: Y}");
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.OBJECT));
	bg.add(menuItem);
	editMenu.add(menuItem);
	menuItem = new JRadioButtonMenuItem("location as V = {x: X, y: Y};");
	menuItem.addActionListener
	    (createLMIActionListener(LocationFormat.STATEMENT));
	menuItem.setSelected(true);
	bg.add(menuItem);
	editMenu.add(menuItem);

	JMenu zoomMenu = new JMenu("Zoom");
	zoomMenu.setMnemonic(KeyEvent.VK_Z);
	menubar.add(zoomMenu);

	menuItem = new JMenuItem("Reset");
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

	menuItem = new JMenuItem("Zoom In");
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

	menuItem = new JMenuItem("Zoom Out");
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

	JMenu measureMenu = new JMenu("Measure Distance");
	measureMenu.setMnemonic(KeyEvent.VK_M);
	menubar.add(measureMenu);
	menuItem = new JMenuItem("Image-Space Distance",
				 KeyEvent.VK_I);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    measureDistance(false);
		}
	    });
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
	measureMenu.add(menuItem);

	menuItem = new JMenuItem("GCS Distance",
				 KeyEvent.VK_G);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_G, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    measureDistance(true);
		}
	    });
	measureMenu.add(menuItem);

	JMenu toolMenu = new JMenu("Tools");
	toolMenu.setMnemonic(KeyEvent.VK_T);
	menubar.add(toolMenu);
 
	menuItem = new JMenuItem("Show Point Table", KeyEvent.VK_S);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    tableFrame.setVisible(true);
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem("Create a Point", KeyEvent.VK_P);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (!cleanupPartialPath(false)) return;
		    if (locationFormat == LocationFormat.STATEMENT) {
			varname = JOptionPane.showInputDialog
			    (frame, "Please enter a variable name:",
			     "Scripting-language Variable Name",
			     JOptionPane.PLAIN_MESSAGE);
			if (varname == null) return;
		    }
		    createLocation();
		}
	    });
	toolMenu.add(menuItem);

	menuItem = new JMenuItem("Create a B\u00e9zier Path", KeyEvent.VK_B);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (!cleanupPartialPath(false)) return;
		    createPath();
		}
	    });
	toolMenu.add(menuItem);


	toolMenu.addSeparator();
	toolMenu.add(new JLabel("B\u00e9zier Path Options"));
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
		    toolMenu.add(new
				 JLabel("Options to end a B\u00e9zier Path"));
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
				setModeline("Loop Complete");

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
				// doClick method, and that sets the
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

	JMenu helpMenu = new JMenu("Help");
	helpMenu.setMnemonic(KeyEvent.VK_H);
	menubar.add(helpMenu);
	menuItem = new JMenuItem("Manual", KeyEvent.VK_M);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    showManual();
		}
	    });
	helpMenu.add(menuItem);

	menuItem = new JMenuItem("Print Manual", KeyEvent.VK_P);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    printManual();
		}
	    });
	helpMenu.add(menuItem);

	menuItem = new JMenuItem("Browser", KeyEvent.VK_B);
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    showManualInBrowser();
		}
	    });
	helpMenu.add(menuItem);

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
	    setModeline(String.format("Left click to insert before the "
				      + "selected point (type = %s); "
				      + "ESC to abort",
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
	    setModeline(String.format("Left click to insert after the "
				      + "selected point (type = %s); "
				      + "ESC to abort",
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
		    (frame, "Choose new point type.",
		     "Change-Type Options",
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
	if (endIndex == n) return;
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
	    // which calls a menu item's doClick
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
	}
	if (nextState != null) saveMenuItem.setEnabled(false);
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
	deleteMenuItem = new JMenuItem("Delete Point");
	deleteMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onDelete();
		}
	    });
	popupMenu.add(deleteMenuItem);

	changeMenuItem = new JMenuItem("Change Point Type");
	changeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onChangeType();
		}
	    });
	popupMenu.add(changeMenuItem);

	insertBeforeMenuItem = new JMenuItem("Insert Before Point");
	insertBeforeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onInsertBefore();
		}
	    });
	popupMenu.add(insertBeforeMenuItem);

	insertAfterMenuItem = new JMenuItem("Insert After Point");
	insertAfterMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onInsertAfter();
		}
	    });
	popupMenu.add(insertAfterMenuItem);

	breakLoopMenuItem = new JMenuItem("Break loop");
	breakLoopMenuItem.setEnabled(false);
	breakLoopMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onBreakLoop();
		}
	    });
	popupMenu.add(breakLoopMenuItem);

	extendPathMenuItem = new JMenuItem("Extend path");
	extendPathMenuItem.setEnabled(true);
	extendPathMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    onExtendPath();
		}
	    });
	popupMenu.add(extendPathMenuItem);

	editingMenu = new JPopupMenu();
	toSegEndMenuItem = new JMenuItem("mode -> SEG_END");
	toSegEndMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ptmodel.setLastRowMode
			(SplinePathBuilder.CPointType.SEG_END);
		    int lastPointIndex = ptmodel.getRowCount()-1;
		    nextState = SplinePathBuilder.CPointType.SEG_END;
		    setModeline(nextState);
		    // This calls ttable.nextState,
		    // which calls a menu item's doClick
		    // method to set the nextState variable
		    // for the radio-button menu item cases.
		    ttable.setState(ptmodel, lastPointIndex);
		}
	    });
	editingMenu.add(toSegEndMenuItem);
	toSplineMenuItem = new JMenuItem("mode -> SPLINE");
	toSplineMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ptmodel.setLastRowMode(SplinePathBuilder.CPointType.SPLINE);
		    int lastPointIndex = ptmodel.getRowCount()-1;
		    nextState = SplinePathBuilder.CPointType.SPLINE;
		    setModeline(nextState);
		    // This calls ttable.nextState,
		    // which calls a menu item's doClick
		    // method to set the nextState variable
		    // for the radio-button menu item cases.
		    ttable.setState(ptmodel, lastPointIndex);
		}
	    });
	editingMenu.add(toSplineMenuItem);
	toControlMenuItem = new JMenuItem("mode -> CONTROL");
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
		    // which calls a menu item's doClick
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
			x -= xorigin;
			y = y - yorigin;
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
			setModeline(String.format("Location ---  %s "
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
						 ("Distance = %s "
						  + "(%s; copied to clipboard)",
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
			    double xp =(p.x/zoom);
			    double yp = (p.y/zoom);
			    double x = xp;
			    double y = height - yp;
			    x *= scaleFactor;
			    y *= scaleFactor;
			    x -= xorigin;
			    y -= yorigin;
			    ptmodel.addRow("",ns, x, y, xp, yp);
			    // This can call a radio-button menu item's
			    // doClick method, and that sets the
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
			String frameText = "Cancel current Operation?";
			String frameTitle = "Cancel";
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
			    frameText = "Undo current path segments?";
			    frameTitle = "Cancel Current Path";
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
			Enum rmode = row.getMode();
			if (rmode instanceof SplinePathBuilder.CPointType
			    && rmode != SplinePathBuilder.CPointType.CLOSE) {
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
			    x -= xorigin;
			    y -= yorigin;
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
			    System.err.println
				("cannot move mouse using keyboard");
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
	    saveMenuItem.setEnabled(true);
	    ttable = null;
	}

	locState = false;
	distState = 0;
    }


    Cursor savedCursorDist = null;
    boolean distInGCS = false;
    double x1 = 0.0, y1 = 0.0;
    public void measureDistance(boolean gcs) {
	resetState();
	distInGCS = gcs;
	setModeline("Left click the first point; ESC to abort");
	distState = 2;
	locState = false;
	savedCursorDist = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    boolean locState = false;

    public void createLocation() {
	resetState();
	setModeline("Left click to create a point (GCS) ; ESC to abort");
	distState = 0; 
	locState = true;
	savedCursorDist = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    TransitionTable ttable = null;
    Enum nextState = null;
    Cursor savedCursorPath = null;

    private void createPath() {
	resetState();
	nextState = SplinePathBuilder.CPointType.MOVE_TO;
	varname = JOptionPane.showInputDialog
	    (frame, "Please enter a variable name:",
	     "Scripting-language Variable Name",
	     JOptionPane.PLAIN_MESSAGE);
	if (varname == null) return;
	ptmodel.addRow(varname, EPTS.Mode.PATH_START, 0.0, 0.0, 0.0, 0.0);
	savedCursorPath = panel.getCursor();
	panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	createPathFinalAction();
    }

    private void createPathFinalAction() {
	ttable = new TransitionTable();
	setModeline(SplinePathBuilder.CPointType.MOVE_TO);
	nextState = SplinePathBuilder.CPointType.MOVE_TO;
	addToPathMenuItem.setEnabled(ptmodel.pathVariableNameCount() > 0);
	saveMenuItem.setEnabled(false);
    }

    boolean mouseMoved = false;
    int selectedRow = -1;
    boolean selectedRowClick = false;
    double lastXSelected = 0.0;
    double lastYSelected = 0.0;
    double lastXPSelected = 0.0;
    double lastYPSelected = 0.0;
    double xpoff = 0.0;
    double ypoff = 0.0;
    boolean altPressed = false;

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
			x -= xorigin;
			y -= yorigin;
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
			x -= xorigin;
			y -= yorigin;
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
		    } else if (distState == 2) {
			// g2d.setXORMode(Color.BLACK);
			// g2d.setStroke(new BasicStroke(1.0F));
			mouseTracked = true;
			x1 = p.x / zoom;
			y1 = p.y / zoom;
			setModeline("Left click the second point; "
				    + "ESC to abort");
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
				    ("Distance = %s (%s; copied to clipboard)",
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
			x -= xorigin;
			y -= yorigin;
			ptmodel.addRow("",ns, x, y, xp, yp);
			// This can call a radio-button menu item's
			// doClick method, and that sets the
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
			    setModeline(String.format
					("Selected point (index %d), type %s",
					 selectedRow, row.getMode()));
			    
			}
			selectedRowClick = true;
			panel.repaint();
		    }
		}
	    }
	    int mouseButton = -1;
	    public void mousePressed(MouseEvent e) {
		mouseButton = e.getButton();
		if (mouseButton == MouseEvent.BUTTON1) {
		    int modifiers = e.getModifiersEx();
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
			    setModeline(String.format
					("Selected point (index %d), type %s",
					 selectedRow, row.getMode()));
			}
			panel.repaint();
		    } else if ((modifiers & KEY_MASK)
			       == InputEvent.ALT_DOWN_MASK) {
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
			    selectedRow = -1;
			    setModeline("");
			}
		    }
		}
		checkPopupMenu(e);
	    }

	    boolean pointDragged = false;
	    public void mouseReleased(MouseEvent e) {
		if (mouseButton != e.getButton()) {
		    System.err.println("mouseButton =  " + mouseButton
				       + ", expecting " + e.getButton());
		}
		if (mouseButton == MouseEvent.BUTTON1) {
		    if (altPressed) {
			JViewport vp = scrollPane.getViewport();
			Point p = vp.getViewPosition();
			e.translatePoint(-p.x, -p.y);
			vp.dispatchEvent(e);
		    } else {
			if (pointDragged && selectedRow != -1) {
			    ptmodel.fireRowChanged(selectedRow);
			}
			selectedRow = -1;
			selectedRowClick = false;
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
		    if (selectedRow != -1) {
			Point p = panel.getMousePosition();
			if (p == null) return;
			double xp =(p.x/zoom) + xpoff;
			double yp = (p.y/zoom) + ypoff;
			double x = xp;
			double y = height - yp;
			x *= scaleFactor;
			y *= scaleFactor;
			x -= xorigin;
			y -= yorigin;
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
		if (altPressed) {
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
    double w = radius2/Math.sqrt(2.0);
    double hw = w/2;
    double lw = w*1.3;
    double lhw = hw*1.3;

    private void drawRows(PointTableModel ptmodel, Graphics2D g2d) {
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
	    double x = zoom*row.getXP();
	    double y = zoom*row.getYP();
	    Enum mode = row.getMode();
	    if (mode instanceof EPTS.Mode) {
		EPTS.Mode emode = (EPTS.Mode)mode;
		switch(emode) {
		case LOCATION:
		    g2d.fill(new Ellipse2D.Double
			     (x-radius, y-radius, radius2, radius2));
		    if (index == selectedRow) {
			g2d.draw(new Ellipse2D.Double
				 (x-lradius, y-lradius, lradius2, lradius2));
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
		    g2d.fill(new Ellipse2D.Double
			     (x-radius, y-radius, radius2, radius2));
		    if (index == selectedRow) {
			g2d.draw(new Ellipse2D.Double
				 (x-lradius, y-lradius, lradius2, lradius2));
		    }
		    pb.append(new SplinePathBuilder.CPoint(smode, x, y));
		    prevx = x;
		    prevy = y;
		    break;
		case SPLINE:
		    g2d.fill(new Ellipse2D.Double
			     (x-radius, y-radius, radius2, radius2));
		    if (index == selectedRow) {
			g2d.draw(new Ellipse2D.Double
				 (x-lradius, y-lradius, lradius2, lradius2));
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
		    g2d.fill(new Ellipse2D.Double
			     (x-radius, y-radius, radius2, radius2));
		    if (index == selectedRow) {
			g2d.draw(new Ellipse2D.Double
				 (x-lradius, y-lradius, lradius2, lradius2));
		    }
		    if (lastMode == SplinePathBuilder.CPointType.CONTROL) {
			g2d.draw(new Line2D.Double(prevx, prevy, x, y));
		    }
		    pb.append(new SplinePathBuilder.CPoint(smode, x, y));
		    prevx = x;
		    prevy = y;
		    break;
		case CONTROL:
		    g2d.fill(new Rectangle2D.Double(x-hw, y-hw, w, w));
		    if (index == selectedRow) {
			g2d.draw(new Rectangle2D.Double
				 (x-lhw, y-lhw, lw, lw));
		    }
		    if (lastMode == SplinePathBuilder.CPointType.SEG_END
			|| lastMode == SplinePathBuilder.CPointType.MOVE_TO) {
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
	g2d.draw(pb.getPath());
    }

    public EPTSWindow(Image image, int port, ArrayList<String> targetList)
	throws IOException, InterruptedException
    {
	this.targetList = targetList;
	imageFileNameSeen = true;
	width = image.getWidth(null);
	height = image.getHeight(null);
	this.port = port;
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
	
	SwingUtilities.invokeLater(new  Runnable () {
		public void run() {
		    panel = new JPanel() {
			    public void paintComponent(Graphics g) {
				super.paintComponent(g);
				AffineTransform af =
				    AffineTransform.getScaleInstance
				    (zoom, zoom);
				if (g instanceof Graphics2D) {
				    Graphics2D g2d = (Graphics2D)g;
				    g2d.setPaintMode();
				    g2d.drawImage(bi, af, null);
				    Stroke savedStroke = g2d.getStroke();
				    Color savedColor = g2d.getColor();
				    try {
					g2d.setStroke(new BasicStroke(1.5F));
					g2d.setColor(Color.WHITE);
					g2d.setXORMode(Color.BLACK);
					if (ptmodel.getRowCount() > 0) {
					    drawRows(ptmodel, g2d);
					}
					if (mouseTracked) {
					    g2d.draw(new Line2D.Double
						     (startx*zoom, starty*zoom,
						      lastx*zoom, lasty*zoom));
					}
				    } finally {
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
		    frame.addKeyListener(kba);
		    panel.addMouseListener(mia);
		    panel.addMouseMotionListener(mia);
		    vport.addMouseListener(vpmia);
		    vport.addMouseMotionListener(vpmia);
		    scrollPane.setOpaque(true);
		    container.add(scrollPane, BorderLayout.CENTER);
		    frame.setContentPane(container);
		    frame.pack();
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    configGCSPane = new ConfigGCSPane();
		    frame.setVisible(true);
		}
	    });
    }
}
