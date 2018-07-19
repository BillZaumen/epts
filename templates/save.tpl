<?xml version="1.1" encoding="UTF-8"?>
<!DOCTYPE epts PUBLIC "-//BZDev//EPTS-1.0//EN"
	  "sresource:epts-1.0.dtd">
<epts xmlns="http://bzdev.org/DTD/epts-1.0">
  <image width="$(width)" height="$(height)" imageURIExists="$(hasImageFile)"/>
$(hasCodebase:endCodebase)  <codebase>$(pathlist:endPathlist)
     <path>$(path)</path>
$(endPathlist)  </codebase>
$(endCodebase)$(hasScript:endScript)  <scripting language="$(language)" animation="$(animation)">$(hasBindings:endBindings)
      <binding name="$(bindingName)" type="$(bindingType)">$(bindingValue)</binding>$(endBindings)
  </scripting>
$(endScript)  <targetList>$(arglist:arglistEnd)
     <argument>$(arg)</argument>$(arglistEnd)
  </targetList>
  <gcsconfig unitIndex="$(unitIndex)" refPointIndex="$(refPointIndex)"
	  userSpaceDistance="$(userSpaceDistance)"
	  gcsDistance="$(gcsDistance)"
	  xrefpointGCS="$(xrefpoint)" yrefpointGCS="$(yrefpoint)"/>
  $(table:endTable)<table>
$(items:endItems)$(location:endLocation)       <row varname="$(varname)" type="LOCATION"
       x="$(x)" y="$(y)" xp="$(xp)" yp="$(yp)" />
$(endLocation)$(pathStatement:endPathStatement)       <row varname="$(varname)" type="PATH_START"/>
$(pathItem:endPathItem)       <row type="$(type)"$(xy:endXY) x="$(x)" y="$(y)" xp="$(xp)" yp="$(yp)"$(endXY)/>
$(endPathItem)       <row type="PATH_END"/>
$(endPathStatement)$(endItems)  </table>$(endTable)
</epts>
