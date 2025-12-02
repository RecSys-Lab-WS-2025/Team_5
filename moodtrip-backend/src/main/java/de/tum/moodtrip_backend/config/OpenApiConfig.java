package de.tum.moodtrip_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置类
 * 用于生成 API 文档
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moodtripOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Moodtrip 后端 API 文档")
                        .version("1.0.0")
                        .description("Moodtrip：基于情绪感知的智能推荐系统 API 文档\n\n" +
                                "## 功能模块\n" +
                                "- **对话管理**: 创建和管理用户对话\n" +
                                "- **情绪分析**: 使用 AI 分析用户情绪\n" +
                                "- **路线推荐**: 基于情绪、天气和位置的智能推荐\n" +
                                "- **音乐推荐**: Spotify 音乐推荐\n" +
                                "- **用户管理**: 用户信息管理")
                        .contact(new Contact()
                                .name("Moodtrip Team")
                                .email("team@moodtrip.de")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地开发服务器"),
                        new Server()
                                .url("https://api.moodtrip.de")
                                .description("生产服务器")
                ));
    }
}
