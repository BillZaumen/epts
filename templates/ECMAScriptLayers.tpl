$(items:endItems)$(pathStatement:endPathStatement)var $(varname) = [
    {type: "PATH_START"$(hasWindingRule:endWR), windingRule: "$(windingRule)"$(endWR)},
$(pathItem:endPathItem)    {type: "$(ltype)"$(xy:endXY), x: $(x), y: $(y)$(endXY)},
$(endPathItem)    {type: "PATH_END"}
    ];
$(endPathStatement)$(endItems)