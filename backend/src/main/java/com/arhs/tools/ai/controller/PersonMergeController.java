package com.arhs.tools.ai.controller;
import com.arhs.tools.ai.dto.DuplicateDtos.AuditView;
import com.arhs.tools.ai.service.DuplicateManagementService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/person-merges")
public class PersonMergeController {
    private final DuplicateManagementService service;
    public PersonMergeController(DuplicateManagementService service) { this.service = service; }
    @GetMapping("/{id}") public AuditView get(@PathVariable long id) { return service.audit(id); }
}
