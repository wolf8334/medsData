package com.xhr.medsdata.service;

import com.xhr.medsdata.common.BusinessException;
import com.xhr.medsdata.domain.Drug;
import com.xhr.medsdata.dto.Requests.DrugReq;
import com.xhr.medsdata.repository.DrugRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DrugService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION");

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
        long id = drugRepository.insert(req);
        opLog.info("新增药品: id={}, name={}, 规格={}, 单位={}", id, req.name(), req.specification(), req.stockUnit());
        return id;
    }

    public void update(long id, DrugReq req) {
        validate(req);
        get(id);
        drugRepository.update(id, req);
        opLog.info("修改药品: id={}, name={}, 规格={}, 单位={}", id, req.name(), req.specification(), req.stockUnit());
    }

    public void changeStatus(long id, String status) {
        get(id);
        String target = "ENABLED".equalsIgnoreCase(status) ? "ENABLED" : "DISABLED";
        drugRepository.updateStatus(id, target);
        opLog.info("{}药品: id={}", "ENABLED".equals(target) ? "启用" : "停用", id);
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
