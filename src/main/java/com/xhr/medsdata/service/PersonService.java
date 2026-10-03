package com.xhr.medsdata.service;

import com.xhr.medsdata.common.BusinessException;
import com.xhr.medsdata.domain.Person;
import com.xhr.medsdata.dto.Requests.PersonReq;
import com.xhr.medsdata.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION");

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
        long id = personRepository.insert(req);
        opLog.info("新增用药人: id={}, name={}", id, req.name());
        return id;
    }

    public void update(long id, PersonReq req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        get(id);
        personRepository.update(id, req);
        opLog.info("修改用药人: id={}, name={}", id, req.name());
    }

    public void changeStatus(long id, String status) {
        get(id);
        String target = "ENABLED".equalsIgnoreCase(status) ? "ENABLED" : "DISABLED";
        personRepository.updateStatus(id, target);
        opLog.info("{}用药人: id={}", "ENABLED".equals(target) ? "启用" : "停用", id);
    }
}
