package org.bzdev.epts;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.parsers.*;

import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.CPointType;
import org.bzdev.graphs.RefPointName;
import org.bzdev.swing.SwingErrorMessage;

import org.xml.sax.helpers.*;
import org.xml.sax.*;


public class EPTSParser {

    static final String PUBLICID = "-//BZDev//EPTS-1.0//EN";
    static final String SYSTEMID = "resource:org/bzdev/epts/epts-1.0.dtd";
    static final String NAMESPACE = "http://bzdev.org/DTD/epts-1.0";
    static final String OUR_SYSTEMID = "resource:org/bzdev/epts/epts-1.0.dtd";

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }
    
    Component comp = null;
    public void setComponent(Component c) {
	comp = c;
    }

    SAXParser parser = null;

    String[] argArray = null;
    int width;
    int height;
    boolean imageURIExists = false;
    boolean scriptSeen = false;
    URI imageURI = null;
    int unitIndex = 0;
    int unitIndexRP = 0;
    int refPointIndex = 0;
    
    // These are strings because of the way the configuration pane
    // is saved.
    String userSpaceDistance = "1.0";
    String gcsDistance = "1.0";
    String xrefpoint = "0.0";
    String yrefpoint = "0.0";
    PointTMR[] rows = null;
    
    LinkedList<String>codebase = new LinkedList<>();
    LinkedList<String>classpath = new LinkedList<>();
    LinkedList<String>modules = new LinkedList<>();
    String resourcePath = null;

    public boolean hasScripts() {return scriptSeen;}
    public String getResourcePath() {return resourcePath;}

    public int getWidth() {return width;}
    public int getHeight() {return height;}
    public boolean imageURIExists() {return imageURIExists;}
    public URI getImageURI() {return imageURI;}
    public int getUnitIndex() {return unitIndex;}
    public int getUnitIndexRP() {return unitIndexRP;}
    public int getRefPointIndex() {return refPointIndex;}
    public RefPointName getRefPoint() {
	return RefPointName.values()[refPointIndex];
    }
    public String getUserSpaceDistance() {return userSpaceDistance;}
    public String getGcsDistance() {return gcsDistance;}

    public double getUserSpaceDistanceNumeric() {
	return Double.parseDouble(userSpaceDistance);
    }

    public double getGcsDistanceMeters() {
	try {
	    // both meters and custom units do not scale the value
	    // of gcsDistance, but the others have to be scaled to
	    // get the distance in meters..
	    return ConfigGCSPane.getGCSDist(gcsDistance, unitIndex);
	} catch (NumberFormatException e) {
	    // should never happen as we'd through a  parser error
	    // before the parser was ready to use this method.
	    return 0.0;
	}
    }

    public double getScaleFactor() {
	double userspaceDistance = getUserSpaceDistanceNumeric();
	double gcsDistance = getGcsDistanceMeters();
	return (userspaceDistance == 0.0)? 1.0:
	    gcsDistance / userspaceDistance;
    }

    public String getXRefpoint() {return xrefpoint;}
    public String getYRefpoint() {return yrefpoint;}

    public double getXRefpointDouble() {
	return (xrefpoint == null)? 0.0:
	    ConfigGCSPane.convert[unitIndexRP]
	    .valueAt(Double.parseDouble(xrefpoint));
    }
    public double getYRefpointDouble() {
	return (yrefpoint == null)? 0.0:
	    ConfigGCSPane.convert[unitIndexRP]
	    .valueAt(Double.parseDouble(yrefpoint));
    }
       
    public boolean usesCustom() {
	return (unitIndex == 0);
    }

    public boolean usesCustomRP() {
	return (unitIndexRP == 0);
    }


    String animation = null;
    String language = null;

    public String getAnimationName() {
	return animation;
    }

    public String getLanguageName() {
	return language;
    }

	ArrayList<EPTS.NameValuePair> bindings = null;

	public List<EPTS.NameValuePair> getBindings() {
	    if (bindings == null) {
		return Collections.emptyList();
	    } else {
		return Collections.unmodifiableList(bindings);
	    }
	}

    public List<String> getCodebase() {
	return Collections.unmodifiableList(codebase);
    }

    public List<String> getClasspath() {
	return Collections.unmodifiableList(classpath);
    }

    public List<String> getModules() {
	return Collections.unmodifiableList(modules);
    }


    public PointTMR[] getRows() {return rows;}


    public Image getImage() throws IOException {
	if (imageURIExists) {
	    URI uri = imageURI;
	    Image image;
	    if (uri != null)  {
		if (!uri.isAbsolute()) {
		    File cdir =
			new File(System.getProperty("user.dir"));
		    uri = cdir.toURI().resolve(uri);
		}
		try {
		    image = ImageIO.read(uri.toURL());
		    imageURI = uri;
		} catch (IOException e) {
		    throw new
			IOException(errorMsg("loadImageError", uri.toString()));
		}
		if (width != image.getWidth(null)) {
		    throw new IllegalStateException
			("save-state width not equal to image width");
		}
		if (height != image.getHeight(null)) {
		    throw new IllegalStateException
			("save-state height not equal to image height");
		}
	    } else {
		// No image URL, so width and height were from
		// a buffered image and we just create it.
		BufferedImage bi = new
		    BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g2d = bi.createGraphics();
		// g2d.setBackground(Color.WHITE);
		g2d.setBackground(EPTS.stdBackgroundColor);
		g2d.clearRect(0, 0, width, height);
		g2d.dispose();
		image = bi;
	    }
	    return image;
	} else if (width > 0 && height > 0) {
	    Image image;
	    BufferedImage bi = new
		BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB_PRE);
	    Graphics2D g2d = bi.createGraphics();
	    // g2d.setBackground(Color.WHITE);
	    g2d.setBackground(EPTS.stdBackgroundColor);
	    g2d.clearRect(0, 0, width, height);
	    g2d.dispose();
	    image = bi;
	    return image;
	} else {
	    return null;
	}
    }


    public EPTSParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        parser = factory.newSAXParser();
    }

    public void parse(InputStream is) throws SAXException, IOException {
        OurDefaultHandler handler = new OurDefaultHandler();
        parser.parse(is, handler);
        if (handler.errorSeen)
            throw new SAXException(errorMsg("badDocument"));
    }

    String xmlFilename = null;
    
    public void setXMLFilename(String name) {
	xmlFilename = name;
    }
    
    void displayMessage(String msg, String title) {
	if (comp != null) {
	    SwingErrorMessage.display(comp, title, msg);
	} else {
	    SwingErrorMessage.display(msg);
	}
    }

    void displayMessage(Locator locator, 
			String msg, String title) {
	SwingErrorMessage.display(xmlFilename, locator.getLineNumber(), msg);
    }

    ArrayList<String> argList = new ArrayList<>();
    public ArrayList<String> getTargetList() {return argList;}


    StringBuilder text = new StringBuilder();

    static enum BType {
	INTEGER,
	DOUBLE,
	STRING,
	BOOLEAN
    }

    ArrayList<PTFilters.TopEntry> filterList = new ArrayList<>();
    public List<PTFilters.TopEntry> getFilterList() {
	if (filterList == null) return null;
	return Collections.unmodifiableList(filterList);
    }

    Map<String,OffsetPane.BME> baseMap = new HashMap<String,OffsetPane.BME>();
    Map<String,String> pathMap = new HashMap<String,String>();

    public void configureOffsetPane() {
	for (Map.Entry<String,OffsetPane.BME> entry: baseMap.entrySet()) {
	    OffsetPane.baseMap.put(entry.getKey(), entry.getValue());
	}
	for (Map.Entry<String,String> entry: pathMap.entrySet()) {
	    OffsetPane.addOurPathName(entry.getKey(), entry.getValue());
	}
    }

    class OurDefaultHandler extends DefaultHandler {

	boolean processingXML = false;
	boolean processingTable = false;
	boolean errorSeen = false;
	boolean publicIDSeen = false;
        Locator locator = null;
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
	boolean processingCodebase = false;
	boolean processingModules = false;
	boolean processingClasspath = false;

	File base;


	boolean mimeTypePISeen = false;


	ArrayList<PointTMR> rowList = new ArrayList<>();

	PTFilters.TopEntry newFilterEntry = null;

	String name = null;

	BType btype = null;

	public void startDocument() {
	    base = new File(System.getProperty("user.dir"));
	    locator = null;
	    errorSeen = false;
	    publicIDSeen = false;
	    text.setLength(0);
	    argList.clear();
	    rowList.clear();
	    processingXML = false;
	    processingTable = false;
	    processingCodebase = false;
	    processingModules = false;
	    processingClasspath = false;
	}

        public void startElement(String uri, String localName,
                                 String qName, Attributes attr)
            throws SAXException 
        {
	    if (!publicIDSeen) {
		throw new SAXException(errorMsg("missingDOCTYPE"));
	    }

	    if (qName.equals("epts")) {
		String ns = attr.getValue("xmlns");
		if (ns == null ||
		    !ns.equals(NAMESPACE)) {
		    throw new SAXException(errorMsg("namespaceError", ns));
		}
		processingXML = true;
	    } else if (qName.equals("image")) {
		try {
		    width = Integer.parseInt(attr.getValue("width"));
		    if (width <= 0) {
			throw new NumberFormatException
			    (errorMsg("notPositive"));
		    }
		} catch (NumberFormatException e) {
		    throw new
			SAXException(errorMsg("widthError", e.getMessage()));
		}
		try {
		    height = Integer.parseInt(attr.getValue("height"));
		    if (height <= 0) {
			throw new NumberFormatException
			    (errorMsg("notPositive"));
		    }
		} catch (NumberFormatException e) {
		    throw new
			SAXException(errorMsg("heightError", e.getMessage()));
		}
		// String s = attr.getValue("imageURIExists");
		// if (s == null) s = "false";
		// imageURIExists = s.equals("true");
		imageURIExists = false;
	    } else if (qName.equals("scripting")) {
		 language = attr.getValue("language");
		 animation = attr.getValue("animation");
		 resourcePath = attr.getValue("resourcePath");
		 bindings = new ArrayList<EPTS.NameValuePair>();
		 scriptSeen = true;
	    } else if (qName.equals("binding")) {
		name = attr.getValue("name");
		String tname = attr.getValue("type");
		if (tname.equals("boolean")) {
		    btype = BType.BOOLEAN;
		} else if (tname.equals("int")) {
		    btype = BType.INTEGER;
		} else if (tname.equals("double")) {
		    btype = BType.DOUBLE;
		} else if (tname.equals("String")) {
		    btype = BType.STRING;
		}
		text.setLength(0);
	    } else if (qName.equals("targetList")) {
		argList.clear();
	    } else if (qName.equals("imageURI")) {
		imageURIExists = true;
		text.setLength(0);
	    } else if (qName.equals("argument")) {
		text.setLength(0);
	    } else if (qName.equals("codebase")) {
		codebase.clear();
		processingCodebase = true;
	    } else if (qName.equals("gcsconfig")) {
		try {
		    unitIndex = Integer.parseInt(attr.getValue("unitIndex"));
		} catch (Exception e) {
		    throw new SAXException
			(errorMsg("attrError", "unitIndex", e.getMessage()));
		}
		String unitIndexRPStr = attr.getValue("unitIndexRP");
		if (unitIndexRPStr != null) {
		    try {
			unitIndexRP = Integer.parseInt(unitIndexRPStr);
		    } catch (Exception e) {
			String msg = e.getMessage();
			throw new SAXException
			    (errorMsg("attrError", "unitIndexRP", msg));
		    }
		}
		try {
		    refPointIndex =
			Integer.parseInt(attr.getValue("refPointIndex"));
		} catch (Exception e) {
		    String msg =
			errorMsg("attrError", "refPointIndex", e.getMessage());
		    throw new SAXException(msg);
		}
		userSpaceDistance = attr.getValue("userSpaceDistance");
		if (userSpaceDistance == null) {
		    userSpaceDistance = "1.0";
		}
		try {
		    Double.parseDouble(userSpaceDistance);
		} catch (NumberFormatException e) {
		    throw new SAXException
			(errorMsg("gcsDistError", e.getMessage()));
		}
		gcsDistance = attr.getValue("gcsDistance");
		if (gcsDistance == null) {
		    gcsDistance = "1.0";
		}
		try {
		    Double.parseDouble(gcsDistance);
		} catch (NumberFormatException e) {
		    throw new SAXException
			(errorMsg("gcsDistError", e.getMessage()));
		}
		xrefpoint = attr.getValue("xrefpointGCS");
		try {
		    Double.parseDouble(xrefpoint);
		} catch (NumberFormatException e) {
		    throw new SAXException
			(errorMsg("xrefpointError", e.getMessage()));
		}
		yrefpoint = attr.getValue("yrefpointGCS");
		try {
		    Double.parseDouble(yrefpoint);
		} catch (NumberFormatException e) {
		    throw new SAXException
			(errorMsg("yrefpointError", e.getMessage()));
		}
	    } else if (qName.equals("table")) {
		processingTable = true;
		rowList.clear();
	    } else if (qName.equals("row")) {
		String varname = attr.getValue("varname");
		if (varname != null) {
		    String key = attr.getValue("key");
		    if (key != null) {
			EPTSWindow.keys.put(varname, key);
		    }
		    String link = attr.getValue("link");
		    if (link != null) {
			EPTSWindow.links.put(varname, link);
		    }
		    String descr = attr.getValue("description");
		    if (descr != null) {
			EPTSWindow.descriptions.put(varname, descr);
		    }
		}
		String type = attr.getValue("type");
		if (type == null) {
		    error(errorMsg("noType"));
		} else {
		    Enum mode = null;
		    if (type.equals("LOCATION")) {
			mode = EPTS.Mode.LOCATION;
		    } else if (type.equals("PATH_START")) {
			mode = EPTS.Mode.PATH_START;
		    } else if (type.equals("PATH_END")) {
			mode = EPTS.Mode.PATH_END;
		    } else if (type.equals("MOVE_TO")) {
			mode = SplinePathBuilder.CPointType.MOVE_TO;
		    } else if (type.equals("SPLINE")) {
			mode = SplinePathBuilder.CPointType.SPLINE;
		    } else if (type.equals("CONTROL")) {
			mode = SplinePathBuilder.CPointType.CONTROL;
		    } else if (type.equals("SEG_END")) {
			mode = SplinePathBuilder.CPointType.SEG_END;
		    } else if (type.equals("CLOSE")) {
			mode = SplinePathBuilder.CPointType.CLOSE;
		    }
		    double x = 0.0, y = 0.0, xp = 0.0, yp = 0.0;
		    try {
			String s = attr.getValue("x");
			x = (s == null)? 0.0: Double.parseDouble(s);
		    } catch (NumberFormatException e) {
			String emsg = errorMsg("xValue", attr.getValue("x"));
			error(emsg);
		    }
		    try {
			String s = attr.getValue("y");
			y = (s == null)? 0.0: Double.parseDouble(s);
		    } catch (NumberFormatException e) {
			String emsg = errorMsg("yValue", attr.getValue("y"));
			error(emsg);
		    }
		    try {
			String s = attr.getValue("xp");
			xp = (s == null)? 0.0: Double.parseDouble(s);
		    } catch (NumberFormatException e) {
			String emsg = errorMsg("xpValue", attr.getValue("xp"));
			error(emsg);
		    }
		    try {
			String s = attr.getValue("yp");
			yp = (s == null)? 0.0: Double.parseDouble(s);
		    } catch (NumberFormatException e) {
			String emsg = errorMsg("ypValue", attr.getValue("yp"));
			error(emsg);
		    }
		    PointTMR row = new
			PointTMR(varname, mode, x, y, xp, yp);
		    rowList.add(row);
		}
	    } else if ((qName.equals("filters"))) {
		filterList.clear();
	    } else if (qName.equals("filter")) {
		String filterName = attr.getValue("name");
		PointTMR.FilterMode filterMode =
		    Enum.valueOf(PointTMR.FilterMode.class,
				 attr.getValue("mode"));
		newFilterEntry = new PTFilters.TopEntry();
		newFilterEntry.name = filterName;
		newFilterEntry.mode = filterMode;
		newFilterEntry.entries = new ArrayList<PTFilters.Entry>();
		filterList.add(newFilterEntry);
	    } else if (qName.equals("filterRow")) {
		String varName = attr.getValue("varname");
		PointTMR.FilterMode fmode
		    = Enum.valueOf(PointTMR.FilterMode.class,
				   attr.getValue("mode"));
		PTFilters.Entry fentry = new PTFilters.Entry();
		fentry.name = varName;
		fentry.mode = fmode;
		newFilterEntry.entries.add(fentry);
	    } else if (qName.equals("offsets")) {
		baseMap.clear();
		pathMap.clear();
	    } else if (qName.equals("basemapEntry")) {
		String base = attr.getValue("base");
		OffsetPane.BME bme = new OffsetPane.BME();
		bme.mindex = Integer.valueOf(attr.getValue("mindex"));
		bme.dist1 = Double.valueOf(attr.getValue("dist1"));
		bme.dist2 = Double.valueOf(attr.getValue("dist2"));
		bme.dist3 = Double.valueOf(attr.getValue("dist3"));
		bme.uindex1 = Integer.valueOf(attr.getValue("uindex1"));
		bme.uindex2 = Integer.valueOf(attr.getValue("uindex2"));
		bme.uindex3 = Integer.valueOf(attr.getValue("uindex3"));
		baseMap.put(base, bme);
	    } else if (qName.equals("pathmapEntry")) {
		pathMap.put(attr.getValue("path"), attr.getValue("base"));
	    }
	}

	String theURL = null;

        public void endElement(String uri, String localName, String qName)
            throws SAXException
        {
	    if (qName.equals("epts")) {
		processingXML = false;
	    } else if (qName.equals("codebase")) {
		processingCodebase = false;
	    } else if (qName.equals("modules")) {
		processingModules = false;
	    } else if (qName.equals("classpath")) {
		processingClasspath = false;
	    } else if (qName.equals("binding")) {
		String value = text.toString();
		text.setLength(0);
		switch(btype) {
		case BOOLEAN:
		    {
			Boolean bval = Boolean.parseBoolean(value);
			bindings.add(new EPTS.NameValuePair(name, bval));
		    }
		    break;
		case INTEGER:
		    try {
			Integer ival = Integer.parseInt(value);
			bindings.add(new EPTS.NameValuePair(name, ival));
		    } catch (NumberFormatException e) {
			String msg = e.getMessage();
			throw new SAXException
			    (errorMsg("typeError", "int", msg));
		    }
		    break;
		case DOUBLE:
		    try {
			Double dval = Double.parseDouble(value);
			bindings.add(new EPTS.NameValuePair(name, dval));
		    } catch (NumberFormatException e) {
			String msg = e.getMessage();
			throw new SAXException
			    (errorMsg("typeError", "double", msg));
		    }
		    break;
		case STRING:
		    bindings.add(new EPTS.NameValuePair(name, value));
		    break;
		}
	    } else if (qName.equals("targetList")) {
		argArray = new String[argList.size()];
	    } else if (qName.equals("imageURI")) {
		if (text.length() == 0 ||
		    (text.length() == 1 && text.charAt(0) == '-')) {
		    imageURI = null;
		    text.setLength(0);
		} else {
		    try {
			imageURI = new URI(text.toString());
		    } catch (URISyntaxException e) {
			String msg = errorMsg("badURI", text.toString());
			error(msg);
			throw new SAXException(msg);
		    } finally {
			text.setLength(0);
		    }
		}
	    } else if (qName.equals("argument")) {
		String nm = text.toString();
		text.setLength(0);
		if (EPTS.maybeURL(nm)) {
		    argList.add(nm);
		} else {
		    try {
			File af = new File(base, nm).getCanonicalFile();
			argList.add(af.toString());
		    } catch (IOException eio) {
			argList.add(nm);
		    }
		}
		text.setLength(0);
	    } else if (qName.equals("path")) {
		if (processingCodebase) {
		    codebase.add(text.toString());
		}
		if (processingClasspath) {
		    classpath.add(text.toString());
		}
		text.setLength(0);
	    } else if (qName.equals("module")) {
		if (processingModules) {
		    modules.add(text.toString());
		}
		text.setLength(0);
	    } else if (qName.equals("table")) {
		processingTable = false;
		rows = new PointTMR[rowList.size()];
		rows = rowList.toArray(rows);
	    }
	}

        public void characters(char [] ch, int start, int length)
            throws SAXException 
        {
            text.append(ch, start, length);
        }

	public void endDocument() {
	}

        public void warning(SAXParseException e) {

            String msg = (xmlFilename == null)?
		String.format(localeString("warningAtLine"),
				     e.getLineNumber(), e.getMessage()):
		String.format(localeString("warningAtLineFN"),
			      xmlFilename, e.getLineNumber(), e.getMessage());
            displayMessage(msg, localeString("warningAtLineTitle"));
        }

	private void error(String msg) {
	    displayMessage(locator, msg, localeString("errorAtLineTitle"));
	    errorSeen = true;
	}

        public void error(SAXParseException e) {
            String msg = (xmlFilename == null)?
		String.format(localeString("errorAtLine"),
			      e.getLineNumber(), e.getMessage()):
		String.format(localeString("errorAtLineFN"),
				     xmlFilename, e.getLineNumber(),
				     e.getMessage());
            displayMessage(msg, localeString("errorAtLineTitle"));
            // System.err.println(msg);
            errorSeen = true;
        }
        public void fatalerror(SAXParseException e) {
            String msg = (xmlFilename == null)?
		String.format(localeString("fatalErrorAtLine"),
				     e.getLineNumber(),
				     e.getMessage()):
		String.format(localeString("fatalErrorAtLineFN"),
				     xmlFilename,
				     e.getLineNumber(),
				     e.getMessage());
            displayMessage(msg, localeString("fatalErrorAtLineTitle"));
            // System.err.println(msg);
            errorSeen = true;
        }

       public InputSource resolveEntity(String publicID, 
                                         String systemID)
            throws SAXException, IOException
        {
	    if (publicID != null) {
		if (publicID.equals(PUBLICID)) {
		    systemID = OUR_SYSTEMID;
		    publicIDSeen = true;
		} else {
		    throw new SAXException
			(errorMsg("illegalPublicID", publicID));
		}
	    } else {
		throw new SAXException(errorMsg("missingPublicID"));
	    }
            if (systemID.matches("resource:.*")) {
                // our DTD is built into the applications JAR file.
                String resource = systemID.substring(10);
                try {
                    if (resource.endsWith(".dtd")) {
			// open URL
                        InputStream stream = (new URL(systemID)).openStream();
			if (stream == null) {
			    throw new IOException(errorMsg("ioError"));
			} else {
			    return new InputSource(stream);
			}
                    } else {
			throw new Exception
			    (errorMsg("illegalSystemID", resource));
                    }
                } catch (Exception e) {
                    String msg = 
			errorMsg("resolveEntity", systemID, e.getMessage());
                    throw new SAXException(msg);
                }
            } else  {
                return null;
            }
        }
    }
}
