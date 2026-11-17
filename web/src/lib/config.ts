const trim = (url: string) => url.replace(/\/+$/, "");

/** Api base URL incl. prefix, e.g. http://localhost:8080/api */
export const apiUrl = trim(import.meta.env.API_URL);

/** REST base: `${apiUrl}/v1` */
export const restUrl = `${apiUrl}/v1`;

/** GraphQL endpoint: `${apiUrl}/graphql` */
export const graphqlUrl = `${apiUrl}/graphql`;

/** Router basepath: path of WEB_URL, e.g. /web */
export const basePath = new URL(import.meta.env.WEB_URL).pathname.replace(/\/+$/, "") || "/";
