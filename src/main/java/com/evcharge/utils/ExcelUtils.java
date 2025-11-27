package com.evcharge.utils;

import com.xyzs.utils.LogsUtil;
import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ExcelUtils {


//    ReadConfig readConfig = ReadConfig.builder()
//            .fields(Arrays.asList("name", "age", "birthday"))
//            .fieldTypeMap(new HashMap<String, Class<?>>() {{
//                put("name", String.class);
//                put("age", Integer.class);
//                put("birthday", Long.class);
//            }})
//            .build();
//    JSONArray data = ExcelUtils.read(multipartFile, readConfig);
//


//    @RestController
//    @RequestMapping("/excel")
//    public class ExcelController {
//
//        @PostMapping("/export/sync") //同步方法
//        public void exportSync(@RequestBody List<Map<String, Object>> data,
//                               ExportConfig config,
//                               HttpServletResponse response) throws IOException {
//            byte[] excelContent = ExcelUtils.syncExport(data, config);
//
//            response.setContentType(EXCEL_XLSX_CONTENT_TYPE);
//            response.setHeader("Content-Disposition", "attachment;filename=" + config.getFileName() + ".xlsx");
//            response.getOutputStream().write(excelContent);
//        }
//
//        @PostMapping("/export/async") 异步方法
//        public String exportAsync(@RequestBody List<Map<String, Object>> data,
//                                  ExportConfig config) {
//            String fileId = ExcelUtils.asyncExport(data, config);
//            // 可以根据实际需求构造下载URL
//            return "/download/" + fileId;
//        }
//    }
    private static final String EXCEL_XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    // 导出任务重试次数
    private static final int MAX_RETRY_COUNT = 3;
    // 每个sheet最大行数
    private static final int MAX_ROWS_PER_SHEET = 100000;

    /**
     * Excel读取配置类
     */
    @Data
    @Builder
    public static class ReadConfig {
        private List<String> fields;
        // 字段类型映射
        private Map<String, Class<?>> fieldTypeMap;
        // Excel表头到字段的映射
        private Map<String, String> headerFieldMap;
    }

    /**
     * Excel导出配置类
     */
    @Data
    @Builder
    public static class ExportConfig {
        // 导出的字段列表
        private List<String> fields;
        // 字段对应的标题
        private Map<String, String> fieldTitleMap;
        // 是否异步导出
        private boolean async;
        // 文件名
        private String fileName;
    }

    /**
     * 读取Excel文件（支持本地文件和网络文件）
     *
     * @param source 文件来源（可以是MultipartFile或URL字符串）
     * @param config 读取配置
     * @return JSON数组
     */
//    public static JSONArray read(Object source, ReadConfig config) throws IOException {
//        // 获取输入流
//        InputStream inputStream;
//        if (source instanceof MultipartFile) {
//            MultipartFile file = (MultipartFile) source;
//            // 校验文件类型
//            if (!EXCEL_XLSX_CONTENT_TYPE.equals(file.getContentType())) {
//                throw new IllegalArgumentException("仅支持.xlsx格式的Excel文件");
//            }
//            inputStream = file.getInputStream();
//        } else if (source instanceof String) {
//            // 网络文件
//            URL url = new URL((String) source);
//            inputStream = url.openStream();
//        } else {
//            throw new IllegalArgumentException("不支持的文件来源类型");
//        }
//
//        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            // 读取表头
//            Row headerRow = sheet.getRow(0);
//            Map<Integer, String> headerMap = new HashMap<>();
//            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
//                Cell cell = headerRow.getCell(i);
//                if (cell != null) {
//                    String headerName = cell.getStringCellValue().trim();
//                    if (config.getFields().contains(headerName)) {
//                        headerMap.put(i, headerName);
//                    }
//                }
//            }
//
//            // 读取数据
//            JSONArray result = new JSONArray();
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                Map<String, Object> rowData = new HashMap<>();
//                for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
//                    Cell cell = row.getCell(entry.getKey());
//                    String fieldName = entry.getValue();
//                    rowData.put(fieldName, getCellValue(cell, config.getFieldTypeMap().get(fieldName)));
//                }
//                result.add(rowData);
//            }
//            return result;
//        }
//    }
//    public static List<Map<String,Object>> read(Object source, ReadConfig config) throws IOException {
//        InputStream inputStream = getInputStream(source);
//
//        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            Row headerRow = sheet.getRow(0);
//
//            // 读取表头并建立映射关系
//            Map<Integer, String> columnFieldMap = new HashMap<>();
//            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
//                Cell cell = headerRow.getCell(i);
//                if (cell != null) {
//                    String headerName = cell.getStringCellValue().trim();
//                    // 优先使用headerFieldMap中的映射
//                    String fieldName = config.getHeaderFieldMap() != null ?
//                            config.getHeaderFieldMap().getOrDefault(headerName, headerName) :
//                            headerName;
//
//                    if (config.getFields().contains(fieldName)) {
//                        columnFieldMap.put(i, fieldName);
//                    }
//                }
//            }
//
//            // 读取数据
//            List<Map<String,Object>> result = new ArrayList<>();
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                Map<String, Object> rowData = new HashMap<>();
//                for (Map.Entry<Integer, String> entry : columnFieldMap.entrySet()) {
//                    Cell cell = row.getCell(entry.getKey());
//                    String fieldName = entry.getValue();
//                    rowData.put(fieldName, getCellValue(cell, config.getFieldTypeMap().get(fieldName)));
//                }
//                result.add(rowData);
//            }
//            return result;
//        }
//    }
    /**
     * 读取Excel文件（支持本地文件和网络文件）
     *
     * @param source 文件来源（可以是MultipartFile或URL字符串）
     * @param config 读取配置
     * @return JSON数组
     */
    public static List<Map<String, Object>> read(Object source, ReadConfig config)  {

        try {
            InputStream inputStream = getInputStream(source);

            try (Workbook workbook = new XSSFWorkbook(inputStream)) {
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);

                // 读取表头并建立映射关系
                Map<Integer, String> columnFieldMap = new HashMap<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        String headerName = cell.getStringCellValue().trim();
                        String fieldName = config.getHeaderFieldMap() != null ?
                                config.getHeaderFieldMap().getOrDefault(headerName, headerName) :
                                headerName;

                        if (config.getFields().contains(fieldName)) {
                            columnFieldMap.put(i, fieldName);
                        }
                    }
                }

                // 读取数据
                List<Map<String, Object>> result = new ArrayList<>();
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Map<String, Object> rowData = new HashMap<>();
                    for (Map.Entry<Integer, String> entry : columnFieldMap.entrySet()) {
                        Cell cell = row.getCell(entry.getKey());
                        String fieldName = entry.getValue();
                        rowData.put(fieldName, getCellValue(cell, config.getFieldTypeMap().get(fieldName)));
                    }
                    result.add(rowData);
                }
                return result;
            }
        }catch (Exception e){
            LogsUtil.error("ExcelUtils",e.getMessage());
        }
        return null;
    }
    private static InputStream getInputStream(Object source) throws IOException {
//        if (source instanceof MultipartFile) {
//            MultipartFile file = (MultipartFile) source;
//            if (!EXCEL_XLSX_CONTENT_TYPE.equals(file.getContentType())) {
//                throw new IllegalArgumentException("仅支持.xlsx格式的Excel文件");
//            }
//            return file.getInputStream();
//        } else if (source instanceof String) {
//            return new URL((String) source).openStream();
//        }
//        throw new IllegalArgumentException("不支持的文件来源类型");
//        if (source instanceof MultipartFile) {
//            MultipartFile file = (MultipartFile) source;
//            if (!EXCEL_XLSX_CONTENT_TYPE.equals(file.getContentType())) {
//                throw new IllegalArgumentException("仅支持.xlsx格式的Excel文件");
//            }
//            return file.getInputStream();
//        } else if (source instanceof String) {
//            String path = (String) source;
//            if (path.startsWith("http")) {
//                return new URL(path).openStream();
//            } else {
//                return new FileInputStream(path);
//            }
//        }
//        throw new IllegalArgumentException("不支持的文件来源类型");
        if (source instanceof MultipartFile) {
            MultipartFile file = (MultipartFile) source;
            if (!EXCEL_XLSX_CONTENT_TYPE.equals(file.getContentType())) {
                throw new IllegalArgumentException("仅支持.xlsx格式的Excel文件");
            }
            if (file.isEmpty()) {
                throw new IllegalArgumentException("上传的Excel文件为空");
            }
            InputStream inputStream = file.getInputStream();
            // 简单验证文件是否为 ZIP 文件（检查文件头部的魔数 PK\003\004）
            byte[] header = new byte[4];
            int bytesRead = inputStream.read(header);
            if (bytesRead < 4 || header[0] != 0x50 || header[1] != 0x4B || header[2] != 0x03 || header[3] != 0x04) {
                throw new IllegalArgumentException("文件不是有效的.xlsx文件");
            }
            // 重置输入流以供后续使用（因为MultipartFile的输入流只能读取一次）
            return new ByteArrayInputStream(file.getBytes());
        } else if (source instanceof String) {
            String path = (String) source;
            if (path.startsWith("http")) {
                return new URL(path).openStream();
            } else {
                return new FileInputStream(path);
            }
        }
        throw new IllegalArgumentException("不支持的文件来源类型");
    }
    /**
     * 导出Excel
     *
     * @param data   数据列表
     * @param config 导出配置
     * @return 如果是异步导出，返回文件下载链接；否则直接写入响应流
     */
    public static String export(List<Map<String, Object>> data, ExportConfig config) {
        if (config.isAsync()) {
            return asyncExport(data, config);
        } else {
            syncExport(data, config);
            return null;
        }
    }

    // 私有方法：获取单元格值并转换类型
    private static Object getCellValue(Cell cell, Class<?> targetType) {
        if (cell == null) return null;

        Object value = null;
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 日期类型转换为时间戳
                    value = cell.getDateCellValue().getTime();
                } else {
                    value = cell.getNumericCellValue();
                }
                break;
            case BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            default:
                break;
        }

        // 类型转换
        if (value != null && targetType != null) {
            if (targetType == Integer.class) {
                value = ((Number) value).intValue();
            } else if (targetType == Long.class) {
                value = ((Number) value).longValue();
            } else if (targetType == Double.class) {
                value = ((Number) value).doubleValue();
            }
            // 可以根据需要添加其他类型的转换
        }

        return value;
    }

    // 私有方法：同步导出
    /**
     * 同步导出Excel，返回字节数组
     *
     * @param data   数据列表
     * @param config 导出配置
     * @return byte[] Excel文件的字节数组
     */
    public static byte[] syncExport(List<Map<String, Object>> data, ExportConfig config) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(500);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            createExcelContent(workbook, data, config);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    // 私有方法：异步导出
    private static String asyncExport(List<Map<String, Object>> data, ExportConfig config) {
        String fileId = UUID.randomUUID().toString();
        CompletableFuture.runAsync(() -> {
            int retryCount = 0;
            while (retryCount < MAX_RETRY_COUNT) {
                try {
                    // 创建临时文件
                    File tempFile = File.createTempFile(fileId, ".xlsx");
                    try (SXSSFWorkbook workbook = new SXSSFWorkbook(500);
                         FileOutputStream fos = new FileOutputStream(tempFile)) {
                        createExcelContent(workbook, data, config);
                        workbook.write(fos);
                    }
                    // TODO: 将临时文件移动到永久存储位置，返回可访问的URL
                    break;
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount >= MAX_RETRY_COUNT) {
                        throw new RuntimeException("导出Excel失败，重试次数已达上限", e);
                    }
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        // 返回文件下载链接
//        return "/download/" + fileId;
        return fileId;
    }

    // 私有方法：创建Excel内容
    private static void createExcelContent(SXSSFWorkbook workbook, List<Map<String, Object>> data, ExportConfig config) {
        int sheetIndex = 0;
        int rowIndex = 0;
        Sheet sheet = null;

        // 写入表头
        for (int i = 0; i < data.size(); i++) {
            if (i % MAX_ROWS_PER_SHEET == 0) {
                sheet = workbook.createSheet("Sheet" + (++sheetIndex));
                // 创建表头
                Row headerRow = sheet.createRow(0);
                for (int j = 0; j < config.getFields().size(); j++) {
                    String field = config.getFields().get(j);
                    Cell cell = headerRow.createCell(j);
                    cell.setCellValue(config.getFieldTitleMap().get(field));
                }
                rowIndex = 1;
            }

            // 写入数据
            Row row = sheet.createRow(rowIndex++);
            Map<String, Object> rowData = data.get(i);
            for (int j = 0; j < config.getFields().size(); j++) {
                String field = config.getFields().get(j);
                Cell cell = row.createCell(j);
                setCellValue(cell, rowData.get(field));
            }
        }
    }

    // 私有方法：设置单元格值
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}