package com.aidenholmes.backstock.config;

import com.aidenholmes.backstock.catalog.ProductListingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ProductListingInterceptor())
                .addPathPatterns("/api/v1/products")
                .includeHttpMethods(HttpMethod.GET);
    }
}
