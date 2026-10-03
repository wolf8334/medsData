package com.xhr.medsdata.service;

import com.xhr.medsdata.common.BusinessException;
import com.xhr.medsdata.domain.Drug;
import com.xhr.medsdata.dto.Requests.DrugReq;
import com.xhr.medsdata.repository.DrugRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DrugService {

    private final DrugRepository drugRepository;

    public DrugService(DrugRepository drugRepository) {
        this.drugRepository = drugRepository;
    }

    public List<Drug> list(String status) {
        return drugRepository.findAll(status);
    }

    public Drug get(long id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new BusinessException("药品不存在"));
    }

    public long create(DrugReq req) {
        validate(req);
        return drugRepository.insert(req);
    }

    public void update(long id, DrugReq req) {
        validate(req);
        get(id);
        drugRepository.update(id, req);
    }

    public void changeStatus(long id, String status) {
        get(id);
        drugRepository.updateStatus(id, "ENABLED".equalsIgnoreCase(status) ? "ENABLED" : "DISABLED");
    }

    private void validate(DrugReq req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BusinessException("药品名称不能为空");
        }
        if (req.stockUnit() == null || req.stockUnit().isBlank()) {
            throw new BusinessException("库存最小单位不能为空");
        }
        if (req.packSize() != null && req.packSize().signum() <= 0) {
            throw new BusinessException("每包装数量必须大于 0");
        }
    }
}
