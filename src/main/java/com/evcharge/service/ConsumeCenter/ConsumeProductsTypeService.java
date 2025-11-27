package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.product.ConsumeProductsTypeEntity;
import org.springframework.stereotype.Service;

@Service
public class ConsumeProductsTypeService {


    /**
     * 获取活动页的codes
     * @return String[]
     */
    public String[] getDefaultSaleCode(){

        String[] codes=ConsumeProductsTypeEntity.getInstance()
                .where("sale_status",1)
                .selectForStringArray("code");

        if(codes.length==0){
            return null;
        }

        return codes;
    }


    public ConsumeProductsTypeEntity getConsumeProductsTypeByCode(String typeCode) {
        return ConsumeProductsTypeEntity.getInstance()
                .cache(String.format("ConsumeCenter:ProductsType:%s", typeCode))
                .where("code",typeCode)
                .findEntity();
    }


}
