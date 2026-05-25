import { createRouter, createWebHistory } from 'vue-router';
import AgentDashboard from '../components/AgentDashboard.vue';
import PageNavigator from '../components/PageNavigator.vue';
import ComponentAnalyzer from '../components/ComponentAnalyzer.vue';
import DatabaseExplorer from '../components/DatabaseExplorer.vue';
import AIChat from '../components/AIChat.vue';

const routes = [
  {
    path: '/agent',
    name: 'agent-dashboard',
    component: AgentDashboard,
    children: [
      {
        path: 'navigator',
        name: 'page-navigator',
        component: PageNavigator,
      },
      {
        path: 'components',
        name: 'component-analyzer',
        component: ComponentAnalyzer,
      },
      {
        path: 'database',
        name: 'database-explorer',
        component: DatabaseExplorer,
      },
      {
        path: 'chat',
        name: 'ai-chat',
        component: AIChat,
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
