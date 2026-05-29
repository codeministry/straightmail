import { Component, effect, input, signal } from '@angular/core';
import { AbstractControl, FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { FormErrorComponent } from '../form-error/form-error.component';
import { ControlDirtyDirective } from '../../../directives/control-dirty.directive';

@Component({
  selector: 'app-select-field',
  standalone: true,
  imports: [FormsModule, TranslatePipe, FormErrorComponent, ControlDirtyDirective],
  templateUrl: './select-field.component.html',
})
/**
 * Reusable select (dropdown) field that wraps an {@link AbstractControl}.
 *
 * Mirrors the control value to an internal signal and writes back through {@link ControlDirtyDirective}.
 * The available options are projected via content projection in the template.
 */
export class SelectFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<AbstractControl | null>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** Whether the field is required (used to display an asterisk). */
  readonly required = input(false);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');
  /** When {@code true}, renders the select as non-interactive (HTML {@code disabled} attribute). */
  readonly disabled = input(false);

  protected value = signal<string>('');
  private sub?: Subscription;

  constructor() {
    effect(() => {
      this.sub?.unsubscribe();
      const ctrl = this.control();
      if (!ctrl) return;
      this.value.set(ctrl.value ?? '');
      this.sub = ctrl.valueChanges.subscribe((v) => this.value.set(v ?? ''));
    });
  }

  /**
   * Propagates the selected value to the form control via {@link ControlDirtyDirective}.
   *
   * @param v The newly selected value.
   * @param cd The dirty directive responsible for updating the control and marking it dirty.
   */
  onChange(v: string, cd: ControlDirtyDirective): void {
    cd.setValue(v);
  }
}
