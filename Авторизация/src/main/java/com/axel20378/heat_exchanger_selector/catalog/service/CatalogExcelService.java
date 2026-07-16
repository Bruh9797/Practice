package com.axel20378.heat_exchanger_selector.catalog.service;

import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchangerFamily;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CodeName;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchItem;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchRequest;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogExcelService {
    private static final String[] HEADERS = {
            "Производитель", "Модель", "Серия", "Тип", "Площадь, м²",
            "Расход от, м³/ч", "Расход до, м³/ч", "Температура от, °C",
            "Температура до, °C", "Макс. давление, bar", "Ширина, мм",
            "Высота, мм", "Глубина/длина, мм", "Масса, кг", "Применение", "Материалы"
    };

    private final CatalogQueryService queryService;

    public CatalogExcelService(CatalogQueryService queryService) {
        this.queryService = queryService;
    }

    public byte[] export(SearchRequest request) {
        SearchRequest allRows = new SearchRequest(
                request.query(), request.families(), request.manufacturerIds(), request.applicationCodes(),
                request.materialCodes(), request.requiredSurfaceAreaM2(), request.requiredFlowM3h(),
                request.requiredTemperatureC(), request.requiredPressureBar(), 0, 100
        );
        List<SearchItem> items = queryService.search(allRows).items();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Теплообменники");
            sheet.setDisplayGridlines(false);
            sheet.createFreezePane(0, 1);

            CellStyle headerStyle = headerStyle(workbook);
            CellStyle textStyle = textStyle(workbook);
            CellStyle wrappedTextStyle = wrappedTextStyle(workbook);
            CellStyle decimalStyle = numericStyle(workbook, "0.0");
            CellStyle millimetreStyle = numericStyle(workbook, "0");

            Row header = sheet.createRow(0);
            header.setHeightInPoints(30);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (SearchItem item : items) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(34);
                int column = 0;
                text(row, column++, item.manufacturer().name(), textStyle);
                text(row, column++, item.model(), textStyle);
                text(row, column++, item.seriesName(), textStyle);
                text(row, column++, familyLabel(item.family()), textStyle);
                number(row, column++, item.surfaceAreaM2(), decimalStyle);
                number(row, column++, item.flowMinM3h(), decimalStyle);
                number(row, column++, item.flowMaxM3h(), decimalStyle);
                number(row, column++, item.temperatureMinC(), decimalStyle);
                number(row, column++, item.temperatureMaxC(), decimalStyle);
                number(row, column++, item.pressureMaxBar(), decimalStyle);
                number(row, column++, item.widthMm(), millimetreStyle);
                number(row, column++, item.heightMm(), millimetreStyle);
                number(row, column++, item.depthMm(), millimetreStyle);
                number(row, column++, item.massKg(), decimalStyle);
                text(row, column++, join(item.applications()), wrappedTextStyle);
                text(row, column, join(item.materials()), wrappedTextStyle);
            }

            if (!items.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(0, items.size(), 0, HEADERS.length - 1));
            }
            int[] widths = {18, 31, 18, 19, 14, 17, 17, 18, 18, 19, 14, 14, 20, 14, 34, 34};
            for (int column = 0; column < widths.length; column++) {
                sheet.setColumnWidth(column, widths[column] * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать Excel-файл", exception);
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private static CellStyle textStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.HAIR);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private static CellStyle numericStyle(Workbook workbook, String pattern) {
        CellStyle style = textStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat(pattern));
        return style;
    }

    private static CellStyle wrappedTextStyle(Workbook workbook) {
        CellStyle style = textStyle(workbook);
        style.setWrapText(true);
        return style;
    }

    private static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void number(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private static String join(List<CodeName> values) {
        return values.stream().map(CodeName::name).collect(Collectors.joining(", "));
    }

    private static String familyLabel(HeatExchangerFamily family) {
        return switch (family) {
            case PLATE -> "Пластинчатый";
            case SHELL_AND_TUBE -> "Кожухотрубный";
            case AIR_COOLED -> "Воздушный";
            case SPIRAL -> "Спиральный";
        };
    }
}
