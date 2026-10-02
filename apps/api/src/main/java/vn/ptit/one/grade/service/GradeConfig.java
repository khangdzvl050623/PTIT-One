package vn.ptit.one.grade.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.ptit.one.grade.policy.GradePolicy;

@Configuration
@EnableConfigurationProperties(GradeProperties.class)
class GradeConfig {

    @Bean
    GradePolicy gradePolicy(GradeProperties properties) {
        return new GradePolicy(properties.trongSoChuyenCan(), properties.trongSoGiuaKy(),
                properties.trongSoCuoiKy(), properties.nguongDat());
    }
}
