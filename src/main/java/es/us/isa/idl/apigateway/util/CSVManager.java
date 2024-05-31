package es.us.isa.idl.apigateway.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CSVManager {


	private static final Logger logger = LogManager.getLogger(CSVManager.class.getName());
	static String[] Header = {"API", "Operation", "RequestParams", "ResponseStatus","DateAndTime", "DependencyFilterMode", "AnalysisTime","ServerResponseTime"};

	private final String csvFilePath;
	private final String serviceName;
	private final String operationPath;
	private final String params;
	// private final String requestID;
	//private final String errorMessage;
	private final String dateAndTime;

	public CSVManager(String csvFilePath, String serviceName, String operationPath, String params) {
		this.csvFilePath = csvFilePath;
		this.serviceName = serviceName;
		this.operationPath = operationPath;
		this.params = params;
		//this.requestID = requestID;
		//set current date and time
		this.dateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	public void writeRow(String status, String analysis, double analysisTime, double serverResponseTime) {
		File file = new File(csvFilePath);
		try (
				FileWriter fileWriter = new FileWriter(file, true);
				CSVPrinter csvPrinter = file.exists() && file.length() > 0
						? new CSVPrinter(fileWriter, CSVFormat.EXCEL)
						: new CSVPrinter(fileWriter, CSVFormat.EXCEL.withHeader(Header))
		)
		 {
			csvPrinter.printRecord(serviceName, operationPath, params, status, dateAndTime, analysis, analysisTime, serverResponseTime);
			csvPrinter.flush();
		} catch (IOException e) {
			logger.error("Failed to write to CSV file", e);
		}
	}
}