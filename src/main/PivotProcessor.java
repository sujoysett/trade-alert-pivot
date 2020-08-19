package main;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PivotProcessor {
	private static String inputFilePath = "";
	private static String inputSheetName = "";

	public static void main(String s[]) {
		try {
			readProperties();
			readFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readFile() throws Exception {
		// obtaining input bytes from a file
		FileInputStream fis = new FileInputStream(new File(inputFilePath));
		XSSFWorkbook wb = new XSSFWorkbook(fis);
		XSSFSheet sheet = wb.getSheet(inputSheetName); // creating a Sheet object to retrieve object
		Iterator<Row> itr = sheet.iterator(); // iterating over excel file
		while (itr.hasNext()) {
			Row row = itr.next();
			Iterator<Cell> cellIterator = row.cellIterator(); // iterating over each column
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				switch (cell.getCellTypeEnum()) {
				case STRING: // field that represents string cell type
					System.out.print(cell.getStringCellValue() + "\t\t\t");
					break;
				case NUMERIC: // field that represents number cell type
					System.out.print(cell.getNumericCellValue() + "\t\t\t");
					break;
				default:
				}
			}
			System.out.println("");
		}
		wb.close();
		fis.close();
	}

	public static void readProperties() throws Exception {
		Properties prop = new Properties();
		FileInputStream ip = new FileInputStream("config.properties");
		prop.load(ip);
		inputFilePath = prop.getProperty("INPUT_FILE");
		inputSheetName = prop.getProperty("INPUT_SHEET");
	}
}

class InputStructure {
	public String alertName;
	public String triggerTimeStampString;
	public Calendar triggerTimeStamp;
	public int count;
	public String stocks;
	public String[] stockArray;
	public HashSet<String> distinctStockArray;
}