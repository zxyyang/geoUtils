package vo;

import lombok.Data;
import org.locationtech.jts.geom.Coordinate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Zixuan.Yang
 * @date 2023/9/15 17:57
 */

public class GeoMakerVo {
    private Map<String,GeoVo> geoVoMap;
    private Map<String, Coordinate[]> polygonsMap = new HashMap<>();

    public Map<String, GeoVo> getGeoVoMap() {
        return geoVoMap;
    }

    public void setGeoVoMap(Map<String, GeoVo> geoVoMap) {
        this.geoVoMap = geoVoMap;
    }

    public Map<String, Coordinate[]> getPolygonsMap() {
        return polygonsMap;
    }

    public void setPolygonsMap(Map<String, Coordinate[]> polygonsMap) {
        this.polygonsMap = polygonsMap;
    }
}
