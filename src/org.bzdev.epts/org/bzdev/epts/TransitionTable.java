package org.bzdev.epts;

import org.bzdev.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
class Transition {
    Enum trigger;
    LinkedHashSet<Enum> next = new LinkedHashSet<>();
    int mode = 0;
    static final int AT_MOST_TWO = 1;
    static final int NO_CLOSE_AFTER_SEG_END = 2;

    Transition(Enum trigger, Enum[] next) {
	this.trigger = trigger;
	for (Enum e: next) {
	    this.next.add(e);
	}
    }

    Transition(Enum trigger, Enum[] next, int mode) {
	this(trigger, next);
	this.mode = mode;
    }
}

public class TransitionTable {

    static String errorMsg(String key, Object... args) {
	return EPTS.errorMsg(key, args);
    }

    static String localeString(String key) {
	return EPTS.localeString(key);
    }

    public static class Pair {
	Enum state;
	JMenuItem menuItem;
	Pair(Enum state, JMenuItem menuItem) {
	    this.state = state;
	    this.menuItem = menuItem;
	}
	public Enum getState() {return state;}
	public JMenuItem getMenuItem() {return menuItem;}
    }

    static LinkedList<JMenuItem> menuItems = new LinkedList<>();
    static LinkedList<Pair> menuItemsWithState = new LinkedList<>();
    static HashMap<Enum,JMenuItem> menuItemMap = new HashMap<>();
    static HashMap<Enum,String> accelMap = new HashMap<>();
    private static ButtonGroup bg = new ButtonGroup();

    private static JMenuItem vectorMenuItem =
	new JMenuItem(localeString("Vector"));
    private static JMenuItem pipeMenuItem =
	new JMenuItem(localeString("Pipe"));
    private static JMenuItem shiftMenuItem =
	new JMenuItem(localeString("Shift"));

    private static JMenuItem arcMenuItem = new JMenuItem(localeString("Arc"));
    private static JMenuItem gotoMenuItem =
	new JMenuItem(localeString("GotoExistingPoint"));
    private static JMenuItem locMenuItem = new JMenuItem(localeString("Loc"));
    private static JMenuItem offsetMenuItem =
	new JMenuItem(localeString("OffsetMenuItem"));

    static {
	vectorMenuItem.setEnabled(false);
	vectorMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0));
	pipeMenuItem.setEnabled(false);
	pipeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
	shiftMenuItem.setEnabled(false);
	shiftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
	arcMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
	arcMenuItem.setEnabled(false);
	gotoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0));
	gotoMenuItem.setEnabled(false);
	locMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
	locMenuItem.setEnabled(false);
	offsetMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK));
	offsetMenuItem.setEnabled(false);
    }

    static JMenuItem getMenuItem(Enum state) {
	return menuItemMap.get(state);
    }

    static JMenuItem getVectorMenuItem() {return vectorMenuItem;}
    static JMenuItem getPipeMenuItem() {return pipeMenuItem;}
    static JMenuItem getShiftMenuItem() {return shiftMenuItem;}
    static JMenuItem getArcMenuItem() {return arcMenuItem;}
    static JMenuItem getGotoMenuItem() {return gotoMenuItem;}
    static JMenuItem getLocMenuItem() {return locMenuItem;}
    static JMenuItem getOffsetMenuItem() {return offsetMenuItem;}

    private static void createMenuItem(Enum state, String label, int keycode,
				       boolean enabled, boolean selected)
    {
	if (state instanceof SplinePathBuilder.CPointType &&
	    state != SplinePathBuilder.CPointType.CLOSE) {
	    JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(label);
	    menuItem.setAccelerator(KeyStroke.getKeyStroke(keycode, 0));
	    menuItem.setEnabled(enabled);
	    menuItem.setSelected(selected);
	    bg.add(menuItem);
	    menuItems.add(menuItem);
	    menuItemMap.put(state, menuItem);
	    menuItemsWithState.add(new Pair(state, menuItem));
	} else {
	    JMenuItem menuItem = new JMenuItem(label);
	    menuItem.setAccelerator(KeyStroke.getKeyStroke(keycode, 0));
	    menuItem.setEnabled(enabled);
	    menuItems.add(menuItem);
	    menuItemMap.put(state, menuItem);
	    menuItemsWithState.add(new Pair(state, menuItem));
	}
    }

    static {
	createMenuItem(SplinePathBuilder.CPointType.SPLINE,
		       localeString("SplinePoints"),
		       KeyEvent.VK_S, false, false);
	createMenuItem(SplinePathBuilder.CPointType.CONTROL,
		       localeString("ControlPoints"),
		       KeyEvent.VK_C, false, false);
	createMenuItem(SplinePathBuilder.CPointType.SEG_END,
		       localeString("EndCurveLineSegment"),
		       KeyEvent.VK_E, false, false);
	createMenuItem(SplinePathBuilder.CPointType.CLOSE,
		       localeString("Loop"), KeyEvent.VK_L, false, false);
	createMenuItem(EPTS.Mode.PATH_END, localeString("PathEnded"),
		       KeyEvent.VK_Z, false, false);
    }
    
    public static List<JMenuItem> getMenuItems() {
	return Collections.unmodifiableList(menuItems);
    }

    public static List<Pair> getMenuItemsWithState() {
	return Collections.unmodifiableList(menuItemsWithState);
    }

    static Enum enum0[] = {SplinePathBuilder.CPointType.MOVE_TO};

    static Enum enum1[] = {SplinePathBuilder.CPointType.SPLINE,
			   SplinePathBuilder.CPointType.CONTROL,
			   SplinePathBuilder.CPointType.SEG_END};

    static Enum enum2[] = {SplinePathBuilder.CPointType.SPLINE,
			   SplinePathBuilder.CPointType.CONTROL,
			   SplinePathBuilder.CPointType.SEG_END,
			   SplinePathBuilder.CPointType.CLOSE};

    static Enum enum3[] = {SplinePathBuilder.CPointType.CONTROL,
			   SplinePathBuilder.CPointType.SPLINE,
			   SplinePathBuilder.CPointType.SEG_END,
			   SplinePathBuilder.CPointType.CLOSE};

    static Enum enum4[] = {SplinePathBuilder.CPointType.SEG_END,
			   SplinePathBuilder.CPointType.SPLINE,
			   SplinePathBuilder.CPointType.CONTROL,
			   SplinePathBuilder.CPointType.CLOSE,
			   EPTS.Mode.PATH_END};
    static Enum enum5[] = {EPTS.Mode.PATH_END};

    static Transition ttable[] = {
	new Transition(EPTS.Mode.PATH_START, enum0),
	new Transition(SplinePathBuilder.CPointType.MOVE_TO, enum1),
	new Transition(SplinePathBuilder.CPointType.SPLINE, enum2,
		       Transition.NO_CLOSE_AFTER_SEG_END),
	new Transition(SplinePathBuilder.CPointType.CONTROL, enum3,
		       Transition.AT_MOST_TWO),
	new Transition(SplinePathBuilder.CPointType.SEG_END, enum4),
	new Transition(SplinePathBuilder.CPointType.CLOSE, enum5)
    };


    HashMap<Enum,Transition> transitions = new HashMap<>();
    Enum currentState;
    int controlPointCount;
    boolean passedSE = false;
    public TransitionTable() {
	for (Transition t: ttable) {
	    Enum current = t.trigger;
	    transitions.put(current, t);
	}
	currentState = EPTS.Mode.PATH_START;
	controlPointCount = 0;
	passedSE = false;
	for (Enum state: menuItemMap.keySet()) {
	    JMenuItem item = menuItemMap.get(state);
	    item.setEnabled(state == currentState);
	    item.setSelected(false);
	}
	vectorMenuItem.setEnabled(false);
	pipeMenuItem.setEnabled(false);
	shiftMenuItem.setEnabled(false);
	arcMenuItem.setEnabled(false);
	gotoMenuItem.setEnabled(false);
	locMenuItem.setEnabled(false);
    }

    public Enum getCurrentState() {return currentState;}

    int successiveCount = 0;

    public boolean setState(PointTableModel ptmodel, int index) {
	// rewind and replay - infrequent operation and number
	// of iterations should be negligible.
	int start = index;
	PointTMR row = ptmodel.getRow(index);
	Enum mode = row.getMode();
	if (mode != EPTS.Mode.PATH_START &&
	    mode != EPTS.Mode.PATH_END &&
	    !(mode instanceof SplinePathBuilder.CPointType)) {
	    throw new IllegalArgumentException(errorMsg("illegalState", mode));
	}
	while (start > 0 && mode != EPTS.Mode.PATH_START) {
	    start--;
	    row = ptmodel.getRow(start);
	    mode = row.getMode();
	}
	successiveCount = 0;
	passedSE = false;
	currentState = mode;
	vectorMenuItem.setEnabled(false);
	pipeMenuItem.setEnabled(true);
	shiftMenuItem.setEnabled(false);
	arcMenuItem.setEnabled(false);
	boolean result = false;
	for (int i = start+1 ; i <= index; i++) {
	    result = nextState(ptmodel.getRow(i).getMode());
	}
	return result;
    }

    /**
     * Go to the next state.
     * @return true if there is only one menu selection; false otherwise.
     */
    public boolean nextState(Enum newState) {
	/*
	System.out.println("currentState = " + currentState
			   + ", newState = "  + newState);
	*/
	if (newState == EPTS.Mode.PATH_END) {
	    if (currentState == EPTS.Mode.PATH_END ||
		currentState == EPTS.Mode.PATH_START) {
		throw new IllegalArgumentException(errorMsg("successivePath"));
	    }
	    currentState = EPTS.Mode.PATH_END;
	    vectorMenuItem.setEnabled(false);
	    pipeMenuItem.setEnabled(false);
	    shiftMenuItem.setEnabled(false);
	    arcMenuItem.setEnabled(false);
	    for (Enum state: menuItemMap.keySet()) {
		JMenuItem item = menuItemMap.get(state);
		item.setEnabled(false);
		item.setSelected(false);
	    }
	    return false;
	}
	Transition t = transitions.get(currentState);
	boolean splineNotAllowed = false; // true to disable SPLINE menu item
	if (!t.next.contains(newState)) {
	    String msg = errorMsg("stateSuccession", newState, currentState);
	    throw new IllegalArgumentException(msg);
	} else {
	    if (t.mode == Transition.NO_CLOSE_AFTER_SEG_END && passedSE
		&& newState == SplinePathBuilder.CPointType.CLOSE) {
		throw new IllegalArgumentException(errorMsg("closeNotAllowed"));
	    } else if (t.mode == Transition.AT_MOST_TWO) {
		if (newState == currentState) {
		    successiveCount++;
		    if (successiveCount > 2) {
			successiveCount--;
			throw new IllegalArgumentException
			    (errorMsg("successiveControlPoints"));
		    } else if (successiveCount == 2) {
			splineNotAllowed = true;
		    }
		} else  {
		    successiveCount = 0;
		}
	    }
	    /*
	    System.out.println("splineNotAllowed = " + splineNotAllowed
			       + ", successiveCount = " + successiveCount);
	    */
	    Transition nt = transitions.get(newState);
	    if (nt.trigger == SplinePathBuilder.CPointType.SEG_END) {
		passedSE = true;
	    }
	    if (nt.mode == Transition.AT_MOST_TWO) {
		if (newState != currentState) {
		    successiveCount = 1;
		}
	    }
	    int count = 0;
	    if (successiveCount == 1
		&& currentState == SplinePathBuilder.CPointType.SPLINE) {
		splineNotAllowed = true;
	    }
	    for (Enum state: menuItemMap.keySet()) {
		JMenuItem item = menuItemMap.get(state);
		if (nt.next.contains(state)) {
		    if (state == newState
			&& newState == SplinePathBuilder.CPointType.CONTROL
			&& currentState == SplinePathBuilder.CPointType.SPLINE
			&& successiveCount == 1) {
			item.setEnabled(false);
			menuItemMap.get(SplinePathBuilder.CPointType.SPLINE)
			    .setEnabled(false);
		    } else if (successiveCount == 2 && state == newState) {
			/*
			System.out.println("... handling successiveCount == 2"
					   + ", " + splineNotAllowed);
			*/
			item.setEnabled(false);
			if (splineNotAllowed) {
			    menuItemMap.get(SplinePathBuilder.CPointType.SPLINE)
				.setEnabled(false);
			}
		    } else if (state == SplinePathBuilder.CPointType.CLOSE
			       && nt.mode
			       == Transition.NO_CLOSE_AFTER_SEG_END
			       && passedSE) {
			item.setEnabled(false);
		    } else {
			if (splineNotAllowed
			    && state == SplinePathBuilder.CPointType.SPLINE) {
			    // the order of iteration for 'state' is not
			    // predictable because menuItemMap is a hash map.
			    item.setEnabled(false);
			} else {
			    item.setEnabled(true);
			}
			count++;
		    }
		} else {
		    item.setEnabled(false);
		}
	    }
	    /*
	    if (splineNotAllowed) {
		JMenuItem smi =
		    menuItemMap.get(SplinePathBuilder.CPointType.SPLINE);
		if (smi.isEnabled()) {
		    System.out.println("... trying again - "
				       + currentState + " -> " + newState);
		    smi.setEnabled(false);
		}
	    }
	    */
	    for (Enum state: nt.next) {
		JMenuItem item = menuItemMap.get(state);
		if (item.isEnabled()) {
		    if (item instanceof JRadioButtonMenuItem) {
			// item.doClick();
			// Note: calling doClick() is very slow, so
			// we just just use the action listeners directly.
			ActionEvent event =
			    (new ActionEvent(item,
					     ActionEvent.ACTION_PERFORMED,
					     item.getActionCommand()));
			for (ActionListener listener:
				 item.getActionListeners()) {
			    listener.actionPerformed(event);
			}
		    }
		    break;
		}
	    }
	    boolean result = (count == 1);
	    if (newState == EPTS.Mode.PATH_END) {
		result = false;
		for (Enum state: menuItemMap.keySet()) {
		    JMenuItem item = menuItemMap.get(state);
		    if (item.isEnabled()) {
			if (item instanceof JRadioButtonMenuItem) {
			    ((JRadioButtonMenuItem)item).setSelected(false);
			}
		    }
		}
	    } else if (newState == SplinePathBuilder.CPointType.SPLINE
		       || newState == SplinePathBuilder.CPointType.CONTROL) {
		// This helps implement a shortcut.  For the SPLINE and
		// CONTROL case, we'll allow a PATH_END but the menu's
		// action listener will respond by automatically changing
		// the last entry's mode to a SEG_END.
		JMenuItem item = menuItemMap.get(EPTS.Mode.PATH_END);
		item.setEnabled(true);
	    }
	    currentState = newState;
	    if (currentState == SplinePathBuilder.CPointType.MOVE_TO) {
		vectorMenuItem.setEnabled(true);
		pipeMenuItem.setEnabled(true);
		gotoMenuItem.setEnabled(true);
		arcMenuItem.setEnabled(true);
	    } else if (currentState == SplinePathBuilder.CPointType.SEG_END) {
		vectorMenuItem.setEnabled(true);
		pipeMenuItem.setEnabled(true);
		arcMenuItem.setEnabled(true);
	    } else {
		vectorMenuItem.setEnabled(false);
		boolean pipestate = (currentState == null
				     || currentState == EPTS.Mode.PATH_START);
		pipeMenuItem.setEnabled(pipestate);
		arcMenuItem.setEnabled(false);
	    }
	    if (currentState != EPTS.Mode.PATH_END
		&& currentState != EPTS.Mode.LOCATION) {
		gotoMenuItem.setEnabled(true);
		shiftMenuItem
		    .setEnabled(currentState != null
				&& currentState != EPTS.Mode.PATH_START);
		locMenuItem.setEnabled(true);
	    } else {
		gotoMenuItem.setEnabled(false);
		locMenuItem.setEnabled(false);
		shiftMenuItem.setEnabled(false);
	    }
	    return result; 
	}
    }
}
