import { createRouter, createWebHashHistory } from 'vue-router'

import BookDetailView from '../views/BookDetailView.vue'
import BookListView from '../views/BookListView.vue'
import MyLoansView from '../views/MyLoansView.vue'

// Hash history keeps deep links working when the built assets are served as plain
// static files by Spring Boot, which needs no extra route-fallback controller.
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'books', component: BookListView },
    { path: '/books/:id', name: 'book-detail', component: BookDetailView },
    { path: '/me/loans', name: 'my-loans', component: MyLoansView },
    // Unknown hash routes would otherwise render an empty page.
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

export default router
