package com.evcharge.service.Admin;


import com.alibaba.fastjson2.JSON;
import com.evcharge.entity.admin.AdminToRegionEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RegionTreeBuilder {

    public static class RegionNode {
        private final String name;
        private final String code;
        private Object child; // RegionNode 或 List<RegionNode>

        public RegionNode(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public Object getChild() {
            return child;
        }

        public void setChild(Object child) {
            this.child = child;
        }
    }

    public static List<RegionNode> buildRegionHierarchy(List<AdminToRegionEntity> regionList) {
        List<RegionNode> result = new ArrayList<>();

        for (AdminToRegionEntity region : regionList) {
            RegionNode provinceNode = createOrFind(result, region.province, region.province_code);
            if ("ALL".equalsIgnoreCase(region.province_code)) {
                continue;
            }

            RegionNode cityNode = createOrFindChild(provinceNode, region.city, region.city_code);
            if ("ALL".equalsIgnoreCase(region.city_code)) {
                continue;
            }

            RegionNode districtNode = createOrFindChild(cityNode, region.district, region.district_code);
            if ("ALL".equalsIgnoreCase(region.district_code)) {
                continue;
            }

            List<RegionNode> streetList;
            if (districtNode.getChild() instanceof List) {
                streetList = (List<RegionNode>) districtNode.getChild();
            } else {
                streetList = new ArrayList<>();
                districtNode.setChild(streetList);
            }

            RegionNode streetNode = new RegionNode(nameOrAll(region.street, region.street_code), region.street_code);
            streetList.add(streetNode);
        }

        return result;
    }

    private static RegionNode createOrFind(List<RegionNode> list, String name, String code) {
        return list.stream()
                .filter(n -> Objects.equals(n.getCode(), code))
                .findFirst()
                .orElseGet(() -> {
                    RegionNode node = new RegionNode(nameOrAll(name, code), code);
                    list.add(node);
                    return node;
                });
    }

    private static RegionNode createOrFindChild(RegionNode parent, String name, String code) {
        if (parent.getChild() == null || !(parent.getChild() instanceof RegionNode)) {
            RegionNode child = new RegionNode(nameOrAll(name, code), code);
            parent.setChild(child);
            return child;
        }

        RegionNode child = (RegionNode) parent.getChild();
        if (Objects.equals(child.getCode(), code)) {
            return child;
        }

        return child;
    }

    private static String nameOrAll(String name, String code) {
        return "ALL".equalsIgnoreCase(code) ? "全部" : name;
    }


}