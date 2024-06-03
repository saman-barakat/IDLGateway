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

import java.util.Map;

@Component
public class IDLFilter extends AbstractGatewayFilterFactory<IDLFilter.Config> {
    final Logger logger = LoggerFactory.getLogger(IDLFilter.class);

    public IDLFilter() {
        super(Config.class);
        logger.info("IDLFilter constructor");
    }

    private static final String BASE_SPEC_PATH = "./src/test/resources/GatewayExperiment";
    private static final String BASE_CSV_PATH = "./src/test/resources/GatewayExperiment/Performance";

    @Override
    public GatewayFilter apply(Config config) {

        logger.info("IDLFilter is applied with [{}] analysis operation", config.analysis);

        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getPath().toString();


            Long startTime = System.nanoTime();

            String serviceName = config.serviceName;
            String operationPath = config.operationPath;
            String SPEC_URL = BASE_SPEC_PATH + config.specPath;
            String csvFilePath = BASE_CSV_PATH + config.csvPath;
            String operationType = exchange.getRequest().getMethod().name().toLowerCase();
            Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

            CSVManager csvManager = new CSVManager(csvFilePath, serviceName, operationPath, paramMap.toString());

            try {
                double analysisTime;

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
                        analysisTime = (analysisEndTime - startTime) / 1000000.0;

                        csvManager.writeRow(HttpStatus.BAD_REQUEST.toString(), config.analysis, analysisTime, 0.0);

                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, responseMessage);
                    } else {
                        Long analysisEndTime = System.nanoTime();
                        analysisTime = (analysisEndTime - startTime) / 1000000.0;
                    }
                } else {
                    analysisTime = 0.0;
                }

                Long serverResponseStartTime = System.nanoTime();

                return chain.filter(exchange)

                        .then(Mono.fromRunnable(() -> {

                            Long serverResponseEndTime = System.nanoTime();
                            double serverResponseTime = (serverResponseEndTime - serverResponseStartTime) / 1000000.0;

                            double totalTime = (serverResponseEndTime - startTime) / 1000000.0;

                            csvManager.writeRow(exchange.getResponse().getStatusCode().toString(), config.analysis, analysisTime, serverResponseTime);

                        }));

            } catch (IDLException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }



        };
    }

    public static class Config {
        private String analysis;
        private String specPath;
        private String csvPath;
        private String serviceName;

        public String getOperationPath() {
            return operationPath;
        }

        public void setOperationPath(String operationPath) {
            this.operationPath = operationPath;
        }

        private String operationPath;

        public String getSpecPath() {
            return specPath;
        }

        public void setSpecPath(String specPath) {
            this.specPath = specPath;
        }

        public String getCsvPath() {
            return csvPath;
        }

        public void setCsvPath(String csvPath) {
            this.csvPath = csvPath;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getAnalysis() {
            return analysis;
        }

        public void setAnalysis(String analysis) {
            this.analysis = analysis;
        }
    }
}