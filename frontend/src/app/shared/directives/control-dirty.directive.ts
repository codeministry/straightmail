import { Directive, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

@Directive({
  selector: '[appControlDirty]',
  standalone: true,
  exportAs: 'appControlDirty',
})
/**
 * Attribute directive that provides a helper for updating a reactive form control's value while
 * simultaneously marking it as dirty.
 *
 * Used by the shared field components (text, select, textarea, code editor) that manage their
 * own value signal internally and need a bridge to propagate changes back to the form control
 * without losing the dirty state that {@code ngModel} would normally set automatically.
 *
 * Apply as {@code [appControlDirty]="control"} and reference via template variable
 * ({@code #cd="appControlDirty"}).
 */
export class ControlDirtyDirective {
  /** The form control this directive is bound to. */
  readonly control = input.required<AbstractControl | null>({ alias: 'appControlDirty' });

  /**
   * Sets the control's value and marks it as dirty, triggering validation and change detection.
   *
   * @param v The new value to set on the control.
   */
  setValue(v: any): void {
    const ctrl = this.control();
    if (ctrl) {
      ctrl.setValue(v, { emitEvent: true });
      ctrl.markAsDirty();
    }
  }

  /** Marks the bound control as dirty without changing its value. */
  markAsDirty(): void {
    this.control()?.markAsDirty();
  }
}
