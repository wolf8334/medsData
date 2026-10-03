package com.xhr.medsdata.service;

import com.xhr.medsdata.common.BusinessException;
import com.xhr.medsdata.common.Period;
import com.xhr.medsdata.domain.Plan;
import com.xhr.medsdata.domain.Views.PlanDetail;
import com.xhr.medsdata.domain.Views.PlanItemView;
import com.xhr.medsdata.dto.Requests.NewVersionReq;
import com.xhr.medsdata.dto.Requests.PlanItemReq;
import com.xhr.medsdata.dto.Requests.PlanReq;
import com.xhr.medsdata.repository.DrugRepository;
import com.xhr.medsdata.repository.PersonRepository;
import com.xhr.medsdata.repository.PlanItemRepository;
import com.xhr.medsdata.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanItemRepository planItemRepository;
    private final PersonRepository personRepository;
    private final DrugRepository drugRepository;

    public PlanService(PlanRepository planRepository, PlanItemRepository planItemRepository,
                       PersonRepository personRepository, DrugRepository drugRepository) {
        this.planRepository = planRepository;
        this.planItemRepository = planItemRepository;
        this.personRepository = personRepository;
        this.drugRepository = drugRepository;
    }

    public List<PlanDetail> listByPerson(long personId) {
        if (!personRepository.existsById(personId)) {
            throw new BusinessException("用药人不存在");
        }
        LocalDate today = LocalDate.now();
        List<PlanDetail> result = new ArrayList<>();
        for (Plan plan : planRepository.findByPersonId(personId)) {
            result.add(new PlanDetail(withDerivedStatus(plan, today), planItemRepository.findViews(plan.id())));
        }
        return result;
    }

    public PlanDetail getDetail(long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("用药方案不存在"));
        return new PlanDetail(withDerivedStatus(plan, LocalDate.now()), planItemRepository.findViews(planId));
    }

    public Optional<Plan> effectivePlan(long personId, LocalDate date) {
        return planRepository.findEffective(personId, date);
    }

    public PlanDetail effectiveDetail(long personId, LocalDate date) {
        return planRepository.findEffective(personId, date)
                .map(p -> new PlanDetail(withDerivedStatus(p, LocalDate.now()), planItemRepository.findViews(p.id())))
                .orElse(null);
    }

    /**
     * 新建方案。版本号自动递增；若方案已生效则关闭旧版本（设置失效日期），历史版本保留。
     */
    @Transactional
    public long create(PlanReq req) {
        if (req == null || req.personId() == null) {
            throw new BusinessException("用药人不能为空");
        }
        if (!personRepository.existsById(req.personId())) {
            throw new BusinessException("用药人不存在");
        }
        validateItems(req.items());
        LocalDate today = LocalDate.now();
        LocalDate effectiveFrom = req.effectiveFrom() == null ? today : req.effectiveFrom();
        int versionNo = planRepository.maxVersion(req.personId()) + 1;
        String status = effectiveFrom.isAfter(today) ? "PENDING" : "CURRENT";
        long planId = planRepository.insert(req.personId(), versionNo, effectiveFrom, status, req.remark());
        planItemRepository.insertBatch(planId, req.items());
        planRepository.closeOpenPlansBefore(req.personId(), effectiveFrom, planId);
        return planId;
    }

    /**
     * 基于已有方案创建新版本（默认复制原方案明细，也可传入新的明细）。
     */
    @Transactional
    public long newVersion(long sourcePlanId, NewVersionReq req) {
        Plan source = planRepository.findById(sourcePlanId)
                .orElseThrow(() -> new BusinessException("用药方案不存在"));
        LocalDate effectiveFrom = req == null ? null : req.effectiveFrom();
        if (effectiveFrom == null) {
            throw new BusinessException("请设置新版本的生效日期");
        }
        if (!effectiveFrom.isAfter(source.effectiveFrom())) {
            throw new BusinessException("新版本生效日期必须晚于原方案生效日期");
        }
        List<PlanItemReq> items = (req.items() == null || req.items().isEmpty())
                ? copyItems(sourcePlanId)
                : req.items();
        validateItems(items);
        LocalDate today = LocalDate.now();
        int versionNo = planRepository.maxVersion(source.personId()) + 1;
        String status = effectiveFrom.isAfter(today) ? "PENDING" : "CURRENT";
        String remark = req.remark() == null ? source.remark() : req.remark();
        long planId = planRepository.insert(source.personId(), versionNo, effectiveFrom, status, remark);
        planItemRepository.insertBatch(planId, items);
        planRepository.closeOpenPlansBefore(source.personId(), effectiveFrom, planId);
        return planId;
    }

    /**
     * 仅允许修改尚未生效（PENDING）且没有用药记录的方案。
     */
    @Transactional
    public void updatePending(long planId, PlanReq req) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("用药方案不存在"));
        if (!plan.effectiveFrom().isAfter(LocalDate.now())) {
            throw new BusinessException("已生效的方案不可修改，请基于当前方案创建新版本");
        }
        if (planRepository.hasRecords(planId)) {
            throw new BusinessException("该方案已产生用药记录，不可修改");
        }
        validateItems(req.items());
        planRepository.updatePending(planId, req.effectiveFrom() == null ? plan.effectiveFrom() : req.effectiveFrom(),
                req.remark());
        planItemRepository.deleteByPlanId(planId);
        planItemRepository.insertBatch(planId, req.items());
    }

    public List<PlanItemView> items(long planId) {
        return planItemRepository.findViews(planId);
    }

    /**
     * 删除方案。已产生用药记录的方案不可删除（历史记录需要通过 plan_id 还原）。
     * 删除后重建该用药人的方案时间线，保证生效区间连续。
     */
    @Transactional
    public void delete(long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("用药方案不存在"));
        if (planRepository.hasRecords(planId)) {
            throw new BusinessException("该方案已产生用药记录，不可删除");
        }
        planItemRepository.deleteByPlanId(planId);
        planRepository.delete(planId);
        rebuildTimeline(plan.personId());
    }

    private void rebuildTimeline(long personId) {
        List<Plan> plans = new ArrayList<>(planRepository.findByPersonId(personId));
        plans.sort(Comparator.comparing(Plan::effectiveFrom).thenComparing(Plan::versionNo));
        LocalDate today = LocalDate.now();
        for (int i = 0; i < plans.size(); i++) {
            Plan p = plans.get(i);
            LocalDate to = (i + 1 < plans.size()) ? plans.get(i + 1).effectiveFrom().minusDays(1) : null;
            if (to != null && to.isBefore(p.effectiveFrom())) {
                to = p.effectiveFrom();
            }
            String status;
            if (p.effectiveFrom().isAfter(today)) {
                status = "PENDING";
            } else if (to == null || !to.isBefore(today)) {
                status = "CURRENT";
            } else {
                status = "HISTORY";
            }
            planRepository.updateEffectiveToStatus(p.id(), to, status);
        }
    }

    private List<PlanItemReq> copyItems(long planId) {
        List<PlanItemReq> items = new ArrayList<>();
        for (PlanItemView v : planItemRepository.findViews(planId)) {
            items.add(new PlanItemReq(v.drugId(), v.period(), v.dose(), v.doseUnit(), v.sortNo(), v.remark()));
        }
        return items;
    }

    private void validateItems(List<PlanItemReq> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("至少需要一条用药明细");
        }
        for (PlanItemReq item : items) {
            if (item.drugId() == null) {
                throw new BusinessException("用药明细中的药品不能为空");
            }
            if (!drugRepository.existsById(item.drugId())) {
                throw new BusinessException("用药明细中的药品不存在");
            }
            Period.of(item.period());
            if (item.dose() == null || item.dose().signum() <= 0) {
                throw new BusinessException("剂量必须大于 0");
            }
            if (item.doseUnit() == null || item.doseUnit().isBlank()) {
                throw new BusinessException("服药单位不能为空");
            }
        }
    }

    private Plan withDerivedStatus(Plan plan, LocalDate today) {
        String status;
        if (plan.effectiveFrom().isAfter(today)) {
            status = "PENDING";
        } else if (plan.effectiveTo() == null || !plan.effectiveTo().isBefore(today)) {
            status = "CURRENT";
        } else {
            status = "HISTORY";
        }
        return new Plan(plan.id(), plan.personId(), plan.versionNo(), plan.effectiveFrom(),
                plan.effectiveTo(), status, plan.remark(), plan.createdAt());
    }
}
