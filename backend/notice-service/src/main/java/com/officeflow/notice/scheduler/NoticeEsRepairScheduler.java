package com.officeflow.notice.scheduler;

import com.officeflow.notice.service.NoticeSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeEsRepairScheduler {

    private final NoticeSearchService noticeSearchService;

    @Scheduled(cron = "0 */30 * * * ?")
    public void repairEsData() {
        log.debug("Running ES data repair for notice...");
        int count = noticeSearchService.repairRecent(35);
        if (count > 0) {
            log.info("ES repair completed: {} notice documents synced", count);
        }
    }
}
