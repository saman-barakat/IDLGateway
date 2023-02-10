package es.us.isa.idl.apigateway.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;


public class CSVManager {

	private static final Logger logger = LogManager.getLogger(CSVManager.class.getName());
	private static String csvFilePath;
	private static String serviceName;
	private static String operationPath;
	private static String params;

	static String[] Header = {"Service", "Operation", "RequestParams", "StartTime", "DurationMS", "Status"};

	public CSVManager(String csvFilePath, String serviceName, String operationPath, String params) {
		this.csvFilePath = csvFilePath;
		this.serviceName = serviceName;
		this.operationPath = operationPath;
		this.params = params;
	}


// create a method to write a row
	public static void writeRow(double durationMS, String status) throws IOException {

		File file = new File(csvFilePath);

		FileWriter fileWriter = new FileWriter(file, true);

		CSVPrinter csvPrinter = null;
		// check file size

		if(file.exists() && file.length() != 0) {
			csvPrinter = new CSVPrinter(fileWriter, CSVFormat.EXCEL);
		}else{
			csvPrinter = new CSVPrinter(fileWriter, CSVFormat.EXCEL
					.withHeader(Header));
		}

		csvPrinter.printRecord(serviceName, operationPath, params, new Date(), durationMS, status);

		csvPrinter.flush();
		csvPrinter.close();
}
}