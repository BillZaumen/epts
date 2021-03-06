$(items:endItems)$(location:endLocation)var $(varname) = {x: $(x), y: $(y)};
$(endLocation)$(pathStatement:endPathStatement)var $(varname) = [{visible: "$(draw)"$(hasAttributes:endAttributes)$(hasWindingRule:endWR),
     windingRule: "$(windingRule)"$(endWR)$(hasGcsMode:endGcsMode),
     "stroke.gcsMode": $(gcsMode)$(endGcsMode)$(hasDrawColor:endDrawColor),
     "color.css": "$(drawColor)"$(endDrawColor)$(hasStrokeCap:endStrokeCap),
     "stroke.cap": "$(strokeCap)"$(endStrokeCap)$(hasDashIncrement:endDashIncrement),
     "stroke.dashIncrement": $(dashIncrement)$(endDashIncrement)$(hasDashPhase:endDashPhase),
     "stroke.dashPhase": $(dashPhase)$(endDashPhase)$(hasDashPattern:endDashPattern),
     "stroke.dashPattern": "$(dashPattern)"$(endDashPattern)$(hasStrokeJoin:endStrokeJoin),
     "stroke.join": "$(strokeJoin)"$(endStrokeJoin)$(hasMiterLimit:endMiterLimit),
     "stroke.miterLimit": $(miterLimit)$(endMiterLimit)$(hasStrokeWidth:endStrokeWidth),
     "stroke.width": $(strokeWidth)$(endStrokeWidth)$(hasZorder:endZorder),
     zorder: $(zorder)$(endZorder)$(endAttributes)},
  {withPrefix: "cpoint", withIndex: [$(pathItem:endPathItem)
    {type: "$(type)"$(xy:endXY), x: $(x), y: $(y)$(endXY)}$(optcomma)$(endPathItem)
    ]}];$(endPathStatement)
$(endItems)