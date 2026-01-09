package com.evcharge.service.GeneralDevice;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.mqtt.XMQTTFactory;
import com.evcharge.service.meter.TQ4GMeterService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import lombok.NonNull;

/**
 * 充电站电源控制与安全管理服务（内部习惯也称“配电箱控制服务”）
 * <p>
 * 业务定位
 * 1) 这是业务侧的“控制入口”，给 API Controller 调用，用于对站点的供电链路进行远程控制。
 * 2) 设备的底层协议适配、指令下发、设备在线/重试/回执等通信细节已在 evcharge-device-hub 中的 GeneralDeviceServer 完成。
 * 因此本类不处理任何协议帧，不关心 DTU / 串口 / Modbus / 自定义协议细节，只负责：
 * - 校验站点是否存在、是否绑定了对应设备
 * - 组织业务参数
 * - 通过 MQTT 发送“标准化控制命令”
 * <p>
 * 典型硬件组成（同一站点可能不全量配置）
 * - 空气开关：供电总开关（通常不可远程直接控制或只做状态）
 * - 漏保：人身安全保护（通常不可远程控制，只做状态/告警）
 * - 交流接触器：主供电通断（可能由 DO 控制）
 * - 4G 电表：电量计量 + 远程分合闸（某些品牌支持）
 * - 4G DTU：通信链路（上报/下发的通道）
 * - DI/DO 中继器：数字量输入输出（控制接触器、指示灯、报警器等）
 * - 水侵报警器、状态灯等：通常作为 DI 上报或 DO 控制
 */
public class ChargePowerControlService {

    /**
     * 4G 电表远程分闸/合闸（控制站点供电）
     * <p>
     * 功能说明
     * - 用站点 CSId 找到站点实体，确认站点有效
     * - 在站点已绑定的通用设备中，找到类型码为 "4GEM" 的设备（4G Electric Meter，4G 电能表）
     * - 根据品牌选择不同的下发方式：
     * - 当前示例仅对品牌 "CHTQDQ"（上海人民拓强4G电能表）走 TQ4GMeterService 下发
     * - 其他品牌暂未实现，返回“未支持/不匹配”
     * <p>
     * 参数说明
     *
     * @param CSId   站点编号（ChargeStationEntity 主键/业务主键）
     * @param status 分合闸状态：
     *               0 = 分闸（断电）
     *               1 = 合闸（通电）
     */
    public static ISyncResult meterSwitch(@NonNull String CSId, int status) {
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
        if (chargeStationEntity == null) return new SyncResult(2, "无效充电桩");

        // 设备类型码约定：
        // "4GEM"：4G 电能表（具备计量/远程拉合闸能力，具体能力取决于品牌与型号）
        GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithCSId(CSId, "4GEM");
        if (deviceEntity == null) return new SyncResult(3, "无法操作：站点未绑定4G电能表");

        // 品牌适配：当前仅实现某品牌电表的远程拉合闸
        if ("CHTQDQ".equalsIgnoreCase(deviceEntity.brandCode)) {
            // sendMQTTGateSwitch 参数语义（按你现有调用推断）：
            // serialNumber：电表唯一编号/设备序列号
            // status：0/1 分合闸
            // -1：通常表示默认/不指定（例如：不指定相位/不指定回路/不指定地址）
            // appChannelCode：设备所在应用通道（多品牌/多租户时用于隔离指令域）
            TQ4GMeterService.getInstance().sendMQTTGateSwitch(
                    deviceEntity.serialNumber,
                    status,
                    -1,
                    deviceEntity.appChannelCode
            );
            return new SyncResult(0, "");
        }

        // 其他品牌暂未实现
        // 你也可以把 message 改得更明确，例如："不支持的电表品牌：" + deviceEntity.brandCode
        return new SyncResult(1, "");
    }

    /**
     * 配电箱控制器 DO 开关控制（数字量输出）
     * <p>
     * 功能说明
     * - 用站点 CSId 找到站点实体，确认站点有效
     * - 在站点绑定的通用设备中，找到类型码为 "PD-CTR-DTU" 的设备（配电箱控制器/DTU 控制器）
     * - 组织标准命令参数，通过 MQTT 下发控制指令
     * <p>
     * 典型用途
     * - 控制交流接触器线圈通断（实现站点主供电通断）
     * - 控制状态灯、蜂鸣器等外设
     * - 控制其他继电器输出通道
     * <p>
     * 参数说明
     *
     * @param CSId        站点编号
     * @param switchIndex DO 输出通道索引，从 0 开始
     *                    例：0=DO1，1=DO2 ...（具体通道映射取决于硬件定义/协议适配层）
     * @param status      开关状态：0 = 关闭 1 = 打开
     *                    <p>
     * MQTT 下发约定说明（按你当前实现）
     * - Topic：{appChannelCode}/{serialNumber}/command/pd_ctr_do_switch
     * 语义：向指定通道下、指定序列号的设备发送“配电箱控制器 DO 切换”命令
     * - Payload(JSON)：
     *   serialNumber：设备序列号（冗余字段，便于 device-hub 侧做一致性校验/日志）
     *   index：DO 通道索引
     *   status：0/1
     * - QoS：1（至少一次投递）
     */
    public static ISyncResult doSwitch(@NonNull String CSId, int switchIndex, int status) {
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
        if (chargeStationEntity == null) return new SyncResult(2, "无效充电桩");

        // 设备类型码约定：
        // "PD-CTR-DTU"：配电箱控制器（通常包含 DI/DO、与 4G DTU 或主控通信能力）
        GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithCSId(CSId, "PD-CTR-DTU");
        if (deviceEntity == null) return new SyncResult(3, "无法操作：站点未绑定配电箱控制器");

        JSONObject requestBody = new JSONObject();
        requestBody.put("serialNumber", deviceEntity.serialNumber);
        requestBody.put("index", switchIndex);
        requestBody.put("status", status);

        // 发布控制命令：
        // QoS=1：保证至少一次到达（可能重复到达，设备侧/协议适配层应保证幂等或可容忍）
        XMQTTFactory.getInstance().publish(
                String.format("%s/%s/command/pd_ctr_do_switch", deviceEntity.appChannelCode, deviceEntity.serialNumber),
                requestBody,
                1
        );
        return new SyncResult(0, "");
    }
}
