import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { MessageListComponent } from './components/message-list/message-list.component';
import { MessageDetailComponent } from './components/message-detail/message-detail.component';
import { ClassificationBadgeComponent } from './components/classification-badge/classification-badge.component';
import { FieldViewerComponent } from './components/field-viewer/field-viewer.component';
import { ReviewPanelComponent } from './components/review-panel/review-panel.component';

@NgModule({
  declarations: [
    AppComponent,
    MessageListComponent,
    MessageDetailComponent,
    ClassificationBadgeComponent,
    FieldViewerComponent,
    ReviewPanelComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
