package com.officeflow.flow.scheduler;

import com.officeflow.flow.service.FlowSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowEsRepairScheduler {

    private final FlowSearchService flowSearchService;

    /**
     * 每 30 分钟执行一次 ES 数据修复，同步最近 35 分钟的变更（留 5 分钟冗余）
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void repairEsData() {
        log.debug("Running ES data repair...");
        int count = flowSearchService.repairRecent(35);
        if (count > 0) {
            log.info("ES repair completed: {} documents synced", count);
        }
    }
}
