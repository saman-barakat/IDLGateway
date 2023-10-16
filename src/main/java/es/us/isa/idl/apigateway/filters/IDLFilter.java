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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class IDLFilter extends AbstractGatewayFilterFactory<IDLFilter.Config> {
    final Logger logger =
            LoggerFactory.getLogger(IDLFilter.class);
    private static final String ANALYSIS = "analysis";
    public IDLFilter() {
        super(Config.class);
        logger.info("IDLFilter constructor");
    }

    private static final String BASE_SPEC_PATH = "./src/test/resources/GatewayExperiment";
    private static final String BASE_CSV_PATH = "./src/test/resources/GatewayExperiment/Performance";

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList(ANALYSIS);
    }
    @Override
    public GatewayFilter apply(Config config) {

        logger.info("IDLFilter is applied with [{}] analysis operation", config.analysis );

        return (exchange, chain) -> {

            Long startTime = System.nanoTime();

            String serviceName;
            String operationPath;
            String SPEC_URL;
            String csvFilePath;
            String requestPath = exchange.getRequest().getPath().toString();
            String operationType = exchange.getRequest().getMethod().name().toLowerCase();
            Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

            if(requestPath.contains("businesses")) {
                serviceName = "Yelp";
                operationPath = "/businesses/search";
                SPEC_URL = BASE_SPEC_PATH + "/YelpBusinessesSearch/openapi.yaml";
                csvFilePath = BASE_CSV_PATH + "/YelpBusinessesSearch.csv";
            }
            else if(requestPath.contains("transactions")) {
                serviceName = "Yelp";
                operationPath = "/transactions/{transaction_type}/search";
                paramMap.put("transaction_type","delivery");
                SPEC_URL = BASE_SPEC_PATH + "/YelpTransactionsSearch/openapi.yaml";
                csvFilePath = BASE_CSV_PATH + "/YelpTransactionsSearch.csv";
            }
            else if(requestPath.contains("folders")) {
                serviceName = "Box";
                operationPath = "/folders/{folder_id}/items";
                String[] parts = requestPath.split("/");
                String value = parts[3];
                paramMap.put("folder_id",value);
                SPEC_URL = BASE_SPEC_PATH + "/Box/openapi.yaml";
                csvFilePath = BASE_CSV_PATH + "/Box/IDLDetection.csv";

            }
            else if(requestPath.contains("repos")) {
                serviceName = "GitHub";
                operationPath = "/user/repos";
                SPEC_URL = BASE_SPEC_PATH + "/GitHub/openapi.yaml";
                csvFilePath = BASE_CSV_PATH + "/GitHub.csv";
            }
            else if(paramMap.get("apikey").equals("9c07f6df")) {
                serviceName = "OMDb";
                operationPath = "/";
                paramMap.remove("apikey");

                SPEC_URL = BASE_SPEC_PATH + "/swagger_byIdOrTitle.yaml";
                csvFilePath = BASE_CSV_PATH + "/OMDb/IDLDetection.csv";
            }
            else if(requestPath.contains("places")) {
                serviceName = "FSQ";
                operationPath = "/places/search";
                SPEC_URL = BASE_SPEC_PATH + "/Foursquare/openapi.yaml";
                csvFilePath = BASE_CSV_PATH + "/FSQ.csv";
            }
            else if(requestPath.contains("flight-offers")) {
                serviceName = "AmadeusFlight";
                operationPath = "/shopping/flight-offers";

                SPEC_URL = BASE_SPEC_PATH + "/AmadeusFlight/openapi.yaml";
                csvFilePath = BASE_CSV_PATH + "/AmadeusFlight.csv";
            }
            else if(requestPath.contains("hotel-offers")) {
                serviceName = "AmadeusHotel";
                operationPath = "/shopping/hotel-offers";

                SPEC_URL = BASE_SPEC_PATH + "/AmadeusHotel/swagger.yaml";
                csvFilePath = BASE_CSV_PATH + "/AmadeusHotel.csv";
            }
            else if(requestPath.contains("address")) {
                serviceName = "DHL";

                operationPath = "/find-by-address";
                SPEC_URL = BASE_SPEC_PATH + "/DHL/openapi.yaml";
                csvFilePath = BASE_CSV_PATH + "/DHL.csv";
            }
            else if(requestPath.contains("blog")) {
                serviceName = "Tumblr";
                operationPath = "/blog/{blog-identifier}/likes";
                paramMap.remove("api_key");
                paramMap.put("blog-identifier","reasonerapigateway.tumblr.com");

                SPEC_URL = BASE_SPEC_PATH + "/Tumblr/swagger.yaml";
                csvFilePath = BASE_CSV_PATH + "/Tumblr.csv";
            }
            else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path did not match!");
            }

            CSVManager csvManager = new CSVManager(csvFilePath, serviceName,operationPath,paramMap.toString());

            try {
                double analysisTime;
             //   Long analysisStartTime = System.nanoTime();

                if (config.analysis.equals("Detection") || config.analysis.equals("Explanation")) {

                Analyzer analyzer = new OASAnalyzer(SPEC_URL, operationPath, operationType);
                boolean valid = analyzer.isValidRequest(paramMap);

                if (!valid) {

                    String responseMessage;

                    if ("Explanation".equalsIgnoreCase(config.analysis))
                        responseMessage = analyzer.getExplanationMessage(paramMap).toString();
                    else
                        responseMessage = "The request is invalid!";

                    Long analysisEndTime = System.nanoTime();

                    analysisTime = (analysisEndTime - startTime)/ 1000000.0;

                    double totalTime = analysisTime;

                    csvManager.writeRow(
                            HttpStatus.BAD_REQUEST.toString(),
                            config.analysis, analysisTime,
                            0.0,totalTime);

                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, responseMessage);
                }
                else {
                    Long analysisEndTime = System.nanoTime();
                    analysisTime = (analysisEndTime - startTime)/ 1000000.0;
                }
            }else {
                analysisTime = 0.0;
                }

                Long serverResponseStartTime = System.nanoTime();

                return chain.filter(exchange).then(Mono.fromRunnable(() -> {


                    Long serverResponseEndTime = System.nanoTime();
                    double serverResponseTime = (serverResponseEndTime - serverResponseStartTime)/ 1000000.0;

                    double totalTime = (serverResponseEndTime - startTime)/ 1000000.0;

                    csvManager.writeRow(exchange.getResponse().getStatusCode().toString(),
                            config.analysis,analysisTime,serverResponseTime,totalTime);

                }));

            } catch (IDLException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }

        };
    }

    public static class Config {
        private String analysis;

        public String getAnalysis() {
            return analysis;
        }

        public void setAnalysis(String analysis) {
            this.analysis = analysis;
        }
    }
}