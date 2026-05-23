<template>
  <div class="page">
    <van-nav-bar title="身体数据" />

    <!-- 身体信息卡片 -->
    <van-cell-group inset style="margin-top: 12px">
      <van-cell title="身高" :value="bodyInfo.heightCm ? bodyInfo.heightCm + ' cm' : '未设置'" />
      <van-cell title="目标体重" :value="bodyInfo.targetWeightKg ? bodyInfo.targetWeightKg + ' kg' : '未设置'" />
      <van-cell title="目标">
        <template #value>
          <van-tag :color="goalColor" round>{{ goalText }}</van-tag>
        </template>
      </van-cell>
      <van-cell title="最新体重" :value="bodyInfo.latestWeight ? bodyInfo.latestWeight + ' kg' : '暂无记录'" />
      <van-cell v-if="bodyInfo.latestWeightDate" title="记录日期" :value="bodyInfo.latestWeightDate" />
      <van-cell is-link @click="openEditInfo">
        <template #title>
          <span style="color: #1989fa">编辑身体信息</span>
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 记录体重 -->
    <van-cell-group inset style="margin-top: 16px">
      <van-cell title="记录体重" />
      <van-field v-model="newWeight" type="digit" placeholder="输入体重 (kg)" label="体重"
        label-width="50px">
        <template #button>
          <van-button size="small" type="primary" :loading="weightLoading" @click="handleRecordWeight">
            记录
          </van-button>
        </template>
      </van-field>
    </van-cell-group>

    <!-- 体重趋势图 -->
    <div v-if="weightChartData.length > 1" style="margin: 16px; background: white; border-radius: 8px; padding: 16px">
      <h4 style="margin-bottom: 12px">体重趋势</h4>
      <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" style="width: 100%; height: 160px">
        <!-- 网格线 -->
        <line v-for="(y, i) in gridLines" :key="'grid'+i"
          :x1="0" :y1="y" :x2="chartWidth" :y2="y"
          stroke="#f0f0f0" stroke-width="1" />
        <!-- 折线 -->
        <polyline :points="linePoints" fill="none" stroke="#1989fa" stroke-width="2" />
        <!-- 数据点 -->
        <circle v-for="(pt, i) in chartPoints" :key="'pt'+i"
          :cx="pt.x" :cy="pt.y" r="3" fill="#1989fa" />
        <!-- 标签（显示开头、中间、结尾） -->
        <text v-for="(pt, i) in labelPoints" :key="'lb'+i"
          :x="pt.x" :y="chartHeight + 14" text-anchor="middle"
          font-size="10" fill="#969799">{{ pt.label }}</text>
      </svg>
    </div>

    <!-- 体重历史列表 -->
    <van-cell-group inset style="margin-top: 16px">
      <van-cell title="体重历史" />
      <template v-if="weightList.length">
        <van-cell v-for="item in weightList" :key="item.recordId">
          <template #title>
            <span>{{ item.recordDate }}</span>
          </template>
          <template #value>
            <span style="font-weight: 600">{{ item.weightKg }} kg</span>
            <span v-if="item.change !== null"
              :style="{ color: item.change > 0 ? '#ee0a24' : item.change < 0 ? '#07c160' : '#969799', marginLeft: '8px' }">
              {{ item.change > 0 ? '+' : '' }}{{ item.change }}
            </span>
            <van-icon name="delete" style="margin-left: 12px; color: #969799"
              @click="handleDeleteWeight(item.recordId)" />
          </template>
        </van-cell>
      </template>
      <van-empty v-else description="暂无体重记录" />
    </van-cell-group>

    <!-- 编辑身体信息弹窗 -->
    <van-popup v-model:show="showEditInfo" position="bottom" round style="padding: 16px">
      <h3 style="margin-bottom: 16px">编辑身体信息</h3>
      <van-form @submit="handleUpdateInfo">
        <van-field v-model="editForm.heightCm" type="digit" label="身高 (cm)" placeholder="如 175" />
        <van-field v-model="editForm.targetWeightKg" type="digit" label="目标体重 (kg)" placeholder="如 78" />
        <van-field label="目标">
          <template #input>
            <van-radio-group v-model="editForm.goal" direction="horizontal">
              <van-radio name="gain">增肌</van-radio>
              <van-radio name="cut" style="margin-left: 12px">减脂</van-radio>
              <van-radio name="maintain" style="margin-left: 12px">保持</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <div style="margin-top: 16px">
          <van-button round block type="primary" native-type="submit">保存</van-button>
        </div>
      </van-form>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { showConfirmDialog, showToast } from 'vant';
import { getBodyInfo, updateBodyInfo, recordWeight, getWeightList, deleteWeight } from '@/api/body';

const goalColorMap = { gain: '#ee0a24', cut: '#07c160', maintain: '#1989fa' };
const goalTextMap = { gain: '增肌', cut: '减脂', maintain: '保持' };

const bodyInfo = reactive({
  heightCm: null, targetWeightKg: null, goal: '',
  latestWeight: null, latestWeightDate: '',
});

const goalText = computed(() => goalTextMap[bodyInfo.goal] || '未设置');
const goalColor = computed(() => goalColorMap[bodyInfo.goal] || '#969799');

const newWeight = ref('');
const weightLoading = ref(false);
const weightList = ref([]);
const showEditInfo = ref(false);

const editForm = reactive({
  heightCm: '', targetWeightKg: '', goal: 'maintain',
});

const openEditInfo = () => {
  editForm.heightCm = bodyInfo.heightCm || '';
  editForm.targetWeightKg = bodyInfo.targetWeightKg || '';
  editForm.goal = bodyInfo.goal || 'maintain';
  showEditInfo.value = true;
};

// 体重图表
const chartWidth = 300;
const chartHeight = 140;
const chartPadding = 10;

const weightChartData = computed(() => {
  const reversed = [...weightList.value].reverse();
  return reversed.map((i) => ({ date: i.recordDate, weight: Number(i.weightKg) }));
});

const minWeight = computed(() => {
  if (!weightChartData.value.length) return 0;
  return Math.min(...weightChartData.value.map((d) => d.weight)) - 2;
});

const maxWeight = computed(() => {
  if (!weightChartData.value.length) return 100;
  return Math.max(...weightChartData.value.map((d) => d.weight)) + 2;
});

const gridLines = computed(() => {
  const lines = [];
  for (let i = 0; i <= 4; i++) {
    lines.push((chartHeight - chartPadding) * i / 4 + chartPadding);
  }
  return lines;
});

const chartPoints = computed(() => {
  const data = weightChartData.value;
  if (data.length < 2) return [];
  const range = maxWeight.value - minWeight.value;
  const stepX = (chartWidth - chartPadding * 2) / Math.max(data.length - 1, 1);

  return data.map((d, i) => ({
    x: chartPadding + i * stepX,
    y: chartPadding + (chartHeight - chartPadding * 2) * (maxWeight.value - d.weight) / range,
    label: d.date.slice(5), // MM-DD
  }));
});

const linePoints = computed(() => {
  return chartPoints.value.map((p) => `${p.x},${p.y}`).join(' ');
});

const labelPoints = computed(() => {
  const pts = chartPoints.value;
  if (pts.length < 3) return pts;
  return [pts[0], pts[Math.floor(pts.length / 2)], pts[pts.length - 1]];
});

// 加载数据
const fetchData = async () => {
  try {
    const [infoRes, weightRes] = await Promise.all([
      getBodyInfo(),
      getWeightList(12),
    ]);
    if (infoRes.code === 200 && infoRes.data) {
      Object.assign(bodyInfo, infoRes.data);
    }
    if (weightRes.code === 200 && weightRes.data) {
      weightList.value = weightRes.data;
    }
  } catch {
    showToast('加载数据失败');
  }
};

// 记录体重
const handleRecordWeight = async () => {
  if (!newWeight.value || isNaN(newWeight.value)) {
    showToast('请输入有效体重');
    return;
  }
  weightLoading.value = true;
  try {
    const res = await recordWeight({ weightKg: Number(newWeight.value) });
    if (res.code === 200) {
      showToast('记录成功');
      newWeight.value = '';
      await fetchData();
    }
  } catch {
    showToast('记录失败');
  } finally {
    weightLoading.value = false;
  }
};

// 更新身体信息
const handleUpdateInfo = async () => {
  try {
    const res = await updateBodyInfo({
      heightCm: editForm.heightCm ? Number(editForm.heightCm) : null,
      targetWeightKg: editForm.targetWeightKg ? Number(editForm.targetWeightKg) : null,
      goal: editForm.goal,
    });
    if (res.code === 200) {
      showToast('保存成功');
      showEditInfo.value = false;
      await fetchData();
    }
  } catch {
    showToast('保存失败');
  }
};

// 删除体重记录
const handleDeleteWeight = async (recordId) => {
  try {
    await showConfirmDialog({ title: '提示', message: '确定删除该体重记录？' });
    await deleteWeight(recordId);
    showToast('删除成功');
    await fetchData();
  } catch {
    // 取消
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.page { padding-bottom: 40px; }
</style>
