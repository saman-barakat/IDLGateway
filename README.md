# IDLFilter for Spring Cloud Gateway

## Overview

IDLFilter is a custom filter for Spring Cloud Gateway, specifically designed to manage inter-parameter dependencies in web APIs.


## Overview

This document provides instructions on how to apply the IDLFilter in Spring Cloud Gateway. The IDLFilter is used to apply specific analysis (Detection, Explanation, None) to the service requests and responses passing through the gateway. In this example, we demonstrate how to configure the filter for a service that interfaces with the Yelp API.

## Prerequisites

- Java Development Kit (JDK) 11 or later
- IDLReasoner 1.0.1.
- Spring Boot and Spring Cloud dependencies included in your project


## Configuration

To apply the IDLFilter in Spring Cloud Gateway, you need to update your `application.yaml` configuration file. Below are the detailed steps to configure the filter for the Yelp Transactions Search service.

### Step-by-Step Instructions

1. Open your `application.yaml` file in your Spring Boot project's `src/main/resources` directory.

2. Add the route configuration for the Yelp Transactions Search service as shown below:

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
```

### Configuration Parameters

- **id**: A unique identifier for the route. In this example, it is set to `YelpTransactionsSearch`.
- **uri**: The URI of the target service. Here, it points to `https://api.yelp.com`.
- **predicates**: Conditions that the request must meet for this route to be selected. The `Path` predicate ensures that only requests to `/v3/transactions/**` are routed.
- **filters**: Specifies the filters to be applied to the requests and responses. 
  - **name**: The name of the filter. In this case, it is `IDLFilter`.
  - **args**: Arguments for the filter:
    - **analysis**: Specifies the type of analysis to be applied. Options include `Detection`, `Explanation`, and `None`. For this example, it is set to `Explanation`.
    - **serviceName**: The name of the service being routed. It is set to `YelpTransactionsSearch`.
    - **operationPath**: The specific operation path for the service. It is set to `/transactions/delivery/search`.

## Example

The following example demonstrates how the configuration appears in the `application.yaml` file:

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
                analysis: Explanation
                serviceName: "YelpTransactionsSearch"
                operationPath: "/transactions/delivery/search"
```

By following these steps, you can successfully apply the IDLFilter to the Yelp Transactions Search route in your Spring Cloud Gateway application.
 

