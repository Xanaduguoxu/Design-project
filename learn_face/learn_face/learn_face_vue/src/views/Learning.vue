<template>
  <main class="brutalist-learning-page">
    <section class="hero-paper-section">
      <div class="container hero-container">
        <div class="hero-block-layout">
          <div class="hero-text-area">
            <div class="tag-box">SYSTEM/LEARN</div>
            <h1 class="hero-headline">KNOWLEDGE <br>ARCHIVE</h1>
            <div class="divider-line"></div>
            <p class="hero-desc">Access the central repository. Filter by difficulty level to retrieve targeted learning modules.</p>
          </div>
          <div class="hero-visual-area">
            <div class="grid-paper-bg">
              <div class="card-rotate-item">
                <div class="inner-card">
                  <span class="x-mark">+</span>
                  <span class="label-text">COURSE</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="content-area-white">
      <div class="container">
        <section class="courses-section">

          <div class="filter-bar-brutal">
            <div class="filter-left">
              <h2 class="filter-title">FILTERS</h2>
            </div>
            <div class="filter-right">
              <div class="filter-group-brutal">
                <el-radio-group v-model="courseFilter" size="large" class="brutal-radio-group">
                  <el-radio-button v-for="category in categoryOptions" :key="category.code" :label="category.code">
                    {{ category.value }}
                  </el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </div>

          <div class="course-grid-brutal">
            <el-row :gutter="0">
              <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="course in paginatedCourses" :key="course.id"
                class="grid-item-brutal">
                <ListCard1 :course="course" @click="handleCardClick" />
              </el-col>
            </el-row>
          </div>

          <div class="pagination-brutal">
            <el-pagination @current-change="handlePageChange" :current-page="currentPage" :page-size="pageSize"
              :total="courses.length" layout="prev, pager, next" background class="brutal-pagination" />
          </div>
        </section>
      </div>
    </div>
  </main>
</template>

<script>
import ListCard1 from '@/components/ListCard1.vue'
import { getCoursesAPI, getDictionaryAPI } from '@/utils/api.js'

export default {
  name: 'Learning',
  components: {
    ListCard1
  },
  data() {
    return {
      courseFilter: 'all',
      currentPage: 1,
      pageSize: 8,
      courses: [],
      categoryOptions: [
        { code: 'all', value: 'All Courses' }
      ]
    }
  },

  computed: {
    paginatedCourses() {
      return this.courses
    }
  },

  watch: {
    courseFilter() {
      this.handleFilterChange()
    }
  },

  mounted() {
    if (this.$route.query.category) {
      this.courseFilter = this.$route.query.category;
    }
    this.fetchCourses()
    this.fetchCategoryDictionary()
  },

  methods: {
    async fetchCourses() {
      try {
        const params = {
          currentPage: this.currentPage,
          pageSize: this.pageSize,
          category: this.courseFilter === 'all' ? '' : this.courseFilter,
          level: ''
        }

        const response = await getCoursesAPI(params)

        if (!response || !response.data) {
          this.courses = []
          return
        }

        this.courses = response.data.map(item => ({
          id: item.id,
          name: item.name,
          teacher: item.teacher,
          cover: item.image,
          views: item.views,
          duration: item.duration,
          category: this.mapCategoryCodeToValue(item.category)
        }))
      } catch (error) {
        console.error('API Error:', error)
        this.courses = []
      }
    },

    async fetchCategoryDictionary() {
      try {
        const response = await getDictionaryAPI('course_level')
        if (response && Array.isArray(response)) {
          this.categoryOptions = [
            { code: 'all', value: 'All Courses' },
            ...response.map(item => ({
              code: item.code,
              value: item.value
            }))
          ]
        }
      } catch (error) {
        this.categoryOptions = [
          { code: 'all', value: 'All Courses' },
          { code: 'beginner', value: 'Basic' },
          { code: 'intermediate', value: 'Core' },
          { code: 'advanced', value: 'Advanced' }
        ]
      }
    },

    mapCategoryCodeToValue(code) {
      const category = this.categoryOptions.find(cat => cat.code === code);
      return category ? category.value : code;
    },

    handleCardClick(course) {
      this.$router.push(`/course/${course.id}`)
    },

    handleFilterChange() {
      this.currentPage = 1
      this.fetchCourses()
    },

    handlePageChange(page) {
      this.currentPage = page
      this.fetchCourses()
      this.$nextTick(() => {
        window.scrollTo({ top: 400, behavior: 'smooth' })
      })
    }
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;600;700&display=swap');

.brutalist-learning-page {
  min-height: 100vh;
  background-color: #E8E8E8;
  font-family: 'Space Grotesk', sans-serif;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.hero-paper-section {
  background-color: #fff;
  padding: 100px 0 80px;
  position: relative;
  overflow: hidden;
  border-bottom: 4px solid #000;
}

.hero-block-layout {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 60px;
  align-items: center;
}

.hero-text-area {
  position: relative;
}

.tag-box {
  display: inline-block;
  border: 2px solid #000;
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  margin-bottom: 30px;
  background: #fff;
}

.hero-headline {
  font-size: 4.5rem;
  font-weight: 700;
  line-height: 0.95;
  margin: 0 0 30px 0;
  text-transform: uppercase;
  letter-spacing: -2px;
}

.divider-line {
  width: 80px;
  height: 6px;
  background: #000;
  margin-bottom: 30px;
}

.hero-desc {
  font-size: 1rem;
  color: #333;
  line-height: 1.6;
  max-width: 400px;
  font-weight: 400;
}

.hero-visual-area {
  position: relative;
  display: flex;
  justify-content: center;
}

.grid-paper-bg {
  position: relative;
  width: 280px;
  height: 280px;
  background-image: linear-gradient(#ccc 1px, transparent 1px), linear-gradient(90deg, #ccc 1px, transparent 1px);
  background-size: 20px 20px;
  border: 2px solid #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-rotate-item {
  width: 180px;
  height: 240px;
  background: #000;
  border: 2px solid #000;
  transform: rotate(-5deg);
  transition: transform 0.3s ease;
  cursor: pointer;
}

.card-rotate-item:hover {
  transform: rotate(0deg) scale(1.05);
}

.inner-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  text-align: center;
}

.x-mark {
  font-size: 4rem;
  font-weight: 200;
  line-height: 1;
  margin-bottom: 10px;
}

.label-text {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
}

.content-area-white {
  background: #E8E8E8;
  padding: 80px 0 100px;
}

.courses-section {
  background: #fff;
  border: 2px solid #000;
  padding: 40px;
  box-shadow: 8px 8px 0 #000;
}

.filter-bar-brutal {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 2px solid #000;
}

.filter-title {
  font-size: 1.5rem;
  font-weight: 700;
  text-transform: uppercase;
  margin: 0;
}

.filter-group-brutal {
  display: flex;
  gap: 10px;
}

:deep(.brutal-radio-group .el-radio-button__inner) {
  background: #fff;
  border: 2px solid #000 !important;
  border-radius: 0 !important;
  color: #000;
  font-weight: 700;
  padding: 10px 24px;
  box-shadow: none !important;
  transition: all 0.1s ease;
  margin-right: -2px;
  text-transform: uppercase;
  font-size: 12px;
}

:deep(.brutal-radio-group .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #000;
  color: #fff;
  box-shadow: 4px 4px 0 #000 !important;
  transform: translate(-2px, -2px);
}

:deep(.brutal-radio-group .el-radio-button__inner:hover) {
  background: #f0f0f0;
}

.course-grid-brutal {
  min-height: 400px;
}

.grid-item-brutal {
  padding: 20px;
  border: 1px dashed #ccc;
  transition: all 0.2s ease;
}

.grid-item-brutal:hover {
  background: #fafafa;
  border-color: #000;
}

.pagination-brutal {
  margin-top: 60px;
  display: flex;
  justify-content: center;
  padding-top: 40px;
  border-top: 2px solid #eee;
}

:deep(.brutal-pagination.is-background .el-pager li) {
  background-color: #fff;
  border: 2px solid #000;
  border-radius: 0;
  color: #000;
  font-weight: 700;
  margin: 0 4px;
  box-shadow: none;
}

:deep(.brutal-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #000;
  color: #fff;
  box-shadow: 4px 4px 0 #ccc;
}

:deep(.brutal-pagination.is-background .btn-prev),
:deep(.brutal-pagination.is-background .btn-next) {
  background-color: #fff;
  border: 2px solid #000;
  border-radius: 0;
  color: #000;
  font-weight: 700;
}

@media (max-width: 768px) {
  .hero-block-layout {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .hero-headline {
    font-size: 3rem;
  }

  .hero-desc {
    margin: 0 auto;
  }

  .grid-paper-bg {
    display: none;
  }

  .filter-bar-brutal {
    flex-direction: column;
    gap: 20px;
  }

  .courses-section {
    padding: 20px;
  }
}
</style>