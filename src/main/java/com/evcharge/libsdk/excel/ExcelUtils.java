package com.evcharge.libsdk.excel;

import com.xyzs.utils.LogsUtil;
import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ExcelUtils {

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

        // 新增字段
        private int imageWidth;  // 单位：像素
        private int imageHeight; // 单位：像素

        private int imageCellWidth = 100;   // 单元格宽度（像素）
        private int imageCellHeight = 100;  // 单元格高度（像素）
    }


    /**
     * 读取Excel文件（支持本地文件和网络文件）
     *
     * @param source 文件来源（可以是MultipartFile或URL字符串）
     * @param config 读取配置
     * @return JSON数组
     */
    public static List<Map<String, Object>> read(Object source, ReadConfig config) {

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
        } catch (Exception e) {
            LogsUtil.error("ExcelUtils", e.getMessage());
        }
        return null;
    }

    private static InputStream getInputStream(Object source) throws IOException {
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

    public static byte[] syncExportWithImage(List<Map<String, Object>> data, ExportConfig config) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");

            int rowIndex = 0;
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            CreationHelper helper = workbook.getCreationHelper();

            List<String> fields = config.getFields();
            int baseColumnCount = fields.size();

            // 写入表头
            Row headerRow = sheet.createRow(rowIndex++);
            for (int j = 0; j < fields.size(); j++) {
                Cell cell = headerRow.createCell(j);
                cell.setCellValue(config.getFieldTitleMap().getOrDefault(fields.get(j), fields.get(j)));
            }

            // 写入图片列标题（images_list -> image_1, image_2...）
            int maxImageCount = 0;
            for (Map<String, Object> row : data) {
                String[] images = (String[]) row.getOrDefault("images_list", new String[0]);
                if (images != null) {
                    maxImageCount = Math.max(maxImageCount, images.length);
                }
            }
            for (int i = 0; i < maxImageCount; i++) {
                Cell cell = headerRow.createCell(baseColumnCount + i);
                cell.setCellValue("Image_" + (i + 1));
            }

            // 写入数据 + 图片
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowIndex);

                // 普通字段写入
                for (int j = 0; j < fields.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = rowData.get(fields.get(j));
                    setCellValue(cell, value);
                }

                // 图片插入
                String[] imageUrls = (String[]) rowData.getOrDefault("images_list", new String[0]);
                for (int i = 0; i < maxImageCount; i++) {
                    int colIndex = baseColumnCount + i;
                    sheet.setColumnWidth(colIndex, config.getImageWidth() * 40); // 列宽
                    row.setHeightInPoints(config.getImageHeight());              // 行高

                    if (i < imageUrls.length && imageUrls[i] != null && !imageUrls[i].isEmpty()) {
                        try (InputStream is = new URL(imageUrls[i]).openStream()) {
                            byte[] bytes = is.readAllBytes();
                            int pictureType = imageUrls[i].endsWith(".png") ? Workbook.PICTURE_TYPE_PNG : Workbook.PICTURE_TYPE_JPEG;
                            int pictureIdx = workbook.addPicture(bytes, pictureType);

                            ClientAnchor anchor = helper.createClientAnchor();
                            anchor.setCol1(colIndex);
                            anchor.setRow1(rowIndex);
                            anchor.setCol2(colIndex + 1);
                            anchor.setRow2(rowIndex + 1);
                            drawing.createPicture(anchor, pictureIdx);
                        } catch (IOException e) {
                            // 图片无法读取，保留空单元格
                        }
                    }
                }

                rowIndex++;
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出包含图片的Excel失败", e);
        }
    }

    //    public static void syncExportWithImage(List<Map<String, Object>> dataList, ExportConfig config, OutputStream os) {
//        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
//            XSSFSheet sheet = workbook.createSheet("Sheet1");
//
//            List<String> fields = config.getFields();
//            Map<String, String> titleMap = config.getFieldTitleMap() != null ? config.getFieldTitleMap() : new HashMap<>();
//            int imageCellWidth = config.getImageCellWidth();
//            int imageCellHeight = config.getImageCellHeight();
//
//            // 标题行
//            XSSFRow headerRow = sheet.createRow(0);
//            int colIndex = 0;
//            for (String field : fields) {
//                String title = titleMap.getOrDefault(field, field);
//                XSSFCell cell = headerRow.createCell(colIndex++);
//                cell.setCellValue(title);
//            }
//
//            // 图片列标题（images_list）
//            int maxImageCount = 0;
//            for (Map<String, Object> row : dataList) {
//                String[] imgs = (String[]) row.get("images_list");
//                if (imgs != null && imgs.length > maxImageCount) {
//                    maxImageCount = imgs.length;
//                }
//            }
//            for (int i = 0; i < maxImageCount; i++) {
//                XSSFCell cell = headerRow.createCell(colIndex++);
//                cell.setCellValue("图片" + (i + 1));
//            }
//
//            CreationHelper helper = workbook.getCreationHelper();
//            Drawing<?> drawing = sheet.createDrawingPatriarch();
//
//            // 数据行
//            for (int rowIndex = 0; rowIndex < dataList.size(); rowIndex++) {
//                Map<String, Object> rowMap = dataList.get(rowIndex);
//                XSSFRow row = sheet.createRow(rowIndex + 1);
//
//                int cellIndex = 0;
//                for (String field : fields) {
//                    Object value = rowMap.get(field);
//                    row.createCell(cellIndex++).setCellValue(value != null ? value.toString() : "");
//                }
//
//                String[] imageUrls = (String[]) rowMap.get("images_list");
//
//                if (imageUrls != null) {
//                    for (int i = 0; i < maxImageCount; i++) {
//                        XSSFCell cell = row.createCell(cellIndex++);
//
//                        // 即使没有图片，也保留空格
//                        if (i >= imageUrls.length || imageUrls[i] == null) continue;
//
//                        try {
//                            byte[] imageBytes = downloadImageBytes(imageUrls[i]);
//                            int pictureType = imageUrls[i].toLowerCase().endsWith(".png")
//                                    ? Workbook.PICTURE_TYPE_PNG : Workbook.PICTURE_TYPE_JPEG;
//                            int pictureIndex = workbook.addPicture(imageBytes, pictureType);
//
//                            int anchorCol = fields.size() + i;
//                            ClientAnchor anchor = helper.createClientAnchor();
//                            anchor.setCol1(anchorCol);
//                            anchor.setRow1(rowIndex + 1);
//                            anchor.setCol2(anchorCol + 1);
//                            anchor.setRow2(rowIndex + 2);
//
//                            // 设置图片大小：缩放90%，居中显示
//                            anchor.setDx1(Units.EMU_PER_PIXEL * (int) (imageCellWidth * 0.05));
//                            anchor.setDy1(Units.EMU_PER_PIXEL * (int) (imageCellHeight * 0.05));
//                            anchor.setDx2(Units.EMU_PER_PIXEL * (int) (imageCellWidth * 0.95));
//                            anchor.setDy2(Units.EMU_PER_PIXEL * (int) (imageCellHeight * 0.95));
//
//                            drawing.createPicture(anchor, pictureIndex);
//                        } catch (Exception e) {
//                            e.printStackTrace(); // 下载失败时跳过
//                        }
//                    }
//                }
//
//                // 设置行高
//                row.setHeight((short) (imageCellHeight * 15));
//            }
//
//            // 设置列宽
//            for (int i = 0; i < fields.size() + maxImageCount; i++) {
//                sheet.setColumnWidth(i, imageCellWidth * 40); // 约等于像素 -> 单位转换
//            }
//
//            workbook.write(os);
//        } catch (Exception e) {
//            throw new RuntimeException("导出Excel失败", e);
//        }
//    }
    public static void syncExportWithImage(List<Map<String, Object>> dataList, ExportConfig config, OutputStream os) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(config.getFileName() != null ? config.getFileName() : "Sheet1");

            List<String> fields = config.getFields();
            Map<String, String> titleMap = config.getFieldTitleMap() != null ? config.getFieldTitleMap() : new HashMap<>();
            int imageCellWidth = config.getImageCellWidth();
            int imageCellHeight = config.getImageCellHeight();

            // 表头
            XSSFRow headerRow = sheet.createRow(0);
            int colIndex = 0;
            for (String field : fields) {
                String title = titleMap.getOrDefault(field, field);
                headerRow.createCell(colIndex++).setCellValue(title);
            }

            int maxImageCount = 0;
            for (Map<String, Object> row : dataList) {
                String[] imgs = (String[]) row.get("images_list");
                if (imgs != null && imgs.length > maxImageCount) {
                    maxImageCount = imgs.length;
                }
            }

            for (int i = 0; i < maxImageCount; i++) {
                headerRow.createCell(colIndex++).setCellValue("图片" + (i + 1));
            }

            Drawing<?> drawing = sheet.createDrawingPatriarch();
            CreationHelper helper = workbook.getCreationHelper();

            for (int rowIndex = 0; rowIndex < dataList.size(); rowIndex++) {
                Map<String, Object> rowMap = dataList.get(rowIndex);
                XSSFRow row = sheet.createRow(rowIndex + 1);
                int cellIndex = 0;

                for (String field : fields) {
                    Object value = rowMap.get(field);
                    row.createCell(cellIndex++).setCellValue(value != null ? value.toString() : "");
                }

                String[] imageUrls = (String[]) rowMap.get("images_list");
                if (imageUrls != null) {
                    for (int i = 0; i < maxImageCount; i++) {
                        row.createCell(cellIndex); // 占位
                        if (i >= imageUrls.length || imageUrls[i] == null || imageUrls[i].isEmpty()) {
                            cellIndex++;
                            continue;
                        }

                        try {
                            byte[] imageBytes = downloadImageBytes(imageUrls[i]);
                            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                            int imgW = bufferedImage.getWidth();
                            int imgH = bufferedImage.getHeight();

                            double maxDrawW = imageCellWidth * 0.9;
                            double maxDrawH = imageCellHeight * 0.9;
                            double scale = Math.min(maxDrawW / imgW, maxDrawH / imgH);
                            int drawW = (int) (imgW * scale);
                            int drawH = (int) (imgH * scale);

                            int dx = Units.pixelToEMU((imageCellWidth - drawW) / 2);
                            int dy = Units.pixelToEMU((imageCellHeight - drawH) / 2);

                            int anchorCol = fields.size() + i;
                            ClientAnchor anchor = helper.createClientAnchor();
                            anchor.setCol1(anchorCol);
                            anchor.setCol2(anchorCol + 1);
                            anchor.setRow1(rowIndex + 1);
                            anchor.setRow2(rowIndex + 2);
                            anchor.setDx1(dx);
                            anchor.setDy1(dy);

                            int pictureType = imageUrls[i].toLowerCase().endsWith(".png") ?
                                    Workbook.PICTURE_TYPE_PNG : Workbook.PICTURE_TYPE_JPEG;
                            int pictureIndex = workbook.addPicture(imageBytes, pictureType);
                            XSSFPicture picture = (XSSFPicture) drawing.createPicture(anchor, pictureIndex);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        cellIndex++;
                    }
                }

                row.setHeight((short) (imageCellHeight * 15)); // 设置行高
            }

            // 设置所有列宽
            int totalColumns = fields.size() + maxImageCount;
            for (int i = 0; i < totalColumns; i++) {
                sheet.setColumnWidth(i, imageCellWidth * 40); // POI 单位：1/256 字符宽
            }

            workbook.write(os);
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    private static byte[] downloadImageBytes(String imageUrl) throws IOException {
        try (InputStream is = new URL(imageUrl).openStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
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
    public static void setCellValue(Cell cell, Object value) {
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