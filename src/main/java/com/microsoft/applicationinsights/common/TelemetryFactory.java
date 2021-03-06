/*
 * ApplicationInsights-Docker
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.common;

import com.microsoft.applicationinsights.contracts.ContainerStateEvent;
import com.microsoft.applicationinsights.contracts.ContainerStatsMetric;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.Map;

/**
 * Created by yonisha on 8/12/2015.
 */
public class TelemetryFactory {

    // region Public

    public Telemetry createEventTelemetry(ContainerStateEvent stateEvent) {
        EventTelemetry telemetry = new EventTelemetry(stateEvent.getName());

        if (!StringUtils.isNullOrEmpty(stateEvent.getInstrumentationKey())) {
            telemetry.getContext().setInstrumentationKey(stateEvent.getInstrumentationKey());
        }

        // Setting operation in order to be able to correlate events related to the same container.
        String containerId = stateEvent.getProperties().get(com.microsoft.applicationinsights.common.Constants.DOCKER_CONTAINER_ID_PROPERTY_KEY);
        telemetry.getContext().getOperation().setId(containerId);
        telemetry.getContext().getOperation().setName(stateEvent.getName());

        telemetry.getProperties().putAll(stateEvent.getProperties());

        return telemetry;
    }

    public Telemetry createMetricTelemetry(ContainerStatsMetric containerStatsMetric) {
        Telemetry telemetry;
        String metricName = containerStatsMetric.getMetricName();

        // If the given metric is one of the build-in PC in Ibiza, we track it as a performance counter telemetry.
        // Otherwise the given metric is sent as a custom metric.
        if (metricName.equalsIgnoreCase(com.microsoft.applicationinsights.internal.perfcounter.Constants.CPU_PC_COUNTER_NAME) || metricName.equalsIgnoreCase(com.microsoft.applicationinsights.internal.perfcounter.Constants.TOTAL_MEMORY_PC_COUNTER_NAME)) {
            telemetry = createPerformanceCounterTelemetry(containerStatsMetric);
        } else {
            MetricTelemetry metricTelemetry = new MetricTelemetry(metricName, containerStatsMetric.getValue());
            metricTelemetry.setMin(containerStatsMetric.getMin());
            metricTelemetry.setMax(containerStatsMetric.getMax());
            metricTelemetry.setCount(containerStatsMetric.getCount());
            metricTelemetry.setStandardDeviation(containerStatsMetric.getStdDev());

            telemetry = metricTelemetry;
        }

        Map<String, String> properties = telemetry.getProperties();
        properties.put(com.microsoft.applicationinsights.common.Constants.DOCKER_HOST_PROPERTY_KEY, containerStatsMetric.getDockerHost());
        properties.put(com.microsoft.applicationinsights.common.Constants.DOCKER_IMAGE_PROPERTY_KEY, containerStatsMetric.getDockerImage());
        properties.put(com.microsoft.applicationinsights.common.Constants.DOCKER_CONTAINER_NAME_PROPERTY_KEY, containerStatsMetric.getDockerContainerName());
        properties.put(com.microsoft.applicationinsights.common.Constants.DOCKER_CONTAINER_ID_PROPERTY_KEY, containerStatsMetric.getDockerContainerId());

        return telemetry;
    }

    private PerformanceCounterTelemetry createPerformanceCounterTelemetry(ContainerStatsMetric containerStatsMetric) {
        PerformanceCounterTelemetry performanceCounterTelemetry = null;

        String metricName = containerStatsMetric.getMetricName();
        if (metricName.equalsIgnoreCase(com.microsoft.applicationinsights.internal.perfcounter.Constants.CPU_PC_COUNTER_NAME)) {
            performanceCounterTelemetry = new PerformanceCounterTelemetry(
                    com.microsoft.applicationinsights.internal.perfcounter.Constants.TOTAL_CPU_PC_CATEGORY_NAME,
                    com.microsoft.applicationinsights.internal.perfcounter.Constants.CPU_PC_COUNTER_NAME,
                    com.microsoft.applicationinsights.internal.perfcounter.Constants.INSTANCE_NAME_TOTAL,
                    containerStatsMetric.getValue());
        } else if (metricName.equalsIgnoreCase(com.microsoft.applicationinsights.internal.perfcounter.Constants.TOTAL_MEMORY_PC_COUNTER_NAME)) {
            performanceCounterTelemetry = new PerformanceCounterTelemetry(
                    com.microsoft.applicationinsights.internal.perfcounter.Constants.TOTAL_MEMORY_PC_CATEGORY_NAME,
                    com.microsoft.applicationinsights.internal.perfcounter.Constants.TOTAL_MEMORY_PC_COUNTER_NAME,
                    "",
                    containerStatsMetric.getValue());
        }

        return performanceCounterTelemetry;
    }

    // endregion Public
}
