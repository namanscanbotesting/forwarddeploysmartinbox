import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ClassificationResult, ExtractedField, ReviewerAction } from '../../models/inbox.models';

@Component({
  selector: 'app-review-panel',
  template: `
    <div class="card">
      <div class="card-header">
        Reviewer Actions
      </div>
      
      <div class="review-form">
        <div class="form-group">
          <label class="form-label">Reviewer ID</label>
          <input type="text" class="form-input" [(ngModel)]="reviewerId" placeholder="Enter your ID">
        </div>
        
        <div class="form-group">
          <label class="form-label">Action</label>
          <select class="form-input" [(ngModel)]="actionType">
            <option value="accept">Accept</option>
            <option value="override">Override</option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="form-label">Field Name (if override)</label>
          <input type="text" class="form-input" [(ngModel)]="fieldName" placeholder="e.g., patient_age">
        </div>
        
        <div class="form-group">
          <label class="form-label">Previous Value</label>
          <textarea class="form-textarea" [(ngModel)]="previousValue" placeholder="Original extracted value"></textarea>
        </div>
        
        <div class="form-group">
          <label class="form-label">New Value</label>
          <textarea class="form-textarea" [(ngModel)]="newValue" placeholder="Corrected value"></textarea>
        </div>
        
        <div class="flex-row">
          <button class="btn btn-success" (click)="submitAction()">Submit Review</button>
          <button class="btn btn-primary" (click)="acceptAll()">Accept All Fields</button>
        </div>
      </div>
      
      <div *ngIf="submittedActions.length > 0" class="mt-3">
        <h4>Review History</h4>
        <div class="field-item" *ngFor="let action of submittedActions">
          <div class="field-header">
            <span class="field-name">{{ action.actionType }}</span>
            <span class="text-muted">{{ action.timestamp | date:'yyyy-MM-dd HH:mm:ss' }}</span>
          </div>
          <div *ngIf="action.fieldName">Field: {{ action.fieldName }}</div>
          <div *ngIf="action.previousValue">From: {{ action.previousValue }}</div>
          <div *ngIf="action.newValue">To: {{ action.newValue }}</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .review-form {
      max-width: 600px;
    }
    
    select.form-input {
      cursor: pointer;
    }
  `]
})
export class ReviewPanelComponent {
  @Input() classificationResultId: number = 0;
  @Output() actionSubmitted = new EventEmitter<ReviewerAction>();
  
  reviewerId = '';
  actionType: 'accept' | 'override' = 'accept';
  fieldName = '';
  previousValue = '';
  newValue = '';
  submittedActions: Partial<ReviewerAction>[] = [];

  submitAction(): void {
    if (!this.reviewerId) {
      alert('Please enter a reviewer ID');
      return;
    }

    const action: Partial<ReviewerAction> = {
      classificationResultId: this.classificationResultId,
      reviewerId: this.reviewerId,
      actionType: this.actionType,
      fieldName: this.fieldName,
      previousValue: this.previousValue,
      newValue: this.newValue,
      timestamp: new Date()
    };

    this.submittedActions.push(action);
    this.actionSubmitted.emit(action as ReviewerAction);
    
    // Reset form
    this.fieldName = '';
    this.previousValue = '';
    this.newValue = '';
    
    alert('Review action submitted successfully');
  }

  acceptAll(): void {
    if (!this.reviewerId) {
      alert('Please enter a reviewer ID');
      return;
    }

    const action: Partial<ReviewerAction> = {
      classificationResultId: this.classificationResultId,
      reviewerId: this.reviewerId,
      actionType: 'accept',
      fieldName: 'ALL_FIELDS',
      previousValue: '',
      newValue: '',
      timestamp: new Date()
    };

    this.submittedActions.push(action);
    this.actionSubmitted.emit(action as ReviewerAction);
    
    alert('All fields accepted');
  }
}
