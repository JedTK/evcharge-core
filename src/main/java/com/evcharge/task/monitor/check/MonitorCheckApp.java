package com.evcharge.task.monitor.check;

import com.evcharge.entity.device.GeneralDeviceCheckLogEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.web.client.RestTemplate;
//import org.springframework.web.socket.client.WebSocketClient;
//import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.*;

public class MonitorCheckApp {

    public static MonitorCheckApp getInstance() {
        return new MonitorCheckApp();
    }


    public void run() {
        // 1. 创建配置对象
        EzvizConfig ezvizConfig = new EzvizConfig();

        // 2. 创建HTTP和WebSocket客户端实例
        RestTemplate restTemplate = new RestTemplate();
//        WebSocketClient webSocketClient = new StandardWebSocketClient();

        // 3. 创建并组装所有检测器
        DeviceChecker ezvizChecker = new EzvizDeviceChecker(ezvizConfig, restTemplate);
        DeviceChecker genericChecker = new GenericWebRtcChecker();

        // 4. 手动创建一个Map来管理检测器，实现策略模式
        Map<DeviceType, DeviceChecker> checkers = new HashMap<>();
        checkers.put(ezvizChecker.supports(), ezvizChecker);
        checkers.put(genericChecker.supports(), genericChecker);


        // 5. 定义待检测的设备列表
        // TODO: 在实际应用中，此设备列表应从数据库或配置文件动态加载。
//        List<Device> devices = List.of(
//                new Device("D001", "客厅萤石摄像头", DeviceType.EZVIZ, Map.of("deviceSerial", "E12345678")),
//                new Device("D002", "仓库WebRTC摄像头", DeviceType.GENERIC_WEBRTC, Map.of("signalingUrl", "wss://echo.websocket.org/")),
//                new Device("D003", "办公室故障WebRTC摄像头", DeviceType.GENERIC_WEBRTC, Map.of("signalingUrl", "ws://localhost:9999"))
//        );
        List<GeneralDeviceEntity> list = GeneralDeviceEntity
                .getInstance()
                .where("typeCode", "4GNVR")
                .where("brandCode", "HIKVISION")
                .selectList();

//        for (GeneralDeviceEntity generalDeviceEntity : list) {
//            devices.add(new Device(
//                    String.valueOf(generalDeviceEntity.id),generalDeviceEntity.deviceName,DeviceType.EZVIZ,new LinkedHashMap<>()
//            ));
//        }

//        for (Device device : devices) {
        for (GeneralDeviceEntity generalDeviceEntity : list) {
            Device device = new Device(
                    String.valueOf(generalDeviceEntity.id)
                    , generalDeviceEntity.deviceName
                    , generalDeviceEntity.serialNumber
                    , DeviceType.EZVIZ, new LinkedHashMap<>()
            );

            // 根据设备类型从 Map 中获取对应的检测器
            DeviceChecker checker = checkers.get(device.getType());
            if (checker == null) {
                LogsUtil.warn(this.getClass().getName(), "未找到支持设备类型 [{%s}] 的检测器，跳过设备 [{%s}]。", device.getType(), device.getName());
                continue;
            }
            Map<String, Object> log = new HashMap<>();
            log.put("device_name", generalDeviceEntity.deviceName);
            log.put("serial_number", generalDeviceEntity.serialNumber);
            log.put("brand_code", generalDeviceEntity.brandCode);
            log.put("type_code", generalDeviceEntity.typeCode);
            log.put("check_time", TimeUtil.getTimestamp());


            // 执行检测
            SyncResult result = checker.check(device);

            // 根据结果输出日志
            if (result.code == 0) {
                LogsUtil.info(this.getClass().getName(), "✅ 检测通过: 设备 [{%s}]({%s}) 状态正常。\n", device.getName(), device.getId());
                log.put("check_status", 1);
            } else {
                LogsUtil.info(this.getClass().getName(), "❌ 检测失败: 设备 [{%s}]({%s}) 状态异常！\n", device.getName(), device.getId());
                // TODO: 在此处添加将异常信息记录到数据库的逻辑。
                log.put("check_status", 2);
            }
            log.put("callback_message", result.msg);
            log.put("callback_content", result.data);
            GeneralDeviceCheckLogEntity.getInstance().insert(log);
        }

        LogsUtil.info(this.getClass().getName(), "********** 设备状态检测执行完毕 **********");


    }


}
