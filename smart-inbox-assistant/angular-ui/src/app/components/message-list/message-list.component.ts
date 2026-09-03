import { Component, OnInit } from '@angular/core';
import { InboxService } from '../../services/inbox.service';
import { IncomingMessage, ClassificationResult } from '../../models/inbox.models';

@Component({
  selector: 'app-message-list',
  template: `
    <div class="card">
      <div class="card-header flex-row" style="justify-content: space-between;">
        <span>Incoming Messages</span>
        <button class="btn btn-primary" (click)="loadMessages()">Refresh</button>
      </div>
      
      <div *ngIf="loading" class="text-muted">Loading messages...</div>
      
      <table class="data-table" *ngIf="!loading && messages.length > 0">
        <thead>
          <tr>
            <th>ID</th>
            <th>Sender</th>
            <th>Subject</th>
            <th>Received</th>
            <th>Categories</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let msg of messages" (click)="selectMessage(msg)" style="cursor: pointer;">
            <td>{{ msg.id }}</td>
            <td>{{ msg.sender }}</td>
            <td>{{ msg.subject }}</td>
            <td>{{ msg.receivedAt | date:'yyyy-MM-dd HH:mm' }}</td>
            <td>
              <app-classification-badge 
                *ngFor="let cat of msg.classifications"
                [category]="cat.category"
                [confidenceTier]="cat.confidenceTier">
              </app-classification-badge>
            </td>
            <td>
              <span [class]="msg.processedFlag ? 'confidence-high' : 'confidence-low'" 
                    class="confidence-badge">
                {{ msg.processedFlag ? 'Processed' : 'Pending' }}
              </span>
            </td>
            <td>
              <button class="btn btn-sm btn-primary" (click)="selectMessage(msg); $event.stopPropagation()">
                View
              </button>
              <button *ngIf="!msg.processedFlag" 
                      class="btn btn-sm btn-success" 
                      (click)="processMessage(msg.id); $event.stopPropagation()">
                Process
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div *ngIf="!loading && messages.length === 0" class="text-muted" style="padding: 20px; text-align: center;">
        No messages found. Add test data to the database.
      </div>
    </div>
  `,
  styles: [`
    .btn-sm {
      padding: 4px 8px;
      font-size: 12px;
      margin-left: 4px;
    }
  `]
})
export class MessageListComponent implements OnInit {
  messages: IncomingMessage[] = [];
  selectedMessage: IncomingMessage | null = null;
  loading = false;

  constructor(private inboxService: InboxService) {}

  ngOnInit(): void {
    this.loadMessages();
  }

  loadMessages(): void {
    this.loading = true;
    this.inboxService.getMessages(50, 0).subscribe({
      next: (data) => {
        this.messages = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        console.error('Failed to load messages');
      }
    });
  }

  selectMessage(message: IncomingMessage): void {
    this.selectedMessage = message;
    // Emit event for detail view (simplified - in production use a service or router)
    console.log('Selected message:', message.id);
  }

  processMessage(messageId: number): void {
    this.inboxService.triggerProcessing(messageId).subscribe({
      next: () => {
        alert('Processing started for message ' + messageId);
        this.loadMessages();
      },
      error: (err) => {
        alert('Failed to start processing: ' + (err.message || 'Unknown error'));
      }
    });
  }
}
