package com.xhr.medsdata.service;

import com.xhr.medsdata.common.BusinessException;
import com.xhr.medsdata.common.Period;
import com.xhr.medsdata.domain.Drug;
import com.xhr.medsdata.domain.Plan;
import com.xhr.medsdata.domain.TakeRecord;
import com.xhr.medsdata.domain.Views.PlanItemView;
import com.xhr.medsdata.dto.Requests.TakeReq;
import com.xhr.medsdata.dto.Requests.TakeUpdateReq;
import com.xhr.medsdata.repository.DrugRepository;
import com.xhr.medsdata.repository.PersonRepository;
import com.xhr.medsdata.repository.PlanItemRepository;
import com.xhr.medsdata.repository.TakeRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TakeRecordService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION");

    private final TakeRecordRepository takeRecordRepository;
    private final PlanService planService;
    private final PersonRepository personRepository;
    private final PlanItemRepository planItemRepository;
    private final DrugRepository drugRepository;
    private final StockService stockService;

    public TakeRecordService(TakeRecordRepository takeRecordRepository, PlanService planService,
                             PersonRepository personRepository, PlanItemRepository planItemRepository,
                             DrugRepository drugRepository, StockService stockService) {
        this.takeRecordRepository = takeRecordRepository;
        this.planService = planService;
        this.personRepository = personRepository;
        this.planItemRepository = planItemRepository;
        this.drugRepository = drugRepository;
        this.stockService = stockService;
    }

    /**
     * 记录服药：关联当天生效的方案；同一用药人同一天同一时段默认只能有一条有效记录。
     * 同时按方案明细自动扣减库存（写入 USE 流水）。
     */
    @Transactional
    public long take(TakeReq req) {
        if (req == null || req.personId() == null) {
            throw new BusinessException("用药人不能为空");
        }
        if (!personRepository.existsById(req.personId())) {
            throw new BusinessException("用药人不存在");
        }
        Period period = Period.of(req.period());
        LocalDate takeDate = req.takeDate() == null ? LocalDate.now() : req.takeDate();
        Plan plan = planService.effectivePlan(req.personId(), takeDate)
                .orElseThrow(() -> new BusinessException("该日期没有生效的用药方案，无法记录"));
        takeRecordRepository.findActive(req.personId(), takeDate, period.name()).ifPresent(r -> {
            throw new BusinessException("该用药人在 " + takeDate + " " + period.label() + " 已记录，请勿重复");
        });
        LocalDateTime takenAt = req.takenAt() == null ? LocalDateTime.now().withNano(0) : req.takenAt();
        long id = takeRecordRepository.insert(req.personId(), plan.id(), takeDate, period.name(), takenAt, req.remark());
        adjustStock(plan.id(), period.name(), true, "服药自动扣减");
        opLog.info("记录服药: id={}, personId={}, 日期={}, 时段={}, planId={}",
                id, req.personId(), takeDate, period.name(), plan.id());
        return id;
    }

    public TakeRecord get(long id) {
        return takeRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用药记录不存在"));
    }

    public List<TakeRecord> listByDate(LocalDate date) {
        return takeRecordRepository.findByDate(date == null ? LocalDate.now() : date);
    }

    public List<TakeRecord> listByPersonRange(long personId, LocalDate from, LocalDate to) {
        return takeRecordRepository.findByPersonAndRange(personId, from, to);
    }

    /**
     * 撤销服药，并把当时扣减的库存加回。
     */
    @Transactional
    public void cancel(long id) {
        TakeRecord record = get(id);
        if ("CANCELLED".equals(record.status())) {
            throw new BusinessException("该记录已撤销");
        }
        takeRecordRepository.cancel(id);
        adjustStock(record.planId(), record.period(), false, "撤销服药回补");
        opLog.info("撤销服药: id={}, personId={}, 日期={}, 时段={}",
                id, record.personId(), record.takeDate(), record.period());
    }

    @Transactional
    public void update(long id, TakeUpdateReq req) {
        TakeRecord record = get(id);
        LocalDateTime takenAt = req == null || req.takenAt() == null ? record.takenAt() : req.takenAt();
        String remark = req == null || req.remark() == null ? record.remark() : req.remark();
        takeRecordRepository.update(id, takenAt, remark);
        opLog.info("修改服药记录: id={}, 实际时间={}", id, takenAt);
    }

    /**
     * 删除服药记录，若原为已服用则把库存加回。
     */
    @Transactional
    public void delete(long id) {
        TakeRecord record = get(id);
        takeRecordRepository.delete(id);
        if ("TAKEN".equals(record.status())) {
            adjustStock(record.planId(), record.period(), false, "删除记录回补");
        }
        opLog.info("删除服药记录: id={}, personId={}, 日期={}, 时段={}",
                id, record.personId(), record.takeDate(), record.period());
    }

    /**
     * 按方案明细调整库存。仅当明细的服药单位与药品最小单位一致时才扣/补，避免错误换算。
     */
    private void adjustStock(long planId, String period, boolean deduct, String remark) {
        List<PlanItemView> items = planItemRepository.findViews(planId);
        for (PlanItemView item : items) {
            if (!period.equals(item.period())) {
                continue;
            }
            Drug drug = drugRepository.findById(item.drugId()).orElse(null);
            if (drug == null || item.doseUnit() == null || drug.stockUnit() == null) {
                continue;
            }
            if (!item.doseUnit().equalsIgnoreCase(drug.stockUnit())) {
                continue;
            }
            BigDecimal delta = deduct ? item.dose().negate() : item.dose();
            stockService.applyChange(drug.id(), delta, deduct ? "USE" : "REVERT", remark);
        }
    }
}
