<script setup lang="ts">
import { watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'

import { useTagsViewStore } from '@/stores/tagsView'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

function addTags() {
  if (route.name) {
    tagsViewStore.addView(route)
  }
}

onMounted(() => {
  addTags()
})

watch(
  () => route.path,
  () => {
    addTags()
  }
)

function closeSelectedTag(view: any) {
  tagsViewStore.delView(view)
  if (isActive(view)) {
    toLastView(tagsViewStore.visitedViews)
  }
}

function isActive(view: any) {
  return view.path === route.path
}

function toLastView(visitedViews: any[]) {
  const latestView = visitedViews.slice(-1)[0]
  if (latestView) {
    router.push(latestView.path)
  } else {
    router.push('/')
  }
}

function closeOthers() {
  tagsViewStore.delAllViews()
  addTags()
}
</script>

<template>
  <div class="tags-view-container">
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <div class="tags-scroll-wrap">
        <router-link
          v-for="tag in tagsViewStore.visitedViews"
          :key="tag.path"
          :to="{ path: tag.path, query: tag.query }"
          class="tags-view-item"
          :class="isActive(tag) ? 'active' : ''"
        >
          {{ tag.meta.title }}
          <el-icon v-if="tag.path !== '/dashboard'" class="icon-close" @click.prevent.stop="closeSelectedTag(tag)">
            <Close />
          </el-icon>
        </router-link>
      </div>
    </el-scrollbar>
  </div>
</template>

<style scoped>
.tags-view-container {
  height: 38px;
  width: 100%;
  background: var(--jasmine-card-bg);
  border-bottom: 1px solid var(--jasmine-border-solid);
  display: flex;
  align-items: center;
  padding: 0 16px;
}

:global(html.dark) .tags-view-container {
  background: #232324;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.tags-scroll-wrap {
  display: flex;
  align-items: center;
  height: 38px;
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
  height: 26px;
  line-height: 26px;
  border: 1px solid var(--jasmine-border-solid);
  color: var(--jasmine-text-muted);
  background: transparent;
  padding: 0 12px;
  font-size: 13px;
  margin-right: 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
  text-decoration: none;
  white-space: nowrap;
}

:global(html.dark) .tags-view-item {
  border: 1px solid rgba(255,255,255,0.08);
}

.tags-view-item.active {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  font-weight: 500;
}

:global(html.dark) .tags-view-item.active {
  background-color: rgba(45, 129, 255, 0.15);
  border-color: rgba(45, 129, 255, 0.4);
  color: #559bff;
}

.tags-view-item.active::before {
  content: '';
  background: var(--el-color-primary);
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}

:global(html.dark) .tags-view-item.active::before {
  background: #559bff;
}

.icon-close {
  border-radius: 50%;
  margin-left: 6px;
  margin-right: -4px;
  padding: 2px;
  font-size: 10px;
  width: 14px;
  height: 14px;
  transition: all 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.tags-view-item:not(.active) .icon-close:hover {
  background-color: #b4bccc;
  color: #fff;
}

:global(html.dark) .tags-view-item:not(.active) .icon-close:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.tags-view-item.active .icon-close:hover {
  background-color: var(--el-color-primary);
  color: #fff;
}

:global(html.dark) .tags-view-item.active .icon-close:hover {
  background-color: #559bff;
}
</style>
