package com.evcharge.libsdk.dahua;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DaHuaDeviceSDK {

    private final String organizeCode = SysGlobalConfigEntity.getString("System:Organize:Code");

    /**
     * 获取设备列表
     */
    public void getDeviceList() {
        DaHuaAuthSDK daHuaAuthSDK = new DaHuaAuthSDK();
        Map<String, Object> param = new HashMap<>();
        param.put("pageNum", 1);
        param.put("pageSize", 100);
        String url = DaHuaConfig.deviceListUrl;
        Map<String, Object> header = daHuaAuthSDK.createHeader(param);
        String text = Http2Util.post(url, param, header, "application/json");

        if (!StringUtils.hasLength(text)) {
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(text);
        if (!jsonObject.getString("code") .equals("200")) {
            LogsUtil.info(this.getClass().getName(), "【大华云联】获取设备列表失败，失败原因：" + jsonObject.getString("msg"));
            return;
        }

        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("pageData").getJSONObject(0).getJSONArray("deviceList");
        for (int i = 0; i < jsonArray.size(); i++) {

//            {
//                "modelId": "产品编号",
//                    "catalog": "设备大类 【NVR/DVR/IPC/SD/ASI】",
//                    "deviceCatalog": "设备大类 【NVR/DVR/IPC/SD/ASI】",
//                    "channelNum": "设备最大支持接入的通道数",
//                    "deviceVersion": "设备版本",
//                    "source": "权限类型",
//                    "deviceId": "设备序列号",
//                    "deviceName": "设备名称",
//                    "deviceStatus": "设备状态 online：在线 offline：离线 sleep：休眠 upgrading升级中",
//                    "accessType": "设备接入类型 PaaS-表示Paas程序接入 Lechange-表示乐橙非PaaS设备 Easy4IP表示Easy4IP程序设备 P2P表示P2P程序设备",
//                    "deviceAbility": "设备能力集",
//                    "playInfo": "设备播放码 opensdk使用",
//                    "channels": [
//                            {
//                                "csStatus": "云存储状态 notExist：未开通套餐 using：开通云存储且没有过期 expired：套餐过期",
//                                    "shareFunctions": "分享权限下的权限字符串",
//                                    "lastOffLineTime": "通道最后离线时间",
//                                    "channelName": "通道名称",
//                                    "channelStatus": "通道状态 online：在线 offline：离线 sleep：休眠 upgrading升级中",
//                                    "channelAbility": "通道能力集",
//                                    "channelPicUrl": "设备通道封面图 新设备可能不存在封面图，可上传设备通道封面图或者刷新设备封面图",
//                                    "channelId": "通道号",
//                                    "encryptMode": "设备加密模式 0-设备默认加密 1-用户自定义加密"
//                            }
//                ],
//                "lastOffLineTime": "设备最后离线时间",
//                    "deviceModel": "设备型号",
//                    "canBeUpgrade": "设备软件程序是否有新版本可以升级",
//                    "brand": "设备品牌信息 lechange-乐橙设备 general-通用设备",
//                    "secondCategoryCode": "设备二级品类"
//            }

            JSONObject deviceObject = jsonArray.getJSONObject(i);

            String deviceId = deviceObject.getString("deviceId");
            String deviceName = deviceObject.getString("deviceName");
            String deviceStatus = deviceObject.getString("deviceStatus");
            int onloadStatus = 0; //默认离线状态

            switch (deviceStatus) {
                case "online":
                    onloadStatus = 1;
                    break;
                case "sleep":
                    onloadStatus = 2;
                    break;
                case "upgrading升级中":
                    onloadStatus = 3;
                    break;
                default:
                    break;
            }
            String simCode = "";
            SyncResult simCodeResult = getDeviceSimCode(deviceId);
            if (simCodeResult.code == 0) {
                simCode = (String) simCodeResult.getData();
            }

            GeneralDeviceEntity generalDeviceEntity = GeneralDeviceEntity.getInstance().getBySerialNumber(deviceId, false);
            Map<String, Object> data = new LinkedHashMap<>();
            if (generalDeviceEntity != null) {

                data.put("deviceName", deviceName);
                data.put("online_status", onloadStatus);
                data.put("update_time", TimeUtil.getTimestamp());
                data.put("simCode", simCode);//SIM编码
                /**
                 * TODO
                 * 如果onloadStatus 不为1 则判断摄像头离线，需要添加企业微信通知
                 */

                GeneralDeviceEntity.getInstance().where("id", generalDeviceEntity.id).update(data);
                continue;
            }

            data.put("deviceName", deviceName);
            data.put("serialNumber", deviceId);
            data.put("spuCode", ""); //产品SPU代码
            data.put("brandCode", ""); //品牌编码
            data.put("typeCode", ""); //类型编码
            data.put("online_status", onloadStatus);
            data.put("status", 1);
            data.put("simCode", simCode);//SIM编码
            data.put("batchNumber", "");//批次号
            data.put("spec", JSONObject.toJSONString(deviceObject));//规格
            data.put("dynamic_info", "");//动态信息
            data.put("organize_code", this.organizeCode);//组织代码
            data.put("platform_code", this.organizeCode);//充电平台代码
            data.put("create_time", TimeUtil.getTimestamp());//充电平台代码
            data.put("update_time", TimeUtil.getTimestamp());
            GeneralDeviceEntity.getInstance().insertGetId(data);

        }

    }


    //

    /**
     * 获取simCode
     *
     * @param deviceId 设备序列号
     */
    public SyncResult getDeviceSimCode(String deviceId) {
        String url = DaHuaConfig.deviceSimUrl;

        DaHuaAuthSDK daHuaAuthSDK = new DaHuaAuthSDK();
        Map<String, Object> param = new HashMap<>();
        param.put("deviceId", deviceId);
        String token = daHuaAuthSDK.getAppAccessToken();

        Map<String, Object> header = new HashMap<>();
        header.put("AppAccessToken", token);
        String text = Http2Util.post(url, param, header, "application/json");


        if (!StringUtils.hasLength(text)) {
            return new SyncResult(1, "大华云联】获取设备simCode失败，设备ID=" + deviceId);
        }
        JSONObject jsonObject = JSONObject.parseObject(text);
        if (!jsonObject.getString("code") .equals("200")) {
            LogsUtil.info(this.getClass().getName(), "【大华云联】获取设备simCode，失败原因：" + jsonObject.getString("msg"));
            return new SyncResult(1, "大华云联】获取设备simCode失败，设备ID=" + deviceId);
        }

        JSONObject simData = jsonObject.getJSONObject("data");


        String simCode = simData.getString("ICCID");

        return new SyncResult(0, "success", simCode);


    }


}
