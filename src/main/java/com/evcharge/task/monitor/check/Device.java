package com.evcharge.task.monitor.check;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

/**
 * 设备数据模型 (使用兼容 Java 11 的标准 class 格式)
 */
@Getter
public class Device {
    private final String id;
    private final String name;
    private final String serialNumber;
    private final DeviceType type;
    private final Map<String, String> properties;

    public Device(String id, String name, String serialNumber, DeviceType type, Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.serialNumber = serialNumber;
        this.type = type;
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(id, device.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}