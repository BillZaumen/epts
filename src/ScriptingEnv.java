import org.bzdev.anim2d.*;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.graphs.Graph;
import org.bzdev.graphs.RefPointName;
import org.bzdev.scripting.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import javax.script.ScriptException;

public class ScriptingEnv {
    private String a2dName;
    private String languageName;
    private Animation2D a2d = null;
    private ScriptingContext scripting;
    public static class EPTSInfo {
	private int frameWidth;
	private int frameHeight;
	private double userDist = 1.0;
	private double gcsDist = 1.0;
	private boolean hasDistances = false;
	private double xorigin = 0.0;
	private double yorigin = 0.0;
	private RefPointName refpointName = RefPointName.LOWER_LEFT;

	private double xfract = 0.0;
	private double yfract = 0.0;

	public int getWidth() {return frameWidth;}
	public int getHeight() {return frameHeight;}
	public boolean hasDistances() {return hasDistances;}
	public double getUserDist() {return userDist;}
	public double getGCSDist() {return gcsDist;}
	public double getXOrigin() {return xorigin;}
	public double getYOrigin() {return yorigin;}
	public RefPointName getRefPointName() {return refpointName;}
	public double getXFract() {return xfract;}
	public double getYFract() {return yfract;}

	public EPTSInfo(int w, int h) {
	    frameWidth = w;
	    frameHeight = h;
	}

	public EPTSInfo(int w, int h, double ud, double gcsd,
			RefPointName rpn,
			double xorigin, double yorigin)
	{
	    frameWidth = w;
	    frameHeight = h;
	    userDist = ud;
	    gcsDist = gcsd;
	    hasDistances = true;
	    this.xorigin = xorigin;
	    this.yorigin = yorigin;
	    this.refpointName = rpn;
	    switch(rpn) {
	    case CENTER:
		xfract = 0.5;
		yfract = 0.5;
		break;
	    case CENTER_LEFT:
		xfract = 0.0;
		yfract = 0.5;
		break;
	    case CENTER_RIGHT:
		xfract = 1.0;
		yfract = 0.5;
		break;
	    case LOWER_CENTER:
		xfract = 0.5;
		yfract = 0.0;
		break;
	    case LOWER_LEFT:
		xfract = 0.0;
		yfract = 0.0;
		break;
	    case LOWER_RIGHT:
		xfract = 1.0;
		yfract = 0.0;
		break;
	    case UPPER_CENTER:
		xfract = 0.5;
		yfract = 1.0;
		break;
	    case UPPER_LEFT:
		xfract = 0.0;
		yfract = 1.0;
		break;
	    case UPPER_RIGHT:
		xfract = 1.0;
		yfract = 1.0;
		break;
	    }
	}
    }

    EPTSInfo epts = null;

    public String getAnimationName() {return (a2dName == null)? "a2d": a2dName;}
    public String getLanguageName() {return languageName;}

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static  String localeString(String key) {
	return EPTS.localeString(key);
    }

    public ScriptingEnv(String languageName, String a2dName)
	throws SecurityException
    {
	this.a2dName = a2dName;
	this.languageName = languageName;
	scripting = new ExtendedScriptingContext
	    (new DefaultScriptingContext(languageName, false));
	ScriptingSecurityManager sm = new ScriptingSecurityManager();
	scripting.putScriptObject("scripting", scripting);
	scripting.putScriptObject("epts", epts);
	System.setSecurityManager(sm);
    }

    public ScriptingEnv(String languageName, String a2dName,
			int width, int height)
	throws SecurityException
    {
	this.a2dName = a2dName;
	this.languageName = languageName;
	scripting = new ExtendedScriptingContext
	    (new DefaultScriptingContext(languageName, false));
	ScriptingSecurityManager sm = new ScriptingSecurityManager();
	scripting.putScriptObject("scripting", scripting);
	epts = new EPTSInfo(width, height);
	scripting.putScriptObject("epts", epts);
	System.setSecurityManager(sm);
    }

    public ScriptingEnv(String languageName, String a2dName,
			int width, int height,
			double userDist, double gcsDist,
			RefPointName rpn, double xorigin, double yorigin)
	throws SecurityException
    {
	this.a2dName = a2dName;
	this.languageName = languageName;
	scripting = new ExtendedScriptingContext
	    (new DefaultScriptingContext(languageName, false));
	ScriptingSecurityManager sm = new ScriptingSecurityManager();
	scripting.putScriptObject("scripting", scripting);
	epts = new EPTSInfo(width, height, userDist, gcsDist,
			    rpn, xorigin, yorigin);
	scripting.putScriptObject("epts", epts);
	System.setSecurityManager(sm);
    }

    public boolean canRescale() {
	return !(xlo != 0.0 || xuo != 0.0 || ylo != 0.0 || yuo != 0.0);
    }

    static double getXLL(double scaleFactor, RefPointName rpn,
			 double width, double xrefpoint)
    {
	switch(rpn) {
	case CENTER:
	    xrefpoint -= width*scaleFactor / 2;
	    break;
	case CENTER_LEFT:
	    break;
	case CENTER_RIGHT:
	    xrefpoint -= width*scaleFactor;
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
	    break;
	case UPPER_LEFT:
	    break;
	case UPPER_RIGHT:
	    xrefpoint -= width*scaleFactor;
	    break;
	}
	return xrefpoint;
    }

    static double getYLL(double scaleFactor, RefPointName rpn,
			 double height, double yrefpoint)
    {
	switch(rpn) {
	case CENTER:
	    yrefpoint -= height*scaleFactor / 2;
	    break;
	case CENTER_LEFT:
	    yrefpoint -= height*scaleFactor / 2;
	    break;
	case CENTER_RIGHT:
	    yrefpoint -= height*scaleFactor / 2;
	    break;
	case LOWER_CENTER:
	    break;
	case LOWER_LEFT:
	    break;
	case LOWER_RIGHT:
	    break;
	case UPPER_CENTER:
	    yrefpoint -= height*scaleFactor;
	    break;
	case UPPER_LEFT:
	    yrefpoint -= height*scaleFactor;
	    break;
	case UPPER_RIGHT:
	    yrefpoint -= height*scaleFactor;
	    break;
	}
	return yrefpoint;
    }

    public void rescale(double scalef, RefPointName rpn,
			double xrefpoint, double yrefpoint)
    {
	if (xlo != 0.0 || xuo != 0.0 || ylo != 0.0 || yuo != 0.0) {
	    throw new IllegalStateException(errorMsg("noOffsetsAllowed"));
	}

	if (epts != null && epts.hasDistances()) {
	    xl = getXLL(scalef, rpn, epts.getWidth(), xrefpoint);
	    yl = getYLL(scalef, rpn, epts.getHeight(),yrefpoint);
	    xu = xl + (epts.getWidth()) * scalef;
	    yu = yl + (epts.getHeight()) * scalef;
	    epts = new EPTSInfo(epts.getWidth(), epts.getHeight(),
				scalef, 1.0, rpn, xrefpoint,
				yrefpoint);
	    scripting.putScriptObject("epts", epts);
	    zoomMap.clear();
	}
    }

    public void putScriptObject(String name, Object object)
    {
	scripting.putScriptObject(name, object);
    }

    public void evalScript(String fileName, Reader reader)
	throws ScriptException
    {
	// make sure that scripting and epts have not been
	// modified by another script.
	scripting.putScriptObject("scripting", scripting);
	scripting.putScriptObject("epts", epts);
	if (fileName.startsWith("sresource:")
	    && a2dName != null
	    && !a2dName.equals("a2d")
	    && scripting.containsScriptObject(a2dName)) {
	    scripting.putScriptObject("a2d",
				      scripting.getScriptObject(a2dName));
	}
	scripting.evalScript(fileName, reader);
    }

    Graph graph = null;
    // x and y upper and lower offsets
    int xlo, ylo, xuo, yuo;
    // upper and lower
    double xl, yl, xu, yu;
    Graph.ImageType gitype;
    Color backgroundColor;
    Graph.FontParms fp;

    private void fetchA2d() throws ScriptException {
	if (a2d == null) {
	    Object obj =
		scripting.getScriptObject((a2dName == null)? "a2d": a2dName);
	    if (obj != null && obj.getClass().equals(Animation2D.class)) {
		a2d = (Animation2D) obj;
	    } else {
		throw new ScriptException
		    (errorMsg("noA2d", getAnimationName()));
	    }
	    // call initFrames, which is necessary to create a graph.
	    // no frames will be scheduled because the maximum frame
	    // count is zero.
	    try {
		a2d.initFrames(0, "img", "png");
		graph = a2d.getGraph();
		xlo = graph.getXLowerOffset();
		ylo = graph.getYLowerOffset();
		xuo = graph.getXUpperOffset();
		yuo = graph.getYUpperOffset();
		xl = graph.getXLower();
		yl = graph.getYLower();
		xu = graph.getXUpper();
		yu = graph.getYUpper();
		gitype  = graph.getImageType();
		backgroundColor = graph.getBackgroundColor();
		fp = graph.getFontParms();
	    } catch (IOException eio) {
		throw new UnexpectedExceptionError(eio);
	    }
	}
    }

    public Graph getGraph() throws ScriptException {
	if (graph == null) {
	    fetchA2d();
	}
	zoomMap.put(1.0, graph.getImage());
	return graph;
    }

    public void drawGraph(Image background) throws Exception {
	fetchA2d();
	// Graph graph = a2d.getGraph();
	Graphics2D g2d  = graph.createGraphics();
	Graphics2D g2dGCS = graph.createGraphicsGCS();

	if(background != null) {
	    g2d.drawImage(background, identityAF, null);
	}

	Set<AnimationObject2D> objects = a2d.getObjectsByZorder();
	for (AnimationObject2D ao: objects) {
	    ao.addTo(graph, g2d, g2dGCS);
	}
    }

    // so we can cache the image to use for each zoom factor.
    private HashMap<Double,BufferedImage> zoomMap = new HashMap<>();

    static AffineTransform identityAF = new AffineTransform();

    private Image lastBackground = null;

    public BufferedImage zoomImage(double zoom, Image background)
    {
	BufferedImage img;
	if (lastBackground != null && lastBackground != background) {
	    // System.out.println("clearing zoom map in zoomImage");
	    zoomMap.clear();
	}
	lastBackground = background;
	img = zoomMap.get(zoom);
	if (img != null) return img;
	double a2dw = a2d.getWidth();
	double a2dh = a2d.getHeight();
	int w, h;
	if (zoom == 1.0) {
	    w = (int)Math.round(a2dw);
	    h = (int)Math.round(a2dh);
	} else {
	    w = (int) Math.round(a2dw*zoom);
	    h = (int) Math.round(a2dh*zoom);
	}
	Graph g = new Graph(w, h, gitype);
	g.setOffsets(xlo, xuo, ylo, yuo);
	g.setRanges(xl, xu, yl, yu);
	g.setBackgroundColor(backgroundColor);
	g.setFontParms(fp);
	Graphics2D g2d  = g.createGraphics();
	Graphics2D g2dGCS = g.createGraphicsGCS();

	if (background != null) {
	    AffineTransform af = (zoom == 1)? identityAF:
		AffineTransform.getScaleInstance(zoom,zoom);
	    g2d.drawImage(background, af, null);
	}

	Set<AnimationObject2D> objects = a2d.getObjectsByZorder();
	for (AnimationObject2D ao: objects) {
	    ao.addTo(g, g2d, g2dGCS);
	}
	img = g.getImage();
	zoomMap.put(zoom, img);
	// System.out.println("recreating img; zoom = " + zoom);
	return img;
    }
}
