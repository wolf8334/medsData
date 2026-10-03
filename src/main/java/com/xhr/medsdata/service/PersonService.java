package com.xhr.medsdata.service;

import com.xhr.medsdata.common.BusinessException;
import com.xhr.medsdata.domain.Person;
import com.xhr.medsdata.dto.Requests.PersonReq;
import com.xhr.medsdata.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<Person> list(String status) {
        return personRepository.findAll(status);
    }

    public Person get(long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用药人不存在"));
    }

    public long create(PersonReq req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        return personRepository.insert(req);
    }

    public void update(long id, PersonReq req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        get(id);
        personRepository.update(id, req);
    }

    public void changeStatus(long id, String status) {
        get(id);
        personRepository.updateStatus(id, "ENABLED".equalsIgnoreCase(status) ? "ENABLED" : "DISABLED");
    }
}
