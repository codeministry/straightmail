import { TestBed } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { AuthState, AuthStateModel } from './auth.state';
import { CheckAuth, Login, LoginComplete, Logout, SetUserData } from './auth.actions';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TenantState } from '../tenant/tenant.state';
import { environment } from '../../../environments/environment';

describe('AuthState', () => {
  let store: Store;
  let oidcSecurityService: OidcSecurityService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxsModule.forRoot([AuthState, TenantState])],
      providers: [
        {
          provide: OidcSecurityService,
          useValue: {
            checkAuth: vi.fn(),
            authorize: vi.fn(),
            logoff: vi.fn(),
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    store = TestBed.inject(Store);
    oidcSecurityService = TestBed.inject(OidcSecurityService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should have initial state', () => {
    const state: AuthStateModel = store.selectSnapshot((state) => state.auth);
    expect(state.isAuthenticated).toBe(false);
    expect(state.userData).toBeNull();
    expect(state.accessToken).toBeNull();
    expect(state.roles).toEqual([]);
  });

  it('should check auth and update state', () => {
    const authResponse = {
      isAuthenticated: true,
      userData: { name: 'Test User' },
      accessToken: 'token123',
    };
    (oidcSecurityService.checkAuth as any).mockReturnValue(of(authResponse));

    store.dispatch(new CheckAuth());

    const state = store.selectSnapshot((state) => state.auth);
    expect(state.isAuthenticated).toBe(true);
    expect(state.userData).toEqual({ name: 'Test User' });
    expect(state.accessToken).toBe('token123');
  });

  it('should call authorize on login', () => {
    store.dispatch(new Login());
    expect(oidcSecurityService.authorize).toHaveBeenCalled();
  });

  it('should clear auth and tenant state on logout', () => {
    (oidcSecurityService.logoff as any).mockReturnValue(of(null));
    store.dispatch(new Logout());

    const auth = store.selectSnapshot((state) => state.auth);
    expect(auth.isAuthenticated).toBe(false);
    expect(auth.userData).toBeNull();
    expect(auth.accessToken).toBeNull();
    expect(auth.roles).toEqual([]);

    const tenant = store.selectSnapshot((state) => state.tenant);
    expect(tenant.selectedTenantId).toBeNull();
    expect(tenant.tenants).toEqual([]);
  });

  it('should load tenants on LoginComplete when authenticated', () => {
    const payload = { realm_access: { roles: ['USER'] } };
    const encoded = btoa(JSON.stringify(payload));
    const accessToken = `header.${encoded}.signature`;

    (oidcSecurityService.checkAuth as any).mockReturnValue(
      of({ isAuthenticated: true, userData: { name: 'New User' }, accessToken }),
    );

    store.dispatch(new LoginComplete());

    const req = httpTesting.expectOne(`${environment.apiUrl}/v1/tenants/me`);
    req.flush([
      {
        slug: 'acme',
        displayName: 'Acme',
        smtpTls: false,
        smtpSsl: false,
        hasApiKey: false,
        active: true,
      },
    ]);

    const tenant = store.selectSnapshot((state) => state.tenant);
    expect(tenant.tenants).toHaveLength(1);
    expect(tenant.selectedTenantId).toBe('acme');
  });

  it('should not load tenants on LoginComplete when not authenticated', () => {
    (oidcSecurityService.checkAuth as any).mockReturnValue(
      of({ isAuthenticated: false, userData: null, accessToken: null }),
    );

    store.dispatch(new LoginComplete());

    httpTesting.expectNone(`${environment.apiUrl}/v1/tenants/me`);
  });

  it('should set userData via SetUserData action', () => {
    const userData = { name: 'Jane Doe', email: 'jane@example.com' };
    store.dispatch(new SetUserData(userData));

    const state = store.selectSnapshot((state) => state.auth);
    expect(state.userData).toEqual(userData);
  });

  it('should extract roles from JWT realm_access', () => {
    // Encode payload with realm_access.roles
    const payload = { realm_access: { roles: ['ADMIN', 'USER'] } };
    const encoded = btoa(JSON.stringify(payload));
    const accessToken = `header.${encoded}.signature`;

    (oidcSecurityService.checkAuth as any).mockReturnValue(
      of({ isAuthenticated: true, userData: {}, accessToken }),
    );

    store.dispatch(new CheckAuth());

    const roles = store.selectSnapshot(AuthState.roles);
    expect(roles).toContain('ADMIN');
    expect(roles).toContain('USER');
  });

  it('should extract roles from userData if not in token', () => {
    (oidcSecurityService.checkAuth as any).mockReturnValue(
      of({
        isAuthenticated: true,
        userData: { roles: ['VIEWER'] },
        accessToken: null,
      }),
    );

    store.dispatch(new CheckAuth());

    const roles = store.selectSnapshot(AuthState.roles);
    expect(roles).toContain('VIEWER');
  });
});
