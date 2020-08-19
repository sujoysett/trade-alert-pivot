package main;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PivotProcessor {
	private static String inputFilePath = "";
	private static String inputSheetName = "";
	private static String dateTimeFormat = "";
	private static ArrayList<InputStructure> inputStructureData = new ArrayList<InputStructure>();

	public static void main(String s[]) {
		try {
			readProperties();
			readFile();
			enhanceData();
			System.out.println(inputStructureData.get(0));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void enhanceData() throws Exception {
		for (InputStructure inputStructure: inputStructureData) {
			// split stocks
			inputStructure.distinctStockArray = new HashSet<String>();
			inputStructure.distinctStockArray.addAll(Arrays.asList(inputStructure.stocks.split(" ")));
			// parse date time
			inputStructure.triggerAtCalendarObj = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat, Locale.ENGLISH);
			inputStructure.triggerAtCalendarObj.setTime(sdf.parse(inputStructure.triggeredAt));
			inputStructure.triggerAtDateObj = inputStructure.triggerAtCalendarObj.getTime();
			SimpleDateFormat datePortionFormat = new SimpleDateFormat("dd-MM-yyyy");
			inputStructure.datePortion = datePortionFormat.format(inputStructure.triggerAtDateObj);
			SimpleDateFormat timePortionFormat = new SimpleDateFormat("HH:mm");
			inputStructure.timePortion = timePortionFormat.format(inputStructure.triggerAtDateObj);
		}
	}

	public static void readFile() throws Exception {
		FileInputStream fis = new FileInputStream(new File(inputFilePath)); // obtaining input bytes from a file
		XSSFWorkbook wb = new XSSFWorkbook(fis);
		XSSFSheet sheet = wb.getSheet(inputSheetName); // creating a Sheet object to retrieve object
		int rowCounter = 0;
		Iterator<Row> itr = sheet.iterator(); // iterating over excel file
		while (itr.hasNext()) {
			Row row = itr.next();
			if (rowCounter > 0) {
				InputStructure inputStructure = new InputStructure();
				int colCounter = 0;
				Iterator<Cell> cellIterator = row.cellIterator(); // iterating over each column
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					switch (cell.getCellTypeEnum()) {
					case STRING: // field that represents string cell type
						// System.out.print(cell.getStringCellValue() + "\t\t\t");
						if (colCounter == 0) {
							inputStructure.alertName = cell.getStringCellValue().trim().toUpperCase();
						}
						if (colCounter == 1) {
							inputStructure.triggeredAt = cell.getStringCellValue().trim();
						}
						if (colCounter == 3) {
							inputStructure.stocks = cell.getStringCellValue().trim().toUpperCase();
						}
						break;
					case NUMERIC: // field that represents number cell type
						// System.out.print(cell.getNumericCellValue() + "\t\t\t");
						break;
					default:
					}
					colCounter++;
				}
				System.out.println("");
				inputStructureData.add(inputStructure);
			}
			rowCounter++;
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
		dateTimeFormat = prop.getProperty("DATE_TIME_FORMAT");
	}
}

class InputStructure {
	public String alertName;
	public String triggeredAt;
	public Calendar triggerAtCalendarObj;
	public Date triggerAtDateObj;
	public String datePortion;
	public String timePortion;
	public int count;
	public String stocks;
	public String[] stockArray;
	public HashSet<String> distinctStockArray;
	@Override
	public String toString() {
		return "InputStructure [alertName=" + alertName + ", triggeredAt=" + triggeredAt + ", triggerAtCalendarObj="
				+ triggerAtCalendarObj + ", triggerAtDateObj=" + triggerAtDateObj + ", datePortion=" + datePortion
				+ ", timePortion=" + timePortion + ", count=" + count + ", stocks=" + stocks + ", stockArray="
				+ Arrays.toString(stockArray) + ", distinctStockArray=" + distinctStockArray + "]";
	}
}