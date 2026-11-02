package com.mvc.nms_be.service;

import java.util.Map;

public interface DashboardService {

    Map<String, Object> overview();

    Map<String, Object> news();

    Map<String, Object> notifications(Integer recipientId);

    // thống kê engagement: tổng view, like, top 5 trending bài viết
    Map<String, Object> engagement();
}
