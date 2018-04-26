<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE layout PUBLIC "-//BZDev//EPTS 1.0//EN"
	  "sresource:epts-1.0.dtd">
<epts xmlns="http://bzdev.org/DTD/epts-1.0">
  <image width="$(width)" height="$(height)" imageNameExists="$(hasImageFile)"/>
  <targetList>$(arglist:arglistEnd)
     <argument>$(arg)</argument>$(arglistEnd)
  </targetList>
  <gcsconfig unitIndex="$(unitIndex)" refPointIndex="$(refPointIndex)"
	  userSpaceDistance="$(userSpaceDistance)"
	  gcsDistance="$(gcsDistance)"/>
  $(table:endTable)<table>$(items:endItems)$(location:endLocation)
     <row varname="$(varname)" type="LOCATION"
       x="$(x)$ y="$(y)" xp="$(xp)" yp="$(yp)" />
  $(endLocation)$(pathStatement:endPathStatement)
      <row varname="$(varname)" type="PATH_START"\>$(pathItem:endPathItem)
      <row type="$(type)"
           $(xy:endXY) x="$(x)" y="$(y)" xp="$(xp)" yp="$(yp)"$(endXY)/>$(endPathItem)
      <row type="PATH_END"\>
  $(endPathStatement)$(endItems)</table>$(endTable)
</epts>
