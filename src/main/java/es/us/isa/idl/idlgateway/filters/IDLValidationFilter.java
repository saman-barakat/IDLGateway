package es.us.isa.idl.idlgateway.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.idl.idlgateway.model.RequestValidation;
import lombok.extern.java.Log;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Component
@Log
public class IDLValidationFilter extends AbstractGatewayFilterFactory<IDLValidationFilter.Config> {
    private static final String idlValidationURL = "http://localhost:8082/api/validation";
    private static final String OPERATION_PATH = "operationPath";
    private static final String OPERATION_TYPE = "operationType";
    private static final String PARAMETERS = "parameters";
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

                ObjectMapper mapper = new ObjectMapper();
                Map paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();
                String parameters = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(paramMap);

                URI uri = new URIBuilder(idlValidationURL)
                        .addParameter(OPERATION_PATH, operationPath)
                        .addParameter(OPERATION_TYPE, operationType)
                        .addParameter(PARAMETERS, parameters)
                        .build();

                HttpClient httpClient = HttpClient.newBuilder().build();

                HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri(uri)
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                log.info("StatusCode: " + response.statusCode());
                log.info("Response: " + response.body());

                if(response.statusCode()!= HttpStatus.OK.value())
                    throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()));

                RequestValidation requestValidation
                        = mapper.readValue(response.body(), RequestValidation.class);

                if (requestValidation.getValid().booleanValue() == false) {
                    throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
                }

                return chain.filter(exchange);

            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        };


    }


    public static class Config {
        // Put the configuration properties
    }
}