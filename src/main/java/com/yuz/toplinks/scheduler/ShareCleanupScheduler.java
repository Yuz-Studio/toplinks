package com.yuz.toplinks.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yuz.toplinks.service.ShareService;

@Component
public class ShareCleanupScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(ShareCleanupScheduler.class);
    
    private final ShareService shareService;
    
    public ShareCleanupScheduler(ShareService shareService) {
        this.shareService = shareService;
    }
    
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpired() {
        int count = shareService.cleanupExpiredShares();
        if (count > 0) {
            log.info("Cleaned up {} expired shares", count);
        }
    }
    
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupMaxDownloads() {
        int count = shareService.cleanupMaxDownloadsReached();
        if (count > 0) {
            log.info("Cleaned up {} shares with max downloads reached", count);
        }
    }
}
