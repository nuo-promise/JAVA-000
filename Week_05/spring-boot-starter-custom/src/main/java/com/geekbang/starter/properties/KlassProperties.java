package com.geekbang.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by yandex on 2020/11/19.
 */
@Data
@ConfigurationProperties(prefix = "spring.custom.klass")
public class KlassProperties {
    private Integer port;
}
