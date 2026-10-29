import { Injectable } from '@angular/core';
import * as CryptoJS from 'crypto-js';

import { environment } from '../../../environments/environment';

/**
 * AES/CBC/Pkcs7 encryption with a key and IV shared with the backend (AESUtil.java).
 * Output is Base64, which the backend decodes before decrypting.
 */
@Injectable({ providedIn: 'root' })
export class EncryptionService {
  private readonly key = CryptoJS.enc.Utf8.parse(environment.aesKey);
  private readonly iv = CryptoJS.enc.Utf8.parse(environment.aesIv);

  encrypt(plainText: string): string {
    return CryptoJS.AES.encrypt(CryptoJS.enc.Utf8.parse(plainText), this.key, {
      iv: this.iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }).toString();
  }

  decrypt(cipherText: string): string {
    return CryptoJS.AES.decrypt(cipherText, this.key, {
      iv: this.iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }).toString(CryptoJS.enc.Utf8);
  }
}
