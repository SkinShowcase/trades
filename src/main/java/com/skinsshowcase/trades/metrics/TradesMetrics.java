package com.skinsshowcase.trades.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Метрики trades: операции с набором предметов и лентой обмена.
 */
@Component
public class TradesMetrics {

    private final MeterRegistry registry;

    public TradesMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordSelectionOperation(String operation) {
        registry.counter("trades.selection.operations",
                "operation", operation).increment();
    }
}
