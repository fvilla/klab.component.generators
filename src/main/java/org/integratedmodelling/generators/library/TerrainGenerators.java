package org.integratedmodelling.generators.library;

import org.integratedmodelling.generators.terrain.Terrain;
import org.integratedmodelling.klab.api.data.Version;
import org.integratedmodelling.klab.api.data.mediation.NumericRange;
import org.integratedmodelling.klab.api.geometry.DimensionScanner2D;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.geometry.Geometry.Dimension;
import org.integratedmodelling.klab.api.knowledge.Artifact.Type;
import org.integratedmodelling.klab.api.knowledge.observation.State;
import org.integratedmodelling.klab.api.knowledge.observation.scale.Scale;
import org.integratedmodelling.klab.api.lang.ServiceCall;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.services.runtime.extension.KlabFunction;
import org.integratedmodelling.klab.api.services.runtime.extension.Library;
import org.integratedmodelling.klab.api.services.runtime.extension.Resolver;
import org.integratedmodelling.klab.runtime.storage.DoubleStorage;

@Library(name = "klab.generators.geospatial", description = """
        Contextualizers that generate realistic-looking geographic terrains and features for stress-testing. Use only on small S2 geometries.""")
public class TerrainGenerators {

    @KlabFunction(name = "terrain", description = """
            Generate fractal surfaces within a user-defined range \
            and with a configurable degree of smoothness, apt to simulating several terrain patterns such as \
            elevation or slope. As the generator works in RAM, this should not be used on very large grids.""",
                  geometry = "S2", type = Type.NUMBER, version = Version.CURRENT,
                  parameters = {
                          @KlabFunction.Argument(name = "range", type = Type.RANGE, description = "The  " +
                                  "min-max range of the values produced. Default is 0 to 4000", optional =
                                  true),
                          @KlabFunction.Argument(name = "detail", type = Type.NUMBER, description =
                                  "Controls the amount of detail in the generated structure. Default is 8, " +
                                          "appropriate "
                                  + "for geographical elevation", optional = true),
                          @KlabFunction.Argument(name = "roughness", type = Type.NUMBER, description =
                                  "Controls the roughness of the generated terrain. Default is 0.55, " +
                                          "appropriate" +
                                  " for geographical elevation", optional = true)})
    public void generateTerrain(DoubleStorage storage, Scale scale, ServiceCall call) {

        var range = call.getParameters().get("range", NumericRange.create(0., 4000., false, false));
        var xy = scale.getSpace().getShape();
        var terrain = new Terrain(call.getParameters().get("detail", 8), call.getParameters().get(
                "roughness", 0.55), range.getLowerBound(), range.getUpperBound());

        /**
         appropriate pattern for generic scale when we handle only one dimension, even if in most
         situations (all
         at the moment) only one subscale will be returned. If there is no time or other dimension, a
         unit scale
         will be returned and at(unit) will later return self. Inside the loop, adapt the overall geometry
         located by each sub-scale to a grid scanner and use a buffer for fast access to storage. The
         geometry
         requirement ensures that we get a regular 2D spatial extent, so this is safe w/o error checking.
         */
        for (Geometry subscale : scale.without(Dimension.Type.SPACE)) {

            double dx = 1.0 / (double) xy.get(0);
            double dy = 1.0 / (double) xy.get(1);
            var buffer = storage.getSliceBuffer(subscale);
            /**
             * The overall geometry is first located to the current non-spatial location, then the
             * space is iterated through a fast 2D offset and storage buffer.
             */
            for (var offset :
                    scale.at(subscale).as(DimensionScanner2D.class)) {
                buffer.add(terrain.getAltitude(dx * offset.x(), dy * offset.y()), offset.position());
            }
        }
    }

}
