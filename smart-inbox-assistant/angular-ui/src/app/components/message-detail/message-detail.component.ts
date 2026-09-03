import { Component, OnInit } from '@angular/core';
import { InboxService } from '../../services/inbox.service';
import { IncomingMessage, ClassificationResult, ExtractedField } from '../../models/inbox.models';

@Component({
  selector: 'app-message-detail',
  template: `
    <div class="card" *ngIf="message">
      <div class="card-header">
        Message Details - ID: {{ message.id }}
      </div>
      
      <div class="message-info">
        <p><strong>From:</strong> {{ message.sender }}</p>
        <p><strong>Subject:</strong> {{ message.subject }}</p>
        <p><strong>Received:</strong> {{ message.receivedAt | date:'yyyy-MM-dd HH:mm:ss' }}</p>
        <p><strong>Status:</strong> 
          <span [class]="message.processedFlag ? 'confidence-high' : 'confidence-low'" class="confidence-badge">
            {{ message.processedFlag ? 'Processed' : 'Pending' }}
          </span>
        </p>
      </div>
      
      <div class="message-body mt-3">
        <h4>Email Body</h4>
        <p>{{ message.bodyText }}</p>
      </div>
      
      <div class="classifications mt-3" *ngIf="classifications.length > 0">
        <h4>Classifications</h4>
        <div *ngFor="let cls of classifications">
          <app-classification-badge [category]="cls.category" [confidenceTier]="cls.confidenceTier"></app-classification-badge>
          <span class="text-muted">{{ cls.reason }}</span>
        </div>
      </div>
      
      <div class="extracted-fields mt-3" *ngIf="fieldsByGroup">
        <h4>Extracted Fields</h4>
        
        <app-field-viewer 
          *ngFor="let group of fieldGroups"
          [fields]="getFieldsForGroup(group)"
          [groupTitle]="group">
        </app-field-viewer>
      </div>
      
      <div class="review-section mt-3">
        <app-review-panel 
          [classificationResultId]="selectedClassificationId"
          (actionSubmitted)="onReviewAction($event)">
        </app-review-panel>
      </div>
    </div>
    
    <div class="card" *ngIf="!message">
      <div class="text-muted" style="padding: 40px; text-align: center;">
        Select a message from the list to view details
      </div>
    </div>
  `,
  styles: [`
    .message-info p {
      margin: 8px 0;
    }
    
    .message-body {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 4px;
    }
    
    .classifications {
      padding: 16px;
      background: #e3f2fd;
      border-radius: 4px;
    }
    
    .extracted-fields {
      padding: 16px;
      background: #fff;
    }
  `]
})
export class MessageDetailComponent implements OnInit {
  message: IncomingMessage | null = null;
  classifications: ClassificationResult[] = [];
  extractedFields: ExtractedField[] = [];
  fieldsByGroup: Map<string, ExtractedField[]> = new Map();
  fieldGroups: string[] = [];
  selectedClassificationId: number = 0;

  constructor(private inboxService: InboxService) {}

  ngOnInit(): void {
    // In production, this would subscribe to a shared service for selected message
    // For now, we'll load a sample or wait for selection event
  }

  loadMessageDetails(messageId: number): void {
    this.inboxService.getMessageById(messageId).subscribe({
      next: (msg) => {
        this.message = msg;
        this.loadClassifications(messageId);
      },
      error: () => console.error('Failed to load message details')
    });
  }

  loadClassifications(messageId: number): void {
    this.inboxService.getClassificationsByMessage(messageId).subscribe({
      next: (classifications) => {
        this.classifications = classifications;
        if (classifications.length > 0) {
          this.selectedClassificationId = classifications[0].id || 0;
          this.loadExtractedFields(this.selectedClassificationId);
        }
      },
      error: () => console.error('Failed to load classifications')
    });
  }

  loadExtractedFields(classificationId: number): void {
    this.inboxService.getExtractedFieldsByClassification(classificationId).subscribe({
      next: (fields) => {
        this.extractedFields = fields;
        this.groupFieldsByCategory();
      },
      error: () => console.error('Failed to load extracted fields')
    });
  }

  groupFieldsByCategory(): void {
    this.fieldsByGroup.clear();
    this.fieldGroups = [];
    
    this.extractedFields.forEach(field => {
      const group = field.fieldGroup;
      if (!this.fieldsByGroup.has(group)) {
        this.fieldsByGroup.set(group, []);
        this.fieldGroups.push(group);
      }
      this.fieldsByGroup.get(group)?.push(field);
    });
  }

  getFieldsForGroup(group: string): ExtractedField[] {
    return this.fieldsByGroup.get(group) || [];
  }

  onReviewAction(action: any): void {
    console.log('Review action submitted:', action);
    // In production, call API to persist the review action
  }
}
