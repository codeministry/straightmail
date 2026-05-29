import { Component, effect, inject, input, signal } from '@angular/core';
import { AbstractControl, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { FormErrorComponent } from '../form-error/form-error.component';
import { ControlDirtyDirective } from '../../../directives/control-dirty.directive';

@Component({
  selector: 'app-textarea-field',
  standalone: true,
  imports: [FormsModule, TranslatePipe, FormErrorComponent, ControlDirtyDirective],
  templateUrl: './textarea-field.component.html',
})
/**
 * Reusable multi-line textarea field that wraps an {@link AbstractControl}.
 *
 * Mirrors the control value to an internal signal and writes back through {@link ControlDirtyDirective}.
 * Supports an optional monospace display mode and a "load sample" action that fetches a
 * sample plain-text body from {@code /samples/sample.txt}.
 */
export class TextareaFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<AbstractControl | null>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** Number of visible text rows in the textarea. */
  readonly rows = input(4);
  /** Placeholder text shown when the textarea is empty. */
  readonly placeholder = input('');
  /** When {@code true}, renders the textarea in a monospace font. */
  readonly mono = input(false);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');
  /** When {@code true}, renders the textarea as non-interactive and hides the sample action. */
  readonly disabled = input(false);

  protected value = signal('');
  private sub?: Subscription;
  private readonly http = inject(HttpClient);

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
   * Propagates the new value to the form control via {@link ControlDirtyDirective}.
   *
   * @param v The new string value entered by the user.
   * @param cd The dirty directive responsible for updating the control and marking it dirty.
   */
  onChange(v: string, cd: ControlDirtyDirective): void {
    cd.setValue(v);
  }

  /** Fetches a sample plain-text body from {@code /samples/sample.txt} and writes it into the control. */
  loadSample(): void {
    this.http.get('/samples/sample.txt', { responseType: 'text' }).subscribe((content) => {
      this.control()?.setValue(content);
      this.control()?.markAsDirty();
    });
  }
}
