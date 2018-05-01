import java.awt.Component;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import javax.xml.parsers.*;

import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.CPointType;
import org.bzdev.swing.ErrorMessage;

import org.xml.sax.helpers.*;
import org.xml.sax.*;


public class EPTSParser {

    static final String PUBLICID = "-//BZDev//EPTS-1.0//EN";
    static final String SYSTEMID = "sresource:epts-1.0.dtd";
    static final String NAMESPACE = "http://bzdev.org/DTD/epts-1.0";
    static final String OUR_SYSTEMID = "sresource:epts-1.0.dtd";

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
    URI imageURI = null;
    int unitIndex = 0;
    int refPointIndex = 0;
    
    // These are strings because of the way the configuration pane
    // is saved.
    String userSpaceDistance = "1.0";
    String gcsDistance = "1.0";
    String xorigin = "0.0";
    String yorigin = ").0";
    PointTMR[] rows = null;
    
    public int getWidth() {return width;}
    public int getHeight() {return height;}
    public String[] getArgArray() {return argArray;}
    public boolean imageURIExists() {return imageURIExists;}
    public URI getImageURI() {return imageURI;}
    public int getUnitIndex() {return unitIndex;}
    public int getRefPointIndex() {return refPointIndex;}
    public String getUserSpaceDistance() {return userSpaceDistance;}
    public String getGcsDistance() {return gcsDistance;}
    public String getXOrigin() {return xorigin;}
    public String getYOrigin() {return yorigin;}
       
    public PointTMR[] getRows() {return rows;}


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
	    ErrorMessage.display(comp, title, msg);
	} else {
	    ErrorMessage.display(msg);
	}
    }

    void displayMessage(Locator locator, 
			String msg, String title) {
	ErrorMessage.display(xmlFilename, locator.getLineNumber(), msg);
    }

    StringBuilder text = new StringBuilder();

    class OurDefaultHandler extends DefaultHandler {

	boolean processingXML = false;
	boolean processingTable = false;
	boolean errorSeen = false;
	boolean publicIDSeen = false;
        Locator locator = null;
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
 
	boolean mimeTypePISeen = false;

	ArrayList<String> argList = new ArrayList<>();
	ArrayList<PointTMR> rowList = new ArrayList<>();

	public void startDocument() {
	    locator = null;
	    errorSeen = false;
	    publicIDSeen = false;
	    text.setLength(0);
	    argList.clear();
	    rowList.clear();
	    processingXML = false;
	    processingTable = false;
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
		} catch (NumberFormatException e) {
		    throw new
			SAXException(errorMsg("widthError", e.getMessage()));
		}
		try {
		    height = Integer.parseInt(attr.getValue("height"));
		} catch (NumberFormatException e) {
		    throw new
			SAXException(errorMsg("heightError", e.getMessage()));
		}
		String s = attr.getValue("imageURIExists");
		if (s == null) s = "false";
		imageURIExists = s.equals("true");
	    } else if (qName.equals("targetList")) {
		argList.clear();
	    } else if (qName.equals("argument")) {
		text.setLength(0);
	    } else if (qName.equals("gcsconfig")) {
		unitIndex = Integer.parseInt(attr.getValue(unitIndex));
		refPointIndex = Integer.parseInt(attr.getValue(unitIndex));
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
		xorigin = attr.getValue("xorigin");
		try {
		    Double.parseDouble(xorigin);
		} catch (NumberFormatException e) {
		    throw new SAXException
			(errorMsg("xoriginError", e.getMessage()));
		}
		yorigin = attr.getValue("yorigin");
		try {
		    Double.parseDouble(yorigin);
		} catch (NumberFormatException e) {
		    throw new SAXException
			(errorMsg("yoriginError", e.getMessage()));
		}
	    } else if (qName.equals("table")) {
		processingTable = true;
		rowList.clear();
	    } else if (qName.equals("row")) {
		String varname = attr.getValue("varname");
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
	    }
	}

	String theURL = null;

        public void endElement(String uri, String localName, String qName)
            throws SAXException
        {
	    if (qName.equals("epts")) {
		processingXML = false;
	    } else if (qName.equals("targetList")) {
		argArray = new String[argList.size()];
	    } else if (qName.equals("argument")) {
		if (imageURIExists && imageURI == null) {
		    try {
			imageURI = new URI(text.toString());
		    } catch (URISyntaxException e) {
			String msg = errorMsg("badURI", text.toString());
			error(msg);
			throw new SAXException(msg);
		    }
		} else {
		    argList.add(text.toString());
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
            if (systemID.matches("sresource:.*")) {
                // our DTD is built into the applications JAR file.
                String resource = systemID.substring(10);
                try {
                    if (resource.endsWith(".dtd")) {
                        InputStream stream =
                            ClassLoader.getSystemResourceAsStream(resource);
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
