package PositionKeeperTest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;


public class poitest {
	public static void main(String[] args) throws Exception {
		File file = new File("test.xls");
		HSSFWorkbook workbook = new HSSFWorkbook();
			workbook.createSheet();
			HSSFSheet sheet = workbook.getSheetAt(0);
			sheet.createRow(0);
			Row row = sheet.createRow(1);
			sheet.addMergedRegion(new CellRangeAddress(1,1,1,12));
			row.createCell(0);
			HSSFCellStyle style = workbook.createCellStyle();
			style.setWrapText(true);
			Cell cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue("INSERT INTO trades (trade_id, account_id, product_cusip, exchange, status, sourcesystem_id, knowledge_date, effective_date, settlement_date, position_delta,create_user, create_timestamp, last_update_user, last_update_timestamp) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
			FileOutputStream out = null;
			for(int i=0;i<=15;i++){
				sheet.autoSizeColumn(i);
			}
			try {
				out = new FileOutputStream(file);
			    workbook.write(out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally{
				if(out!=null)
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
	}
}
