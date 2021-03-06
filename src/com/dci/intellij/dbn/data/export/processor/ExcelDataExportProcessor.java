package com.dci.intellij.dbn.data.export.processor;

import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.locale.options.RegionalSettings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportException;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportModel;
import com.intellij.openapi.project.Project;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class ExcelDataExportProcessor extends DataExportProcessor{

    protected DataExportFormat getFormat() {
        return DataExportFormat.EXCEL;
    }

    @Override
    public String getFileExtension() {
        return "xls";
    }

    public boolean canCreateHeader() {
        return true;
    }

    public boolean canExportToClipboard() {
        return false;
    }

    public boolean canQuoteValues() {
        return false;
    }

    @Override
    public String adjustFileName(String fileName) {
        if (!fileName.contains(".xls")) {
            fileName = fileName + ".xls";
        }
        return fileName;
    }

    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connectionHandler) throws DataExportException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(model.getTableName());

        if (instructions.createHeader()) {
            HSSFRow headerRow = sheet.createRow(0);

            for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++){
                String columnName = model.getColumnName(columnIndex);

                HSSFCell cell = headerRow.createCell(columnIndex);
                cell.setCellValue(columnName);

                HSSFCellStyle cellStyle = workbook.createCellStyle();
                HSSFFont tableHeadingFont = workbook.createFont();
                tableHeadingFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
                cellStyle.setFont(tableHeadingFont);
                cell.setCellStyle(cellStyle);
            }
        }

        CellStyleCache cellStyleCache = new CellStyleCache(workbook, model.getProject());

        for (short rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
            HSSFRow row = sheet.createRow(rowIndex + 1);
            for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++){
                HSSFCell cell = row.createCell(columnIndex);
                Object value = model.getValue(rowIndex, columnIndex);
                if (value != null) {
                    if (value instanceof Number) {
                        Number number = (Number) value;
                        double doubleValue = number.doubleValue();
                        cell.setCellValue(doubleValue);
                        cell.setCellStyle(
                                doubleValue % 1 == 0 ?
                                        cellStyleCache.getIntegerStyle() :
                                        cellStyleCache.getNumberStyle());

                    } else if (value instanceof Date) {
                        Date date = (Date) value;
                        boolean hasTime = hasTimeComponent(date);
                        cell.setCellValue(date);
                        cell.setCellStyle(hasTime ?
                                cellStyleCache.getDatetimeStyle() :
                                cellStyleCache.getDateStyle());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }
        }

        for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
            sheet.autoSizeColumn(columnIndex);
        }

        File file = instructions.getFile();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            workbook.write(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new DataExportException(
                    "Could not write file " + file.getPath() +".\n" +
                    "Reason: " + e.getMessage());
        }
    }

    private class CellStyleCache {
        private HSSFWorkbook workbook;
        private RegionalSettings regionalSettings;

        private HSSFCellStyle dateStyle;
        private HSSFCellStyle datetimeStyle;
        private HSSFCellStyle numberStyle;
        private HSSFCellStyle integerStyle;

        private CellStyleCache(HSSFWorkbook workbook, Project project) {
            this.workbook = workbook;
            regionalSettings = RegionalSettings.getInstance(project);
        }


        private Formatter getFormatter() {
            return regionalSettings.getFormatter();
        }

        public HSSFCellStyle getDateStyle() {
            if (dateStyle == null) {
                dateStyle = workbook.createCellStyle();
                String dateFormatPattern = getFormatter().getDateFormatPattern();
                short dateFormat = getFormat(dateFormatPattern);
                dateStyle.setDataFormat(dateFormat);
            }
            return dateStyle;
        }

        public HSSFCellStyle getDatetimeStyle() {
            if (datetimeStyle == null) {
                datetimeStyle = workbook.createCellStyle();
                String datetimeFormatPattern = getFormatter().getDatetimeFormatPattern();
                short dateFormat = getFormat(datetimeFormatPattern);
                datetimeStyle.setDataFormat(dateFormat);
            }

            return datetimeStyle;
        }

        public HSSFCellStyle getNumberStyle() {
            if (numberStyle == null) {
                numberStyle = workbook.createCellStyle();
                String numberFormatPattern = getFormatter().getNumberFormatPattern();
                short numberFormat = getFormat(numberFormatPattern);
                numberStyle.setDataFormat(numberFormat);
            }

            return numberStyle;
        }

        public HSSFCellStyle getIntegerStyle() {
            if (integerStyle == null) {
                integerStyle = workbook.createCellStyle();
                String integerFormatPattern = getFormatter().getIntegerFormatPattern();
                short integerFormat = getFormat(integerFormatPattern);
                integerStyle.setDataFormat(integerFormat);
            }

            return integerStyle;
        }

        private short getFormat(String datetimeFormatPattern) {
            CreationHelper creationHelper = workbook.getCreationHelper();
            DataFormat dataFormat = creationHelper.createDataFormat();
            return dataFormat.getFormat(datetimeFormatPattern);
        }

    }
}