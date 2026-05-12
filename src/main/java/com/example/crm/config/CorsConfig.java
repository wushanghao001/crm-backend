// package com.example.crm.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// import org.springframework.web.filter.CorsFilter;

// import java.util.Arrays;
// import java.util.List;

// @Configuration
// public class CorsConfig {

//     @Bean
//     public CorsFilter corsFilter() {
//         CorsConfiguration config = new CorsConfiguration();
        
//         config.setAllowedOriginPatterns(List.of("*"));
//         config.setAllowedHeaders(Arrays.asList("*"));
//         config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//         config.setAllowCredentials(true);
//         config.setMaxAge(3600L);
        
//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", config);
        
//         return new CorsFilter(source);
//     }
// }
package com.example.crm.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.Arrays;
import java.util.List;
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源跨域（适配手机端各种请求场景，自用完全安全）
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许所有请求头，避免手机端登录请求（带账号密码）被拦截
        config.setAllowedHeaders(Arrays.asList("*"));
        // 允许登录所需的POST请求及其他常用请求方式，补充OPTIONS请求（避免预请求失败）
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许携带凭证（登录时的cookie/令牌），手机端跨域必备
        config.setAllowCredentials(true);
        // 跨域请求缓存时间，减少重复验证，提升手机端登录速度
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效，覆盖登录接口，确保手机端请求能通过
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 