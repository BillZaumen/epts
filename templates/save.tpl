<?xml version="1.1" encoding="UTF-8"?>
<!DOCTYPE epts PUBLIC "-//BZDev//EPTS-1.0//EN"
	  "resource:org/bzdev/epts/epts-1.0.dtd">
<epts xmlns="http://bzdev.org/DTD/epts-1.0">
  <image width="$(width)" height="$(height)">$(hasImageURI:endImageURI)
    <imageURI>$(imageURI)</imageURI>
$(endImageURI)  </image>
$(hasCodebase:endCodebase)  <codebase>
$(pathlist:endPathlist)     <path>$(path)</path>
$(endPathlist)  </codebase>
$(endCodebase)$(hasClasspath:endClasspath)  <classpath>
$(classpathlist:endcpl)     <path>$(path)</path>
$(endclp)  </classpath>
$(endClasspath)$(hasAddedModules:endhsa)  <modules>
$(moduleslist:endml)     <module>$(module)</module>
$(endml)  </modules>
$(endhsa)$(hasScript:endScript)  <scripting language="$(language)" animation="$(animation)">
$(hasBindings:endBindings)      <binding name="$(bindingName)" type="$(bindingType)">$(bindingValue)</binding>
$(endBindings)  </scripting>
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
$(endPathStatement)$(endItems)  </table>$(endTable)$(hasFilterItems:endHasFilterItems)
  <filters>$(filterItems:endFilterItems)
      <filter name="$(filterName)" mode="$(filterMode)">$(filterRows:endFilterRows)
         <filterRow varname="$(filterVarname)" mode="$(filterRowMode)"/>$(endFilterRows)
      </filter>$(endFilterItems)
  </filters>$(endHasFilterItems)$(hasOffsets:endHasOffsets)
  <offsets>$(hasBasemap:endHasBasemap)
    <basemap>$(basemapEntries:endBasemapEntries)
        <basemapEntry base="$(base)" mindex="$(mindex)"
	    dist1="$(dist1)" dist2="$(dist2)" dist3="$(dist3)"
	    uindex1="$(uindex1)" uindex2="$(uindex2)" uindex3="$(uindex3)" />
$(endBasemapEntries)    </basemap>$(endHasBasemap)$(hasPathmap:endHasPathmap)
    <pathmap>$(pathmapEntries:endPathmapEntries)
        <pathmapEntry path="$(pathForEntry)" base="$(base)" />
$(endPathmapEntries)    </pathmap>$(endHasPathmap)
  </offsets>$(endHasOffsets)
</epts>
