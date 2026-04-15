package com.smartcampus.backend.service;

import com.smartcampus.backend.entity.Resource;
import com.smartcampus.backend.enums.ResourceStatus;
import com.smartcampus.backend.enums.ResourceType;
import com.smartcampus.backend.exception.ResourceNotFoundException;
import com.smartcampus.backend.repository.ResourceRepository;
import com.smartcampus.backend.request.CreateResourceRequest;
import com.smartcampus.backend.request.UpdateResourceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    public Resource createResource(CreateResourceRequest request) {
        Resource resource = new Resource();
        resource.setResourceCode("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        resource.setName(request.getName());
        resource.setType(request.getType());
        resource.setLocation(request.getLocation());
        resource.setCapacity(request.getCapacity());
        resource.setDescription(request.getDescription());
        resource.setAvailabilityStart(request.getAvailabilityStart());
        resource.setAvailabilityEnd(request.getAvailabilityEnd());
        resource.setStatus(ResourceStatus.ACTIVE);
        return resourceRepository.save(resource);
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    public List<Resource> searchResources(ResourceType type, String location, Integer minCapacity, ResourceStatus status) {
        return resourceRepository.searchResources(type, location, minCapacity, status);
    }

    public Resource updateResource(Long id, UpdateResourceRequest request) {
        Resource resource = getResourceById(id);
        if (request.getName() != null) resource.setName(request.getName());
        if (request.getType() != null) resource.setType(request.getType());
        if (request.getLocation() != null) resource.setLocation(request.getLocation());
        if (request.getCapacity() != null) resource.setCapacity(request.getCapacity());
        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getStatus() != null) resource.setStatus(request.getStatus());
        if (request.getAvailabilityStart() != null) resource.setAvailabilityStart(request.getAvailabilityStart());
        if (request.getAvailabilityEnd() != null) resource.setAvailabilityEnd(request.getAvailabilityEnd());
        return resourceRepository.save(resource);
    }

    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        resourceRepository.deleteById(id);
    }
    //test
}
