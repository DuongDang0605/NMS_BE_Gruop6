package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.request.SystemAccountUpdateRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.dto.request.UserSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface SystemAccountService {

    List<SystemAccountResponseDto> findAll();

    Page<SystemAccountResponseDto> search(@NotNull @Valid UserSearchRequestDto filter, @NotNull Pageable pageable);

    SystemAccountResponseDto getById(@NotNull @Positive Integer id);

    SystemAccountResponseDto create(@NotNull @Valid SystemAccountRequestDto request);

    SystemAccountResponseDto update(@NotNull @Positive Integer id,
                                   @NotNull @Valid SystemAccountUpdateRequestDto request);

    void delete(@NotNull @Positive Integer id);
}
