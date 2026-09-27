package com.notevault.searchworker;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("worker.sqs")
public record WorkerSqsProperties(
        @NotBlank String queueUrl,
        @NotBlank String region,
        @Min(1) @Max(16) @DefaultValue("2") int concurrency
) { }
