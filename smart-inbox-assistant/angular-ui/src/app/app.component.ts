import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  template: `
    <div class="app-container">
      <header class="app-header">
        <h1>Smart Inbox Assistant</h1>
        <p class="subtitle">Pharmacovigilance Document Review System</p>
      </header>
      
      <main class="app-main">
        <app-message-list></app-message-list>
        <app-message-detail></app-message-detail>
      </main>
      
      <footer class="app-footer">
        <p>&copy; 2024 Smart Inbox Assistant - Phase 0-4 Complete</p>
      </footer>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    
    .app-header {
      background: linear-gradient(135deg, #1976d2, #1565c0);
      color: white;
      padding: 20px 40px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.2);
    }
    
    .app-header h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 500;
    }
    
    .subtitle {
      margin: 8px 0 0 0;
      opacity: 0.9;
      font-size: 14px;
    }
    
    .app-main {
      flex: 1;
      padding: 20px 40px;
      background-color: #f5f5f5;
    }
    
    .app-footer {
      background: #333;
      color: #aaa;
      text-align: center;
      padding: 16px;
      font-size: 13px;
    }
  `]
})
export class AppComponent {
  title = 'Smart Inbox Assistant';
}
