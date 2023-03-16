package es.us.isa.idl.apigateway.filters;

import java.io.IOException;
import java.util.Map;

import es.us.isa.idl.apigateway.util.CSVManager;
import es.us.isa.idlreasonerchoco.analyzer.Analyzer;
import es.us.isa.idlreasonerchoco.analyzer.OASAnalyzer;
import es.us.isa.idlreasonerchoco.configuration.IDLException;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@Component
public class IDLDetectionAndExplanationFilter extends AbstractGatewayFilterFactory<IDLDetectionAndExplanationFilter.Config> {

    public IDLDetectionAndExplanationFilter() { super(Config.class); }
    @Override
  
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            try {

                String serviceName = null;
                String SPEC_URL;
                String operationPath;
                String requestPath = exchange.getRequest().getPath().toString();
                String csvFilePath = "./src/test/resources/GatewayExperiment/ExecutionTime";
                String operationType = exchange.getRequest().getMethod().toString().toLowerCase();
                Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

                if(requestPath.contains("businesses")) {
                    serviceName = "Yelp";
                    operationPath = "/businesses/search";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/YelpBusinessesSearch/openapi.yaml";
                    csvFilePath += "/YelpBusinessesSearch/IDLDetectionAndExplanation.csv";
                }
                else if(requestPath.contains("transactions")) {
                    serviceName = "Yelp";
                    operationPath = "/transactions/{transaction_type}/search";
                    paramMap.put("transaction_type","delivery");
                    csvFilePath += "/YelpTransactionsSearch/IDLDetectionAndExplanation.csv";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/YelpTransactionsSearch/openapi.yaml";
                }
                else if(requestPath.contains("folders")) {
                    serviceName = "Box";
                    operationPath = "/folders/{folder_id}/items";
                    String[] parts = requestPath.split("/");
                    String value = parts[3];
                    paramMap.put("folder_id",value);
                    csvFilePath += "/Box/IDLDetectionAndExplanation.csv";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/Box/openapi.yaml";
                }
                else if(requestPath.contains("repos")) {
                    serviceName = "GitHub";
                    operationPath = "/user/repos";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/GitHub/openapi.yaml";
                    csvFilePath += "/GitHub/IDLDetectionAndExplanation.csv";
                }
           /*     else if(paramMap.get("apikey").equals("9c07f6df")) {
                    serviceName = "OMDb";
                    operationPath = "/";
                    paramMap.remove("apikey");
                    csvFilePath += "/OMDb/IDLDetectionAndExplanation.csv";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/OMDb/swagger_byIdOrTitle.yaml";
                }*/
                else if(requestPath.contains("places")) {
                    serviceName = "FSQ";
                    operationPath = "/places/search";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/Foursquare/openapi.yaml";
                    csvFilePath += "/FSQ/IDLDetectionAndExplanation.csv";
                }
                else if(requestPath.contains("flight-offers")) {
                    serviceName = "AmadeusFlight";
                    csvFilePath += "/AmadeusFlight/IDLDetectionAndExplanation.csv";
                    operationPath = "/shopping/flight-offers";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/AmadeusFlight/openapi.yaml";
                }
                else if(requestPath.contains("hotel-offers")) {
                    serviceName = "AmadeusHotel";
                    csvFilePath += "/AmadeusHotel/IDLDetectionAndExplanation.csv";
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
                    serviceName = "DHL";
                    csvFilePath += "/DHL/IDLDetectionAndExplanation.csv";
                    operationPath = "/find-by-address";
                    SPEC_URL = "./src/test/resources/GatewayExperiment/DHL/openapi.yaml";
                }
                else if(requestPath.contains("blog")) {
                    serviceName = "Tumblr";
                    operationPath = "/blog/{blog-identifier}/likes";
                    paramMap.remove("api_key");
                    csvFilePath += "/Tumblr/IDLDetectionAndExplanation.csv";
                    paramMap.put("blog-identifier","reasonerapigateway.tumblr.com");
                    SPEC_URL = "./src/test/resources/GatewayExperiment/Tumblr/swagger.yaml";
                }
                else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path did not matchX!");
                }

                CSVManager csvManager = new CSVManager(csvFilePath, serviceName,operationPath,paramMap.toString());

                Long startTime = System.nanoTime();

                Analyzer analyzer = new OASAnalyzer(SPEC_URL, operationPath, operationType);
                boolean valid = analyzer.isValidRequest(paramMap);

                if (!valid) {
                    String explanation = analyzer.getRequestExplanation(paramMap).toString();

                    Long endTime = System.nanoTime();
                    double timeElapsedMs = (endTime - startTime)/ 1000000.0;
                    csvManager.writeRow(timeElapsedMs,HttpStatus.BAD_REQUEST.toString());

                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, explanation);
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