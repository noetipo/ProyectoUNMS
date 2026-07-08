import { Component, inject, input, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentoEstudianteService } from '../../services/documento-estudiante.service';
import { DocumentoEstudiante, TipoDocumento } from '../../models/student.models';

interface DocPreview {
  url: string;
  safeUrl: SafeResourceUrl;
  contentType: string;
  name: string;
}

@Component({
  selector: 'app-documentos',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './documentos.component.html',
})
export class DocumentosComponent implements OnInit {
  readonly estudianteId = input.required<string>();

  private svc       = inject(DocumentoEstudianteService);
  private sanitizer = inject(DomSanitizer);

  readonly TipoDocumento = TipoDocumento;
  readonly tipos = [TipoDocumento.DNI_CE, TipoDocumento.PARTIDA_NACIMIENTO];

  readonly tipoLabel: Record<TipoDocumento, string> = {
    [TipoDocumento.DNI]:                'DNI',
    [TipoDocumento.CARNET_EXTRANJERIA]: 'CE',
    [TipoDocumento.PASAPORTE]:          'Pasaporte',
    [TipoDocumento.PTP]:                'PTP',
    [TipoDocumento.CARNET_DIPLOMATICO]: 'Carnet Diplomático',
    [TipoDocumento.DNI_CE]:             'DNI / CE',
    [TipoDocumento.PARTIDA_NACIMIENTO]: 'Partida de Nacimiento',
  };

  documentos   = signal<DocumentoEstudiante[]>([]);
  uploading    = signal(false);
  errorMsg     = signal<string | null>(null);
  previewDoc   = signal<DocPreview | null>(null);
  previewing   = signal(false);

  pendingFile: File | null = null;
  pendingTipo: TipoDocumento = TipoDocumento.DNI_CE;

  ngOnInit(): void {
    this._load();
  }

  private _load(): void {
    this.svc.getByEstudiante(this.estudianteId()).subscribe({
      next: docs => this.documentos.set(docs),
      error: e   => this.errorMsg.set(e.message),
    });
  }

  onTipoChange(event: Event): void {
    this.pendingTipo = (event.target as HTMLSelectElement).value as TipoDocumento;
  }

  onFileChange(event: Event): void {
    this.pendingFile = (event.target as HTMLInputElement).files?.[0] ?? null;
  }

  upload(): void {
    if (!this.pendingFile) return;
    this.uploading.set(true);
    this.errorMsg.set(null);
    this.svc.upload(this.estudianteId(), this.pendingTipo, this.pendingFile).subscribe({
      next: doc => {
        this.uploading.set(false);
        this.pendingFile = null;
        this.documentos.update(docs => {
          const idx = docs.findIndex(d => d.tipoDocumento === doc.tipoDocumento);
          return idx >= 0 ? docs.map((d, i) => i === idx ? doc : d) : [...docs, doc];
        });
      },
      error: e => { this.uploading.set(false); this.errorMsg.set(e.message); },
    });
  }

  download(doc: DocumentoEstudiante): void {
    this.svc.download(this.estudianteId(), doc.id).subscribe(blob => {
      const url  = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href     = url;
      link.download = doc.nombreOriginal;
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  openPreview(doc: DocumentoEstudiante): void {
    this.previewing.set(true);
    this.svc.download(this.estudianteId(), doc.id).subscribe({
      next: blob => {
        const ct  = doc.contentType || blob.type || 'application/octet-stream';
        const url = URL.createObjectURL(new Blob([blob], { type: ct }));
        this.previewDoc.set({
          url,
          safeUrl: this.sanitizer.bypassSecurityTrustResourceUrl(url),
          contentType: ct,
          name: doc.nombreOriginal,
        });
        this.previewing.set(false);
      },
      error: () => this.previewing.set(false),
    });
  }

  closePreview(): void {
    const p = this.previewDoc();
    if (p) URL.revokeObjectURL(p.url);
    this.previewDoc.set(null);
  }

  delete(doc: DocumentoEstudiante): void {
    if (!confirm(`¿Eliminar "${doc.nombreOriginal}"?`)) return;
    this.svc.delete(this.estudianteId(), doc.id).subscribe({
      next: () => this.documentos.update(docs => docs.filter(d => d.id !== doc.id)),
      error: e  => this.errorMsg.set(e.message),
    });
  }

  isImage(contentType: string): boolean {
    return contentType.startsWith('image/');
  }
}