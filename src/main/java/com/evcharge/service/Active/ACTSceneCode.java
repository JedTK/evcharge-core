package com.evcharge.service.Active;

import com.xyzs.utils.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 活动触发场景枚举
 * <p>
 * 设计原则：
 * 1、code 为“落库值/协议值”，一旦发布尽量不要改（改了会导致旧配置失效）
 * 2、枚举名用于代码可读性，支持 IDE 自动补全
 * 3、后续新增场景只允许追加，不建议删除（删除会影响历史配置/日志回溯）
 */
public enum ACTSceneCode {

    // 充电链路
    CHARGE_ORDER_CREATE("CHARGE_ORDER_CREATE", "创建充电订单"),
    CHARGE_START("CHARGE_START", "充电开始"),
    CHARGE_FINISH("CHARGE_FINISH", "充电结算完成"),
    CHARGE_FINISH_FAIL("CHARGE_FINISH_FAIL", "充电结算失败（需人工/系统重试）"),

    // 充值/余额链路
    RECHARGE_CREATE("RECHARGE_CREATE", "创建充值订单"),
    RECHARGE_PAID("RECHARGE_PAID", "充值支付成功回调"),
    RECHARGE_PAY_FAIL("RECHARGE_PAY_FAIL", "充值支付失败回调"),
    RECHARGE_REFUND("RECHARGE_REFUND", "充值退款成功回调"),
    BALANCE_CHANGE("BALANCE_CHANGE", "账户余额变更（入账/扣款）"),
    COUPON_GRANTED("COUPON_GRANTED", "发放优惠券完成"),
    COUPON_USED("COUPON_USED", "优惠券核销完成"),

    // 退款/售后链路
    REFUND_APPLY("REFUND_APPLY", "发起退款申请"),
    REFUND_AUDIT_PASS("REFUND_AUDIT_PASS", "退款审核通过"),
    REFUND_AUDIT_REJECT("REFUND_AUDIT_REJECT", "退款审核拒绝"),
    REFUND_SUCCESS("REFUND_SUCCESS", "退款成功回调"),
    REFUND_FAIL("REFUND_FAIL", "退款失败回调"),

    // 用户链路
    USER_REGISTER("USER_REGISTER", "用户注册成功"),
    USER_LOGIN("USER_LOGIN", "用户登录成功"),
    USER_FIRST_LOGIN("USER_FIRST_LOGIN", "用户首次登录"),
    USER_LOGOUT("USER_LOGOUT", "用户退出登录"),
    USER_BIND_PHONE("USER_BIND_PHONE", "用户绑定手机号"),
    USER_VERIFY_SUCCESS("USER_VERIFY_SUCCESS", "用户实名认证通过"),
    USER_VERIFY_FAIL("USER_VERIFY_FAIL", "用户实名认证失败"),

    // 页面与行为（通用场景，细分靠 params）
    PAGE_ENTER("PAGE_ENTER", "进入页面（params.page_code）"),
    BUTTON_CLICK("BUTTON_CLICK", "点击按钮（params.button_code）"),
    BANNER_CLICK("BANNER_CLICK", "点击运营位（params.banner_code）"),


    UNKNOWN("UNKNOWN", "未知场景"); // 用于结束枚举而已

    private final String code;
    private final String title;

    ACTSceneCode(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    // 反查：用于入参校验 / 兼容外部字符串输入
    private static final Map<String, ACTSceneCode> BY_CODE = new HashMap<>();

    static {
        for (ACTSceneCode v : values()) BY_CODE.put(v.code, v);
    }

    public static ACTSceneCode ofCode(String code) {
        if (StringUtil.isEmpty(code)) return null;
        return BY_CODE.get(code);
    }

    public static boolean isValid(String code) {
        return ofCode(code) != null;
    }
}
