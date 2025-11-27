package com.evcharge.service.GeneralDevice;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceConfigEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.utils.JSONFormatConfig;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一般设备配置-业务逻辑层;
 *
 * @author : JED
 * @date : 2024-11-13
 */
@Service
public class GeneralDeviceConfigService {
    /**
     * 获得一个实例
     *
     * @return
     */
    public static GeneralDeviceConfigService getInstance() {
        return new GeneralDeviceConfigService();
    }

    /**
     * 获取设备配置
     *
     * @param serialNumber 序列号
     * @return JSON格式配置
     */
    public JSONObject getJSONObject(String serialNumber) {
        return getJSONObject(serialNumber, true);
    }

    /**
     * 获取设备配置
     *
     * @param serialNumber 序列号
     * @param node         节点
     * @return JSON格式配置
     */
    public JSONObject getJSONObject(String serialNumber, String node) {
        return getJSONObject(serialNumber, node, true);
    }

    /**
     * 获取设备配置
     *
     * @param serialNumber 序列号
     * @param inCache      是否从缓存中获取
     * @return JSON格式配置
     */
    public JSONObject getJSONObject(String serialNumber, boolean inCache) {
        return JSONFormatConfig.format(getJSONArray(serialNumber, inCache), "");
    }

    /**
     * 获取设备配置
     *
     * @param serialNumber 序列号
     * @param node         节点
     * @param inCache      是否从缓存中获取
     * @return JSON格式配置
     */
    public JSONObject getJSONObject(String serialNumber, String node, boolean inCache) {
        return JSONFormatConfig.format(getJSONArray(serialNumber, inCache), node);
    }

    /**
     * 获取配置信息
     *
     * @param serialNumber 序列号
     * @return JSONArray的配置信息
     */
    public JSONArray getJSONArray(String serialNumber) {
        return getJSONArray(serialNumber, true);
    }

    /**
     * 获取配置信息
     *
     * @param serialNumber 序列号
     * @param inCache      是否优先从缓存中获取
     * @return JSONArray的配置信息
     */
    public JSONArray getJSONArray(String serialNumber, boolean inCache) {
        GeneralDeviceConfigEntity entity = new GeneralDeviceConfigEntity();
        if (inCache) entity.cache(String.format("GeneralDevice:%s:Config", serialNumber));

        Map<String, Object> data = entity
                .field("config")
                .where("serialNumber", serialNumber).find();
        String configString = MapUtil.getString(data, "config");
        if (!StringUtils.hasLength(configString)) return null;
        return JSONArray.parseArray(configString);
    }

    /**
     * 修改配置
     *
     * @param serialNumber 序列号
     * @param rootName     根节点名
     * @param name         配置名
     * @param value        配置值
     * @return 是否修改成功
     */
    public boolean setConfig(String serialNumber, String rootName, String name, String value) {
        if (!StringUtil.hasLength(serialNumber)) return false;

        Map<String, Object> data = GeneralDeviceConfigEntity.getInstance()
                .field("config")
                .where("serialNumber", serialNumber)
                .find();
        if (data == null || data.isEmpty()) return false;

        JSONArray root = MapUtil.getJSONArray(data, "config");
        if (root == null) return false;

        JSONFormatConfig jsonConfig = new JSONFormatConfig(root);
        jsonConfig.put(rootName, name, value);
        return setConfig(serialNumber, jsonConfig.origin);
    }

    /**
     * 更新配置
     *
     * @param serialNumber 序列号
     * @param config       配置结构
     * @return 是否更新成功
     */
    public boolean setConfig(String serialNumber, JSONArray config) {
        DataService.getMainCache().del(String.format("GeneralDevice:%s:Config", serialNumber));

        GeneralDeviceConfigEntity configEntity = new GeneralDeviceConfigEntity();
        if (!configEntity.where("serialNumber", serialNumber).exist()) {
            // 不存在数据，则进行新增
            return configEntity.insert(new LinkedHashMap<>() {{
                put("serialNumber", serialNumber);
                put("config", config.toJSONString());
            }}) > 0;
        }
        int noquery = configEntity
                .where("serialNumber", serialNumber)
                .update(new HashMap<>() {{
                    put("config", config.toJSONString());
                }});
        return noquery > 0;
    }
}
