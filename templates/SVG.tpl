<?xml version="1.0"  encoding="UTF-8"?>
<svg width="$(width)pt" height="$(height)pt" viewBox="0 0 $(width) $(height)" xmlns="http://www.w3.org/2000/svg">
$(paths:endPaths)  <path id="$(varname)" d="$(segments:endSegments)$(hasMoveTo:endMoveTo) M $(x0),$(y0)$(endMoveTo)$(hasClose:endClose) Z$(endClose)$(hasLineTo:endLineTo) L $(x0),$(y0)$(endLineTo)$(hasQuadTo:endQuadTo) Q $(x0),$(y0) $(x1),$(y1)$(endQuadTo)$(hasCubicTo:endCubicTo) C $(x0),$(y0) $(x1),$(y1) $(x2),$(y2)$(endCubicTo)$(endSegments)"
  	fill-rule="$(fillRule)"
	style="stroke:$(stroke)$(+strokeOpacity:endSO);stroke-opacity:$(strokeOpacity)$(endSO);fill:$(fill)$(+fillOpacity:endFO);fill-opacity:$(fillOpacity)$(endFO)$(hasStrokeCap:endStrokeCap);stroke-linecap:$(strokeCap)$(endStrokeCap)$(hasDashPhase:endDashPhase);stroke-dashoffset:$(dashPhase)$(endDashPhase)$(hasDashArray:endDashArray);stroke-dasharray:$(dashArray)$(endDashArray)$(hasStrokeJoin:endStrokeJoin);stroke-linejoin:$(strokeJoin)$(endStrokeJoin)$(hasMiterLimit:endMiterLimit);stroke-miterlimit:$(miterLimit)$(endMiterLimit)$(hasStrokeWidth:endStrokeWidth);stroke-width:$(strokeWidth)$(endStrokeWidth)" />
$(endPaths)$(locations:endLocations)$(location:endLocation) <circle id="$(varname)" cx="$(x)" cy="$(y)" r="$(r)" stroke="white" stroke-width="2" fill="black" />
  <line x1="$(xL)" y1="$(y)" x2="$(xR)" y2="$(y)" style="stroke:white;stroke-width:1" />
  <line x1="$(x)" y1="$(yT)" x2="$(x)" y2="$(yB)" style="stroke:white;stroke-width:$(lw)" />
$(endLocation)$(endLocations)
</svg>
