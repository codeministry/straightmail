import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { of } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import { CheckAuth, Login, LoginComplete, Logout, SetUserData } from './auth.actions';
import { TenantActions } from '../tenant/tenant.actions';

/** Shape of the data persisted in the NGXS {@link AuthState}. */
export interface AuthStateModel {
  /** Whether the user is currently authenticated. */
  isAuthenticated: boolean;
  /** Raw user-data claims object returned by the OIDC provider. */
  userData: any;
  /** Current JWT access token, or {@code null} when not authenticated. */
  accessToken: string | null;
  /** Roles extracted from the JWT {@code realm_access.roles} claim or user-data claims. */
  roles: string[];
}

/**
 * NGXS state for OIDC/JWT authentication.
 *
 * Manages the authentication lifecycle: session check on startup, OIDC login redirect, silent
 * token refresh on 401, and logout. Roles are extracted from the JWT payload's
 * {@code realm_access.roles} claim (Keycloak convention) with fallbacks to user-data claims.
 * State is persisted in {@code LOCAL_STORAGE} via the NGXS persist plugin.
 */
@State<AuthStateModel>({
  name: 'auth',
  defaults: {
    isAuthenticated: false,
    userData: null,
    accessToken: null,
    roles: [],
  },
})
@Injectable()
export class AuthState {
  private readonly oidcSecurityService = inject(OidcSecurityService);

  /** Selector that emits {@code true} while the user is authenticated. */
  @Selector()
  static isAuthenticated(state: AuthStateModel): boolean {
    return state.isAuthenticated;
  }

  /** Selector that emits the raw user-data claims object. */
  @Selector()
  static userData(state: AuthStateModel): any {
    return state.userData;
  }

  /** Selector that emits the current JWT access token, or {@code null}. */
  @Selector()
  static accessToken(state: AuthStateModel): string | null {
    return state?.accessToken ?? null;
  }

  /** Selector that emits the list of roles extracted from the JWT or user-data claims. */
  @Selector()
  static roles(state: AuthStateModel): string[] {
    return state.roles;
  }

  @Action(CheckAuth)
  checkAuth(ctx: StateContext<AuthStateModel>) {
    return this.updateAuthState(ctx);
  }

  @Action(LoginComplete)
  loginComplete(ctx: StateContext<AuthStateModel>) {
    return this.updateAuthState(ctx).pipe(
      switchMap(() => {
        const { isAuthenticated } = ctx.getState();
        return isAuthenticated ? ctx.dispatch(new TenantActions.LoadTenants()) : of(null);
      }),
    );
  }

  @Action(Login)
  login(ctx: StateContext<AuthStateModel>) {
    this.oidcSecurityService.authorize();
  }

  @Action(Logout)
  logout(ctx: StateContext<AuthStateModel>) {
    return this.oidcSecurityService.logoff().pipe(
      tap(() => {
        ctx.patchState({
          isAuthenticated: false,
          userData: null,
          accessToken: null,
          roles: [],
        });
        ctx.dispatch(new TenantActions.ClearTenants());
      }),
    );
  }

  @Action(SetUserData)
  setUserData(ctx: StateContext<AuthStateModel>, action: SetUserData) {
    ctx.patchState({
      userData: action.userData,
    });
  }

  private decodeJwtPayload(token: string): any {
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch {
      return {};
    }
  }

  private updateAuthState(ctx: StateContext<AuthStateModel>) {
    return this.oidcSecurityService.checkAuth().pipe(
      tap(({ isAuthenticated, userData, accessToken }) => {
        const tokenPayload = accessToken ? this.decodeJwtPayload(accessToken) : {};
        const roles: string[] =
          tokenPayload?.realm_access?.roles ??
          userData?.realm_access?.roles ??
          userData?.roles ??
          [];
        ctx.patchState({
          isAuthenticated,
          userData,
          accessToken,
          roles,
        });
      }),
    );
  }
}
