package com.evcharge.libsdk.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExcelExportUtil {

    /**
     * 将数据导出到 Excel 文件
     * @param headers 列标题
     * @param data 每一行的数据，Map 的 key 应该与列标题匹配
     * @param filePath 保存的文件路径
     * @throws IOException 如果文件写入出错
     */
    public static void exportToExcel(List<String> headers, List<Map<String, Object>> data, String filePath) throws IOException {
        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();
        // 创建工作表
        Sheet sheet = workbook.createSheet("Sheet1");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        // 填充数据
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> rowData = data.get(i);
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.createCell(j);
                Object value = rowData.get(headers.get(j));
                setCellValue(cell, value);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入文件
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }

        // 关闭工作簿
        workbook.close();
    }

    /**
     * 设置单元格的值
     * @param cell 单元格
     * @param value 值
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 创建标题样式
     * @param workbook 工作簿
     * @return 标题样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}