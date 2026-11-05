package org.example.bookweb.schedule;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.entity.Book;
import org.example.bookweb.entity.BorrowRecord;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.mapper.BorrowRecordMapper;
import org.example.bookweb.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 到期提醒定时任务（v0.6，PRD 2.1 锁定 @Scheduled，禁止 Quartz/xxl-job）：
 * 每天 09:00（Asia/Shanghai）扫描 due_at 距今不足 3 天的 active 记录 → 生成站内提醒通知；
 * 同书同人当日去重：提醒文案只含用户/书名且措辞确定，同人同书同日内容一致，以 content+当日判重。
 * 验收手工触发方式：OverdueScanJobTest 直接调用 scanDueSoon()（不修改线上 cron）。
 */
@Component
@RequiredArgsConstructor
public class OverdueScanJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueScanJob.class);

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final NotificationMapper notificationMapper;

    /** 可独立调用（验收/测试手工触发）；@Scheduled 每天 09:00 自动执行 */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Shanghai")
    public int scanDueSoon() {
        List<BorrowRecord> dueSoon = borrowRecordMapper.findDueSoon();
        int created = 0;
        for (BorrowRecord record : dueSoon) {
            Book book = bookMapper.findById(record.getBookId());
            String title = book == null ? ("#" + record.getBookId()) : book.getTitle();
            String content = "您借阅的《" + title + "》将在 3 天内到期，请及时归还";
            if (notificationMapper.countTodayByUserAndContent(record.getUserId(), content) > 0) {
                continue;
            }
            notificationMapper.insert(record.getUserId(), content);
            created++;
        }
        if (created > 0) {
            log.info("OverdueScanJob 生成到期提醒 {} 条（扫描 {} 条在借记录）", created, dueSoon.size());
        }
        return created;
    }
}
