package es.us.isa.idl.apigateway.filters;

import es.us.isa.idl.apigateway.util.CSVManager;
import es.us.isa.idlreasonerchoco.analyzer.Analyzer;
import es.us.isa.idlreasonerchoco.analyzer.OASAnalyzer;
import es.us.isa.idlreasonerchoco.configuration.IDLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;


import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@Component
public class IDLDetectionFilter extends AbstractGatewayFilterFactory<IDLDetectionFilter.Config> {
    final Logger logger =
            LoggerFactory.getLogger(IDLDetectionFilter.class);

    public IDLDetectionFilter() { super(Config.class); }

    @Override
  
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            try {
                String serviceName = null;
            	String SPEC_URL;
            	String operationPath;
            	String requestPath = exchange.getRequest().getPath().toString();
                String csvFilePath = "./src/test/resources/GatewayExperiment/ExecutionTime";
                String operationType = exchange.getRequest().getMethod().name().toLowerCase();
                Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

                if(requestPath.contains("businesses")) {
                    serviceName = "Yelp";
                	operationPath = "/businesses/search";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/YelpBusinessesSearch/openapi.yaml";
                    csvFilePath += "/YelpBusinessesSearch/IDLDetection.csv";
                }
                else if(requestPath.contains("transactions")) {
                    serviceName = "Yelp";
                    operationPath = "/transactions/{transaction_type}/search";
                    paramMap.put("transaction_type","delivery");
                    csvFilePath += "/YelpTransactionsSearch/IDLDetection.csv";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/YelpTransactionsSearch/openapi.yaml";
                }
                else if(requestPath.contains("folders")) {
                    serviceName = "Box";
                    operationPath = "/folders/{folder_id}/items";
                    String[] parts = requestPath.split("/");
                    String value = parts[3];
                    paramMap.put("folder_id",value);
                    csvFilePath += "/Box/IDLDetection.csv";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/Box/openapi.yaml";
                }
                else if(requestPath.contains("repos")) {
                    serviceName = "GitHub";
                    operationPath = "/user/repos";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/GitHub/openapi.yaml";
                    csvFilePath += "/GitHub/IDLDetection.csv";
                }
            /*    else if(paramMap.get("apikey").equals("9c07f6df")) {
                    serviceName = "OMDb";
                    operationPath = "/";
                    paramMap.remove("apikey");
                    csvFilePath += "/OMDb/IDLDetection.csv";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/OMDb/swagger_byIdOrTitle.yaml";
                }*/
                else if(requestPath.contains("places")) {
                    serviceName = "FSQ";
                    operationPath = "/places/search";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/Foursquare/openapi.yaml";
                    csvFilePath += "/FSQ/IDLDetection.csv";
                }
                else if(requestPath.contains("flight-offers")) {
                    serviceName = "AmadeusFlight";
                    csvFilePath += "/AmadeusFlight/IDLDetection.csv";
                	operationPath = "/shopping/flight-offers";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/AmadeusFlight/openapi.yaml";
                }
                else if(requestPath.contains("hotel-offers")) {
                    serviceName = "AmadeusHotel";
                    csvFilePath += "/AmadeusHotel/IDLDetection.csv";
                	operationPath = "/shopping/hotel-offers";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/AmadeusHotel/swagger.yaml";
                }
                else if(requestPath.contains("comics")) {
                	operationPath = "/v1/public/comics/{comicId}";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/Marvel/swagger_getComicById.yaml";
                }
                else if(requestPath.contains("youtube")) {
                	operationPath = "/youtube/v3/videos";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/YouTube/openapi.yaml";
                }
                else if(requestPath.contains("address")) {
                    serviceName = "DHL";
                    csvFilePath += "/DHL/IDLDetection.csv";
                    operationPath = "/find-by-address";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/DHL/openapi.yaml";
                }
                else if(requestPath.contains("blog")) {
                    serviceName = "Tumblr";
                    operationPath = "/blog/{blog-identifier}/likes";
                    paramMap.remove("api_key");
                    paramMap.put("blog-identifier","reasonerapigateway.tumblr.com");
                    csvFilePath += "/Tumblr/IDLDetection.csv";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/Tumblr/swagger.yaml";
                }
                else {
                	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path did not match!");
                }

                CSVManager csvManager = new CSVManager(csvFilePath, serviceName,operationPath,paramMap.toString());

                Long startTime = System.nanoTime();

                Analyzer analyzer = new OASAnalyzer(SPEC_URL, operationPath, operationType);
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

            } catch (IOException | IDLException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        };
    }

    public static class Config {
        // Put the configuration properties
    }
}