import { TestBed } from '@angular/core/testing';
import { TextFieldComponent } from './text-field.component';
import { FormControl } from '@angular/forms';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';

describe('TextFieldComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextFieldComponent, TranslatePipe],
      providers: [provideTranslateService()],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Test Label');
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display the label', () => {
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Email Address');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('label')?.textContent).toContain('Email Address');
  });

  it('should sync value from control', () => {
    const control = new FormControl('initial value');
    const fixture = TestBed.createComponent(TextFieldComponent);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Test');
    fixture.detectChanges();

    expect(fixture.componentInstance['value']()).toBe('initial value');

    control.setValue('new value');
    fixture.detectChanges();
    expect(fixture.componentInstance['value']()).toBe('new value');
  });
});
