package com.fitness.module.workout.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ErrorCode;
import com.fitness.module.workout.entity.ExerciseLibrary;
import com.fitness.module.workout.mapper.ExerciseLibraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseLibraryService {

    private final ExerciseLibraryMapper exerciseLibraryMapper;

    @Cacheable(value = "exercise:library", key = "'all'", unless = "#result.isEmpty()")
    public List<ExerciseLibrary> getAll(Long userId) {
        LambdaQueryWrapper<ExerciseLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(ExerciseLibrary::getUserId)
                     .or().eq(ExerciseLibrary::getUserId, userId));
        return exerciseLibraryMapper.selectList(wrapper);
    }

    @Cacheable(value = "exercise:library", key = "'group'", unless = "#result.isEmpty()")
    public Map<String, List<ExerciseLibrary>> getGrouped(Long userId) {
        List<ExerciseLibrary> list = getAll(userId);
        return list.stream().collect(Collectors.groupingBy(ExerciseLibrary::getTargetMuscle));
    }

    @CacheEvict(value = "exercise:library", allEntries = true)
    public ExerciseLibrary addCustom(ExerciseLibrary exercise) {
        exercise.setId(null);
        exercise.setUserId(exercise.getUserId());
        exerciseLibraryMapper.insert(exercise);
        return exercise;
    }

    @CacheEvict(value = "exercise:library", allEntries = true)
    public void updateCustom(Long id, Long userId, ExerciseLibrary update) {
        ExerciseLibrary existing = exerciseLibraryMapper.selectById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        existing.setName(update.getName());
        existing.setTargetMuscle(update.getTargetMuscle());
        existing.setEquipment(update.getEquipment());
        existing.setType(update.getType());
        exerciseLibraryMapper.updateById(existing);
    }

    @CacheEvict(value = "exercise:library", allEntries = true)
    public void deleteCustom(Long id, Long userId) {
        ExerciseLibrary existing = exerciseLibraryMapper.selectById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        exerciseLibraryMapper.deleteById(id);
    }
}
