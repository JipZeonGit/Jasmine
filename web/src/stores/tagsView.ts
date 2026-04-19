import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<RouteLocationNormalized[]>([])

  function addView(view: RouteLocationNormalized) {
    if (visitedViews.value.some((v) => v.path === view.path)) return
    if (view.meta && view.meta.title && view.path !== '/login') {
      if (view.path === '/dashboard') {
        visitedViews.value.unshift(Object.assign({}, view))
      } else {
        visitedViews.value.push(Object.assign({}, view))
      }
    }
  }

  function delView(view: RouteLocationNormalized) {
    if (view.path === '/dashboard') return
    const i = visitedViews.value.findIndex((v) => v.path === view.path)
    if (i > -1) {
      visitedViews.value.splice(i, 1)
    }
  }

  function delAllViews() {
    const affixTags = visitedViews.value.filter(tag => tag.path === '/dashboard')
    visitedViews.value = affixTags
  }

  return { visitedViews, addView, delView, delAllViews }
})
