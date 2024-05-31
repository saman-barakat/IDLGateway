package es.us.isa.idl.apigateway;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Configuration
public class ProxyConfiguration {

    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return httpClient -> httpClient.proxy(proxy ->
                proxy.type(ProxyProvider.Proxy.HTTP)
                        .host("proxy.int.local") // Replace with your proxy host
                        .port(3128)        // Replace with your proxy port
        );
    }
}

