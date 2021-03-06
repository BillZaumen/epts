// This file was automatically generated by the epts program, and 
// should not be edited.
$(hasPackage:endPackage)package $(package);
$(endPackage)import org.bzdev.anim2d.AbstrAnimPath2DFactory;
import org.bzdev.anim2d.AnimationPath2D;
import org.bzdev.anim2d.AnimationLayer2D.Type;
import org.bzdev.geom.SplinePathBuilder;
import org.bzdev.geom.SplinePathBuilder.WindingRule;
import org.bzdev.obnaming.NamedObjectFactory;
import org.bzdev.obnaming.NamedObjectFactory.IndexedSetter;
$(public)$(optSpace)final class $(class) {
$(items:endItems)$(pathStatement:endPathStatement)    public static IndexedSetter $(varname) = new IndexedSetter() {
        public int setIndexed(AbstrAnimPath2DFactory f, int start) {
$(hasWindingRule:endWR)            f.set("windingRule", WindingRule.$(windingRule));
$(endWR)$(pathItem:endPathItem)            f.set("cpoint.type", start, Type.$(type));
$(xy:endXY)            f.set("cpoint.x", start, $(x));
            f.set("cpoint.y", start, $(y));
$(endXY)            start++;
$(endPathItem)	    f.set("visible", "$(draw)");
	    f.set("stroke.gcsMode", $(gcsMode));$(endGcsMode)$(hasDrawColor:endDrawColor)
	    f.set("color.css", "$(drawColor)");$(endDrawColor)$(hasStrokeCap:endStrokeCap)
	    f.set("stroke.cap", "$(strokeCap)");$(endStrokeCap)$(hasDashIncrement:endDashIncrement)
	    f.set("stroke.dashIncrement", $(dashIncrement));$(endDashIncrement)$(hasDashPhase:endDashPhase)
	    f.set("stroke.dashPhase", $(dashPhase));$(endDashPhase)$(hasDashPattern:endDashPattern)
	    f.set("stroke.dashPattern", "$(dashPattern)");$(endDashPattern)$(hasStrokeJoin:endStrokeJoin)
	    f.set("stroke.join", "$(strokeJoin)");$(endStrokeJoin)$(hasMiterLimit:endMiterLimit)
	    f.set("stroke.miterLimit", $(miterLimit));$(endMiterLimit)$(hasStrokeWidth:endStrokeWidth)
	    f.set("stroke.width", $(strokeWidth));$(endStrokeWidth)$(hasZorder:endZorder)
	    f.set("zorder", $(zorder));$(endZorder)
            return start;
        }
    }
$(endPathStatement)$(endItems)
}
