package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Contact;
import com.example.crm.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Contact>>> listContacts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId) {

        PageResponse<Contact> response = contactService.listContacts(pageNum, pageSize, keyword, customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Contact>> getContact(@PathVariable Long id) {
        Contact contact = contactService.getContactById(id);
        if (contact == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "联系人不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<PageResponse<Contact>>> getByCustomerId(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageResponse<Contact> response = contactService.getByCustomerId(customerId, pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Contact>> createContact(@RequestBody Contact contact) {
        Contact created = contactService.createContact(contact);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Contact>> updateContact(@PathVariable Long id, @RequestBody Contact contact) {
        Contact updated = contactService.updateContact(id, contact);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}