package com.nalepa.publishon

import io.micrometer.core.instrument.observation.MeterObservationHandler
import io.micrometer.observation.Observation
import org.springframework.stereotype.Component

@Component
class RequestObservationHandler : MeterObservationHandler<Observation.Context> {

    override fun onStart(context: Observation.Context) {
        if (context.name == "http.server.requests") {
            AppLogger.log("OBSERVATION HANDLER: Request started")
        }
    }

    override fun onStop(context: Observation.Context) {
        if (context.name == "http.server.requests") {
            AppLogger.log("OBSERVATION HANDLER: Request stopped")
        }
    }
}