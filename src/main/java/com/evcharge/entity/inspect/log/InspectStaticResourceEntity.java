package com.evcharge.entity.inspect.log;

import com.evcharge.libsdk.aliyun.AliYunOSS;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 巡检静态资源表;
 *
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("inspect")
public class InspectStaticResourceEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 关联的巡检日志明细ID
     */
    public long detail_id;
    /**
     * 资源类型
     */
    public String resource_type;
    /**
     * 文件路径
     */
    public String file_path;
    /**
     * 文件名
     */
    public String file_name;
    /**
     * 文件大小
     */
    public long file_size;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static InspectStaticResourceEntity getInstance() {
        return new InspectStaticResourceEntity();
    }

    public List<Map<String, Object>> getResourceUrl(long detailId) {
        return getResourceUrl(detailId, "");
    }

    /**
     * 获取日志目录的图片资源
     *
     * @param detailId 日志id
     * @return List<Map < String, Object>>
     */
    public List<Map<String, Object>> getResourceUrl(long detailId, String type) {

        List<InspectStaticResourceEntity> list = getInspectStaticResourceEntities(detailId, type);
        List<Map<String, Object>> newList = new ArrayList<>();
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        for (InspectStaticResourceEntity inspectStaticResourceEntity : list) {
            String showUrl = AliYunOSS.getImageUrl(inspectStaticResourceEntity.file_path);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", inspectStaticResourceEntity.id);
            data.put("resource_type", inspectStaticResourceEntity.resource_type);
            data.put("show_url", showUrl);
            data.put("url", showUrl);
            data.put("file_path", inspectStaticResourceEntity.file_path);
            newList.add(data);
        }

        return newList;
    }

    private static List<InspectStaticResourceEntity> getInspectStaticResourceEntities(long detailId, String type) {
        InspectStaticResourceEntity inspectStaticResource= new InspectStaticResourceEntity();
        inspectStaticResource
                .where("detail_id", detailId);

        if (StringUtils.hasLength(type)) {
            inspectStaticResource.where("resource_type", type);
//                    .cache(String.format("Inspect:Resource:%s:%s", detailId, type));
        }
//        else {
//            inspectStaticResource
//                    .cache(String.format("Inspect:Resource:%s", detailId));
//        }
        List<InspectStaticResourceEntity> list = inspectStaticResource.selectList();
        return list;
    }


}