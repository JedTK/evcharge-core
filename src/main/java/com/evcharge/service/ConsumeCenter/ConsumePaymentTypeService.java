package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.payment.ConsumePaymentTypeEntity;
import com.xyzs.cache.ECacheTime;
import org.springframework.stereotype.Service;

@Service
public class ConsumePaymentTypeService {

    /**
     * 检查支付方式是否存在，不存在返回false，存在返回true
     * @param paymentType 支付类型
     * @return boolean
     */
    public boolean checkPaymentTypeExits(String paymentType) {
        return ConsumePaymentTypeEntity.getInstance()
                .where("method_code", paymentType)
                .cache(String.format("EvcPaymentType:%s", paymentType), ECacheTime.WEEK)
                .exist();

    }
}
