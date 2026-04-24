import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/learning',
      name: 'Learning',
      component: () => import('../views/Learning.vue')
    },
    {
      path: '/course/:id',
      name: 'CourseDetail',
      component: () => import('../views/CourseDetail.vue')
    },
    {
      path: '/community',
      name: 'Community',
      component: () => import('../views/Community.vue')
    },
    {
      path: '/post/:id',
      name: 'PostDetail',
      component: () => import('../views/PostDetail.vue')
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/Profile.vue')
    },
    {
      path: '/task',
      name: 'Task',
      component: () => import('../views/Task.vue')
    },
    {
      path: '/goal',
      name: 'Goal',
      component: () => import('../views/Goal.vue')
    },
    {
      path: '/wrongbook',
      name: 'WrongBook',
      component: () => import('../views/WrongBook.vue')
    },
    {
      path: '/auto-paper',
      name: 'AutoPaper',
      component: () => import('../views/AutoPaper.vue')
    },
    {
      path: '/learningAnalysis',
      name: 'LearningAnalysis',
      component: () => import('../views/LearningAnalysis.vue')
    },
    {
      path: '/face',
      name: 'Face',
      component: () => import('../views/Face.vue')
    }
  ]
})

export default router
