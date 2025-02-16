package org.example;

import junit.framework.TestCase;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestShapefile extends TestCase {

    public TestShapefile(String testName) {
        super(testName);
    }

    //
    public void testReadShapef() throws IOException {
        URL path = getClass().getResource("/shapefile/cities.shp");
        File file = new File(path.getFile());
        Map<String, Object> maps = new HashMap<>();
        maps.put("url", file.toURI().toURL());

//        DataStore dataStore = DataStoreFinder.getDataStore(maps);
        ShapefileDataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature simpleFeature = features.next();
                Object geomtry = simpleFeature.getDefaultGeometry();
                System.out.println("geomtry:" + geomtry);
                System.out.println("attributes" + simpleFeature.getAttributes());
                System.out.println("properties:" + simpleFeature.getProperties());
            }
        }
    }

    public void testCreateShpfile() {
        try {
            // 1. 定义要素类型
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("Location");
            builder.setCRS(org.geotools.referencing.crs.DefaultGeographicCRS.WGS84); // 设置坐标系

            // 添加属性
            builder.add("the_geom", Point.class);
            builder.add("name", String.class);
            builder.add("number", Integer.class);
            SimpleFeatureType TYPE = builder.buildFeatureType();

            // 2. 准备要素数据
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            // 创建点要素
            Point point = geometryFactory.createPoint(new Coordinate(105.0, 24.0));
            // 添加属性，注意添加顺序要与eatureType中定义的匹配，否则会通不过校验
            featureBuilder.add(point);
            featureBuilder.add("name");
            featureBuilder.add(1);
            SimpleFeature feature = featureBuilder.buildFeature("1");
            List<SimpleFeature> features = new ArrayList<>();
            features.add(feature);
            // 3. 创建 Shapefile
            File file = new File("example.shp");
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

            Map<String, Serializable> params = new HashMap<>();
            params.put("url", file.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);

            // 4. 写入数据
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(DataUtilities.collection(features));
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            }

            System.out.println("Shapefile 创建成功！");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addPoint(List<SimpleFeature> features,
                                 SimpleFeatureBuilder featureBuilder,
                                 GeometryFactory geometryFactory,
                                 double longitude,
                                 double latitude,
                                 String name,
                                 int number) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        featureBuilder.add(point);
        featureBuilder.set("the_geom", point);
        featureBuilder.add(name);
        featureBuilder.add(number);
        SimpleFeature feature = featureBuilder.buildFeature(null);
        features.add(feature);
    }
}
