<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { salesApi } from '@/api/sales'
import type { TodayBusinessSummaryVO } from '@/types'
import { DataLine, Money, Sell, Wallet } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const dateString = computed(() => {
  const d = new Date()
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日`
})

const todaySummary = ref<TodayBusinessSummaryVO>({
  todaySalesAmount: 0,
  todayPurchaseCost: 0,
  todayGrossProfit: 0,
  todayNetCashflow: 0,
  todaySalesOrderCount: 0,
  todayPurchaseCount: 0
})

onMounted(async () => {
  const response = await salesApi.getTodaySummary()
  todaySummary.value = response.data || todaySummary.value
})
</script>

<template>
  <div class="dashboard-wrap">
    <div class="welcome-box">
      <div class="welcome-title">{{ authStore.name || 'admin' }}，您好！欢迎使用 Jasmine 智慧花卉管理系统。</div>
      <div class="welcome-sub text-gray">今天是 {{ dateString }}，为您呈现今日最新的经营数据。</div>
    </div>

    <div class="stat-deck">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">今日销售额</span>
          <div class="stat-icon-box bg-blue"><el-icon><Sell /></el-icon></div>
        </div>
        <div class="stat-value">¥ {{ Number(todaySummary.todaySalesAmount || 0).toFixed(2) }}</div>
        <div class="stat-footer">包含成单项目 {{ todaySummary.todaySalesOrderCount || 0 }} 笔</div>
      </div>

      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">今日进货成本</span>
          <div class="stat-icon-box bg-orange"><el-icon><Money /></el-icon></div>
        </div>
        <div class="stat-value">¥ {{ Number(todaySummary.todayPurchaseCost || 0).toFixed(2) }}</div>
        <div class="stat-footer">执行采购变动 {{ todaySummary.todayPurchaseCount || 0 }} 笔</div>
      </div>

      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">今日销售毛利</span>
          <div class="stat-icon-box bg-green"><el-icon><DataLine /></el-icon></div>
        </div>
        <div class="stat-value">¥ {{ Number(todaySummary.todayGrossProfit || 0).toFixed(2) }}</div>
        <div class="stat-footer">业务基础毛利润估算</div>
      </div>

      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">今日净流入</span>
          <div class="stat-icon-box bg-purple"><el-icon><Wallet /></el-icon></div>
        </div>
        <div class="stat-value">¥ {{ Number(todaySummary.todayNetCashflow || 0).toFixed(2) }}</div>
        <div class="stat-footer">总计流入现金流</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-wrap {
  display: flex;
  flex-direction: column;
}

.welcome-box {
  background: var(--el-bg-color-overlay);
  padding: 24px 32px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.text-gray {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.stat-deck {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-card {
  background: var(--el-bg-color-overlay);
  border-radius: 8px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

.stat-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.bg-blue { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.bg-orange { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }
.bg-green { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.bg-purple { background: rgba(142, 68, 173, 0.1); color: #8e44ad; }

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 16px 0 12px 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  letter-spacing: -0.5px;
}

.stat-footer {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

@media (max-width: 1400px) {
  .stat-deck {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-deck {
    grid-template-columns: 1fr;
  }
}
</style>
