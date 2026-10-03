package com.xhr.medsdata.service;

import com.xhr.medsdata.common.BusinessException;
import com.xhr.medsdata.domain.StockLog;
import com.xhr.medsdata.domain.StockView;
import com.xhr.medsdata.dto.Requests.StockOpReq;
import com.xhr.medsdata.repository.DrugRepository;
import com.xhr.medsdata.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StockService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final StockRepository stockRepository;
    private final DrugRepository drugRepository;

    public StockService(StockRepository stockRepository, DrugRepository drugRepository) {
        this.stockRepository = stockRepository;
        this.drugRepository = drugRepository;
    }

    public List<StockView> list() {
        return stockRepository.findAllViews();
    }

    public List<StockView> lowStock() {
        return stockRepository.findLowStock();
    }

    public List<StockLog> logs(Long drugId, Integer limit) {
        return stockRepository.findLogs(drugId, limit == null || limit <= 0 ? 100 : limit);
    }

    /**
     * 库存变动：当前库存更新与库存流水新增在同一事务中完成。
     */
    @Transactional
    public StockView change(StockOpReq req) {
        if (req == null || req.drugId() == null) {
            throw new BusinessException("药品不能为空");
        }
        if (!drugRepository.existsById(req.drugId())) {
            throw new BusinessException("药品不存在");
        }
        if (req.quantity() == null) {
            throw new BusinessException("数量不能为空");
        }
        String type = req.changeType() == null ? "PURCHASE" : req.changeType().trim().toUpperCase();
        BigDecimal input = req.quantity();
        BigDecimal before = stockRepository.findQuantity(req.drugId()).orElse(ZERO);
        BigDecimal change;
        BigDecimal after;
        switch (type) {
            case "PURCHASE" -> {
                if (input.signum() <= 0) {
                    throw new BusinessException("入库数量必须大于 0");
                }
                change = input;
                after = before.add(input);
            }
            case "OUT", "USE" -> {
                if (input.signum() <= 0) {
                    throw new BusinessException("减少数量必须大于 0");
                }
                change = input.negate();
                after = before.subtract(input);
            }
            case "ADJUST" -> {
                if (input.signum() < 0) {
                    throw new BusinessException("调整后的库存不能为负数");
                }
                change = input.subtract(before);
                after = input;
            }
            default -> throw new BusinessException("不支持的库存变动类型: " + type);
        }
        if (after.signum() < 0) {
            throw new BusinessException("库存不足，当前库存 " + before.stripTrailingZeros().toPlainString());
        }
        stockRepository.upsertQuantity(req.drugId(), after);
        stockRepository.insertLog(req.drugId(), change, before, after, type, req.remark());
        return stockRepository.findView(req.drugId())
                .orElseThrow(() -> new BusinessException("药品不存在"));
    }

    /**
     * 自动增减库存（服药扣减、撤销回补）。允许扣成负数，避免因库存不准而阻断服药记录。
     */
    @Transactional
    public void applyChange(long drugId, BigDecimal delta, String changeType, String remark) {
        if (delta == null || delta.signum() == 0) {
            return;
        }
        if (!drugRepository.existsById(drugId)) {
            throw new BusinessException("药品不存在");
        }
        BigDecimal before = stockRepository.findQuantity(drugId).orElse(ZERO);
        BigDecimal after = before.add(delta);
        stockRepository.upsertQuantity(drugId, after);
        stockRepository.insertLog(drugId, delta, before, after, changeType, remark);
    }
}
