package es.us.isa.idl.idlgateway.filters;




import lombok.extern.java.Log;
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
@Log
public class IDLValidationFilter extends AbstractGatewayFilterFactory<IDLValidationFilter.Config> {

    private static final String SPEC_URL = "./src/test/resources/Yelp/openapi.yaml";

    private final WebClient.Builder webClientBuilder;

    public IDLValidationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override

    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            try {

                String operationPath = exchange.getRequest().getPath().subPath(2).toString();
                String operationType = exchange.getRequest().getMethodValue().toLowerCase();
                Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

                Analyzer analyzer = null;

                    analyzer = new OASAnalyzer("oas", SPEC_URL, operationPath, operationType, false);

                boolean valid = analyzer.isValidRequest(paramMap);

                if (!valid) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The Request is invalid!");
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