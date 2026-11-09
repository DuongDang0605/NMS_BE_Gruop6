package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.request.SystemAccountUpdateRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.dto.request.UserSearchRequestDto;
import com.mvc.nms_be.dto.request.ChangePasswordRequestDto;
import com.mvc.nms_be.dto.response.PageResponseDto;
import com.mvc.nms_be.service.SystemAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final SystemAccountService systemAccountService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public SystemAccountResponseDto me() { return systemAccountService.me(); }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
        systemAccountService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/verify")
    public SystemAccountResponseDto toggleVerify(@PathVariable @Positive Integer id) {
        return systemAccountService.toggleVerify(id);
    }

    @GetMapping
    public PageResponseDto<SystemAccountResponseDto> findAll(
            @Valid @ModelAttribute UserSearchRequestDto filter,
            @ParameterObject @PageableDefault(size = 10, sort = "accountId") Pageable pageable) {
        return PageResponseDto.from(systemAccountService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public SystemAccountResponseDto getById(@PathVariable @Positive Integer id) {
        return systemAccountService.getById(id);
    }

    @PostMapping
    public ResponseEntity<SystemAccountResponseDto> create(@Valid @RequestBody SystemAccountRequestDto request) {
        SystemAccountResponseDto result = systemAccountService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + result.getAccountId())).body(result);
    }

    @PutMapping("/{id}")
    public SystemAccountResponseDto update(@PathVariable @Positive Integer id,
                                          @Valid @RequestBody SystemAccountUpdateRequestDto request) {
        return systemAccountService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Integer id) {
        systemAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public SystemAccountResponseDto activate(@PathVariable @Positive Integer id) {
        return systemAccountService.activate(id);
    }
}
