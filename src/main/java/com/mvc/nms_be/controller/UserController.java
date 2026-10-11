package com.mvc.nms_be.controller;

import com.mvc.nms_be.service.SystemAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SystemAccountService systemAccountService;
}
