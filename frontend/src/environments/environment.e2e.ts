export const environment = {
  production: false,
  apiUrl: 'http://localhost:50003',
  authMode: 'api-key' as 'oidc' | 'api-key' | 'none',
  oidc: {
    authority: 'https://localhost',
    clientId: 'dummy',
    redirectUrl: 'http://localhost:4200/',
    postLogoutRedirectUri: 'http://localhost:4200/',
    scope: 'openid',
    responseType: 'code',
    silentRenew: false,
    useRefreshToken: false,
    renewTimeBeforeTokenExpiresInSeconds: 30,
    autoUserInfo: false,
    logLevel: 3,
  },
};
