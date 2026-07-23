package com.officeflow.flow.scheduler;

import com.officeflow.flow.service.FlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowScheduler {

    private final FlowService flowService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void autoRejectOverdueApplies() {
        log.debug("Running auto-reject overdue applies...");
        int count = flowService.autoRejectOverdueApplies();
        if (count > 0) {
            log.info("Auto rejected {} overdue approval applications", count);
        }
    }
}
