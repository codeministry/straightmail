import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthState } from '../../store/auth/auth.state';
import { TenantState } from '../../store/tenant/tenant.state';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let store: Store;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([AuthState, TenantState])],
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: vi.fn(), navigateByUrl: vi.fn() } },
        { provide: OidcSecurityService, useValue: { forceRefreshSession: () => of(null) } },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    store = TestBed.inject(Store);
    store.reset({
      ...store.snapshot(),
      auth: { isAuthenticated: true, accessToken: 'jwt-token', userData: null, roles: [] },
    });
  });

  afterEach(() => httpTesting.verify());

  it('should attach the bearer token on the API base path', () => {
    http.get('/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('/api/v1/templates');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush({});
  });

  it('should not send the bearer token to a foreign origin with the same path', () => {
    // same path, different host — the token must not travel there
    http.get('https://attacker.example/api/v1/templates').subscribe();

    const req = httpTesting.expectOne('https://attacker.example/api/v1/templates');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.headers.has('X-Tenant-ID')).toBe(false);
    req.flush({});
  });

  it('should not modify requests outside the API base path', () => {
    http.get('/assets/i18n/en.json').subscribe();

    const req = httpTesting.expectOne('/assets/i18n/en.json');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
