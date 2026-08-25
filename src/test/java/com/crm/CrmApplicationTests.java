package com.crm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 脚手架冒烟测试。
 *
 * <p>仅验证主启动类存在且可加载，不启动完整 Spring 上下文，
 * 因此无需数据库连接即可运行 {@code mvn test}。
 */
class CrmApplicationTests {

    @Test
    void mainApplicationClassShouldBeLoadable() {
        assertThat(CrmApplication.class).isNotNull();
    }
}
