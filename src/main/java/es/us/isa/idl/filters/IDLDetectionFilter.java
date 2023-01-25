package es.us.isa.idl.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import idlanalyzer.analyzer.Analyzer;
import idlanalyzer.analyzer.OASAnalyzer;
import idlanalyzer.configuration.IDLException;

import java.util.Map;

@Component
public class IDLDetectionFilter extends AbstractGatewayFilterFactory<IDLDetectionFilter.Config> {
  

    public IDLDetectionFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
    }

    @Override
  
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            try {
            	String SPEC_URL = null;
            	String operationPath = null;
            	String requestPath = exchange.getRequest().getPath().toString();
 
                if(requestPath.indexOf("businesses") > -1) {
                	operationPath = "/businesses/search";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/Yelp/swagger.yaml";
                }
                else if(requestPath.indexOf("flight-offers") > -1) {
                	operationPath = "/shopping/flight-offers";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/AmadeusFlight/swagger.yaml";
                }
                else if(requestPath.indexOf("hotel-offers") > -1) {
                	operationPath = "/shopping/hotel-offers";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/AmadeusHotel/swagger.yaml";
                }
                else if(requestPath.indexOf("comics") > -1) {
                	operationPath = "/v1/public/comics/{comicId}";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/Marvel/swagger_getComicById.yaml";
                }
                else if(requestPath.indexOf("omdbapi") > -1) {
                	operationPath = "/";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/OMDb/swagger_byIdOrTitle.yaml";
                }
                else if(requestPath.indexOf("youtube") > -1) {
                	operationPath = "/youtube/v3/videos";
                	SPEC_URL = "./src/test/resources/GatewayExperiment/YouTube/openapi.yaml";
                }

                else {
                	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path did not match!");
                }
                
                String operationType = exchange.getRequest().getMethodValue().toLowerCase();
                Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();
                Analyzer analyzer = null;

                analyzer = new OASAnalyzer("oas", SPEC_URL, operationPath, operationType, false);
                    
                boolean valid = analyzer.isValidRequest(paramMap);

                if (!valid) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The request is invalid!");
                }

                return chain.filter(exchange);
                
            } catch (IDLException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        };
    }

    public static class Config {
        // Put the configuration properties
    }
}