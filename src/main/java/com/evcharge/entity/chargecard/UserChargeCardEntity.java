package com.evcharge.entity.chargecard;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.EChargePaymentType;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * 用户充电卡;
 * <p>
 * 现在有有些使用的情景：现有充电卡A
 * 1、在充电桩A中购买充电卡A，充电卡A只可以在充电桩A、B、C中使用
 * 2、在充电桩D中购买充电卡A，充电卡A只可以在充电桩D中使用
 * 3、在充电桩E中购买充电卡A，充电卡允许所有充电桩中使用
 * 4、在充电桩F中购买充电卡A，充电卡允许所有充电桩中使用（除了私有充电桩is_private=1）
 *
 * @author : JED
 * @date : 2022-10-9
 */
public class UserChargeCardEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 卡名
     */
    public String cardName;
    /**
     * 卡号
     */
    public String cardNumber;
    /**
     * 充电卡配置ID(待删除)
     */
    @Deprecated
    public long cardConfigId;
    /**
     * 充电卡配置唯一编码
     */
    public String spu_code;
    /**
     * 卡类型：1-数字充电卡，2-NFC-ID实体卡
     */
    public int cardTypeId;
    /**
     * 描述
     */
    public String describe;
    /**
     * 状态：0-删除，1-正常
     */
    public int status;
    /**
     * 优先级别
     */
    public int priority;
    /**
     * 生效时间
     */
    public long start_time;
    /**
     * 到期时间
     */
    public long end_time;
    /**
     * 是否为测试订单，0=否，1=是
     */
    public int isTest;
    /**
     * 测试ID
     */
    public long testId;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 创建时间戳
     */
    public long create_time;
    //endregion

    /**
     * 获得一个实例
     */
    public static UserChargeCardEntity getInstance() {
        return new UserChargeCardEntity();
    }

    // region 2025-04-14 过时函数，待删除

    /**
     * 检查用户是否允许添加新的充电卡
     *
     * @param uid          用户id
     * @param cardConfigId 充电卡id
     */
    @Deprecated
    public SyncResult checkAdd(long uid, long cardConfigId) {
        ChargeCardConfigEntity configEntity = ChargeCardConfigEntity.getInstance()
                .cache(String.format("ChargeCardConfig:%s:Detail", cardConfigId))
                .findEntity(cardConfigId);
        if (configEntity == null || configEntity.id == 0) return new SyncResult(2, "充电卡id错误");

        long nowTime = TimeUtil.getTimestamp();

        //region 检查充电卡互斥

        List<Object> excludeIds = ChargeCardConfigExcludeEntity.getInstance().getListWithCard1(cardConfigId);
        if (!excludeIds.isEmpty()) {
            //检查用户生效的卡是否存在互斥卡
            if (this.where("uid", uid)
                    .whereIn("cardConfigId", excludeIds)
                    .where("start_time", "<", nowTime)
                    .where("end_time", ">", nowTime)
                    .exist()) {
                return new SyncResult(10, "您已拥有同类型的充电卡，无需再购买");
            }
        }

        //endregion

        //检查是否有同类型的充电卡生效
        if (configEntity.allowSuperposition == 0) {
            //查询用户同类型的卡最新信息
            UserChargeCardEntity ucc = this
                    .where("uid", uid)
                    .where("cardConfigId", cardConfigId)
                    .order("id DESC")
                    .findEntity();
            //不存在数据，自动生成数据
            if (ucc == null || ucc.id == 0) {

            } else if (ucc != null && ucc.id > 0 && ucc.end_time > nowTime) {
                return new SyncResult(11, "已有相同类型充电卡");
            }
        }
        return new SyncResult(0, "");
    }

    /**
     * 赋予用户充电卡
     *
     * @param uid          用户id
     * @param cardConfigId 充电卡id
     */
    @Deprecated
    public SyncResult add(long uid, long cardConfigId) {
        return this.beginTransaction(connection -> addTransaction(connection, uid, cardConfigId, null, 0));
    }

    /**
     * 赋予用户充电卡 TODO：目前这种做法无法做到所有类型卡的根据优先级生效
     *
     * @param uid          用户id
     * @param cardConfigId 充电卡id
     */
    @Deprecated
    public SyncResult addTransaction(Connection connection, long uid, long cardConfigId) throws SQLException {
        return addTransaction(connection, uid, cardConfigId, null, 0);
    }

    /**
     * 赋予用户充电卡 TODO：目前这种做法无法做到所有类型卡的根据优先级生效
     *
     * @param uid          用户id
     * @param cardConfigId 充电卡id
     * @param testId       测试id
     * @param CSIdLimit    充电桩限制
     */
    @Deprecated
    public SyncResult addTransaction(Connection connection, long uid, long cardConfigId, String[] CSIdLimit, long testId) throws SQLException {
        SyncResult r = checkAdd(uid, cardConfigId);
        if (r.code != 0) return r;

        long nowTime = TimeUtil.getTimestamp();
        long start_time = 0;
        long end_time = 0;

        //是否可以继承？
        boolean canInherit = false;

        ChargeCardConfigEntity configEntity = ChargeCardConfigEntity.getInstance()
                .cache(String.format("ChargeCardConfig:%s:Detail", cardConfigId))
                .findEntity(cardConfigId);
        if (configEntity == null || configEntity.id == 0) return new SyncResult(2, "充电卡id错误");

        //查询用户同类型的卡最新信息
        UserChargeCardEntity ucc = this
                .where("uid", uid)
                .where("cardConfigId", cardConfigId)
                .order("id DESC")
                .findEntity();
        //不存在数据，自动生成数据
        if (ucc == null || ucc.id == 0) canInherit = false;
        //检查最新的数据是否还在生效中
        if (ucc != null && ucc.id > 0 && ucc.end_time > nowTime) canInherit = true;

        //判断是否继承数据
        if (canInherit) {
            end_time = ucc.end_time;
            switch (configEntity.typeId) {
                case 1://日
                    end_time += configEntity.countValue * 86400000L;
                    break;
                case 2://月
                    end_time = TimeUtil.getAddMonthTimestamp(end_time, configEntity.countValue);
                    break;
                case 3://年
                    end_time = TimeUtil.getAddMonthTimestamp(end_time, configEntity.countValue * 12);
                    break;
            }

            start_time = ucc.end_time;
        } else {
            //不继承则生产新的数据
            start_time = TimeUtil.getTime00();
            end_time = TimeUtil.getTime00();
            switch (configEntity.typeId) {
                case 1://日
                    if (configEntity.countValue == 1) {
                        start_time = TimeUtil.getTimestamp();
                        //如果只是一天卡，结束时间应该以当前时间的第二天作为结束时间，这样体验效果会好一些
                        end_time = TimeUtil.getTimestamp();
                    }
                    end_time += configEntity.countValue * 86400000L;
                    break;
                case 2://月
                    end_time = TimeUtil.getAddMonthTimestamp(end_time, configEntity.countValue);
                    break;
                case 3://年
                    end_time = TimeUtil.getAddMonthTimestamp(end_time, configEntity.countValue * 12);
                    break;
            }
        }

        //生成卡号
        String cardNumber = String.format("%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"), common.randomInt(100, 999));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", uid);
        data.put("cardNumber", cardNumber);
        data.put("cardConfigId", cardConfigId);
        data.put("spu_code", spu_code);
        data.put("priority", configEntity.priority);
        data.put("start_time", start_time);
        data.put("end_time", end_time);
        data.put("status", 1);
        data.put("testId", testId);
        data.put("create_time", TimeUtil.getTimestamp());
        if (this.insertTransaction(connection, data) == 0) return new SyncResult(1, "操作失败");

        //充电桩限制
        if (CSIdLimit == null) {

        }
        return new SyncResult(0, "");
    }

    /**
     * 检查用户是否拥有充电卡并且是否适用此充电设备
     *
     * @param uid        用户id
     * @param deviceCode 设备code
     * @return 同步结果
     */
    @Deprecated
    public SyncResult check(long uid, String deviceCode) {
        //检查是否拥有充电卡
        List<UserChargeCardEntity> userChargeCardList = UserChargeCardEntity.getInstance().getValidChargeCardList(uid);
        if (userChargeCardList.isEmpty()) {
            return new SyncResult(3, "您没有购买充电卡");
        }

        //region检查设备
        if (!StringUtils.hasLength(deviceCode)) return new SyncResult(2, "请选择正确的插座");

        //endregion
        //获取设备信息
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);

        UserChargeCardEntity userChargeCardEntity = null;

        for (UserChargeCardEntity nd : userChargeCardList) {
            //判断此设备是否支持该充电卡
            Map<String, Object> dcc = DeviceChargeCardItemEntity.getInstance()
                    .cache(String.format("Device:%s:ChargeCard:%s", deviceEntity.id, nd.cardConfigId))
                    .where("deviceId", deviceEntity.id)
                    .where("cardConfigId", nd.cardConfigId)
                    .find();
            if (dcc == null || dcc.isEmpty()) continue;
            userChargeCardEntity = nd;
            break;
        }
        if (userChargeCardEntity == null) return new SyncResult(1, "此设备不支持此充电卡");
        return new SyncResult(0, "", userChargeCardEntity);
    }

    /**
     * 获取用户有效的充电卡
     *
     * @param uid 用户id
     * @return 用户充电卡
     */
    @Deprecated
    public UserChargeCardEntity getValidChargeCard(long uid) {
        long nowTime = TimeUtil.getTimestamp();
        return this.field("*")
                .cache(String.format("User:%s:ChargeCard:Valid", uid), 5 * 60 * 1000)
                .where("uid", uid)
                .where("start_time", "<", nowTime)
                .where("end_time", ">", nowTime)
                .order("priority")
                .findEntity();
    }

    /**
     * 获取用户有效的充电卡
     *
     * @param uid 用户id
     * @return 有效的列表充电卡
     */
    @Deprecated
    public List<UserChargeCardEntity> getValidChargeCardList(long uid) {
        long nowTime = TimeUtil.getTimestamp();
        return this.field("*")
                .cache(String.format("User:%s:ChargeCard:ValidList", uid), 5 * 60 * 1000)
                .where("uid", uid)
                .where("start_time", "<", nowTime)
                .where("end_time", ">", nowTime)
                .order("priority")
                .selectList();
    }

    /**
     * 获取用户今日的充电时长
     * 这里涉及到最大充电功率的盈亏问题，一般情况下低于200w的充电功率日耗时间是正常的，
     * 但是当出现高于200w以上的充电功率时，日耗时间应该以倍数增长来计算
     *
     * @param uid     用户ID
     * @param onCache 是否在缓存中读取（不存在缓存也会重新计算）
     * @return 今日充电时长
     */
    @Deprecated
    public long getTodayChargeTime(long uid, boolean onCache) {
        //昨天凌晨时间戳
        long yesterday00 = TimeUtil.getTime00(-1);
        //今日凌晨时间戳
        long today00 = TimeUtil.getTime00();
        //今日结束时间戳
        long today24 = TimeUtil.getTime24();

        long totalChargeTime = 0;
        //从缓存读取
        if (onCache) {
            totalChargeTime = DataService.getMainCache().getInt(String.format("User:%s:ChargeCard:TodayChargeTime", uid), 0);
            if (totalChargeTime > 0) return totalChargeTime;
            totalChargeTime = 0;
        }

        //获取今日的充电订单
        List<ChargeOrderEntity> list = ChargeOrderEntity.getInstance()
                .where("uid", uid)
                .where("create_time", ">", yesterday00)
                .where("create_time", "<", today24)
                .where("paymentTypeId", EChargePaymentType.ChargeCard)
                .whereIn("status", "1,2")//充电中和完成的订单
                .selectList();

        DeviceEntity deviceEntity = null;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ChargeOrderEntity nd = (ChargeOrderEntity) it.next();
            if (nd.stopTime <= today00) continue;
            if (nd.status == 2) {
                //TODO 这里需要调整，新的充电功率，要消耗对应的月卡时间
                //判断特殊情况，如果是昨天充电到现在的订单则进行计算
                if (nd.startTime < today00 && (nd.stopTime > today00 && nd.stopTime < today24)) {
                    double chargePower = nd.maxPower;
                    long chargeCardConsumeTime = nd.totalChargeTime;
                    if (nd.userEbikeId != 0) chargePower = nd.limitChargePower;

                    if (deviceEntity == null || deviceEntity.deviceCode != nd.deviceCode) {
                        deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(nd.deviceCode);
                    }
                    if (deviceEntity == null || deviceEntity.id == 0) {
                        LogsUtil.error("", "查询设备数据出错,deviceCode=%s", nd.deviceCode);
                        chargeCardConsumeTime = (nd.stopTime - today00) / 1000;
                    } else {
                        double realChargeTime = (nd.stopTime - today00) / 1000;
                        chargeCardConsumeTime = Convert.toLong(realChargeTime * nd.chargeCardConsumeTimeRate);
                    }
                    totalChargeTime += chargeCardConsumeTime;
                    continue;
                }
            }
            totalChargeTime += nd.chargeCardConsumeTime;
        }

        DataService.getMainCache().set(String.format("User:%s:TodayChargeTime", uid), totalChargeTime, 5 * 60 * 1000);
        return totalChargeTime;
    }

    // endregion

    /**
     * 根据充电卡ID查询充电卡对应的配置
     *
     * @param chargeCardId 充电卡ID
     */
    public ChargeCardConfigEntity getChargeCardConfigWithId(long chargeCardId) {
        UserChargeCardEntity entity = this.cache(String.format("UserChargeCard:%s", chargeCardId)).findEntity(chargeCardId);
        if (entity == null || entity.id == 0) return null;
        return ChargeCardConfigEntity.getInstance().getConfigWithId(entity.cardConfigId);
    }

    /**
     * 通过卡号获取充电卡的配置信息
     *
     * @param cardNumber 卡号
     * @return 充电卡配置信息
     */
    public ChargeCardConfigEntity getChargeCardConfigWithCardNumber(String cardNumber) {
        UserChargeCardEntity entity = this.cache(String.format("UserChargeCard:%s", cardNumber))
                .where("cardNumber", cardNumber)
                .findEntity();
        if (entity == null || entity.id == 0) return null;
        return ChargeCardConfigEntity.getInstance().getConfigWithId(entity.cardConfigId);
    }

    /**
     * 用户购买充电卡前的检测逻辑
     * <p>
     * 1、根据ChargeCardConfigId读取配置信息，根据CSId读取ChargeStation信息
     * 2、检查is_exclude_private=1，ChargeStation的is_private=1
     * 3、检查limit_provinces=1时，检查CSId所属省份和ChargeCardConfigProvincesLimit是否可用
     * 4、检查limit_city=1时，检查CSId所属城市和ChargeCardConfigCityLimit是否可用
     * 5、检查limit_district=1时，检查CSId所属区域和ChargeCardConfigDistrictLimit是否可用
     * 6、检查limit_street=1时，检查CSId所属街道和ChargeCardConfigStreetLimit是否可用
     * 7、检查limit_communities=1时，检查CSId所属社区和ChargeCardConfigCommunitiesLimit是否可用
     * 8、新增UserChargeCard信息
     * 9、检查limit_buy_cs=1时，检查is_related_cs=1，新增UserChargeCardStationLimit数据（限制UserChargeCard充电桩使用）
     *
     * @param uid                    用户id
     * @param chargeStationEntity    充电桩
     * @param chargeCardConfigEntity 购买的充电卡配置
     * @param forceBuy               强制购买
     * @return SyncResult  检测结果
     */
    public SyncResult purchaseCheck(long uid
            , ChargeStationEntity chargeStationEntity
            , ChargeCardConfigEntity chargeCardConfigEntity
            , boolean forceBuy) {
        if (chargeStationEntity == null) {
            return new SyncResult(2, "无效充电桩");
        }

        if (chargeCardConfigEntity == null || chargeCardConfigEntity.id == 0) {
            return new SyncResult(101, "充电卡配置错误");
        }

        // 排除私有充电桩
        if (chargeCardConfigEntity.is_exclude_private == 1 && chargeStationEntity.is_private == 1) {
            return new SyncResult(102, "此充电卡不适用于本充电桩");
        }

        // 检查此充电桩是否不允许此类充电卡配置
//        if (!ChargeStationToChargeCardLimitEntity.getInstance().isAllow(chargeStationEntity.CSId, chargeCardConfigEntity.id, false)) {
//            return new SyncResult(102, "此充电卡不适用于本充电桩");
//        }

        // 检查此充电桩是否不允许此类充电卡配置
        if (!ChargeCardConfigChargeStationLimitEntity.getInstance().isAllow(chargeStationEntity.CSId, chargeCardConfigEntity.id)) {
            return new SyncResult(102, "此充电卡不适用于本充电桩");
        }

        // 强制购买，主要应用于用户愿意购买相同类型的充电卡
        if (!forceBuy && chargeCardConfigEntity.limit_buy_station == 1) {
            //region 检查用户是否已有对应站点的充电卡
            try {
                long nowTime = TimeUtil.getTimestamp();
                List<UserChargeCardStationLimitEntity> userChargeCardStationLimitEntityList = UserChargeCardStationLimitEntity.getInstance()
                        .field("a.*")
                        .alias("a")
                        .join(UserChargeCardEntity.getInstance().theTableName(), "b", "a.cardNumber = b.cardNumber")
                        .where("b.uid", uid)
                        .where("b.start_time", "<", nowTime)
                        .where("b.end_time", ">", nowTime)
                        .addWhere("AND FIND_IN_SET(?, a.allow_cs_ids) > 0", chargeStationEntity.CSId)
                        .selectList();
                if (userChargeCardStationLimitEntityList != null && !userChargeCardStationLimitEntityList.isEmpty()) {
                    for (UserChargeCardStationLimitEntity stationLimitEntity : userChargeCardStationLimitEntityList) {
                        boolean allow = isCSIdAllowed(stationLimitEntity.allow_cs_ids, chargeStationEntity.CSId);
                        if (allow) return new SyncResult(103, "您已有对应可用充电卡");
                    }
                }
            } catch (Exception e) {
                LogsUtil.error(e, this.getClass().getSimpleName(), "检查用户是否已有对应站点的充电卡 - 发生错误");
            }
            //endregion
        }

        // 检查是否有地区限制
        SyncResult regionalLimitResult = checkRegionalLimit(chargeCardConfigEntity, chargeStationEntity);
        if (regionalLimitResult.code != 0) {
            return regionalLimitResult;
        }

        // 所有检测通过
        return new SyncResult(0, "检测通过，可以进行购买");
    }

    /**
     * 购买回调逻辑处理 - 一般购买前先调用 purchaseCheck 进行检测是否可以购买
     *
     * @param uid                用户id
     * @param CSId               充电桩ID
     * @param chargeCardConfigId 充电卡配置Id
     * @param OrderSN            订单号
     * @param cardNumber         (可选)卡号码，如果空值会自动生成
     * @return 是否购买成功
     */
    public SyncResult purchaseCallback(long uid
            , String CSId
            , long chargeCardConfigId
            , String OrderSN
            , String cardNumber
    ) {
        // 根据CSId读取ChargeStation信息
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId, false);
        if (chargeStationEntity == null) {
            return new SyncResult(2, "无效充电桩");
        }

        // 根据ChargeCardConfigId读取配置信息
        ChargeCardConfigEntity chargeCardConfigEntity = ChargeCardConfigEntity.getInstance().getConfigWithId(chargeCardConfigId);
        if (chargeCardConfigEntity == null || chargeCardConfigEntity.id == 0) {
            return new SyncResult(101, "充电卡配置错误");
        }
        return purchaseCallback(uid, chargeStationEntity, chargeCardConfigEntity, OrderSN, cardNumber);
    }

    /**
     * 购买回调逻辑处理 - 一般购买前先调用 purchaseCheck 进行检测是否可以购买
     *
     * @param uid                    用户id
     * @param chargeStationEntity    充电桩
     * @param chargeCardConfigEntity 充电卡配置
     * @param OrderSN                订单号
     * @param cardNumber             (可选)卡号码，如果空值会自动生成
     * @return 是否购买成功
     */
    public SyncResult purchaseCallback(long uid
            , ChargeStationEntity chargeStationEntity
            , ChargeCardConfigEntity chargeCardConfigEntity
            , String OrderSN
            , String cardNumber
    ) {
        long start_time = TimeUtil.getTime00();
        long end_time = start_time;
        //region 处理生效时间
        switch (chargeCardConfigEntity.typeId) {
            case 1://日
                if (chargeCardConfigEntity.countValue == 1) {
                    start_time = TimeUtil.getTimestamp();
                    //如果只是一天卡，结束时间应该以当前时间的第二天作为结束时间，这样体验效果会好一些
                    end_time = TimeUtil.getTimestamp();
                }
                end_time += chargeCardConfigEntity.countValue * 86400000L;
                break;
            case 2://月
                end_time = TimeUtil.getAddMonthTimestamp(end_time, chargeCardConfigEntity.countValue);
                break;
            case 3://年
                end_time = TimeUtil.getAddMonthTimestamp(end_time, chargeCardConfigEntity.countValue * 12);
                break;
            default:
                return new SyncResult(101, "错误的配置类型");
        }
        //endregion

        //region 生成卡号
        if (!StringUtils.hasLength(cardNumber)) {
            cardNumber = cardNumber
                    + chargeCardConfigEntity.typeId
                    + TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                    + common.randomInt(1000, 9999);
        }
        //endregion

        String finalCardNumber = cardNumber;
        long finalStart_time = start_time;
        long finalEnd_time = end_time;
        return this.beginTransaction(connection -> {
            String describe = "";
            //新增UserChargeCard信息
            this.uid = uid;
            this.cardName = chargeCardConfigEntity.cardName;
            this.cardNumber = finalCardNumber;
            this.cardConfigId = chargeCardConfigEntity.id;
            this.spu_code = chargeCardConfigEntity.spu_code;
            this.cardTypeId = chargeCardConfigEntity.cardTypeId;
            this.describe = describe;
            this.status = 1;
            this.priority = chargeCardConfigEntity.priority;
            this.start_time = finalStart_time;
            this.end_time = finalEnd_time;
            this.OrderSN = OrderSN;
            this.create_time = TimeUtil.getTimestamp();
            this.id = this.insertGetIdTransaction(connection);
            if (this.id == 0) return new SyncResult(1, "操作失败");

            //region 新增用户充电卡限制信息：检查limit_buy_cs=1时，检查is_related_cs=1，新增UserChargeCardStationLimit数据（限制UserChargeCard充电桩使用）
            if (chargeCardConfigEntity.limit_buy_station == 1) {
                String allow_cs_ids = chargeStationEntity.CSId;
                if (chargeCardConfigEntity.is_related_cs == 1 && StringUtils.hasLength(chargeStationEntity.group_cs_id)) {
                    List<Map<String, Object>> allow_cs = ChargeStationEntity.getInstance()
                            .field("CSId,name")
                            .where("group_cs_id", chargeStationEntity.group_cs_id)
                            .select();
                    if (allow_cs != null && !allow_cs.isEmpty()) {
                        allow_cs_ids = "";
                        String allow_describe = "";
                        for (Map<String, Object> nd : allow_cs) {
                            allow_cs_ids += String.format(",%s", MapUtil.getString(nd, "CSId"));
                            allow_describe += String.format("、%s", MapUtil.getString(nd, "name"));
                        }
                        if (StringUtils.hasLength(allow_cs_ids)) allow_cs_ids = allow_cs_ids.substring(1);
                        if (StringUtils.hasLength(allow_describe)) {
                            describe += String.format("适用充电桩：%s", allow_describe.substring(1));
                        }
                    }
                } else {
                    describe += String.format("适用充电桩：%s", chargeStationEntity.name);
                }
                UserChargeCardStationLimitEntity stationLimitEntity = new UserChargeCardStationLimitEntity();
                stationLimitEntity.uid = uid;
                stationLimitEntity.cardNumber = finalCardNumber;
                stationLimitEntity.allow_cs_ids = allow_cs_ids;
                stationLimitEntity.insertTransaction(connection);
            }
            //endregion

            String regionalLimitDescribe = buildRegionalLimitDescribe(chargeCardConfigEntity, chargeStationEntity);
            if (StringUtils.hasLength(regionalLimitDescribe)) describe += regionalLimitDescribe;

            if (!StringUtils.hasLength(describe)) {
                describe = chargeCardConfigEntity.describe;
            }

            String finalDescribe = describe;
            this.updateTransaction(connection, this.id, new LinkedHashMap<>() {{
                put("describe", finalDescribe);
            }});

            return new SyncResult(0, "");
        });
    }

    /**
     * 检查充电卡是否可在对用充电桩中使用 - 2023-12-07 逻辑处理
     *
     * @param CSId       充电桩ID
     * @param cardNumber 充电卡
     */
    public SyncResult checkChargeCardForStation(String CSId, String cardNumber) {
        //根据CSId读取ChargeStation信息
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
        if (chargeStationEntity == null) return new SyncResult(2, "无效充电桩");

        //读取充电卡信息
        UserChargeCardEntity chargeCardEntity = UserChargeCardEntity.getInstance()
                .where("cardNumber", cardNumber)
                .findEntity();
        if (chargeCardEntity == null) return new SyncResult(2, "无效充电卡");

        return checkChargeCardForStation(chargeStationEntity, chargeCardEntity);
    }

    /**
     * 检查充电卡是否可在对用充电桩中使用 - 2023-12-07 逻辑处理
     * 1、根据CSId读取ChargeStation信息
     * 2、根据cardNumber查询信息，检查过期时间
     * 3、查询ChargeCardConfig信息
     * 4、检查UserChargeCardStationLimit是否可用
     * 5、检查is_exclude_private=1，ChargeStation的is_private=1
     * 6、检查limit_provinces=1时，检查CSId所属省份和ChargeCardConfigProvincesLimit是否可用
     * 7、检查limit_city=1时，检查CSId所属城市和ChargeCardConfigCityLimit是否可用
     * 8、检查limit_district=1时，检查CSId所属区域和ChargeCardConfigDistrictLimit是否可用
     * 9、检查limit_street=1时，检查CSId所属街道和ChargeCardConfigStreetLimit是否可用
     * 10、检查limit_communities=1时，检查CSId所属社区和ChargeCardConfigCommunitiesLimit是否可用
     *
     * @param chargeStationEntity 充电桩实体类
     * @param chargeCardEntity    充电卡实体类
     */
    public SyncResult checkChargeCardForStation(ChargeStationEntity chargeStationEntity, UserChargeCardEntity chargeCardEntity) {
        //根据CSId读取ChargeStation信息
        if (chargeStationEntity == null) return new SyncResult(2, "无效充电桩");

        //读取充电卡信息
        if (chargeCardEntity == null) return new SyncResult(2, "无效充电卡");

        //检查是否还在生效
        long nowTime = TimeUtil.getTimestamp();
        if (chargeCardEntity.start_time > nowTime || chargeCardEntity.end_time < nowTime) {
            return new SyncResult(1, "充电卡已失效");
        }

        //根据ChargeCardConfigId读取配置信息
        ChargeCardConfigEntity configEntity = ChargeCardConfigEntity.getInstance().getConfigWithId(chargeCardEntity.cardConfigId);
        if (configEntity == null || configEntity.id == 0) return new SyncResult(101, "充电卡配置错误");

        //排除私有
        if (configEntity.is_exclude_private == 1 && chargeStationEntity.is_private == 1) {
            return new SyncResult(102, "此充电卡不适用于本充电桩");
        }

        //检查此充电桩是否不允许此类充电卡配置
//        if (!ChargeStationToChargeCardLimitEntity.getInstance().isAllow(chargeStationEntity.CSId, configEntity.id, false)) {
//            return new SyncResult(102, "此充电卡不适用于本充电桩");
//        }

        //检查此充电桩是否不允许此类充电卡配置
        if (!ChargeCardConfigChargeStationLimitEntity.getInstance().isAllow(chargeStationEntity.CSId, configEntity.id)) {
            return new SyncResult(102, "此充电卡不适用于本充电桩");
        }

        //检查是否有地区限制
        SyncResult regionalLimitResult = checkRegionalLimit(configEntity, chargeStationEntity);
        if (regionalLimitResult.code != 0) {
            return new SyncResult(102, "此充电卡不适用于本充电桩");
        }

        //检查UserChargeCardStationLimit是否可用
        UserChargeCardStationLimitEntity stationLimitEntity = UserChargeCardStationLimitEntity.getInstance().getWithCardNumber(chargeCardEntity.cardNumber);
        if (stationLimitEntity != null) {
            if (!isCSIdAllowed(stationLimitEntity.allow_cs_ids, chargeStationEntity.CSId)) {
                return new SyncResult(102, "此充电卡不适用于本充电桩");
            }
        }
        return new SyncResult(0, "");
    }

    /**
     * 更新 充电卡 绑定的站点
     *
     * @param cardNumber       充电卡卡号
     * @param allow_CSId_Array 允许使用的充电桩ID列表
     */
    public SyncResult updateChargeStationLimit(String cardNumber, String[] allow_CSId_Array) {
        //查询充电卡信息
        UserChargeCardEntity userChargeCardEntity = UserChargeCardEntity.getInstance()
                .where("cardNumber", cardNumber)
                .findEntity();
        if (userChargeCardEntity == null) return new SyncResult(13, "无效充电卡");

        //查询充电卡配置信息
        ChargeCardConfigEntity chargeCardConfigEntity = ChargeCardConfigEntity.getInstance().getConfigWithId(userChargeCardEntity.cardConfigId);
        if (chargeCardConfigEntity == null) return new SyncResult(14, "无效充电卡配置");

        //删除缓存
        DataService.getMainCache().del(String.format("User:ChargeCard:StationLimit:%s", cardNumber));

        String describe = "";
        String allow_cs_ids = "";
        //region 新增用户充电卡限制信息：检查limit_buy_cs=1时，检查is_related_cs=1，新增UserChargeCardStationLimit数据（限制UserChargeCard充电桩使用）
        List<Map<String, Object>> allow_cs = ChargeStationEntity.getInstance()
                .field("CSId,name")
                .whereIn("CSId", allow_CSId_Array)
                .select();
        if (allow_cs != null && allow_cs.size() > 0) {
            String allow_describe = "";
            for (Map<String, Object> nd : allow_cs) {
                allow_cs_ids += String.format(",%s", MapUtil.getString(nd, "CSId"));
                allow_describe += String.format("、%s", MapUtil.getString(nd, "name"));
            }
            if (StringUtils.hasLength(allow_cs_ids)) allow_cs_ids = allow_cs_ids.substring(1);
            if (StringUtils.hasLength(allow_describe)) {
                describe += String.format("适用充电桩：%s", allow_describe.substring(1));
            }
        }

        if (!StringUtils.hasLength(allow_cs_ids)) return new SyncResult(15, "无效充电桩站点");

        String finalAllow_cs_ids = allow_cs_ids;
        String finalDescribe = describe;

        //开启事务执行
        return beginTransaction(connection -> {
            UserChargeCardStationLimitEntity stationLimitEntity = UserChargeCardStationLimitEntity.getInstance()
                    .where("cardNumber", cardNumber)
                    .findEntity();
            if (stationLimitEntity == null) {
                stationLimitEntity = new UserChargeCardStationLimitEntity();
                stationLimitEntity.uid = uid;
                stationLimitEntity.cardNumber = cardNumber;
                stationLimitEntity.allow_cs_ids = finalAllow_cs_ids;
                if (stationLimitEntity.insertTransaction(connection) == 0) return new SyncResult(15, "绑定新站点失败");
            } else {
                if (stationLimitEntity.updateTransaction(connection, stationLimitEntity.id, new LinkedHashMap<>() {{
                    put("allow_cs_ids", finalAllow_cs_ids);
                }}) == 0) {
                    return new SyncResult(16, "绑定新站点失败");
                }
            }
            //endregion

            userChargeCardEntity.updateTransaction(connection, userChargeCardEntity.id, new LinkedHashMap<>() {{
                put("describe", finalDescribe);
            }});
            return new SyncResult(0, "");
        });
    }

    /**
     * 获取用户有效充电卡的数量
     *
     * @param uid 用户id
     */
    public int getCardCount(long uid) {
        return getCardCount(uid, "", true);
    }

    /**
     * 获取用户有效充电卡的数量
     *
     * @param uid        用户id
     * @param cardTypeId 卡类型：1-数字充电卡，2-NFC-ID实体卡
     */
    public int getCardCount(long uid, String cardTypeId) {
        return getCardCount(uid, cardTypeId, true);
    }

    /**
     * 获取用户有效充电卡的数量
     *
     * @param uid        用户id
     * @param cardTypeId 卡类型：1-数字充电卡，2-NFC-ID实体卡
     * @param inCache    是否优先充缓存中获得
     */
    public int getCardCount(long uid, String cardTypeId, boolean inCache) {
        if (uid == 0) return 0;

        long nowTime = TimeUtil.getTimestamp();
        UserChargeCardEntity userChargeCardEntity = UserChargeCardEntity.getInstance();
        userChargeCardEntity.where("uid", uid)
                .where("start_time", "<", nowTime)
                .where("end_time", ">", nowTime);

        if (StringUtils.hasLength(cardTypeId)) userChargeCardEntity.whereIn("cardTypeId", cardTypeId);
        if (inCache) userChargeCardEntity.cache(String.format("User:ChargeCard:count:%s", uid), 5 * ECacheTime.MINUTE);

        return userChargeCardEntity.count();
    }

    /**
     * 查询用户有效的充电卡
     *
     * @param uid 用户id
     * @return 充电卡列表
     */
    public List<Map<String, Object>> getCardList(long uid) {
        return getCardList(uid, "");
    }

    /**
     * 查询用户有效的充电卡
     * 1、根据CSId读取ChargeStation信息
     * 2、通过uid、start_time、end_time查询还在生效的充电卡
     * 3、再通过checkStationAllowChargeCard逐一检查
     *
     * @param uid  用户id
     * @param CSId 充电桩ID
     * @return 充电卡列表
     */
    public List<Map<String, Object>> getCardList(long uid, String CSId) {
        return getCardList(uid, CSId, "");
    }

    /**
     * 查询用户有效的充电卡
     * 1、根据CSId读取ChargeStation信息
     * 2、通过uid、start_time、end_time查询还在生效的充电卡
     * 3、再通过checkStationAllowChargeCard逐一检查
     *
     * @param uid        用户id
     * @param CSId       充电桩ID
     * @param cardTypeId 卡类型：1-数字充电卡，2-NFC-ID实体卡
     * @return 充电卡列表
     */
    public List<Map<String, Object>> getCardList(long uid, String CSId, String cardTypeId) {
        long nowTime = TimeUtil.getTimestamp();

        UserChargeCardEntity userChargeCardEntity = UserChargeCardEntity.getInstance();
        userChargeCardEntity.where("uid", uid)
                .where("status", 1)
                .where("start_time", "<", nowTime)
                .where("end_time", ">", nowTime);

        if (StringUtils.hasLength(cardTypeId)) {
            userChargeCardEntity.whereIn("cardTypeId", cardTypeId);
        }

        List<UserChargeCardEntity> cardList = userChargeCardEntity.selectList();
        if (cardList == null || cardList.isEmpty()) return new LinkedList<>();

        ChargeStationEntity chargeStationEntity = null;
        if (StringUtils.hasLength(CSId) && !"0".equals(CSId)) {
            chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
            if (chargeStationEntity == null) return new LinkedList<>();
        }

        List<Map<String, Object>> allowList = new LinkedList<>();
        List<Map<String, Object>> notAllowList = new LinkedList<>();
        ChargeCardConfigEntity configEntity = null;

        for (UserChargeCardEntity card : cardList) {
            Map<String, Object> nd = new LinkedHashMap<>();
            if (configEntity == null || configEntity.id != card.cardConfigId) {
                configEntity = ChargeCardConfigEntity.getInstance().getConfigWithId(card.cardConfigId);
            }

            nd.put("coverImage", String.format("%s", configEntity.coverImage));
            nd.put("tags", configEntity.tags);
            nd.put("cardNumber", card.cardNumber);
            nd.put("cardName", card.cardName);
            nd.put("subtitle", String.format("%s", configEntity.subtitle));
            nd.put("describe", card.describe);
            nd.put("start_time", card.start_time);
            nd.put("end_time", card.end_time);
            nd.put("dailyChargeTime", configEntity.dailyChargeTime);

            if (chargeStationEntity != null) {
                SyncResult r = checkChargeCardForStation(chargeStationEntity, card);
                nd.put("allow", r.code == 0 ? 1 : 0);
                if (r.code == 0) {
                    allowList.add(nd);
                } else {
                    notAllowList.add(nd);
                }
            } else {
                nd.put("allow", 1);
                allowList.add(nd); // 如果不检查充电桩兼容性，所有卡都视为允许
            }
        }

        // 先添加允许的充电卡，然后添加不允许的充电卡
        List<Map<String, Object>> sortedList = new LinkedList<>();
        sortedList.addAll(allowList);
        sortedList.addAll(notAllowList);
        return sortedList;
    }


    /**
     * 获取用户过期的充电卡
     */
    public List<Map<String, Object>> getExpiredCardList(long uid) {
        long nowTime = TimeUtil.getTimestamp();

        List<UserChargeCardEntity> cardList = UserChargeCardEntity.getInstance()
                .where("uid", uid)
                .where("cardTypeId", 1)
                .where("end_time", "<", nowTime)
                .selectList();
        if (cardList == null || cardList.size() == 0) return new LinkedList<>();

        List<Map<String, Object>> list = new LinkedList<>();
        ChargeCardConfigEntity configEntity = null;

        for (UserChargeCardEntity card : cardList) {
            Map<String, Object> nd = new LinkedHashMap<>();
            if (configEntity == null || configEntity.id != card.cardConfigId) {
                configEntity = ChargeCardConfigEntity.getInstance().getConfigWithId(card.cardConfigId);
            }

            nd.put("coverImage", String.format("%s", configEntity.coverImage));
            nd.put("cardNumber", card.cardNumber);
            nd.put("cardName", card.cardName);
            nd.put("subtitle", String.format("%s", configEntity.subtitle));
            nd.put("describe", card.describe);
            nd.put("start_time", card.start_time);
            nd.put("end_time", card.end_time);

            list.add(nd); // 如果不检查充电桩兼容性，所有卡都视为允许
        }
        return list;
    }


    /**
     * 获取充电卡当日的充电时间，单位秒
     */
    public long getTodayChargeTime(UserChargeCardEntity userChargeCardEntity, boolean inCache) {
        long yesterday00 = TimeUtil.getTime00(-1);
        long today00 = TimeUtil.getTime00();
        long today24 = TimeUtil.getTime24();

        if (inCache) {
            long cachedTime = DataService.getMainCache().getInt(String.format("User:%s:ChargeCard:%s:TodayChargeTime"
                    , userChargeCardEntity.uid
                    , userChargeCardEntity.cardNumber
            ), 0);
            if (cachedTime > 0) return cachedTime;
        }

        long totalChargeTime = 0;
        //读取昨天到今天的充电卡充电订单
        List<ChargeOrderEntity> list = ChargeOrderEntity.getInstance()
                .where("cardNumber", userChargeCardEntity.cardNumber)
                .where("create_time", ">", yesterday00)
                .where("create_time", "<", today24)
                .where("paymentTypeId", EChargePaymentType.ChargeCard.index)
                .whereIn("status", "1,2")
                .selectList();

        Iterator it = list.iterator();
        while (it.hasNext()) {
            ChargeOrderEntity orderEntity = (ChargeOrderEntity) it.next();
            //停止时间如果是小于今日凌晨的表示是昨天的订单，应该忽略不计
            if (orderEntity.stopTime <= today00) continue;
            //默认充电卡消耗时间
            long chargeCardConsumeTime = orderEntity.chargeCardConsumeTime;
            if (orderEntity.status == 2) {
                //判断跨天充电：开始充电是昨天开始，今天结束的
                if (orderEntity.startTime < today00) {
                    double realChargeTime = Convert.toDouble(orderEntity.stopTime - today00) / 1000;
                    chargeCardConsumeTime = Convert.toLong(realChargeTime * orderEntity.chargeCardConsumeTimeRate);
                }
            }
            totalChargeTime += chargeCardConsumeTime;
        }

        DataService.getMainCache().set(String.format("User:%s:ChargeCard:%s:TodayChargeTime"
                , userChargeCardEntity.uid
                , userChargeCardEntity.cardNumber
        ), totalChargeTime, ECacheTime.MINUTE * 5);
        return totalChargeTime;
    }

    /**
     * 获取充电卡剩余充电时间,单位秒
     */
    public long getTodayCardTimeBalance(UserChargeCardEntity userChargeCardEntity, boolean inCache) {
        ChargeCardConfigEntity configEntity = ChargeCardConfigEntity.getInstance().getConfigWithId(userChargeCardEntity.cardConfigId);
        if (configEntity == null || configEntity.id == 0) return 0;

        //今日充电时间
        long todayChargeTime = userChargeCardEntity.getTodayChargeTime(userChargeCardEntity, inCache);
        //实际剩余充电时间
        return configEntity.dailyChargeTime - todayChargeTime;
    }

    /**
     * 通过卡号获取充电卡信息
     *
     * @param cardNumber 卡号
     * @return 充点卡信息
     */
    public UserChargeCardEntity getCardWithNumber(String cardNumber) {
        return getCardWithNumber(cardNumber, true);
    }

    /**
     * 通过卡号获取充电卡信息
     *
     * @param cardNumber 卡号
     * @param inCache    是否优先从缓存中获取
     * @return 充点卡信息
     */
    public UserChargeCardEntity getCardWithNumber(String cardNumber, boolean inCache) {
        if (!StringUtils.hasLength(cardNumber)) return null;
        if (inCache) this.cache(String.format("ChargeCard:%s", cardNumber));
        this.where("cardNumber", cardNumber);
        return this.findEntity();
    }


    /**
     * 辅助函数 - 检查充电卡配置是否适用于特定充电站的地区。
     *
     * @param configEntity        充电卡配置实体
     * @param chargeStationEntity 充电站实体
     * @return SyncResult 检查结果
     */
    private static SyncResult checkRegionalLimit(ChargeCardConfigEntity configEntity, ChargeStationEntity chargeStationEntity) {
        // 检查省份限制
        if (configEntity.limit_provinces == 1 && !ChargeCardConfigProvincesLimitEntity.getInstance()
                .where("config_id", configEntity.id)
                .where("province", chargeStationEntity.province)
                .exist()) {
            return new SyncResult(103, String.format("此充电卡不适用于[%s]", chargeStationEntity.province));
        }

        // 检查城市限制
        if (configEntity.limit_city == 1 && !ChargeCardConfigCityLimitEntity.getInstance()
                .where("config_id", configEntity.id)
                .where("city", chargeStationEntity.city)
                .exist()) {
            return new SyncResult(103, String.format("此充电卡不适用于[%s]", chargeStationEntity.city));
        }

        // 检查区域限制
        if (configEntity.limit_district == 1 && !ChargeCardConfigDistrictLimitEntity.getInstance()
                .where("config_id", configEntity.id)
                .where("district", chargeStationEntity.district)
                .exist()) {
            return new SyncResult(103, String.format("此充电卡不适用于[%s]", chargeStationEntity.district));
        }

        // 检查街道限制
        if (configEntity.limit_street == 1 && !ChargeCardConfigStreetLimitEntity.getInstance()
                .where("config_id", configEntity.id)
                .where("code", chargeStationEntity.street_code)
                .exist()) {
            return new SyncResult(103, String.format("此充电卡不适用于[%s]", chargeStationEntity.street));
        }

        // 检查社区限制
        if (configEntity.limit_communities == 1 && !ChargeCardConfigCommunitiesLimitEntity.getInstance()
                .where("config_id", configEntity.id)
                .where("communities", chargeStationEntity.communities)
                .exist()) {
            return new SyncResult(103, String.format("此充电卡不适用于[%s]", chargeStationEntity.communities));
        }
        return new SyncResult(0, "");
    }

    /**
     * 辅助函数：组装区域限制说明
     *
     * @param configEntity        充电卡配置
     * @param chargeStationEntity 充电桩
     */
    private static String buildRegionalLimitDescribe(ChargeCardConfigEntity configEntity, ChargeStationEntity chargeStationEntity) {
        String describe = "";
        // 检查省份限制
        if (configEntity.limit_provinces == 1) {
            List<ChargeCardConfigProvincesLimitEntity> list = ChargeCardConfigProvincesLimitEntity.getInstance()
                    .where("config_id", configEntity.id)
                    .selectList();
            String desc = "";
            for (ChargeCardConfigProvincesLimitEntity nd : list) {
                desc = String.format("%s、", nd.province);
            }
            if (StringUtils.hasLength(desc)) {
                desc = desc.substring(0, desc.length() - 1);
                describe += String.format("适用省份：%s", desc);
            }
        }

        // 检查城市限制
        if (configEntity.limit_city == 1) {
            List<ChargeCardConfigCityLimitEntity> list = ChargeCardConfigCityLimitEntity.getInstance()
                    .where("config_id", configEntity.id)
                    .selectList();
            String desc = "";
            for (ChargeCardConfigCityLimitEntity nd : list) {
                desc = String.format("%s、", nd.city);
            }
            if (StringUtils.hasLength(desc)) {
                desc = desc.substring(0, desc.length() - 1);
                describe += String.format("适用城市：%s", desc);
            }
        }

        // 检查区域限制
        if (configEntity.limit_district == 1) {
            List<ChargeCardConfigDistrictLimitEntity> list = ChargeCardConfigDistrictLimitEntity.getInstance()
                    .where("config_id", configEntity.id)
                    .selectList();
            String desc = "";
            for (ChargeCardConfigDistrictLimitEntity nd : list) {
                desc = String.format("%s、", nd.district);
            }
            if (StringUtils.hasLength(desc)) {
                desc = desc.substring(0, desc.length() - 1);
                describe += String.format("适用区域：%s", desc);
            }
        }

        // 检查街道限制
        if (configEntity.limit_street == 1) {
            List<ChargeCardConfigStreetLimitEntity> list = ChargeCardConfigStreetLimitEntity.getInstance()
                    .where("config_id", configEntity.id)
                    .selectList();
            String desc = "";
            for (ChargeCardConfigStreetLimitEntity nd : list) {
                desc = String.format("%s、", nd.street);
            }
            if (StringUtils.hasLength(desc)) {
                desc = desc.substring(0, desc.length() - 1);
                describe += String.format("适用街道：%s", desc);
            }
        }

        // 检查社区限制
        if (configEntity.limit_communities == 1) {
            List<ChargeCardConfigCommunitiesLimitEntity> list = ChargeCardConfigCommunitiesLimitEntity.getInstance()
                    .where("config_id", configEntity.id)
                    .selectList();
            String desc = "";
            for (ChargeCardConfigCommunitiesLimitEntity nd : list) {
                desc = String.format("%s、", nd.communities);
            }
            if (StringUtils.hasLength(desc)) {
                desc = desc.substring(0, desc.length() - 1);
                describe += String.format("适用社区：%s", desc);
            }
        }
        return describe;
    }

    /**
     * 辅助函数 - 检查
     *
     * @param allow_cs_ids 允许的充电桩，空值或All表示所有都可以使用，如果存在太多的数据，可以新增多一条进行限制
     * @param CSId         充电桩ID
     */
    private static boolean isCSIdAllowed(String allow_cs_ids, String CSId) {
        if (!StringUtils.hasLength(allow_cs_ids)) return true;
        if ("ALL".equalsIgnoreCase(allow_cs_ids)) return true;
        // 将 allowCSIds 字符串按逗号分割
        String[] allowedIds = allow_cs_ids.split(",");
        // 遍历数组并检查是否包含 csId
        for (String allowedId : allowedIds) {
            if (allowedId.trim().equalsIgnoreCase(CSId)) return true;
        }
        return false;
    }
}
