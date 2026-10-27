package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.TrendingArticleDto;
import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> overview();

    Map<String, Object> news();

    Map<String, Object> notifications(Integer recipientId);

    /** Thống kê engagement: tổng view, like, top 5 trending bài viết. */
    Map<String, Object> engagement();
}
