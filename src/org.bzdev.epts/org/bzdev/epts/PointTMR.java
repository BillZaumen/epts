package org.bzdev.epts;

/**
 * Class representing a table row.
 */
public class PointTMR {

    static String localeString(String key) {
	return EPTS.localeString(key);
    }


    public enum FilterMode {
	/**
	 * Use a filter-specified default
	 */
	DEFAULT,
	/**
	 * Points on a row's path can be selected and drawn.
	 */
	SELECTABLE,
	/**
	 * Points on a row's path can be drawn but not selected.
	 */
	DRAWABLE,
	/**
	 * The path is invisible, which implies it is not drawable and
	 * not selectable.
	 */
	INVISIBLE;

	public String toString() {return localeString(name());}
    }


    String varname;
    Enum<?> mode;
    double x;
    double y;
    double xp;
    double yp;

    private boolean selectable = true;
    private boolean drawable = true;

    public boolean isSelectable() {return selectable;}
    public boolean isDrawable() {return drawable;}

    public void setFilterMode(FilterMode mode) {
	if (mode == null) mode = FilterMode.SELECTABLE;
	switch (mode) {
	case DEFAULT:
	    break;
	case SELECTABLE:
	    selectable = true;
	    drawable = true;
	    break;
	case DRAWABLE:
	    selectable = false;
	    drawable = true;
	    break;
	case INVISIBLE:
	    selectable = false;
	    drawable = false;
	    break;
	}
    }

    public PointTMR(String varname, Enum mode, double x, double y,
		    double xp, double yp) {
	this.varname = (varname == null)? varname: varname.trim();
	this.mode = mode;
	this.x = x;
	this.y = y;
	this.xp = xp;
	this.yp = yp;
    }

    public Enum<?> getMode() {return mode;}

    protected void setMode(Enum mode) {
	this.mode = mode;
    }

    public String getVariableName() {return varname;}

    public double getX() {return x;}

    public double getXP() {
	return xp;
    }

    public double getY() {return y;}

    public double getYP() {
	return yp;
    }

    public void setX(double x, double xp) {
	this.x = x;
	this.xp = xp;
    }

    public void setY(double y, double yp) {
	this.y = y;
	this.yp = yp;
    }

}
