package org.bzdev.epts;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.*;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.bzdev.graphs.Colors;
import org.bzdev.graphs.Graph;
import org.bzdev.graphs.RefPointName;
import org.bzdev.imageio.ImageMimeInfo;
import org.bzdev.io.ZipDocFile;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.net.URLClassLoaderOps;
import org.bzdev.net.URLPathParser;
import org.bzdev.obnaming.misc.BasicStrokeParm;
import org.bzdev.obnaming.misc.BasicStrokeParm.Cap;
import org.bzdev.obnaming.misc.BasicStrokeParm.Join;
import org.bzdev.scripting.Scripting;
import org.bzdev.swing.SwingErrorMessage;
import org.bzdev.swing.SimpleConsole;
import org.bzdev.swing.WholeNumbTextField;
// import org.bzdev.util.CopyUtilities;
import org.bzdev.util.SafeFormatter;
import org.bzdev.util.TemplateProcessor;

public class EPTS {

    public enum Mode {
	LOCATION, PATH_START, PATH_END
    }

    static final String pathSeparator = System.getProperty("path.separator");

    // resource bundle for messages used by exceptions and errors
    static ResourceBundle exbundle =
	ResourceBundle.getBundle("org.bzdev.epts.EPTS");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }

    static String localeString(String key) {
	return exbundle.getString(key);
    }

    private static final String JAVACMD = "java";
    private static String javacmd = JAVACMD;

    private static final String dotDotDot = "...";
    private static final String dotDotDotSep = dotDotDot + File.separator;
    private static final String tildeSep = "~" + File.separator;
    private static String ourCodebase;
    // used to set ourCodebaseDir, which is declared to be final.
    private static String ourCodebaseDir2;

    static {
	try {
	    File f = (new File(EPTS.class.getProtectionDomain()
			       .getCodeSource().getLocation().toURI()))
		.getCanonicalFile();
	    // The path to the EPTS jar file
	    ourCodebase = f.getCanonicalPath();
	    f = (new File(Scripting.class.getProtectionDomain()
			       .getCodeSource().getLocation().toURI()))
		.getCanonicalFile();
	    // The path to the libbzdev.jar file with symbolic links
	    // resolved.
	    ourCodebaseDir2 = f.getParentFile().getCanonicalPath();
	    /*
	    f = (new File(EPTS.class.getProtectionDomain()
			       .getCodeSource().getLocation().toURI()))
		.getCanonicalFile();
	    ourCodebase2 = f.getCanonicalPath();
	    ourCodebaseDir2 = f.getParentFile().getCanonicalPath();
	    */

	} catch (Exception e) {
	    System.err.println(errorMsg("missingOwnCodebase"));
	    System.exit(1);
	}
    }
    static final String ourCodebaseDir = ourCodebaseDir2;


    private static Properties defs = new Properties();
    private static boolean propertyNotAllowed(String name) {
	if (System.getProperty(name) != null) return true;
	if (name.equals("java.system.class.loader")
	    || name.equals("java.security.manager")
	    || name.equals("scrunner.sysconf")
	    || name.equals("scrunner.usrconf")) {
	    return true;
	}
	return false;
    }
    private static String languageName = null;
    private static String getExtension(String pathname) throws Exception {
	File file = new File(pathname);
	if (!file.isFile()) {
	    throw new Exception(errorMsg("notNormal", pathname));
	}
	if (!file.canRead()) {
	    throw new Exception(errorMsg("notReadable", pathname));
	}

	String name = file.getName();
	int index = name.lastIndexOf('.');
	if (index++ == 0 || index == name.length()) {
	    return null;
	}
	return name.substring(index);
    }


    private static LinkedList<String> codebase = new LinkedList<>();
    private static HashSet<String>codebaseSet = new HashSet<>();

    private static LinkedList<String> classpath = new LinkedList<>();
    private static HashSet<String>classpathSet = new HashSet<>();

    private static LinkedList<String> addedModules = new LinkedList<>();


    public static List<String> getCodebase() {
	return Collections.unmodifiableList(codebase);
    }

    public static List<String> getClasspath() {
	return Collections.unmodifiableList(classpath);
    }

    public static List<String> getAddedModules() {
	/*
	System.out.println("in getAddedModules(), size = "
			   + addedModules.size());
	*/
	return Collections.unmodifiableList(addedModules);
    }

    private static StringBuilder sbmp = new StringBuilder();
    private static StringBuilder sbmod = new StringBuilder();
    private static Set<String>modSet = new HashSet<>();
    private static Map<String,Boolean> urlMap = new HashMap<>();

    private static StringBuilder sbcp = new StringBuilder();
    /*
    static {
	sbcp.append(ourCodebase);
    }
    */
    // static HashSet<String> sbcpAppended = new HashSet<>();

    static boolean guiMode = false;

    private static void displayError(String string) {
	if (guiMode) {
	    SwingErrorMessage.displayFormat((Component)null,
					    localeString("eptsErrorTitle"),
					    "%s", string);
	} else {
	    if (string.startsWith("epts:")) {
		System.err.println(string);
	    } else {
		System.err.println("epts: " + string);
	    }
	}
    }

    static void extendCodebase(String codebase) {
	extendCodebase(codebase, true);
    }


    static void extendCodebase(String codebase, boolean modulePath)  {
	try {
	    extendCodebase(codebase, System.err, modulePath);
	} catch (Exception e) {
	    System.exit(1);
	}
    }
    static void extendCodebase(String codebase, Appendable err,
			       boolean modulePath)
	throws Exception
    {
	try {
	    URL[] urls = URLPathParser.getURLs(null, codebase,
					       ourCodebaseDir,
					       err);
	    for (URL url: urls) {
		String fname;
		if (url.getProtocol().equals("file")) {
		    File f = new File(url.toURI());
		    if (!f.canRead()) {
			throw new IOException
			    (errorMsg("noFile", url.toString()));
		    } else if (!f.isDirectory() && !f.isFile()) {
			throw new IOException
			    (errorMsg("noFile", url.toString()));
		    }
		    fname = f.getCanonicalPath();
		    url = f.getCanonicalFile().toURI().toURL();
		    if (urlMap.containsKey(url.toString())) {
			if (urlMap.get(url.toString()) != modulePath) {
			    String msg = errorMsg("doubleUse", url.toString());
			    System.err.println(msg);
			    System.exit(1);
			}
			continue;
		    }
		} else {
		    if (urlMap.containsKey(url.toString())) {
			if (urlMap.get(url.toString()) != modulePath) {
			    String msg = errorMsg("doubleUse", url.toString());
			    System.err.println(msg);
			    System.exit(1);
			}
			continue;
		    }
		    File tmp = File.createTempFile("scrunner", "jar");
		    tmp.deleteOnExit();
		    OutputStream os = new FileOutputStream(tmp);
		    InputStream is = url.openConnection().getInputStream();
		    // CopyUtilities.copyStream(is, os);
		    is.transferTo(os);
		    os.close();
		    is.close();
		    fname = tmp.getCanonicalPath();
		}
		urlMap.put(url.toString(), modulePath);
		if (modulePath) {
		    if (sbmp.length() > 0) {
			sbmp.append(pathSeparator);
		    }
		    sbmp.append(fname);
		} else {
		    if (sbcp.length() > 0) {
			sbcp.append(pathSeparator);
		    }
		    sbcp.append(fname);
		}
		/*
		if (!sbcpAppended.contains(codebase)) {
		    if (sbcp.length() > 0) {
			sbcp.append(pathSeparator);
		    }
		    sbcp.append(fname);
		    sbcpAppended.add(codebase);
		}
		*/
	    }
	    // URLClassLoaderOps.addURLs(urls);
	} catch (Exception e) {
	    err.append
		(errorMsg("codebaseError", codebase, e.getMessage()) + "\n");
	    // System.exit(1);
	    throw e;
	}
    }
    
    private static boolean classpathExtended = false;

    private static String MODULE_NAME_RE =
	"^([\\p{L}_$][\\p{L}\\p{N}_$]*)([.][\\p{L}_$][\\p{L}\\p{N}_$]*)*$";

    private static void readConfigFiles(String languageName, String fileName) {
	File file = new File (fileName);
	if (!file.exists()) {
	    return;
	}
	if (!file.isFile()) {
	    displayError(errorMsg("notConfigFile", fileName));

	    System.exit(1);
	}

	if (!file.canRead()) {
	    displayError(errorMsg("configFileNotReadable", fileName));
	    System.exit(1);
	}
	try {
	    LineNumberReader reader = 
		new LineNumberReader(new FileReader(file));
	    try {
		int mode = 0;
		boolean langsearch = true;
		boolean langsection = false;
		boolean error = false;
		boolean modulePath = false;
		for (String line = reader.readLine(); line != null;
		     line = reader.readLine()) {
		    line = line.trim();
		    if (line.length() == 0) continue;
		    if (line.startsWith("#")) continue;
		    if (line.equals("%end")) { 
			mode = 0;
			continue;
		    } else if (line.equals("%defs")) {
			mode = 1;
			langsearch = true;
			langsection = false;
			continue;
		    } else if (line.equals("%classpath.components")) {
			mode = 2;
			langsearch = true;
			langsection = false;
			modulePath = false;
			continue;
		    } else if (line.equals("%modulepath.components")) {
			mode = 2;
			langsearch = true;
			langsection = false;
			modulePath = true;
			continue;
		    } else if (line.equals("%modules")) {
			mode = 3;
			langsearch = true;
			langsection = false;
			continue;
		    } else if (line.startsWith("%java")) {
			if (line.length() < 6) {
			    int lno = reader.getLineNumber();
			    String msg = errorMsg("directive", fileName, lno);
			    displayError(msg);
			    System.exit(1);
			}
			char ch = line.charAt(5);
			if (!Character.isSpaceChar(ch)) {
			    int lno = reader.getLineNumber();
			    String msg = errorMsg("directive", fileName, lno);
			    displayError(msg);
			    System.exit(1);
			}
			String cmd = line.substring(6).trim();
			if (cmd.length() > 0) {
			    javacmd = cmd;
			}
			mode = 0;
			continue;
		    }
		    switch (mode) {
		    case 1:
			if (line.startsWith("%lang")) {
			    String language = line.substring(5).trim();
			    
			    if (languageName == null) {
				mode = 0;
				langsection = false;
			    } else if (languageName.equals(language)) {
				langsection = true;
			    } else  {
				if (langsection) mode = 0;
				langsection = false;
			    }
			    langsearch = false;
			    continue;
			} else if (line.startsWith("%")) {
			    int lno = reader.getLineNumber();
			    String msg =
				errorMsg("endExpected", fileName, lno);
			    displayError(msg);
			    error = true;
			}
			if (langsearch || langsection) {
			    String[] parts = line.split("\\s*=\\s*", 2);
			    if (parts.length != 2) {
				int lno = reader.getLineNumber();
				String msg =
				    errorMsg("syntax", fileName, lno);
				displayError(msg);
				error = true;
			    } else {
				String name = parts[0];
				if (propertyNotAllowed(name) &&
				    !name.equals("java.ext.dirs")) {
				    // do not override standard Java properties
				    int lno = reader.getLineNumber();
				    String msg =
					errorMsg("propname",fileName,lno,name);
				    displayError(msg);
				    error = true;
				    continue;
				}
				String value = parts[1];
				if (name.equals("java.ext.dirs")) {
				    StringBuilder nv = new StringBuilder();
				    int cnt = 0;
				    for (String val:
					     value.split
					     (Pattern.quote(File
							    .pathSeparator))) {
					if (val.startsWith("~/")) {
					    val =
						System.getProperty("user.home")
						+ val.substring(1);
					} else if (val.equals("~")) {
					    val =
						System.getProperty("user.home");
					} else if (val.startsWith("~~")) {
					    val = val.substring(1);
					}
					nv.append(val);
					if ((cnt++) > 0) {
					    nv.append(File.pathSeparator);
					}
				    }
				    value = nv.toString();
				} else {
				    if (value.startsWith("~/")) {
					value = System.getProperty("user.home")
					    + value.substring(1);
				    } else if (value.equals("~")) {
					value = System.getProperty("user.home");
				    } else if (value.startsWith("~~")) {
					value = value.substring(1);
				    }
				}
				defs.setProperty(name, value);
			    }
			}
			break;
		    case 2: // classpath components
			if (line.startsWith("%lang")) {
			    String language = line.substring(5).trim();
			    
			    if (languageName == null) {
				mode = 0;
				langsection = false;
			    } else if (languageName.equals(language)) {
				langsection = true;
			    } else  {
				if (langsection) mode = 0;
				langsection = false;
			    }
			    langsearch = false;
			    continue;
			} else if (line.startsWith("%")) {
			    int lno = reader.getLineNumber();
			    String msg =
				errorMsg("endExpected", fileName, lno);
			    displayError(msg);
			    error = true;
			    continue;
			}
			if (langsearch || langsection) {
			    if (line.startsWith(tildeSep)) {
				line = System.getProperty("user.home")
				    + line.substring(1);
			    } else if (line.equals("~")) {
				line = System.getProperty("user.home");
			    } else if (line.startsWith("~~")) {
				line = line.substring(1);
			    } else if (line.equals(dotDotDot)) {
				line = ourCodebaseDir;
			    } else if (line.startsWith(dotDotDotSep)) {
				// replace the "..." with a directory name
				line = ourCodebaseDir
				    + File.separator + line.substring(4);
			    }
			    try {
				URL url;
				if (line.startsWith("file:")) {
				    url = new URL(line);
				    File f = new File(url.toURI());
				    line = f.getCanonicalPath();
				} else {
				    url = (new File(line)).toURI().toURL();
				}
				extendCodebase(url.toString(), System.err,
					       modulePath);
				classpathExtended = true;
				/*
				URLClassLoaderOps.addURL(url);
				if (!sbcpAppended.contains(line)) {
				    sbcp.append(File.pathSeparator);
				    sbcp.append(line);
				    sbcpAppended.add(line);
				}
				*/
			    } catch (Exception e) {
				int lno = reader.getLineNumber();
				String msg =
				    errorMsg("urlParse", fileName, lno);
				displayError(msg);
				System.exit(1);
			    }
			}
			break;
		    case 3: // modules (for use with Java's --add-modules)
			if (line.startsWith("%lang")) {
			    String language = line.substring(5).trim();
			    if (languageName == null) {
				mode = 0 /*3*/;
				langsection = false;
			    } else if (languageName.equals(language)) {
				langsection = true;
			    } else  {
				if (langsection) mode = 0 /*3*/;
				langsection = false;
			    }
			    langsearch = false;
			    continue;
			} else if (line.startsWith("%")) {
			    int lno = reader.getLineNumber();
			    String msg =
				errorMsg("endExpected", fileName, lno);
			    System.err.println(msg);
			    error = true;
			    continue;
			} else if (!line.matches(MODULE_NAME_RE)) {
			    int lno = reader.getLineNumber();
			    String msg =
				errorMsg("badModuleName", fileName, lno, line);
			    System.err.println("msg");
			    error = true;
			    continue;
			}
			if (modSet.contains(line) == false) {
			    if (sbmod.length() > 0) {
				sbmod.append(",");
			    }
			    sbmod.append(line);
			    modSet.add(line);
			}
			break;
		    }
		}
	    } catch (Exception e) {
		int lno = reader.getLineNumber();
		String msg =
		    errorMsg("exceptionMsg", fileName, lno, e.getMessage());
		displayError(msg);
		System.exit(1);
	    }
	    reader.close();
	} catch (Exception er) {
	    displayError(errorMsg("scException", er.getMessage()));
	    System.exit(1);
	}
    }

    private static String sysconf = null;
    private static String usrconf = null;

    private static void readConfigFiles(String languageName) {
	if (sysconf != null) {
	    readConfigFiles(languageName, sysconf);
	}
	if (usrconf != null) {
	    readConfigFiles(languageName, usrconf);
	}
    }

    private static String defaultSysConfigFile() {
	String fsep = System.getProperty("file.separator");
	if (fsep.equals("/")) {
	    // Unix file separator. Try to find it in /etc
	    // or /etc/opt ; return null otherwise as the
	    // location is unknown.
	    Path p;
	    try {
		p = Paths.get(EPTS.class.getProtectionDomain()
			      .getCodeSource().getLocation().toURI());
	    } catch(Exception e) {
		return null;
	    }
	    if (p.startsWith("/opt")) {
		File cf = new File("/etc/opt/bzdev/scrunner.conf");
		if (cf.canRead()) {
		    return "/etc/opt/bzdev/scrunner.conf";
		}
	    } else if (p.startsWith("/usr/local")) {
		File cf = new File("/usr/local/etc/bzdev/scrunner.conf");
		if (cf.canRead()) {
		    return "/usr/local/etc/bzdev/scrunner.conf";
		}
	    } else {
		File cf = new File("/etc/bzdev/scrunner.conf");
		if (cf.canRead()) {
		    return "/etc/bzdev/scrunner.conf";
		}
	    }
	    return null;
	} else {
	    // for Windows or other operating systems that do
	    // not look like Unix, assume the configuration file
	    // is in the same directory as the jar files.
	    try {
		File fp = new File
		    (EPTS.class.getProtectionDomain()
		     .getCodeSource().getLocation().toURI());
		fp = fp.getParentFile();
		return new File(fp, "scrunner.conf").getCanonicalPath();
	    } catch (Exception e) {
		return null;
	    }
	}
    }

    private static String defaultUsrConfigFile() {
	String fsep = System.getProperty("file.separator");
	if (fsep.equals("/")) {
	    // Unix file separator. The file is in the user's
	    // home directory in the  .config/bzdev subdirectory.
	    // (This defines the location of the file - it is
	    // not required to actually exist).
	    return System.getProperty("user.home")
		+ "/.config/bzdev/scrunner.conf";
	} else {
	    // For any other OS (primarily Windows in practice)
	    // assume there is only a system config file. A startup
	    // script can always define scrunner.usrconf to set the
	    // file explicitly.
	    return null;
	}
    }

    static Map<String,String> createMap(InputStream is) throws Exception {
	BufferedReader rd =
	    new BufferedReader(new InputStreamReader(is, "UTF-8"));
	Map<String,String> map = new HashMap<String,String>();
	String line;
	while ((line = rd.readLine()) != null) {
	    String[] tokens = line.trim().split("\\s*", 2);
	    if (tokens.length != 2) {
		throw new Exception(errorMsg("needTwoFields", line));
	    }
	    map.put(tokens[0], tokens[1]);
	}
	return map;
    }


    static int width = 1024;
    static int height = 1024;

    static final String reservedTDefNames[] = {
	"paths", "width", "height",
	"hasPackage", "class", "optSpace", "package", "public",
	"items"
    };

    /*
    static final String reservedTDefNames[] = {
	"varname", "subvarname",
	"windingRule",  "hasWindingRule",
	"segments",
	"type", "ltype", "atype",
	"method",
	"has0",	"has1",	"has2",
	"x0", "y0", "x1", "y1", "x2", "y2",
	"hasMoveTo", "hasLineTo", "hasQuadTo", "hasCubicTo", "hasClose",
	"area", "circumference", "pathLength",
	"vindex", "index", "pindex",
	"location",
	"x", "y", "xp", "yp", "ypr", "xy",
	"hasParameterInfo", "hasSubvarname", "subvarname","u", "s",
	"items", "pathStatement", "pathItem", "optcomma",
	"draw", "fill", "hasAttributes", "gcsMode", "hasGcsMode",
	"drawColor", "hasDrawColor", "fillColor", "hasFillColor",
	"strokeCap", "hasStrokeCap", "strokeWidth", "hasStrokeWidth",
	"strokeJoin", "hasStrokeJoin","miterLimit", "hasMiterLimit",
	"dashIncrement", "hasDashIncrement", "dashPhase", "hasDashPhase",
	"dashPattern", "hasDashPattern",
	"zorder", "hasZorder",
	"width", "height", "package", "hasPackage", "packageDir",
	"class", "hasClass", "optSpace", "public", "mapName", "module"
    };
    */
    static final String pathsRTDN[] = {
	"varname",
	"windingRule",
	"segments",
	"area",
	"circumference",
	"pathLength"
    };

    static final String segmentsRTDN[] = {
	"type", "method", "hasClose", "hasCubicTo",
	"has0", "has1", "has2", "x0", "y0", "x1", "y1",
	"x2", "y2"
    };

    static final String itemsRTDN[] = {
	"varname", "vindex", "index",
	"location", "pathStatement"
    };

    static final String locationRTDN[] = {
	"x", "y", "xp", "yp", "ypr"
    };

    static final String pathStatementRTDN[] = {
	"pathItem", "windingRule", "hasWindingRule",
	"draw", "fill", "hasAttributes",
	"gcsMode", "hasGCSMode",
	"drawColor", "hasDrawColor",
	"fillColor", "hasFillColor",
	"strokeCap", "hasStrokeCap",
	"dashIncrement", "hasDashIncrement",
	"strokeJoin", "hasStrokeJoin",
	"miterLimit", "hasMiterLimit",
	"strokeWidth", "hasStrokeWidth",
	"zorder", "hasZorder",
    };

    static final String pathItemRTDN[] = {
	"pindex", "type", "ltype", "atype",
	"hasParameterInfo", "optcomma",
	"xy"
    };

    static final String xyRTDN[] = {
	"x", "y", "xp", "yp", "ypr"
    };

    static final String hasParameterInfoRTDN[] = {
	"subvarname", "hasSubvarname", "u", "s"
    };

    // use a TreeSet because a dialog box has a table of these in
    // alphabetical order.
    static TreeSet<String> reservedTDefSet =
	new TreeSet<>(/*2*reservedTDefNames.length*/);

    static HashMap<String,TreeSet<String>> reservedTDefMap
	= new HashMap<>(64);

    static {
	for (String s: reservedTDefNames) {
	    reservedTDefSet.add(s);
	}
	TreeSet<String> tset = new TreeSet<String>();
	for (String s: pathsRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("paths", tset);

	tset = new TreeSet<String>();
	for (String s: segmentsRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("segments", tset);

	tset = new TreeSet<String>();
	for (String s: itemsRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("items", tset);

	tset = new TreeSet<String>();
	for (String s: locationRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("location", tset);

	tset = new TreeSet<String>();
	for (String s: pathStatementRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("pathStatement", tset);

	tset = new TreeSet<String>();
	for (String s: pathItemRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("pathItem", tset);

	tset = new TreeSet<String>();
	for (String s: xyRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("xy", tset);

	tset = new TreeSet<String>();
	for (String s: hasParameterInfoRTDN) {
	    tset.add(s);
	}
	reservedTDefMap.put("hasParameterInfo", tset);
    }

    public static Set<String> getReservedTDefNames() {
	return Collections.unmodifiableSet(reservedTDefSet);
    }

    public static Map<String,Set<String>> getReservedTDefMap() {
	return Collections.unmodifiableMap(reservedTDefMap);
    }

    public static final boolean isReservedTdef(String name) {
	int ind = name.indexOf(":");
	if (ind > -1) {
	    String test = name.substring(0, ind).trim();
	    name = name.substring(ind+1).trim();
	    if (reservedTDefSet.contains(test)) return true;
	}
	return reservedTDefSet.contains(name);
    }

    static final String EMPTYMAP = "<EMPTY KEYMAP>";
    static HashMap<String,String> tdefTable = new HashMap<>();

    static void storeTDef(String name, String value)
	throws IllegalStateException
    {
	int ind = name.indexOf(":");
	if (ind > -1) {
	    String test = name.substring(0, ind).trim();
	    if (tdefTable.containsKey(test)) {
		throw new IllegalStateException(errorMsg("storeTDef", test));
	    }
	    if (value != null  && value.length() > 0) {
		tdefTable.put(test, EMPTYMAP);
	    }
	    name = name.substring(ind+1).trim();
	}
	if (tdefTable.containsKey(name)) {
	    throw new IllegalStateException(errorMsg("storeTDef", name));
	}
	if (value != null) {
	    if (value.length() != 0) {
		tdefTable.put(name, value);
	    }
	} else {
	    tdefTable.put(name, EMPTYMAP);
	}
    }


    static void addDefinitionsTo(TemplateProcessor.KeyMap map)
    {
	for (Map.Entry<String,String> entry: tdefTable.entrySet()) {
	    String value = entry.getValue();
	    if (value == EMPTYMAP) {
		// use '==' so we have to literally match the constant
		// defined above.
		map.put(entry.getKey(), new TemplateProcessor.KeyMap());
	    } else {
		map.put(entry.getKey(), value);
	    }
	}
    }

    public static class PnameInfo {
	String name;
	String[] nameArray;
	public PnameInfo(String name, String[] nameArray) {
	    this.name = name;
	    this.nameArray = nameArray;
	}
    }

    public static class FilterInfo {
	String name;
	String[] nameArray;
	// SVG only
	String stroke = "none";
	String fillSVG = "none";
	String fillRule = "evenodd";
	// other templates
	String windingRule = null;
	String draw = "false";
	String fill = "false";
	String gcsMode;
	String drawColor;
	String fillColor;
	String strokeCap;
	String dashIncrement;
	String dashPhase;
	String dashPattern;
	String strokeJoin;
	String miterLimit;
	String strokeWidth;
	String zorder;
	// Value of zorder as a long integer
	long zo = Long.MIN_VALUE;
	public FilterInfo() {}
    }

    static String getDashArray(String dashPattern, String dashIncrement)
	throws IllegalArgumentException
    {
	double dashIncr = Double.parseDouble(dashIncrement);
	float[] array = BasicStrokeParm.getDashArray(dashPattern, dashIncr);
	if (array == null) return null;
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < array.length; i++) {
	    if (i > 0) sb.append(",");
	    sb.append("" + (double)array[i]);
	}
	return sb.toString();
    }


    static boolean stackTrace = false;
    static void printStackTrace(Throwable e, Appendable out) {
	StackTraceElement[] elements = e.getStackTrace();
	try {
	    for (StackTraceElement element: elements) {
		out.append("    " + element + "\n");
	    }
	} catch (IOException ioe) {}
    }

    static class NameValuePair {
	String name;
	Object value;
	public NameValuePair(String n, Object v) {
	    name = n;
	    value = v;
	}
	public String getName() {return name;}
	public Object getValue() {return value;}
    }

    static String initialcwd = System.getProperty("user.dir");
    static File initialcwdFile = (initialcwd == null)? null:
	new File(initialcwd);


    static SimpleConsole.ExitAccessor exitAccessor =
	new SimpleConsole.ExitAccessor();

    static SimpleConsole console = null;

    static void processDoScriptException(String filename, Exception e) {
	if (guiMode) {
	    try {
		console = SimpleConsole.newFramedInstance
		    (800, 600, localeString("consoleTitle"), true,
		     exitAccessor);
		console.setSeparatorColor(Color.BLACK);
		/*
		JFrame frame =
		    new JFrame(localeString("consoleTitle"));
		frame.setLayout(new BorderLayout());
		frame.setPreferredSize(new Dimension(800, 600));
		frame.setDefaultCloseOperation
		    (JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener
		    (new WindowAdapter() {
			    @Override
			    public void windowClosed
				(WindowEvent e)
			    {
				System.exit(1);
			    }
			});
		console = new SimpleConsole();
		JScrollPane scrollPane =
		    new JScrollPane(console);
		frame.add(scrollPane, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		*/
	    } catch (Exception ie) {
		console = null;
	    }
	}
	Appendable output = (console == null)? System.err: console;
	if (console != null) {
	    console.addSeparatorIfNeeded();
	    console.setBold(true);
	}
	try {
	    output.append(errorMsg("processingFile", filename) + "\n\n");
	} catch (IOException eio) {}
	if (console != null) {
	    console.setBold(false);
	}
	if (stackTrace) {
	    Throwable cause = e.getCause();
	    try {
		output.append(e.getClass().getName() + ": "
			      + e.getMessage() + "\n");
		printStackTrace(e, output);
		while (cause != null) {
		    output.append("---------\n");
		    output.append(cause.getClass().getName() + ": "
				  + cause.getMessage() + "\n");
		    printStackTrace(cause, output);
		    cause = cause.getCause();
		}
	    } catch (IOException eio) {}
	} else {
	    Throwable cause = e.getCause();
	    Class<?> ec = e.getClass();
	    String msg;
	    if (e instanceof ScriptException) {
		ScriptException sex = (ScriptException)e;
		String fn = sex.getFileName();
		int ln = sex.getLineNumber();
		if (cause != null) {
		    String m = cause.getMessage();
		    if (ln != -1) {
			// Some scripting-related exceptions tag
			// a string containing of the form
			//  (FILENAME#LINENO)
			// to the end of a message.  This is redundant
			// so we will eliminate it when it matches the
			// file name and line number we are printing.
			// The following lines contain all the 'cause'
			// messages anyway, so all the information is
			// available.  The lack of redundancy makes the
			// first message easier to read.
			String tail = String.format("(%s#%d)", fn, ln);
			if (m.endsWith(tail)) {
			    m = m.substring(0, m.lastIndexOf(tail));
			    m = m.trim();
			}
		    }
		    if (e.getMessage().contains(m)) {
			if (ln == -1) {
			    msg = errorMsg("unnumberedException", fn,m);
			} else {
			    msg = errorMsg("numberedException",fn,ln,m);
			}
		    } else {
			msg =
			    errorMsg("scriptException", e.getMessage());
		    }
		} else {
		    msg = errorMsg("scriptException", e.getMessage());
		}
	    } else {
		String cn = e.getClass().getName();
		msg = errorMsg("exception2", cn, e.getMessage());
	    }
	    try {
		output.append(msg + "\n");
		while (cause != null) {
		    Class<?> clasz = cause.getClass();
		    Class<?> target =
			org.bzdev.obnaming.NamedObjectFactory
			.ConfigException.class;
		    String tn = errorMsg("ldotsConfigException");
		    String cn =(clasz.equals(target))? tn: clasz.getName();
		    msg = errorMsg("continued", cn, cause.getMessage());
		    output.append("  " + msg + "\n");
		    cause = cause.getCause();
		}
	    } catch (IOException eio) {
		System.err.println(errorMsg("eioOnErr", eio.getMessage()));
	    } catch (Throwable t) {
		t.printStackTrace(System.err);
	    }
	}
    }

    static Graph doScripts(ScriptingEnv se,
			   List<NameValuePair> bindings,
			   List<String> targetList,
			   List<String> targetList2)
    {
	File cdir = new File(System.getProperty("user.dir"));
	// ScriptingEnv se = new ScriptingEnv(languageName, a2dName);
	for (NameValuePair binding: bindings) {
	    se.putScriptObject(binding.getName(), binding.getValue());
	}
	String current = null;
	try {
	    if (targetList != null) {
		for (String filename: targetList) {
		    InputStream is;
		    current = filename;
		    if (maybeURL(filename)) {
			URL url = new URL(cdir.toURI().toURL(), filename);
			is = url.openStream();
		    } else {
			File f = new File(filename);
			f = f.isAbsolute()? f: new File(cdir, filename);
			is = new FileInputStream(f);
		    }
		    Reader r = new InputStreamReader(is, "UTF-8");
		    r = new BufferedReader(r);
		    se.evalScript(filename, r);
		}
	    }
	    if (targetList2 != null) {
		cdir = initialcwdFile;
		for (String filename: targetList2) {
		    InputStream is;
		    current = filename;
		    if (maybeURL(filename)) {
			URL url = new URL(cdir.toURI().toURL(), filename);
			is = url.openStream();
		    } else {
			File tmp = new File(filename);

			File ifile = (tmp.isAbsolute())? tmp:
			    new File(cdir, filename);
			ifile = ifile.getCanonicalFile();
			is = new FileInputStream(ifile);
		    }
		    Reader r = new InputStreamReader(is, "UTF-8");
		    r = new BufferedReader(r);
		    se.evalScript(filename, r);
		}
	    }
	} catch (FileNotFoundException ef) {
	    displayError(errorMsg("readError", current, ef.getMessage()));
	    System.exit(1);
	} catch (IOException eio) {
	    String msg = errorMsg("readError", current, eio.getMessage());
	    displayError(msg);
	    System.exit(1);
	} catch (Exception e) {
	    if (guiMode) {
		final Exception ee = e;
		final String fn = current;
		try {
		    SwingUtilities.invokeAndWait(() -> {
			    processDoScriptException(fn, e);
			});
		} catch (InterruptedException ie) {
		} catch (InvocationTargetException ite) {
		}
	    } else {
		processDoScriptException(current, e);
	    }
	    if (guiMode) {
		return null;
	    } else {
		System.exit(1);
	    }
	}
	try {
	    se.drawGraph(image);
	    Graph graph = se.getGraph();
	    double scale = graph.getXScale();
	    double yscale = graph.getYScale();
	    if (scale != yscale) {
		double max = Math.max(Math.abs(scale), Math.abs(yscale));
		if (max == 0.0 || Math.abs((scale-yscale)/max) > 1.e-12) {
		    displayError(errorMsg("xyScale", scale, yscale));
		    System.exit(1);
		}
	    }
	    return graph;
	} catch (Exception e) {
	    if (stackTrace) {
		e.printStackTrace(System.err);
	    } else {
		e.printStackTrace();
		displayError(errorMsg("exception", e.getMessage()));
	    }
	    System.exit(1);
	    return null;
	}
    }

    private static final String units[] = {
	"nm", "um", "mm", "cm", "m", "km",
	"in", "ft", "yd", "mi"
    };

    static double convert(String arg) {
	arg = arg.replaceAll("\\p{Space}+", "");
	String suffix = null;
	double value = 0.0;
	for (String s: units) {
	    if (arg.endsWith(s)) {
		suffix = s;
		arg = arg.substring(0, arg.indexOf(suffix));
		value = Double.parseDouble(arg);
		break;
	    }
	}
	if (suffix == null) {
	    value = Double.parseDouble(arg);
	} else if (suffix.equals("nm")) {
	    value *= 1.e-9;
	} else if (suffix.equals("um")) {
	    value *= 1.e-6;
	} else if (suffix.equals("mm")) {
	    value *= 1.e-3;
	} else if (suffix.equals("cm")) {
	    value *= 1.e-2;
	} else if (suffix.equals("m")) {
	    // nothing to do
	} else if (suffix.equals("km")) {
	    value *= 1000;
	} else if (suffix.equals("in")) {
	    value *= .0254;
	} else if (suffix.equals("ft")) {
	    value *= .3048;
	} else if (suffix.equals("yd")) {
	    value *= .9144;
	} else if (suffix.equals("mi")) {
	    value *= 1609.344;
	} else {
	    throw new NumberFormatException(errorMsg("unknownUnits", suffix));
	}
	return value;
    }

    static String parseArgument(String option, String arg, Class<?> clazz) {
	return parseArgument(option, arg, clazz, null, false, null, false);
    }

    static String parseArgument(String option, String arg, Class<?> clazz,
				String lower, boolean lowerClosed,
				String upper, boolean upperClosed)
	throws IllegalArgumentException, NumberFormatException
    {
	try {
	    if (clazz.equals(Boolean.class)) {
		arg = arg.trim().toLowerCase();
		if (arg.equals("true")) {
		    return "true";
		} else if (arg.equals("false")) {
		    return "false";
		} else {
		    throw new IllegalArgumentException
			(errorMsg("notBoolean", option, arg));
		}
	    } else if (clazz.equals(Integer.class))
		{
		int val = Integer.parseInt(arg);
		if (lower != null) {
		    int bound = Integer.parseInt(lower);
		    if (lowerClosed) {
			if (val < bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    } else {
			if (val <= bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    }
		}
		if (upper != null) {
		    int bound = Integer.parseInt(upper);
		    if (upperClosed) {
			if (val > bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    } else {
			if (val >= bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    }
		}
		return arg;
	    } else if (clazz.equals(Long.class))
		{
		long val = Long.parseLong(arg);
		if (lower != null) {
		    long bound = Long.parseLong(lower);
		    if (lowerClosed) {
			if (val < bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    } else {
			if (val <= bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    }
		}
		if (upper != null) {
		    long bound = Long.parseLong(upper);
		    if (upperClosed) {
			if (val > bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    } else {
			if (val >= bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    }
		}
		return arg;
	    } else if (clazz.equals(Double.class)) {
		double val = convert(arg);
		// double val = Double.parseDouble(arg);
		if (lower != null) {
		    double bound = Double.parseDouble(lower);
		    if (lowerClosed) {
			if (val < bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    } else {
			if (val <= bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", option, arg));
			}
		    }
		}
		if (upper != null) {
		    double bound = Double.parseDouble(upper);
		    if (upperClosed) {
			if (val > bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", arg));
			}
		    } else {
			if (val >= bound) {
			    throw new IllegalArgumentException
				(errorMsg("outofRange", arg));
			}
		    }
		}
		return arg;
	    } else if (clazz.equals(Color.class)) {
		try {
		    return parseColor(arg);
		} catch (IllegalArgumentException ec) {
		    throw new IllegalArgumentException
			(errorMsg("colorFormat", option, arg), ec);
		}
	    } else if (clazz.equals(BasicStrokeParm.Cap.class)) {
		arg = arg.trim().toUpperCase();
		BasicStrokeParm.Cap.valueOf(arg);
		return arg;
	    } else if (clazz.equals(BasicStrokeParm.Join.class)) {
		arg = arg.trim().toUpperCase();
		BasicStrokeParm.Join.valueOf(arg);
		return arg;
	    } else {
		throw new UnexpectedExceptionError();
	    }
	} catch (NumberFormatException e) {
	    throw new IllegalArgumentException
		(errorMsg("numberFormat", option, arg), e);
	}
    }

    // Convert to rgb or rgba syntax to make minimal assumptions about
    // how such colors might be used.
    static String parseColor(String cssSpec) throws IllegalArgumentException {
	if (cssSpec.equals("none")) {
	    return null;
	}
	int[] components = Colors.getComponentsByCSS(cssSpec);
	if (components.length == 3) {
	    return String.format((Locale)null, "rgb(%d,%d,%d)",
				 components[0],
				 components[1],
				 components[2]);
	} else if (components.length == 4) {
	    return String.format((Locale)null, "rgba(%d,%d,%d,%d)",
				 components[0],
				 components[1],
				 components[2],
				 components[3]);
	} else {
	    return null;
	}
    }

    private static Pattern urlPattern =
	Pattern.compile("\\p{Alpha}[\\p{Alnum}.+-]*:.*");

    public static boolean maybeURL(String s) {
	boolean result =  urlPattern.matcher(s).matches();
	if (result) {
	    // Windows can use a drive letter followed by a colon
	    // as part of a file name, so do a check to avoid confusion
	    for (File root: File.listRoots()) {
		String name = root.toString();
		if (s.startsWith(name)) {
		    result = false;
		    break;
		}
	    }
	}
	if (!result) {
	    if (!s.contains(File.separator) && s.contains("/")) {
		// relative URIs use URI-path syntax, so if there is a
		// "/" and the "/" is not a file-separator character,
		// we'll treat it as a relative URI if a "/" appears
		// but there is no file-separator character in the
		// string.
		return true;
	    }
	}
	return result;
    }

    static String processLanguageName(String arg) {
	String languageName = null;
	int extInd = arg.lastIndexOf('.');
	int lastSlash = arg.lastIndexOf('/');
	int lastSep = arg
	    .lastIndexOf(File.separatorChar);
	if (extInd>0 && extInd>lastSlash && extInd>lastSep) {
	    extInd++;
	    if (extInd < arg.length()) {
		languageName = arg.substring(extInd);
		if (languageName.equals("epts")
		    || languageName.equals("eptt")
		    || languageName.equals("eptc")) {
		    languageName = null;
		} else if (ImageMimeInfo
			   .getFormatNameForSuffix(languageName)
			   != null) {
		    languageName = null;
		}
	    }
	}
	return languageName;
    }



    public static boolean isImagePath(String path) {
	if (path == null) return false;
	path = path.trim();
	if (path.length() == 0) return false;
	if (maybeURL(path)) {
	    int ind = path.indexOf('#');
	    if (ind > -1) {
		path = path.substring(0, ind);
	    }
	    ind = path.indexOf('?');
	    if (ind > -1) {
		path = path.substring(0, ind);
	    }
	}
	int i = path.lastIndexOf('.');
	if (i == -1) return false;
	path = path.substring(i+1).toLowerCase();
	for (String suffix: ImageIO.getReaderFileSuffixes()) {
	    suffix = suffix.toLowerCase();
	    if (path.equals(suffix)) return true;
	}
	return false;
    }


    static void processImageArg(String imageArg, ArrayList<String>targetList,
				File cdir)
    {
	String extension = null;
	int extInd = imageArg.lastIndexOf('.');
	int lastSlash = imageArg.lastIndexOf('/');
	int lastSep = imageArg
	    .lastIndexOf(File.separatorChar);
	if (extInd>0 && extInd>lastSlash && extInd>lastSep) {
	    extInd++;
	    if (extInd < imageArg.length()) {
		extension = imageArg.substring(extInd);
	    }
	    if (isImagePath(imageArg)
		/*extension != null
		&& ImageMimeInfo
		.getMIMETypeForSuffix(extension) != null*/) {
		// implied image mode based on file-name
		// extension
		try {
		    if (maybeURL(imageArg)
			&& !(imageArg.startsWith("file:")
			     /*&& imageArg.endsWith(".epts")*/)) {
			try {
			    URL imageURL = new URL(cdir.toURI()
						   .toURL(),
						   imageArg);
			    imageURI = imageURL.toURI();
			    image = ImageIO.read(imageURL);
			} catch (Exception e) {
			    e.printStackTrace(System.err);
			    System.exit(1);
			}
		    } else {
			File ifile = null;
			if (imageArg.startsWith("file:")) {
			    ifile = new File(new URI(imageArg));
			} else {
			    ifile = new File(imageArg);
			}
			File parent = ifile.getParentFile();
			if (parent != null) {
			    System.setProperty
				("user.dir",
				 parent.getCanonicalPath());
			}
			imageURI = ifile.getCanonicalFile().toURI();
			image = ImageIO.read(ifile);
		    }
		} catch (Exception e) {
		    String emsg = e.getMessage();
		    if (emsg == null) {
			emsg = e.getClass().getName();
		    }
		    String msg = errorMsg("cannotReadImage", imageArg, emsg);
		    displayError(msg);
		    System.exit(1);
		}
	    } else {
		targetList.add(imageArg);
	    }
	} else {
	    targetList.add(imageArg);
	}
    }

    static URI imageURI = null;
    static Image image = null;
    static boolean custom = false;
    static final Color endColor = Colors.getColorByCSS("darkred");

    static void appendFinalMsg() {
	try {
	    SwingUtilities.invokeAndWait(() -> {
		    console.perform((cc) -> {
			    cc.addSeparatorIfNeeded();
			    cc.setBold(true);
			    cc.setTextForeground(endColor);
			    cc.append(errorMsg("doScriptConsole") + "\n");
			});
		});
	} catch (InterruptedException ie) {
	    displayError(errorMsg("doScriptConsole"));
	} catch (InvocationTargetException ite) {
	    displayError(errorMsg("doScriptConsole"));
	}
    }

    static void ifork(String argv[]) {
	// used to restart after config files read.
	LinkedList<String>alist = new LinkedList<>();
	alist.add("java");
	alist.add("-Dorg.bzdev.epts.ifork=true");
	for (String key: defs.stringPropertyNames()) {
	    alist.add("-D" + key + "=" + defs.getProperty(key));
	}
	alist.add("-p");
	alist.add(sbmp.toString());
	if (sbmod.length() > 0) {
	    alist.add("--add-modules");
	    alist.add(sbmod.toString());
	}
	if (sbcp.length() > 0) {
	    alist.add("-classpath");
	    alist.add(sbcp.toString());
	}
	alist.add("-m");
	alist.add("org.bzdev.epts");
	for (String arg: argv) {
	    alist.add(arg);
	}
	ProcessBuilder pb = new ProcessBuilder(alist);
	pb.inheritIO();
	try {
	    Process process = pb.start();
	    System.exit(process.waitFor());
	} catch (Exception e) {
	    displayError(errorMsg("scException", e.getMessage()));
	    System.exit(1);
	}
    }

    static void fork(ArrayList<String>jargsList, ArrayList<String> argsList,
		     String policyFile, boolean dryrun)
    {
	jargsList.add("-m");
	jargsList.add ("org.bzdev.epts");
	// jargsList.add(0, sbcp.toString());
	// jargsList.add(0, "-classpath");
	if (policyFile == null) {
	    try {
		File pf = new File(EPTS.class.getProtectionDomain()
				   .getCodeSource().getLocation().toURI());
		pf = pf.getParentFile();
		jargsList.add(0, "-Djava.security.policy="
			      + (new File(pf, "epts.policy")
				 .getCanonicalPath()));
	    } catch (Exception eio) {
		displayError(errorMsg("policyFile"));
		System.exit(1);
	    }
	}
	/*
	String classLoader = defs.getProperty("java.system.class.loader");
	if (classLoader == null) {
	    jargsList.add(0, "-Djava.system.class.loader="
			  + "org.bzdev.lang.DMClassLoader");
	}
	*/

	if (sbmp.length() > 0) {
	    jargsList.add(0, sbmp.toString());
	    jargsList.add(0, "-p");
	}
	if (sbmod.length() > 0) {
	    jargsList.add(0, sbmod.toString());
	    jargsList.add(0, "--add-modules");
	}
	if (sbcp.length() > 0) {
	    jargsList.add(0, sbcp.toString());
	    jargsList.add(0, "-classpath");
	}

	jargsList.add(0, "-Depts.fdrUsed="+(fdrUsed? "true": "false"));
	jargsList.add(0, "-Depts.alreadyforked=true");
	jargsList.add(0, javacmd);
	jargsList.addAll(argsList);
	if (dryrun) {
	    for (String s: jargsList) {
		System.out.print(s);
		System.out.print(" ");
	    }
	    System.out.println();
	    System.exit(0);
	}
	ProcessBuilder pb = new ProcessBuilder(jargsList);
	pb.inheritIO();
	try {
	    Process process = pb.start();
	    System.exit(process.waitFor());
	} catch (Exception e) {
	    displayError(errorMsg("scException", e.getMessage()));
	    System.exit(1);
	}
    }

    public static boolean extensionMatch(File f, String[] extensions) {
	String name = f.getName();
	if (name == null) return false;
	int index = name.lastIndexOf('.');
	if (index == -1) return false;
	String ext = name.substring(index+1);
	for (String extension: extensions) {
	    if (ext.equals(extension)) return true;
	}
	return false;
    }
    private static boolean scriptMode = false;
    private static boolean imageMode = false;
    private static String a2dName = null;
    private static boolean dryrun = false;

    private static String inputFile = null;
    private static boolean fdrUsed = false;

    private static final Runnable fileDialogRunnable = new Runnable() {
	    public void run() {
		String cdir = System.getProperty("user.dir");
		OpeningFileChooser ofc = new
		    OpeningFileChooser(localeString("EPTSFiles"),
				       new String[] {
					   "epts",
					   "eptc",
					   "eptt"
				       });
		switch(ofc.showOpeningDialog(null, new File(cdir))) {
		case OpeningFileChooser.DIMENSIONS_OPTION:
		    width = ofc.getWidth();
		    height = ofc.getHeight();
		    imageMode = true;
		    break;
		case OpeningFileChooser.APPROVE_OPTION:
		    try {
			inputFile = ofc.getSelectedFile().getCanonicalPath();
		    } catch (IOException e) {
			String name = ofc.getSelectedFile().getName();
			String  msg = errorMsg("toCanonicalPath", name);
			System.err.println(msg);
			System.exit(1);
		    }
		    break;
		case OpeningFileChooser.CANCEL_OPTION:
		    System.exit(0);
		    return;
		}
		fdrUsed = true;
		/*
		for(;;) {
		    JFileChooser fc = new JFileChooser(cdir);
		    fc.setAcceptAllFileFilterUsed(false);
		    String[] extensions =
			ImageIO.getReaderFileSuffixes();
		    FileNameExtensionFilter filter =
			new FileNameExtensionFilter
			(localeString("Images"), extensions);
		    Set<String> extensionSet =
			Scripting.getExtensionSet();
		    String[] sextensions =
			extensionSet.toArray
			(new String[extensionSet.size()]);
		    ArrayList<String> allExtensionsList =
			new ArrayList<>();
		    allExtensionsList.add("epts");
		    allExtensionsList.add("eptc");
		    for (String ext: extensions) {
			allExtensionsList.add(ext);
		    }
		    for (String ext: sextensions) {
			allExtensionsList.add(ext);
		    }
		    String[] allExtensions = allExtensionsList
			.toArray(new String[allExtensionsList.size()]);
		    FileNameExtensionFilter afilter =
			new FileNameExtensionFilter
			(localeString("StatesConfImagesScripts"),
			 allExtensions);
		    String eptsExtensions[] = {"epts", "eptc"};
		    FileNameExtensionFilter efilter =
			new FileNameExtensionFilter
			(localeString("EPTSSavedStatesConf"),
			 eptsExtensions);
		    fc.addChoosableFileFilter(afilter);
		    fc.addChoosableFileFilter(efilter);
		    fc.addChoosableFileFilter(filter);
		    FileNameExtensionFilter sfilter =
			new FileNameExtensionFilter
			(localeString("Scripts"), sextensions);
		    fc.addChoosableFileFilter(sfilter);
		    int status = fc.showOpenDialog(null);
		    if (status == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			try {
			    if (extensionMatch(f, eptsExtensions)) {
				inputFile = f.getCanonicalPath();
				break;
			    } else if (extensionMatch(f, extensions)) {
				inputFile = f.getCanonicalPath();
				break;
			    } else if (extensionMatch(f, sextensions)) {
				inputFile = f.getCanonicalPath();
				break;
			    } else {
				SwingErrorMessage.setComponent(null);
				SwingErrorMessage.display
				    (null, null, errorMsg("unrecognizedFNE"));
			    }
			    continue;
			} catch (Exception e) {
			    SwingErrorMessage.setComponent(null);
			    SwingErrorMessage.display(e);
			    System.exit(1);
			}
		    } else {
			inputFile = null;
			break;
		    }
		}
		fdrUsed = true;
		*/
	    }
	};

    private static String[] preprocessArgs(int ind, String[] argv, String arg) {
	if (arg == null) {
	    try {
		boolean prevImageMode = imageMode;
		SwingUtilities.invokeAndWait(fileDialogRunnable);
		if (inputFile != null) {
		    boolean by1 =
			(argv.length > 0 && argv[argv.length-1].equals("--"));
		    String[] newargv = new String[argv.length + (by1? 1: 2)];
		    System.arraycopy(argv, 0, newargv, 0, argv.length);
		    if (!by1) newargv[argv.length] = "--";
		    newargv[argv.length+ (by1? 0: 1)] = inputFile;
		    argv = newargv;
		    arg = inputFile;
		} else if (imageMode && (prevImageMode == false)) {
		    // we set width and height using fileDialogRunnable
		    boolean by1 =
			(argv.length > 0 && argv[argv.length-1].equals("--"));
		    String[] newargv = new String[argv.length + (by1? 3: 4)];
		    System.arraycopy(argv, 0, newargv, 0, argv.length);
		    if (!by1) newargv[argv.length] = "--";
		    int indx = argv.length+ (by1? -1: 0);
		    newargv[indx++] = "-w";
		    newargv[indx++] = "" + width;
		    newargv[indx++] = "-h";
		    newargv[indx++] = "" + height;
		    argv = newargv;
		} else {
		    // we must have canceled
		    if (ind > -1 && ind < 4) {
			System.exit(0);
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace(System.err);
		System.exit(1);
	    }
	}
	if (arg != null /*&& !arg.startsWith("-")*/) {
	    String ext = null;
	    boolean url = false;
	    try {
		if (maybeURL(arg)) {
		    if (arg.startsWith("file:")) {
			ext = getExtension(arg);
			url = true;
		    }
		} else {
		    ext = getExtension(arg);
		}
		if (ext != null && ext.equals("eptc")) {
		    if (url) {
			arg = new File(new URI(arg)).getAbsolutePath();
		    }
		    ZipDocFile zdf =
			new ZipDocFile(arg, Charset.forName("UTF-8"));
		    String mediaType = zdf.getMimeType();
		    if (!mediaType.equals
			("application/vnd.bzdev.epts-config+zip")) {
			throw new IOException("notEPTCFile");
		    }
		    if (needInitialConfig) {
			readConfigFiles(null);
			if (System.getProperty("org.bzdev.epts.ifork") == null
			    && classpathExtended) {
			    ifork(argv);
			}
			needInitialConfig = false;
		    }
		    
		    File argf = new File(arg);
		    File argfp = argf.getParentFile();
		    if (argfp != null) {
			System.setProperty("user.dir",
					   argfp.getCanonicalPath());
		    }
		    argv = Setup.getSetupArgs(zdf, argf);
		    // System.setProperty("user.dir", initialcwd);
		    zdf.close();
		}
		if (ext != null && ext.equals("eptt")) {
		    if (url) {
			arg = new File(new URI(arg)).getAbsolutePath();
		    }
		    ZipDocFile zdf =
			new ZipDocFile(arg, Charset.forName("UTF-8"));
		    String mediaType = zdf.getMimeType();
		    if (!mediaType.equals
			("application/vnd.bzdev.epts-template-config+zip")) {
			throw new IOException("notEPTTFile");
		    }
		    if (needInitialConfig) {
			readConfigFiles(null);
			if (System.getProperty("org.bzdev.epts.ifork") == null
			    && classpathExtended) {
			    ifork(argv);
			}
			needInitialConfig = false;
		    }
		    File argf = new File(arg);
		    File argfp = argf.getParentFile();
		    if (argfp != null) {
			System.setProperty("user.dir",
					   argfp.getCanonicalPath());
		    }
		    argv = TemplateSetup.getSetupArgs(zdf, argf, null);
		    // System.setProperty("user.dir", initialcwd);
		    zdf.close();
		}
	    }  catch (Exception e) {
		e.printStackTrace(System.err);
		System.exit(1);
	    }
	}
	return argv;
    }

    private static String[][] argpatterns = {
	{"--sessionConfig"},			// index 0
	{"--templateConfig"},			// index 1
	{"--stackTrace", "--sessionConfig"},	// index 2
	{"--stackTrace", "--templateConfig"},	// index 3
	{"--gui", "--stackTrace", "--"},	// index 4
	{"--gui", "--stackTrace"},		// index 5
	{"--gui", "--"},			// index 6
	{"--gui"},				// index 7
	{"--stackTrace", "--"},			// index 8
	{"--stackTrace"},			// index 9
	{"--"},					// index 10
	{}					// index11
    };

    private static int getAPIndex(String[] argv) {
	int ind = -1;
	for (String[] pattern: argpatterns) {
	    ind++;
	    int n = pattern.length;
	    if (argv.length < n) continue;
	    if (argv.length > n+1) continue;
	    boolean cont = false;
	    for (int i = 0; i < n; i++) {
		if (!pattern[i].equals(argv[i])) {
		    cont = true;
		    break;
		}
	    }
	    if (cont) continue;
	    if (ind == (argpatterns.length - 1)) {
		if (argv.length == 1
		    && (argv[0].endsWith(".eptc")
			|| argv[0].endsWith(".eptt"))) {
		    return ind;
		} else {
		    continue;
		}
	    }
	    switch(ind) {
	    case 2: case 3: case 4: case 5: case 8: case 9:
		stackTrace = true;
		break;
	    default:
		break;
	    }
	    return ind;
	}
	return -1;
    }

    private static String getArgFromIndex(String[] argv, int ind) {
	if (ind == -1) return null;
	int n = argpatterns[ind].length;
	if (n >= argv.length) {
	    return null;
	} else {
	    return argv[n];
	}
    }

    private static String ofoptions[] = {
	"--stackTrace",	"-o", "--"
    };

    private static String templateCase(String argv[]) {
	int index = 0;
	if (argv.length == 1) return null;
	if (!argv[argv.length-1].endsWith(".eptt")) return null;
	String result = null;
	for (int i = 0; i < argv.length-1; i++) {
	    String option = argv[i];
	    int j;
	    for (j = 0; j < ofoptions.length; j++) {
		if (ofoptions[j].equals(option)) break;
	    }
	    if (j == ofoptions.length) {
		return null;
	    }
	    if (argv[i].equals("--stackTrace")) stackTrace = true;
	    if (argv[i].equals("--") && argv.length != i+2) return null;
	    if (argv[i].equals("-o")) {
		result = argv[++i];
	    }
	}
	return result;
    }

    // This is used so we can find a codebase before we fork.
    private static EPTSParser createParsedParser(String filename) {
	try {
	    EPTSParser parser = new EPTSParser();
	    if (filename.startsWith("file:")) {
		filename = (new File(new URI(filename))).getCanonicalPath();
	    } else if (maybeURL(filename)) {
		displayError(errorMsg("urlForSavedState", filename));
		System.exit(1);
	    }
	    File parent  = new File(filename).getParentFile();
	    if (parent != null) {
		System.setProperty("user.dir",
				   parent.getCanonicalPath());
	    }
	    parser.setXMLFilename(filename);
	    parser.parse(new FileInputStream(filename));
	    return parser;
	} catch (Exception e) {
	    if (stackTrace) {
		e.printStackTrace(System.err);
	    }
	    String msg = e.getMessage();
	    if (msg == null) msg = e.getClass().toString();
	    displayError(errorMsg("exception2", e.getClass().getName(),
				  e.getMessage()));
	    System.exit(1);
	}
	return null;
    }

    private static boolean needInitialConfig = true;

    // Need these two for createTemplateMenuItem in EPTSWindow
    static String initialModulePath = null;
    static String EPTSmodule = null;

    static void init(String argv[]) throws Exception {

	final File cdir = new File(System.getProperty("user.dir"));

	String initialClassPath = System.getProperty("java.class.path");

	if (initialClassPath != null) {
	    String[] ccomps = initialClassPath.split(pathSeparator);
	    for (String comp: ccomps) {
		extendCodebase(comp, false);
	    }
	    // sbcp.append(initialClassPath);
	}
	initialModulePath = System.getProperty("jdk.module.path");
	if (initialModulePath != null) {
	    String[] mcomps = initialModulePath.split(pathSeparator);
	    for (String comp: mcomps) {
		extendCodebase(comp, true);
	    }
	}

	EPTSmodule = System.getProperty("jdk.module.main");

	String alreadyForkedString = System.getProperty("epts.alreadyforked");
	boolean alreadyForked = (alreadyForkedString != null)
	    && alreadyForkedString.equals("true");
	if (System.getProperty("epts.alreadyforked") == null
	    || !(System.getProperty("epts.alreadyforked").equals("true"))) {
	    sysconf = defaultSysConfigFile();
	    usrconf = defaultUsrConfigFile();
	}
	String fdrUsedString = System.getProperty("epts.fdrUsed");
	if (fdrUsedString != null && fdrUsedString.equals("true")) {
	    fdrUsed = true;
	}
	// special cases.
	String outfile = null;
	if (!alreadyForked) {
	    if (argv.length == 0) {
		argv = preprocessArgs(-1, argv, null);
		/*
		System.out.println("arguments:");
		for (String s: argv) {
		    System.out.println("    " + s);
		}
		*/
	    } else if ((outfile = templateCase(argv)) != null /*(argv.length == 3 &&
			argv[0].equals("-o")
			&& argv[2].endsWith(".eptt")
			&& argv[2].charAt(0) != '-')
		       || (argv.length == 4 &&
			   argv[0].equals("-o")
			   && argv[2].equals("--")
			   && argv[3].endsWith(".epntt"))*/) {
		// String outfile = argv[1];
		if (outfile.startsWith("file:")) {
		    outfile = (new File(new URI(outfile))).getAbsolutePath();
		}
		String infile = argv[argv.length - 1];
		if (infile.startsWith("file:")) {
		    infile = (new File(new URI(infile))).getAbsolutePath();
		}
		ZipDocFile zdf = new ZipDocFile(infile,
						Charset.forName("UTF-8"));
		if (!zdf.getMimeType().equals
		    ("application/vnd.bzdev.epts-template-config+zip")) {
		    throw new IOException("notEPTTFile");
		}
		// create args without starting the GUI.
		TemplateSetup.outFile = outfile;
		TemplateSetup.restore(zdf, false, null);
		zdf.close();
		try {
		    argv = TemplateSetup.generate();
		} catch (Exception e) {
		    e.printStackTrace();
		    System.exit(1);
		}
	    } else {
		int ind = getAPIndex(argv);
		if (ind > -1 && ind < 4) {
		    if (argv.length != argpatterns[ind].length) {
			if (argv.length > 1 &&
			    !argv[argv.length-2].equals("--templateConfig")) {
			    System.err.println
				(localeString("noArgsSessionConfig"));
			    System.exit(1);
			}
		    }
		    if (argv[0].equals("--stackTrace")) {
			stackTrace = true;
		    }
		    if (needInitialConfig) {
			readConfigFiles(null);
			if (System.getProperty("org.bzdev.epts.ifork") == null
			    && classpathExtended) {
			    ifork(argv);
			}
			needInitialConfig = false;
		    }
		    if (argv[argv.length-1].equals("--sessionConfig")) {
			argv = Setup.getSetupArgs(null, null);
		    } else if (argv[argv.length-1].equals("--templateConfig")) {
			argv = TemplateSetup.getSetupArgs(null, null, null);
		    } else if (argv.length > 1
			       && argv[argv.length-2].equals("--templateConfig")
			       && (argv[argv.length-1].endsWith(".epts")
				   || argv[argv.length-1].endsWith(".EPTS"))) {
			String eptsName = argv[argv.length-1];
			File eptsf = new File(eptsName);
			if (eptsf.isFile() && eptsf.canRead()) {
			    File eptsfp = eptsf.getParentFile();
			    if (eptsfp != null) {
				System.setProperty("user.dir",
						   eptsfp.getCanonicalPath());
			    }
			    eptsName = eptsf.getCanonicalPath();
			    argv = TemplateSetup.getSetupArgs(null, null,
							      eptsName);
			} else {
			    System.err.println(errorMsg("nosuchFile",eptsName));
			    System.exit(1);
			}
		    } else {
			System.err.println(errorMsg("unrecognizedOption",
						    argv[argv.length-1]));
			System.exit(-1);
		    }
		    /*
		    System.out.println("arguments:");
		    for (String s: argv) {
			System.out.println("    " + s);
		    }
		    */
		} else if (ind != -1) {
		    String arg = getArgFromIndex(argv, ind);
		    argv = preprocessArgs(ind, argv, arg);
		    /*
		    System.out.println("arguments:");
		    for (String s: argv) {
			System.out.println("    " + s);
		    }
		    */
		}
	    }
	}
	int index = -1;
	int port = 0;

	File eptsFile = null;

	ArrayList<String> jargsList = new ArrayList<>();
	ArrayList<String> targetList = new ArrayList<>();
	Map<String,String> map = null;
	boolean jcontinue = true;
	boolean hasCodebase = false;
	boolean webserverOnly = false;
	ArrayList<String> argsList = new ArrayList<>();
	OutputStream out = null;
	String outName = null;
	String pname = null;
	String[] pnameArray = null;
	URL templateURL = null;

	double flatness = 0.0;
	int limit = 10;
	boolean straight = false;
	boolean elevate = false;
	boolean gcs = false;
	boolean svg = false;
	FilterInfo filterInfo = new FilterInfo();
	ArrayList<FilterInfo> filterInfoList = new ArrayList<>();
	ArrayList<PnameInfo> pnameInfoList = new ArrayList<>();

	String pkg = null;
	String moduleName = null;
	String clazz = null;
	boolean isPublic = false;

	String imageArg = null;
	EPTSParser iparser = null;

	ArrayList<NameValuePair> bindings = new ArrayList<>();

	while ((++index) < argv.length) {
	    if (jcontinue) {
		if (argv[index].startsWith("-D") ||
		       argv[index].startsWith("-J-D")) {
		    // These -D arguments are provided *after*
		    // EPTS appears on the
		    // java command line, but will be put *before*
		    // ETPS appears on the java
		    // command line that this program generates
		    String arg = argv[index];
		    if (arg.startsWith("-J")) arg = arg.substring(2);
		    int ind = arg.indexOf('=');
		    if (ind < 0) {
			displayError(errorMsg("badArgument", arg));
			System.exit(1);
		    }
		    String[] pair = new String[2];
		    pair[0] = arg.substring(2, ind);
		    pair[1] = arg.substring(ind+1);
		    String name = pair[0];
		    String value = pair[1];
		    if (propertyNotAllowed(name)) {
			displayError(errorMsg("badArgProp", name));
			System.exit(1);
		    }
		    if (name.equals("scrunner.sysconf")) {
			sysconf = value;
		    } else if (name.equals("scrunner.usrconf")) {
			usrconf = value;
		    } else {
			defs.setProperty(name, value);
		    }
		} else if (argv[index].startsWith("-J")) {
		    jargsList.add(argv[index].substring(2));
		    continue;
		} else if (argv[index].equals("--dryrun")) {
		    dryrun = true;
		    // argsList.add(argv[index]);
		} else if (argv[index].equals("-L")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    languageName = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		    continue;
		} else if (argv[index].equals("--module")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    moduleName = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--class")
			   || argv[index].equals("--mapName")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    clazz = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--public")) {
		    isPublic = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("-p")
		       || argv[index].equals("--module-path")) {
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    String[] mcomps = argv[index].trim().split(pathSeparator);
		    for (String mcomp: mcomps) {
			extendCodebase(mcomp, true);
			codebase.add(mcomp);
			codebaseSet.add(mcomp);
		    }
		} else if (argv[index].equals("--add-modules")) {
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    String[] modules = argv[index].trim().split(",");
		    for (String mod: modules) {
			if (modSet.contains(argv[index]) == false) {
			    if (sbmod.length() > 0) {
				sbmod.append(",");
			    }
			    // System.out.println("adding module " + mod);
			    sbmod.append(mod);
			    addedModules.add(mod);
			    /*
			    System.out.println("addedModules.size() = "
					       + addedModules.size());
			    */
			    modSet.add(mod);
			}
		    }
		} else if (argv[index].equals("--codebase")) {
		    hasCodebase = true;
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    extendCodebase(argv[index], true);
		    for (String cb: URLPathParser.split(argv[index])) {
			if (!codebaseSet.contains(cb)) {
			    codebase.add(argv[index]);
			    codebaseSet.add(argv[index]);
			}
		    }
		} else if (argv[index].equals("--classpathCodebase")) {
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    extendCodebase(argv[index], false);
		    for (String cb: URLPathParser.split(argv[index])) {
			if (!classpathSet.contains(cb)) {
			    classpath.add(argv[index]);
			    classpathSet.add(argv[index]);
			}
		    }
		} else if (argv[index].equals("--animation")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    a2dName = argv[index];
		    // scriptMode = true;
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--image")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    imageMode = true;
		    imageArg = argv[index];
		    if (imageArg.equals("-")) {
			imageURI = null;
			image = null;
		    } else {
			try {
			    if (maybeURL(imageArg)
				&& !(imageArg.startsWith("file:")
				     && imageArg.endsWith(".epts"))) {
				URL imageURL = new URL(cdir.toURI().toURL(),
						       imageArg);
				imageURI = imageURL.toURI();
				image = ImageIO.read(imageURL);
			    } else {
				File ifile = null;
				if (imageArg.startsWith("file:")) {
				    ifile = new File(new URI(imageArg));
				} else {
				    ifile = new File(imageArg);
				}
				if (imageArg.endsWith(".epts")) {
				    iparser = new EPTSParser();
				    File parent = ifile.getParentFile();
				    if (parent != null) {
					System.setProperty
					    ("user.dir",
					     parent.getCanonicalPath());
				    }
				    iparser.setXMLFilename(imageArg);
				    iparser.parse
					(new FileInputStream(ifile));
				    if (iparser.hasScripts()) {
					String msg =
					    errorMsg("parserHasScripts");
					throw new Exception(msg);
				    }
				    imageURI = iparser.getImageURI();
				    image = iparser.getImage();
				    custom = iparser.usesCustom();
				    if (parent != null) {
					System.setProperty("user.dir",
							   initialcwd);
				    }
				} else {
				    File parent = ifile.getParentFile();
				    if (parent != null) {
					System.setProperty
					    ("user.dir",
					     parent.getCanonicalPath());
				    }
				    imageURI = ifile.getCanonicalFile().toURI();
				    image = ImageIO.read(ifile);
				}
			    }
			} catch (Exception ex) {
			    // image = null; imageURI = null;
			    String msg = errorMsg
				("readError", argv[index], ex.getMessage());
			    displayError(msg);
			    System.exit(1);
			}
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--port")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    try {
			port = Integer.parseInt(argv[index]);
		    } catch (Exception e) {
			displayError
			    (errorMsg("notInteger", argv[index]));
			System.exit(1);
					   
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stackTrace")) {
		    stackTrace = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--svg")) {
		    if (svg == false) {
			if (flatness != 0.0 || templateURL != null
			    || straight || elevate) {
			    displayError
				(errorMsg("noSVG"));
			    System.exit(1);
			}
		    }
		    svg = true;
		    limit = 0;
		    flatness = 0.0;
		    templateURL = new URL("resource:org/bzdev/epts/SVG");
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-color")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String css = parseArgument(argv[index-1], argv[index],
					       Color.class);
		    filterInfo.stroke = (css == null)? "none": css;
		    if (css != null) {
			filterInfo.draw = "true";
			filterInfo.drawColor = css;
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-gcs-mode")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.gcsMode =
			parseArgument(argv[index-1], argv[index],
				      Boolean.class);
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-cap")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.strokeCap =
			parseArgument(argv[index-1], argv[index],
				      BasicStrokeParm.Cap.class);
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-dash-incr")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.dashIncrement =
			parseArgument(argv[index-1], argv[index],
				      Double.class,
				      "0.0", true, null, false);
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-dash-pattern")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.dashPattern = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-dash-phase")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.dashPhase =
			parseArgument(argv[index-1],argv[index],
				      Double.class,
				      null, true, null, false);
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-join")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.strokeJoin =
			parseArgument(argv[index-1], argv[index],
				      BasicStrokeParm.Join.class);
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-miter-limit")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.miterLimit =
			parseArgument(argv[index-1],argv[index],
				      Double.class,
				      "1.0", true, null, false);
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-width")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.strokeWidth = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--fill-color")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String css = parseArgument(argv[index-1], argv[index],
					       Color.class);
		    filterInfo.fillSVG = (css == null)? "none": css;
		    if (css != null) {
			filterInfo.fill = "true";
			filterInfo.fillColor = css;
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--zorder")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.zorder =
			parseArgument(argv[index-1],argv[index],
				      Long.class,
				      null, false , null, false);
		    filterInfo.zo = Long.parseLong(argv[index]);
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--web")) {
		    webserverOnly = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--map")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    try {
			String mname = argv[index];
			if  (mname.startsWith("resource:")) {
			    mname = mname.replaceFirst
				("resource:", "resource:org/bzdev/epts/");
			}
			if (mname.startsWith("resource:")
			    || mname.startsWith("file:")
			    || mname.startsWith("http:")
			    || mname.startsWith("https:")
			    || mname.startsWith("ftp:")) {
			    URL mapURL = new URL(mname);
			    map = createMap(mapURL.openStream());
			} else {
			    File mapFile = new File(mname);
			    if (mapFile.canRead()) {
				map = createMap(new FileInputStream(mapFile));
			    } else {
				displayError
				    (errorMsg("cannotRead", argv[index]));
				System.exit(1);
			    }
			}
		    } catch (Exception e) {
			displayError
			    (errorMsg("ioError", argv[index], e.getMessage()));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--package")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    pkg = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--pname")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    pname = argv[index].trim();
		    if (pname.indexOf(':') != -1) {
			pnameArray = pname.split(":", 2);
			pname = pnameArray[0];
			pnameArray = pnameArray[1].split(",");
			for (int i = 0; i < pnameArray.length; i++) {
			    pnameArray[i] = pnameArray[i].trim();
			}
		    } else {
			pnameArray = new String[1];
			pnameArray[0] = pname;
		    }
		    pnameInfoList.add(new PnameInfo(pname, pnameArray));
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--flatness")) {
		    if (svg) {
			displayError
			    (errorMsg("svgmode","--flatness"));
			System.exit(1);
		    }
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    flatness = convert(argv[index]);
		    if (flatness < 0)  {
			throw new IllegalArgumentException
			    (errorMsg("negative", argv[index]));
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--limit")) {
		    if (svg) {
			displayError
			    (errorMsg("svgmode","--limit"));
			System.exit(1);
		    }
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    limit = Integer.parseInt(argv[index]);
		    if (limit < 0) {
			throw new IllegalArgumentException
			    (errorMsg("negative", argv[index]));
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--straight")) {
		    if (svg) {
			displayError
			    (errorMsg("svgmode","--straight"));
			System.exit(1);
		    }
		    straight = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--elevate"))  {
		    if (svg) {
			displayError
			    (errorMsg("svgmode","--elevate"));
			System.exit(1);
		    }
		    elevate = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--gcs")) {
		    if (svg) {
			displayError
			    (errorMsg("svgmode","--gcs"));
			System.exit(1);
		    }
		    gcs = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--template")) {
		    if (svg) {
			displayError
			    (errorMsg("svgmode","--template"));
			System.exit(1);
		    }
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String tname = argv[index];
			if  (tname.startsWith("resource:")) {
			    tname = tname.replaceFirst
				("resource:", "resource:org/bzdev/epts/");
			}
		    if (tname.startsWith("resource:")
			|| tname.startsWith("file:")
			|| tname.startsWith("http:")
			|| tname.startsWith("https:")
			|| tname.startsWith("ftp:")) {
			templateURL = new URL(tname);
		    } else {
			templateURL = (new File(tname)).toURI().toURL();
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].startsWith("--template:")) {
		    String resource = argv[index].substring(11);
		    String tname = "resource:org/bzdev/epts/" + resource;
		    templateURL = new URL(tname);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--tdef")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String tdef = argv[index];
		    int tdefEqInd = tdef.indexOf('=');
		    if (tdefEqInd > -1) {
			String tdefName = tdef.substring(0, tdefEqInd).trim();
			String tdefValue = tdef.substring(tdefEqInd+1);
			if (isReservedTdef(tdefName)) {
			    displayError(errorMsg("reservedTDef", tdefName));
			    System.exit(1);
			}
			if (tdefValue.length() > 0) {
			    storeTDef(tdefName, tdefValue);
			}
		    } else {
			if (tdef.indexOf(":") > 0) {
			    displayError(errorMsg("illformedArg", tdef));
			    System.exit(1);
			} else {
			    storeTDef(tdef, null);
			}
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--tname")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String tname = argv[index].trim();
		    String[] tnameArray;
		    if (tname.indexOf(':') != -1) {
			tnameArray = tname.split(":", 2);
			tname = tnameArray[0];
			tnameArray = tnameArray[1].split(",");
			for (int i = 0; i < tnameArray.length; i++) {
			    tnameArray[i] = tnameArray[i].trim();
			}
		    } else {
			tnameArray = new String[1];
			tnameArray[0] = tname;
		    }
		    filterInfo.name = tname;
		    filterInfo.nameArray = tnameArray;
		    filterInfoList.add(filterInfo);
		    filterInfo = new FilterInfo();
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("-o")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    outName = argv[index];
		    if (!outName.equals("-")) {
			File f = new File(outName);
			f = f.getCanonicalFile();
			if (f.exists()) {
			    if (f.canWrite() == false) {
				displayError
				    (errorMsg("cannotWrite", outName));
				System.exit(1);
			    }
			} else {
			    if (f.getParentFile().canWrite() == false) {
				displayError
				    (errorMsg("cannotWrite", outName));
				System.exit(1);
			    }
			}
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--winding-rule")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String windingRule = argv[index];
		    if (windingRule.equals("evenodd")) {
			filterInfo.windingRule = "WIND_EVEN_ODD";
		    } else if (windingRule.equals("nonzero")) {
			filterInfo.windingRule = "WIND_NON_ZERO";
		    } else {
			String msg = errorMsg("badWindingRule", windingRule);
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--boolean")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String[] tokens = argv[index].split("=", 2);
		    if (tokens.length == 2) {
			try {
			    Boolean value;
			    if (tokens[1].equals("true")) {
				value = Boolean.TRUE;
			    } else if (tokens[1].equals("false")) {
				value = Boolean.FALSE;
			    } else {
				String msg = errorMsg("notBoolean2",
						      argv[index-1], tokens[0]);
				throw new Exception(msg);
			    }
			    bindings.add(new NameValuePair(tokens[0], value));
			} catch (Exception e) {
			    String msg = errorMsg("badBoolean",
						  argv[index-1], tokens[1]);
			    displayError(msg);
			    System.exit(1);
			}
		    } else {
			displayError(errorMsg("noEqual", argv[index-1]));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--int")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String[] tokens = argv[index].split("=", 2);
		    if (tokens.length == 2) {
			try {
			    Integer value = Integer.parseInt(tokens[1]);
			    bindings.add(new NameValuePair(tokens[0], value));
			} catch (Exception e) {
			    String msg = errorMsg("illegalToken",
						  tokens[1],
						  argv[index-1],
						  e.getMessage());
			    displayError(msg);
			    System.exit(1);
			}
		    } else {
			displayError(errorMsg("noEqual", argv[index-1]));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--double")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String[] tokens = argv[index].split("=", 2);
		    if (tokens.length == 2) {
			try {
			    Double value = convert(tokens[1]);
			    bindings.add(new NameValuePair(tokens[0], value));
			} catch (Exception e) {
			    String msg = errorMsg("illegalToken",
						  tokens[1],
						  argv[index-1],
						  e.getMessage());
			    displayError(msg);
			    System.exit(1);
			}
		    } else {
			displayError(errorMsg("noEqual", argv[index-1]));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--string")) {
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String[] tokens = argv[index].split("=", 2);
		    if (tokens.length == 2) {
			String value = tokens[1];
			bindings.add(new NameValuePair(tokens[0], value));
		    } else {
			displayError(errorMsg("noEqual", argv[index-1]));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--customUnits")) {
		    custom = true;
		} else if (argv[index].equals("--mksUnits")) {
		    custom = false;
		} else if (argv[index].equals("--gui")) {
		    guiMode = true;
		    argsList.add(argv[index]);
		}  else if (argv[index].equals("-w")) {
		    // Special case - we just want to open a black frame
		    // with a specified width.
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    width = Integer.parseInt(argv[index]);
		    imageMode = true;
		}  else if (argv[index].equals("-h")) {
		    // Special case - we just want to open a black frame
		    // with a specified height.
		    index++;
		    if (index == argv.length) {
			displayError
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    height = Integer.parseInt(argv[index]);
		    imageMode = true;
		} else if (argv[index].equals("--")) {
		    argsList.add(argv[index]);
		    jcontinue = false;
		} else if (argv[index].startsWith("-")) {
		    displayError(errorMsg("unknownOption", argv[index]));
		    System.exit(1);
		} else {
		    //System.out.println("argv[" + index + "] = "+argv[index]);
		    if (languageName == null) {
			String extension = processLanguageName(argv[index]);
			if (extension != null) {
			    languageName =
				Scripting.getLanguageNameByExtension(extension);
			    scriptMode = true;
			}
		    } else if (processLanguageName(argv[index]) != null) {
			scriptMode = true;
		    }
		    argsList.add(argv[index]);
		    if (!imageMode && targetList.isEmpty()) {
			processImageArg(argv[index], targetList, cdir);
			if (imageURI != null) {
			    imageMode = true;
			    if (guiMode && imageURI != null
				&& imageURI.getScheme().equals("file")) {
				File ifile = new File(imageURI);
				File parent = ifile.getParentFile();
				if (parent != null) {
				    initialcwdFile = parent.getCanonicalFile();
				    initialcwd = parent.getCanonicalPath();
				    System.setProperty("user.dir", initialcwd);
				}
			    }
			}
			// Infer script mode from first file-name argument,
			// which is not an image file. processLanguageName
			// will return a non-null value only if the file
			// has an extension suitable for a script.
			if (targetList.size() == 1) {
			    if (processLanguageName(argv[index]) != null) {
				if (scriptMode == false) {
				    scriptMode = true;
				    a2dName = "a2d";
				}
			    }
			}
		    } else {
			targetList.add(argv[index]);
		    }
		    jcontinue = false;
		}
	    } else {
		if (languageName == null) {
		    String extension = processLanguageName(argv[index]);
		    if (extension != null) {
			languageName =
			    Scripting.getLanguageNameByExtension(extension);
			scriptMode = true;
		    }
		} else if (processLanguageName(argv[index]) != null) {
		    scriptMode = true;
		}
		argsList.add(argv[index]);
		if (!imageMode && targetList.isEmpty()) {
		    processImageArg(argv[index], targetList, cdir);
		    if (targetList.isEmpty()) {
			imageMode = true;
			if (guiMode && imageURI != null
			    && imageURI.getScheme().equals("file")) {
			    File ifile = new File(imageURI);
			    File parent = ifile.getParentFile();
			    if (parent != null) {
				initialcwdFile = parent;
				initialcwd = parent.getCanonicalPath();
				System.setProperty("user.dir", initialcwd);
			    }
			}
		    }

		    // Infer script mode from first file-name argument,
		    // which is not an image file. processLanguageName
		    // will return a non-null value only if the file
		    // has an extension suitable for a script.
		    if (targetList.size() == 1) {
			if (processLanguageName(argv[index]) != null) {
			    if (scriptMode == false) {
				scriptMode = true;
				a2dName = "a2d";
			    }
			}
		    }
		} else {
		    targetList.add(argv[index]);
		}
	    }
	}

	if ((imageMode ^ scriptMode) == false) {
	    if (iparser == null && (imageMode || scriptMode)) {
		displayError(errorMsg("modes1"));
		displayError(errorMsg("modes2", imageArg));
		System.exit(1);
	    }
	}

	if (!scriptMode && imageMode && targetList.size() != 0) {
	    displayError(errorMsg("noImagesAndScripts"));
	    System.exit(1);
	}

	if (scriptMode && targetList.size() == 0) {
	    displayError(errorMsg("targetLength0"));
	    System.exit(1);
	}
	// We use a scripting language only if there is a script to
	// process.
	if (scriptMode == false) {
	    // System.out.println("setting language to null");
	    languageName = null;
	}
	if (languageName != null) {
	    // need to extend class path because scripting-language-independent
	    // class path entries in the configuration files may be needed for
	    // scripting languages to be recognized.
	    if (needInitialConfig) {
		readConfigFiles(null);
		if (System.getProperty("org.bzdev.epts.ifork") == null
		    && classpathExtended) {
		    ifork(argv);
		}
		needInitialConfig = false;
	    }
	    if (!Scripting.supportsLanguage(languageName)) {
		String ln =
		    Scripting.getLanguageNameByAlias(languageName);
		if (ln == null) {
		    String msg =
			(errorMsg("badScriptingLanguageName",languageName));
		    displayError(msg);
		    System.exit(1);
		} else {
		    languageName = ln;
		}
	    }
	}
	readConfigFiles(languageName);

	if (defs.size() > 0) {
	    String svalue = defs.getProperty("java.system.class.loader");
	    if (svalue != null && svalue.equals("-none-")) {
		defs.remove("java.system.class.loader");
	    }
	    for (String name: defs.stringPropertyNames()) {
		String value = defs.getProperty(name);
		jargsList.add("-D" + name + "=" + value);
	    }
	}

	EPTSParser parser = null;
	if (imageMode == false && scriptMode == false
	    && targetList.size() >= 1) {
	    String filename = targetList.get(0);
	    if (filename.endsWith(".epts")) {
		parser = createParsedParser(filename);
		if (outName == null && !alreadyForked) {
		    // we only run the scripts if there is no
		    // output file as the output file simply
		    // formats the tables.
		    for (String name: parser.getCodebase()) {
			StringBuilder sb = new StringBuilder();
			URL[] urls = null;
			try {
			    urls = URLPathParser.getURLs(null, name,
							 ourCodebaseDir,
							 sb);
			} catch (Exception urle) {
			    if (sb.length() != 0) {
				displayError(sb.toString());
				System.exit(1);
			    }
			}
			if (urls.length != 1) {
			    String title = errorMsg("errorTitle");
			    String msg = errorMsg("multipleURLs");
			    SwingErrorMessage.display(null, title, msg);
			    System.exit(1);
			}
			if (name.startsWith(".../")) {
			    extendCodebase(name, true);
			    // URLClassLoaderOps.addURLs(urls);
			} else {
			    String title = errorMsg("acceptTitle");
			    String msg = errorMsg("acceptURL", name);
			    if (JOptionPane.OK_OPTION ==
				JOptionPane
				.showConfirmDialog(null, msg, title,
						   JOptionPane
						   .YES_NO_OPTION,
						   JOptionPane
						   .QUESTION_MESSAGE)) {
				extendCodebase(name,true);
				// URLClassLoaderOps.addURLs(urls);
			    }
			}
		    }
		}
	    } else {
		displayError(errorMsg("eptsFileExpected"));
		System.exit(1);
	    }
	}



	// fork if a different JVM executable was requested, if we
	// have some arguments that must be passed to the JVM.
	boolean mustFork = (jargsList.size() > 0 || javacmd != JAVACMD);
	String policyFile = defs.getProperty("java.security.policy");
	// If we don't have a policy file, fork if scriptMode is true or
	// if both scriptMode and imageMode are false. The later can
	// occur when restoring a saved state, but we won't know the
	// saved state runs a script until the parser is run, so it is
	// easier to just fork to be safe.
	if (policyFile == null && scriptMode) mustFork = true;
	if (policyFile == null && !scriptMode && !imageMode) mustFork = true;
	if (!alreadyForked &&  mustFork) {
	    fork(jargsList, argsList, policyFile, dryrun);
	} else if (dryrun) {
	    jargsList.add(0, "EPTS");
	    jargsList.add(0, sbcp.toString());
	    jargsList.add(0, "-classpath");
	    jargsList.add(0, "java");
	    jargsList.addAll(argsList);
	    for (String s: jargsList) {
		System.out.print(s);
		System.out.print(" ");
	    }
	    System.out.println();
	    System.exit(0);
	}

	/*
	try {
	    org.bzdev.protocols.Handlers.enable();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
	*/


	/*
	// not needed as we now call extendCodebase when the codebases are
	// read.
	if (codebase.size() > 0) {
	    for (String cp: codebase) {
		try {
		    URL[] urls = URLPathParser.getURLs(null, cp,
						       ourCodebaseDir,
						       System.err);
		    // Make sure this occurs only after we restarted,
		    // so the codebase is not needed as we'll put everything
		    // on the class path or module path
		    // URLClassLoaderOps.addURLs(urls);
		} catch (MalformedURLException e) {
		    System.err.append(errorMsg("codebaseError", cp) + "\n");
		    System.exit(1);
		}
	    }
	}
	*/
	if (webserverOnly) {
	    EPTSWindow.setPort(port);
	    return;
	}

	if (!fdrUsed && !imageMode && !scriptMode && targetList.size() == 0) {
	    // No arguments that would indicate a saved state, image file,
	    // or script files, so open a dialog box prompting for
	    // the input to use (an image file)

	    SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			File f;
			OpeningFileChooser ofc = new
			    OpeningFileChooser(localeString("EPTSSavedStates"),
					       new String[] {
						   "epts"
					       });
			switch(ofc.showOpeningDialog(null, cdir)) {
			case OpeningFileChooser.DIMENSIONS_OPTION:
			    width = ofc.getWidth();
			    height = ofc.getHeight();
			    BufferedImage bi = new
				BufferedImage(width,height,
					      BufferedImage.TYPE_INT_ARGB_PRE);
			    Graphics2D g2d = bi.createGraphics();
			    g2d.setBackground(Color.WHITE);
			    g2d.clearRect(0, 0, width, height);
			    imageMode = true;
			    imageURI = null;
			    image = bi;
			    break;
			case OpeningFileChooser.APPROVE_OPTION:
			    f = ofc.getSelectedFile();
			    try {
				String[] extensions =
				    ImageIO.getReaderFileSuffixes();
			    Set<String> extensionSet =
				Scripting.getExtensionSet();
			    String[] sextensions =
				extensionSet.toArray
				(new String[extensionSet.size()]);
			    ArrayList<String> allExtensionsList =
				new ArrayList<>();
			    allExtensionsList.add("epts");
			    for (String ext: extensions) {
				allExtensionsList.add(ext);
			    }
			    for (String ext: sextensions) {
				allExtensionsList.add(ext);
			    }
			    String[] allExtensions = allExtensionsList
				.toArray(new String[allExtensionsList.size()]);
			    String eptsExtensions[] = {"epts"};
				if (extensionMatch(f, eptsExtensions)) {
				    targetList.add(f.getCanonicalPath());
				    break;
				} else if (extensionMatch(f, extensions)) {
				    imageMode = true;
				    imageURI = f.toURI();
				    image = ImageIO.read(f);
				    break;
				} else if (extensionMatch(f, sextensions)) {
				    // script case.
				    scriptMode = true;
				    String pathname = f.getCanonicalPath();
				    argsList.add(pathname);
				    if (!alreadyForked) {
					// need to add
					// -Djava.security.policy=...
					// if not already there
					fork(jargsList, argsList,
					     policyFile, dryrun);
				    } else {
					targetList.add(pathname);
				    }
				    String ext =
					processLanguageName(pathname);
				    if (a2dName == null) a2dName = "a2d";
				    if (ext != null) {
					languageName =
					    Scripting
					    .getLanguageNameByExtension
					    (ext);
				    }
				    break;
				} else {
				    SwingErrorMessage.setComponent(null);
				    SwingErrorMessage.display
					(null, null,
					 errorMsg("unrecognizedFNE"));
				}
			    }  catch (Exception e) {
				SwingErrorMessage.setComponent(null);
				SwingErrorMessage.display(e);
				System.exit(1);
			    }
			    break;
			case OpeningFileChooser.CANCEL_OPTION:
			    System.exit(0);
			}
			/*
			for(;;) {
			    JFileChooser fc = new JFileChooser(cdir);
			    fc.setAcceptAllFileFilterUsed(false);
			    String[] extensions =
				ImageIO.getReaderFileSuffixes();
			    FileNameExtensionFilter filter =
				new FileNameExtensionFilter
				(localeString("Images"), extensions);
			    Set<String> extensionSet =
				Scripting.getExtensionSet();
			    String[] sextensions =
				extensionSet.toArray
				(new String[extensionSet.size()]);
			    ArrayList<String> allExtensionsList =
				new ArrayList<>();
			    allExtensionsList.add("epts");
			    for (String ext: extensions) {
				allExtensionsList.add(ext);
			    }
			    for (String ext: sextensions) {
				allExtensionsList.add(ext);
			    }
			    String[] allExtensions = allExtensionsList
				.toArray(new String[allExtensionsList.size()]);
			    FileNameExtensionFilter afilter =
				new FileNameExtensionFilter
				(localeString("StatesImagesScripts"),
				 allExtensions);
			    String eptsExtensions[] = {"epts"};
			    FileNameExtensionFilter efilter =
				new FileNameExtensionFilter
				 (localeString("EPTSSavedStates"),
				  eptsExtensions);
			    fc.addChoosableFileFilter(afilter);
			    fc.addChoosableFileFilter(efilter);
			    fc.addChoosableFileFilter(filter);
			    FileNameExtensionFilter sfilter =
				new FileNameExtensionFilter
				(localeString("Scripts"), sextensions);
			    fc.addChoosableFileFilter(sfilter);
			    int status = fc.showOpenDialog(null);
			    if (status == JFileChooser.APPROVE_OPTION) {
				File f = fc.getSelectedFile();
				try {
				    if (extensionMatch(f, eptsExtensions)) {
					targetList.add(f.getCanonicalPath());
					break;
				    } else if (extensionMatch(f, extensions)) {
					imageMode = true;
					imageURI = f.toURI();
					image = ImageIO.read(f);
					break;
				    } else if (extensionMatch(f, sextensions)) {
					// script case.
					scriptMode = true;
					String pathname = f.getCanonicalPath();
					argsList.add(pathname);
					if (!alreadyForked) {
					    // need to add
					    // -Djava.security.policy=...
					    // if not already there
					    fork(jargsList, argsList,
						 policyFile, dryrun);
					} else {
					    targetList.add(pathname);
					}
					String ext =
					    processLanguageName(pathname);
					if (a2dName == null) a2dName = "a2d";
					if (ext != null) {
					    languageName =
						Scripting
						.getLanguageNameByExtension
						(ext);
					}
					break;
				    } else {
					SwingErrorMessage.setComponent(null);
					SwingErrorMessage.display
					    (null, null,
					     errorMsg("unrecognizedFNE"));
				    }
				    continue;
				} catch (Exception e) {
				    SwingErrorMessage.setComponent(null);
				    SwingErrorMessage.display(e);
				    System.exit(1);
				}
			    } else {
				// So we try a blank image
				imageMode = true;
				break;
			    }
			}
			*/
		    }
		});
	} else if (fdrUsed && !imageMode && !scriptMode
		   && targetList.size() == 0) {
	    imageMode = true;
	}
	if (!imageMode && scriptMode && targetList.size() > 1 &&
	    targetList.get(0).endsWith(".epts") && outName == null) {
	    // if the EPTS saved state includes an image file,
	    // treat this as if the --image argument was used, but
	    String fileArg = targetList.remove(0);
	    if (fileArg.startsWith("file")) {
		eptsFile = new File(new URI(fileArg));
	    } else {
		eptsFile = new File(fileArg);
	    }
	    iparser = new EPTSParser();
	    File parent = eptsFile.getParentFile();
	    if (parent != null) {
		System.setProperty("user.dir", parent.getCanonicalPath());
	    }
	    iparser.setXMLFilename(fileArg);
	    iparser.parse(new FileInputStream(eptsFile));
	    if (iparser.hasScripts()) {
		throw new Exception(errorMsg("parserHasScripts"));
	    }
	    custom = iparser.usesCustom();
	    if (iparser.imageURIExists()) {
		imageURI = iparser.getImageURI();
		image = iparser.getImage();
		imageMode = true;
	    }
	}
	if (imageMode) {
	    if (imageURI == null) {
		/*
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
			    InputVerifier iv = new InputVerifier() {
				    @Override
				    public boolean verify(JComponent input) {
					WholeNumbTextField wntf
					    = (WholeNumbTextField) input;
					return (wntf.getValue() > 0);
				    }
				};
			    WholeNumbTextField wtf = new WholeNumbTextField(8);
			    WholeNumbTextField htf = new WholeNumbTextField(8);
			    wtf.setDefaultValue(0);
			    htf.setDefaultValue(0);
			    wtf.setText("1024");
			    htf.setText("1024");
			    wtf.setInputVerifier(iv);
			    htf.setInputVerifier(iv);
			    JPanel panel = new JPanel();
			    GridBagLayout gridbag = new GridBagLayout();
			    GridBagConstraints c = new GridBagConstraints();
			    panel.setLayout(gridbag);
			    JLabel wl = new JLabel(localeString("widthLabel"));
			    JLabel hl = new JLabel(localeString("heightLabel"));
			    c.insets = new Insets(5, 5, 5, 5);
			    c.ipadx = 5;
			    c.ipady = 5;
			    c.anchor = GridBagConstraints.LINE_START;
			    c.gridwidth = 1;
			    gridbag.setConstraints(wl, c);
			    panel.add(wl);
			    c.gridwidth = GridBagConstraints.REMAINDER;
			    gridbag.setConstraints(wtf, c);
			    panel.add(wtf);
			    c.gridwidth = 1;
			    gridbag.setConstraints(hl, c);
			    panel.add(hl);
			    c.gridwidth = GridBagConstraints.REMAINDER;
			    gridbag.setConstraints(htf, c);
			    panel.add(htf);
			    int status = JOptionPane.showConfirmDialog
				(null, panel,
				 localeString("widthHeightTitle"),
				 JOptionPane.OK_CANCEL_OPTION,
				 JOptionPane.QUESTION_MESSAGE);
			    width = wtf.getValue();
			    height = htf.getValue();
			    if (status != JOptionPane.OK_OPTION) {
				System.exit(0);
			    }
			}
		    });
		BufferedImage bi = new
		    BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g2d = bi.createGraphics();
		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);
		image = bi;
		*/
		if (image == null) {
		    BufferedImage bi = new
			BufferedImage(width,height,
				      BufferedImage.TYPE_INT_ARGB_PRE);
		    Graphics2D g2d = bi.createGraphics();
		    g2d.setBackground(Color.WHITE);
		    g2d.clearRect(0, 0, width, height);
		    image = bi;
		}
	    } else {
		// image already exists and was read.
		width = image.getWidth(null);
		height = image.getHeight(null);
	    }
	    EPTSWindow.setPort(port);
	    if (scriptMode) {
		if (iparser == null) {
		    ScriptingEnv se = new ScriptingEnv(languageName, a2dName,
						       width, height);
		    Graph graph = doScripts(se, bindings, targetList, null);
		    // null returned only if a console was shown.
		    // so we print a message to stderr just in case
		    // and then return. The application will exit when
		    // the user closes the console.
		    if ((console != null) && (graph == null)) {
			appendFinalMsg();
			return;
		    }
		    new EPTSWindow(graph, bindings, targetList, custom,
				   se, null,
				   image, imageURI, null, null);
		} else {
		    double userDist = iparser.getUserSpaceDistanceMeters();
		    double gcsDist = iparser.getGcsDistanceMeters();
		    RefPointName rpn = iparser.getRefPoint();
		    double xo = iparser.getXRefpointDouble();
		    double yo = iparser.getYRefpointDouble();
		    ScriptingEnv se = new ScriptingEnv(languageName, a2dName,
						       width, height,
						       userDist, gcsDist,
						       rpn, xo, yo);
		    Graph graph = doScripts(se, bindings, targetList, null);
		    // null returned only if a console was shown.
		    // so we print a message to stderr just in case
		    // and then return. The application will exit when
		    // the user closes the console.
		    if ((console != null) && (graph == null)) {
			appendFinalMsg();
			return;
		    }
		    // If no eptsFile, we are just reading a saved state
		    // to copy the image.
		    PointTMR rows[] = (eptsFile == null)? null:
			iparser.getRows();
		    new EPTSWindow(graph, bindings, targetList, custom,
				   se, rows,
				   image, imageURI, iparser, eptsFile);
		}
	    } else if (iparser != null) {
		new EPTSWindow(null, null, null, iparser.usesCustom(),
			       null, null,
			       image, imageURI, iparser, null);
	    } else {
		new EPTSWindow(image, imageURI);
	    }
	} else if (scriptMode) {
	    ScriptingEnv se = new ScriptingEnv(languageName, a2dName);
	    Graph graph = doScripts(se, bindings, targetList, null);
	    // null returned only if a console was shown.
	    // so we print a message to stderr just in case
	    // and then return. The application will exit when
	    // the user closes the console.
	    if ((console != null) && (graph == null)) {
		appendFinalMsg();
		return;
	    }
	    EPTSWindow.setPort(port);
	    new EPTSWindow(graph, bindings, targetList, custom, se, null,
			   null, null, null, null);
	    //			   languageName, a2dName, null);
	} else if (targetList.size() >= 1) {
	    // EPTSParser parser = new EPTSParser();
	    String filename = targetList.get(0);
	    if (filename.endsWith(".epts")) {
		try {
		    if (parser == null) {
			// parser is initialized after tests that replicates
			// the ones that got us to this branch.
			throw new UnexpectedExceptionError();
		    }
		    // parser = createParsedParser(filename);
		    /*
		    if (filename.startsWith("file:")) {
			filename =
			    (new File(new URI(filename))).getCanonicalPath();
		    } else if (maybeURL(filename)) {
			displayError
			    (errorMsg("urlForSavedState", filename));
			System.exit(1);
		    }
		    File parent  = new File(filename).getParentFile();
		    if (parent != null) {
			System.setProperty("user.dir",
					   parent.getCanonicalPath());
		    }
		    parser.setXMLFilename(filename);
		    parser.parse(new FileInputStream(filename));
		    */
		    if (outName == null) {
			EPTSWindow.setPort(port);
			if (parser.hasScripts()) {
			    List<NameValuePair> pbindings
				= parser.getBindings();
			    List<String> ptargetList
				= parser.getTargetList();
			    String languageName = parser.getLanguageName();
			    String animationName = parser.getAnimationName();
			    imageURI = parser.getImageURI();
			    image = parser.getImage();
			    ScriptingEnv se;
			    RefPointName rpn = parser.getRefPoint();
			    double xo = parser.getXRefpointDouble();
			    double yo = parser.getYRefpointDouble();
			    double userdist =
				parser.getUserSpaceDistanceMeters();
			    double gcsdist = parser.getGcsDistanceMeters();
			    if (image == null) {
				se = new ScriptingEnv(languageName,
						      animationName,
						      parser.getWidth(),
						      parser.getHeight(),
						      userdist, gcsdist,
						      rpn, xo, yo);
			    } else {
				se = new ScriptingEnv(languageName,
						      animationName,
						      image.getWidth(null),
						      image.getHeight(null),
						      userdist, gcsdist,
						      rpn, xo, yo);
			    }
			    ArrayList<NameValuePair> bl =
				new ArrayList<>(pbindings.size()
						+ bindings.size());
			    /*
			    ArrayList<String> tl =
				new ArrayList<>(ptargetList.size()
						+ targetList.size());
			    */
			    bl.addAll(pbindings);
			    bl.addAll(bindings);
			    // tl.addAll(ptargetList);
			    targetList.remove(0);
			    // tl.addAll(targetList);
			    Graph graph = doScripts(se, bl, ptargetList,
						    targetList);
			    // null returned only if a console was shown.
			    // so we print a message to stderr just in case
			    // and then return. The application will exit when
			    // the user closes the console.
			    if ((console != null) && (graph == null)) {
				appendFinalMsg();
				return;
			    }
			    new EPTSWindow(graph, pbindings, ptargetList,
					   parser.usesCustom(),
					   se,
					   // languageName, animationName,
					   parser.getRows(),
					   image, imageURI, parser,
					   new File(filename));
			} else {
			    if (targetList.size() > 1) {
				// image with scripts embellishing it.
				imageURI = parser.getImageURI();
				image = parser.getImage();
				ScriptingEnv se;
				RefPointName rpn = parser.getRefPoint();
				double xo = parser.getXRefpointDouble();
				double yo = parser.getYRefpointDouble();
				double userdist =
				    parser.getUserSpaceDistanceMeters();
				double gcsdist = parser.getGcsDistanceMeters();
				se = new ScriptingEnv(languageName,
						      null,
						      image.getWidth(null),
						      image.getHeight(null),
						      userdist, gcsdist,
						      rpn, xo, yo);
				targetList.remove(0);
				Graph graph = doScripts(se, bindings, null,
							targetList);
				// null returned only if a console was shown.
				// so we print a message to stderr just in case
				// and then return. The application will
				// exit when the user closes the console.
				if ((console != null) && (graph == null)) {
				    appendFinalMsg();
				    return;
				}
				new EPTSWindow(graph, bindings, targetList,
					       parser.usesCustom(),
					       se,
					       parser.getRows(),
					       image, imageURI, parser,
					       new File(filename));
			    } else {
				new EPTSWindow(parser, new File(filename));
			    }
			}
		    } else if (targetList.size() == 1) {
			TemplateProcessor.KeyMap emptyMap
			    = new TemplateProcessor.KeyMap();
			OutputStream os;
			if (outName.equals("-")) {
			    os = System.out;
			} else {
			    os = new FileOutputStream(outName);
			}
			PointTableModel ptmodel = new PointTableModel(null);
			for (PointTMR row: parser.getRows()) {
			    ptmodel.addRow(row);
			}
			filterInfoList.sort
			    (new Comparator<FilterInfo>() {
				    public int compare(FilterInfo o1,
						       FilterInfo o2)
				    {
					if (o1.zo < o2.zo) {
					    return -1;
					} else if (o1.zo == o2.zo) {
					    return 0;
					} else {
					    return 1;
					}
				    }
				});
			TemplateProcessor tp;
			if (pname != null) {
			    TemplateProcessor.KeyMap kmap =
				new TemplateProcessor.KeyMap();
			    TemplateProcessor.KeyMapList kmaplist =
				new TemplateProcessor.KeyMapList();
			    for (PnameInfo info: pnameInfoList) {
				kmaplist.add(EPTSWindow.getPathKeyMap
					     (ptmodel,
					      info.name, info.nameArray,
					      flatness, limit, straight,
					      elevate, gcs));
			    }
			    kmap.put("paths", kmaplist);
			    kmap.put("width", "" + parser.getWidth());
			    kmap.put("height", "" + parser.getHeight());
			    addDefinitionsTo(kmap);
			    tp  = new TemplateProcessor(kmap);
			} else if (svg) {
			    TemplateProcessor.KeyMap svgmap =
				new TemplateProcessor.KeyMap();
			    svgmap.put("width", "" + parser.getWidth());
			    double height = parser.getHeight();
			    svgmap.put("height", "" + height);
			    TemplateProcessor.KeyMapList kmaplist =
				new TemplateProcessor.KeyMapList();
			    TemplateProcessor.KeyMapList lkmaplist =
				new TemplateProcessor.KeyMapList();
			    Map<String,TemplateProcessor.KeyMap> lmap
				= ptmodel.getLocationMap(height, gcs);
			    for (FilterInfo info: filterInfoList) {
				if (lmap.containsKey(info.name)) {
				    // System.out.println("saw " + info.name);
				    lkmaplist.add(lmap.get(info.name));
				    continue;
				}
				TemplateProcessor.KeyMap kmap
				    = EPTSWindow.getPathKeyMap(ptmodel,
							       info.name,
							       info.nameArray,
							       flatness, limit,
							       straight,
							       elevate,
							       gcs);
				kmap.put("fillRule", info.fillRule);
				kmap.put("stroke", info.stroke);
				kmap.put("fill", info.fillSVG);
				if (info.strokeCap != null) {
				    kmap.put("strokeCap",
					     info.strokeCap.toLowerCase());
				    kmap.put("hasStrokeCap", emptyMap);
				}
				if (info.dashPhase != null) {
				    kmap.put("dashPhase", info.dashPhase);
				    kmap.put("hasDashPhase", emptyMap);
				}
				if (info.dashPattern != null &&
				    info.dashIncrement != null) {
				    String dashArray =
					getDashArray(info.dashPattern,
						     info.dashIncrement);
				    if (dashArray != null) {
					kmap.put("dashArray", dashArray);
					kmap.put("hasDashArray", emptyMap);
				    }
				}
				if (info.strokeJoin != null) {
				    kmap.put("strokeJoin",
					     info.strokeJoin.toLowerCase());
				    kmap.put("hasStrokeJoin", emptyMap);
				}
				if (info.miterLimit != null) {
				    kmap.put("miterLimit", info.miterLimit);
				    kmap.put("hasMiterLimit", emptyMap);
				}
				if (info.strokeWidth != null) {
				    kmap.put("strokeWidth", info.strokeWidth);
				    kmap.put("hasStrokeWidth", emptyMap);
				}
				kmaplist.add(kmap);
			    }
			    svgmap.put("paths", kmaplist);
			    svgmap.put("locations", lkmaplist);
			    tp = new TemplateProcessor(svgmap);
			} else if (filterInfoList.size() > 0) {
			    FilterInfo[] filters =
				new FilterInfo[filterInfoList.size()];
			    filters = filterInfoList.toArray(filters);
			    TemplateProcessor.KeyMap kmap =
				ptmodel.getKeyMap(filters, map,
						  (double)parser.getHeight());
			    if (pkg != null) {
				kmap.put("package", pkg);
				kmap.put("packageDir", pkg.replace('.','/'));
				kmap.put("hasPackage", emptyMap);
			    }
			    if (moduleName != null) {
				kmap.put("module", moduleName);
			    }
			    if (clazz != null) {
				kmap.put("class", clazz);
				// for HTML image maps
				kmap.put("mapName", clazz);
			    } else {
				kmap.put("class", "GeneratedByEPTS");
			    }
			    if (isPublic) {
				kmap.put("public", "public");
				kmap.put("optSpace", " ");
			    }
			    addDefinitionsTo(kmap);
			    tp = new TemplateProcessor(kmap);
			} else {
			    TemplateProcessor.KeyMap kmap =
				ptmodel.getKeyMap(map,
						  (double)parser.getHeight());
			    // Fix it up by adding the filterInfo data
			    // that was recorded but not used due to the
			    // lack of a --tname option.  We replicate
			    // for each path statement in the list to be
			    // consistent with the --tname cases.
			    TemplateProcessor.KeyMapList kmlist =
				(TemplateProcessor.KeyMapList)
				kmap.get("items");
			    for (TemplateProcessor.KeyMap kmi: kmlist) {
				Object entry = kmi.get("pathStatement");
				if (entry == null) continue;
				TemplateProcessor.KeyMap km =
				    (TemplateProcessor.KeyMap)entry;

				if (filterInfo.windingRule != null) {
				    km.put("windingRule",
					   filterInfo.windingRule);
				    km.put("hasWindingRule", emptyMap);
				}

				km.put("draw", filterInfo.draw);
				km.put("fill", filterInfo.fill);

				if (filterInfo.draw.equals("true")
				    || filterInfo.fill.equals("true")) {
				    km.put("hasAttributes", emptyMap);
				}

				if (filterInfo.gcsMode != null) {
				    km.put("gcsMode", filterInfo.gcsMode);
				    km.put("hasGcsMode", emptyMap);
				}
				if (filterInfo.drawColor != null) {
				    km.put("drawColor", filterInfo.drawColor);
				    km.put("hasDrawColor", emptyMap);
				}
				if (filterInfo.fillColor != null) {
				    km.put("fillColor", filterInfo.fillColor);
				    km.put("hasFillColor", emptyMap);
				}
				if (filterInfo.strokeCap != null) {
				    km.put("strokeCap", filterInfo.strokeCap);
				    km.put("hasStrokeCap", emptyMap);
				}
				if (filterInfo.dashIncrement != null) {
				    km.put("dashIncrement",
					     filterInfo.dashIncrement);
				    km.put("hasDashIncrement", emptyMap);
				}
				if (filterInfo.dashPhase != null) {
				    km.put("dashPhase", filterInfo.dashPhase);
				    km.put("hasDashPhase", emptyMap);
				}
				if (filterInfo.dashPattern != null) {
				    km.put("dashPattern",
					   filterInfo.dashPattern);
				    km.put("hasDashPattern", emptyMap);
				}
				if (filterInfo.strokeJoin != null) {
				    km.put("strokeJoin", filterInfo.strokeJoin);
				    km.put("hasStrokeJoin", emptyMap);
				}
				if (filterInfo.miterLimit != null) {
				    km.put("miterLimit", filterInfo.miterLimit);
				    km.put("hasMiterLimit", emptyMap);
				}
				if (filterInfo.strokeWidth != null) {
				    km.put("strokeWidth",
					   filterInfo.strokeWidth);
				    km.put("hasStrokeWidth", emptyMap);
				}
				if (filterInfo.zorder != null) {
				    km.put("zorder", filterInfo.zorder);
				    km.put("hasZorder", emptyMap);
				}
			    }
			    if (pkg != null) {
				kmap.put("package", pkg);
				kmap.put("packageDir", pkg.replace('.','/'));
				kmap.put("hasPackage", emptyMap);
			    }
			    if (moduleName != null) {
				kmap.put("module", moduleName);
			    }
			    if (clazz != null) {
				kmap.put("class", clazz);
				kmap.put("mapName", clazz);
			    } else {
				kmap.put("class", "GeneratedByEPTS");
			    }
			    if (isPublic) {
				kmap.put("public", "public");
				kmap.put("optSpace", " ");
			    }
			    addDefinitionsTo(kmap);
			    tp = new TemplateProcessor(kmap);
			}
			tp.processURL(templateURL, "UTF-8", os);
			os.flush();
			System.exit(0);
		    } else {
			displayError(errorMsg("tooManyArgs"));
			System.exit(1);
		    }
		} catch (Exception e) {
		    e.printStackTrace(System.err);
		    System.exit(1);
		}
	    } else {
		displayError(errorMsg("eptsFileExpected"));
		System.exit(1);
	    }
	} else {
	    displayError(errorMsg("tooManyArgs"));
	    System.exit(1);
	}
    }

    private static List<Image> iconList = new LinkedList<Image>();

    public static List<Image> getIconList() {return iconList;}

    private static String[] iconNames = {
	"eptsicon16.png",
	"eptsicon20.png",
	"eptsicon22.png",
	"eptsicon24.png",
	"eptsicon32.png",
	"eptsicon36.png",
	"eptsicon48.png",
	"eptsicon64.png",
	"eptsicon72.png",
	"eptsicon96.png",
	"eptsicon128.png",
	"eptsicon192.png",
	"eptsicon256.png",
	"eptsicon512.png"
    };

    static {
	try {
	    for (String iconName: iconNames) {
		iconList.add(new
			     ImageIcon((EPTS.class.getResource(iconName)))
			     .getImage());
	    }
	} catch (Exception e) {
	    System.err.println("could not initilize icons");
	}
    }


    public static void main(String argv[]) {
	try {
	    String url  = (new File(ourCodebase)).toURI().toURL().toString();
	    String path = url + "|jar:" + url + "!/org/bzdev/epts/";
	   System.setProperty("org.bzdev.protocols.resource.path", path);
	   org.bzdev.protocols.Handlers.enable();
	   init(argv);
	} catch (Exception e) {
	    SwingErrorMessage.display(e);
	    if (stackTrace) {
		e.printStackTrace();
	    }
	    System.exit(1);
	}
    }
}
