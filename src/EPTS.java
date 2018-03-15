import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.imageio.*;
import javax.swing.*;

import org.bzdev.net.URLClassLoaderOps;
import org.bzdev.net.URLPathParser;
import org.bzdev.scripting.Scripting;
import org.bzdev.swing.ErrorMessage;
import org.bzdev.util.SafeFormatter;

public class EPTS {
    // resource bundle for messages used by exceptions and errors
    static ResourceBundle exbundle = ResourceBundle.getBundle("EPTS");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }

    private static final String pathSeparator =
	System.getProperty("path.separator");

    private static final String JAVACMD = "java";
    private static String javacmd = JAVACMD;

    private static final String dotDotDot = "...";
    private static final String dotDotDotSep = dotDotDot + File.separator;
    private static final String tildeSep = "~" + File.separator;
    private static String ourCodebaseDir;
    static {
	try {
	    ourCodebaseDir =
		(new File(Scripting.class.getProtectionDomain()
			  .getCodeSource().getLocation().toURI()))
		.getCanonicalFile().getParentFile().getCanonicalPath();
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
    private static LinkedList<String> classpath = new LinkedList<>();

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
					     value.split(Pattern.quote
							 (pathSeparator))) {
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
					    nv.append(pathSeparator);
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
			    if (languageName == null) {
				// occurs during the first pass where
				// we have to make these files and directories
				// available so that we can get data about
				// scripting languages.
				try {
				    URL url;
				    if (line.startsWith("file:")) {
					url = new URL(line);
				    } else {
					url = (new File(line)).toURI().toURL();
				    }
				    URLClassLoaderOps.addURL(url);
				} catch (Exception e) {
				    int lno = reader.getLineNumber();
				    String msg =
					errorMsg("urlParse", fileName, lno);
				    System.err.println(msg);
				    System.exit(1);
				}
			    } else {
				if (sbcp.length() > 0) {
				    sbcp.append(pathSeparator);
				}
				sbcp.append((line));
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

    static private void readConfigFiles(String languageName) {
	if (sysconf != null) {
	    readConfigFiles(languageName, sysconf);
	}
	if (usrconf != null) {
	    readConfigFiles(languageName, usrconf);
	}
    }

    static void init(String argv[]) throws Exception {
	int index = -1;
	
	ArrayList<String> jargsList = new ArrayList<>();
	boolean jcontinue = false;
	boolean hasCodebase = false;
	ArrayList<String> argsList = new ArrayList<>();

	while ((++index) < argv.length) {
	    if (jcontinue) {
		if (argv[index].startsWith("-D") ||
		       argv[index].startsWith("-J-D")) {
		    // These -D arguments are provided *after*
		    // org.bzdev.bin.scrunner.SCRunnerCmd appears on the
		    // java command line, but will be put *before*
		    // org.bzdev.bin.scrunner.SCRunner appears on the java
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
		} else if (argv[index].equals("-L")) {
		    index++;
		    languageName = argv[index];
		    argsList.add(argv[index-1]);
		    argsList.add(argv[index]);
		    continue;
		} else if (argv[index].equals("--codebase")) {
		    hasCodebase = true;
		    argsList.add(argv[index]);
		    index++;
		    argsList.add(argv[index]);
		    classpath.add(argv[index]);
		} else if (argv[index].equals("--")) {
		    argsList.add(argv[index]);
		    jcontinue = false;
		} else {
		    argsList.add(argv[index]);
		}
	    }
	}
	
	// need to extend class path because scripting-language-independent
	// class path entries in the configuration files may be needed for
	// scripting languages to be recognized.
	readConfigFiles(null);
	if (languageName != null) {
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
	    if (defs.getProperty("java.system.class.loader").equals("-none-")) {
		defs.remove("java.system.class.loader");
	    }
	    for (String name: defs.stringPropertyNames()) {
		String value = defs.getProperty(name);
		if (name.equals("java.system.class.loader")
		    && value.equals("-none-")) {
		    continue;
		}
		jargsList.add("-D" + name + "=" + value);
	    }
	}

	if (jargsList.size() > 0 || javacmd != JAVACMD) {
	    String policyFile = defs.getProperty("java.security.policy");
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
	    jargsList.add(0, javacmd);
	    jargsList.addAll(argsList);
	    ProcessBuilder pb = new ProcessBuilder(jargsList);
	    pb.inheritIO();
	    try {
		Process process = pb.start();
		System.exit(process.waitFor());
	    } catch (Exception e) {
		System.err.println(errorMsg("scException", e.getMessage()));
		System.exit(1);
	    }
	}

	// We should reach this point in the code only if there is no
	// reason to restart the Java virtual machine.
	String[] args = new String[argsList.size()];
	args = argsList.toArray(args);

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

	index = 0;
	while (index < args.length && args[index].startsWith("-")) {
	    if (args[index].equals("--measureDistance")) {
	    } else if (argv[index].equals("--points")) {
	    } else if (argv[index].equals("--script")) {
	    } else if (argv[index].equals("--image")) {
	    } else if (argv[index].equals("--")) {
		break;
	    }
	}
    }


    public static void main(String argv[]) {
	try {
	    init(argv);
	} catch (Exception e) {
	    ErrorMessage.display(e);
	    System.exit(1);
	}

    }
}
