<template>
  <el-dialog v-model="visibleLocal" title="备考计划制定" width="440px" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="目标" prop="goal">
        <el-select v-model="form.goal" placeholder="请选择">
          <el-option label="秋招" value="秋招" />
          <el-option label="春招" value="春招" />
          <el-option label="暑期实习" value="暑期实习" />
          <el-option label="日常实习" value="日常实习" />
        </el-select>
      </el-form-item>
      <el-form-item label="剩余时间" prop="months">
        <el-input v-model="form.months" placeholder="如：3个月" />
      </el-form-item>
      <el-form-item label="每日时间" prop="dailyHours">
        <el-input v-model="form.dailyHours" placeholder="如：4小时" />
      </el-form-item>
      <el-form-item label="目标岗位" prop="job">
        <el-input v-model="form.job" placeholder="如：Java 后端（可空）" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visibleLocal = false">取消</el-button>
      <el-button type="primary" :disabled="!canSubmit" @click="submit">生成并发送</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';

const props = defineProps<{ visible: boolean }>();
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void;
  (e: 'insert', prompt: string): void;
}>();

const visibleLocal = ref(props.visible);
watch(
  () => props.visible,
  (v) => (visibleLocal.value = v),
);
watch(visibleLocal, (v) => emit('update:visible', v));

const formRef = ref<FormInstance>();
const form = reactive({
  goal: '',
  months: '',
  dailyHours: '',
  job: '',
});

const rules: FormRules = {
  goal: [{ required: true, message: '请选择目标', trigger: 'change' }],
  months: [{ required: true, message: '请输入剩余时间', trigger: 'blur' }],
};

const canSubmit = computed(
  () => !!form.goal && !!form.months.trim(),
);

function buildPrompt() {
  return `请为「${form.goal}」制定一份可落地的备考计划，输出包含：\n- 分阶段时间表（如基础期/强化期/冲刺期，按剩余${form.months}拆分）\n- 每周典型安排（算法刷题、八股、项目、简历各占多少）\n- 关键时间节点与投递提醒\n- 注意事项\n\n- 目标：${form.goal}\n- 剩余时间：${form.months}\n- 每日可投入：${form.dailyHours || '未指定'}\n- 目标岗位：${form.job || '未指定'}\n`;
}

function submit() {
  if (!formRef.value) return;
  formRef.value.validate((ok) => {
    if (!ok) return;
    emit('insert', buildPrompt());
    visibleLocal.value = false;
  });
}
</script>
