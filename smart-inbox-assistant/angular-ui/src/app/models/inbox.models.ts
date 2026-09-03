export interface ClassificationResult {
  id?: number;
  messageId: number;
  category: 'ICSR' | 'PQC' | 'MI' | 'NOT_RELEVANT';
  confidenceTier: 'HIGH' | 'MEDIUM' | 'LOW';
  reason: string;
  modelRunId?: number;
  createdAt?: Date;
}

export interface ExtractedField {
  id?: number;
  classificationResultId: number;
  fieldGroup: 'Patient' | 'Reporter' | 'Product' | 'Reaction' | 'Severity' | 'Narrative' | 'PQC' | 'MI';
  fieldName: string;
  fieldValue: string;
  confidenceTier: 'HIGH' | 'MEDIUM' | 'LOW';
  sourceType: 'email_body' | 'pdf_page';
  sourceRef: string;
  verifiedFlag: boolean;
  extractionRunId?: number;
}

export interface IncomingMessage {
  id?: number;
  sender: string;
  subject: string;
  receivedAt: Date;
  bodyText: string;
  rawSourceRef: string;
  processedFlag: number;
  attachments?: Attachment[];
  classifications?: ClassificationResult[];
  extractedFields?: ExtractedField[];
}

export interface Attachment {
  id?: number;
  messageId: number;
  filename: string;
  mimeType: string;
  pdfType: 'digital' | 'scanned' | 'article' | 'non_english';
  storageRef: string;
  pageCount: number;
  languageDetected: string;
  createdAt?: Date;
}

export interface ReviewerAction {
  id?: number;
  classificationResultId: number;
  reviewerId: string;
  actionType: 'accept' | 'override';
  previousValue: string;
  newValue: string;
  fieldName: string;
  timestamp: Date;
}

export interface ProcessingJob {
  id?: number;
  messageId: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  errorMessage?: string;
  createdAt: Date;
  startedAt?: Date;
  completedAt?: Date;
}
