/**
 * Class representing a table row.
 */
public class PointTMR {

    String varname;
    Enum<?> mode;
    double x;
    double y;
    double xp;
    double yp;

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
