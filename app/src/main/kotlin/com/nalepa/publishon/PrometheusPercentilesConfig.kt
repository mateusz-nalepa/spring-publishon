package com.nalepa.publishon

import io.micrometer.core.instrument.Meter.Id
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class PrometheusPercentilesConfig(
    private val meterRegistry: MeterRegistry,
) {
    @PostConstruct
    fun configurePercentiles() {
        meterRegistry
            .config()
            .meterFilter(object : MeterFilter {

                override fun configure(id: Id, config: DistributionStatisticConfig): DistributionStatisticConfig? {
                    return DistributionStatisticConfig
                        .builder()
                        .percentiles(0.99, 0.999)
                        .build()
                        .merge(config)
                }
            })
    }

}
