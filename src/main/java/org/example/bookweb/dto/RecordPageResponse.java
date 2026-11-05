package org.example.bookweb.dto;

import lombok.Data;
import org.example.bookweb.entity.BorrowRecord;

import java.util.List;

/** /api/records 分页响应（v0.4）：data: {total, page, size, list}，total 为筛选后全量 */
@Data
public class RecordPageResponse {

    private int total;
    private int page;
    private int size;
    private List<BorrowRecord> list;

    public static RecordPageResponse of(int total, int page, int size, List<BorrowRecord> list) {
        RecordPageResponse r = new RecordPageResponse();
        r.total = total;
        r.page = page;
        r.size = size;
        r.list = list;
        return r;
    }
}
