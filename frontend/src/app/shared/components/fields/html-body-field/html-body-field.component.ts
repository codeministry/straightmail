import { Component, effect, inject, input, signal } from '@angular/core';
import { AbstractControl, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { CodeEditorComponent } from '../code-editor/code-editor.component';
import { FormErrorComponent } from '../form-error/form-error.component';
import { ControlDirtyDirective } from '../../../directives/control-dirty.directive';

@Component({
  selector: 'app-html-body-field',
  standalone: true,
  imports: [
    FormsModule,
    TranslatePipe,
    CodeEditorComponent,
    FormErrorComponent,
    ControlDirtyDirective,
  ],
  templateUrl: './html-body-field.component.html',
})
/**
 * HTML body editor field that wraps a {@link CodeEditorComponent} (CodeMirror) for HTML.
 *
 * Mirrors the control value to an internal signal and writes back through {@link ControlDirtyDirective}.
 * Provides a "load sample" action that fetches a sample HTML body from {@code /samples/sample.html}.
 */
export class HtmlBodyFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<AbstractControl | null>();
  /** Translation key used as the field label. */
  readonly label = input('templates.field_html');
  /** Number of visible rows for the code editor. */
  readonly rows = input(12);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');
  /** When {@code true}, renders the editor as non-interactive and hides the sample action. */
  readonly disabled = input(false);

  protected editorValue = signal('');
  private sub?: Subscription;
  private readonly http = inject(HttpClient);

  constructor() {
    effect(() => {
      this.sub?.unsubscribe();
      const ctrl = this.control();
      if (!ctrl) return;
      this.editorValue.set(ctrl.value ?? '');
      this.sub = ctrl.valueChanges.subscribe((v) => this.editorValue.set(v ?? ''));
    });
  }

  /**
   * Propagates the new value to the form control via {@link ControlDirtyDirective}.
   *
   * @param v The new HTML string value from the editor.
   * @param cd The dirty directive responsible for updating the control and marking it dirty.
   */
  onValueChange(v: string, cd: ControlDirtyDirective): void {
    cd.setValue(v);
  }

  /** Fetches a sample HTML body from {@code /samples/sample.html} and writes it into the control. */
  loadSample(): void {
    this.http.get('/samples/sample.html', { responseType: 'text' }).subscribe((content) => {
      this.control()?.setValue(content);
      this.control()?.markAsDirty();
    });
  }
}
