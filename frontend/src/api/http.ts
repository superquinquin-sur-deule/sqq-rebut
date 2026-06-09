import Axios, { type AxiosRequestConfig } from 'axios';

// baseURL vide → URLs relatives (/api/...) résolues contre l'origine courante,
// puis proxifiées vers Quarkus :8080 par le dev-server Vite.
export const AXIOS_INSTANCE = Axios.create({ baseURL: '' });

/** Mutator utilisé par le client généré par orval. Renvoie directement les données. */
export const http = <T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<T> => {
  return AXIOS_INSTANCE({ ...config, ...options }).then(({ data }) => data as T);
};

export default http;
