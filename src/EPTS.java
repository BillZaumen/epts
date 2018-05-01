import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bzdev.net.URLClassLoaderOps;
import org.bzdev.net.URLPathParser;
import org.bzdev.scripting.Scripting;
import org.bzdev.swing.ErrorMessage;
import org.bzdev.util.SafeFormatter;

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
    static {
	try {
	    File f = (new File(EPTS.class.getProtectionDomain()
			       .getCodeSource().getLocation().toURI()))
		.getCanonicalFile();
	    ourCodebase = f.getCanonicalPath();
	    ourCodebaseDir = f.getParentFile().getCanonicalPath();
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


    static void init(String argv[]) throws Exception {
	int index = -1;
	
	int port = 0;

	ArrayList<String> jargsList = new ArrayList<>();
	ArrayList<String> targetList = new ArrayList<>();
	boolean dryrun = false;
	boolean jcontinue = true;
	boolean scriptMode = false;
	boolean imageMode = false;
	boolean hasCodebase = false;
	ArrayList<String> argsList = new ArrayList<>();

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
			System.err.println("scrunner: bad argument \""
					   + arg + "\"");
			System.exit(1);
		    }
		    String[] pair = new String[2];
		    pair[0] = arg.substring(2, ind);
		    pair[1] = arg.substring(ind+1);
		    String name = pair[0];
		    String value = pair[1];
		    if (propertyNotAllowed(name)) {
			System.err.println("epts: bad argument "
					   +"(cannot set property \"" 
					   + name + "\")");
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
		    languageName = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		    continue;
		} else if (argv[index].equals("--codebase")) {
		    hasCodebase = true;
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    index++;
		    if (!alreadyForked) {
			argsList.add(argv[index]);
		    }
		    codebase.add(argv[index]);
		} else if (argv[index].equals("--script")) {
		    scriptMode = true;
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


	boolean mustFork = (jargsList.size() > 0 || javacmd != JAVACMD);
	String policyFile = defs.getProperty("java.security.policy");
	if (policyFile == null && scriptMode) mustFork = true;
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
				  + (new File(pf, "libbzdev.policy")
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
	if (!imageMode && !scriptMode && targetList.size() == 0) {
	    // No arguments that would indicate a saved state, image file,
	    // or script files, so open a dialog box prompting for
	    // the input to use.
	    imageMode = true;
	    SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			File cdir = new File(System.getProperty("user.dir"));
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
			} else {
			    System.exit(0);
			}
		    }
		});
	}
	if (imageMode) {
	    Image image = ImageIO.read(new File(targetList.get(0)));
	    new EPTSWindow(image, port, targetList);
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
		    new EPTSWindow(parser);
		    /*
		    System.out.println("width " + parser.getWidth());
		    System.out.println("height " + parser.getHeight());
		    System.out.println("hasImage " + parser.imageURIExists());
		    System.out.println("imageURI " + parser.getImageURI());
		    System.out.println("user-space dist "
				       + parser.getUserSpaceDistance());
		    System.out.println("GCS dist "
				       + parser.getGcsDistance());
		    System.out.println("number of rows = "
				       +parser.getRows().length);
		    for (PointTMR row: parser.getRows()) {

			System.out.format("%s, %s, %s, %s, %s, %s\n",
					  row.getVariableName(),
					  row.getMode(),
					  row.getX(),
					  row.getY(),
					  row.getXP(),
					  row.getYP());
		    }
		    */
		} catch (Exception e) {
		    e.printStackTrace();
		    System.exit(1);
		}
	    }
	}
    }


    public static void main(String argv[]) {
	try {
	    init(argv);
	} catch (Exception e) {
	    ErrorMessage.display(e);
	    e.printStackTrace();
	    System.exit(1);
	}

    }
}
