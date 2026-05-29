import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Store } from '@ngxs/store';
import { EMPTY, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TenantState } from '../../store/tenant/tenant.state';
import { AuthState } from '../../store/auth/auth.state';

const apiBasePath = new URL(environment.apiUrl).pathname;

/**
 * HTTP interceptor for OIDC/JWT authentication mode ({@code environment.authEnabled === true}).
 *
 * For every outgoing request whose path starts with the configured API base path, this interceptor
 * appends the {@code Authorization: Bearer <token>} header from {@link AuthState} and, when a
 * tenant is selected, the {@code X-Tenant-ID} header from {@link TenantState}.
 * On a {@code 401} response it attempts a silent token refresh via
 * {@code OidcSecurityService.forceRefreshSession()} and retries the original request with the new
 * token. If the refresh fails, the user is redirected to {@code /unauthorized}.
 * Requests outside the API base path are forwarded unchanged.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const reqPath = new URL(req.url, window.location.origin).pathname;
  if (!reqPath.startsWith(apiBasePath)) {
    return next(req);
  }

  const oidcSecurityService = inject(OidcSecurityService);
  const store = inject(Store);
  const router = inject(Router);

  const token = store.selectSnapshot(AuthState.accessToken);
  const tenantId = store.selectSnapshot(TenantState.selectedTenantId);

  let headers = req.headers;
  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }
  if (tenantId) {
    headers = headers.set('X-Tenant-ID', tenantId);
  }

  return next(req.clone({ headers })).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      return oidcSecurityService.forceRefreshSession().pipe(
        switchMap((result) => {
          if (result.isAuthenticated && result.accessToken) {
            const retried = req.clone({
              headers: req.headers.set('Authorization', `Bearer ${result.accessToken}`),
            });
            return next(retried);
          }
          router.navigate(['/unauthorized']);
          return EMPTY;
        }),
        catchError(() => {
          router.navigate(['/unauthorized']);
          return EMPTY;
        }),
      );
    }),
  );
};
