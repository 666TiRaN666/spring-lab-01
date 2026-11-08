package kz.iitu.springlab.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(

        @NotBlank String owner,

        @NotBlank String group,

        @Valid Mail mail,

        @Valid Cache cache) {

    public record Mail(
            @NotBlank @Email String from,
            @Min(1) @Max(10) @DefaultValue("3") int retryCount,
            @DefaultValue("5s") Duration timeout,
            @DefaultValue("true") boolean enabled) {
    }

    // Variant 7: app.cache
    public record Cache(
            @NotNull @DefaultValue("10m") Duration ttl,
            @Min(10) @Max(10000) @DefaultValue("1000") int maxEntries) {
    }
}
