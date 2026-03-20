package org.solmate.external.ls;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ls.api")
public class LsProperties {

    private String baseUrl;
    private String wsUrl;
    private String appKey;
    private String appSecret;
}
