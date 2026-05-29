import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngxs/store';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiKeyState } from '../../store/api-key/api-key.state';
import { TenantState } from '../../store/tenant/tenant.state';
import { ClearApiKey } from '../../store/api-key/api-key.actions';
import { TenantActions } from '../../store/tenant/tenant.actions';

const apiBasePath = new URL(environment.apiUrl).pathname;

/**
 * HTTP interceptor for API-key authentication mode ({@code environment.authEnabled === false}).
 *
 * For every outgoing request whose path starts with the configured API base path, this interceptor
 * appends the {@code X-API-KEY} header (from {@link ApiKeyState}) and, when a tenant is selected,
 * the {@code X-Tenant-ID} header (from {@link TenantState}).
 * On a {@code 401} response it clears stored credentials and redirects to {@code /api-key-login}.
 * Requests outside the API base path are forwarded unchanged.
 */
export const apiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  const reqPath = new URL(req.url, window.location.origin).pathname;
  if (!reqPath.startsWith(apiBasePath)) {
    return next(req);
  }

  const store = inject(Store);
  const router = inject(Router);

  let apiKey: string | null = null;
  let tenantId: string | null = null;

  try {
    apiKey = store.selectSnapshot(ApiKeyState.apiKey);
    tenantId = store.selectSnapshot(TenantState.selectedTenantId);
  } catch {
    // NGXS states not yet initialized (before @@INIT); pass request through without headers.
    return next(req);
  }

  let headers = req.headers;
  if (apiKey) {
    headers = headers.set('X-API-KEY', apiKey);
  }
  if (tenantId) {
    headers = headers.set('X-Tenant-ID', tenantId);
  }

  return next(req.clone({ headers })).pipe(
    catchError((error: HttpErrorResponse) => {
      // Only clear credentials and redirect when a key was actually stored (session expiry).
      // During a login attempt the key is not yet in the store, so the component handles the error.
      if (error.status === 401 && apiKey) {
        store.dispatch(new ClearApiKey());
        store.dispatch(new TenantActions.ClearTenants());
        router.navigate(['/api-key-login']);
      }
      return throwError(() => error);
    }),
  );
};
