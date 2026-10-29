import { Injectable } from '@angular/core';
import { Observable, from, map, shareReplay, switchMap } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * RSA-OAEP (SHA-256) encryption with the server's public key, using the browser's Web Crypto API.
 * The public key is fetched from the backend; only the backend holds the private key
 * (RSAUtil.java), so nothing in the frontend bundle can decrypt the values.
 */
@Injectable({ providedIn: 'root' })
export class EncryptionService {
  private publicKey$?: Observable<CryptoKey>;

  constructor(private authService: AuthService) {}

  /** Fetches and imports the public key once; later calls reuse it. */
  loadPublicKey(): Observable<CryptoKey> {
    if (!this.publicKey$) {
      this.publicKey$ = this.authService.getPublicKey().pipe(
        switchMap(res => from(crypto.subtle.importKey(
          'spki',
          this.base64ToBytes(res.publicKey),
          { name: 'RSA-OAEP', hash: 'SHA-256' },
          false,
          ['encrypt']
        ))),
        shareReplay(1)
      );
    }
    return this.publicKey$;
  }

  /** Forgets the cached key, e.g. after the server rejects a value it could not decrypt. */
  clearPublicKey(): void {
    this.publicKey$ = undefined;
  }

  /** Returns the Base64 RSA-OAEP ciphertext of the text. */
  encrypt(plainText: string): Observable<string> {
    return this.loadPublicKey().pipe(
      switchMap(key => from(crypto.subtle.encrypt(
        { name: 'RSA-OAEP' },
        key,
        new TextEncoder().encode(plainText)
      ))),
      map(cipherText => this.bytesToBase64(new Uint8Array(cipherText)))
    );
  }

  private base64ToBytes(base64: string): Uint8Array {
    return Uint8Array.from(atob(base64), c => c.charCodeAt(0));
  }

  private bytesToBase64(bytes: Uint8Array): string {
    return btoa(String.fromCharCode(...bytes));
  }
}
