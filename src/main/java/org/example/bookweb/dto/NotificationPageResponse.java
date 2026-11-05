package org.example.bookweb.dto;

import lombok.Data;
import org.example.bookweb.entity.Notification;

import java.util.List;

/** /api/notifications 分页响应（v0.6）：data: {total, page, size, list}，list 未读优先 */
@Data
public class NotificationPageResponse {

    private int total;
    private int page;
    private int size;
    private List<Notification> list;

    public static NotificationPageResponse of(int total, int page, int size, List<Notification> list) {
        NotificationPageResponse r = new NotificationPageResponse();
        r.total = total;
        r.page = page;
        r.size = size;
        r.list = list;
        return r;
    }
}
