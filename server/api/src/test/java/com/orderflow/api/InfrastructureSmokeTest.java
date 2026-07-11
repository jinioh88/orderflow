package com.orderflow.api;

import com.orderflow.api.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class InfrastructureSmokeTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void MySQL에_연결되고_테스트_전용_DB를_사용한다() {
        String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        assertThat(database).isEqualTo("orderflow_test");
    }
}
