<template>
  <div class="agreement-page">
    <div class="agreement-page__bar">
      <router-link to="/" class="agreement-page__back">← 返回首页</router-link>
    </div>

    <div class="agreement-card">
      <h1 class="agreement-title">用户协议与隐私政策</h1>
      <p class="agreement-updated">
        更新日期：<span class="pixel-font">2026-08-05</span> · 生效日期：<span class="pixel-font">2026-08-05</span>
      </p>

      <section class="agreement-section">
        <h2 id="agreement">一、用户协议</h2>

        <h3>1. 服务内容</h3>
        <p>
          「AI 职规助手」（以下简称"本服务"）面向计算机专业学生及求职者，提供职业规划咨询、学习路线建议、
          校招备战辅导、面试模拟、八股练习等 AI 对话与内容服务。本服务通过互联网向注册用户提供上述功能。
        </p>

        <h3>2. 账号规范</h3>
        <ul>
          <li>用户须以真实、合法信息完成注册，妥善保管账号与密码，因账号泄露造成的损失由用户自行承担。</li>
          <li>禁止注册多个账号进行恶意操作、滥用服务，禁止利用本服务从事任何违法违规活动。</li>
          <li>若账号存在安全风险或违规行为，本服务有权暂停或终止提供服务。</li>
        </ul>

        <h3>3. AI 内容免责声明</h3>
        <ul>
          <li>本服务中的 AI 生成内容仅供参考，不构成任何专业建议（包括但不限于求职、法律、财务、心理等），请结合自身情况审慎判断。</li>
          <li>AI 可能生成不准确或带有偏差的内容，本服务不对 AI 内容的准确性、完整性作任何保证。</li>
          <li>用户应遵守所在地区法律法规与校园/单位相关规定，不得将 AI 生成内容用于学术不端等不当用途。</li>
        </ul>

        <h3>4. 用户行为规范</h3>
        <ul>
          <li>禁止输入、传播任何违法、色情、暴力、赌博、诈骗、侵犯他人权益或危害国家安全的内容。</li>
          <li>禁止尝试攻击、入侵、破解本服务系统，禁止利用漏洞获取他人数据。</li>
          <li>违反上述规定的，本服务有权删除相关内容、封禁账号，并保留追究法律责任的权利。</li>
        </ul>

        <h3>5. 知识产权</h3>
        <p>
          本服务的软件、界面、文案、标识等知识产权归本服务运营方所有；AI 生成内容的版权归属按适用法律执行。
          用户在使用过程中产生的对话数据仅用于改善服务质量，未经授权不对外公开。
        </p>

        <h3>6. 服务的变更与终止</h3>
        <p>
          本服务有权根据运营需要调整或终止部分或全部功能，并将在合理范围内提前通知。
          用户违反本协议约定的，本服务有权立即终止提供服务并保留法律追责权利。
        </p>

        <h3>7. 协议变更</h3>
        <p>
          本协议更新后将在本页面公示，用户继续使用本服务即视为接受更新后的协议内容。
        </p>
      </section>

      <section class="agreement-section">
        <h2 id="privacy">二、隐私政策</h2>

        <h3>1. 我们收集的信息</h3>
        <ul>
          <li>注册信息：用户名、密码（加密存储）、邮箱地址（用于接收验证码）。</li>
          <li>使用信息：对话内容、会话记录、反馈信息，用于提供 AI 对话服务及改进回答质量。</li>
          <li>设备与日志信息：浏览器类型、IP 地址、访问时间等基础访问日志。</li>
        </ul>

        <h3>2. 信息的存储与保护</h3>
        <ul>
          <li>用户密码采用不可逆加密算法（bcrypt）存储，本服务不保存、不展示明文密码。</li>
          <li>对话数据存储于受访问控制保护的数据库，仅用于向您提供对话上下文与历史回看功能。</li>
          <li>我们采取合理的技术与管理措施保护您的数据安全，但无法保证绝对安全，请您妥善保管账号凭证。</li>
        </ul>

        <h3>3. 信息的使用</h3>
        <ul>
          <li>用于提供、维护和改进本服务（如多轮对话上下文、跨设备历史同步）。</li>
          <li>用于安全防护（如限流、异常行为检测、敏感内容过滤）。</li>
          <li>不会将您的个人数据出售给任何第三方。</li>
        </ul>

        <h3>4. 与第三方的共享</h3>
        <ul>
          <li>为提供 AI 能力，对话内容可能经加密通道发送至第三方大模型服务商进行处理；该服务商不获取您的账号身份信息。</li>
          <li>为提供基础服务，可能使用云服务器、对象存储等基础设施服务商。</li>
          <li>除上述情形或法律法规要求外，我们不会向第三方披露您的个人信息。</li>
        </ul>

        <h3>5. 您的权利</h3>
        <ul>
          <li>访问与导出：您可随时查看自己的会话记录。</li>
          <li>更正与删除：您可通过个人中心或联系客服修改个人信息、删除会话记录。</li>
          <li>注销账号：如需注销账号，请通过意见反馈或邮件联系我们处理。</li>
        </ul>

        <h3>6. 未成年人保护</h3>
        <p>
          本服务面向成年用户。若您为未满 18 周岁的未成年人，请在监护人陪同下使用本服务，并由监护人阅读并同意本政策。
        </p>

        <h3>7. 政策变更与联系我们</h3>
        <p>
          本政策如有更新将在本页面公示。如您对本政策或数据使用有任何疑问，可通过「意见反馈」页面或发送邮件至
          <a href="mailto:2770382967@qq.com">2770382967@qq.com</a> 联系我们。
        </p>
      </section>

      <div class="agreement-footer">
        <router-link to="/register" class="agreement-footer__link">同意并前往注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';

// 支持锚点直达（页脚「隐私政策」链接 /agreement#privacy 滚动到对应章节）
onMounted(() => {
  const hash = window.location.hash;
  if (hash === '#privacy' || hash === '#agreement') {
    const el = document.getElementById(hash.slice(1));
    if (el) {
      setTimeout(() => el.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100);
    }
  }
});
</script>

<style scoped>
.agreement-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--app-space-xl);
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
}

.theme-dark .agreement-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.agreement-page__bar {
  width: 100%;
  max-width: 820px;
  padding: var(--app-space-md) 0;
}

.agreement-page__back {
  display: inline-flex;
  align-items: center;
  padding: 9px 18px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #2f6bff, #17c3f8);
  border-radius: 9999px;
  text-decoration: none;
  box-shadow: 0 4px 14px rgba(47, 107, 255, 0.3);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.agreement-page__back:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(47, 107, 255, 0.42);
  text-decoration: none;
}

.agreement-card {
  width: 100%;
  max-width: 820px;
  padding: 40px 48px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  margin-bottom: var(--app-space-xl);
}

.agreement-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 800;
}

.agreement-updated {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin: 0 0 var(--app-space-xl);
}

.agreement-section {
  margin-bottom: var(--app-space-xl);
}

.agreement-section h2 {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 var(--app-space-md);
  padding-bottom: var(--app-space-sm);
  border-bottom: 1px solid var(--app-border);
}

.agreement-section h3 {
  font-size: 15px;
  font-weight: 600;
  margin: var(--app-space-md) 0 var(--app-space-sm);
}

.agreement-section p,
.agreement-section li {
  font-size: 14px;
  line-height: 1.8;
  color: var(--app-text-secondary);
}

.agreement-section ul {
  margin: 0;
  padding-left: 20px;
}

.agreement-section li {
  margin: 4px 0;
}

.agreement-section a {
  color: var(--app-accent-blue);
}

.agreement-footer {
  text-align: center;
  padding-top: var(--app-space-lg);
  border-top: 1px solid var(--app-border);
}

.agreement-footer__link {
  display: inline-block;
  padding: 10px 32px;
  border-radius: 9999px;
  background: #409eff;
  color: #fff;
  font-weight: 600;
  text-decoration: none;
  box-shadow: 0 4px 18px rgba(64, 158, 255, 0.4);
}

.agreement-footer__link:hover {
  box-shadow: 0 6px 22px rgba(64, 158, 255, 0.5);
}

@media (max-width: 767px) {
  .agreement-page {
    padding: var(--app-space-md);
  }

  .agreement-card {
    padding: 24px 18px;
  }
}
</style>
