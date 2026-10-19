package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.request.SystemAccountUpdateRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.service.SystemAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final SystemAccountService systemAccountService;

    @GetMapping
    public List<SystemAccountResponseDto> findAll() {
        return systemAccountService.findAll();
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
}
