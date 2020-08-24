package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PivotProcessor {
	private static String multiInput = "NO";
	private static String inputDirectoryPath = "";
	private static String inputFilePath = "";
	private static String inputSheetName = "";
	private static String triggerTimespampFormat = "";
	private static String outputFilePath = "";
	private static String outputSheetName = "";
	private static int inputColIndexForAlertName = -1;
	private static int inputColIndexForTriggerTimestamp = -1;
	private static int inputColIndexForStocks = -1;
	private static ArrayList<InputStructure> inputStructureData = new ArrayList<InputStructure>();
	private static OutputStructure1 outputStructureData = new OutputStructure1();
	private static int universalAlertCount = 0;
	private static ArrayList<String> universalAlertList = new ArrayList<String>();

	public static void main(String s[]) {
		try {
			readProperties();
			System.out.println("Reading Properties");
			
			readData();
			System.out.println("Reading File(s)");
			
			enhanceData();
			System.out.println("Enhancing Data");
			
			pivotData();
			System.out.println("Pivoting Data");
			
			writeFile();
			System.out.println("Writing File");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeFile() throws Exception {

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet(outputSheetName);

		Cell cell = null;
		int colCount = 0;
		int rowCount = 0;

		// header row
		Row headerRow = sheet.createRow(0);
		cell = headerRow.createCell(0);
		cell.setCellValue("Date");
		cell = headerRow.createCell(1);
		cell.setCellValue("Stock Name");
		colCount = 2;
		for (String alertName : universalAlertList) {
			cell = headerRow.createCell(colCount);
			cell.setCellValue(alertName);
			colCount++;
		}

		// body rows
		rowCount = 1;
		for (String dateOP : outputStructureData.mapDateStructure2.keySet()) {
			for (String stockOP : outputStructureData.mapDateStructure2.get(dateOP).mapStockStructure3.keySet()) {
				Row bodyRow = sheet.createRow(rowCount);
				cell = bodyRow.createCell(0);
				cell.setCellValue(dateOP);
				cell = bodyRow.createCell(1);
				cell.setCellValue(stockOP);
				colCount = 2;
				for (String alertName : universalAlertList) {
					String timeOP = outputStructureData.mapDateStructure2.get(dateOP).mapStockStructure3
							.get(stockOP).mapAlertTime.get(alertName);
					cell = bodyRow.createCell(colCount);
					cell.setCellValue(timeOP == null ? "" : timeOP);
					colCount++;
				}
				rowCount++;
			}
		}

		// Resize all columns to fit the content size
		for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
			sheet.autoSizeColumn(i);
		}

		// write
		FileOutputStream fileOut = new FileOutputStream(outputFilePath);
		workbook.write(fileOut);
		fileOut.close();
	}

	public static void pivotData() throws Exception {
		for (InputStructure inputStructure : inputStructureData) {
			if (!outputStructureData.mapDateStructure2.containsKey(inputStructure.datePortion)) {
				outputStructureData.mapDateStructure2.put(inputStructure.datePortion, new OutputStructure2());
			}
			OutputStructure2 ref2 = outputStructureData.mapDateStructure2.get(inputStructure.datePortion);
			for (String stock : inputStructure.distinctStockArray) {
				if (!ref2.mapStockStructure3.containsKey(stock)) {
					ref2.mapStockStructure3.put(stock, new OutputStructure3());
					OutputStructure3 ref3 = ref2.mapStockStructure3.get(stock);
					for (String universalAlert : universalAlertList) {
						ref3.mapAlertTime.put(universalAlert, null);
					}
				}
				OutputStructure3 ref3 = ref2.mapStockStructure3.get(stock);
				if (ref3.mapAlertTime.containsKey(inputStructure.alertName)) {
					if (ref3.mapAlertTime.get(inputStructure.alertName) == null) {
						ref3.mapAlertTime.put(inputStructure.alertName, inputStructure.timePortion);
					} else {
						String prevEntry = ref3.mapAlertTime.get(inputStructure.alertName);
						String thisEntry = inputStructure.timePortion;
						SimpleDateFormat sdf = new SimpleDateFormat("K:mm");
						if (sdf.parse(thisEntry).before(sdf.parse(prevEntry))) {
							// System.out.println(thisEntry + " is before "+ prevEntry);
							ref3.mapAlertTime.put(inputStructure.alertName, thisEntry);
						}
					}
				} else {
					ref3.mapAlertTime.put(inputStructure.alertName, inputStructure.timePortion);
				}
			}
		}
	}

	public static void enhanceData() throws Exception {
		for (InputStructure inputStructure : inputStructureData) {
			// split stocks
			inputStructure.distinctStockArray = new HashSet<String>();
			inputStructure.distinctStockArray.addAll(Arrays.asList(inputStructure.stocks.split(" ")));
			// parse date time
			System.out.println(inputStructure.triggeredAt);
			inputStructure.triggerAtCalendarObj = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(triggerTimespampFormat);
			inputStructure.triggerAtCalendarObj.setTime(sdf.parse(inputStructure.triggeredAt));
			inputStructure.triggerAtDateObj = inputStructure.triggerAtCalendarObj.getTime();
			SimpleDateFormat datePortionFormat = new SimpleDateFormat("dd-MM-yyyy");
			inputStructure.datePortion = datePortionFormat.format(inputStructure.triggerAtDateObj);
			SimpleDateFormat timePortionFormat = new SimpleDateFormat("HH:mm");
			inputStructure.timePortion = timePortionFormat.format(inputStructure.triggerAtDateObj);
			System.out.println(inputStructure.datePortion);
			System.out.println(inputStructure.timePortion);
		}
	}
	
	public static void readData() throws Exception {
		if (multiInput.equalsIgnoreCase("YES")) {
			File folder = new File(inputDirectoryPath);
			for (final File fileEntry : folder.listFiles()) {
		        if (! fileEntry.isDirectory()) {
		            readFile(fileEntry.getAbsolutePath());
		        }
		    }
		}
		else if (multiInput.equalsIgnoreCase("NO"))  {
			readFile(inputFilePath);
		}
	}

	public static void readFile(String fileName) throws Exception {
		FileInputStream fis = new FileInputStream(new File(fileName)); // obtaining input bytes from a file
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
						if (colCounter == inputColIndexForAlertName) {
							inputStructure.alertName = cell.getStringCellValue().trim().toUpperCase();
						}
						if (colCounter == inputColIndexForTriggerTimestamp) {
							inputStructure.triggeredAt = cell.getStringCellValue().trim();
						}
						if (colCounter == inputColIndexForStocks) {
							inputStructure.stocks = cell.getStringCellValue().trim().toUpperCase();
						}
						break;
					case NUMERIC: // field that represents number cell type
						break;
					default:
					}
					colCounter++;
				}
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
		multiInput = prop.getProperty("MULTI_INPUT");
		inputDirectoryPath = prop.getProperty("INPUT_DIRECTORY");
		inputFilePath = prop.getProperty("INPUT_FILE");
		inputSheetName = prop.getProperty("INPUT_SHEET");
		triggerTimespampFormat = prop.getProperty("TRIGGER_TIMESTAMP_FORMAT");
		outputFilePath = prop.getProperty("OUTPUT_FILE");
		outputSheetName = prop.getProperty("OUTPUT_SHEET");
		inputColIndexForAlertName = Integer.parseInt(prop.getProperty("INPUT_COLUMN_INDEX_FOR_ALERT_NAME"));
		inputColIndexForTriggerTimestamp = Integer.parseInt(prop.getProperty("INPUT_COLUMN_INDEX_FOR_TRIGGER_TIMESTAMP"));
		inputColIndexForStocks = Integer.parseInt(prop.getProperty("INPUT_COLUMN_INDEX_FOR_STOCKS"));
		universalAlertCount = Integer.parseInt(prop.getProperty("ALERT_COUNT"));
		for (int i=1; i<= universalAlertCount; i++) {
			universalAlertList.add(prop.getProperty("ALERT_"+i));
		}
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

class OutputStructure1 {
	public HashMap<String, OutputStructure2> mapDateStructure2;

	public OutputStructure1() {
		this.mapDateStructure2 = new HashMap<String, OutputStructure2>();
	}

	@Override
	public String toString() {
		return "OutputStructure1 [mapDateStructure2=" + mapDateStructure2 + "]";
	}
}

class OutputStructure2 {
	public TreeMap<String, OutputStructure3> mapStockStructure3;

	public OutputStructure2() {
		this.mapStockStructure3 = new TreeMap<String, OutputStructure3>();
	}

	@Override
	public String toString() {
		return "OutputStructure2 [mapStockStructure3=" + mapStockStructure3 + "]";
	}
}

class OutputStructure3 {
	public TreeMap<String, String> mapAlertTime;

	public OutputStructure3() {
		this.mapAlertTime = new TreeMap<String, String>();
	}

	@Override
	public String toString() {
		return "OutputStructure3 [mapAlertTime=" + mapAlertTime + "]";
	}
}