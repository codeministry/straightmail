export const environment = {
  production: false,
  get apiUrl(): string {
    return (window as any).__runtimeConfig?.apiUrl ?? 'http://localhost:50003/api';
  },
  get authMode(): 'oidc' | 'api-key' | 'none' {
    return (window as any).__runtimeConfig?.authMode ?? 'oidc';
  },
  oidc: {
    get authority(): string {
      return (
        (window as any).__runtimeConfig?.oidcAuthority ??
        'http://localhost:8090/realms/straightmail'
      );
    },
    clientId: 'straightmail',
    get redirectUrl(): string {
      return `${window.location.origin}/`;
    },
    get postLogoutRedirectUri(): string {
      return `${window.location.origin}/`;
    },
    scope: 'openid profile email',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
    renewTimeBeforeTokenExpiresInSeconds: 30,
    autoUserInfo: true,
    logLevel: 1, // 0 = None, 1 = Debug, 2 = Warn, 3 = Error
  },
};
