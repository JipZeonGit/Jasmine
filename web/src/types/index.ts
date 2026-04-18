export interface ResultEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface TableData<T> {
  total: number
  rows: T[]
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  refreshToken: string
}

export interface MenuItem {
  menuId: number
  parentId?: number
  path: string
  component: string
  redirect?: string
  hidden?: boolean
  alwaysShow?: boolean
  name?: string
  title: string
  icon?: string
  children?: MenuItem[]
}

export interface UserInfoVO {
  name: string
  avatar?: string
  phone?: string
  email?: string
  status?: number
  roles: string[]
  menuList: MenuItem[]
}

export interface VipVO {
  id: number
  vid: string
  name: string
  sex: string
  phone: string
}

export interface FlowerVO {
  id: number
  name: string
  unit: string
  salePrice: number
  costPrice: number
  safeStock: number
  currentStock: number
  status: number
}

export interface InventoryVO {
  id: number
  bizNo: string
  flowerId: number
  flowerName: string
  bizType: string
  bizTypeLabel: string
  quantity: number
  beforeStock: number
  afterStock: number
  unitCost?: number | null
  totalCost?: number | null
  remark?: string
  date: string
}

export interface SalesItemVO {
  id: number
  flowerId: number
  flowerName: string
  quantity: number
  unitPrice: number
  unitCost: number
  amount: number
  costAmount: number
}

export interface SalesVO {
  id: number
  orderNo: string
  vipId?: number | null
  vipName?: string | null
  vipPhone?: string | null
  totalAmount: number
  remark?: string | null
  itemCount: number
  date: string
  items: SalesItemVO[]
}

export interface TodayBusinessSummaryVO {
  todaySalesAmount: number
  todayPurchaseCost: number
  todayGrossProfit: number
  todayNetCashflow: number
  todaySalesOrderCount: number
  todayPurchaseCount: number
}

export interface AppointmentVO {
  id: number
  vid: string
  name: string
  sex: string
  phone: string
  date: string
  content: string
}

export interface RoleVO {
  roleId: number
  roleName: string
  roleDesc: string
  menuIdList?: number[]
}

export interface UserVO {
  id: number
  username: string
  phone?: string
  status?: number
  email?: string
  roleIdList?: number[]
}
