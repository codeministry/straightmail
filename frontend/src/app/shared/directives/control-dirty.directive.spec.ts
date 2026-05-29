import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TestBed } from '@angular/core/testing';
import { ControlDirtyDirective } from './control-dirty.directive';

@Component({
  template: `<input [appControlDirty]="control" #cd="appControlDirty" />`,
  standalone: true,
  imports: [ReactiveFormsModule, ControlDirtyDirective],
})
class HostComponent {
  control = new FormControl('');
}

describe('ControlDirtyDirective', () => {
  it('should mark control as dirty when setValue is called', () => {
    TestBed.configureTestingModule({
      imports: [HostComponent, ControlDirtyDirective],
    });
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const directive = fixture.debugElement.children[0].injector.get(ControlDirtyDirective);
    directive.setValue('new value');

    expect(fixture.componentInstance.control.value).toBe('new value');
    expect(fixture.componentInstance.control.dirty).toBe(true);
  });

  it('should mark control as dirty when markAsDirty is called', () => {
    TestBed.configureTestingModule({
      imports: [HostComponent, ControlDirtyDirective],
    });
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const directive = fixture.debugElement.children[0].injector.get(ControlDirtyDirective);
    directive.markAsDirty();

    expect(fixture.componentInstance.control.dirty).toBe(true);
  });
});
