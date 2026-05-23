<template>
  <div class="page">
    <van-nav-bar title="训练历史" left-text="记录" @click-left="$router.push('/workout')" />

    <!-- 月份切换 -->
    <div style="display: flex; align-items: center; justify-content: center; gap: 16px; padding: 16px">
      <van-icon name="arrow-left" @click="prevMonth" style="font-size: 20px" />
      <span style="font-size: 18px; font-weight: 500; min-width: 140px; text-align: center">
        {{ currentYear }}年{{ currentMonth }}月
      </span>
      <van-icon name="arrow" @click="nextMonth" style="font-size: 20px" />
    </div>

    <!-- 日历网格 -->
    <van-cell-group inset>
      <!-- 星期头 -->
      <div class="calendar-header">
        <span v-for="d in weekDays" :key="d" class="calendar-weekday">{{ d }}</span>
      </div>
      <!-- 日期 -->
      <div class="calendar-grid">
        <div v-for="(day, idx) in calendarDays" :key="idx" class="calendar-cell"
          :class="{ 'has-workout': day.hasWorkout, 'today': day.isToday }"
          @click="viewDetail(day)">
          <span class="calendar-day">{{ day.day }}</span>
          <div v-if="day.hasWorkout" class="workout-dot" />
          <div v-if="day.isToday && !day.hasWorkout" class="today-dot" />
        </div>
      </div>
    </van-cell-group>

    <!-- 选中日期的详情 -->
    <div v-if="selectedDetail" style="margin: 16px">
      <van-cell-group inset>
        <van-cell title="训练日期" :value="selectedDetail.sessionDate" />
        <van-cell title="训练部位">
          <template #value>
            <van-tag v-for="part in selectedDetail.bodyParts" :key="part" round
              style="margin-right: 4px" color="#1989fa">
              {{ muscleMap[part] || part }}
            </van-tag>
            <span v-if="!selectedDetail.bodyParts?.length" style="color: #969799">-</span>
          </template>
        </van-cell>
        <van-cell title="总组数" :value="selectedDetail.totalSets" />
        <van-cell title="训练感受">
          <template #value>
            <van-rate :model-value="selectedDetail.feelRating" :count="5" size="16px"
              readonly />
          </template>
        </van-cell>
        <van-cell v-if="selectedDetail.notes" title="备注" :value="selectedDetail.notes" />
      </van-cell-group>

      <h4 style="margin: 16px 0 8px">动作明细</h4>
      <van-cell-group inset>
        <template v-for="(ex, idx) in groupedExercises" :key="idx">
          <van-cell :title="ex.exerciseName" style="font-weight: 500" />
          <van-cell v-for="s in ex.sets" :key="s.setNumber">
            <template #title>
              <span style="font-size: 13px">
                第{{ s.setNumber }}组
                <template v-if="s.weightKg !== null && s.weightKg !== undefined">
                  · {{ s.weightKg }}kg
                </template>
                <template v-if="s.reps !== null && s.reps !== undefined">
                  · {{ s.reps }}次
                </template>
                <template v-if="s.rpe !== null && s.rpe !== undefined">
                  · RPE {{ s.rpe }}
                </template>
              </span>
            </template>
          </van-cell>
        </template>
      </van-cell-group>

      <div style="margin: 16px 0">
        <van-button round block type="danger" @click="handleDelete">
          删除本次训练
        </van-button>
      </div>
    </div>

    <van-empty v-if="!selectedDetail && !loading" description="点击日历中的日期查看详情" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { showConfirmDialog, showToast } from 'vant';
import { getWorkoutCalendar, getWorkoutDetail, deleteWorkout } from '@/api/workout';

const muscleMap = {
  chest: '胸', back: '背', shoulder: '肩',
  arms: '手臂', legs: '腿', other: '其他',
};

const weekDays = ['日', '一', '二', '三', '四', '五', '六'];

const now = new Date();
const currentYear = ref(now.getFullYear());
const currentMonth = ref(now.getMonth() + 1);

const workoutMap = ref({}); // key: "YYYY-MM-DD" → CalendarItemResponse
const selectedDate = ref('');
const selectedDetail = ref(null);
const loading = ref(false);

// 生成日历网格
const calendarDays = computed(() => {
  const year = currentYear.value;
  const month = currentMonth.value;
  const firstDay = new Date(year, month - 1, 1);
  const lastDay = new Date(year, month, 0);
  const daysInMonth = lastDay.getDate();
  const startWeekday = firstDay.getDay();

  const today = new Date();
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

  const days = [];

  // 空白占位
  for (let i = 0; i < startWeekday; i++) {
    days.push({ day: '', hasWorkout: false, isToday: false, date: '' });
  }

  // 日期
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    days.push({
      day: d,
      date: dateStr,
      hasWorkout: !!workoutMap.value[dateStr],
      isToday: dateStr === todayStr,
    });
  }

  return days;
});

// 动作按 exerciseName 分组
const groupedExercises = computed(() => {
  if (!selectedDetail.value?.exercises) return [];
  const groups = {};
  selectedDetail.value.exercises.forEach((ex) => {
    if (!groups[ex.exerciseName]) groups[ex.exerciseName] = [];
    groups[ex.exerciseName].push(ex);
  });
  return Object.entries(groups).map(([name, sets]) => ({
    exerciseName: name,
    sets: sets.sort((a, b) => a.setNumber - b.setNumber),
  }));
});

const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentYear.value--;
    currentMonth.value = 12;
  } else {
    currentMonth.value--;
  }
  fetchCalendar();
};

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentYear.value++;
    currentMonth.value = 1;
  } else {
    currentMonth.value++;
  }
  fetchCalendar();
};

const fetchCalendar = async () => {
  try {
    const res = await getWorkoutCalendar(currentYear.value, currentMonth.value);
    if (res.code === 200 && res.data) {
      workoutMap.value = {};
      res.data.forEach((item) => {
        workoutMap.value[item.sessionDate] = item;
      });
    }
  } catch {
    // 静默失败
  }
};

const viewDetail = async (day) => {
  selectedDate.value = day.date;
  loading.value = true;
  try {
    const res = await getWorkoutDetail(day.date);
    if (res.code === 200 && res.data) {
      selectedDetail.value = res.data;
    } else {
      selectedDetail.value = null;
      showToast('未找到训练记录');
    }
  } catch {
    showToast('获取详情失败');
    selectedDetail.value = null;
  } finally {
    loading.value = false;
  }
};

const handleDelete = async () => {
  if (!selectedDetail.value) return;
  try {
    await showConfirmDialog({ title: '提示', message: '确定删除该训练记录？' });
    await deleteWorkout(selectedDetail.value.sessionId);
    showToast('删除成功');
    selectedDetail.value = null;
    await fetchCalendar();
  } catch {
    // 取消
  }
};

onMounted(() => {
  fetchCalendar();
});
</script>

<style scoped>
.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.calendar-weekday {
  font-size: 13px;
  color: #969799;
}
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
}
.calendar-cell {
  padding: 8px 0;
  cursor: pointer;
  position: relative;
  min-height: 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.calendar-cell.has-workout {
  color: #1989fa;
  font-weight: 600;
}
.calendar-cell.today .calendar-day {
  background: #1989fa;
  color: white;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.calendar-day {
  font-size: 14px;
}
.workout-dot {
  width: 5px;
  height: 5px;
  background: #1989fa;
  border-radius: 50%;
  margin-top: 2px;
}
.today-dot {
  width: 5px;
  height: 5px;
  background: #cccccc;
  border-radius: 50%;
  margin-top: 2px;
}
</style>
