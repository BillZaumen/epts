// Script to create a background showing polar coordinates
// The following variables may be set:
//
//    * frameWidth - an integer giving the width of the background image
//                   that will be created. The default is 1920. The value
//                   is ignored if the animation was created by another
//                   script. If undefined or null, the default will be used.
//    * frameHeight - an integer giving the height of the background image
//                    that will be created. The default is 1080. The value
//                    is ignored if the animation was created by another
//                    script. If undefined or null, the default will be used.
//    * userdist - a double giving a reference distance in user space. The
//                 default is 1.0. The ratio userdist/gcsdist is the
//                 scaling factor for converting distances in graph coordinate
//                 space to user space. if undefined or null, the default
//                 will be used.
//    * gcsdist -  a double giving a reference distance in graph coordinate
//                 space. The default is 1.0. The ratio userdist/gcsdist is the
//                 scaling factor for converting distances in graph coordinate
//                 space to user space. If undefined or null, the default will
//                 be used.
//    * xorigin - a double giving the X coordinate in graph coordinate space
//                for a reference point in the frame. The default is 0.0.
//                The value is ignored if the animation was created by
//                another script. if undefined or null, the default will be
//                used.
//    * yorigin - a double giving the Y coordinate in graph coordinate space
//                for a reference point in the frame. The  default is 0.0.
//                The value is ignored if the animation was created by
//                another script. If undefined or null, the default will be
//                used.
//    * xfract - a double giving the fraction of the frame width at which
//               the reference point appears (0.0 is the left edge and 1.0
//               is the right edge). The default is 0.0. The value is
//               ignored if the animation was created by another script.
//               If undefined or null, the default will be used.
//    * yfract - a double giving the fraction of the frame height at which
//               the reference point appears (0.0 is the lower edge and 1.0
//               is the upper edge). The default is 0.0. The value is
//               ignored if the animation was created by another script.
//               If undefined or null, the default will be used.
//   *  fractional - true if the grid origin's coordinates
//                   (gridXOrigin, gridYOrigin) are a fraction of the
//                   position in the frame (excluding offsets); false
//                   if absolute values in graph coordinate space are
//                   used. If undefined or null, a default will be used.
//    * gridXOrigin - a double giving the X component of the grid's
//                    origin.  When fractional is true, the value is
//                    a fraction of the position in the frame, excluding
//                    offsets, in the X direction. When false, it is the
//                    X coordinate of the origin for a polar coordinate
//                    grid, given in graph-coordinate-space units. If
//                    undefined or null, a default value will be used.
//    * gridXOrigin - a double giving the Y component of the grid's
//                    origin.  When fractional is true, the value is
//                    a fraction of the position in the frame, excluding
//                    offsets, in the Y direction. When false, it is the
//                    Y coordinate of the origin for a polar coordinate
//                    grid, given in graph-coordinate-space units. If
//                    undefined or null, a default value will be used.
//    * radialSpacing - a double giving the radial spacing for concentric
//                      circles; null, undefined, 0.0 or negative for a default.
//    * angularSpacing  - the angular spacing in degrees for radial lines.
//                        The value should be a divisor of 90 degrees or
//                        either null or undefined for a default.
//    * gridColor - the grid-line color provided as a CSS string,
//                  undefined or null for the default.
//    * strokeWidth - a double giving the width of the stroke used to create
//                    grid lines; undefined or null for a default.
//    * gridZorder - an integer giving the z-order to use for the object
//                   that creates the grid. The default is the javascript
//                   value java.lang.Long.MAX_VALUE (2^53 - 1).

scripting.importClass("org.bzdev.anim2d.Animation2D");
scripting.importClass("org.bzdev.anim2d.AnimationLayer2DFactory");
scripting.importClass("org.bzdev.anim2d.PolarGridFactory");

if (typeof frameWidth === 'undefined' || frameWidth == null) {
    if (typeof a2d === 'undefined' || a2d == null) {
	if (typeof epts === 'undefined' || epts == null) {
	    var frameWidth = 1920;
	} else {
	    var frameWidth = epts.getWidth();
	}
    } else {
	var frameWidth = a2d.getWidthAsInt();
    }
}
if (typeof frameHeight === 'undefined' || frameHeight == null) {
    if (typeof a2d === 'undefined' || a2d == null) {
	if (typeof epts === 'undefined' || epts == null) {
	    var frameHeight = 1080;
	} else {
	    var frameHeight = epts.getHeight();
	}
    } else {
	var frameHeight = a2d.getHeightAsInt();
    }
}

if (typeof userdist === 'undefined' || userdist == null) {
    if (typeof epts == 'undefined' || epts == null || !epts.hasDistances()) {
	var userdist = 1.0;
    } else {
	var userdist = epts.getUserDist();
    }
}

if (typeof gcsdist == 'undefined' || gcsdist == null) {
    if (typeof epts == 'undefined' || epts == null || !epts.hasDistances()) {
	var gcsdist = 1.0;
    } else {
	var gcsdist = epts.getGCSDist();
    }
}

if (typeof xorigin === 'undefined') {
    if (typeof epts == 'undefined' || epts == null || !epts.hasDistances()) {
	var xorigin = 0.0;
    } else {
	var xorigin = epts.getXOrigin();
    }
}

if (typeof yorigin === 'undefined') {
    if (typeof epts == 'undefined' || epts == null || !epts.hasDistances()) {
	var yorigin = 0.0;
    } else {
	var yorigin = epts.getYOrigin();
    }
}

if (typeof xfract === 'undefined') {
    if (typeof epts == 'undefined' || epts == null || !epts.hasDistances()) {
	var xfract = 0.0;
    } else {
	var xfract = epts.getXFract();
    }
}

if (typeof fractional === 'undefined' || fractional == null) {
    var fractional = true;
} else {
    if (fractional != true && fractional != false) {
	throw "The variable 'fractional' is not a boolean"
    }
}

if (typeof gridXOrigin === 'undefined' || gridXOrigin == null) {
    var gridXOrigin = fractional? 0.5: 0.0;
}

if (typeof gridYOrigin === 'undefined' || gridYOrigin == null) {
    var gridYOrigin = fractional? 0.5: 0.0;
}

if (typeof gridZorder === 'undefined' || gridZorder == null) {
    var gridZorder = java.lang.Long.MAX_VALUE;
}

if (typeof radialSpacing === 'undefined') {
    var radialSpacing = null;
}

if (typeof angularSpacing === 'undefined') {
    var angularSpacing = null;
}

if (typeof strokeWidth === 'undefined') {
    var strokeWidth = null;
}

if (typeof gridColor === 'undefined') {
    var gridColor = null;
}

var a2d;

// Use a function because the 'let' keyword is not recognized
// by Nashorn and we want any variable not defined above  (and
// hence not documented) to be a local variable.  The keyword
// 'var' makes variables inside functions local.

(function() {
    var JMath = java.lang.Math;
    var plainBackground = false;
    if (typeof a2d === 'undefined' || a2d == null) {
	a2d = new Animation2D(scripting, frameWidth, frameHeight, 1000.0, 40);
	var scalef = userdist / gcsdist;
	a2d.setRanges(xorigin, yorigin, xfract, yfract, scalef, scalef);
	if (epts === 'undefined' || epts == null) {
	    plainBackground = true;
	}
    } else {
	var scalef = (a2d.getWidth() - a2d.getXLowerOffset()
		      - a2d.getXUpperOffset())
	    / (a2d.getXUpper() - a2d.getXLower());
    }
    var objects = [];

    if (plainBackground) {
	var xlower = a2d.getXLower();
	var xupper = a2d.getXUpper();
	var ylower = a2d.getYLower();
	var yupper = a2d.getYUpper();
	var alf = new AnimationLayer2DFactory(a2d);
	alf.configure([
	    {zorder: gridZorder, visible: true},
	    {withPrefix: "object",
	     withIndex: {type: "RECTANGLE", x: xlower, y: ylower,
			refPoint: "LOWER_LEFT",
			width: (xupper-xlower),
			height: (yupper-ylower),
			fill: true, draw: true,
			"stroke.width": 1.0,
			"fillColor.css": "white",
			"drawColor.css": "white"}}]);
	alf.createObject();
    }
    var i = 1;
    var spec = [{zorder: gridZorder, visible: true,
		 fractional: fractional,
		 xo: gridXOrigin, yo: gridYOrigin}];

    if (angularSpacing != null) {
	spec[i++] = {angularSpacing: angularSpacing};
    }

    if (radialSpacing != null) {
	spec[i++] = {radialSpacing: radialSpacing};
    }

    if (strokeWidth != null) {
	spec[i++] = {strokeWidth: strokeWidth};
    }

    if (gridColor != null) {
	spec[i++] = {"color.css": gridColor};
    }

    var gridf = new PolarGridFactory(a2d);
    gridf.configure(spec);
    gridf.createObject();
}())
