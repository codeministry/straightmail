import { Component, effect, inject, input, signal } from '@angular/core';
import { AbstractControl, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { CodeEditorComponent } from '../code-editor/code-editor.component';
import { FormErrorComponent } from '../form-error/form-error.component';
import { ControlDirtyDirective } from '../../../directives/control-dirty.directive';

@Component({
  selector: 'app-json-model-field',
  standalone: true,
  imports: [
    FormsModule,
    TranslatePipe,
    CodeEditorComponent,
    FormErrorComponent,
    ControlDirtyDirective,
  ],
  templateUrl: './json-model-field.component.html',
})
/**
 * JSON model editor field that wraps a {@link CodeEditorComponent} (CodeMirror) for JSON.
 *
 * Mirrors the control value to an internal signal and writes back through {@link ControlDirtyDirective}.
 * The placeholder is resolved through {@code TranslateService} so translation keys are supported.
 * Provides a "load sample" action that fetches a sample JSON model from {@code /samples/sample.json}.
 */
export class JsonModelFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<AbstractControl | null>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** Translation key for the editor placeholder text. */
  readonly placeholder = input('');
  /** Number of visible rows for the code editor. */
  readonly rows = input(4);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');

  protected editorValue = signal('');
  protected resolvedPlaceholder = signal('');
  private sub?: Subscription;

  private readonly translate = inject(TranslateService);
  private readonly http = inject(HttpClient);

  constructor() {
    effect(() => {
      this.sub?.unsubscribe();
      const ctrl = this.control();
      if (!ctrl) return;
      this.editorValue.set(ctrl.value ?? '');
      this.sub = ctrl.valueChanges.subscribe((v) => this.editorValue.set(v ?? ''));
    });
    effect(() => {
      const key = this.placeholder();
      this.resolvedPlaceholder.set(key ? this.translate.instant(key) : '');
    });
  }

  /**
   * Propagates the new value to the form control via {@link ControlDirtyDirective}.
   *
   * @param v The new JSON string value from the editor.
   * @param cd The dirty directive responsible for updating the control and marking it dirty.
   */
  onValueChange(v: string, cd: ControlDirtyDirective): void {
    cd.setValue(v);
  }

  /** Fetches a sample JSON model from {@code /samples/sample.json} and writes it into the control. */
  loadSample(): void {
    this.http.get('/samples/sample.json', { responseType: 'text' }).subscribe((content) => {
      this.control()?.setValue(content);
      this.control()?.markAsDirty();
    });
  }
}
