package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface SystemAccountService {

    List<SystemAccountResponseDto> findAll();

    SystemAccountResponseDto getById(@NotNull @Positive Integer id);

    SystemAccountResponseDto create(@NotNull @Valid SystemAccountRequestDto request);

    SystemAccountResponseDto update(@NotNull @Positive Integer id,
                                   @NotNull @Valid SystemAccountRequestDto request);

    void delete(@NotNull @Positive Integer id);
}
