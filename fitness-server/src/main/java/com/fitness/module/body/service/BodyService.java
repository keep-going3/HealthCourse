package com.fitness.module.body.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ErrorCode;
import com.fitness.module.body.dto.*;
import com.fitness.module.body.entity.WeightRecord;
import com.fitness.module.body.mapper.WeightRecordMapper;
import com.fitness.module.user.entity.User;
import com.fitness.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BodyService {

    private final UserMapper userMapper;
    private final WeightRecordMapper weightRecordMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Cacheable(value = "user:body", key = "#userId")
    public BodyInfoResponse getBodyInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        BodyInfoResponse resp = new BodyInfoResponse();
        resp.setHeightCm(user.getHeightCm());
        resp.setTargetWeightKg(user.getTargetWeightKg());
        resp.setGoal(user.getGoal());

        // 查询最新体重
        WeightRecord latest = weightRecordMapper.selectOne(
                new LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getUserId, userId)
                        .orderByDesc(WeightRecord::getRecordDate)
                        .last("LIMIT 1"));
        if (latest != null) {
            resp.setLatestWeight(latest.getWeightKg());
            resp.setLatestWeightDate(latest.getRecordDate().toString());
        }

        return resp;
    }

    @CacheEvict(value = "user:body", key = "#userId")
    public void updateBodyInfo(Long userId, BodyInfoUpdateRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        user.setHeightCm(req.getHeightCm());
        user.setTargetWeightKg(req.getTargetWeightKg());
        user.setGoal(req.getGoal());
        userMapper.updateById(user);
    }

    public WeightRecord recordWeight(Long userId, WeightRecordRequest req) {
        LocalDate date = req.getRecordDate() != null
                ? LocalDate.parse(req.getRecordDate(), DATE_FMT)
                : LocalDate.now();

        // upsert: 当天已有则更新
        WeightRecord existing = weightRecordMapper.selectOne(
                new LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getUserId, userId)
                        .eq(WeightRecord::getRecordDate, date));
        if (existing != null) {
            existing.setWeightKg(req.getWeightKg());
            weightRecordMapper.updateById(existing);
            return existing;
        }

        WeightRecord record = new WeightRecord();
        record.setUserId(userId);
        record.setWeightKg(req.getWeightKg());
        record.setRecordDate(date);
        weightRecordMapper.insert(record);
        return record;
    }

    public List<WeightListItem> getWeightList(Long userId, Integer months) {
        LocalDate start = months != null
                ? LocalDate.now().minusMonths(months)
                : LocalDate.now().minusMonths(3);

        List<WeightRecord> records = weightRecordMapper.selectList(
                new LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getUserId, userId)
                        .ge(WeightRecord::getRecordDate, start)
                        .orderByAsc(WeightRecord::getRecordDate));

        if (records.isEmpty()) return new ArrayList<>();

        // 计算 change
        List<WeightListItem> items = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            WeightListItem item = new WeightListItem();
            item.setRecordId(records.get(i).getId());
            item.setRecordDate(records.get(i).getRecordDate());
            item.setWeightKg(records.get(i).getWeightKg());
            item.setChange(i == 0 ? null :
                    records.get(i).getWeightKg().subtract(records.get(i - 1).getWeightKg()));
            items.add(item);
        }

        // 倒序（最新在前）
        List<WeightListItem> reversed = new ArrayList<>(items);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    public void updateWeight(Long userId, Long recordId, WeightRecordRequest req) {
        WeightRecord record = weightRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        record.setWeightKg(req.getWeightKg());
        weightRecordMapper.updateById(record);
    }

    public void deleteWeight(Long userId, Long recordId) {
        WeightRecord record = weightRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        weightRecordMapper.deleteById(recordId);
    }
}
