package com.xhr.medsdata.controller;

import com.xhr.medsdata.common.ApiResponse;
import com.xhr.medsdata.domain.Person;
import com.xhr.medsdata.dto.Requests.PersonReq;
import com.xhr.medsdata.service.PersonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public ApiResponse<List<Person>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(personService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Person> get(@PathVariable long id) {
        return ApiResponse.ok(personService.get(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody PersonReq req) {
        return ApiResponse.ok(personService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody PersonReq req) {
        personService.update(id, req);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable long id, @RequestParam String status) {
        personService.changeStatus(id, status);
        return ApiResponse.ok();
    }
}
