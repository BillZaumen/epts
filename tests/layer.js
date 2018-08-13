scripting.importClass("org.bzdev.anim2d.Animation2D");
scripting.importClass("org.bzdev.swing.AnimatedPanelGraphics");
scripting.importClass("org.bzdev.util.units.MKS");

if (typeof frameWidth === 'undefined') {
    var frameWidth = 1920/2;
}
if (typeof frameHeight === 'undefined') {
    frameHeight = 1080/2;
}

a2d = new Animation2D(scripting, frameWidth, frameHeight, 1000.0, 40);

if (epts === 'undefined' && typeof da === 'undefined') {
    var apg = AnimatedPanelGraphics.newFramedInstance(a2d,
						      "Swerving",
						      true, true, null);
}
width = MKS.feet(46.0);
scalef = frameHeight/width;
length = frameWidth/scalef;
a2d.setRanges(0.0, 0.0, 0.0, 0.5, scalef, scalef);

a2d.createFactories("org.bzdev.anim2d", {
    alf: "AnimationLayer2DFactory",
    gvf: "GraphViewFactory",
    pathf: "AnimationPath2DFactory"
});

lightgray = {red: 200, blue: 200, green: 200, alpha: 255};
gray = {red: 64, blue: 64, green: 64, alpha: 255};
red = {red: 255, blue: 0, green: 0}
yellow = {blue: 0, red: 255, green: 255}

stroke = {width: 2.0, gcsMode: false};

alf.createObject("background", [
    {zorder: 1, visible: true},
    {withPrefix: "object",
     withIndex: [
	{type: "RECTANGLE",
	 config: [
	     {x: -10, y: -width/2.0, refPoint: "LOWER_LEFT",
	      width: 20+length, height: width},
	     {withPrefix: "stroke", config: stroke},
	     {withPrefix: "fillColor", config: lightgray},
	     {withPrefix: "drawColor", config: lightgray},
	     {fill: true},
	     {draw: true}
	 ]},
	{type: "RECTANGLE",
	 config: [
	     {x: -10, y: -MKS.feet(18), refPoint: "LOWER_LEFT",
	      width: 20+length, height: MKS.feet(36)},
	     {withPrefix: "stroke", config: stroke},
	     {withPrefix: "fillColor", config: gray},
	     {withPrefix: "drawColor", config: gray},
	     {fill: true},
	     {draw: true}
	 ]}
     ]
    }
]);
