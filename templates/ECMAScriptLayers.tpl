$(items:endItems)$(pathStatement:endPathStatement)var $(varname) = [
    {type: "PATH_START"$(hasWindingRule:endWR), windingRule: "$(windingRule)"$(endWR)},
$(pathItem:endPathItem)    {type: "$(ltype)"$(xy:endXY), x: $(x), y: $(y)$(endXY)},
$(endPathItem)    {type: "PATH_END", draw: "$(draw)", fill: "$(fill)"$(hasGcsMode:endGcsMode),
          "stroke.gcsMode": $(gcsMode)$(endGcsMode)$(hasDrawColor:endDrawColor),
	  "drawColor.css": "$(drawColor)"$(endDrawColor)$(hasFillColor:endFillColor),
	  "fillColor.css": "$(fillColor)"$(endFillColor)$(hasStrokeCap:endStrokeCap),
	  "stroke.cap": "$(strokeCap)"$(endStrokeCap)$(hasDashIncrement:endDashIncrement),
	  "stroke.dashIncrement": $(dashIncrement)$(endDashIncrement)$(hasDashPhase:endDashPhase),
	  "stroke.dashPhase": $(dashPhase)$(endDashPhase)$(hasDashPattern:endDashPattern),
	  "stroke.dashPattern": "$(dashPattern)"$(endDashPattern)$(hasStrokeJoin:endStrokeJoin),
	  "stroke.join": "$(strokeJoin)"$(endStrokeJoin)$(hasMiterLimit:endMiterLimit),
	  "stroke.miterLimit": $(miterLimit)$(endMiterLimit)$(hasStrokeWidth:endStrokeWidth),
	  "stroke.width": $(strokeWidth)$(endStrokeWidth)}
    ];
$(endPathStatement)$(endItems)