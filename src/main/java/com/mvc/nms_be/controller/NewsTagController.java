package com.mvc.nms_be.controller;

import com.mvc.nms_be.service.NewsTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news-tags")
@RequiredArgsConstructor
public class NewsTagController {

    private final NewsTagService newsTagService;
}
