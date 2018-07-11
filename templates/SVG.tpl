<?xml version="1.0"  encoding="UTF-8"?>
<svg viewBox="0 0 $(width) $(height)" xmlns="http://www.w3.org/2000/svg">
$(paths:endPaths)  <path d="$(segments:endSegments)$(hasMoveTo:endMoveTo) M $(x0),$(y0)$(endMoveTo)$(hasClose:endClose) Z$(endClose)$(hasLineTo:endLineTo) L $(x0),$(y0)$(endLineTo)$(hasQuadTo:endQuadTo) Q $(x0),$(y0) $(x1),$(y1)$(endQuadTo)$(hasCubicTo:endCubicTo) C $(x0),$(y0) $(x1),$(y1) $(x2),$(y2)$(endCubicTo)$(endSegments)"
  	fill-rule="$(fillRule)"
	style="stroke:$(stroke);fill:$(fill)$(hasStrokeCap:endStrokeCap);stroke-linecap:$(strokeCap)$(endStrokeCap)$(hasDashPhase:endDashPhase);stroke-dashoffset:$(dashPhase)$(endDashPhase)$(hasDashArray:endDashArray);stroke-dasharray:$(dashArray)$(endDashArray)$(hasStrokeJoin:endStrokeJoin);stroke-linejoin:$(strokeJoin)$(endStrokeJoin)$(hasMiterLimit:endMiterLimit);stroke-miterlimit:$(miterLimit)$(endMiterLimit)$(hasStrokeWidth:endStrokeWidth);stroke-width:$(strokeWidth)$(endStrokeWidth)" />
$(endPaths)
</svg>
