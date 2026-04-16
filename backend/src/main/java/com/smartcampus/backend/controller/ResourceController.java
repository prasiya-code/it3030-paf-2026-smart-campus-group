package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Resource;
import com.smartcampus.backend.enums.ResourceStatus;
import com.smartcampus.backend.enums.ResourceType;
import com.smartcampus.backend.request.CreateResourceRequest;
import com.smartcampus.backend.request.UpdateResourceRequest;
import com.smartcampus.backend.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping
    public ResponseEntity<Resource> createResource(@Valid @RequestBody CreateResourceRequest request,
                                                   @AuthenticationPrincipal OAuth2User principal) {
        Resource resource = resourceService.createResource(request);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Resource>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Resource>> searchResources(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) ResourceStatus status) {

        return ResponseEntity.ok(resourceService.searchResources(type, location, minCapacity, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Resource> updateResource(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateResourceRequest request) {
        return ResponseEntity.ok(resourceService.updateResource(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id,
                                               @AuthenticationPrincipal OAuth2User principal) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
