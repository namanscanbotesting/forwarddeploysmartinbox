import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-classification-badge',
  template: `
    <span class="category-badge" [ngClass]="getClassForCategory()">
      {{ category }}
      <span *ngIf="confidenceTier" class="confidence-dot" [ngClass]="getClassForConfidence()"></span>
    </span>
  `,
  styles: [`
    .confidence-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-left: 6px;
    }
  `]
})
export class ClassificationBadgeComponent {
  @Input() category: string = '';
  @Input() confidenceTier: string = '';

  getClassForCategory(): string {
    const categoryMap: { [key: string]: string } = {
      'ICSR': 'category-icsr',
      'PQC': 'category-pqc',
      'MI': 'category-mi',
      'NOT_RELEVANT': 'category-not-relevant'
    };
    return categoryMap[this.category] || '';
  }

  getClassForConfidence(): string {
    const confidenceMap: { [key: string]: string } = {
      'HIGH': 'confidence-high',
      'MEDIUM': 'confidence-medium',
      'LOW': 'confidence-low'
    };
    return confidenceMap[this.confidenceTier] || '';
  }
}
