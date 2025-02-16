package org.example;

import junit.framework.TestCase;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TestProjection extends TestCase {

    public void testProjection() throws IOException, FactoryException {
        URL path = getClass().getResource("/shapefile/cities.shp");
        File file = new File(path.getFile());
        Map<String, Object> maps = new HashMap<>();
        maps.put("url", file.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(maps);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource =
                dataStore.getFeatureSource(typeName);
        SimpleFeatureType schema = featureSource.getSchema();
        CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
        MathTransform transform = CRS.findMathTransform(dataCRS, targetCRS);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource.getFeatures();

        // 写入新的shpfile
        File newFile = new File("example_reprojection.shp");
        DataStore dataStore1 = new ShapefileDataStore(newFile.toURI().toURL());
        SimpleFeatureType featureType = SimpleFeatureTypeBuilder.retype(schema, targetCRS);
        dataStore1.createSchema(featureType);
        String createdName = dataStore.getTypeNames()[0];

        Transaction transaction = new DefaultTransaction("Reproject");
        try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                     dataStore.getFeatureWriterAppend(createdName, transaction);
             SimpleFeatureIterator iterator = (SimpleFeatureIterator) featureCollection.features()) {
            while (iterator.hasNext()) {
                // copy the contents of each feature and transform the geometry
                SimpleFeature feature = iterator.next();
                SimpleFeature copy = writer.next();
                copy.setAttributes(feature.getAttributes());

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Geometry geometry2 = JTS.transform(geometry, transform);

                copy.setDefaultGeometry(geometry2);
                writer.write();
            }
            transaction.commit();
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
        } finally {
            transaction.close();
        }

    }
}
