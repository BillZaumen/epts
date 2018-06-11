import org.bzdev.anim2d.*;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.graphs.Graph;
import org.bzdev.scripting.*;

import java.awt.Graphics2D;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import javax.script.ScriptException;


public class ScriptingEnv {
    private String a2dName;
    private Animation2D a2d = null;
    private ScriptingContext scripting;

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
	scripting = new ExtendedScriptingContext
	    (new DefaultScriptingContext(languageName, false));
	ScriptingSecurityManager sm = new ScriptingSecurityManager();
	scripting.putScriptObject("scripting", scripting);
	scripting.putScriptObject("epts", Boolean.TRUE);
	System.out.println("security manager set");
	System.setSecurityManager(sm);
    }

    public void putScriptObject(String name, Object object)
    {
	scripting.putScriptObject(name, object);
    }

    public void evalScript(String fileName, Reader reader)
	throws ScriptException
    {
	scripting.evalScript(fileName, reader);
    }

    private void fetchA2d() throws ScriptException {
	if (a2d == null) {
	    Object obj = scripting.getScriptObject(a2dName);
	    if (obj == null || obj.getClass().equals(Animation2D.class)) {
		a2d = (Animation2D) obj;
	    } else {
		throw new ScriptException(errorMsg("noA2d", a2dName));
	    }
	    // call initFrames, which is necessary to create a graph.
	    // no frames will be scheduled because the maximum frame
	    // count is zero.
	    try {
		a2d.initFrames(0, "img", "png");
	    } catch (IOException eio) {
		throw new UnexpectedExceptionError(eio);
	    }
	}
    }

    public Graph getGraph() throws ScriptException {
	fetchA2d();
	return a2d.getGraph();
    }

    public void drawGraph() throws Exception {
	fetchA2d();
	Graph graph = a2d.getGraph();
	Graphics2D g2d  = graph.createGraphics();
	Graphics2D g2dGCS = graph.createGraphicsGCS();

	Set<AnimationObject2D> objects = a2d.getObjectsByZorder();
	
	for (AnimationObject2D ao: objects) {
	    ao.addTo(graph, g2d, g2dGCS);
	}
    }
}
