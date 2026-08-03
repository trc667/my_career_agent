<template>
  <el-dialog v-model="visibleLocal" title="学习路线规划" width="440px" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="目标方向" prop="direction">
        <el-select v-model="form.direction" placeholder="请选择">
          <el-option label="Java 后端" value="Java 后端" />
          <el-option label="前端开发" value="前端开发" />
          <el-option label="算法工程师" value="算法工程师" />
          <el-option label="测试开发" value="测试开发" />
          <el-option label="运维开发" value="运维开发" />
        </el-select>
      </el-form-item>
      <el-form-item label="基础水平" prop="level">
        <el-select v-model="form.level" placeholder="请选择">
          <el-option label="零基础" value="零基础" />
          <el-option label="有编程基础" value="有编程基础" />
          <el-option label="在校学生" value="在校学生" />
          <el-option label="转行求职" value="转行求职" />
        </el-select>
      </el-form-item>
      <el-form-item label="周期" prop="cycle">
        <el-input v-model="form.cycle" placeholder="如：3个月/6个月/1年" />
      </el-form-item>
      <el-form-item label="每日时间" prop="dailyHours">
        <el-input v-model="form.dailyHours" placeholder="如：2小时/4小时" />
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
  direction: '',
  level: '',
  cycle: '',
  dailyHours: '',
});

const rules: FormRules = {
  direction: [{ required: true, message: '请选择目标方向', trigger: 'change' }],
  level: [{ required: true, message: '请选择基础水平', trigger: 'change' }],
  cycle: [{ required: true, message: '请输入周期', trigger: 'blur' }],
};

const canSubmit = computed(
  () => !!form.direction && !!form.level && !!form.cycle.trim(),
);

function buildPrompt() {
  return `请为目标「${form.direction}」方向制定一份学习路线规划，输出包含：分阶段（基础→进阶→项目实战→求职冲刺）、每阶段的核心知识点与推荐学习资源、练习建议，输出用条目化：\n\n- 目标方向：${form.direction}\n- 基础水平：${form.level}\n- 学习周期：${form.cycle}\n- 每日可投入时间：${form.dailyHours || '未指定'}\n`;
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
