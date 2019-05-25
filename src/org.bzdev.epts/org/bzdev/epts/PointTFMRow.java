package org.bzdev.epts;

public class PointTFMRow {
    String varname;
    PointTMR.FilterMode mode = PointTMR.FilterMode.DEFAULT;

    public PointTMR.FilterMode getMode() {return mode;}

    protected void setMode(PointTMR.FilterMode mode) {
	this.mode = mode;
    }

    public String getVariableName() {return varname;}

    public PointTFMRow(String varname) {
	this.varname = varname;
    }
}
