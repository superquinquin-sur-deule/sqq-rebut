<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from './api';

// Bannière « staging » : visible quand le backend a SQQ_DLC_STAGING=true.
const staging = ref(false);

onMounted(async () => {
  try {
    staging.value = (await api.getConfig()).staging;
  } catch {
    // Pas de config joignable → on ne montre rien (on suppose la prod).
  }
});
</script>

<template>
  <div v-if="staging" class="staging-ribbon" aria-hidden="true"><span>Staging</span></div>
  <router-view />
</template>
