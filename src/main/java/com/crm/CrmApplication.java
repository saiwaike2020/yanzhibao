package com.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * CRM 系统主启动类。
 *
 * <p>通过 {@code @SpringBootApplication} 开启 Spring Boot 自动配置，
 * 当前脚手架已集成：Web、Security、Data JPA、Validation、PostgreSQL，
 * 以及 LangChain4j（LLM / RAG）与 PGVector 向量检索能力。
 * {@code @EnableAsync} 开启异步处理（文件上传后异步处理，v3.8）。
 */
@SpringBootApplication
@EnableAsync
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
