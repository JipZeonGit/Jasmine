const getters = {
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  name: state => state.user.name,
  menuList: state => state.user.menuList,
  visitedViews: state => state.tagsView.visitedViews,
  cachedViews: state => state.tagsView.cachedViews,
  premission_routes: state => state.premission.routes,
  phone: state => state.user.phone,
  email: state => state.user.email,
  status: state => state.user.status,
  roles: state => state.user.roles
}
export default getters
