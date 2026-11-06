package com.example.ms_account_reservation.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "logging.filter")
public class LoggingProperties {

    // Заголовки, которые не должны попасть в лог по умолчанию. Properties полностью заменяет этот список
    private List<String> ignoredHeaders = List.of("Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization");

    private int maxBodyLength = 16384; // в байтах

    // Тоже по умолчанию, properties заменяет
    @NotEmpty
    private List<String> includePaths = List.of("/**");
}
