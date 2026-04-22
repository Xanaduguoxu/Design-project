package org.example.utils;

import cn.hutool.json.JSONObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileUtils {

    private static final Map<String, String> MY_MAP;

    //  TODO: 添加字段映射关系
    static {
        Map<String, String> tempMap = new HashMap<>();
        tempMap.put("名字", "name");
        tempMap.put("别名", "alias");
        tempMap.put("产地", "place");
        tempMap.put("性味", "nature");
        tempMap.put("归经", "tropism");
        tempMap.put("功效", "effect");
        tempMap.put("规格", "standard");
        tempMap.put("数量", "quantity");
        tempMap.put("单位", "unit");
        tempMap.put("总价格", "price");
        tempMap.put("生产日期", "productionDate");
        tempMap.put("有效期", "expirationDate");
        tempMap.put("序列码", "sn");
        MY_MAP = Collections.unmodifiableMap(tempMap);
    }

    public static <T> void exportExcel(List<T> dataList, String fileName, HttpServletResponse response) throws Exception {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();

        Class<?> clazz = dataList.get(0).getClass();
        // 递归获取所有字段（包括父类）
        List<Field> allFields = getAllFields(clazz);

        // 创建表头行
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < allFields.size(); i++) {
            Cell cell = headerRow.createCell(i);
            // 可以添加注解支持，获取字段的中文名等
            cell.setCellValue(allFields.get(i).getName());
        }

        // 填充数据
        for (int i = 0; i < dataList.size(); i++) {
            Row dataRow = sheet.createRow(i + 1);
            T data = dataList.get(i);
            for (int j = 0; j < allFields.size(); j++) {
                Field field = allFields.get(j);
                field.setAccessible(true);
                Object value = field.get(data);
                Cell cell = dataRow.createCell(j);
                cell.setCellValue(value != null ? value.toString() : "");
            }
        }

        // 设置响应头
        response.setContentType("application/octet-stream;charset=utf-8");
        String encodedFileName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8.toString());
        response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + encodedFileName);

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            workbook.close();
        }
    }

    public static List<JSONObject> importExcel(MultipartFile file) throws Exception {
        // 校验文件
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xls") || filename.endsWith(".xlsx"))) {
            throw new IllegalArgumentException("仅支持Excel文件(.xls, .xlsx)");
        }

        List<JSONObject> answer = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {

            // 获取第一个Sheet
            Sheet sheet = workbook.getSheetAt(0);

            // 获取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return answer;
            }

            // 将表头单元格的值存入一个List
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(MY_MAP.get(cell.getStringCellValue()));
            }

            // 遍历数据行（从第二行开始）
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow == null) {
                    continue;
                }

                JSONObject rowData = new JSONObject();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = dataRow.getCell(j);
                    String header = headers.get(j);
                    String value = getCellValueAsString(cell);
                    rowData.putOpt(header, value);
                }
                answer.add(rowData);
            }
        } catch (Exception e) {
            throw new IOException("解析Excel文件失败: " + e.getMessage(), e);
        }

        return answer;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return sdf.format(date);
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.format("%d", (long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            default:
                return null;
        }
    }
}
