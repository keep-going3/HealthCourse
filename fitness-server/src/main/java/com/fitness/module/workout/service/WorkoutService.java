package com.fitness.module.workout.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ErrorCode;
import com.fitness.module.workout.dto.*;
import com.fitness.module.workout.entity.WorkoutExercise;
import com.fitness.module.workout.entity.WorkoutSession;
import com.fitness.module.workout.mapper.WorkoutExerciseMapper;
import com.fitness.module.workout.mapper.WorkoutSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutSessionMapper sessionMapper;
    private final WorkoutExerciseMapper exerciseMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public WorkoutDetailResponse save(Long userId, WorkoutSaveRequest req) {
        LocalDate date = LocalDate.parse(req.getSessionDate(), DATE_FMT);

        // 检查当天是否已有记录
        Long count = sessionMapper.selectCount(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getSessionDate, date));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "当天已有训练记录");
        }

        return insertWorkout(userId, date, req);
    }

    @Transactional
    public WorkoutDetailResponse update(Long userId, WorkoutSaveRequest req) {
        LocalDate date = LocalDate.parse(req.getSessionDate(), DATE_FMT);

        // 查找当天的记录
        WorkoutSession existing = sessionMapper.selectOne(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getSessionDate, date));
        if (existing == null) {
            // 没有则新增
            return insertWorkout(userId, date, req);
        }

        // 删除旧的动作明细
        exerciseMapper.delete(
                new LambdaQueryWrapper<WorkoutExercise>()
                        .eq(WorkoutExercise::getSessionId, existing.getId()));

        // 更新 session
        updateSessionFromReq(existing, req);
        sessionMapper.updateById(existing);

        // 批量插入新动作
        batchInsertExercises(existing.getId(), req.getExercises());

        return buildDetail(existing);
    }

    public WorkoutDetailResponse getDetail(Long userId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
        WorkoutSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getSessionDate, date));
        if (session == null) {
            return null;
        }
        return buildDetail(session);
    }

    public List<CalendarItemResponse> getCalendar(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<WorkoutSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .between(WorkoutSession::getSessionDate, start, end));

        if (sessions.isEmpty()) return Collections.emptyList();

        Set<Long> sessionIds = sessions.stream().map(WorkoutSession::getId).collect(Collectors.toSet());

        List<WorkoutExercise> allExercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<WorkoutExercise>()
                        .in(WorkoutExercise::getSessionId, sessionIds));

        // 按 sessionId 分组
        Map<Long, List<WorkoutExercise>> exerciseMap = allExercises.stream()
                .collect(Collectors.groupingBy(WorkoutExercise::getSessionId));

        return sessions.stream().map(s -> {
            CalendarItemResponse item = new CalendarItemResponse();
            item.setSessionDate(s.getSessionDate());
            item.setSessionId(s.getId());
            item.setTotalSets(s.getTotalSets());
            item.setFeelRating(s.getFeelRating());

            List<WorkoutExercise> exList = exerciseMap.getOrDefault(s.getId(), Collections.emptyList());
            String summary = exList.stream()
                    .map(WorkoutExercise::getExerciseName)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.joining(", "));
            if (exList.stream().map(WorkoutExercise::getExerciseName).distinct().count() > 3) {
                summary += " ...";
            }
            item.setBodyPartsSummary(summary);
            return item;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        WorkoutSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 级联删除 exercise（ON DELETE CASCADE）
        sessionMapper.deleteById(sessionId);
    }

    // ---- 私有方法 ----

    private WorkoutDetailResponse insertWorkout(Long userId, LocalDate date, WorkoutSaveRequest req) {
        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        session.setSessionDate(date);
        updateSessionFromReq(session, req);
        sessionMapper.insert(session);

        batchInsertExercises(session.getId(), req.getExercises());

        return buildDetail(session);
    }

    private void updateSessionFromReq(WorkoutSession session, WorkoutSaveRequest req) {
        try {
            session.setBodyParts(objectMapper.writeValueAsString(
                    req.getBodyParts() != null ? req.getBodyParts() : Collections.emptyList()));
        } catch (Exception e) {
            session.setBodyParts("[]");
        }

        // total_sets = 所有组数总和
        int totalSets = req.getExercises().stream()
                .mapToInt(ExerciseSetDTO::getSetNumber)
                .sum();
        session.setTotalSets(totalSets);
        session.setFeelRating(req.getFeelRating());
        session.setNotes(req.getNotes());
    }

    private void batchInsertExercises(Long sessionId, List<ExerciseSetDTO> exercises) {
        List<WorkoutExercise> list = exercises.stream().map(dto -> {
            WorkoutExercise e = new WorkoutExercise();
            e.setSessionId(sessionId);
            e.setExerciseName(dto.getExerciseName());
            e.setSetNumber(dto.getSetNumber());
            e.setWeightKg(dto.getWeightKg());
            e.setReps(dto.getReps());
            e.setRpe(dto.getRpe());
            return e;
        }).collect(Collectors.toList());

        // MyBatis-Plus 批量插入
        list.forEach(exerciseMapper::insert);
    }

    private WorkoutDetailResponse buildDetail(WorkoutSession session) {
        WorkoutDetailResponse resp = new WorkoutDetailResponse();
        resp.setSessionId(session.getId());
        resp.setSessionDate(session.getSessionDate());
        resp.setTotalSets(session.getTotalSets());
        resp.setFeelRating(session.getFeelRating());
        resp.setNotes(session.getNotes());

        // 解析 bodyParts JSON
        try {
            List<String> parts = objectMapper.readValue(
                    session.getBodyParts() != null ? session.getBodyParts() : "[]",
                    new TypeReference<List<String>>() {});
            resp.setBodyParts(parts);
        } catch (Exception e) {
            resp.setBodyParts(Collections.emptyList());
        }

        // 查动作明细
        List<WorkoutExercise> exercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<WorkoutExercise>()
                        .eq(WorkoutExercise::getSessionId, session.getId()));

        resp.setExercises(exercises.stream().map(ex -> {
            WorkoutDetailResponse.ExerciseDetail d = new WorkoutDetailResponse.ExerciseDetail();
            d.setExerciseName(ex.getExerciseName());
            d.setSetNumber(ex.getSetNumber());
            d.setWeightKg(ex.getWeightKg());
            d.setReps(ex.getReps());
            d.setRpe(ex.getRpe());
            return d;
        }).collect(Collectors.toList()));

        return resp;
    }
}
