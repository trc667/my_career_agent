<template>
  <el-dialog v-model="visibleLocal" title="技能差距分析" width="min(520px, 92vw)" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="岗位 JD" prop="jd">
        <el-input
          v-model="form.jd"
          type="textarea"
          :rows="6"
          placeholder="粘贴目标岗位的招聘要求（JD）"
        />
      </el-form-item>
      <el-form-item label="我的技能" prop="skills">
        <el-input
          v-model="form.skills"
          type="textarea"
          :rows="6"
          placeholder="列出我掌握的技术栈、项目经历、证书等"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visibleLocal = false">取消</el-button>
      <el-button type="primary" :disabled="!canSubmit" @click="submit">分析并发送</el-button>
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
  jd: '',
  skills: '',
});

const rules: FormRules = {
  jd: [{ required: true, message: '请粘贴岗位 JD', trigger: 'blur' }],
  skills: [{ required: true, message: '请填写我的技能', trigger: 'blur' }],
};

const canSubmit = computed(() => !!form.jd.trim() && !!form.skills.trim());

function buildPrompt() {
  return `请对比下面的「目标岗位 JD」与「我的技能清单」，做技能差距分析，输出包括：\n1) 逐条列出已满足与缺失的技能点\n2) 缺失项按优先级排序并说明理由\n3) 3条具体可执行的补强建议（含建议的学习资源）\n\n目标岗位 JD：\n${form.jd}\n\n我的技能清单：\n${form.skills}\n`;
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
