<template>
  <el-dialog v-model="visibleLocal" title="面试话术生成" width="min(420px, 92vw)" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="场景" prop="scene">
        <el-select v-model="form.scene" placeholder="请选择">
          <el-option label="自我介绍" value="自我介绍" />
          <el-option label="项目介绍" value="项目介绍" />
          <el-option label="技术问答" value="技术问答" />
          <el-option label="反问环节" value="反问环节" />
        </el-select>
      </el-form-item>

      <el-form-item label="风格" prop="style">
        <el-select v-model="form.style" placeholder="请选择">
          <el-option label="简洁" value="简洁" />
          <el-option label="自信沉稳" value="自信沉稳" />
          <el-option label="突出亮点" value="突出亮点" />
        </el-select>
      </el-form-item>

      <el-form-item label="岗位" prop="job">
        <el-input v-model="form.job" placeholder="如：Java 后端开发（可空）" />
      </el-form-item>

      <el-form-item label="经历背景" prop="info">
        <el-input v-model="form.info" type="textarea" :rows="3" placeholder="项目要点 / 技术栈 / 亮点" />
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
  scene: '',
  style: '',
  job: '',
  info: '',
});

const rules: FormRules = {
  scene: [{ required: true, message: '请选择场景', trigger: 'change' }],
  style: [{ required: true, message: '请选择风格', trigger: 'change' }],
  info: [{ required: true, message: '请输入经历背景', trigger: 'blur' }],
};

const canSubmit = computed(
  () => !!form.scene && !!form.style && !!form.info.trim(),
);

function buildPrompt() {
  return `请根据以下信息，为「${form.scene}」环节生成一段面试话术（中文第一人称，3～6句，风格：${form.style}）：\n\n- 目标岗位：${form.job || '未指定'}\n- 我的经历/亮点：${form.info}\n`;
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
