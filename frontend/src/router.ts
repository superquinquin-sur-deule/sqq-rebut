import { createRouter, createWebHistory } from 'vue-router';
import ScannetteView from './views/ScannetteView.vue';
import PosteView from './views/PosteView.vue';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/scannette' },
    { path: '/scannette', name: 'scannette', component: ScannetteView },
    { path: '/poste', name: 'poste', component: PosteView },
  ],
});
