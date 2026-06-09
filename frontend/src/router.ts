import { defineComponent } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import ScannetteView from './views/ScannetteView.vue';
import PosteView from './views/PosteView.vue';
import { api } from './api';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/scannette' },
    { path: '/scannette', name: 'scannette', component: ScannetteView },
    {
      path: '/releves',
      component: defineComponent({ render: () => null }),
      beforeEnter: async (_to, _from, next) => {
        try {
          const list = await api.getReleves();
          if (list.length) next({ name: 'poste', params: { releveId: String(list[0].id) }, replace: true });
          else next({ name: 'historique', replace: true });
        } catch {
          next({ name: 'historique', replace: true });
        }
      },
    },
    { path: '/releves/:releveId(\\d+)', name: 'poste', component: PosteView },
    { path: '/historique', name: 'historique', component: () => import('./views/HistoriqueView.vue') },
    { path: '/poste', redirect: '/releves' },
  ],
});
