package es.us.isa.idl.apigateway.filters;

import es.us.isa.idl.apigateway.util.CSVManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@Component
public class NoDetectionFilter extends AbstractGatewayFilterFactory<NoDetectionFilter.Config> {
    final Logger logger =
            LoggerFactory.getLogger(NoDetectionFilter.class);

    public NoDetectionFilter() { super(Config.class); }

    @Override
  
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            try {
                String serviceName ;
            	String operationPath;
            	String requestPath = exchange.getRequest().getPath().toString();
                String csvFilePath = "./src/test/resources/GatewayExperiment/ExecutionTime";

                Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

                if(requestPath.contains("businesses")) {
                    serviceName = "Yelp";
                	operationPath = "/businesses/search";
                    csvFilePath += "/YelpBusinessesSearch/NoDetection.csv";
                }
                else if(requestPath.contains("transactions")) {
                    serviceName = "Yelp";
                    operationPath = "/transactions/{transaction_type}/search";
                    csvFilePath += "/YelpTransactionsSearch/NoDetection.csv";
                }
                else if(requestPath.contains("folders")) {
                    serviceName = "Box";
                    operationPath = "/folders/{folder_id}/items";
                    csvFilePath += "/Box/NoDetection.csv";
                }
                else if(requestPath.contains("repos")) {
                    serviceName = "GitHub";
                    operationPath = "/user/repos";
                    csvFilePath += "/GitHub/NoDetection.csv";
                }
        /*        else if(paramMap.get("apikey").equals("9c07f6df")) {
                    serviceName = "OMDb";
                    operationPath = "/";
                    csvFilePath += "/OMDb/NoDetection.csv";
                   // System.out.println("No: test");
                }*/
                else if(requestPath.contains("places")) {
                    serviceName = "FSQ";
                    operationPath = "/places/search";
                    csvFilePath += "/FSQ/NoDetection.csv";
                }
                else if(requestPath.contains("flight-offers")) {
                    serviceName = "AmadeusFlight";
                	operationPath = "/shopping/flight-offers";
                    csvFilePath += "/AmadeusFlight/NoDetection.csv";
                }
                else if(requestPath.contains("hotel-offers")) {
                    serviceName = "AmadeusHotel";
                	operationPath = "/shopping/hotel-offers";
                    csvFilePath += "/AmadeusHotel/NoDetection.csv";
                }
                else if(requestPath.contains("address")) {
                    serviceName = "DHL";
                    operationPath = "/find-by-address";
                    csvFilePath += "/DHL/NoDetection.csv";
                }
                else if(requestPath.contains("blog")) {
                    serviceName = "Tumblr";
                    operationPath = "/blog/{blog-identifier}/likes";
                    csvFilePath += "/Tumblr/NoDetection.csv";
                }
                else {
                	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path did not match!");
                }

                CSVManager csvManager = new CSVManager(csvFilePath, serviceName,operationPath,paramMap.toString());

                Long startTime = System.nanoTime();

                return chain.filter(exchange).then(Mono.fromRunnable(() -> {

                    Long endTime = System.nanoTime();
                    double timeElapsedMs = (endTime - startTime)/ 1000000.0;
                    try {
                        csvManager.writeRow(timeElapsedMs, exchange.getResponse().getStatusCode().toString());
                    } catch (IOException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                    }
                }));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }

        };
    }

    public static class Config {
        // Put the configuration properties
    }
}