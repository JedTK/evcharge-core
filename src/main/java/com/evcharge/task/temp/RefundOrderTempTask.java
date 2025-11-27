package com.evcharge.task.temp;

import com.evcharge.entity.chargecard.UserChargeCardOrderEntity;
import com.evcharge.entity.chargecard.UserChargeCardRefundOrderEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.recharge.RechargeOrderEntity;
import com.evcharge.entity.recharge.RechargeRefundOrderEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值退款订单临时任务
 */
public class RefundOrderTempTask {
    private final static String TAG = "退款订单修复任务";

    public static RefundOrderTempTask getInstance() {
        return new RefundOrderTempTask();
    }

    /**
     * 修复充值退款订单
     */
    public void repairRechargeRefundOrder(long start_time, long end_time) {
        if (start_time == 0) start_time = TimeUtil.getTime00(-2);
        if (end_time == 0) end_time = TimeUtil.getTime24(0);

        int page = 1;
        int limit = 100;
        long pages;
        long total_count = RechargeRefundOrderEntity.getInstance()
                .where("refund_OrderSN", "")
                .where("create_time", ">=", start_time)
                .where("create_time", "<=", end_time)
                .countGetLong("1");
        if (total_count == 0) {
            LogsUtil.warn(TAG, "%s ~ %s 已无可修复的退款订单"
                    , TimeUtil.toShortTimeString(start_time)
                    , TimeUtil.toShortTimeString(end_time)
            );
            return;
        }
        pages = Convert.toInt(Math.ceil(total_count * 1.0 / limit));

        while (page <= pages) {
            List<Map<String, Object>> list = RechargeRefundOrderEntity.getInstance()
                    .field("id,refund_OrderSN,create_time")
                    .where("refund_OrderSN", "")
                    .where("create_time", ">=", start_time)
                    .where("create_time", "<=", end_time)
                    .select();
            if (list == null || list.isEmpty()) break;

            LogsUtil.info(TAG, "%s ~ %s 修复进度：%s/%s - 开始修复"
                    , TimeUtil.toShortTimeString(start_time)
                    , TimeUtil.toShortTimeString(end_time)
                    , page
                    , pages
            );

            for (Map<String, Object> map : list) {
                long id = MapUtil.getLong(map, "id");

                updateRechargeRefundOrderJob(map);

                LogsUtil.info(TAG, "%s ~ %s 修复进度：%s/%s - %s 已经修复"
                        , TimeUtil.toShortTimeString(start_time)
                        , TimeUtil.toShortTimeString(end_time)
                        , page
                        , pages
                        , id
                );
            }

            page++;
        }

        LogsUtil.info(TAG, "%s ~ %s 已完成修复退款订单"
                , TimeUtil.toShortTimeString(start_time)
                , TimeUtil.toShortTimeString(end_time)
        );
    }

    /**
     * 修复充值退款订单具体执行
     *
     * @param data
     */
    private void updateRechargeRefundOrderJob(Map<String, Object> data) {
        long id = MapUtil.getLong(data, "id");
        long create_time = MapUtil.getLong(data, "create_time");

        String refund_OrderSN = String.format("FU%s%s", TimeUtil.toTimeString(create_time, "yyyyMMddHHmmss"), common.randomStr(4));

        Map<String, Object> set_data = new HashMap<>();
        set_data.put("refund_OrderSN", refund_OrderSN);
        RechargeRefundOrderEntity.getInstance()
                .where("id", id)
                .update(set_data);
    }

    /**
     * 修复充值退款订单
     */
    public void repairChargeCardRefundOrder(long start_time, long end_time) {
        if (start_time == 0) start_time = TimeUtil.getTime00(-2);
        if (end_time == 0) end_time = TimeUtil.getTime24(0);

        int page = 1;
        int limit = 100;
        long pages;
        long total_count = UserChargeCardRefundOrderEntity.getInstance()
                .where("refund_OrderSN", "")
                .where("create_time", ">=", start_time)
                .where("create_time", "<=", end_time)
                .countGetLong("1");
        if (total_count == 0) {
            LogsUtil.warn(TAG, "%s ~ %s 已无可修复的退款订单"
                    , TimeUtil.toShortTimeString(start_time)
                    , TimeUtil.toShortTimeString(end_time)
            );
            return;
        }
        pages = Convert.toInt(Math.ceil(total_count * 1.0 / limit));

        while (page <= pages) {
            List<Map<String, Object>> list = UserChargeCardRefundOrderEntity.getInstance()
                    .field("id,refund_OrderSN,create_time")
                    .where("refund_OrderSN", "")
                    .where("create_time", ">=", start_time)
                    .where("create_time", "<=", end_time)
                    .select();
            if (list == null || list.isEmpty()) break;

            LogsUtil.info(TAG, "%s ~ %s 修复进度：%s/%s - 开始修复"
                    , TimeUtil.toShortTimeString(start_time)
                    , TimeUtil.toShortTimeString(end_time)
                    , page
                    , pages
            );

            for (Map<String, Object> map : list) {
                long id = MapUtil.getLong(map, "id");

                updateChargeCardRefundOrderJob(map);

                LogsUtil.info(TAG, "%s ~ %s 修复进度：%s/%s - %s 已经修复"
                        , TimeUtil.toShortTimeString(start_time)
                        , TimeUtil.toShortTimeString(end_time)
                        , page
                        , pages
                        , id
                );
            }
            page++;
        }

        LogsUtil.info(TAG, "%s ~ %s 已完成修复退款订单"
                , TimeUtil.toShortTimeString(start_time)
                , TimeUtil.toShortTimeString(end_time)
        );
    }

    /**
     * 修复充值退款订单具体执行
     *
     * @param data
     */
    private void updateChargeCardRefundOrderJob(Map<String, Object> data) {
        long id = MapUtil.getLong(data, "id");
        long create_time = MapUtil.getLong(data, "create_time");

        String refund_OrderSN = String.format("FU%s%s", TimeUtil.toTimeString(create_time, "yyyyMMddHHmmss"), common.randomStr(4));

        Map<String, Object> set_data = new HashMap<>();
        set_data.put("refund_OrderSN", refund_OrderSN);
        UserChargeCardRefundOrderEntity.getInstance()
                .where("id", id)
                .update(set_data);
    }

    /**
     * 通过河马支付的订单数据来修复消费退款订单数据
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void consumeOrderRefund(HttpServletRequest request, HttpServletResponse response) throws IOException {
        File imFile = HttpRequestUtil.getFile(request, "file");
        if (imFile == null) return;

        try (FileInputStream fis = new FileInputStream(imFile); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            //头部行号索引
            int headerRowIndex = 0; //索引从0开始

            for (Row row : sheet) {
                int rowNum = row.getRowNum();
                if (rowNum <= headerRowIndex) continue;

                // 银行单号
                String refund_bank_order_no = ExcelUtil.getString(row, "G");
                // 银联流水号
                String refund_bank_trx_no = ExcelUtil.getString(row, "H");
                // 原商户订单号
                String order_sn = ExcelUtil.getString(row, "I");
                // 订单金额
                BigDecimal refund_amount = ExcelUtil.getBigDecimal(row, "O");
                // 订单创建时间
                String create_time_str = ExcelUtil.getString(row, "T");

                // 如果退款订单中已经存在这笔订单了就不要再处理
                boolean is_exist = ConsumeOrderRefundsEntity.getInstance()
                        .where("refund_bank_order_no", refund_bank_order_no)
                        .exist();
                if (is_exist) {
                    LogsUtil.info(TAG, "%s 已经存在退款信息，无需处理", refund_bank_order_no);
                    continue;
                }

                ConsumeOrdersEntity consumeOrdersEntity = ConsumeOrdersEntity.getInstance()
                        .where("order_sn", order_sn)
                        .findEntity();
                if (consumeOrdersEntity == null || consumeOrdersEntity.id == 0) {
                    LogsUtil.info(TAG, "%s 未找到对应订单：order_sn=%s", refund_bank_order_no, order_sn);
                    continue;
                }

                long create_time = TimeUtil.toTimestamp(create_time_str);
                String refund_order_sn = String.format("FU%s%s", TimeUtil.toTimeString(create_time, "yyyyMMddHHmmss"), common.randomStr(4));

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("uid", consumeOrdersEntity.uid);
                data.put("order_id", consumeOrdersEntity.id);
                data.put("order_sn", order_sn);
                data.put("refund_order_sn", refund_order_sn);
                data.put("refund_amount", refund_amount);
                data.put("refund_bank_order_no", refund_bank_order_no);
                data.put("refund_bank_trx_no", refund_bank_trx_no);
                data.put("status", "SUCCESS");
                data.put("create_time", create_time);
                data.put("update_time", create_time);

                ConsumeOrderRefundsEntity.getInstance().insert(data);
            }
        }
    }
}
