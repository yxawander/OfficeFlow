package com.officeflow.notice.scheduler;

import com.officeflow.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeScheduler {

    private final NoticeService noticeService;

    @Scheduled(cron = "0 * * * * ?")
    public void autoPublishNotices() {
        log.debug("Running scheduled notice publisher...");
        int count = noticeService.autoPublishScheduledNotices();
        if (count > 0) {
            log.info("Auto published {} scheduled notices", count);
        }
    }
}
