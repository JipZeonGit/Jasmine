import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'
import type { MenuItem } from '@/types'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import NotFoundView from '@/views/error/NotFoundView.vue'
import ProfileView from '@/views/profile/ProfileView.vue'
import FlowerManageView from '@/views/flower/FlowerManageView.vue'
import InventoryManageView from '@/views/inventory/InventoryManageView.vue'
import SalesManageView from '@/views/sales/SalesManageView.vue'
import VipManageView from '@/views/vip/VipManageView.vue'
import AppointmentManageView from '@/views/appointment/AppointmentManageView.vue'
import UserManageView from '@/views/system/UserManageView.vue'
import RoleManageView from '@/views/system/RoleManageView.vue'

const componentMap: Record<string, Component> = {
  'dashboard/index': DashboardView,
  'profile/index': ProfileView,
  'custom/flowerManage': FlowerManageView,
  'custom/inventoryManage': InventoryManageView,
  'custom/salesManage': SalesManageView,
  'custom/VIP': VipManageView,
  'custom/appointment': AppointmentManageView,
  'system/user': UserManageView,
  'system/role': RoleManageView,
}

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, hidden: true },
  },
  {
    path: '/',
    component: AppLayout,
    name: 'Root',
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: DashboardView,
        meta: { title: '首页', icon: 'House' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: ProfileView,
        meta: { title: '个人信息', hidden: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundView,
    meta: { hidden: true },
  },
]

export function buildDynamicRoutes(menuList: MenuItem[]): RouteRecordRaw[] {
  return menuList.map((item) => mapMenuToRoute(item))
}

function mapMenuToRoute(item: MenuItem): RouteRecordRaw {
  const route = {
    path: item.path,
    name: item.name || item.title,
    meta: {
      title: item.title,
      icon: item.icon,
      hidden: item.hidden,
      alwaysShow: item.alwaysShow,
    },
  } as unknown as RouteRecordRaw

  if (item.component === 'Layout') {
    route.component = AppLayout
  } else {
    route.component = componentMap[item.component] || NotFoundView
  }

  if (item.redirect) {
    route.redirect = item.redirect
  }

  if (item.children?.length) {
    route.children = item.children.map((child) => mapMenuToRoute(child))
  }

  return route
}
