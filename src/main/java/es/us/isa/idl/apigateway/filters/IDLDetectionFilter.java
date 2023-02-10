package es.us.isa.idl.apigateway.filters;

import es.us.isa.idl.apigateway.util.CSVManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import idlanalyzer.analyzer.Analyzer;
import idlanalyzer.analyzer.OASAnalyzer;
import idlanalyzer.configuration.IDLException;
import reactor.core.publisher.Mono;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Component
public class IDLDetectionFilter extends AbstractGatewayFilterFactory<IDLDetectionFilter.Config> {
    final Logger logger =
            LoggerFactory.getLogger(IDLDetectionFilter.class);

 private String serviceName;


    public IDLDetectionFilter() { super(Config.class); }

    @Override
  
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            try {

                String serviceName = null;
            	String SPEC_URL = null;
            	String operationPath = null;
            	String requestPath = exchange.getRequest().getPath().toString();
                String csvFilePath = "./src/test/resources/GatewayExperiment/ExecutionTime";

                if(requestPath.contains("businesses")) {
                    serviceName = "Yelp";
                	operationPath = "/businesses/search";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/Yelp/openapi.yaml";
                    csvFilePath += "/Yelp/IDLDetection.csv";
                }
                else if(requestPath.contains("flight-offers")) {
                	operationPath = "/shopping/flight-offers";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/AmadeusFlight/swagger.yaml";
                }
                else if(requestPath.contains("hotel-offers")) {
                	operationPath = "/shopping/hotel-offers";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/AmadeusHotel/swagger.yaml";
                }
                else if(requestPath.contains("comics")) {
                	operationPath = "/v1/public/comics/{comicId}";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/Marvel/swagger_getComicById.yaml";
                }
                else if(requestPath.contains("omdbapi")) {
                	operationPath = "/";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/OMDb/swagger_byIdOrTitle.yaml";
                }
                else if(requestPath.contains("youtube")) {
                	operationPath = "/youtube/v3/videos";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/YouTube/openapi.yaml";
                }
                else if(requestPath.contains("address")) {
                    operationPath = "/find-by-address";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/DHL/openapi.yaml";
                }
                else {
                	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path did not match!");
                }

                String operationType = exchange.getRequest().getMethodValue().toLowerCase();
                Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

                CSVManager csvManager = new CSVManager(csvFilePath, serviceName,operationPath,paramMap.toString());

                Long startTime = System.nanoTime();

                Analyzer analyzer = null;
                analyzer = new OASAnalyzer("oas", SPEC_URL, operationPath, operationType, false);
                boolean valid = analyzer.isValidRequest(paramMap);

                if (!valid) {

                    Long endTime = System.nanoTime();
                    double timeElapsedMs = (endTime - startTime)/ 1000000.0;
                    csvManager.writeRow(timeElapsedMs,HttpStatus.BAD_REQUEST.toString());

                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The request is invalid!");
                }

                return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                    Long endTime = System.nanoTime();
                    double timeElapsedMs = (endTime - startTime)/ 1000000.0;
                    try {
                        csvManager.writeRow(timeElapsedMs,exchange.getResponse().getStatusCode().toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }));

            } catch (IDLException | IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        };
    }

    public static class Config {
        // Put the configuration properties
    }
}