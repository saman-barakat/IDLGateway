# IDLFilter for Spring Cloud Gateway  

## Overview  

`IDLFilter` is a custom filter for **Spring Cloud Gateway**, designed to handle **inter-parameter dependencies** in web APIs. It ensures that API requests comply with constraints defined in an **OpenAPI Specification (OAS) file**, applying specific analysis modes:  

- **Detection** – Identifies constraint violations.  
- **Explanation** – Provides reasons for violations.  
- **None** – Disables validation.  

This document provides instructions on configuring and applying `IDLFilter` in **Spring Cloud Gateway**, using an example service that interacts with the **Yelp API**.  

---

## How IDLFilter Works  

`IDLFilter` extends `AbstractGatewayFilterFactory`, integrating `IDLReasoner` to validate API requests in three steps:  

1. **Initialize the Analyzer** – Create an `IDLReasoner` instance with the **OAS file**, **operation path**, and **operation type**.  
2. **Validate Requests** – Check if the request meets the defined constraints.
3. **Generate Responses** – Return a response message if the request is invalid.

---

## Prerequisites  

Before using `IDLFilter`, ensure the following dependencies are included in your project:  

- **Java Development Kit (JDK) 11 or later**  
- **IDLReasoner 1.0.1**  
- **Spring Boot & Spring Cloud Gateway dependencies**  

---

## Simplified IDLFilter Code  

Below is an example implementation of `IDLFilter`:  

```java
@Component
public class IDLFilter extends AbstractGatewayFilterFactory<IDLFilter.Config> {
    private final Logger logger = LoggerFactory.getLogger(IDLFilter.class);

    public IDLFilter() {
        super(Config.class);
        logger.info("IDLFilter initialized");
    }

    private static final String BASE_SPEC_PATH = "./src/test/resources/GatewayExperiment";

    @Override
    public GatewayFilter apply(Config config) {
        logger.info("Applying IDLFilter with [{}] analysis mode", config.analysis);

        return (exchange, chain) -> {
            String operationPath = config.operationPath;
            String SPEC_URL = BASE_SPEC_PATH + config.specPath;
            String operationType = exchange.getRequest().getMethod().name().toLowerCase();
            Map<String, String> paramMap = exchange.getRequest().getQueryParams().toSingleValueMap();

            try {
                if ("Detection".equals(config.analysis) || "Explanation".equals(config.analysis)) {
                    // Step 1: Initialize the Analyzer
                    Analyzer analyzer = new OASAnalyzer(SPEC_URL, operationPath, operationType);

                    // Step 2: Validate the Request
                    boolean isValid = analyzer.isValidRequest(paramMap);

                    // Step 3: Generate the Response if Invalid
                    if (!isValid) {
                        String responseMessage = "The request is invalid!";
                        if ("Explanation".equalsIgnoreCase(config.analysis)) {
                            responseMessage = analyzer.getExplanationMessage(paramMap).toString();
                        }
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, responseMessage);
                    }
                }
            } catch (IDLException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
            return chain.filter(exchange);
        };
    }

    public static class Config {
        private String analysis;
        private String specPath;
        private String operationPath;

        // Getters and setters
        public String getOperationPath() { return operationPath; }
        public void setOperationPath(String operationPath) { this.operationPath = operationPath; }

        public String getSpecPath() { return specPath; }
        public void setSpecPath(String specPath) { this.specPath = specPath; }

        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
    }
}
```
## Configuration  

To apply `IDLFilter` in **Spring Cloud Gateway**, update your `application.yaml` file with the appropriate route configuration.  

### Step-by-Step Instructions  

1. Navigate to `src/main/resources` in your Spring Boot project.  
2. Open (or create) `application.yaml`.  
3. Add a route configuration using the following structure:  

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: <UniqueRouteID>
          uri: <TargetServiceURI>
          predicates:
            - Path=<RequestPathPattern>
          filters:
            - name: IDLFilter
              args:
                analysis: <AnalysisMode>  # Options: Detection, Explanation, None
                serviceName: "<ServiceName>"
                operationPath: "<OperationPath>"
                specPath: "<SPECPath>"
```

### Configuration Parameters  

| Parameter       | Description |
|----------------|------------|
| **id**         | Unique identifier for the route (`<UniqueRouteID>`). |
| **uri**        | Target service URI (`<TargetServiceURI>`). |
| **predicates** | Defines request matching conditions (routes only `<RequestPathPattern>`). |
| **filters**    | Specifies the filter to be applied (`IDLFilter`). |
| **analysis**   | Analysis mode (`Detection`, `Explanation`, or `None`). |
| **operationPath** | API operation path (`<OperationPath>`). |
| **specPath** | Path to the OpenAPI specification file (`<SPECPath>`). |


### Example: Yelp Transactions Search  

Below is an example configuration for applying `IDLFilter` to the **Yelp Transactions Search** API:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: YelpTransactionsSearch
          uri: https://api.yelp.com
          predicates:
            - Path=/v3/transactions/**
          filters:
            - name: IDLFilter
              args:
                analysis: Explanation  # Options: Detection, Explanation, None
                serviceName: "YelpTransactionsSearch"
                operationPath: "/transactions/delivery/search"
                specPath: "/YelpBusinessesSearch/openapi.yaml"
```

