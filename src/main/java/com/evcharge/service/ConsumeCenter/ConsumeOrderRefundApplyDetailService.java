package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundApplyDetailEntity;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ConsumeOrderRefundApplyDetailService {

    public List<ConsumeOrderRefundApplyDetailEntity> getOrderList(String applySn){

        return ConsumeOrderRefundApplyDetailEntity
                .getInstance()
                .where("apply_sn",applySn)
                .selectList();

    }


}
