import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { IncomingMessage, ClassificationResult, ExtractedField, ReviewerAction } from '../models/inbox.models';

@Injectable({
  providedIn: 'root'
})
export class InboxService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const credentials = btoa('admin:admin123');
    return new HttpHeaders({
      'Authorization': `Basic ${credentials}`,
      'Content-Type': 'application/json'
    });
  }

  // Messages
  getMessages(limit: number = 50, offset: number = 0): Observable<IncomingMessage[]> {
    let params = new HttpParams()
      .set('limit', limit.toString())
      .set('offset', offset.toString());
    
    return this.http.get<IncomingMessage[]>(`${this.apiUrl}/messages`, {
      headers: this.getAuthHeaders(),
      params
    }).pipe(
      catchError(this.handleError<IncomingMessage[]>('getMessages', []))
    );
  }

  getMessageById(id: number): Observable<IncomingMessage> {
    return this.http.get<IncomingMessage>(`${this.apiUrl}/messages/${id}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError<IncomingMessage>('getMessageById'))
    );
  }

  getUnprocessedMessages(): Observable<IncomingMessage[]> {
    return this.http.get<IncomingMessage[]>(`${this.apiUrl}/messages/unprocessed`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError<IncomingMessage[]>('getUnprocessedMessages', []))
    );
  }

  // Classifications
  getClassificationsByMessage(messageId: number): Observable<ClassificationResult[]> {
    return this.http.get<ClassificationResult[]>(`${this.apiUrl}/classifications/message/${messageId}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError<ClassificationResult[]>('getClassificationsByMessage', []))
    );
  }

  // Extracted Fields
  getExtractedFieldsByClassification(classificationId: number): Observable<ExtractedField[]> {
    return this.http.get<ExtractedField[]>(`${this.apiUrl}/fields/classification/${classificationId}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError<ExtractedField[]>('getExtractedFieldsByClassification', []))
    );
  }

  // Reviewer Actions
  submitReviewerAction(action: Partial<ReviewerAction>): Observable<ReviewerAction> {
    return this.http.post<ReviewerAction>(`${this.apiUrl}/review/actions`, action, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError<ReviewerAction>('submitReviewerAction'))
    );
  }

  // Processing
  triggerProcessing(messageId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/processing/trigger/${messageId}`, {}, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError<any>('triggerProcessing'))
    );
  }

  getProcessingStatus(jobId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/processing/status/${jobId}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError<any>('getProcessingStatus'))
    );
  }

  // Health check
  healthCheck(): Observable<any> {
    return this.http.get(`${this.apiUrl}/health`).pipe(
      catchError(this.handleError<any>('healthCheck'))
    );
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);
      return of(result as T);
    };
  }
}
