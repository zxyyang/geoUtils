
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;

import java.io.File;
import java.util.*;


import org.locationtech.jts.algorithm.PointLocator;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.StringUtil;
import vo.GeoMakerVo;
import vo.GeoVo;

import java.io.File;
import java.io.IOException;


/**
 * @author Zixuan.Yang
 * @date 2023/9/11 14:39
 * https://map.vanbyte.com/
 */
@Slf4j
public class GeoJSONParser {
    // 创建Map用于存储Coordinate多边形对象
    private static final String COUNTY_DIR = "country";
    private static final String CHINA_GEO = "china";

    private static final String PROVINCE_DIR = "province";

    private static final String CITY_DIR = "city";


    public static void main(String[] args) {
        int[] x = NumberUtil.generateRandomNumber(30000, 39999,1000);
        int[] y = NumberUtil.generateRandomNumber(52852, 59999,100);
        for (int i = 0; i < x.length; i++) {
         for (int j = 0; j < y.length; j++) {
             StopWatch stopWatch = new StopWatch();
             stopWatch.start("getGeo");
            double longitude = Double.valueOf(121+"."+i); // 经度
             double latitude = Double.valueOf(31+"."+j);  // 纬度
             GeoVo geoByXY = getGeoByXY(longitude, latitude);
             stopWatch.stop();
             System.err.println("耗时："+stopWatch.getLastTaskTimeMillis()+JSONObject.toJSON(geoByXY));
         }
        }
        //我的位置，我在广州

    }

    private static GeoVo getGeoByXY(double longitude,double latitude) {
        //省
        GeoMakerVo geo = getGEO(CHINA_GEO, COUNTY_DIR);
        String containingPolygon = findContainingPolygon(geo.getPolygonsMap(),longitude, latitude);
        GeoVo geoVo = geo.getGeoVoMap().get(containingPolygon);
        if (geoVo == null){
           // System.out.println("未找到省！");
            return null;
        }
        if (Objects.nonNull(geoVo.getId())) {
            GeoMakerVo geoP = getGEO(geoVo.getId(), PROVINCE_DIR);
            String containingPolygonP = findContainingPolygon(geoP.getPolygonsMap(), longitude, latitude);
            GeoVo geoPVo = geoP.getGeoVoMap().get(containingPolygonP);
            if (geoPVo == null){
                //System.out.println("未找到市！");
                return geoVo;
            }
            //区
            GeoMakerVo geoC = getGEO(geoPVo.getId(), CITY_DIR);
            if ((geoC == null) ){
              //  System.out.println("未找到区！");
                geoVo.setChild(geoPVo);
                return geoVo;
            }else {
                if (geoC.getPolygonsMap().size() == 0){
                    geoVo.setChild(geoPVo);
                    return geoVo;
                }
                String containingPolygonC = findContainingPolygon(geoC.getPolygonsMap(), longitude, latitude);
                GeoVo geoCVo = geoC.getGeoVoMap().get(containingPolygonC);
                //
                geoPVo.setChild(geoCVo);
                geoVo.setChild(geoPVo);
                return geoVo;
            }
        }
        return geoVo;

    }

    private static GeoMakerVo getGEO(String adCode,String leveType) {
        GeoMakerVo geoMakerVo = new GeoMakerVo();
        Map<String,GeoVo> geoVoMap = new HashMap<>();
         Map<String, Coordinate[]> polygonsMap = new HashMap<>();
        // 创建ObjectMapper对象
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 读取.geojson文件并解析为JsonNode对象

                File  file = new File("map/"+leveType+"/"+adCode+".json");

         //  File file = new File("map/"+leveType+"/"+adCode+".json");
            if(!file.exists()){
                return geoMakerVo;

            }
            JsonNode rootNode = objectMapper.readTree(file);

            // 获取FeatureCollection下的features数组
            JsonNode featuresNode = rootNode.get("features");

            // 遍历features数组
            for (JsonNode featureNode : featuresNode) {
                // 获取geometry字段
                JsonNode geometryNode = featureNode.get("geometry");

                // 获取类型字段
                String type = geometryNode.get("type").asText();

                // 获取坐标字段
                JsonNode coordinatesNode = geometryNode.get("coordinates");

                // 获取properties字段
                JsonNode propertiesNode = featureNode.get("properties");

                // 获取code字段
                String code = propertiesNode.get("id").asText();
                GeoVo geoVo = new GeoVo();
                geoVo.setId(code);
                geoVo.setName(propertiesNode.get("name").asText());
                geoVo.setLevel(propertiesNode.get("level").asInt());
                geoVoMap.put(code,geoVo);
                // 根据类型进行相应处理
                if (type.equals("Polygon")) {
                    // 处理Polygon类型
                    Coordinate[] coordinates = parseCoordinates(coordinatesNode);
                    polygonsMap.put(code, coordinates);
                } else if (type.equals("MultiPolygon")) {
                    // 处理MultiPolygon类型
                    for (JsonNode polygonNode : coordinatesNode) {
                        Coordinate[] coordinates = parseCoordinates(polygonNode);
                        polygonsMap.put(code, coordinates);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        geoMakerVo.setGeoVoMap(geoVoMap);
        geoMakerVo.setPolygonsMap(polygonsMap);
        return geoMakerVo;
    }

    public static Coordinate[] parseCoordinates(JsonNode coordinatesNode) {
        if (!coordinatesNode.isArray()) {
            return null; // 非法的 JSON 数组
        }

        List<Coordinate> coordinateList = new ArrayList<>();
        Iterator<JsonNode> iterator = coordinatesNode.elements();
        while (iterator.hasNext()) {
            JsonNode coordinateNode = iterator.next();
            if (coordinateNode.isArray() && coordinateNode.size() == 2) {
                double x = coordinateNode.get(0).asDouble();
                double y = coordinateNode.get(1).asDouble();
                Coordinate coordinate = new Coordinate(x, y);
                coordinateList.add(coordinate);
            }else if(coordinateNode.isArray() && coordinateNode.size() > 2){
                Iterator<JsonNode> elements = coordinateNode.elements();
                while (elements.hasNext()) {
                    JsonNode next = elements.next();
                    if (next.isArray() && next.size() == 2) {
                        double x = next.get(0).asDouble();
                        double y = next.get(1).asDouble();
                        Coordinate coordinate = new Coordinate(x, y);
                        coordinateList.add(coordinate);
                    }
                }
            } else {
                return null; // 非法的坐标格式
            }
        }

        return coordinateList.toArray(new Coordinate[0]);
    }

    public static String findContainingPolygon(Map<String, Coordinate[]> polygonsMap,double longitude, double latitude) {
        // 创建GeometryFactory
        GeometryFactory geometryFactory = new GeometryFactory();

        // 创建点坐标
        Coordinate pointCoordinate = new Coordinate(longitude, latitude);

        // 遍历多边形Map，查找包含点的多边形
        for (Map.Entry<String, Coordinate[]> entry : polygonsMap.entrySet()) {
            String code = entry.getKey();
            Coordinate[] coordinates = entry.getValue();
            //对比coordinates第一项和最后一项是否相同,如果不相同就在Coordinate最后加上第一项的数据
            if (!coordinates[0].equals(coordinates[coordinates.length - 1])) {
                coordinates = Arrays.copyOf(coordinates, coordinates.length + 1);
                coordinates[coordinates.length - 1] = coordinates[0];
            }
            // 创建MultiPolygon对象
            MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[] {
                    geometryFactory.createPolygon(coordinates)
            });

            // 使用PointLocator检查点是否在多边形内
            PointLocator pointLocator = new PointLocator();
            int location = pointLocator.locate(pointCoordinate, multiPolygon);

            if (location != Location.EXTERIOR) {
                return code;
            }
        }

        return null; // 未找到包含点的多边形
    }


}
