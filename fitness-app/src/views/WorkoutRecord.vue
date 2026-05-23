<template>
  <div class="page">
    <van-nav-bar title="训练记录" left-text="历史" @click-left="$router.push('/history')" />

    <!-- 训练日期 -->
    <van-cell-group inset style="margin-top: 12px">
      <van-field label="训练日期" :model-value="sessionDate" is-link readonly
        @click="showDatePicker = true" />
    </van-cell-group>

    <!-- 训练部位 -->
    <van-cell-group inset style="margin-top: 12px">
      <van-field label="训练部位" readonly>
        <template #input>
          <van-tag v-for="part in bodyParts" :key="part" style="margin-right: 6px" round
            color="#1989fa">
            {{ muscleMap[part] || part }}
          </van-tag>
          <span v-if="!bodyParts.length" style="color: #969799">选择动作后自动填充</span>
        </template>
      </van-field>
    </van-cell-group>

    <!-- 动作列表 -->
    <div style="margin: 12px 16px">
      <van-button round block icon="plus" type="primary" @click="showExercisePicker = true">
        添加动作
      </van-button>
    </div>

    <div v-for="(group, gIdx) in exerciseGroups" :key="gIdx" style="margin: 12px 16px">
      <van-cell-group inset>
        <van-cell :title="group.exerciseName" label="点击添加组" is-link
          @click="addSet(gIdx)" />
        <van-cell v-for="(set, sIdx) in group.sets" :key="sIdx">
          <template #title>
            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
              <span style="min-width: 40px">第{{ set.setNumber }}组</span>
              <van-field v-model="set.weightKg" type="digit" placeholder="重量kg"
                style="width: 90px" size="small" />
              <van-field v-model="set.reps" type="digit" placeholder="次数"
                style="width: 80px" size="small" />
              <van-field v-model="set.rpe" type="digit" placeholder="RPE"
                style="width: 70px" size="small" />
              <van-icon name="delete" color="#ee0a24" @click="removeSet(gIdx, sIdx)" />
            </div>
          </template>
        </van-cell>
        <van-cell v-if="!group.sets.length">
          <span style="color: #969799; font-size: 13px">暂无组记录，点击上方添加</span>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 评分 -->
    <van-cell-group inset style="margin: 12px 16px">
      <van-cell title="训练感受">
        <template #value>
          <van-rate v-model="feelRating" :count="5" size="20px" />
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 备注 -->
    <van-cell-group inset style="margin: 12px 16px">
      <van-field v-model="notes" type="textarea" rows="2" placeholder="备注（可选）"
        maxlength="500" show-word-limit />
    </van-cell-group>

    <!-- 保存 -->
    <div style="margin: 20px 16px 40px">
      <van-button round block type="primary" :loading="saving" @click="handleSave">
        保存训练
      </van-button>
    </div>

    <!-- 日期选择器 -->
    <van-popup v-model:show="showDatePicker" position="bottom">
      <van-date-picker v-model="selectedDate" :min-date="minDate" :max-date="maxDate"
        title="选择日期" @confirm="onDateConfirm" @cancel="showDatePicker = false" />
    </van-popup>

    <!-- 动作选择器 -->
    <van-popup v-model:show="showExercisePicker" position="bottom" round style="max-height: 80vh">
      <div style="padding: 16px">
        <h3 style="margin-bottom: 12px">选择动作</h3>

        <!-- 自定义输入 -->
        <div style="display: flex; gap: 8px; margin-bottom: 12px">
          <van-field v-model="customExerciseName" placeholder="手动输入动作名称" clearable
            style="flex: 1" @keydown.enter.prevent="addCustomExercise" />
          <van-popover v-model:show="showMusclePicker" :actions="muscleActions"
            @select="onMuscleSelect" placement="bottom">
            <template #reference>
              <van-button plain :type="customMuscle ? 'primary' : 'default'" size="small">
                {{ muscleMap[customMuscle] || '部位' }}
              </van-button>
            </template>
          </van-popover>
          <van-button type="success" size="small" @click="addCustomExercise">添加</van-button>
        </div>

        <van-divider style="margin: 8px 0">或从动作库选择</van-divider>

        <van-search v-model="searchQuery" placeholder="搜索动作" />
        <van-tree-select
          :items="muscleGroups"
          :active-id="selectedExerciseIds"
          :main-active-index="activeMuscleIndex"
          @click-item="toggleExercise"
          @click-nav="onMuscleChange"
          style="max-height: 40vh; overflow-y: auto"
        />
        <van-button round block type="primary" style="margin-top: 12px"
          @click="confirmExercises">
          确定 ({{ selectedExerciseIds.length }})
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { showConfirmDialog, showToast } from 'vant';
import { saveWorkout, updateWorkout, getExerciseLibrary, addCustomExercise as addCustomExerciseApi } from '@/api/workout';

const muscleMap = {
  chest: '胸', back: '背', shoulder: '肩',
  arms: '手臂', legs: '腿', other: '其他',
};

// 日期
const sessionDate = ref(new Date().toISOString().slice(0, 10));
const showDatePicker = ref(false);
const selectedDate = ref(new Date());
const minDate = new Date(2020, 0, 1);
const maxDate = new Date();
const onDateConfirm = ({ selectedValues }) => {
  const [year, month, day] = selectedValues;
  sessionDate.value = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  showDatePicker.value = false;
};

// 动作库
const library = ref([]);
const searchQuery = ref('');

const filteredLibrary = computed(() => {
  let list = library.value;
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase();
    list = list.filter((ex) => ex.name.toLowerCase().includes(q));
  }
  return list;
});

const muscleGroups = computed(() => {
  const groups = {};
  filteredLibrary.value.forEach((ex) => {
    if (!groups[ex.targetMuscle]) groups[ex.targetMuscle] = [];
    groups[ex.targetMuscle].push(ex);
  });
  return Object.entries(groups).map(([key, items]) => ({
    text: muscleMap[key] || key,
    children: items.map((i) => ({ text: i.name, id: i.id })),
  }));
});

const activeMuscleIndex = ref(0);
const selectedExerciseIds = ref([]);
const selectedExerciseNames = ref([]);

// 自定义动作
const customExerciseName = ref('');
const customMuscle = ref('');
const showMusclePicker = ref(false);
const muscleActions = Object.entries(muscleMap).map(([key, label]) => ({
  text: label, value: key,
}));
const onMuscleSelect = (action) => {
  customMuscle.value = action.value;
  showMusclePicker.value = false;
};
const addCustomExercise = async () => {
  const name = customExerciseName.value.trim();
  if (!name) { showToast('请输入动作名称'); return; }
  if (!customMuscle.value) { showToast('请选择身体部位'); return; }

  try {
    // 调用后端 API 持久化到动作库
    await addCustomExerciseApi({
      name,
      targetMuscle: customMuscle.value,
      equipment: 'bodyweight',
      type: 'isolation',
    });

    // 刷新动作库（新动作会出现在对应部位分组中）
    await fetchLibrary();

    // 添加到当前训练
    const existingNames = new Set(exerciseGroups.value.map((g) => g.exerciseName));
    if (!existingNames.has(name)) {
      exerciseGroups.value.push({ exerciseName: name, sets: [] });
      existingNames.add(name);
    }

    customExerciseName.value = '';
    customMuscle.value = '';
  } catch {
    showToast('添加自定义动作失败');
  }
};

const onMuscleChange = (idx) => { activeMuscleIndex.value = idx; };

const toggleExercise = (item) => {
  const idx = selectedExerciseIds.value.indexOf(item.id);
  if (idx > -1) {
    selectedExerciseIds.value.splice(idx, 1);
    selectedExerciseNames.value.splice(idx, 1);
  } else {
    selectedExerciseIds.value.push(item.id);
    selectedExerciseNames.value.push(item.text);
  }
};

// 选中的动作和组
const exerciseGroups = ref([]);
const showExercisePicker = ref(false);

const confirmExercises = () => {
  // 把新选的动作添加到已存在的列表
  const existingNames = new Set(exerciseGroups.value.map((g) => g.exerciseName));
  selectedExerciseNames.value.forEach((name) => {
    if (!existingNames.has(name)) {
      exerciseGroups.value.push({ exerciseName: name, sets: [] });
      existingNames.add(name);
    }
  });
  selectedExerciseIds.value = [];
  selectedExerciseNames.value = [];
  showExercisePicker.value = false;
  updateBodyParts();
};

const addSet = (gIdx) => {
  const group = exerciseGroups.value[gIdx];
  const setNum = group.sets.length + 1;
  group.sets.push({ setNumber: setNum, weightKg: '', reps: '', rpe: '' });
};

const removeSet = (gIdx, sIdx) => {
  exerciseGroups.value[gIdx].sets.splice(sIdx, 1);
  // 重新编号
  exerciseGroups.value[gIdx].sets.forEach((s, i) => { s.setNumber = i + 1; });
};

// 身体部位自动填充
const bodyParts = computed(() => {
  const parts = new Set();
  exerciseGroups.value.forEach((g) => {
    const ex = library.value.find((e) => e.name === g.exerciseName);
    if (ex) parts.add(ex.targetMuscle);
  });
  return [...parts];
});

const updateBodyParts = () => { /* computed 自动更新 */ };

// 评分
const feelRating = ref(3);
const notes = ref('');
const saving = ref(false);

// 保存
const handleSave = async () => {
  if (!exerciseGroups.value.length) {
    showToast('请至少添加一个动作');
    return;
  }

  // 展平所有组
  const exercises = [];
  exerciseGroups.value.forEach((g) => {
    if (g.sets.length === 0) {
      // 没添加具体组数，至少加一组空的
      exercises.push({ exerciseName: g.exerciseName, setNumber: 1, weightKg: null, reps: null, rpe: null });
    } else {
      g.sets.forEach((s) => {
        exercises.push({
          exerciseName: g.exerciseName,
          setNumber: s.setNumber,
          weightKg: s.weightKg ? Number(s.weightKg) : null,
          reps: s.reps ? Number(s.reps) : null,
          rpe: s.rpe ? Number(s.rpe) : null,
        });
      });
    }
  });

  const formData = {
    sessionDate: sessionDate.value,
    bodyParts: bodyParts.value,
    notes: notes.value,
    feelRating: feelRating.value,
    exercises,
  };

  saving.value = true;
  try {
    await saveWorkout(formData);
    showToast('保存成功');
    resetForm();
  } catch (err) {
    if (err.response?.data?.code === 409) {
      try {
        await showConfirmDialog({
          title: '提示',
          message: '当天已有训练记录，是否覆盖？',
        });
        await updateWorkout(formData);
        showToast('覆盖成功');
        resetForm();
      } catch { /* 取消覆盖 */ }
    } else {
      showToast(err.response?.data?.message || '保存失败');
    }
  } finally {
    saving.value = false;
  }
};

const resetForm = () => {
  exerciseGroups.value = [];
  feelRating.value = 3;
  notes.value = '';
};

// 初始化加载动作库
const fetchLibrary = async () => {
  try {
    const res = await getExerciseLibrary();
    if (res.code === 200) {
      library.value = res.data;
    }
  } catch {
    showToast('加载动作库失败');
  }
};

onMounted(fetchLibrary);
</script>

<style scoped>
.page { padding-bottom: 40px; }
</style>
