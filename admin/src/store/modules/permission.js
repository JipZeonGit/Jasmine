import Layout from '@/layout'
import { constantRoutes } from '@/router'

// 后端返回的是菜单树，这里负责把菜单结构转换成 vue-router 可识别的路由对象。
function mapMenuToRoutes(menuList = []) {
  return menuList.map(menu => {
    const route = { ...menu }
    if (route.component === 'Layout') {
      route.component = Layout
    } else {
      route.component = require(`@/views/${route.component}.vue`).default
    }
    if (route.children && route.children.length) {
      route.children = mapMenuToRoutes(route.children)
    }
    return route
  })
}

const state = {
  routes: constantRoutes,
  addRoutes: []
}

const mutations = {
  SET_ROUTES: (state, routes) => {
    state.addRoutes = routes
    state.routes = constantRoutes.concat(routes)
  }
}

const actions = {
  generateRoutes({ commit }, menuList) {
    return new Promise(resolve => {
      const accessedRoutes = mapMenuToRoutes(menuList)
      // 404 放在动态路由最后，避免前面的业务菜单被兜底路由截胡。
      accessedRoutes.push({
        path: '*',
        redirect: '/404',
        hidden: true
      })
      commit('SET_ROUTES', accessedRoutes)
      resolve(accessedRoutes)
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
