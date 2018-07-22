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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.imageio.*;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bzdev.graphs.Colors;
import org.bzdev.graphs.Graph;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.net.URLClassLoaderOps;
import org.bzdev.net.URLPathParser;
import org.bzdev.obnaming.misc.BasicStrokeParm;
import org.bzdev.obnaming.misc.BasicStrokeParm.Cap;
import org.bzdev.obnaming.misc.BasicStrokeParm.Join;
import org.bzdev.scripting.Scripting;
import org.bzdev.swing.ErrorMessage;
import org.bzdev.swing.WholeNumbTextField;
import org.bzdev.util.SafeFormatter;
import org.bzdev.util.TemplateProcessor;

public class EPTS {

    public enum Mode {
	LOCATION, PATH_START, PATH_END
    }

    // resource bundle for messages used by exceptions and errors
    static ResourceBundle exbundle = ResourceBundle.getBundle("EPTS");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }

    static  String localeString(String key) {
	return exbundle.getString(key);
    }

    private static final String JAVACMD = "java";
    private static String javacmd = JAVACMD;

    private static final String dotDotDot = "...";
    private static final String dotDotDotSep = dotDotDot + File.separator;
    private static final String tildeSep = "~" + File.separator;
    private static String ourCodebase;
    private static String ourCodebaseDir;
    // private static String ourCodebase2;
    // private static String ourCodebaseDir2;
    static {
	try {
	    File f = (new File(EPTS.class.getProtectionDomain()
			       .getCodeSource().getLocation().toURI()))
		.getCanonicalFile();
	    ourCodebase = f.getCanonicalPath();
	    f = (new File(Scripting.class.getProtectionDomain()
			       .getCodeSource().getLocation().toURI()))
		.getCanonicalFile();
	    ourCodebaseDir = f.getParentFile().getCanonicalPath();
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
	    throw new Exception("\"" + pathname  + "\" - not a normal file");
	}
	if (!file.canRead()) {
	    throw new Exception("\"" + pathname + "\" - not readable");
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

    public static List<String> getCodebase() {
	return Collections.unmodifiableList(codebase);
    }

    private static StringBuilder sbcp = new StringBuilder();
    static {
	sbcp.append(ourCodebase);
    }
    static HashSet<String> sbcpAppended = new HashSet<>();

    private static void readConfigFiles(String languageName, String fileName) {
	File file = new File (fileName);
	if (!file.exists()) {
	    return;
	}
	if (!file.isFile()) {
	    System.err.println(errorMsg("notConfigFile", fileName));
	    System.exit(1);
	}

	if (!file.canRead()) {
	    System.err.println(errorMsg("configFileNotReadable", fileName));
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
			continue;
		    } else if (line.startsWith("%java")) {
			if (line.length() < 6) {
			    int lno = reader.getLineNumber();
			    String msg = errorMsg("directive", fileName, lno);
			    System.err.println(msg);
			    System.exit(1);
			}
			char ch = line.charAt(5);
			if (!Character.isSpaceChar(ch)) {
			    int lno = reader.getLineNumber();
			    String msg = errorMsg("directive", fileName, lno);
			    System.err.println(msg);
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
				mode = 3;
				langsection = false;
			    } else if (languageName.equals(language)) {
				langsection = true;
			    } else  {
				if (langsection) mode = 3;
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
			}
			if (langsearch || langsection) {
			    String[] parts = line.split("\\s*=\\s*", 2);
			    if (parts.length != 2) {
				int lno = reader.getLineNumber();
				String msg =
				    errorMsg("syntax", fileName, lno);
				System.err.println(msg);
				error = true;
			    } else {
				String name = parts[0];
				if (propertyNotAllowed(name) &&
				    !name.equals("java.ext.dirs")) {
				    // do not override standard Java properties
				    int lno = reader.getLineNumber();
				    String msg =
					errorMsg("propname",fileName,lno,name);
				    System.err.println(msg);
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
				mode = 3;
				langsection = false;
			    } else if (languageName.equals(language)) {
				langsection = true;
			    } else  {
				if (langsection) mode = 3;
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
				URLClassLoaderOps.addURL(url);
				if (!sbcpAppended.contains(line)) {
				    sbcp.append(File.pathSeparator);
				    sbcp.append(line);
				    sbcpAppended.add(line);
				}
			    } catch (Exception e) {
				int lno = reader.getLineNumber();
				String msg =
				    errorMsg("urlParse", fileName, lno);
				System.err.println(msg);
				System.exit(1);
			    }
			}
		    }
		}
	    } catch (Exception e) {
		int lno = reader.getLineNumber();
		String msg =
		    errorMsg("exceptionMsg", fileName, lno, e.getMessage());
		System.err.println(msg);
		System.exit(1);
	    }
	    reader.close();
	} catch (Exception er) {
	    System.err.println(errorMsg("scException", er.getMessage()));
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
	public FilterInfo() {}
    }

    static String getDashArray(String dashPattern, String dashIncrement)
	throws IllegalArgumentException
    {
	double dashIncr = Double.parseDouble(dashIncrement);
	float[] array = BasicStrokeParm.getDashArray(dashPattern, dashIncr);
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < array.length; i++) {
	    if (i > 0) sb.append(",");
	    sb.append("" + (double)array[i]);
	}
	return sb.toString();
    }


    static boolean stackTrace = false;
    static void printStackTrace(Throwable e, PrintStream out) {
	StackTraceElement[] elements = e.getStackTrace();
	for (StackTraceElement element: elements) {
	    out.println("    " + element);
	}
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

    static Graph doScripts(String languageName, String a2dName,
			   List<NameValuePair> bindings,
			   List<String> targetList)
    {
	final File cdir = new File(System.getProperty("user.dir"));
	System.out.println("doScript: "
			   + System.getProperty("java.security.policy"));
	ScriptingEnv se = new ScriptingEnv(languageName, a2dName);
	for (NameValuePair binding: bindings) {
	    se.putScriptObject(binding.getName(), binding.getValue());
	}
	String current = null;
	try {
	    for (String filename: targetList) {
		InputStream is;
		current = filename;
		if (mayBeURL(filename)) {
		    URL url = new URL(cdir.toURI().toURL(), filename);
		    is = url.openStream();
		} else {
		    is = new FileInputStream(filename);
		}
		Reader r = new InputStreamReader(is, "UTF-8");
		r = new BufferedReader(r);
		se.evalScript(filename, r);
	    }
	} catch (FileNotFoundException ef) {
	    System.err.println(errorMsg("readError", current, ef.getMessage()));
	    System.exit(1);
	} catch (IOException eio) {
	    String msg = errorMsg("readError", current, eio.getMessage());
	    System.err.println(msg);
	    System.exit(1);
	} catch (Exception e) {
	    if (stackTrace) {
		System.err.println(e.getClass().getName() + ": "
				   + e.getMessage());
		printStackTrace(e, System.err);
		Throwable cause = e.getCause();
		while (cause != null) {
		    System.err.println("---------");
		    System.err.println(cause.getClass().getName() + ": "
				       + cause.getMessage());
		    printStackTrace(cause, System.err);
		    cause = cause.getCause();
		}
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
		System.err.println(msg);
		while (cause != null) {
		    Class<?> clasz = cause.getClass();
		    Class<?> target =
			org.bzdev.obnaming.NamedObjectFactory
			.ConfigException.class;
		    String tn = errorMsg("ldotsConfigException");
		    String cn =(clasz.equals(target))? tn: clasz.getName();
		    msg = errorMsg("continued", cn, cause.getMessage());
		    System.err.println("  " + msg);
		    cause = cause.getCause();
		}
	    }
	    System.exit(1);
	}
	try {
	    se.drawGraph();
	    Graph graph = se.getGraph();
	    double scale = graph.getXScale();
	    double yscale = graph.getYScale();
	    if (scale != yscale) {
		System.err.println(errorMsg("xyScale", scale, yscale));
		System.exit(1);
	    }
	    return graph;
	} catch (Exception e) {
	    if (stackTrace) {
		e.printStackTrace(System.err);
	    } else {
		System.err.println(errorMsg("exception", e.getMessage()));
	    }
	    System.exit(1);
	    return null;
	}
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
		double val = Double.parseDouble(arg);
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

    public static boolean mayBeURL(String s) {
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
		// relative URI use URI-path syntax and the "/"
		// is not a file-separator character, so we'll treat it
		// as a relative URI if a "/" appears but there is no
		// file-separator character in the string.
		return true;
	    }
	}
	return result;
    }


    static void init(String argv[]) throws Exception {

	final File cdir = new File(System.getProperty("user.dir"));

	int index = -1;
	int port = 0;

	ArrayList<String> jargsList = new ArrayList<>();
	ArrayList<String> targetList = new ArrayList<>();
	Map<String,String> map = null;
	boolean dryrun = false;
	boolean jcontinue = true;
	boolean scriptMode = false;
	String a2dName = null;
	boolean imageMode = false;
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
	boolean meters = true;

	String pkg = null;
	String clazz = null;
	boolean isPublic = false;

	ArrayList<NameValuePair> bindings = new ArrayList<>();

	String alreadyForkedString = System.getProperty("epts.alreadyforked");
	boolean alreadyForked = (alreadyForkedString != null)
	    && alreadyForkedString.equals("true");
	if (System.getProperty("epts.alreadyforked") == null
	    || !(System.getProperty("epts.alreadyforked").equals("true"))) {
	    sysconf = defaultSysConfigFile();
	    usrconf = defaultUsrConfigFile();
	}

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
			System.err.println(errorMsg("badArgument", arg));
			System.exit(1);
		    }
		    String[] pair = new String[2];
		    pair[0] = arg.substring(2, ind);
		    pair[1] = arg.substring(ind+1);
		    String name = pair[0];
		    String value = pair[1];
		    if (propertyNotAllowed(name)) {
			System.err.println(errorMsg("badArgProp", name));
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
		    argsList.add(argv[index]);
		} else if (argv[index].equals("-L")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    languageName = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		    continue;
		} else if (argv[index].equals("--class")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    clazz = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--public")) {
		    isPublic = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--codebase")) {
		    hasCodebase = true;
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    for (String cb: URLPathParser.split(argv[index])) {
			if (!codebaseSet.contains(cb)) {
			    codebase.add(argv[index]);
			    codebaseSet.add(argv[index]);
			}
		    }
		} else if (argv[index].equals("--script")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    a2dName = argv[index];
		    scriptMode = true;
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--image")) {
		    imageMode = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--port")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    try {
			port = Integer.parseInt(argv[index]);
		    } catch (Exception e) {
			System.err.println
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
			    System.err.println
				(errorMsg("noSVG"));
			    System.exit(1);
			}
		    }
		    svg = true;
		    limit = 0;
		    flatness = 0.0;
		    templateURL = new URL("sresource:SVG");
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-color")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
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
			System.err.println
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
			System.err.println
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
			System.err.println
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
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.dashPattern = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--stroke-dash-phase")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
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
			System.err.println
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
			System.err.println
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
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.strokeWidth = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--fill-color")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
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
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    filterInfo.zorder =
			parseArgument(argv[index-1],argv[index],
				      Long.class,
				      null, false , null, false);

		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--web")) {
		    webserverOnly = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--map")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    try {
			String mname = argv[index];
			if (mname.startsWith("sresource:")
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
				System.err.println
				    (errorMsg("cannotRead", argv[index]));
				System.exit(1);
			    }
			}
		    } catch (Exception e) {
			System.err.println
			    (errorMsg("ioError", argv[index], e.getMessage()));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--package")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    pkg = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--pname")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
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
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--flatness")) {
		    if (svg) {
			System.err.println
			    (errorMsg("svgmode","--flatness"));
			System.exit(1);
		    }
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    flatness = Double.parseDouble(argv[index]);
		    if (flatness < 0)  {
			throw new IllegalArgumentException
			    (errorMsg("negative", argv[index]));
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--limit")) {
		    if (svg) {
			System.err.println
			    (errorMsg("svgmode","--limit"));
			System.exit(1);
		    }
		    index++;
		    if (index == argv.length) {
			System.err.println
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
			System.err.println
			    (errorMsg("svgmode","--straight"));
			System.exit(1);
		    }
		    straight = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--elevate"))  {
		    if (svg) {
			System.err.println
			    (errorMsg("svgmode","--elevate"));
			System.exit(1);
		    }
		    elevate = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--gcs")) {
		    if (svg) {
			System.err.println
			    (errorMsg("svgmode","--gcs"));
			System.exit(1);
		    }
		    gcs = true;
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--template")) {
		    if (svg) {
			System.err.println
			    (errorMsg("svgmode","--template"));
			System.exit(1);
		    }
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String tname = argv[index];
		    if (tname.startsWith("sresource:")
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
		    String tname = "resource:" + resource;
		    templateURL = new URL(tname);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--tname")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
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
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    outName = argv[index];
		    if (!outName.equals("-")) {
			File f = new File(outName);
			f = f.getCanonicalFile();
			if (f.exists()) {
			    if (f.canWrite() == false) {
				System.err.println
				    (errorMsg("cannotWrite", outName));
				System.exit(1);
			    }
			} else {
			    if (f.getParentFile().canWrite() == false) {
				System.err.println
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
			System.err.println
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
		} else if (argv[index].equals("--int")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
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
			    System.err.println(msg);
			    System.exit(1);
			}
		    } else {
			System.err.println(errorMsg("noEqual", argv[index-1]));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--double")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String[] tokens = argv[index].split("=", 2);
		    if (tokens.length == 2) {
			try {
			    Double value = Double.parseDouble(tokens[1]);
			    bindings.add(new NameValuePair(tokens[0], value));
			} catch (Exception e) {
			    String msg = errorMsg("illegalToken",
						  tokens[1],
						  argv[index-1],
						  e.getMessage());
			    System.err.println(msg);
			    System.exit(1);
			}
		    } else {
			System.err.println(errorMsg("noEqual", argv[index-1]));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--string")) {
		    index++;
		    if (index == argv.length) {
			System.err.println
			    (errorMsg("missingArg", argv[--index]));
			System.exit(1);
		    }
		    String[] tokens = argv[index].split("=", 2);
		    if (tokens.length == 2) {
			String value = tokens[1];
			bindings.add(new NameValuePair(tokens[0], value));
		    } else {
			System.err.println(errorMsg("noEqual", argv[index-1]));
			System.exit(1);
		    }
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		} else if (argv[index].equals("--customUnits")) {
		    meters = false;
		} else if (argv[index].equals("--mksUnits")) {
		    meters = true;
		} else if (argv[index].equals("--")) {
		    argsList.add(argv[index]);
		    jcontinue = false;
		} else if (argv[index].startsWith("-")) {
		    System.err.println(errorMsg("unknownOption", argv[index]));
		    System.exit(1);
		} else {
		    if (scriptMode && languageName == null) {
			int extInd = argv[index].lastIndexOf('.');
			int lastSlash = argv[index].lastIndexOf('/');
			int lastSep = argv[index]
			    .lastIndexOf(File.separatorChar);
			if (extInd>0 && extInd>lastSlash && extInd>lastSep) {
			    extInd++;
			    if (extInd < argv[index].length()) {
				languageName = argv[index].substring(extInd);
			    }
			}
		    }
		    argsList.add(argv[index]);
		    targetList.add(argv[index]);
		    jcontinue = false;
		}
	    } else {
		argsList.add(argv[index]);
		targetList.add(argv[index]);
	    }
	}

	if ((imageMode ^ scriptMode) == false) {
	    if (imageMode || scriptMode) {
		System.err.println("imageMode = " + imageMode);
		System.err.println("scriptMode = " + scriptMode);
		System.err.println(errorMsg("modes"));
		System.exit(1);
	    }
	}

	if (imageMode && targetList.size() != 1) {
	    System.err.println(errorMsg("targetLength1"));
	    System.exit(1);
	}

	if (scriptMode && targetList.size() == 0) {
	    System.err.println(errorMsg("targetLength0"));
	    System.exit(1);
	}

	// We use a scripting language only if there is a script to
	// process.
	if (scriptMode == false) languageName = null;
	
	if (languageName != null) {
	    // need to extend class path because scripting-language-independent
	    // class path entries in the configuration files may be needed for
	    // scripting languages to be recognized.
	    readConfigFiles(null);
	    if (!Scripting.supportsLanguage(languageName)) {
		String ln =
		    Scripting.getLanguageNameByAlias(languageName);
		if (ln == null) {
		    String msg =
			(errorMsg("badScriptingLanguageName",languageName));
		    System.err.println(msg);
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
	    jargsList.add("EPTS");
	    jargsList.add(0, sbcp.toString());
	    jargsList.add(0, "-classpath");
	    if (policyFile == null) {
		try {
		    File pf = new File(EPTS.class.getProtectionDomain()
				       .getCodeSource().getLocation().toURI());
		    pf = pf.getParentFile();
		    jargsList.add(0, "-Djava.security.policy="
				  + (new File(pf, "epts.policy")
				     .getCanonicalPath()));
		} catch (Exception eio) {
		    System.err.println(errorMsg("policyFile"));
		    System.exit(1);
		}
	    }
	    String classLoader = defs.getProperty("java.system.class.loader");
	    if (classLoader == null) {
		jargsList.add(0, "-Djava.system.class.loader="
			      + "org.bzdev.lang.DMClassLoader");
	    }
	    jargsList.add(0, "-Depts.alreadyforked=true");
	    jargsList.add(0, javacmd);
	    jargsList.addAll(argsList);
	    if (dryrun) {
		System.out.print("Command: ");
		for (String s: jargsList) {
		    System.out.print(s);
		    System.out.print(" ");
		}
		System.out.println();
	    }
	    ProcessBuilder pb = new ProcessBuilder(jargsList);
	    pb.inheritIO();
	    try {
		Process process = pb.start();
		System.exit(process.waitFor());
	    } catch (Exception e) {
		System.err.println(errorMsg("scException", e.getMessage()));
		System.exit(1);
	    }
	} else if (dryrun) {
	    System.out.println("image mode  = " + imageMode);
	    System.out.println("scriptMode = " + scriptMode);
	    if (scriptMode) {
		System.out.println("a2dName = " + a2dName);
	    }
	    if (languageName != null) {
		System.out.println("scripting language = " + languageName);
	    }
	    for (String s: targetList) {
		System.out.println("target = " + s);
	    }
	    System.exit(0);
	}
	try {
	    org.bzdev.protocols.Handlers.enable();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}

	if (codebase.size() > 0) {
	    for (String cp: codebase) {
		try {
		    URL[] urls = URLPathParser.getURLs(null, cp,
						       ourCodebaseDir,
						       System.err);
		    URLClassLoaderOps.addURLs(urls);
		} catch (MalformedURLException e) {
		    System.err.append(errorMsg("codebaseError", cp) + "\n");
		    System.exit(1);
		}
	    }
	}

	if (webserverOnly) {
	    EPTSWindow.setPort(port);
	    return;
	}

	if (!imageMode && !scriptMode && targetList.size() == 0) {
	    // No arguments that would indicate a saved state, image file,
	    // or script files, so open a dialog box prompting for
	    // the input to use.
	    imageMode = true;
	    SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			JFileChooser fc = new JFileChooser(cdir);
			String[] extensions = ImageIO.getReaderFileSuffixes();
			FileNameExtensionFilter filter =
			    new FileNameExtensionFilter("Images", extensions);
			fc.setFileFilter(filter);
			int status = fc.showOpenDialog(null);
			if (status == JFileChooser.APPROVE_OPTION) {
			    File f = fc.getSelectedFile();
			    try {
				targetList.add(f.getCanonicalPath());
			    } catch (IOException e) {
				ErrorMessage.setComponent(null);
				ErrorMessage.display(e);
				System.exit(1);
			    }
			}/* else {
			    System.exit(0);
			    }*/
		    }
		});
	}
	if (imageMode) {
	    URI imageURI = null;
	    Image image;
	    if (targetList.size() == 0) {
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
			    width = htf.getValue();
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
	    } else {
		String urlstring = targetList.get(0);
		try {
		    if (mayBeURL(urlstring)) {
			URL imageURL = new URL(cdir.toURI().toURL(), urlstring);
			imageURI = imageURL.toURI();
			image = ImageIO.read(imageURL);
		    } else {
			File ifile = new File(urlstring);
			imageURI = ifile.getCanonicalFile().toURI();
			image = ImageIO.read(ifile);
		    }
		} catch (Exception ex) {
		    image = null; imageURI = null;
		    String msg =
			errorMsg("readError", urlstring, ex.getMessage());
		    System.err.println(msg);
		    System.exit(1);
		}
	    }
	    EPTSWindow.setPort(port);
	    new EPTSWindow(image, imageURI);
	} else if (scriptMode) {
	    Graph graph =
		doScripts(languageName, a2dName, bindings, targetList);
	    EPTSWindow.setPort(port);
	    new EPTSWindow(graph, bindings, targetList, meters,
			   languageName, a2dName, null);
	} else if (targetList.size() == 1) {
	    EPTSParser parser = new EPTSParser();
	    String filename = targetList.get(0);
	    if (filename.endsWith(".epts")) {
		try {
		    File parent  = new File(filename).getParentFile();
		    if (parent != null) {
			System.setProperty("user.dir",
					   parent.getCanonicalPath());
		    }
		    parser.parse(new FileInputStream(filename));
		    if (outName == null) {
			EPTSWindow.setPort(port);
			for (String name: parser.getCodebase()) {
			    URL[] urls =
				URLPathParser.getURLs(null, name,
						      ourCodebaseDir,
						      System.err);
			    if (urls.length != 1) {
				String title = errorMsg("errorTitle");
				String msg = errorMsg("multipleURLs");
				ErrorMessage.display(null, title, msg);
				System.exit(1);
			    }
			    if (name.startsWith(".../")) {
				URLClassLoaderOps.addURLs(urls);
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
				    URLClassLoaderOps.addURLs(urls);
				}
			    }
			}
			if (parser.hasScripts()) {
			    List<NameValuePair> pbindings
				= parser.getBindings();
			    List<String> ptargetList
				= parser.getTargetList();
			    String languageName = parser.getLanguageName();
			    String animationName = parser.getAnimationName();
			    Graph graph = doScripts(languageName, animationName,
						    pbindings, ptargetList);
			    new EPTSWindow(graph, pbindings, ptargetList,
					   parser.usesMeters(),
					   languageName, animationName,
					   parser.getRows());
			} else {
			    new EPTSWindow(parser, new File(filename));
			}
		    } else {
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
			TemplateProcessor tp;
			if (pname != null) {
			    TemplateProcessor.KeyMap kmap
				= EPTSWindow.getPathKeyMap(ptmodel, pname,
							   pnameArray,
							   flatness, limit,
							   straight, elevate,
							   gcs);
			    kmap.put("width", "" + parser.getWidth());
			    kmap.put("height", "" + parser.getHeight());
			    tp  = new TemplateProcessor(kmap);
			} else if (svg) {
			    TemplateProcessor.KeyMap svgmap =
				new TemplateProcessor.KeyMap();
			    svgmap.put("width", "" + parser.getWidth());
			    svgmap.put("height", "" + parser.getHeight());
			    TemplateProcessor.KeyMapList kmaplist =
				new TemplateProcessor.KeyMapList();
			    for (FilterInfo info: filterInfoList) {
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
				    kmap.put("dashArray", dashArray);
				    kmap.put("hasDashArray", emptyMap);
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
				kmap.put("hasPackage", emptyMap);
			    }
			    if (clazz != null) {
				kmap.put("class", clazz);
			    } else {
				kmap.put("class", "GeneratedByEPTS");
			    }
			    if (isPublic) {
				kmap.put("public", "public");
				kmap.put("optSpace", " ");
			    }
			    tp = new TemplateProcessor(kmap);
			} else {
			    TemplateProcessor.KeyMap kmap =
				ptmodel.getKeyMap(map,
						  (double)parser.getHeight());
			    if (filterInfo.windingRule != null) {
				kmap.put("windingRule", filterInfo.windingRule);
				kmap.put("hasWindingRule", emptyMap);
			    }
			    kmap.put("draw", filterInfo.draw);
			    kmap.put("fill", filterInfo.fill);

			    if (filterInfo.draw.equals("true")
				|| filterInfo.fill.equals("true")) {
				kmap.put("hasAttributes", emptyMap);
			    }

			    if (filterInfo.gcsMode != null) {
				kmap.put("gcsMode", filterInfo.gcsMode);
				kmap.put("hasGcsMode", emptyMap);
			    }
			    if (filterInfo.drawColor != null) {
				kmap.put("drawColor", filterInfo.drawColor);
				kmap.put("hasDrawColor", emptyMap);
			    }
			    if (filterInfo.fillColor != null) {
				kmap.put("fillColor", filterInfo.fillColor);
				kmap.put("hasFillColor", emptyMap);
			    }
			    if (filterInfo.strokeCap != null) {
				kmap.put("strokeCap", filterInfo.strokeCap);
				kmap.put("hasStrokeCap", emptyMap);
			    }
			    if (filterInfo.dashIncrement != null) {
				kmap.put("dashIncrement",
					 filterInfo.dashIncrement);
				kmap.put("hasDashIncrement", emptyMap);
			    }
			    if (filterInfo.dashPhase != null) {
				kmap.put("dashPhase", filterInfo.dashPhase);
				kmap.put("hasDashPhase", emptyMap);
			    }
			    if (filterInfo.dashPattern != null) {
				kmap.put("dashPattern", filterInfo.dashPattern);
				kmap.put("hasDashPattern", emptyMap);
			    }
			    if (filterInfo.strokeJoin != null) {
				kmap.put("strokeJoin", filterInfo.strokeJoin);
				kmap.put("hasStrokeJoin", emptyMap);
			    }
			    if (filterInfo.miterLimit != null) {
				kmap.put("miterLimit", filterInfo.miterLimit);
				kmap.put("hasMiterLimit", emptyMap);
			    }
			    if (filterInfo.strokeWidth != null) {
				kmap.put("strokeWidth", filterInfo.strokeWidth);
				kmap.put("hasStrokeWidth", emptyMap);
			    }
			    if (filterInfo.zorder != null) {
				kmap.put("zorder", filterInfo.zorder);
				kmap.put("hasZorder", emptyMap);
			    }
			    if (pkg != null) {
				kmap.put("package", pkg);
				kmap.put("hasPackage", emptyMap);
			    }
			    if (clazz != null) {
				kmap.put("class", clazz);
				kmap.put("hasClass", emptyMap);
			    }
			    if (isPublic) {
				kmap.put("public", "public");
				kmap.put("optSpace", " ");
			    }
			    tp = new TemplateProcessor(kmap);
			}
			tp.processURL(templateURL, "UTF-8", os);
			os.flush();
			System.exit(0);
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		    System.exit(1);
		}
	    }
	}
    }


    public static void main(String argv[]) {
	try {
	    org.bzdev.protocols.Handlers.enable();
	    init(argv);
	} catch (Exception e) {
	    ErrorMessage.display(e);
	    if (stackTrace) {
		e.printStackTrace();
	    }
	    System.exit(1);
	}
    }
}
