import { Component, Input } from '@angular/core';
import { ExtractedField } from '../../models/inbox.models';

@Component({
  selector: 'app-field-viewer',
  template: `
    <div class="field-group" *ngIf="fields && fields.length > 0">
      <h4>{{ groupTitle }}</h4>
      <div class="field-list">
        <div class="field-item" *ngFor="let field of fields">
          <div class="field-header">
            <span class="field-name">{{ field.fieldName }}</span>
            <span class="confidence-badge" [ngClass]="getConfidenceClass(field.confidenceTier)">
              {{ field.confidenceTier }}
            </span>
            <span *ngIf="field.verifiedFlag" class="verified-badge">✓ Verified</span>
          </div>
          <div class="field-value">{{ field.fieldValue }}</div>
          <div class="field-source text-muted">
            Source: {{ field.sourceType }} - {{ field.sourceRef }}
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .field-group {
      margin-bottom: 20px;
    }
    
    .field-group h4 {
      color: #1976d2;
      border-bottom: 2px solid #e0e0e0;
      padding-bottom: 8px;
      margin-bottom: 12px;
    }
    
    .field-item {
      background: #fafafa;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 8px;
      border-left: 3px solid #1976d2;
    }
    
    .field-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 6px;
    }
    
    .field-name {
      font-weight: 600;
      color: #333;
    }
    
    .field-value {
      font-size: 15px;
      color: #222;
      margin-bottom: 6px;
    }
    
    .field-source {
      font-size: 12px;
    }
    
    .verified-badge {
      background: #4caf50;
      color: white;
      padding: 2px 6px;
      border-radius: 3px;
      font-size: 11px;
    }
  `]
})
export class FieldViewerComponent {
  @Input() fields: ExtractedField[] = [];
  @Input() groupTitle: string = '';

  getConfidenceClass(tier: string): string {
    const map: { [key: string]: string } = {
      'HIGH': 'confidence-high',
      'MEDIUM': 'confidence-medium',
      'LOW': 'confidence-low'
    };
    return map[tier] || '';
  }
}
