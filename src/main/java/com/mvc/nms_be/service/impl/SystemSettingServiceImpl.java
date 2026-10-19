package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.SystemSettingRequestDto;
import com.mvc.nms_be.dto.response.SystemSettingResponseDto;
import com.mvc.nms_be.entity.SystemSetting;
import com.mvc.nms_be.mapper.SystemSettingMapper;
import com.mvc.nms_be.repository.SystemSettingRepository;
import com.mvc.nms_be.service.SystemSettingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

}
