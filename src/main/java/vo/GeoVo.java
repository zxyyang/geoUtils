package vo;

import lombok.Data;
import org.locationtech.jts.geom.Coordinate;

import java.util.Map;

/**
 * @author Zixuan.Yang
 * @date 2023/9/15 17:49
 */

public class GeoVo {
        private  String id;
        private  String name;
        private  Integer level;
        private GeoVo child;

        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public Integer getLevel() {
                return level;
        }

        public void setLevel(Integer level) {
                this.level = level;
        }

        public GeoVo getChild() {
                return child;
        }

        public void setChild(GeoVo child) {
                this.child = child;
        }
}
