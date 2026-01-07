package com.evcharge.libsdk.dahua;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceConfigEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DaHuaDeviceSDK {

    private final String organizeCode = SysGlobalConfigEntity.getString("System:Organize:Code");
    // 定义常量，避免硬编码
    private static final String BRAND_DAHUA = "dahuatech";
    private static final String STATUS_ONLINE = "1";
    private static final String STATUS_OFFLINE = "0";

    /**
     * 检查摄像头在线常态
     */
    public void syncCheckDeviceStatus() {
        int pageNum = 1;
        final int PAGE_SIZE = 50; // 建议50，平衡网络IO和内存
        boolean hasNext = true;

        LogsUtil.info(this.getClass().getName(), "【大华云联】开始执行设备在线状态巡检...");

        while (hasNext) {
            // 1. 分页查询数据库中的大华设备
            List<GeneralDeviceEntity> deviceList = GeneralDeviceEntity.getInstance()
                    .where("organizeCode", organizeCode)
                    .where("brandCode", BRAND_DAHUA)
                    .page(pageNum, PAGE_SIZE)
                    .selectList();

            if (deviceList == null || deviceList.isEmpty()) {
                hasNext = false;
                break;
            }

            // 2. 批量处理当前页（独立方法，利于GC）
            processBatchStatusCheck(deviceList);

            // 3. 翻页逻辑
            if (deviceList.size() < PAGE_SIZE) {
                hasNext = false;
            } else {
                pageNum++;
                // 避免频繁请求导致被大华接口限流，建议短暂休眠
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
        }

        LogsUtil.info(this.getClass().getName(), "【大华云联】设备状态巡检完成。");
    }

    /**
     * 处理单批次设备的逻辑
     */
    private void processBatchStatusCheck(List<GeneralDeviceEntity> deviceList) {
        for (GeneralDeviceEntity deviceEntity : deviceList) {
            if (!StringUtils.hasLength(deviceEntity.serialNumber)) continue;

            try {
                // 调用接口检查状态
                SyncResult syncResult = checkDeviceOnlineStatus(deviceEntity.serialNumber);

                if (syncResult.code != 0) {
                    // 接口调用失败，仅记录日志，不中断循环，继续下一个设备
                    LogsUtil.error(this.getClass().getName(), "检查设备状态异常: " + syncResult.msg);
                    continue;
                }

                String onlineStatus = (String) syncResult.data;

                // 准备更新数据
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("online_status", onlineStatus); // 建议转为int存储
                updateMap.put("update_time", TimeUtil.getTimestamp());

                // 业务逻辑分支
                if (STATUS_OFFLINE.equals(onlineStatus)) {
                    // 1. 即使离线，也要更新数据库状态！否则数据库永远显示在线
                    GeneralDeviceEntity.getInstance().where("id", deviceEntity.id).update(updateMap);

                    // 2. 触发报警通知
                    sendOffLineNotification(deviceEntity);
                } else {
                    // 在线，正常更新
                    GeneralDeviceEntity.getInstance().where("id", deviceEntity.id).update(updateMap);
                }

            } catch (Exception e) {
                LogsUtil.error(this.getClass().getName(), "处理设备状态未知异常 ID:" + deviceEntity.id, e);
            }
        }
    }

    /**
     * 检查设备状态（已修复参数丢失Bug）
     *
     * @param deviceId 设备id (序列号)
     * @return code=0 成功, data="1"(在线)/"0"(离线)
     */
    public SyncResult checkDeviceOnlineStatus(String deviceId) {
        DaHuaAuthSDK daHuaAuthSDK = new DaHuaAuthSDK();
        Map<String, Object> param = new HashMap<>();

        // 【关键修复】原代码漏传了 deviceId，接口根本不知道查哪个设备
        param.put("deviceId", deviceId);

        String url = DaHuaConfig.deviceOnlineUrl;
        Map<String, Object> header = daHuaAuthSDK.createHeader(param);

        try {
            String text = Http2Util.post(url, param, header, "application/json");

            if (!StringUtils.hasLength(text)) {
                return new SyncResult(1, String.format("API返回为空，设备ID: %s", deviceId));
            }

            JSONObject jsonObject = JSONObject.parseObject(text);
            if (!"200".equals(jsonObject.getString("code"))) {
                // 记录详细错误以便排查
                String errorMsg = jsonObject.getString("msg");
                LogsUtil.info(this.getClass().getName(), String.format("【大华云联】查询设备[%s]失败: %s", deviceId, errorMsg));
                return new SyncResult(1, errorMsg);
            }

            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                return new SyncResult(1, "API返回数据缺失data字段");
            }

            String deviceStatus = data.getString("onLine");
            return new SyncResult(0, "success", deviceStatus);

        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), "网络请求或JSON解析异常", e);
            return new SyncResult(1, "系统异常: " + e.getMessage());
        }
    }

    /**
     * 辅助方法：处理离线通知
     */
    private void sendOffLineNotification(GeneralDeviceEntity device) {
        // TODO: 实现企业微信通知逻辑
        // 建议增加去重逻辑（例如：如果数据库里已经是离线状态，就不要重复发通知了，避免消息轰炸）
        // int currentStatus = device.online_status;
        // if (currentStatus != 0) { 发送通知... }
        LogsUtil.info(this.getClass().getName(), "发现设备离线，准备发送通知: " + device.deviceName);
    }


//
//    /**
//     * 获取设备列表
//     */
//    public void getDeviceList() {
//        DaHuaAuthSDK daHuaAuthSDK = new DaHuaAuthSDK();
//        Map<String, Object> param = new HashMap<>();
//        param.put("pageNum", 1);
//        param.put("pageSize", 100);
//        String url = DaHuaConfig.deviceListUrl;
//        Map<String, Object> header = daHuaAuthSDK.createHeader(param);
//        String text = Http2Util.post(url, param, header, "application/json");
//
//        if (!StringUtils.hasLength(text)) {
//            return;
//        }
//        JSONObject jsonObject = JSONObject.parseObject(text);
//        if (!jsonObject.getString("code").equals("200")) {
//            LogsUtil.info(this.getClass().getName(), "【大华云联】获取设备列表失败，失败原因：" + jsonObject.getString("msg"));
//            return;
//        }
//
//        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("pageData").getJSONObject(0).getJSONArray("deviceList");
//
//        if (jsonArray.isEmpty()) return;
//
//        for (int i = 0; i < jsonArray.size(); i++) {
////            {
////                "modelId": "",
////                    "catalog": "SD",
////                    "deviceCatalog": "SD",
////                    "channelNum": 1,
////                    "deviceVersion": "2.820.0000017.0.R.250821",
////                    "source": "share",
////                    "deviceId": "AC0A84BRAJ00761",
////                    "deviceName": "东莞市江南农副产品批发市场站3",
////                    "deviceStatus": "online",
////                    "accessType": "PaaS",
////                    "deviceAbility": "MT,HSEncrypt,CloudStorage,LocalStorage,PlaybackByFilename,BreathingLight,LocalRecord,XUpgrade,Auth,ModifyPassword,LocalStorageEnable,Siren,WhiteLight,ESV1,PlaySound,Reboot,LinkDevAlarm,AbAlarmSound,SCCode,CustomRing,RDV2,RTSV2,PBSV2,TLSEnable,CK,WLV2,SirenTime,LRRF,DHPenetrate,DevReset,MSGSW,SIMV1,SMSCSS,SIMSS,SMSCASV1,CloudStorageRestrict,AlarmMD,PT,AudioEncodeControl,FrameReverse,MDW,MDS,HeaderDetect,CollectionPoint,SmartTrack,WideDynamic,CheckAbDecible,PT1,NVM,LEDS,Preset,PTZAC,SBPD,PTZRESET,NoIris,AlarmSchedule,SMDV,AudioTalk",
////                    "playInfo": "6ynkx7+gWj2AUK/rxrJwhpWzwRRhRnBv6d8xb896GVTwcUrDMzw9CTCmsvk7eh6xcfX3NM0EdbsOVBrzH1ZmTXpYPXL0s5xi8KTSjoAYaJ8ClahkH+ULn0arYbs/CO3nNwo7h+zDtxBmEWRgb2RI32L1XgxtEFxO6AC/wrfsL3lkQtdsv367WsSy+Q7xcbnv5HVCQJBjVcwHRkVvsLMnUpg6VgroiYxnbQ+WP08mv1Ln9yLbiQB94m9dL9vUXJ5fDxzercmMH/o39OSg9G+kukcgQTrrtioWXojJZcefNgL2IQUOJvzAUsLHPWAtxn0SoDw82ZDgAPj1PuExTZaJ6TZFhz8GBZqXfVpx7ROgpL9gjMV7ZzKdAUolBK9x2dRO1voLh5/OFPzapTKaVSYRRftU0AqC8a8nLeynKfxHYuGQAFl1qwkCHZJ62KSdxsVjao27tMaFyTf59TzfXzZuvvNxFcwRrh2o8GohXEPX0eYwR57GhE1WdWhoZyow4JI298hCNK38Ev0qjeZ5X4bTipz8BzeudWvqjKTSqag75PJPmpfizG0esKjU0PrYLKm3BADoA/AluIIiMIejK7ebqKu3S15XYpvfw4X2OmEAF1xkDCeV8va+7AwbhxgtUmNYhsPfG/dHj9/xtYPf4P0SusZP27EbTCP5KMhOhS7bNYmNjoODjXDcOhiGemL3nJzQOi+PY9HpNmiwvgwCDxs+E0KH/OtvMI7ymXWUvfhW2r/tBcgD3Dcj0Wxjv/gAuuUp81uISNqWme717XLxVUzZUcsHh/ETyihUoZBIJIHFv/DTYtzWNUNJIYjFAko2W4B9lgZJ+LWu7etYB/f0ld2Nd0CzZEnOkL5E2kTU7inn+JqcMvHAAASIdZlA6NQVLe1o/sOcEtNQnYhQa3dQ+qH2WN+bqH+upChlR5Y7YzmjqZPO3+cqK6UZOKQ6ocwQ5KafyQMrAJYSmiRvGe5hxq5cm1FK1NrKaw/+j5318zmiYtGveWFZmjVXetsym+TXA9Q1mv+3mgUqZtIs5ah+1ogt5F9F50JJQX2vxzu4JnDU0IMz1jTwlyd4ywn3pCwqP2xkdnqax2dNQ5I06JIh3VGVHzzUKI0qFiR0ItXBEfBKcOThV/DmTs+TCikalqe51d/Ge+Fw0st+EvgrkZWgwBOxE4+u9dF/CbJqZj+5aP9xUaw47BaqhRnYymZb5qal+7KH5u0b8ku4FY96o/meIVufK9xWi5GpKaLZZ34HE6PkR1qUnTSXbVlnVCu9hoxfxBqbi4Gkg0ljn2/wzM/4ACP7BTU2I9zernXZxyDSum6AGfg3oFdda9UEOamGRvldfNRW/K2abyuu7rrJar6oAkiqjEH5PtqSyEN6aYjvLzFlq0A7p4pc6Oq3uaCDIzxtM6Mj+lF9Td0l8kCWfHu4bueFueWoL09LWPUEHDC0CnDgbLd/qg5oTLHSbjcWjtvzbPsV6PstMA3U063rrqM6w/Hbk5F49wwkjzrvfAZ0MksneAksJAV4+oY0hyMp6ERWdDCN/mZOIXrZtdL3B23qbf3GfwYGnQsVMAqR4c+/KgnyTOEGmrqw9clU1i6v4QzpJtOYoUkFgi7zeLf9zzz9UJcsDoY+hDoD9YuapVpTI+D2HTREVwhbB3AmRGjHnH+4fIMOSQAxizzoxRdY46ZvPzIBShfuW6zY1e2FkFT5XN8ZfNwt9XZ5e7gyT9COxl6aqaw4W+4h3XTGqfFx+MCxg9ouBRCAzm10WYvD4Bh9kuMj8RAauWXvkmzTCHwzWYRoOW3l",
////                    "channels": [
////{
////                    "csStatus": null,
////                        "shareFunctions": null,
////                        "lastOffLineTime": "20260106T004325Z",
////                        "channelName": "东莞市江南农副产品批发市场站3",
////                        "channelStatus": "online",
////                        "channelAbility": "MT,HSEncrypt,CloudStorage,LocalStorage,PlaybackByFilename,BreathingLight,LocalRecord,XUpgrade,Auth,ModifyPassword,LocalStorageEnable,Siren,WhiteLight,ESV1,PlaySound,Reboot,LinkDevAlarm,AbAlarmSound,SCCode,CustomRing,RDV2,RTSV2,PBSV2,TLSEnable,CK,WLV2,SirenTime,LRRF,DHPenetrate,DevReset,MSGSW,SIMV1,SMSCSS,SIMSS,SMSCASV1,CloudStorageRestrict,AlarmMD,PT,AudioEncodeControl,FrameReverse,MDW,MDS,HeaderDetect,CollectionPoint,SmartTrack,WideDynamic,CheckAbDecible,PT1,NVM,LEDS,Preset,PTZAC,SBPD,PTZRESET,NoIris,AlarmSchedule,SMDV,AudioTalk",
////                        "channelPicUrl": null,
////                        "channelId": "0"
////                }
////],
////                "encryptMode": "0",
////                    "lastOffLineTime": "20260106T004324Z",
////                    "deviceModel": "DH-P4A-4G",
////                    "canBeUpgrade": false,
////                    "brand": "general",
////                    "secondCategoryCode": "SD"
////            }
//
//            JSONObject deviceObject = jsonArray.getJSONObject(i);
//
//            String deviceId = deviceObject.getString("deviceId");
//
//            String CSId = "";
//            String simCode = "";
//            String deviceName = deviceObject.getString("deviceName");
//            String deviceStatus = deviceObject.getString("deviceStatus");
//            String deviceModel = deviceObject.getString("deviceModel");
//            int onloadStatus = 0; //默认离线状态
//
//            switch (deviceStatus) {
//                case "online":
//                    onloadStatus = 1;
//                    break;
//                case "sleep":
//                    onloadStatus = 2;
//                    break;
//                case "upgrading升级中":
//                    onloadStatus = 3;
//                    break;
//                default:
//                    break;
//            }
//            SyncResult simCodeResult = getDeviceSimCode(deviceId);
//            if (simCodeResult.code == 0) {
//                simCode = (String) simCodeResult.getData();
//            }
//
//            Map<String, Object> data = new LinkedHashMap<>();
//            GeneralDeviceEntity generalDeviceEntity = GeneralDeviceEntity.getInstance().getBySerialNumber(deviceId, false);
//
//            if (generalDeviceEntity != null) {
//
//                data.put("deviceName", deviceName);
//                data.put("spuCode", deviceModel); //产品SPU代码
//                data.put("online_status", onloadStatus);
//                data.put("update_time", TimeUtil.getTimestamp());
//                data.put("simCode", simCode);//SIM编码
//                /**
//                 * TODO
//                 * 如果onloadStatus 不为1 则判断摄像头离线，需要添加企业微信通知
//                 */
//                if (!StringUtils.hasLength(generalDeviceEntity.CSId)) {
//                    ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().where("name", deviceName).findEntity();
//                    if (chargeStationEntity != null) {
//                        CSId = chargeStationEntity.CSId;
//                        data.put("CSId", CSId);
//                    }
//                }
//                GeneralDeviceEntity.getInstance().where("id", generalDeviceEntity.id).update(data);
//                continue;
//            } else {
//                ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().where("name", deviceName).findEntity();
//                if (chargeStationEntity != null) {
//                    CSId = chargeStationEntity.CSId;
//                }
//            }
//
//            data.put("deviceName", deviceName);
//            data.put("serialNumber", deviceId);
//            data.put("CSId", CSId);
//            data.put("spuCode", deviceModel); //产品SPU代码
//            data.put("brandCode", "dahuatech"); //品牌编码
//            data.put("typeCode", "4GNVR"); //类型编码
//            data.put("online_status", onloadStatus);
//            data.put("status", 1);
//            data.put("simCode", simCode);//SIM编码
//            data.put("batchNumber", "");//批次号
//            data.put("spec", JSONObject.toJSONString(deviceObject));//规格
//            data.put("dynamic_info", "");//动态信息
//            data.put("organize_code", this.organizeCode);//组织代码
//            data.put("platform_code", this.organizeCode);//充电平台代码
//            data.put("create_time", TimeUtil.getTimestamp());//充电平台代码
//            data.put("update_time", TimeUtil.getTimestamp());
//            GeneralDeviceEntity.getInstance().insertGetId(data);
//
//        }
//
//    }


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
        if (!jsonObject.getString("code").equals("200")) {
            LogsUtil.info(this.getClass().getName(), "【大华云联】获取设备simCode，失败原因：" + jsonObject.getString("msg"));
            return new SyncResult(1, "大华云联】获取设备simCode失败，设备ID=" + deviceId);
        }

        JSONObject simData = jsonObject.getJSONObject("data");

        String simCode = simData.getString("ICCID");

        return new SyncResult(0, "success", simCode);
    }


    public void SyncDeviceList() {
        getDeviceList();
    }

    /**
     * 获取设备列表信息
     */
    public void getDeviceList() {
        int pageNum = 1;
        // 内存优化策略：减小 pageSize，例如改为 50 或 20。
        // 虽然网络交互次数增加，但能显著降低单次循环的内存峰值。
        final int PAGE_SIZE = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            // 1. 获取原始数据字符串
            String responseText = fetchDeviceListFromApi(pageNum, PAGE_SIZE);

            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.info(this.getClass().getName(), "【大华云联】第 " + pageNum + " 页获取为空，结束同步。");
                break;
            }

            // 2. 解析为轻量级对象进行校验 (避免过早解析深层结构)
            JSONObject jsonObject = JSONObject.parseObject(responseText);
            if (jsonObject == null || !"200".equals(jsonObject.getString("code"))) {
                String msg = jsonObject != null ? jsonObject.getString("msg") : "结果为null";
                LogsUtil.info(this.getClass().getName(), "【大华云联】获取设备列表失败或结束，页码：" + pageNum + "，原因：" + msg);
                break;
            }

            // 3. 提取设备列表数组
            JSONArray deviceList = extractDeviceList(jsonObject);

            // 显式释放大字符串内存（虽然方法结束后也会释放，但在内存紧张系统中很有用）
            responseText = null;
            jsonObject = null;

            if (deviceList == null || deviceList.isEmpty()) {
                hasNextPage = false;
            } else {
                // 4. 调用独立方法处理业务逻辑（核心内存优化点）
                processPageData(deviceList);

                // 如果返回的数据条数小于请求的条数，说明是最后一页
                if (deviceList.size() < PAGE_SIZE) {
                    hasNextPage = false;
                } else {
                    pageNum++;
                    // 可选：Thread.sleep(100); // 稍微休眠，给GC喘息时间，防止CPU飙升
                }
            }
        }
    }

    /**
     * 封装网络请求，保持主流程清晰
     */
    private String fetchDeviceListFromApi(int pageNum, int pageSize) {
        DaHuaAuthSDK daHuaAuthSDK = new DaHuaAuthSDK();
        Map<String, Object> param = new HashMap<>();
        param.put("pageNum", pageNum);
        param.put("pageSize", pageSize);
        String url = DaHuaConfig.deviceListUrl;
        Map<String, Object> header = daHuaAuthSDK.createHeader(param);
        return Http2Util.post(url, param, header, "application/json");
    }

    /**
     * 安全提取设备列表，处理多层级空指针风险
     */
    private JSONArray extractDeviceList(JSONObject rootJson) {
        try {
            JSONObject data = rootJson.getJSONObject("data");
            if (data == null) return null;

            JSONArray pageData = data.getJSONArray("pageData");
            if (pageData == null || pageData.isEmpty()) return null;

            // 根据你提供的JSON结构，deviceList在pageData的第一个元素里
            JSONObject firstPageObj = pageData.getJSONObject(0);
            if (firstPageObj == null) return null;

            return firstPageObj.getJSONArray("deviceList");
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), "解析大华设备列表结构异常", e);
            return null;
        }
    }

    /**
     * 处理单页数据
     * 优点：方法执行完后，内部创建的大量临时对象（Map, String等）可立即被GC回收
     */
    private void processPageData(JSONArray deviceList) {
        // 预先获取时间戳，避免循环内重复调用
        long currentTimestamp = TimeUtil.getTimestamp();

        // 缓存 ChargeStationEntity，避免循环内重复查询数据库（性能优化）
        // Key: deviceName, Value: CSId
        Map<String, String> stationCache = new HashMap<>();

        for (int i = 0; i < deviceList.size(); i++) {
            JSONObject deviceObject = deviceList.getJSONObject(i);
            if (deviceObject == null) continue;

            try {
                saveOrUpdateDevice(deviceObject, currentTimestamp, stationCache);
            } catch (Exception e) {
                // 单个设备处理失败不影响整个批次
                LogsUtil.error(this.getClass().getName(), "处理设备异常: " + deviceObject.getString("deviceId"), e);
            }
        }
    }


    /**
     * 单个设备的入库逻辑
     */
    private void saveOrUpdateDevice(JSONObject deviceObject, long currentTimestamp, Map<String, String> stationCache) {

//            {
//                "modelId": "",
//                    "catalog": "SD",
//                    "deviceCatalog": "SD",
//                    "channelNum": 1,
//                    "deviceVersion": "2.820.0000017.0.R.250821",
//                    "source": "share",
//                    "deviceId": "AC0A84BRAJ00761",
//                    "deviceName": "东莞市江南农副产品批发市场站3",
//                    "deviceStatus": "online",
//                    "accessType": "PaaS",
//                    "deviceAbility": "MT,HSEncrypt,CloudStorage,LocalStorage,PlaybackByFilename,BreathingLight,LocalRecord,XUpgrade,Auth,ModifyPassword,LocalStorageEnable,Siren,WhiteLight,ESV1,PlaySound,Reboot,LinkDevAlarm,AbAlarmSound,SCCode,CustomRing,RDV2,RTSV2,PBSV2,TLSEnable,CK,WLV2,SirenTime,LRRF,DHPenetrate,DevReset,MSGSW,SIMV1,SMSCSS,SIMSS,SMSCASV1,CloudStorageRestrict,AlarmMD,PT,AudioEncodeControl,FrameReverse,MDW,MDS,HeaderDetect,CollectionPoint,SmartTrack,WideDynamic,CheckAbDecible,PT1,NVM,LEDS,Preset,PTZAC,SBPD,PTZRESET,NoIris,AlarmSchedule,SMDV,AudioTalk",
//                    "playInfo": "6ynkx7+gWj2AUK/rxrJwhpWzwRRhRnBv6d8xb896GVTwcUrDMzw9CTCmsvk7eh6xcfX3NM0EdbsOVBrzH1ZmTXpYPXL0s5xi8KTSjoAYaJ8ClahkH+ULn0arYbs/CO3nNwo7h+zDtxBmEWRgb2RI32L1XgxtEFxO6AC/wrfsL3lkQtdsv367WsSy+Q7xcbnv5HVCQJBjVcwHRkVvsLMnUpg6VgroiYxnbQ+WP08mv1Ln9yLbiQB94m9dL9vUXJ5fDxzercmMH/o39OSg9G+kukcgQTrrtioWXojJZcefNgL2IQUOJvzAUsLHPWAtxn0SoDw82ZDgAPj1PuExTZaJ6TZFhz8GBZqXfVpx7ROgpL9gjMV7ZzKdAUolBK9x2dRO1voLh5/OFPzapTKaVSYRRftU0AqC8a8nLeynKfxHYuGQAFl1qwkCHZJ62KSdxsVjao27tMaFyTf59TzfXzZuvvNxFcwRrh2o8GohXEPX0eYwR57GhE1WdWhoZyow4JI298hCNK38Ev0qjeZ5X4bTipz8BzeudWvqjKTSqag75PJPmpfizG0esKjU0PrYLKm3BADoA/AluIIiMIejK7ebqKu3S15XYpvfw4X2OmEAF1xkDCeV8va+7AwbhxgtUmNYhsPfG/dHj9/xtYPf4P0SusZP27EbTCP5KMhOhS7bNYmNjoODjXDcOhiGemL3nJzQOi+PY9HpNmiwvgwCDxs+E0KH/OtvMI7ymXWUvfhW2r/tBcgD3Dcj0Wxjv/gAuuUp81uISNqWme717XLxVUzZUcsHh/ETyihUoZBIJIHFv/DTYtzWNUNJIYjFAko2W4B9lgZJ+LWu7etYB/f0ld2Nd0CzZEnOkL5E2kTU7inn+JqcMvHAAASIdZlA6NQVLe1o/sOcEtNQnYhQa3dQ+qH2WN+bqH+upChlR5Y7YzmjqZPO3+cqK6UZOKQ6ocwQ5KafyQMrAJYSmiRvGe5hxq5cm1FK1NrKaw/+j5318zmiYtGveWFZmjVXetsym+TXA9Q1mv+3mgUqZtIs5ah+1ogt5F9F50JJQX2vxzu4JnDU0IMz1jTwlyd4ywn3pCwqP2xkdnqax2dNQ5I06JIh3VGVHzzUKI0qFiR0ItXBEfBKcOThV/DmTs+TCikalqe51d/Ge+Fw0st+EvgrkZWgwBOxE4+u9dF/CbJqZj+5aP9xUaw47BaqhRnYymZb5qal+7KH5u0b8ku4FY96o/meIVufK9xWi5GpKaLZZ34HE6PkR1qUnTSXbVlnVCu9hoxfxBqbi4Gkg0ljn2/wzM/4ACP7BTU2I9zernXZxyDSum6AGfg3oFdda9UEOamGRvldfNRW/K2abyuu7rrJar6oAkiqjEH5PtqSyEN6aYjvLzFlq0A7p4pc6Oq3uaCDIzxtM6Mj+lF9Td0l8kCWfHu4bueFueWoL09LWPUEHDC0CnDgbLd/qg5oTLHSbjcWjtvzbPsV6PstMA3U063rrqM6w/Hbk5F49wwkjzrvfAZ0MksneAksJAV4+oY0hyMp6ERWdDCN/mZOIXrZtdL3B23qbf3GfwYGnQsVMAqR4c+/KgnyTOEGmrqw9clU1i6v4QzpJtOYoUkFgi7zeLf9zzz9UJcsDoY+hDoD9YuapVpTI+D2HTREVwhbB3AmRGjHnH+4fIMOSQAxizzoxRdY46ZvPzIBShfuW6zY1e2FkFT5XN8ZfNwt9XZ5e7gyT9COxl6aqaw4W+4h3XTGqfFx+MCxg9ouBRCAzm10WYvD4Bh9kuMj8RAauWXvkmzTCHwzWYRoOW3l",
//                    "channels": [
//                {
//                    "csStatus": null,
//                        "shareFunctions": null,
//                        "lastOffLineTime": "20260106T004325Z",
//                        "channelName": "东莞市江南农副产品批发市场站3",
//                        "channelStatus": "online",
//                        "channelAbility": "MT,HSEncrypt,CloudStorage,LocalStorage,PlaybackByFilename,BreathingLight,LocalRecord,XUpgrade,Auth,ModifyPassword,LocalStorageEnable,Siren,WhiteLight,ESV1,PlaySound,Reboot,LinkDevAlarm,AbAlarmSound,SCCode,CustomRing,RDV2,RTSV2,PBSV2,TLSEnable,CK,WLV2,SirenTime,LRRF,DHPenetrate,DevReset,MSGSW,SIMV1,SMSCSS,SIMSS,SMSCASV1,CloudStorageRestrict,AlarmMD,PT,AudioEncodeControl,FrameReverse,MDW,MDS,HeaderDetect,CollectionPoint,SmartTrack,WideDynamic,CheckAbDecible,PT1,NVM,LEDS,Preset,PTZAC,SBPD,PTZRESET,NoIris,AlarmSchedule,SMDV,AudioTalk",
//                        "channelPicUrl": null,
//                        "channelId": "0"
//                }
//],
//                "encryptMode": "0",
//                    "lastOffLineTime": "20260106T004324Z",
//                    "deviceModel": "DH-P4A-4G",
//                    "canBeUpgrade": false,
//                    "brand": "general",
//                    "secondCategoryCode": "SD"
//            }


        String deviceId = deviceObject.getString("deviceId");
        String deviceName = deviceObject.getString("deviceName");
        String deviceStatus = deviceObject.getString("deviceStatus");
        String deviceModel = deviceObject.getString("deviceModel");

        int onloadStatus = parseOnlineStatus(deviceStatus);

        // 获取SIM卡号 (注意：如果这里是网络请求，会严重拖慢速度，建议检查是否有批量获取接口)
        String simCode = "";
        SyncResult simCodeResult = getDeviceSimCode(deviceId);
        if (simCodeResult != null && simCodeResult.code == 0) {
            simCode = (String) simCodeResult.getData();
        }

        // 查找站点ID (先查缓存，再查库)
        String csId = findCSId(deviceName, stationCache);

        GeneralDeviceEntity generalDeviceEntity = GeneralDeviceEntity.getInstance().getBySerialNumber(deviceId, false);

        // 构建数据 Map
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceName", deviceName);
        data.put("spuCode", deviceModel);
        data.put("online_status", onloadStatus);
        data.put("update_time", currentTimestamp);
        data.put("simCode", simCode);

        if (StringUtils.hasLength(csId)) {
            data.put("CSId", csId);
        }
        checkCGeneralDeviceConfig(deviceId);
        if (generalDeviceEntity != null) {
            // --- 更新逻辑 ---
            /**
             * TODO 离线通知逻辑可在此处触发
             */
            // 只有当本地没存 CSId 且现在获取到了，才更新 CSId，否则保持原样（根据你原代码逻辑推断）
            if (!StringUtils.hasLength(generalDeviceEntity.CSId) && StringUtils.hasLength(csId)) {
                data.put("CSId", csId);
            }
            GeneralDeviceEntity.getInstance().where("id", generalDeviceEntity.id).update(data);
        } else {
            // --- 新增逻辑 ---
            data.put("serialNumber", deviceId);
            data.put("brandCode", "dahuatech");
            data.put("typeCode", "4GNVR");
            data.put("status", 1);
            data.put("batchNumber", "");
            data.put("spec", deviceObject.toJSONString()); // 注意：这会产生较大字符串
            data.put("dynamic_info", "");
            data.put("organize_code", this.organizeCode);
            data.put("platform_code", this.organizeCode);
            data.put("create_time", currentTimestamp);

            GeneralDeviceEntity.getInstance().insertGetId(data);

        }
    }


    private int parseOnlineStatus(String status) {
        if (status == null) return 0;
        switch (status) {
            case "online":
                return 1;
            case "sleep":
                return 2;
            case "upgrading升级中":
                return 3;
            default:
                return 0;
        }
    }

    /**
     * 辅助方法：获取CSId，增加简单的内存缓存，减少DB查询
     */
    private String findCSId(String deviceName, Map<String, String> cache) {
        if (!StringUtils.hasLength(deviceName)) return "";

        if (cache.containsKey(deviceName)) {
            return cache.get(deviceName);
        }

        ChargeStationEntity entity = ChargeStationEntity.getInstance().where("name", deviceName).findEntity();
        String csId = (entity != null) ? entity.CSId : "";

        cache.put(deviceName, csId);
        return csId;
    }


    private void checkCGeneralDeviceConfig(String deviceId){
        if (!StringUtils.hasLength(deviceId)) return;
        GeneralDeviceConfigEntity entity = GeneralDeviceConfigEntity.getInstance().where("serialNumber", deviceId).findEntity();
        if (entity != null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serialNumber", deviceId);
        String config="[{\"name\":\"base\",\"text\":\"基础\",\"type\":\"array\",\"value\":[{\"name\":\"LiveProtocol\",\"text\":\"直播协议\",\"type\":\"text\",\"value\":\"dahua\"},{\"name\":\"channel_count\",\"text\":\"通道数量\",\"type\":\"text\",\"value\":\"1\"}]}]";
        data.put("config", config);
        GeneralDeviceConfigEntity.getInstance().insertGetId(data);
    }



}
