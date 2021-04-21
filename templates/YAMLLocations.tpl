%YAML 1.2
---
 - execute:
$(items:endItems)$(location:endLocation)
    - !bzdev!esp >-
      var $(varname) = {x: $(x), y: $(y)}
$(endLocation)$(endItems)...
