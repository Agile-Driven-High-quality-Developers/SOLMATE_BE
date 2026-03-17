package org.solmate.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			// Swagger Authorize 버튼 누르면 모든 요청에 Bearer 헤더 자동 포함
			.info(new Info()
				.title("Solmate Backend API")
				.description("프로디지털아카데미 Solmate 프로젝트의 백엔드 API 문서입니다.")
				.version("1.0.0")
				.contact(new Contact()
					.name("Solmate Dev Team")
					.email("team.solmate@example.com"))
				.license(new License()
					.name("Apache License 2.0")
					.url("https://www.apache.org/licenses/LICENSE-2.0.html")));
	}
}
