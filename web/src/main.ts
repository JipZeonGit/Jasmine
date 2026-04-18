import { createApp } from 'vue'
import ElementPlus, { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'
import './styles/index.css'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$confirm = ElMessageBox.confirm

app.mount('#app')
