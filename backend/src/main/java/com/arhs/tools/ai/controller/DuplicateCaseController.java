package com.arhs.tools.ai.controller;

import com.arhs.tools.ai.domain.DuplicateCase;
import com.arhs.tools.ai.dto.DuplicateDtos.*;
import com.arhs.tools.ai.service.DuplicateManagementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/duplicate-cases")
public class DuplicateCaseController {
    private final DuplicateManagementService service;
    public DuplicateCaseController(DuplicateManagementService service) { this.service = service; }
    @PostMapping("/scan") public ScanResult scan() { return service.scan(); }
    @GetMapping public CasePage list(@RequestParam(defaultValue = "OPEN") DuplicateCase.Status status,
                                    @RequestParam(required = false) DuplicateCase.Confidence confidence,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return service.list(status, confidence, page, size);
    }
    @GetMapping("/{id}") public CaseDetail detail(@PathVariable long id) { return service.detail(id); }
    @PostMapping("/{id}/dismiss") public CaseDetail dismiss(@PathVariable long id, @RequestBody DismissRequest request) {
        return service.dismiss(id, request);
    }
    @PostMapping("/{id}/merge") public AuditView merge(@PathVariable long id, @RequestBody MergeRequest request) {
        return service.merge(id, request);
    }
}
