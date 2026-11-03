package com.siva.jobmanager.config;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringContext {
    private static ApplicationContext context;
    public SpringContext(ApplicationContext applicationContext) { context = applicationContext; }
    public static <T> T getBean(Class<T> type) { return context.getBean(type); }
    public static ApplicationContext getContext() { return context; }
}
