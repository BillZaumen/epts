// Script to create a background consisting of a grid
// used to located points in graph coordinate space.
// The following variables may be set:
//
//    * frameWidth - an integer giving the width of the background image
//                   that will be created. The default is 1920. The value
//                   is ignored if the animation was created by another
//                   script. If undefined or null, the default will be used.
//    * frameHeight - an integer giving the height of the background image
//                    that will be created. The default is 1080. The value
//                   is ignored if the animation was created by another
//                   script. If undefined or null, the default will be used.
//    * userdist - a double giving a reference distance in user space. The
//                 default is 1.0. The ratio userdist/gcsdist is the
//                 scaling factor for converting distances in graph coordinate
//                 space to user space. The value is ignored if the
//                 animation was created by another script. If undefined or
//                 null, the default will be used.
//    * gcsdist -  a double giving a reference distance in graph coordinate
//                 space. The default is 1.0. The ratio userdist/gcsdist is the
//                 scaling factor for converting distances in graph coordinate
//                 space to user space. The value is ignored if the
//                 animation was created by another script. If undefined or
//                 null, the default will be used.
//    * xorigin - a double giving the X coordinate in graph coordinate space
//                for the frame's reference point. The default is 0.0.
//                The value is ignored if the animation was created by
//                another script. If undefined or
//                 null, the default will be used.
//    * yorigin - a double giving the Y coordinate in graph coordinate space
//                for the frame's reference point. The  default is 0.0.
//                The value is ignored if the animation was created by
//                another script. If undefined or
//                 null, the default will be used.
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
//    * spacing - a double giving the grid spacing in graph-coordinate
//                space units. If not provided explicitly or if the
//                value is null, 0.0, or negative, the default is computed by
//                taking the minimum of the frame width and frame height,
//                converting that minimum to graph coordinate space units,
//                then dividing by 10, and finally finding the largest
//                power of 10 that is not larger than this value. The
//                grid lines will appear at X or Y coordinates that are
//                an integral multiple of the spacing.
//    * subspacing - the number of subspacings per spacing for a finer grid.
//                   if undefined or 1, the value is ignored. In practice,
//                   values that create a subgrid are either 2, 4, 5, or 10.
//                   The value must be an integer. If undefined or null,
//                   a default value is used.
//    * axisColor - the axis color provided as a CSS string,
//                  null or undefined for the default.
//    * spacingColor - the spacing color provided as a CSS string,
//                     null or undefined for the default.
//    * subspacingColor - the subspacing color provided as a CSS string,
//                        null or undefined for the default.
//    * strokeWidth - a double giving the width of the stroke used to create
//                    grid lines; undefined or null for a default.
//    * gridZorder - an integer giving the z-order to use for the object
//                   that creates the grid. The default is the javascript
//                   value java.lang.Long.MAX_VALUE (2^53 - 1).
//    * plainBackgroundColor - a color specification for the background
//                   color to use if no image was provided, provided as
//                   a CSS string.
//
// The variable a2d is expected to be the variable name for an animation.
// It will be created if it does not already exist, but an existing one
// can be used as well so that a grid can be overlayed.
// Generally, if provided, the axisColor should be more prominent than
// the spacing color, which in turn should be more prominent than the
// subspacing color.

scripting.importClass("org.bzdev.anim2d.Animation2D");
scripting.importClass("org.bzdev.anim2d.AnimationLayer2DFactory");
scripting.importClass("org.bzdev.anim2d.CartesianGrid2DFactory");

if (typeof subspacing  === 'undefined') {
    var subspacing = 1;
} else if (subspacing != Math.round(subspacing) || subspacing < 0) {
    throw "subspacing = " + subspacing;
}
if (subspacing == 0) {
    subspacing = 1;
}

if (typeof frameWidth === 'undefined'|| frameWidth == null) {
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
    if (typeof epts == 'undefined' || epts == null) {
	var gcsdist = 1.0;
    } else {
	var gcsdist = epts.getGCSDist();
    }
}

if (typeof xorigin === 'undefined' || xorigin == null) {
    if (typeof epts == 'undefined' || epts == null) {
	var xorigin = 0.0;
    } else {
	var xorigin = epts.getXOrigin();
    }
}

if (typeof yorigin === 'undefined' || yorigin == null) {
    if (typeof epts == 'undefined' || epts == null) {
	var yorigin = 0.0;
    } else {
	var yorigin = epts.getYOrigin();
    }
}

if (typeof xfract === 'undefined'|| xfract == null) {
    if (typeof epts == 'undefined' || epts == null) {
	var xfract = 0.0;
    } else {
	var xfract = epts.getXFract();
    }
}

if (typeof yfract === 'undefined' || yfract == null) {
    if (typeof epts == 'undefined' || epts == null || !epts.hasDistances()) {
	var yfract = 0.0;
    } else {
	var yfract = epts.getYFract();
    }
}

if (typeof spacing === 'undefined') {
    var spacing = null;
}

if (typeof gridZorder === 'undefined' || gridZOrder == null) {
    var gridZorder = java.lang.Long.MAX_VALUE;
}

if (typeof axisColor === 'undefined') {
    var axisColor = null;
}

if (typeof spacingColor === 'undefined') {
    var spacingColor = null;
}

if (typeof subspacingColor === 'undefined') {
    var subspacingColor = null;
}

if (typeof strokeWidth === 'undefined') {
    var strokeWidth = null;
}

if (typeof plainBackgroundColor == 'undefined'
   || plainBackgroundColor == null) {
    var plainBackgroundColor = "white";
}


// Use a function because the 'let' keyword is not recognized
// by Nashorn and we want any variable not defined above  (and
// hence not documented) to be a local variable.  The keyword
// 'var' makes variables inside functions local.

var a2d;

(function() {
    var JMath = java.lang.Math;
    var plainBackground = false;
    if (typeof a2d === 'undefined' || a2d == null) {
	a2d = new Animation2D(scripting, frameWidth, frameHeight, 1000.0, 40);
	if (epts === 'undefined' || epts == null) {
	    plainBackground = true;
	}
	var scalef = userdist / gcsdist;
	a2d.setRanges(xorigin, yorigin, xfract, yfract, scalef, scalef);
    } else {
	var scalef = (a2d.getWidth() - a2d.getXLowerOffset()
		      - a2d.getXUpperOffset())
	    / (a2d.getXUpper() - a2d.getXLower());
    }



    if (spacing == null) {
	// By default, arrange so that there are roughly 10 lines in the
	// shortest direction, with the constraint that the graph coordinate
	// space values are powers of 10 in graph coordinate space
	spacing = (frameWidth > frameHeight)? frameHeight: frameWidth;
	spacing /= scalef;
	spacing = Math.floor(JMath.log10(spacing/10));
	if (spacing > -0.1) {
	    if (spacing < 0.0) spacing = 0.0;
	    spacing = JMath.round(JMath.pow(10.0, spacing));
	} else {
	    spacing = 1.0 / JMath.round(JMath.pow(10.0, spacing));
	}
    }
    var spec;
    if (plainBackground) {
	var xlower = a2d.getXLower();
	var xupper = a2d.getXUpper();
	var ylower = a2d.getYLower();
	var yupper = a2d.getYUpper();
	var alf = new AnimationLayer2DFactory(a2d);
	spec = [
	    {zorder: gridZorder, visible: true},
	    {withPrefix: "object",
	     withIndex: [{type: "RECTANGLE", x: xlower, y: ylower,
			  refPoint: "LOWER_LEFT",
			  width: (xupper-xlower),
			  height: (yupper-ylower),
			  fill: true, draw: true,
			  "stroke.width": 1.0,
			  "fillColor.css": plainBackgroundColor,
			  "drawColor.css": plainBackgroundColor}]}];
	alf.configure(spec);
	alf.createObject();
    }

    var gridf = new CartesianGrid2DFactory(a2d);

    spec = [{zorder: gridZorder, visible: true},
	    {spacing: spacing, subspacing: subspacing}];

    var i = 2;
    if (axisColor != null) {
	spec[i++] = {"axisColor.css": axisColor};
    }
    if (spacingColor != null) {
	spec[i++] = {"spacingColor.css": spacingColor};
    }
    if (subspacingColor != null) {
	spec[i++] = {"subspacingColor.css": subspacingColor};
    }
    if (strokeWidth != null) {
	spec[i++] = {strokeWidth: strokeWidth};
    }

    gridf.configure(spec);
    gridf.createObject();
})();
