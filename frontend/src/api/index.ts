import { getRelevéDLCAPI } from './generated';

// Ré-exporte les types générés + une instance prête à l'emploi du client.
export * from './generated';

export const api = getRelevéDLCAPI();
