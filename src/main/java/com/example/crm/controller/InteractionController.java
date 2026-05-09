package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.InteractionDTO;
import com.example.crm.dto.PageResult;
import com.example.crm.entity.Interaction;
import com.example.crm.service.InteractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<InteractionDTO>>> getInteractions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId) {
        PageResult<InteractionDTO> result = interactionService.getInteractions(page, size, keyword, type, operator, status, customerId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InteractionDTO>> getById(@PathVariable Long id) {
        InteractionDTO interaction = interactionService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(interaction));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<Interaction>>> getByCustomerId(@PathVariable Long customerId) {
        List<Interaction> interactions = interactionService.getByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(interactions));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Interaction>> createInteraction(@RequestBody Interaction interaction) {
        Interaction created = interactionService.createInteraction(interaction);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Interaction>> updateInteraction(@PathVariable Long id, @RequestBody Interaction interaction) {
        Interaction updated = interactionService.updateInteraction(id, interaction);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInteraction(@PathVariable Long id) {
        interactionService.deleteInteraction(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}