import Vue from 'vue'
import VueRouter from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'

Vue.use(VueRouter)

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/',
        component: Layout,
        redirect: '/analyse',
        children: [
            {
                path: '/log',
                name: 'Log',
                component: () => import('../views/Log.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/account',
                name: 'Account',
                component: () => import('../views/Account.vue'),
                meta: { requiresAuth: true, roles: ['root'] }
            },
            {
                path: '/dictionary',
                name: 'Dictionary',
                component: () => import('../views/Dictionary.vue'),
                meta: { requiresAuth: true, roles: ['root'] }
            },
            {
                path: '/goal',
                name: 'Goal',
                component: () => import('../views/Goal.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/llm',
                name: 'LLM',
                component: () => import('../views/LlM.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/course',
                name: 'Course',
                component: () => import('../views/Course.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/banner',
                name: 'Banner',
                component: () => import('../views/Banner.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/forum',
                name: 'Forum',
                component: () => import('../views/Forum.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/comment',
                name: 'Comment',
                component: () => import('../views/Comment.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/question',
                name: 'Question',
                component: () => import('../views/Question.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/exam',
                name: 'Exam',
                component: () => import('../views/Exam.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/analyse',
                name: 'Analyse',
                component: () => import('../views/Analyse.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/medal',
                name: 'Medal',
                component: () => import('../views/Medal.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
            {
                path: '/testpaper',
                name: 'TestPaper',
                component: () => import('../views/TestPaper.vue'),
                meta: { requiresAuth: true, roles: ['root', 'admin'] }
            },
        ]
    }
]

const router = new VueRouter({
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    const role = localStorage.getItem('role')

    if (to.matched.some(record => record.meta.requiresAuth)) {
        if (!token) {
            next('/login')
        } else {
            if (to.meta.roles && !to.meta.roles.includes(role)) {
                next('/analyse')
            } else {
                next()
            }
        }
    } else {
        next()
    }
})

export default router
