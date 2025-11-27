package com.evcharge.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import org.springframework.util.StringUtils;

/**
 * JSONConfig 类
 * <p>
 * 该类用于处理通用设备的配置数据，主要功能包括：
 * 1. 修改配置项的值。
 * 2. 将复杂的配置结构格式化为简单的键值对形式。
 * <p>
 * 配置数据的结构通常如下：
 * [
 * {
 * "name": "base",
 * "text": "基础",
 * "type": "array",
 * "value": [
 * {
 * "name": "isHost",
 * "text": "主机",
 * "type": "bool",
 * "value": 0
 * },
 * {
 * "name": "display_status",
 * "text": "是否显示",
 * "type": "bool",
 * "value": 1
 * },
 * {
 * "name": "socketCount",
 * "text": "充电端口数",
 * "type": "int",
 * "value": 2
 * },
 * {
 * "name": "maxPower",
 * "text": "支持最大功率",
 * "type": "int",
 * "value": 2000
 * },
 * {
 * "name": "socketLimitPower",
 * "text": "端口限制功率",
 * "type": "int",
 * "value": 1000
 * }
 * ]
 * }
 * ]
 * <p>
 * 该数据结构中，每个配置项都包含以下属性：
 * - `name`：配置项的名称，用于标识此项配置。
 * - `text`：配置项的描述或文本信息。
 * - `type`：配置项的类型，如 `bool`、`int`、`text`、`array` 等。
 * - `value`：配置项的值，可能是一个简单值，也可能是一个数组（如类型为 `array` 时）。
 * <p>
 * 示例数据还可能包含多个根节点，如 "base" 和 "api"，每个节点下有不同的配置项。
 */
public class JSONFormatConfig {

    public JSONArray origin;

    public JSONFormatConfig(JSONArray origin) {
        this.origin = origin;
    }

    /**
     * 修改配置
     * <p>
     * 该方法用于修改JSON数组中的配置值。默认情况下，它会在根节点名称为"base"的配置中修改对应的配置项。
     *
     * @param name  配置名，要修改的配置项名称
     * @param value 配置值，新值
     * @return 修改后的JSONArray
     */
    public JSONArray put(String name, Object value) {
        return put("base", name, value);
    }

    /**
     * 修改配置
     * <p>
     * 该方法用于修改JSON数组中的配置值。可以指定根节点名称，在该节点下查找并修改对应的配置项。
     *
     * @param rootName 根节点名，在此节点下查找配置项
     * @param name     配置名，要修改的配置项名称
     * @param value    配置值，新值
     * @return 修改后的JSONArray
     */
    public JSONArray put(String rootName, String name, Object value) {
        if (origin == null) return null;
        // 使用JSONPath查找指定的配置项并修改其值
        JSONPath.set(origin, String.format("$[?(@.name=='%s')].value[?(@.name=='%s')].value", rootName, name), value);
        return origin;
    }

    /**
     * 通用设备配置格式化
     * <p>
     * 该方法将复杂的配置JSON数组格式化为一个简单的JSONObject
     *
     * @param config 配置数据，类型为JSONArray
     * @return 格式化后的JSONObject，包含配置项的键值对
     */
    public static JSONObject format(JSONArray config) {
        return format(config, "");
    }

    /**
     * 通用设备配置格式化
     * <p>
     * 该方法将复杂的配置JSON数组格式化为一个简单的JSONObject。可以指定首层节点名称，只格式化该节点下的配置项。
     *
     * @param config 配置数据，类型为JSONArray
     * @param node   首层节点，如果有值只格式化对应的首层节点数据
     * @return 格式化后的JSONObject，包含配置项的键值对
     */
    public static JSONObject format(JSONArray config, String node) {
        if (config == null || config.isEmpty()) return null;

        JSONObject formatted = new JSONObject();
        config.forEach(obj -> {
            JSONObject item = (JSONObject) obj;
            if (item == null) return;

            // 获取当前配置项的名称和类型
            String itemName = item.getString("name");
            String itemType = item.getString("type");

            // 如果配置项名称为空，跳过该项
            if (!StringUtils.hasLength(itemName)) return;
            // 如果指定了节点名称且不匹配当前项，跳过该项
            if (StringUtils.hasLength(node) && !node.equalsIgnoreCase(itemName)) return;

            // 如果配置项不是数组类型，直接将其值放入结果中
            if (!StringUtils.hasLength(itemType) || !"array".equalsIgnoreCase(itemType)) {
                Object value = item.get("value");
                formatted.put(itemName, value);
                return;
            }

            // 如果配置项是数组类型，遍历其子项并放入结果中
            JSONArray subArray = item.getJSONArray("value");
            if (subArray == null) return;

            subArray.forEach(subObj -> {
                JSONObject subItem = (JSONObject) subObj;
                String subItemName = subItem.getString("name");
                if (!StringUtils.hasLength(subItemName)) return;

                Object value = subItem.get("value");
                formatted.put(subItemName, value);
            });
        });
        return formatted;
    }
}